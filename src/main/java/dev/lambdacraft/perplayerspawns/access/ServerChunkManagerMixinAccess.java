package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerMobDistanceMap;
import net.minecraft.server.world.ServerWorld;

public interface ServerChunkManagerMixinAccess {
    ServerWorld getServerWorld();
    PlayerMobDistanceMap getPlayerDistanceMap();
}
