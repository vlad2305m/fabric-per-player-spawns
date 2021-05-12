package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.PlayerEntityAccess;
import dev.lambdacraft.perplayerspawns.access.TACSAccess;
import dev.lambdacraft.perplayerspawns.util.PlayerMobDistanceMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// crust name: PlayerChunkMap
@Mixin (ThreadedAnvilChunkStorage.class)
public abstract class TACSMixin implements TACSAccess {
	@Shadow private int watchDistance;
	private final PlayerMobDistanceMap playerMobDistanceMap = new PlayerMobDistanceMap();

	@Override
	public void updatePlayerMobTypeMap(Entity entity) {
		int chunkX = (int) Math.floor(entity.getX()) >> 4;
		int chunkZ = (int) Math.floor(entity.getZ()) >> 4;

		int index = entity.getType().getSpawnGroup().ordinal();

		// Find players in range of entity
		for (PlayerEntity player : this.playerMobDistanceMap.getPlayersInRange(chunkX, chunkZ)) {
			// Increment player's sighting of entity
			((PlayerEntityAccess)player).getMobCounts()[index] += 1;
			//System.out.print(" +1 ");
		}
		//System.out.print(";");
	}

	@Override
	public PlayerMobDistanceMap playerMobDistanceMap() {
		return this.playerMobDistanceMap;
	}

	@Override
	public int renderDistance() {
		return this.watchDistance;
	}
}
