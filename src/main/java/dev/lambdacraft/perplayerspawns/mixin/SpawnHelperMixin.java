package dev.lambdacraft.perplayerspawns.mixin;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {

    @Redirect(method = "Lnet/minecraft/world/SpawnHelper;collectSpawnableGroups(Lnet/minecraft/world/SpawnHelper$Info;ZZZ)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper$Info;isBelowCap(Lnet/minecraft/entity/SpawnGroup;)Z"))
    private static boolean collectAllSpawnableGroups(SpawnHelper.Info instance, SpawnGroup group){
        return true; // Don't optimize out these
    }

}
