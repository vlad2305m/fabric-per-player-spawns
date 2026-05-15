package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.DistanceManagerAccess;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin (DistanceManager.class)
public abstract class DistanceManagerMixin implements DistanceManagerAccess {
	@Shadow private int simulationDistance;
	public int fabric_per_player_spawns$simulationDistance() {
		return this.simulationDistance;
	}
}
