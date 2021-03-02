package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.InfoAccess;
import dev.lambdacraft.perplayerspawns.access.ServerChunkManagerMixinAccess;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnHelper.Info.class)
public class WeirdInfoAkaGravityAndMobCapCheckingMixin implements InfoAccess {

    private ServerChunkManagerMixinAccess chunkManager;

    public void setChunkManager(ServerChunkManagerMixinAccess chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Inject(method = "isBelowCap", at = @At("HEAD"), cancellable = true)
    private void isBelowPlayerCap(SpawnGroup group, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(chunkManager.getNOfMobsToSpawn(group) > 0);
    }
}
