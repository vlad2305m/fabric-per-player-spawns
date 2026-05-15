package dev.lambdacraft.perplayerspawns.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Spottedleaf
 */
public final class PlayerDistanceMap {

	private static final PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> EMPTY_SET = new PooledHashSets.PooledObjectLinkedOpenHashSet<>();

	private final Map<ServerPlayer, SectionPos> players = new HashMap<>();
	// we use linked for better iteration.
	private final Long2ObjectOpenHashMap<PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer>> playerMapUnsynched = new Long2ObjectOpenHashMap<>(1024, 0.5f);
	private final Long2ObjectMap<PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer>> playerMap = Long2ObjectMaps.synchronize(playerMapUnsynched);
	private int viewDistance;

	private final PooledHashSets<ServerPlayer> pooledHashSets = new PooledHashSets<>();

	public PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> getPlayersInRange(final long l) {
		return this.playerMap.getOrDefault(l, EMPTY_SET);
	}
	public long posMapSize() { return this.playerMap.size(); }

	public void update(final List<ServerPlayer> currentPlayers, final int newViewDistance) {

		final ObjectLinkedOpenHashSet<ServerPlayer> gone = new ObjectLinkedOpenHashSet<>(this.players.keySet());

		final int oldViewDistance = this.viewDistance;
		this.viewDistance = newViewDistance;

		for (final ServerPlayer player : currentPlayers) {
			if (player.isSpectator()) {
				continue; // will be left in 'gone' (or not added at all)
			}

			gone.remove(player);

			final SectionPos newPosition = player.getLastSectionPos();
			final SectionPos oldPosition = this.players.put(player, newPosition);

			if (oldPosition == null) {
				this.addNewPlayer(player, newPosition, newViewDistance);
			} else {
				this.updatePlayer(player, oldPosition, newPosition, oldViewDistance, newViewDistance);
			}
			//this.validatePlayer(player, newViewDistance); // debug only
		}

		for (final ServerPlayer player : gone) {
			final SectionPos oldPosition = this.players.remove(player);
			if (oldPosition != null) {
				this.removePlayer(player, oldPosition, oldViewDistance);
			}
		}
	}

	// expensive op, only for debug
	/*private void validatePlayer(final ServerPlayer player, final int viewDistance) {

		int entiesGot = 0;
		int expectedEntries = (2 * viewDistance + 1);
		expectedEntries *= expectedEntries;

		final SectionPos currPosition = ((ServerServerPlayer)player).getCameraPosition();

		final int centerX = currPosition.getX();
		final int centerZ = currPosition.getZ();

		for (final Long2ObjectLinkedOpenHashMap.Entry<PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer>> entry : this.playerMap.long2ObjectEntrySet()) {
			final long key = entry.getLongKey();
			final PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> map = entry.getValue();

			if (map.referenceCount == 0) {
				throw new IllegalStateException("Invalid map");
			}

			if (map.set.contains(player)) {
				++entiesGot;

				final int chunkX = ChunkPos.getPackedX(key);
				final int chunkZ = ChunkPos.getPackedZ(key);

				final int dist = Math.max(Math.abs(chunkX - centerX), Math.abs(chunkZ - centerZ));

				if (dist > viewDistance) {
					throw new IllegalStateException("Expected view distance " + viewDistance + ", got " + dist);
				}
			}
		}

		if (entiesGot != expectedEntries) {
			throw new IllegalStateException("Expected " + expectedEntries + ", got " + entiesGot);
		}
	}*/

	private void addPlayerTo(final ServerPlayer player, final int chunkX, final int chunkZ) {
		this.playerMap.compute(ChunkPos.pack(chunkX, chunkZ), (final Long key, final PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> players) -> {
			if (players == null) {
				return new PooledHashSets.PooledObjectLinkedOpenHashSet<>(player);
			} else {
				return PlayerDistanceMap.this.pooledHashSets.findMapWith(players, player);
			}
		});
	}

	private void removePlayerFrom(final ServerPlayer player, final int chunkX, final int chunkZ) {
		this.playerMap.compute(ChunkPos.pack(chunkX, chunkZ), (final Long keyInMap, final PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> players) -> {
			assert players != null;
			return PlayerDistanceMap.this.pooledHashSets.findMapWithout(players, player); // rets null instead of an empty map
		});
	}

