package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.*;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;


@Mixin (ServerChunkManager.class)
public class ServerChunkManagerMixin implements ServerChunkManagerMixinAccess {
	@Shadow @Final private ServerWorld world;
	//public ServerWorld getServerWorld() { return this.world; }

	@Shadow @Final private ChunkTicketManager ticketManager;

	@Shadow @Final private ChunkLevelManager levelManager;
	@Unique
	private final PlayerDistanceMap playerDistanceMap = new PlayerDistanceMap();
	public PlayerDistanceMap fabric_per_player_spawns$getPlayerDistanceMap() { return playerDistanceMap; }

	@SuppressWarnings({"InvalidInjectorMethodSignature"})
	@Inject(method = "tickChunks(Lnet/minecraft/util/profiler/Profiler;J)V", at = @At(value = "INVOKE_ASSIGN",
			target = "Lnet/minecraft/world/SpawnHelper;setupSpawn(ILjava/lang/Iterable;Lnet/minecraft/world/SpawnHelper$ChunkSource;Lnet/minecraft/world/SpawnDensityCapper;)Lnet/minecraft/world/SpawnHelper$Info;"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void setupSpawning(Profiler profiler, long timeDelta, CallbackInfo ci, int i, SpawnHelper.Info info){

		/*
			Every all-chunks tick:
			1. Update distance map by adding all players
			2. Reset player's nearby mob counts
			3. Loop through all world's entities and add them to player's counts
	 	*/
		// update distance map
		playerDistanceMap.update(this.world.getPlayers(), ((TACSAccess) this.levelManager).fabric_per_player_spawns$simulationDistance());
		((InfoAccess)info).fabric_per_player_spawns$setChunkManager(this);

		// calculate mob counts
		Iterator<Entity> var5 = world.iterateEntities().iterator();
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
				long ll = ChunkPos.toLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
					// Find players in range of entity
					for (ServerPlayerEntity player : this.playerDistanceMap.getPlayersInRange(ll)) {
						// Increment player's sighting of entity
						((InfoAccess)info).fabric_per_player_spawns$incrementPlayerMobCount(player, spawnGroup);
				}
			}
		}

		/* debugging */

		PlayerMobCountMap map = ((InfoAccess)info).fabric_per_player_spawns$getPlayerMobCountMap();
		for (ServerPlayerEntity player : this.world.getPlayers()) {
			if(!player.getMainHandStack().isOf(Items.GLISTERING_MELON_SLICE)) continue;

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
				player.sendMessage(Text.literal(playerDistanceMap.posMapSize() + " Chunks stored. Caps: You: " + mobCountNearPlayer + "; Highest here - " + playerM.getName().getString() + ": " + mobCountNearPlayerM), true);
			}
			else if(player.isSpectator()) {
				StringBuilder str = new StringBuilder();
				str.append(playerDistanceMap.posMapSize()).append(" Chunks stored. ");
				str.append("Players affecting this chunk: ");
				int x = ((int) player.getX()) / 16;
				int z = ((int) player.getZ()) / 16;
				for (ServerPlayerEntity playerN : playerDistanceMap.getPlayersInRange(ChunkPos.toLong(x, z))) {
					str.append(playerN.getName().getString()).append(" ")
							.append(map.getPlayerMobCount(playerN, SpawnGroup.MONSTER)).append(", ");
				}
				player.sendMessage(Text.literal(str.toString()), true);
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


	}

}


