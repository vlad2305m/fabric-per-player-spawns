package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.TACSAccess;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin (ChunkTicketManager.class)
public abstract class TACSMixin implements TACSAccess {
	@Shadow private int simulationDistance;
	public int simulationDistance() {
		return this.simulationDistance;
	}
}
