package dev.lambdacraft.perplayerspawns.util;

import dev.lambdacraft.perplayerspawns.Main;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;

public class PlayerMobCountMap {

    private final Object2ObjectOpenHashMap<ServerPlayer, int[]> playerMobCounts = new Object2ObjectOpenHashMap<>();
    private static final int[] Zarray = new int[Main.ENTITIES_CATEGORY_LENGTH];

    public int getPlayerMobCount(ServerPlayer playerEntity, MobCategory spawnGroup) {
        return playerMobCounts.getOrDefault(playerEntity, Zarray)
                [spawnGroup.ordinal()];
    }

    public void incrementPlayerMobCount(ServerPlayer playerEntity, MobCategory spawnGroup) {
        playerMobCounts.computeIfAbsent(playerEntity, k -> Zarray.clone())
                [spawnGroup.ordinal()]++;
    }

}
