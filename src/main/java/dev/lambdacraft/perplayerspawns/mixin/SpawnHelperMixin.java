package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.SpawnHelperAccess;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {
	@Redirect(
			method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;getLimitPerChunk()I"))
	private static int getLimitPerChunk(MobEntity entity) {
		SpawnHelperAccess.trackEntity.get().accept(entity);

		// need to exit in spawnEntitiesInChunk if (spawnCountInChunk >= thisReturnValue)
		return Math.min(SpawnHelperAccess.maxSpawns.get(), entity.getLimitPerChunk());
	}
}
