package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.registry.BlockEntityFactoryRegistry;
import org.mtr.mapping.tool.BlockStateTileEntity;

import static org.junit.jupiter.api.Assertions.*;

/** Native Forge factory contracts: BlockWithEntity factories may deliberately return null. */
public final class NullableTileFactoryScenarios {
    private static final BlockPos POS = new BlockPos(4, 64, 4);
    private static int nextFixture;

    public void compactNullableFactoriesMayReturnNoTileEntity() {
        NullableBlock block = new NullableBlock(false);
        BlockPlacementScenarios.registerProbe("nullable_compact_" + nextFixture++, block);
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        assertFalse(block.getStateDefinition().requiresTileEntity());
        for (int metadata = 0; metadata < 4; metadata++) {
            assertNull(block.createTileEntity(nativeWorld, metadata));
            assertEquals(new BlockPos(0, 0, 0), block.lastFactoryPos);
            assertEquals(block.getDefaultState2(), block.lastFactoryState);
        }
        World world = new World(nativeWorld);
        BlockState placed = block.getDefaultState2().with(new Property<>(DirectionHelper.FACING.data), Direction.EAST.data);
        assertTrue(world.setBlockState(POS, placed, 2));
        assertNull(nativeWorld.getTileEntity(POS.getX(), POS.getY(), POS.getZ()));
        assertEquals(placed, world.getBlockState(POS));
    }

    public void extendedNullableFactoriesKeepStateOnlyPersistence() {
        NullableBlock block = new NullableBlock(true);
        BlockPlacementScenarios.registerProbe("nullable_extended_" + nextFixture++, block);
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        assertTrue(block.getStateDefinition().requiresTileEntity());
        BlockStateTileEntity fresh = assertInstanceOf(BlockStateTileEntity.class, block.createTileEntity(nativeWorld, 0));
        assertEquals(BlockStateTileEntity.class, fresh.getClass(), "A null functional factory needs only the extended-state container");
        assertEquals(block.getDefaultState2(), fresh.readMtrState(block.getStateDefinition(), 0));
        assertFalse(fresh.canUpdate());

        World world = new World(nativeWorld);
        BlockState placed = block.getDefaultState2()
            .with(new Property<>(DirectionHelper.FACING.data), Direction.WEST.data)
            .with(new Property<>(BlockPlacementScenarios.ProbeBlock.RUNNING.data), false)
            .with(new Property<>(BlockPlacementScenarios.ProbeBlock.VARIANT.data), 5);
        assertTrue(world.setBlockState(POS, placed, 2));
        BlockStateTileEntity tile = assertInstanceOf(BlockStateTileEntity.class,
            nativeWorld.getTileEntity(POS.getX(), POS.getY(), POS.getZ()));
        assertEquals(BlockStateTileEntity.class, tile.getClass());
        assertEquals(placed, world.getBlockState(POS));

        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        BlockStateTileEntity restored = new BlockStateTileEntity();
        restored.readFromNBT(saved);
        assertEquals(placed, restored.readMtrState(block.getStateDefinition(),
            nativeWorld.getBlockMetadata(POS.getX(), POS.getY(), POS.getZ())));
    }

    public void nonNullFactoriesKeepTheirFunctionalTilesAndState() {
        for (boolean extended : new boolean[]{false, true}) {
            FunctionalBlock block = new FunctionalBlock(extended);
            String name = "nonnull_factory_" + nextFixture++;
            BlockPlacementScenarios.registerProbe(name, block);
            block.type = BlockEntityFactoryRegistry.register(new Identifier("mtr", name),
                (pos, state) -> new FunctionalTile(block, pos, state), () -> new Block(block));
            BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
            FunctionalTile fresh = assertInstanceOf(FunctionalTile.class, block.createTileEntity(nativeWorld, 0));
            assertSame(block.lastFactoryTile, fresh, "The common bridge must return the original functional factory result");
            assertSame(block.type, fresh.getType2());
            assertEquals(block.getDefaultState2(), fresh.getCachedState2());
            assertEquals(block.getDefaultState2(), fresh.readMtrState(block.getStateDefinition(), 0));

            World world = new World(nativeWorld);
            BlockState placed = block.getDefaultState2().with(new Property<>(DirectionHelper.FACING.data), Direction.SOUTH.data);
            if (extended) placed = placed.with(new Property<>(BlockPlacementScenarios.ProbeBlock.VARIANT.data), 5);
            assertTrue(world.setBlockState(POS, placed, 2));
            FunctionalTile tile = assertInstanceOf(FunctionalTile.class,
                nativeWorld.getTileEntity(POS.getX(), POS.getY(), POS.getZ()));
            assertSame(block.lastFactoryTile, tile);
            assertEquals(placed, tile.getCachedState2());
            assertEquals(placed, world.getBlockState(POS));
            tile.payload = "retained payload";
            BlockState changed = placed.with(new Property<>(DirectionHelper.FACING.data), Direction.WEST.data);
            assertTrue(world.setBlockState(POS, changed, 2));
            assertSame(tile, nativeWorld.getTileEntity(POS.getX(), POS.getY(), POS.getZ()));
            assertEquals("retained payload", tile.payload);
            assertEquals(changed, tile.getCachedState2());
        }
    }

    private static final class NullableBlock extends BlockPlacementScenarios.ProbeBlock implements BlockWithEntity {
        BlockPos lastFactoryPos;
        BlockState lastFactoryState;
        NullableBlock(boolean extended) { super(extended); }
        @Override public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) {
            lastFactoryPos = pos; lastFactoryState = state;
            return null;
        }
    }

    private static final class FunctionalBlock extends BlockPlacementScenarios.ProbeBlock implements BlockWithEntity {
        BlockEntityType<FunctionalTile> type;
        FunctionalTile lastFactoryTile;
        FunctionalBlock(boolean extended) { super(extended); }
        @Override public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) {
            return lastFactoryTile = new FunctionalTile(this, pos, state);
        }
    }

    private static final class FunctionalTile extends BlockEntityExtension {
        String payload = "";
        FunctionalTile(FunctionalBlock block, BlockPos pos, BlockState state) { super(block.type, pos, state); }
        @Override public void writeCompoundTag(CompoundTag tag) { tag.putString("payload", payload); }
        @Override public void readCompoundTag(CompoundTag tag) { payload = tag.getString("payload"); }
    }
}
