package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public final class PlayerEntity extends NativeHolder<net.minecraft.entity.player.EntityPlayer> {
    public PlayerEntity(net.minecraft.entity.player.EntityPlayer data) { super(data); }
    public static PlayerEntity cast(HolderBase<?> holder) { return new PlayerEntity((net.minecraft.entity.player.EntityPlayer) holder.data); }
    public static boolean isInstance(HolderBase<?> holder) { return holder.data instanceof net.minecraft.entity.player.EntityPlayer; }
    public ItemStack getMainHandStack() { return new ItemStack(data.getHeldItem()); }
    public boolean isHolding(Item item) { return !getMainHandStack().isEmpty() && getMainHandStack().getItem().equals(item); }
    public boolean isCreative() { return data.capabilities.isCreativeMode; }
    public boolean isSneaking() { return data.isSneaking(); }
    public UUID getUuid() { return data.getUniqueID(); }
    public GameProfile getGameProfile() { return data.getGameProfile(); }
    public BlockPos getBlockPos() { return new BlockPos(net.minecraft.util.MathHelper.floor_double(data.posX),
        net.minecraft.util.MathHelper.floor_double(Entity.cast(this).getYMapped()), net.minecraft.util.MathHelper.floor_double(data.posZ)); }
    public void sendMessage(Text text, boolean actionBar) {
        if (actionBar) org.mtr.mapping.registry.LegacyPlayerMessages.sendActionBar(this, text);
        else data.addChatMessage(text.data);
    }
}
