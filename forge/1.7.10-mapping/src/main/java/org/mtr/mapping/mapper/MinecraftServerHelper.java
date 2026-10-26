package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class MinecraftServerHelper {
    private MinecraftServerHelper() {}
    public static void iterateWorlds(MinecraftServer server, Consumer<ServerWorld> consumer) {
        for (net.minecraft.world.WorldServer world : server.data.worldServers) {
            if (world != null) consumer.accept(new ServerWorld(world));
        }
    }
    public static void iteratePlayers(MinecraftServer server, Consumer<ServerPlayerEntity> consumer) {
        for (Object player : server.data.getConfigurationManager().playerEntityList) {
            consumer.accept(new ServerPlayerEntity((net.minecraft.entity.player.EntityPlayerMP) player));
        }
    }
    public static void iteratePlayers(ServerWorld world, Consumer<ServerPlayerEntity> consumer) {
        iteratePlayers(world, player -> true, consumer);
    }
    public static void iteratePlayers(ServerWorld world, Predicate<ServerPlayerEntity> predicate, Consumer<ServerPlayerEntity> consumer) {
        for (Object player : world.data.playerEntities) {
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                ServerPlayerEntity holder = new ServerPlayerEntity((net.minecraft.entity.player.EntityPlayerMP) player);
                if (predicate.test(holder)) consumer.accept(holder);
            }
        }
    }
    public static Identifier getWorldId(World world) {
        int id = world.data.provider.dimensionId;
        switch (id) {
            case 0: return new Identifier("minecraft", "overworld");
            case -1: return new Identifier("minecraft", "the_nether");
            case 1: return new Identifier("minecraft", "the_end");
            default: return new Identifier("forge", "dimension_" + id);
        }
    }
}
