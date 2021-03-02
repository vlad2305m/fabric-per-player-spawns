package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PooledHashSets;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;

public interface PlayerEntityAccess {
	int[] getMobCounts();
	PooledHashSets.PooledObjectLinkedOpenHashSet<PlayerEntity> getDistanceMap();
	int getMobCountForSpawnGroup(SpawnGroup category);
}
