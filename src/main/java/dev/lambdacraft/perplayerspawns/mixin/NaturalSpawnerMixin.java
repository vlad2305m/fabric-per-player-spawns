package dev.lambdacraft.perplayerspawns.mixin;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

    @Redirect(method = "getFilteredSpawningCategories", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner$SpawnState;canSpawnForCategoryGlobal(Lnet/minecraft/world/entity/MobCategory;)Z"))
    private static boolean collectAllSpawnableGroups(NaturalSpawner.SpawnState instance, MobCategory group) {
        return true; // Don't optimize out these
    }

}
