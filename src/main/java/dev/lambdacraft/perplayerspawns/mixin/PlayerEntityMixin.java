package dev.lambdacraft.perplayerspawns.mixin;

import com.mojang.authlib.GameProfile;
import dev.lambdacraft.perplayerspawns.Main;
import dev.lambdacraft.perplayerspawns.util.PooledHashSets;
import dev.lambdacraft.perplayerspawns.access.PlayerEntityAccess;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements PlayerEntityAccess {
	private int[] mobCounts;
	private PooledHashSets.PooledObjectLinkedOpenHashSet<PlayerEntity> cachedSingleMobDistanceMap;
	@Inject(method = "<init>", at = @At("RETURN"))
	private void cachedSingleMobDistanceMap(World world, BlockPos pos, float yaw, GameProfile profile, CallbackInfo ci) {
		this.mobCounts = new int[Main.ENTITIES_CATEGORY_LENGTH];
		this.cachedSingleMobDistanceMap = new PooledHashSets.PooledObjectLinkedOpenHashSet<>((PlayerEntity)(Object) this);
	}

	@Override
	public final int[] getMobCounts() {
		return this.mobCounts;
	}

	@Override
	public final int getMobCountForSpawnGroup(SpawnGroup category) {
		return this.mobCounts[category.ordinal()];
	}

	@Override
	public final PooledHashSets.PooledObjectLinkedOpenHashSet<PlayerEntity> getDistanceMap() {
		return this.cachedSingleMobDistanceMap;
	}
}
