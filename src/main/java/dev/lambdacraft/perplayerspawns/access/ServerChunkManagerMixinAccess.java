package dev.lambdacraft.perplayerspawns.access;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;

public interface ServerChunkManagerMixinAccess {
    boolean method_20727(Entity entity);
    int getNOfMobsToSpawn(SpawnGroup category);
    int Nnonspectators();
}
