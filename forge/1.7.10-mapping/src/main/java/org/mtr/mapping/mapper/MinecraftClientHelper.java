package org.mtr.mapping.mapper;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.mtr.mapping.holder.Entity;
import org.mtr.mapping.holder.EntityAbstractMapping;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class MinecraftClientHelper {
    private static int nextLocalId = -1;
    private MinecraftClientHelper() {}
    public static int getRenderDistance() { return Minecraft.getMinecraft().gameSettings.renderDistanceChunks; }
    public static File getResourcePackDirectory() { return Minecraft.getMinecraft().getResourcePackRepository().getDirResourcepacks(); }
    public static java.nio.file.Path getDashboardMapCacheDirectory(org.mtr.mapping.holder.World world) {
        Minecraft client = Minecraft.getMinecraft();
        if (world == null || client.theWorld == null || world.data != client.theWorld || world.data.provider == null || client.mcDataDir == null) return null;
        java.nio.file.Path base = client.mcDataDir.toPath().resolve("mtr/dashboard-map-cache/v1");
        int dimension = world.data.provider.dimensionId;
        try {
            net.minecraft.server.integrated.IntegratedServer server = client.getIntegratedServer();
            if (server != null) {
                // worldServerForDimension can initialize a world; identity must only inspect loaded worlds.
                if (server.worldServers != null) for (net.minecraft.world.WorldServer loaded : server.worldServers) {
                    if (loaded != null && loaded.provider != null && loaded.provider.dimensionId == 0) {
                        net.minecraft.world.storage.ISaveHandler saveHandler = loaded.getSaveHandler();
                        if (saveHandler == null) return null;
                        File save = saveHandler.getWorldDirectory();
                        return save == null ? null : org.mtr.mapping.tool.DashboardMapIdentity.singleplayer(base,
                            save.getCanonicalFile().toPath(), loaded.getSeed(), dimension);
                    }
                }
                return null;
            }
            net.minecraft.client.multiplayer.ServerData remote = client.func_147104_D();
            if (remote == null || remote.serverIP == null || remote.serverIP.trim().isEmpty()) return null;
            return org.mtr.mapping.tool.DashboardMapIdentity.multiplayer(base, remote.serverIP, dimension);
        } catch (java.io.IOException | SecurityException exception) {
            return null;
        }
    }
    public static void getEntities(Consumer<Entity> consumer) {
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        Set<net.minecraft.entity.Entity> entities = ReflectionHelper.getPrivateValue(WorldClient.class,world,"entityList","field_73032_d");
        for (net.minecraft.entity.Entity entity : new ArrayList<>(entities)) if (!entity.isDead) consumer.accept(new Entity(entity));
    }
    public static void addEntity(EntityAbstractMapping entity) {
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        if (entity.worldObj != world) throw new IllegalArgumentException("Client entity belongs to a different world");
        while (world.getEntityByID(nextLocalId) != null) nextLocalId = nextLocalId == Integer.MIN_VALUE ? -1 : nextLocalId-1;
        int id = nextLocalId; nextLocalId = nextLocalId == Integer.MIN_VALUE ? -1 : nextLocalId-1;
        // Native addEntityToWorld inserts into a HashSet before changing the ID used by Entity.hashCode().
        entity.setEntityId(id);
        world.addEntityToWorld(id,entity);
    }
}
