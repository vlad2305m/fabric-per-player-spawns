package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.TACSAccess;
import net.minecraft.server.world.ChunkLevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin (ChunkLevelManager.class)
public abstract class TACSMixin implements TACSAccess {
	@Shadow private int simulationDistance;
	public int fabric_per_player_spawns$simulationDistance() {
		return this.simulationDistance;
	}
}
