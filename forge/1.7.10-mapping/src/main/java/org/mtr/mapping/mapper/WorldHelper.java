package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.holder.World;

public final class WorldHelper {
    private WorldHelper() {}
    public static long getTimeOfDay(ServerWorld world) { return world.data.getWorldTime(); }
    public static long getTimeOfDay(World world) { return world.data.getWorldTime(); }
}
