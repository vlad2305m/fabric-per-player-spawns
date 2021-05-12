package dev.lambdacraft.perplayerspawns.access;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.World;

public interface ServerChunkManagerMixinAccess {
    boolean method_20727(Entity entity);
    int getNOfMobsToSpawn(SpawnGroup category);
    int Nnonspectators();
    World getWorld();
    TACSAccess getTACS();
}
