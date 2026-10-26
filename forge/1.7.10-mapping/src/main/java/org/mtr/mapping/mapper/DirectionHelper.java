package org.mtr.mapping.mapper;

import net.minecraftforge.common.util.ForgeDirection;
import org.mtr.mapping.holder.DirectionProperty;
import org.mtr.mapping.tool.PropertyDefinition;
import java.util.Arrays;
import java.util.Locale;

public interface DirectionHelper {
    DirectionProperty FACING = new DirectionProperty(new PropertyDefinition<>("facing",
        Arrays.asList(ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST),
        value -> value.name().toLowerCase(Locale.ROOT)));
    DirectionProperty FACING_NORMAL = new DirectionProperty(new PropertyDefinition<>("facing",
        Arrays.asList(ForgeDirection.DOWN, ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST),
        value -> value.name().toLowerCase(Locale.ROOT)));
}
