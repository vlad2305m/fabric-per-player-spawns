package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.InfoAccess;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnHelper.Info.class)
public class SpawnHelperMixin {

    // My way to ensure chunk is right
    @Redirect(method = "isBelowCap", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;getInt(Ljava/lang/Object;)I", remap = false))
    private int isBelowChunkCap0(Object2IntOpenHashMap instance, Object k, SpawnGroup group, ChunkPos pos){
        return group.isRare() ? instance.getInt(k) : ((InfoAccess)(Object)this).isBelowChunkCap(group, pos);
    }

}
