package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;
import java.util.UUID;

public final class ServerPlayerEntity extends NativeHolder<net.minecraft.entity.player.EntityPlayerMP> {
    public ServerPlayerEntity(net.minecraft.entity.player.EntityPlayerMP data) { super(data); }
    public static ServerPlayerEntity cast(HolderBase<?> holder) { return new ServerPlayerEntity((net.minecraft.entity.player.EntityPlayerMP) holder.data); }
    public static boolean isInstance(HolderBase<?> holder) { return holder.data instanceof net.minecraft.entity.player.EntityPlayerMP; }
    public MinecraftServer getServerMapped() { return new MinecraftServer(data.mcServer); }
    public ServerWorld getServerWorld() { return new ServerWorld((net.minecraft.world.WorldServer) data.worldObj); }
    public World getEntityWorld() { return new World(data.worldObj); }
    public UUID getUuid() { return data.getUniqueID(); }
    public Text getName() { return new Text(new net.minecraft.util.ChatComponentText(data.getCommandSenderName())); }
    public BlockPos getBlockPos() { return new PlayerEntity(data).getBlockPos(); }
    public void sendMessage(Text text, boolean actionBar) { new PlayerEntity(data).sendMessage(text, actionBar); }
    public void setFallDistanceMapped(float value) { data.fallDistance = value; }
    public void setNoClipMapped(boolean value) { data.noClip = value && org.mtr.mapping.tool.PlayerPhysics.authorized(data); }
    public void setNoGravity(boolean value) { org.mtr.mapping.tool.PlayerPhysics.setNoGravity(data,value); }
    public void setInTeleportationState(boolean value) { org.mtr.mapping.tool.PlayerPhysics.setTeleporting(data,value); }
}
