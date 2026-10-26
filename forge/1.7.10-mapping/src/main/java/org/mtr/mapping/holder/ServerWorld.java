package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;
import java.util.UUID;

public final class ServerWorld extends NativeHolder<net.minecraft.world.WorldServer> {
    public ServerWorld(net.minecraft.world.WorldServer data) { super(data); }
    public static ServerWorld cast(HolderBase<?> holder) { return new ServerWorld((net.minecraft.world.WorldServer) holder.data); }
    public MinecraftServer getServer() { return new MinecraftServer(data.func_73046_m()); }
    public BlockState getBlockState(BlockPos pos) { return new World(data).getBlockState(pos); }
    public BlockEntity getBlockEntity(BlockPos pos) { return new World(data).getBlockEntity(pos); }
    public boolean setBlockState(BlockPos pos, BlockState state) { return new World(data).setBlockState(pos, state); }
    public void playSound(PlayerEntity excluded, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        new World(data).playSound(excluded,pos,sound,category,volume,pitch);
    }
    public PlayerEntity getPlayerByUuid(UUID id) {
        for (Object player : data.playerEntities) {
            net.minecraft.entity.player.EntityPlayer nativePlayer = (net.minecraft.entity.player.EntityPlayer) player;
            if (nativePlayer.getUniqueID().equals(id)) return new PlayerEntity(nativePlayer);
        }
        return null;
    }
}
