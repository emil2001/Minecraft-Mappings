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
