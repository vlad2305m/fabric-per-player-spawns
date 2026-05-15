package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;

public interface SpawnStateAccess {
    void fabric_per_player_spawns$setChunkManager(ServerChunkCacheMixinAccess chunkManager);
    PlayerMobCountMap fabric_per_player_spawns$getPlayerMobCountMap();
    void fabric_per_player_spawns$incrementPlayerMobCount(ServerPlayer playerEntity, MobCategory spawnGroup);
    boolean fabric_per_player_spawns$isAboveChunkCap(MobCategory group, ChunkPos chunk);
}
