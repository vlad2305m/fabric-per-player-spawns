package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

public interface InfoAccess {
    void fabric_per_player_spawns$setChunkManager(ServerChunkManagerMixinAccess chunkManager);
    PlayerMobCountMap fabric_per_player_spawns$getPlayerMobCountMap();
    void fabric_per_player_spawns$incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup);
    boolean fabric_per_player_spawns$isAboveChunkCap(SpawnGroup group, ChunkPos chunk);
}
