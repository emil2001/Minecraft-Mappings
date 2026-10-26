package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;
import java.util.List;

public interface ItemHelper {
    default void addTooltips(ItemStack stack, World world, List<MutableText> tooltip, TooltipContext options) {}
}
