package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EntityHelper {
    public static final List<UUID> HIDDEN_PLAYERS=new ArrayList<>();
    private EntityHelper() {}
    public static float getPitch(Entity entity) { return entity.data.rotationPitch; }
    public static float getYaw(Entity entity) { return entity.data.rotationYaw; }
    public static void setPitch(Entity entity,float pitch) { entity.data.rotationPitch=pitch; }
    public static void setYaw(Entity entity,float yaw) { entity.data.rotationYaw=yaw; }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static double renderY(net.minecraft.entity.player.EntityPlayer player,double y) {
        // MTR cancels this offset with +1000 when drawing a passenger in the vehicle's transformed scene.
        if (!HIDDEN_PLAYERS.contains(player.getUniqueID())) return y;
        // Upstream replaces the ordinary sneak offset, while legacy RenderPlayer adds it separately.
        return y-1000+(player.isSneaking() && !(player instanceof net.minecraft.client.entity.EntityPlayerSP)?.125:0);
    }
    public static void spawnItem(ServerWorld world,BlockPos pos,ItemStack item) {
        if (!item.isEmpty()) world.data.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(world.data,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,item.data));
    }
}
