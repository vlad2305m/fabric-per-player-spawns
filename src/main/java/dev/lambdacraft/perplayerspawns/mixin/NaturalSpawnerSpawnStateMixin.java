package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.SpawnStateAccess;
import dev.lambdacraft.perplayerspawns.access.ServerChunkCacheMixinAccess;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.SpawnState.class)
public class NaturalSpawnerSpawnStateMixin implements SpawnStateAccess {

    // My way to ensure chunk is right
    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void canSpawnInChunkDueToPerPlayerCaps(EntityType<?> type, Level level, BlockPos testPos, ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir){
        if (this.fabric_per_player_spawns$isAboveChunkCap(type.getCategory(), ChunkPos.containing(testPos))) cir.setReturnValue(false);
    }

    @Unique
    private final PlayerMobCountMap playerMobCountMap = new PlayerMobCountMap();
    public PlayerMobCountMap fabric_per_player_spawns$getPlayerMobCountMap() { return this.playerMobCountMap; }
    @Unique
    public void fabric_per_player_spawns$incrementPlayerMobCount(ServerPlayer playerEntity, MobCategory spawnGroup) { this.playerMobCountMap.incrementPlayerMobCount(playerEntity, spawnGroup); }

    //private ServerWorld world;
    @Unique
    private PlayerDistanceMap playerDistanceMap;
    public void fabric_per_player_spawns$setChunkManager(ServerChunkCacheMixinAccess chunkManager) {
        //this.world = chunkManager.getServerWorld();
        this.playerDistanceMap = chunkManager.fabric_per_player_spawns$getPlayerDistanceMap();
    }

    public boolean fabric_per_player_spawns$isAboveChunkCap(MobCategory spawnGroup, ChunkPos chunk) {
        //if (// too lazy to add proper settings
        //        !world.getPlayers(p -> !p.isSpectator()).size() >= 2
        //) return isBelowCap(spawnGroup); else {

            // Compute if mobs should be spawned between all players in range of chunk
            int cap = spawnGroup.getMaxInstancesPerChunk();
            for (ServerPlayer player : playerDistanceMap.getPlayersInRange(chunk.pack())) {
                int mobCountNearPlayer = playerMobCountMap.getPlayerMobCount(player, spawnGroup);
                //System.out.println("!!!"+spawnGroup.getName()+" :"+mobCountNearPlayer+"/"+cap);
                if(cap <= mobCountNearPlayer) return true;
            }
            return false;

        //}
    }


    @Inject(method = "afterSpawn", at = @At("HEAD"))
    private void addSpawnedMobToMap(Mob entity, ChunkAccess chunk, CallbackInfo callbackInfo){
        for (ServerPlayer player : playerDistanceMap.getPlayersInRange(chunk.getPos().pack())) {
            // Increment player's sighting of entity
            fabric_per_player_spawns$incrementPlayerMobCount(player, entity.getType().getCategory());
        }
    }

}
