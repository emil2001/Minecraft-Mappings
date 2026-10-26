package org.mtr.mapping.holder;

public final class Fluids {
    private Fluids() {}
    public static Fluid getEmptyMapped() { return new Fluid(null); }
    public static Fluid getWaterMapped() { return new Fluid(net.minecraftforge.fluids.FluidRegistry.WATER); }
    public static Fluid getLavaMapped() { return new Fluid(net.minecraftforge.fluids.FluidRegistry.LAVA); }
}
