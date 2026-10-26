package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.SlabBlockExtension;
import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mapping.tool.WaterloggingFlow;
import static org.junit.jupiter.api.Assertions.*;

public final class ExtendedNativeStateScenarios {
    public void nativeExtendedMetadataSurvivesWaterReadsAndSlabChanges() {
        BlockStateDefinition nativeDefinition = BlockStateDefinition.forBlock(net.minecraft.init.Blocks.planks);
        for (int metadata : new int[]{0, 15, 16, 255, 256, 0xFEDC, 65535}) {
            BlockStateDefinition.State state = nativeDefinition.read(metadata, new NBTTagCompound());
            assertEquals(metadata, state.write(new NBTTagCompound()));
            assertEquals(metadata, state.getRawStateIndex());
        }
        assertThrows(IllegalArgumentException.class, () -> nativeDefinition.read(-1, new NBTTagCompound()));
        assertThrows(IllegalArgumentException.class, () -> nativeDefinition.read(65536, new NBTTagCompound()));
        BlockExtension mapped = new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.rock));
        assertThrows(IllegalArgumentException.class, () -> mapped.getStateDefinition().read(16, new NBTTagCompound()));

        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld() {
            @Override public int getBlockMetadata(int x, int y, int z) {
                return x == 1 && y == 64 && z == 0 ? 0xFEDC : super.getBlockMetadata(x, y, z);
            }
        };
        world.setBlock(0, 64, 0, net.minecraft.init.Blocks.flowing_water, 0, 2);
        world.setBlock(1, 64, 0, net.minecraft.init.Blocks.planks, 0, 2);
        assertEquals(0xFEDC, new BlockView(world).getBlockState(new BlockPos(1, 64, 0)).data.write(new NBTTagCompound()));
        WaterloggingFlow.enter(world, 0, 64, 0);
        try { assertFalse(WaterloggingFlow.blocked(world, 1, 64, 0)); }
        finally { WaterloggingFlow.leave(); }

        BlockState slab = new BlockState(BlockStateDefinition.forBlock(net.minecraft.init.Blocks.stone_slab).read(0xFED5, new NBTTagCompound()));
        assertEquals(0xFEDD, slab.with(new Property<>(SlabBlockExtension.TYPE), SlabType.TOP).data.write(new NBTTagCompound()));
        assertEquals(0xFED5, slab.with(new Property<>(SlabBlockExtension.TYPE), SlabType.DOUBLE).data.write(new NBTTagCompound()));
    }
}
