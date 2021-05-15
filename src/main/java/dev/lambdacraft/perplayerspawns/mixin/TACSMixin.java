package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.TACSAccess;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin (ThreadedAnvilChunkStorage.class)
public abstract class TACSMixin implements TACSAccess {
	@Shadow private int watchDistance;
	public int renderDistance() {
		return this.watchDistance;
	}
}
