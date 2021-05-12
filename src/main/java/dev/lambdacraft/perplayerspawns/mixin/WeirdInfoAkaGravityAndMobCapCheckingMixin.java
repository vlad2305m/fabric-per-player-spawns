package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.InfoAccess;
import dev.lambdacraft.perplayerspawns.access.ServerChunkManagerMixinAccess;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.logging.Logger;

@Mixin(SpawnHelper.Info.class)
public class WeirdInfoAkaGravityAndMobCapCheckingMixin implements InfoAccess {

    private ServerChunkManagerMixinAccess chunkManager = null;

    public void setChunkManager(ServerChunkManagerMixinAccess chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Inject(method = "isBelowCap", at = @At("HEAD"), cancellable = true)
    private void isBelowPlayerCap(SpawnGroup group, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (
                true
                //&& chunkManager.Nnonspectators() >= 0
                //&& chunkManager.getWorld().getDimension().isBedWorking()
        ) callbackInfoReturnable.setReturnValue(chunkManager.getNOfMobsToSpawn(group) > 0);
    }

    private boolean warned = false;
    @Inject(method = "run", at = @At("HEAD"))
    private void addSpawnedMobToMap(MobEntity entity, Chunk chunk, CallbackInfo callbackInfo){
        if (chunkManager != null) {
            chunkManager.getTACS().updatePlayerMobTypeMap(entity);
        }
        else if (!warned) {
            Logger.getLogger("Fabric Per Player Spawns").warning("Spawned mob from elsewhere. Are you using spawners?");
            warned = true;
        }
    }

}
