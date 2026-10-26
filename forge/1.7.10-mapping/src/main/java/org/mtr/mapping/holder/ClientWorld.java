package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;
import java.util.UUID;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ClientWorld extends NativeHolder<net.minecraft.world.World> {
    public ClientWorld(net.minecraft.world.World data) { super(data); }
    public BlockState getBlockState(BlockPos pos) { return new World(data).getBlockState(pos); }
    public BlockEntity getBlockEntity(BlockPos pos) { return new World(data).getBlockEntity(pos); }
    public int getTopY(HeightMapType type,int x,int z) {
        return new World(data).getTopY(type,x,z);
    }
    public void playSoundAtBlockCenter(BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean delay) {
        data.playSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,org.mtr.mapping.tool.LegacySoundKeys.key(sound,category).toString(),volume,pitch,delay);
    }
    public int getLightLevel(LightType type, BlockPos pos) { return new World(data).getLightLevel(type,pos); }
    public PlayerEntity getPlayerByUuid(UUID id) {
        for (Object value : data.playerEntities) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) value;
            if (player.getUniqueID().equals(id)) return new PlayerEntity(player);
        }
        return null;
    }
}
