package dev.lambdacraft.perplayerspawns.mixin;

import com.mojang.datafixers.util.Either;
import dev.lambdacraft.perplayerspawns.access.*;
import dev.lambdacraft.perplayerspawns.util.PlayerMobDistanceMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// crust name: ChunkProviderServer
@Mixin (ServerChunkManager.class)
public class ServerChunkManagerMixin implements ServerChunkManagerMixinAccess {
	@Shadow @Final private ServerWorld world;

	@Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

	//@Shadow @Final private static int CHUNKS_ELIGIBLE_FOR_SPAWNING;

	////////////////////////
	//fixed some yelling by reimplementing removed method
	@Shadow private boolean isFutureReady(long pos, Function<ChunkHolder, CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>>> futureFunction){return false;}

	public boolean method_20727(Entity entity) {
		long l = ChunkPos.toLong(MathHelper.floor(entity.getX()) >> 4, MathHelper.floor(entity.getZ()) >> 4);
		return this.isFutureReady(l, ChunkHolder::getAccessibleFuture);
	}
	////////////////////////

	/*@Inject(
			method = "tickChunks",
			at = @At (value = "INVOKE", target = "Lnet/minecraft/entity/SpawnGroup;values()[Lnet/minecraft/entity/SpawnGroup;"))
	/*
		Every all-chunks tick:
		1. Update distance map by adding all players
		2. Reset player's nearby mob counts
		3. Loop through all world's entities and add them to player's counts
	 * /
	private void updateDistanceMap(CallbackInfo info) {
		TACSAccess tacs = ((TACSAccess) this.threadedAnvilChunkStorage);
		// update distance map
		tacs.playerMobDistanceMap().update(this.world.getPlayers(), tacs.renderDistance());
		// re-set mob counts
		for (PlayerEntity player : this.world.getPlayers()) {
			Arrays.fill(((PlayerEntityAccess) player).getMobCounts(), 0);
		}
		((ServerWorldAccess)this.world).updatePlayerMobTypeMapFromWorld();
//		for (PlayerEntity player : this.world.getPlayers()) {
//			System.out.println(player.getName().asString() + ": " + Arrays.toString(((PlayerEntityAccess) player).getMobCounts()));
//		}
	}//

	//@Shadow private SpawnHelper.Info spawnEntry;

	@Redirect (method = "tickChunks",
	           at = @At (value = "INVOKE",
	                     target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;getInt(Ljava/lang/Object;)I"))
	private int spawnCalculatedMobsInChunk(
			Object2IntMap<Integer> map, Object obj, long time, boolean b, SpawnGroup[] categories, boolean b2, int i,
			Object2IntMap map2, BlockPos pos, int j, ChunkHolder holder
	) {
		SpawnGroup category = (SpawnGroup) obj;
		TACSAccess tacs = (TACSAccess)this.threadedAnvilChunkStorage;
		PlayerMobDistanceMap mobDistanceMap = tacs.playerMobDistanceMap();
		WorldChunk chunk = holder.getEntityTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left().get();

		int minDiff = Integer.MAX_VALUE;

		// Compute minimum mobs that should be spawned between all players in range of chunk
		for (PlayerEntity entityPlayer : mobDistanceMap.getPlayersInRange(chunk.getPos())) {
			int mobCountNearPlayer = ((PlayerEntityAccess)entityPlayer).getMobCountForSpawnGroup(category);
			minDiff = Math.min(category.getCapacity() -  mobCountNearPlayer, minDiff);
		}

		int difference = (minDiff == Integer.MAX_VALUE) ? 0 : minDiff;
		if (difference > 0) {
			SpawnHelperAccess.maxSpawns.set(difference); // to pass diff to spawnEntitiesInChunk
			SpawnHelperAccess.trackEntity.set(tacs::updatePlayerMobTypeMap);
			SpawnHelper.spawnEntitiesInChunk(category, this.world, chunk, pos, (entityType, blockPos, chunkx) -> {
				return spawnEntry.test(entityType, blockPos, chunkx);
			}, (mobEntity, chunkx) -> {
				spawnEntry.run(mobEntity, chunkx);
			});
		}

		// Return max value to stop vanilla spawning if statement condition
		return Integer.MAX_VALUE;
	}//*/

	private ChunkHolder holder;

	//this is just a little lambda, it exists just fine
	@Redirect(method = "method_20801(JZLnet/minecraft/world/SpawnHelper$Info;ZILnet/minecraft/server/world/ChunkHolder;)V", at = @At (value = "INVOKE", target = "net/minecraft/server/world/ChunkHolder.getPos()Lnet/minecraft/util/math/ChunkPos;"))
	//Steal it each time just before spawning starts
	private ChunkPos stealChunkHolder(ChunkHolder chunkHolder){
		this.holder = chunkHolder;
		return chunkHolder.getPos();
	}

	public int getNOfMobsToSpawn(SpawnGroup category) {
		TACSAccess tacs = (TACSAccess)this.threadedAnvilChunkStorage;
		PlayerMobDistanceMap mobDistanceMap = tacs.playerMobDistanceMap();
		Optional<WorldChunk> optional = holder.getEntityTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left();
			if(!optional.isPresent()) return 0;
			WorldChunk chunk = optional.get();

		int minDiff = Integer.MAX_VALUE;

		// Compute minimum mobs that should be spawned between all players in range of chunk
		for (PlayerEntity entityPlayer : mobDistanceMap.getPlayersInRange(chunk.getPos())) {
			int mobCountNearPlayer = ((PlayerEntityAccess)entityPlayer).getMobCountForSpawnGroup(category);
			minDiff = Math.min(category.getCapacity() -  mobCountNearPlayer, minDiff);
		}

		//?????????????????????
		SpawnHelperAccess.maxSpawns.set((minDiff == Integer.MAX_VALUE) ? 0 : minDiff); // to pass diff to spawnEntitiesInChunk
		SpawnHelperAccess.trackEntity.set(tacs::updatePlayerMobTypeMap);
		//?????????????????????

		return (minDiff == Integer.MAX_VALUE) ? 0 : minDiff;
	}
	//parse all the good stuff down to where I moved the logic
	@Redirect(method = "tickChunks", at = @At(value = "INVOKE",
			target = "net/minecraft/world/SpawnHelper.setupSpawn (ILjava/lang/Iterable;Lnet/minecraft/world/SpawnHelper$ChunkSource;)Lnet/minecraft/world/SpawnHelper$Info;"))
	private SpawnHelper.Info insertChunkManager(int spawningChunkCount, Iterable<Entity> entities, SpawnHelper.ChunkSource chunkSource){
		SpawnHelper.Info info = SpawnHelper.setupSpawn(spawningChunkCount, entities, chunkSource);
		///////migrated////////
		TACSAccess tacs = ((TACSAccess) this.threadedAnvilChunkStorage);
		// update distance map
		tacs.playerMobDistanceMap().update(this.world.getPlayers(), tacs.renderDistance());
		// re-set mob counts
		for (PlayerEntity player : this.world.getPlayers()) {
			Arrays.fill(((PlayerEntityAccess) player).getMobCounts(), 0);
		}
		((ServerWorldAccess)this.world).updatePlayerMobTypeMapFromWorld();
		///////////////////////
		((InfoAccess)info).setChunkManager(this);
		return info;
	}

	public int Nnonspectators(){
		return this.world.getPlayers((ServerPlayerEntity) -> !ServerPlayerEntity.isSpectator()).size();
	}
}


