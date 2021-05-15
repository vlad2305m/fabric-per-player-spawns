package dev.lambdacraft.perplayerspawns.util;

import dev.lambdacraft.perplayerspawns.Main;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerMobCountMap {

    private final Object2ObjectOpenHashMap<ServerPlayerEntity, int[]> playerMobCounts = new Object2ObjectOpenHashMap<>();
    private static final int[] Zarray = new int[Main.ENTITIES_CATEGORY_LENGTH];

    public int getPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup) {
        return playerMobCounts.getOrDefault(playerEntity, Zarray)
                [spawnGroup.ordinal()];
    }

    public void incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup) {
        playerMobCounts.computeIfAbsent(playerEntity, k -> Zarray.clone())
                [spawnGroup.ordinal()]++;
    }

}
