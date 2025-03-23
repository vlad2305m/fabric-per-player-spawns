package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.InfoAccess;
import dev.lambdacraft.perplayerspawns.access.ServerChunkManagerMixinAccess;
import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnHelper.Info.class)
public class InnerInfoMixin implements InfoAccess {

    // My way to ensure chunk is right
    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void canSpawnInChunkDueToPerPlayerCaps(SpawnGroup group, ChunkPos chunkPos, CallbackInfoReturnable<Boolean> cir){
        if (this.isAboveChunkCap(group, chunkPos)) cir.setReturnValue(false);
    }

    private final PlayerMobCountMap playerMobCountMap = new PlayerMobCountMap();
    public PlayerMobCountMap getPlayerMobCountMap() { return this.playerMobCountMap; }
    public void incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup) { this.playerMobCountMap.incrementPlayerMobCount(playerEntity, spawnGroup); }

    //private ServerWorld world;
    private PlayerDistanceMap playerDistanceMap;
    public void setChunkManager(ServerChunkManagerMixinAccess chunkManager) {
        //this.world = chunkManager.getServerWorld();
        this.playerDistanceMap = chunkManager.fabric_per_player_spawns$getPlayerDistanceMap();
    }

    public boolean isAboveChunkCap(SpawnGroup spawnGroup, ChunkPos chunk) {
        //if ( // too lazy to add proper settings
        //        !world.getPlayers(p -> !p.isSpectator()).size() >= 2
        //) return isBelowCap(spawnGroup); else {

            // Compute if mobs should be spawned between all players in range of chunk
            int cap = spawnGroup.getCapacity();
            for (ServerPlayerEntity player : playerDistanceMap.getPlayersInRange(chunk.toLong())) {
                int mobCountNearPlayer = playerMobCountMap.getPlayerMobCount(player, spawnGroup);
                //System.out.println("!!!"+spawnGroup.getName()+" :"+mobCountNearPlayer+"/"+cap);
                if(cap <= mobCountNearPlayer) return true;
            }
            return false;

        //}
    }


    @Inject(method = "run", at = @At("HEAD"))
    private void addSpawnedMobToMap(MobEntity entity, Chunk chunk, CallbackInfo callbackInfo){
        for (ServerPlayerEntity player : playerDistanceMap.getPlayersInRange(chunk.getPos().toLong())) {
            // Increment player's sighting of entity
            incrementPlayerMobCount(player, entity.getType().getSpawnGroup());
        }
    }

}
