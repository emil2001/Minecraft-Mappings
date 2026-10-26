package org.mtr.mapping.mapper;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.holder.*;
import java.util.List;
import java.util.function.ToIntFunction;

public interface BlockHelper {
    default void addBlockProperties(List<HolderBase<?>> properties) {}
    default void addTooltips(ItemStack stack, BlockView world, List<MutableText> tooltip, TooltipContext options) {}
    static BlockSettings createBlockSettings(boolean blockPiston,boolean forceSolid) {
        return new BlockSettings(net.minecraft.block.material.Material.iron,blockPiston);
    }
    static BlockSettings createBlockSettings(boolean blockPiston,boolean forceSolid,ToIntFunction<BlockState> luminance) {
        return createBlockSettings(blockPiston,forceSolid).luminance(luminance);
    }
}
