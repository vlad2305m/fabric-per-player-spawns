package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.WorldChunk;

public interface InfoAccess {
    void setChunkManager(ServerChunkManagerMixinAccess chunkManager);
    PlayerMobCountMap getPlayerMobCountMap();
    void incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup);
    boolean isBelowChunkCap(SpawnGroup group, WorldChunk chunk);
}
