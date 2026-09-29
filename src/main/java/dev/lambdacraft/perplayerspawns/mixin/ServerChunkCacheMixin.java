package dev.lambdacraft.perplayerspawns.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import dev.lambdacraft.perplayerspawns.access.SpawnStateAccess;
import dev.lambdacraft.perplayerspawns.access.ServerChunkCacheMixinAccess;
import dev.lambdacraft.perplayerspawns.access.DistanceManagerAccess;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;


@Mixin (ServerChunkCache.class)
public class ServerChunkCacheMixin implements ServerChunkCacheMixinAccess {
	@Shadow @Final private ServerLevel level;
	//public ServerWorld getServerWorld() { return this.world; }

	@Shadow @Final private TicketStorage ticketStorage;

	@Shadow @Final private DistanceManager distanceManager;
	@Unique
	private final PlayerDistanceMap playerDistanceMap = new PlayerDistanceMap();
	public PlayerDistanceMap fabric_per_player_spawns$getPlayerDistanceMap() { return playerDistanceMap; }

	@Definition(id = "createState", method = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;")
    @Expression("? = createState(?, ?, ?, ?)")
    @Inject(
			method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
	private void setupSpawning(ProfilerFiller profiler, CallbackInfo ci, int i, NaturalSpawner.SpawnState info){

		/*
			Every all-chunks tick:
			1. Update distance map by adding all players
			2. Reset player's nearby mob counts
			3. Loop through all world's entities and add them to player's counts
	 	*/
		// update distance map
		playerDistanceMap.update(this.level.players(), ((DistanceManagerAccess) this.distanceManager).fabric_per_player_spawns$simulationDistance());
		((SpawnStateAccess)info).fabric_per_player_spawns$setChunkManager(this);

		// calculate mob counts
		Iterator<Entity> var5 = level.getAllEntities().iterator();
		out:
		while(true) {
			Entity entity;
			Mob mobEntity;
			do {
				if (!var5.hasNext()) break out;
				entity = var5.next();
				if (!(entity instanceof Mob) ) break;
				mobEntity = (Mob) entity;
			}
			while(mobEntity.isPersistenceRequired() || mobEntity.requiresCustomPersistence());

			MobCategory spawnGroup = entity.getType().getCategory();
			if (spawnGroup != MobCategory.MISC) {
				BlockPos blockPos = entity.blockPosition();
				long ll = ChunkPos.pack(blockPos.getX() >> 4, blockPos.getZ() >> 4);
					// Find players in range of entity
					for (ServerPlayer player : this.playerDistanceMap.getPlayersInRange(ll)) {
						// Increment player's sighting of entity
						((SpawnStateAccess)info).fabric_per_player_spawns$incrementPlayerMobCount(player, spawnGroup);
				}
			}
		}

		/* debugging */

		PlayerMobCountMap map = ((SpawnStateAccess)info).fabric_per_player_spawns$getPlayerMobCountMap();
		for (ServerPlayer player : this.level.players()) {
			if(!player.getMainHandItem().is(Items.GLISTERING_MELON_SLICE)) continue;

			//System.out.println(player.getName().asString() + ": " + Arrays.toString(((PlayerEntityAccess) player).getMobCounts()));
			if (player.isCreative()) {
				int x = ((int) player.getX()) >> 4;
				int z = ((int) player.getZ()) >> 4;
				ServerPlayer playerM = player;
				int mobCountNearPlayer = map.getPlayerMobCount(player, MobCategory.MONSTER);
				int mobCountNearPlayerM = map.getPlayerMobCount(playerM, MobCategory.MONSTER);
				for (ServerPlayer playerN : playerDistanceMap.getPlayersInRange(ChunkPos.pack(x, z))) {
					int mobCountNearPlayerN = map.getPlayerMobCount(playerN, MobCategory.MONSTER);
					if (mobCountNearPlayerN > mobCountNearPlayerM) {
						playerM = playerN;
						mobCountNearPlayerM = mobCountNearPlayerN;
					}
				}
				player.sendSystemMessage(Component.literal(playerDistanceMap.posMapSize() + " Chunks stored. Caps: You: " + mobCountNearPlayer + "; Highest here - " + playerM.getName().getString() + ": " + mobCountNearPlayerM), true);
			}
			else if(player.isSpectator()) {
				StringBuilder str = new StringBuilder();
				str.append(playerDistanceMap.posMapSize()).append(" Chunks stored. ");
				str.append("Players affecting this chunk: ");
				int x = ((int) player.getX()) / 16;
				int z = ((int) player.getZ()) / 16;
				for (ServerPlayer playerN : playerDistanceMap.getPlayersInRange(ChunkPos.pack(x, z))) {
					str.append(playerN.getName().getString()).append(" ")
							.append(map.getPlayerMobCount(playerN, MobCategory.MONSTER)).append(", ");
				}
				player.sendSystemMessage(Component.literal(str.toString()), true);
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


