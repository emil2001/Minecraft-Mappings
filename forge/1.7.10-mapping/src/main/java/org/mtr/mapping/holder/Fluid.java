package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

/** Fluid identity is the registered Forge fluid; source/flowing belongs to FluidState. */
public final class Fluid extends NativeHolder<net.minecraftforge.fluids.Fluid> {
    public Fluid(net.minecraftforge.fluids.Fluid data) { super(data); }
    public static Fluid cast(HolderBase<?> holder) { return new Fluid((net.minecraftforge.fluids.Fluid)holder.data); }
    public FluidState getStill(boolean falling) {
        if (data==null) return FluidState.empty();
        net.minecraft.block.Block block;
        if (data==net.minecraftforge.fluids.FluidRegistry.WATER) block=net.minecraft.init.Blocks.water;
        else if (data==net.minecraftforge.fluids.FluidRegistry.LAVA) block=net.minecraft.init.Blocks.lava;
        else {
            block=data.getBlock();
            if (!(block instanceof net.minecraftforge.fluids.BlockFluidClassic) || falling)
                throw new UnsupportedOperationException("This Forge fluid has no known infinite-source state");
        }
        return FluidState.fromBlockState(new BlockState(org.mtr.mapping.tool.BlockStateDefinition.forBlock(block)
            .read(falling?8:0,new net.minecraft.nbt.NBTTagCompound())));
    }
}
