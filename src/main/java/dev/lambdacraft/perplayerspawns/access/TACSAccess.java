package dev.lambdacraft.perplayerspawns.access;

import dev.lambdacraft.perplayerspawns.util.PlayerMobDistanceMap;
import net.minecraft.entity.Entity;

public interface TACSAccess {
	void updatePlayerMobTypeMap(Entity entity);
	PlayerMobDistanceMap playerMobDistanceMap();
	int renderDistance();
}
