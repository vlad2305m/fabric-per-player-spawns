package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
//import net.minecraft.server.world.ServerWorld;

public interface ServerChunkManagerMixinAccess {
    //ServerWorld getServerWorld();
    PlayerDistanceMap fabric_per_player_spawns$getPlayerDistanceMap();
}
