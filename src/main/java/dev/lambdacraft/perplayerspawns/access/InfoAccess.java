package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

public interface InfoAccess {
    void setChunkManager(ServerChunkManagerMixinAccess chunkManager);
    PlayerMobCountMap getPlayerMobCountMap();
    void incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup);
    boolean isAboveChunkCap(SpawnGroup group, ChunkPos chunk);
}
