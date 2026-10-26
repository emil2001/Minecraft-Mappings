package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;
import java.util.UUID;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ClientPlayerEntity extends NativeHolder<net.minecraft.entity.player.EntityPlayer> {
    public ClientPlayerEntity(net.minecraft.entity.player.EntityPlayer data) { super(data); }
    public UUID getUuid() { return data.getUniqueID(); }
    public double getX() { return data.posX; }
    public double getY() { return Entity.cast(this).getYMapped(); }
    public double getZ() { return data.posZ; }
    public Vector3d getPos() { return new Vector3d(getX(), getY(), getZ()); }
    public BlockPos getBlockPos() { return new PlayerEntity(data).getBlockPos(); }
    public float getForwardSpeedMapped() { return data.moveForward; }
    public float getSidewaysSpeedMapped() { return data.moveStrafing; }
    public boolean isHolding(Item item) { return new PlayerEntity(data).isHolding(item); }
    public boolean isSneaking() { return data.isSneaking(); }
    public boolean isSprinting() { return data.isSprinting(); }
    public ItemStack getMainHandStack() { return new ItemStack(data.getHeldItem()); }
    // Minecraft 1.7.10 has one item hand and no spectator game mode.
    public ItemStack getOffHandStack() { return ItemStack.getEmptyMapped(); }
    public Hand getActiveHand() { return Hand.MAIN_HAND; }
    public boolean isSpectator() { return false; }
    public ItemStack getStackInHand(Hand hand) { return hand == Hand.MAIN_HAND ? getMainHandStack() : getOffHandStack(); }
    public void setFallDistanceMapped(float value) { data.fallDistance = value; }
    public void playSound(SoundEvent sound, float volume, float pitch) {
        data.playSound(org.mtr.mapping.tool.LegacySoundKeys.key(sound,SoundCategory.BLOCKS).toString(),volume,pitch);
    }
    public void setVelocity(double x, double y, double z) { data.setVelocity(x, y, z); }
    public void setMovementSpeed(float value) { data.setAIMoveSpeed(value); }
    // Native posY includes eye offset and sneak smoothing; MTR positions refer to feet.
    public void updatePosition(double x, double y, double z) { data.setPosition(x, y + data.yOffset - data.ySize, z); }
    public void sendMessage(Text text, boolean actionBar) {
        if (actionBar) net.minecraft.client.Minecraft.getMinecraft().ingameGUI.func_110326_a(text.data.getFormattedText(), false);
        else data.addChatMessage(text.data);
    }
}
