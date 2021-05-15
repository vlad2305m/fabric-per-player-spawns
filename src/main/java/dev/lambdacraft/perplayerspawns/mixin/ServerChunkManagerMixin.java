package dev.lambdacraft.perplayerspawns.mixin;

import com.mojang.datafixers.util.Either;
import dev.lambdacraft.perplayerspawns.access.*;
import dev.lambdacraft.perplayerspawns.util.PlayerMobDistanceMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GravityField;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;


@Mixin (ServerChunkManager.class)
public class ServerChunkManagerMixin implements ServerChunkManagerMixinAccess {
	@Shadow @Final private ServerWorld world;
	public ServerWorld getServerWorld() { return this.world; }

	@Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

	private final PlayerMobDistanceMap playerMobDistanceMap = new PlayerMobDistanceMap();
	public PlayerMobDistanceMap getPlayerDistanceMap() { return playerMobDistanceMap; }

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
		playerMobDistanceMap.update(this.world.getPlayers(), ((TACSAccess) this.threadedAnvilChunkStorage).renderDistance());

		// calculate mob counts

		SpawnHelper.Info info = SpawnHelper.setupSpawn(spawningChunkCount, entities, chunkSource);
		Iterator var5 = entities.iterator();
		out:
		while(true) {
			Entity entity;
			MobEntity mobEntity;
			do {
				if (!var5.hasNext()) break out;
				entity = (Entity)var5.next();
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
					for (ServerPlayerEntity player : this.playerMobDistanceMap.getPlayersInRange(l)) {
						// Increment player's sighting of entity
						((InfoAccess)info).incrementPlayerMobCount(player, spawnGroup);
					}
				});
			}
		}

		/* debugging * /
		PlayerMobDistanceMap mobDistanceMap = tacs.playerMobDistanceMap();
		for (PlayerEntity player : this.world.getPlayers()) {

			//System.out.println(player.getName().asString() + ": " + Arrays.toString(((PlayerEntityAccess) player).getMobCounts()));
			if (player.isCreative()) {
				int x = ((int) player.getX()) / 16;
				int z = ((int) player.getZ()) / 16;
				PlayerEntity playerM = player;
				int mobCountNearPlayer = ((PlayerEntityAccess) player).getMobCountForSpawnGroup(SpawnGroup.MONSTER);
				int mobCountNearPlayerM = ((PlayerEntityAccess) player).getMobCountForSpawnGroup(SpawnGroup.MONSTER);
				for (PlayerEntity playerN : mobDistanceMap.getPlayersInRange(x, z)) {
					int mobCountNearPlayerN = ((PlayerEntityAccess) playerN).getMobCountForSpawnGroup(SpawnGroup.MONSTER);
					if (mobCountNearPlayerN > mobCountNearPlayerM) {
						playerM = playerN;
						mobCountNearPlayerM = mobCountNearPlayerN;
					}
				}
				player.sendMessage(new LiteralText("You: " + mobCountNearPlayer + "; Highest here - " + playerM.getName().asString()+": "+mobCountNearPlayerM), true);
			}
			else if(player.isSpectator()) {
				StringBuilder str = new StringBuilder();
				str.append("Players affecting this chunk: ");
				int x = ((int) player.getX()) / 16;
				int z = ((int) player.getZ()) / 16;
				for (PlayerEntity playerN : mobDistanceMap.getPlayersInRange(x, z)) {
					str.append(playerN.getName().asString()).append(" ")
							.append(((PlayerEntityAccess) playerN).getMobCountForSpawnGroup(SpawnGroup.MONSTER)).append(", ");
				}
				player.sendMessage(new LiteralText(str.toString()), true);
			}
			/*if(player.isCreative() && player.isOnFire() && player.isSneaking() && player.isHolding(Items.STRUCTURE_VOID)){
				Gson gson = new GsonBuilder().create();
				File plF = new File("playerDump.txt");
				plF.createNewFile();
				System.out.println(gson.toJson(player));
				System.out.println(gson.toJson(mobDistanceMap));
			}* /
		}
		/**/

		((InfoAccess)info).setChunkManager(this);
		return info;
	}

}


