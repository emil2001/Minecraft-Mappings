package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

public final class WorldAccess extends NativeHolder<net.minecraft.world.World> {
    public WorldAccess(net.minecraft.world.World data) { super(data); }
    public static WorldAccess cast(HolderBase<?> holder) { return new WorldAccess((net.minecraft.world.World) holder.data); }
    public BlockState getBlockState(BlockPos pos) { return new BlockView(data).getBlockState(pos); }
    public boolean setBlockState(BlockPos pos, BlockState state, int flags) { return new World(data).setBlockState(pos, state, flags); }
}