	private void updatePlayer(final ServerPlayer player, final SectionPos oldPosition, final SectionPos newPosition, final int oldViewDistance, final int newViewDistance) {
		final int toX = newPosition.getX();
		final int toZ = newPosition.getZ();
		final int fromX = oldPosition.getX();
		final int fromZ = oldPosition.getZ();

		final int dx = toX - fromX;
		final int dz = toZ - fromZ;

		final int totalX = Math.abs(fromX - toX);
		final int totalZ = Math.abs(fromZ - toZ);

		if (Math.max(totalX, totalZ) >= (2 * oldViewDistance)) {
			// teleported?
			this.removePlayer(player, oldPosition, oldViewDistance);
			this.addNewPlayer(player, newPosition, newViewDistance);
			return;
		}

		// x axis is width
		// z axis is height
		// right refers to the x axis of where we moved
		// top refers to the z axis of where we moved

		if (oldViewDistance == newViewDistance) {
			// same view distance

			// used for relative positioning
			final int up = 1 | (dz >> (Integer.SIZE - 1)); // 1 if dz >= 0, -1 otherwise
			final int right = 1 | (dx >> (Integer.SIZE - 1)); // 1 if dx >= 0, -1 otherwise

			// The area excluded by overlapping the two view distance squares creates four rectangles:
			// Two on the left, and two on the right. The ones on the left we consider the "removed" section
			// and on the right the "added" section.
			// https://i.imgur.com/MrnOBgI.png is a reference image. Note that the outside border is not actually
			// exclusive to the regions they surround.
			// ▓ → █ (distance = 3)
			// ▒ - player stored; ░ - player not stored
			// ▒ ▒ ▒ ▒ ▒
			// ▒ ▒ ▒ ▒ ▒ ░ ░
			// ▒ ▒ ▓ ▒ ▒ ░ ░
			// ▒ ▒ ▒ ▒ █ ░ ░
			// ▒ ▒ ▒ ▒ ▒ ░ ░
			//     ░ ░ ░ ░ ░

			// 4 points of the rectangle
			int maxX; // exclusive
			int minX; // inclusive
			int maxZ; // exclusive
			int minZ; // inclusive

			if (dx != 0) {
				// handle right addition
				// ▒ ▒ ▒ ▒ ▒
				// ▒ ▒ ▒ ▒ ▒ + +
				// ▒ ▒ ▓ ▒ ▒ + +
				// ▒ ▒ ▒ ▒ █ + +
				// ▒ ▒ ▒ ▒ ▒ + +
				//     ░ ░ ░ ░ ░

				maxX = toX + (oldViewDistance * right) + right; // exclusive
				minX = fromX + (oldViewDistance * right) + right; // inclusive
				maxZ = fromZ + (oldViewDistance * up) + up; // exclusive
				minZ = toZ - (oldViewDistance * up); // inclusive

				for (int currX = minX; currX != maxX; currX += right) {
					for (int currZ = minZ; currZ != maxZ; currZ += up) {
						this.addPlayerTo(player, currX, currZ);
					}
				}
			}

			if (dz != 0) {
				// handle up addition
				// ▒ ▒ ▒ ▒ ▒
				// ▒ ▒ ▒ ▒ ▒ ▒ ▒
				// ▒ ▒ ▓ ▒ ▒ ▒ ▒
				// ▒ ▒ ▒ ▒ █ ▒ ▒
				// ▒ ▒ ▒ ▒ ▒ ▒ ▒
				//     + + + + +

				maxX = toX + (oldViewDistance * right) + right; // exclusive
				minX = toX - (oldViewDistance * right); // inclusive
				maxZ = toZ + (oldViewDistance * up) + up; // exclusive
				minZ = fromZ + (oldViewDistance * up) + up; // inclusive

				for (int currX = minX; currX != maxX; currX += right) {
					for (int currZ = minZ; currZ != maxZ; currZ += up) {
						this.addPlayerTo(player, currX, currZ);
					}
				}
			}

			if (dx != 0) {
				// handle left removal
				// ▒ ▒ ▒ ▒ ▒
				// - - ▒ ▒ ▒ ▒ ▒
				// - - ▓ ▒ ▒ ▒ ▒
				// - - ▒ ▒ █ ▒ ▒
				// - - ▒ ▒ ▒ ▒ ▒
				//     ▒ ▒ ▒ ▒ ▒

				maxX = toX - (oldViewDistance * right); // exclusive
				minX = fromX - (oldViewDistance * right); // inclusive
				maxZ = fromZ + (oldViewDistance * up) + up; // exclusive
				minZ = toZ - (oldViewDistance * up); // inclusive

				for (int currX = minX; currX != maxX; currX += right) {
					for (int currZ = minZ; currZ != maxZ; currZ += up) {
						this.removePlayerFrom(player, currX, currZ);
					}
				}
			}

			if (dz != 0) {
				// handle down removal
				// - - - - -
				// ░ ░ ▒ ▒ ▒ ▒ ▒
				// ░ ░ ▓ ▒ ▒ ▒ ▒
				// ░ ░ ▒ ▒ █ ▒ ▒
				// ░ ░ ▒ ▒ ▒ ▒ ▒
				//     ▒ ▒ ▒ ▒ ▒

				maxX = fromX + (oldViewDistance * right) + right; // exclusive
				minX = fromX - (oldViewDistance * right); // inclusive
				maxZ = toZ - (oldViewDistance * up); // exclusive
				minZ = fromZ - (oldViewDistance * up); // inclusive

				for (int currX = minX; currX != maxX; currX += right) {
					for (int currZ = minZ; currZ != maxZ; currZ += up) {
						this.removePlayerFrom(player, currX, currZ);
					}
				}
			}
			// ░ ░ ░ ░ ░
			// ░ ░ ▒ ▒ ▒ ▒ ▒
			// ░ ░ ▓ ▒ ▒ ▒ ▒
			// ░ ░ ▒ ▒ █ ▒ ▒
			// ░ ░ ▒ ▒ ▒ ▒ ▒
			//     ▒ ▒ ▒ ▒ ▒
		} else {
			// different view distance
			// for now :)
			this.removePlayer(player, oldPosition, oldViewDistance);
			this.addNewPlayer(player, newPosition, newViewDistance);
		}
	}

	private void removePlayer(final ServerPlayer player, final SectionPos position, final int viewDistance) {
		final int x = position.getX();
		final int z = position.getZ();

		for (int xoff = -viewDistance; xoff <= viewDistance; ++xoff) {
			for (int zoff = -viewDistance; zoff <= viewDistance; ++zoff) {
				this.removePlayerFrom(player, x + xoff, z + zoff);
			}
		}
	}

	private void addNewPlayer(final ServerPlayer player, final SectionPos position, final int viewDistance) {
		final int x = position.getX();
		final int z = position.getZ();

		for (int xoff = -viewDistance; xoff <= viewDistance; ++xoff) {
			for (int zoff = -viewDistance; zoff <= viewDistance; ++zoff) {
				this.addPlayerTo(player, x + xoff, z + zoff);
			}
		}
	}
}
