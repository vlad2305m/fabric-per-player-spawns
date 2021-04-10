package dev.lambdacraft.perplayerspawns;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.MinecraftServer;

public class Main {
	public static final int ENTITIES_CATEGORY_LENGTH = SpawnGroup.values().length;
	// non final cus clients, even though I'm pretty sure none of this works with clients anyways
	public static MinecraftServer current;
	public static int playerLowerBound = 1;
}
