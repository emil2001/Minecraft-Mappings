package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.BlockSnapshot;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.registry.BlockEntityFactoryRegistry;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class WorldMetadataScenarios {
    private static final BlockPos BUTTON = new BlockPos(4, 64, 4), FLOOR = new BlockPos(4, 64, 7);

    public void compactFunctionalTileKeepsPayloadAcrossMetadataChanges() {
        Fixture fixture = new Fixture(new BlockPlacementScenarios.MemoryWorld());
        assertFalse(fixture.block.getStateDefinition().requiresTileEntity(), "Compact native state still owns a functional tile");
        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.NORTH}) {
            BlockState state = fixture.world.getBlockState(BUTTON).with(new Property<>(DirectionHelper.FACING.data), facing.data);
            assertTrue(fixture.world.setBlockState(BUTTON, state));
            assertEquals(facing.data, fixture.world.getBlockState(BUTTON).get(new Property<>(DirectionHelper.FACING.data)));
            assertSame(fixture.tile, fixture.nativeWorld.getTileEntity(4, 64, 4), "Metadata updates must keep the functional tile and its payload");
            assertLinks(fixture.tile);
            NBTTagCompound saved = new NBTTagCompound();
            fixture.tile.writeToNBT(saved);
            assertArrayEquals(new long[]{FLOOR.asLong()}, new CompoundTag(saved).getLongArray("track_floor_pos"));
        }
    }

    public void compactStateUpdatesKeepFlagsSnapshotsAndNoOpSemantics() {
        TrackingWorld nativeWorld = new TrackingWorld();
        Fixture fixture = new Fixture(nativeWorld);
        nativeWorld.flags.clear(); nativeWorld.neighborNotifications = 0;
        BlockState unlocked = fixture.world.getBlockState(BUTTON);
        BlockState locked = unlocked.with(new Property<>(DirectionHelper.FACING.data), Direction.SOUTH.data);
        assertTrue(fixture.world.setBlockState(BUTTON, locked, 2));
        assertEquals(Collections.singletonList(2), nativeWorld.flags);
        assertEquals(0, nativeWorld.neighborNotifications);
        assertTrue(fixture.world.setBlockState(BUTTON, unlocked, 3));
        assertEquals(Arrays.asList(2, 3), nativeWorld.flags);
        assertEquals(1, nativeWorld.neighborNotifications);
        assertSame(fixture.tile, nativeWorld.getTileEntity(4, 64, 4)); assertLinks(fixture.tile);
        int metadataBefore = nativeWorld.getBlockMetadata(4, 64, 4);
        nativeWorld.captureBlockSnapshots = true;
        try {
            assertTrue(fixture.world.setBlockState(BUTTON, locked, 3));
            assertEquals(1, nativeWorld.capturedBlockSnapshots.size());
            BlockSnapshot snapshot = nativeWorld.capturedBlockSnapshots.get(0);
            assertEquals(3, snapshot.flag); assertEquals(metadataBefore, snapshot.meta);
            assertLinks(assertInstanceOf(PayloadTile.class, snapshot.getTileEntity()));
            assertEquals(Arrays.asList(2, 3), nativeWorld.flags, "Capture must suppress native client and neighbor notification");
            assertEquals(1, nativeWorld.neighborNotifications);
            assertSame(fixture.tile, nativeWorld.getTileEntity(4, 64, 4)); assertLinks(fixture.tile);
            assertFalse(fixture.world.setBlockState(BUTTON, locked, 3));
            assertEquals(1, nativeWorld.capturedBlockSnapshots.size(), "No-op must not add a snapshot");
        } finally { nativeWorld.captureBlockSnapshots = false; }
    }

    public void replacingCompactFunctionalBlocksStillRemovesOldTileData() {
        Fixture fixture = new Fixture(new BlockPlacementScenarios.MemoryWorld());
        assertTrue(fixture.world.setBlockState(BUTTON, new Block(net.minecraft.init.Blocks.stone).getDefaultState(), 3));
        assertSame(net.minecraft.init.Blocks.stone, fixture.nativeWorld.getBlock(4, 64, 4));
        assertNull(fixture.nativeWorld.getTileEntity(4, 64, 4)); assertTrue(fixture.tile.isInvalid());
        assertTrue(fixture.world.setBlockState(BUTTON, fixture.block.getDefaultState2(), 3));
        PayloadTile replacement = assertInstanceOf(PayloadTile.class, fixture.nativeWorld.getTileEntity(4, 64, 4));
        assertNotSame(fixture.tile, replacement);
        assertEquals(0, replacement.links.length, "A real replacement must not inherit the removed payload");
    }

    private static void assertLinks(PayloadTile tile) {
        assertArrayEquals(new long[]{FLOOR.asLong()}, tile.links);
    }

    private static final class Fixture {
        final BlockPlacementScenarios.MemoryWorld nativeWorld;
        final World world;
        final CompactFunctionalBlock block;
        final PayloadTile tile;
        Fixture(BlockPlacementScenarios.MemoryWorld nativeWorld) {
            this.nativeWorld = nativeWorld; world = new World(nativeWorld);
            block = new CompactFunctionalBlock();
            String name = "test_compact_payload_" + nextFixture++;
            BlockPlacementScenarios.registerProbe(name, block);
            block.type = BlockEntityFactoryRegistry.register(new Identifier("mtr", name),
                (pos, state) -> new PayloadTile(block, pos, state), () -> new Block(block));
            assertTrue(world.setBlockState(BUTTON, block.getDefaultState2(), 2));
            tile = assertInstanceOf(PayloadTile.class, nativeWorld.getTileEntity(4, 64, 4));
            tile.links = new long[]{FLOOR.asLong()}; tile.markDirty2(); assertLinks(tile);
        }
    }

    private static int nextFixture;
    private static final class CompactFunctionalBlock extends BlockPlacementScenarios.ProbeBlock implements BlockWithEntity {
        BlockEntityType<PayloadTile> type;
        CompactFunctionalBlock() { super(false); }
        @Override public PayloadTile createBlockEntity(BlockPos pos, BlockState state) { return new PayloadTile(this, pos, state); }
    }
    private static final class PayloadTile extends BlockEntityExtension {
        long[] links = new long[0];
        PayloadTile(CompactFunctionalBlock block, BlockPos pos, BlockState state) { super(block.type, pos, state); }
        @Override public void writeCompoundTag(CompoundTag tag) { tag.putLongArray("track_floor_pos", links); }
        @Override public void readCompoundTag(CompoundTag tag) { links = tag.getLongArray("track_floor_pos"); }
    }

    private static final class TrackingWorld extends BlockPlacementScenarios.MemoryWorld {
        final List<Integer> flags = new ArrayList<>();
        int neighborNotifications;
        @Override public void markAndNotifyBlock(int x, int y, int z, Chunk chunk, net.minecraft.block.Block oldBlock, net.minecraft.block.Block newBlock, int flag) {
            flags.add(flag); super.markAndNotifyBlock(x, y, z, chunk, oldBlock, newBlock, flag);
        }
        @Override public void notifyBlockChange(int x, int y, int z, net.minecraft.block.Block oldBlock) {
            neighborNotifications++; super.notifyBlockChange(x, y, z, oldBlock);
        }
    }
}
