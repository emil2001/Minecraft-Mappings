package org.mtr.mapping.holder;

/** Implemented by hosts whose persisted state is handled by the native waterlogging engine. */
public interface Waterloggable {
    default boolean canFillWithFluid(BlockView world,BlockPos pos,BlockState state,Fluid fluid) {
        return fluid.data==net.minecraftforge.fluids.FluidRegistry.WATER && org.mtr.mapping.tool.Waterlogging.eligible(state)
            && !org.mtr.mapping.tool.Waterlogging.isWet(state);
    }
    default boolean tryFillWithFluid(World world,BlockPos pos,BlockState state,FluidState fluid) {
        return fluid.getFluid().data==net.minecraftforge.fluids.FluidRegistry.WATER && org.mtr.mapping.tool.Waterlogging.fill(world.data,pos);
    }
    default Fluid tryDrainFluid(World world,BlockPos pos,BlockState state) {
        return org.mtr.mapping.tool.Waterlogging.drain(world.data,pos)?Fluids.getWaterMapped():Fluids.getEmptyMapped();
    }
}
