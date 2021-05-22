package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.*;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

//import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
//import net.minecraft.text.LiteralText;

@Mixin (ServerChunkManager.class)
public class ServerChunkManagerMixin implements ServerChunkManagerMixinAccess {
	@Shadow @Final private ServerWorld world;
	//public ServerWorld getServerWorld() { return this.world; }

	@Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

	private final PlayerDistanceMap playerDistanceMap = new PlayerDistanceMap();
	public PlayerDistanceMap getPlayerDistanceMap() { return playerDistanceMap; }

	@SuppressWarnings("UnresolvedMixinReference")
	@Redirect(method = "tickChunks", at = @At(value = "INVOKE",
			target = "net/minecraft/world/SpawnHelper.setupSpawn (ILjava/lang/Iterable;Lnet/minecraft/world/SpawnHelper$ChunkSource;)Lnet/minecraft/world/SpawnHelper$Info;"))
	private SpawnHelper.Info setupSpawning(int spawningChunkCount, Iterable<Entity> entities, SpawnHelper.ChunkSource chunkSource){

		/*
			Every all-chunks tick:
			1. Update distance map by adding all players
			2. Reset player's nearby mob counts
			3. Loop through all world's entities and add them to player's counts
	 	*/
		// update distance map
		playerDistanceMap.update(this.world.getPlayers(), ((TACSAccess) this.threadedAnvilChunkStorage).renderDistance());

		// calculate mob counts

		SpawnHelper.Info info = SpawnHelper.setupSpawn(spawningChunkCount, entities, chunkSource);
		Iterator<Entity> var5 = entities.iterator();
		out:
		while(true) {
			Entity entity;
			MobEntity mobEntity;
			do {
				if (!var5.hasNext()) break out;
				entity = var5.next();
				if (!(entity instanceof MobEntity)) break;
				mobEntity = (MobEntity)entity;
			}
			while(mobEntity.isPersistent() || mobEntity.cannotDespawn());

			SpawnGroup spawnGroup = entity.getType().getSpawnGroup();
			if (spawnGroup != SpawnGroup.MISC) {
				BlockPos blockPos = entity.getBlockPos();
				long l = ChunkPos.toLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
				chunkSource.query(l, (worldChunk) -> {
					// Find players in range of entity
					for (ServerPlayerEntity player : this.playerDistanceMap.getPlayersInRange(l)) {
						// Increment player's sighting of entity
						((InfoAccess)info).incrementPlayerMobCount(player, spawnGroup);
					}
				});
			}
		}

		/* debugging * /

		PlayerMobCountMap map = ((InfoAccess)info).getPlayerMobCountMap();
		for (ServerPlayerEntity player : this.world.getPlayers()) {

			//System.out.println(player.getName().asString() + ": " + Arrays.toString(((PlayerEntityAccess) player).getMobCounts()));
			if (player.isCreative()) {
				int x = ((int) player.getX()) >> 4;
				int z = ((int) player.getZ()) >> 4;
				ServerPlayerEntity playerM = player;
				int mobCountNearPlayer = map.getPlayerMobCount(player, SpawnGroup.MONSTER);
				int mobCountNearPlayerM = map.getPlayerMobCount(playerM, SpawnGroup.MONSTER);
				for (ServerPlayerEntity playerN : playerDistanceMap.getPlayersInRange(ChunkPos.toLong(x, z))) {
					int mobCountNearPlayerN = map.getPlayerMobCount(playerN, SpawnGroup.MONSTER);
					if (mobCountNearPlayerN > mobCountNearPlayerM) {
						playerM = playerN;
						mobCountNearPlayerM = mobCountNearPlayerN;
					}
				}
				player.sendMessage(new LiteralText(playerDistanceMap.posMapSize() + "Chunks stored. Caps: You: " + mobCountNearPlayer + "; Highest here - " + playerM.getName().asString() + ": " + mobCountNearPlayerM), true);
			}
			else if(player.isSpectator()) {
				StringBuilder str = new StringBuilder();
				str.append(playerDistanceMap.posMapSize()).append(" Chunks stored. ");
				str.append("Players affecting this chunk: ");
				int x = ((int) player.getX()) / 16;
				int z = ((int) player.getZ()) / 16;
				for (ServerPlayerEntity playerN : playerDistanceMap.getPlayersInRange(ChunkPos.toLong(x, z))) {
					str.append(playerN.getName().asString()).append(" ")
							.append(map.getPlayerMobCount(playerN, SpawnGroup.MONSTER)).append(", ");
				}
				player.sendMessage(new LiteralText(str.toString()), true);
			}
			//if(player.isCreative() && player.isOnFire() && player.isSneaking() && player.isHolding(Items.STRUCTURE_VOID)){
			//	Gson gson = new GsonBuilder().create();
			//	File plF = new File("playerDump.txt");
			//	plF.createNewFile();
			//	System.out.println(gson.toJson(player));
			//	System.out.println(gson.toJson(mobDistanceMap));
			//}
		}
		/**/

		((InfoAccess)info).setChunkManager(this);
		return info;
	}

}


