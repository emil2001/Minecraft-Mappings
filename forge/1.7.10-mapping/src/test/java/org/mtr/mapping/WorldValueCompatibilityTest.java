package org.mtr.mapping;

import net.minecraftforge.common.util.ForgeDirection;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.DirectionHelper;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WorldValueCompatibilityTest {
    @Test void railwayAndSignalPaletteKeepsModernMtrRgbValues() {
        MapColor[] palette={MapColor.getBlackMapped(),MapColor.getBlueMapped(),MapColor.getBrownMapped(),MapColor.getCyanMapped(),
            MapColor.getDiamondBlueMapped(),MapColor.getEmeraldGreenMapped(),MapColor.getGrayMapped(),MapColor.getGreenMapped(),
            MapColor.getIronGrayMapped(),MapColor.getLightBlueMapped(),MapColor.getLightGrayMapped(),MapColor.getLimeMapped(),
            MapColor.getMagentaMapped(),MapColor.getOakTanMapped(),MapColor.getOffWhiteMapped(),MapColor.getOrangeMapped(),
            MapColor.getPalePurpleMapped(),MapColor.getPinkMapped(),MapColor.getPurpleMapped(),MapColor.getRedMapped(),
            MapColor.getStoneGrayMapped(),MapColor.getWhiteMapped(),MapColor.getYellowMapped()};
        int[] expected={1644825,3361970,6704179,5013401,6085589,55610,5000268,6717235,10987431,6724056,10066329,8375321,
            11685080,9402184,16776437,14188339,10526975,15892389,8339378,10040115,7368816,16777215,15066419};
        assertArrayEquals(expected,Arrays.stream(palette).mapToInt(MapColor::getColorMapped).toArray());
        assertSame(net.minecraft.block.material.MapColor.woodColor,MapColor.getOakTanMapped().data);
        assertEquals(6837042,net.minecraft.block.material.MapColor.woodColor.colorValue,"Port must not mutate the native vanilla map palette");
    }
    @Test void resourceIdentifiersAllowTheModernEmptyTextureSentinelButRejectInvalidCharacters() {
        assertEquals("minecraft:",new Identifier("").toString());
        assertEquals("",new Identifier("").getPath());
        assertEquals(new Identifier("mtr","models/car.obj"),new Identifier("mtr:models/car.obj"));
        assertThrows(IllegalArgumentException.class,() -> new Identifier("mtr:bad path"));
        assertThrows(IllegalArgumentException.class,() -> new Identifier("MTR:path"));
        assertThrows(IllegalArgumentException.class,() -> new Identifier("mtr:path:extra"));
    }
    enum Side implements StringIdentifiable {
        LEFT, RIGHT, MIDDLE;
        public String asString2() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    @Test
    void blockPositionsKeepModernPackedFormatIncludingNegativeCoordinates() {
        assertEquals(-3841L, new BlockPos(-1, 255, -1).asLong());
        for (int x : new int[]{-33554432, -30000000, -1, 0, 1, 33554431})
        for (int y : new int[]{-2048, -1, 0, 255, 2047})
        for (int z : new int[]{-33554432, -1, 0, 33554431}) {
            BlockPos pos = new BlockPos(x, y, z);
            assertEquals(pos, BlockPos.fromLong(pos.asLong()));
            assertEquals(pos.hashCode(), BlockPos.fromLong(pos.asLong()).hashCode());
        }
        BlockPos origin = new BlockPos(1, 64, -5);
        assertEquals(new BlockPos(3, 66, -8), origin.east().east().up(2).north().north().north());
        assertEquals(7, origin.getManhattanDistance(new Vector3i(3, 66, -8)));
        assertEquals(new BlockPos(1, 64, -5), origin);
    }

    @Test
    void directionDataIsUsableByLegacyForgeAndMtrProperties() {
        assertEquals(ForgeDirection.NORTH, Direction.NORTH.data);
        assertEquals(Direction.NORTH, Direction.convert(ForgeDirection.NORTH));
        assertThrows(IllegalArgumentException.class, () -> Direction.convert(ForgeDirection.UNKNOWN));
        assertEquals(Direction.SOUTH, Direction.fromRotation(0));
        assertEquals(Direction.WEST, Direction.fromRotation(90));
        assertEquals(Direction.EAST, Direction.fromRotation(-90));
        for (Direction facing : Direction.values()) {
            assertEquals(facing, facing.getOpposite().getOpposite());
            BlockPos origin = new BlockPos(0, 0, 0);
            assertEquals(origin, origin.offset(facing, 3).offset(facing.getOpposite(), 3));
            if (facing.getAxis().isHorizontal()) {
                assertEquals(facing, facing.rotateYClockwise().rotateYCounterclockwise());
                assertEquals(facing, Direction.fromRotation(facing.asRotation()));
            }
        }
        assertEquals(4, new Property<>(DirectionHelper.FACING.data).getValues().size());
        assertEquals(6, new Property<>(DirectionHelper.FACING_NORMAL.data).getValues().size());
    }

    @Test
    void propertyDefinitionsPreserveFilteredDomainsAndRejectInvalidData() {
        Property<Side> sides = new Property<>(EnumProperty.of("side", Side.class, s -> s != Side.MIDDLE).data);
        assertEquals(Arrays.asList(Side.LEFT, Side.RIGHT), sides.getValues());
        assertEquals("right", sides.data.name(Side.RIGHT));
        assertEquals(Side.RIGHT, sides.data.parse("right").get());
        assertFalse(sides.data.parse("middle").isPresent());
        assertThrows(IllegalArgumentException.class, () -> sides.data.name(Side.MIDDLE));
        assertThrows(UnsupportedOperationException.class, () -> sides.getValues().clear());
        assertEquals(Arrays.asList(true, false), new Property<>(BooleanProperty.of("running").data).getValues());
        assertEquals(Arrays.asList(0, 1, 2, 3), new Property<>(IntegerProperty.of("type", 0, 3).data).getValues());
        assertThrows(IllegalArgumentException.class, () -> IntegerProperty.of("type", 3, 1));
        assertThrows(IllegalArgumentException.class, () -> BooleanProperty.of("Bad Name"));
        assertThrows(IllegalArgumentException.class, () -> EnumProperty.of("side", Side.class, s -> false));
    }
}
