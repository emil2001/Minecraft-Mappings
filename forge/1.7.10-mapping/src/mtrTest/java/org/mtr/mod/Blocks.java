package org.mtr.mod;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.*;
import org.mtr.mod.block.*;

/** Test-only registration table for the unchanged upstream escalator classes. */
public final class Blocks {
    public static final Registry REGISTRY = new Registry();
    public static final BlockRegistryObject ESCALATOR_STEP = REGISTRY.registerBlock(new Identifier("mtr", "escalator_step"), () -> new Block(new BlockEscalatorStep()));
    public static final BlockRegistryObject ESCALATOR_SIDE = REGISTRY.registerBlock(new Identifier("mtr", "escalator_side"), () -> new Block(new BlockEscalatorSide()));
    public static BlockSettings createDefaultBlockSettings(boolean metal) { return new BlockSettings(net.minecraft.block.material.Material.iron); }
    public static BlockSettings createDefaultBlockSettings(boolean blockPiston, java.util.function.ToIntFunction<BlockState> luminance) { return org.mtr.mapping.mapper.BlockHelper.createBlockSettings(blockPiston,true,luminance).strength(3); }
    public static void init() { Items.init(); REGISTRY.init(); }
}
