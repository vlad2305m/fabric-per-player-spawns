package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.InfoAccess;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {

    // My way to ensure chunk is right
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "spawn", at = @At(value = "INVOKE", target = "net/minecraft/world/SpawnHelper$Info.method_27829 (Lnet/minecraft/world/SpawnHelper$Info;Lnet/minecraft/entity/SpawnGroup;)Z"))
    private static boolean isBelowChunkCap(SpawnHelper.Info info, SpawnGroup spawnGroup, ServerWorld world, WorldChunk chunk){
        return ((InfoAccess)info).isBelowChunkCap(spawnGroup, chunk);
    }

}
