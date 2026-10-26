package org.mtr.mapping;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.storage.SaveHandlerMP;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.registry.BlockEntityFactoryRegistry;
import org.mtr.mapping.tool.BlockStateTileEntity;
import org.mtr.mapping.tool.HolderBase;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BlockPlacementScenarios {
    public void neighborUpdatesKeepTheirSourceDirectionAndLegacyCallbacks() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        DirectionalBlock block = new DirectionalBlock();
        registerProbe("test_directional_neighbor", block);
        world.setBlockState(POS, block.getDefaultState2(), 2);
        world.setBlockState(POS.offset(Direction.EAST), new Block(net.minecraft.init.Blocks.stone).getDefaultState());
        assertEquals(Collections.singletonList(Direction.EAST), block.directions);
        assertEquals(1, block.legacyCalls);
        assertTrue(world.getBlockState(POS).isOf(new Block(block)), "Unchanged upper neighbor must not be dispatched");
        nativeWorld.notifyBlocksOfNeighborChange(POS.getX(), POS.getY() + 1, POS.getZ(), net.minecraft.init.Blocks.stone, 0);
        assertEquals(1, block.directions.size(), "Excluded side must not receive a callback");
        world.setBlockState(POS.up(), new Block(net.minecraft.init.Blocks.stone).getDefaultState());
        assertEquals(Direction.UP, block.directions.get(1));
        world.setBlockState(POS.up(), new Block(net.minecraft.init.Blocks.air).getDefaultState());
        assertTrue(world.getBlockState(POS).isOf(new Block(net.minecraft.init.Blocks.air)));
    }

    static class DirectionalBlock extends BlockExtension {
        final List<Direction> directions = new ArrayList<>();
        int legacyCalls;
        DirectionalBlock() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState,
                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
            directions.add(direction);
            assertEquals(pos.offset(direction), neighborPos);
            return direction == Direction.UP && neighborState.isOf(new Block(net.minecraft.init.Blocks.air))
                ? new Block(net.minecraft.init.Blocks.air).getDefaultState() : state;
        }
        @Override public void onNeighborBlockChange(net.minecraft.world.World world, int x, int y, int z, net.minecraft.block.Block source) {
            legacyCalls++;
        }
    }

    static ProbeBlock extended;
    static ProbeBlock compact;
    static FunctionalBlock functional;
    static StairBlock stairs;
    static BlockEntityType<LiftSettingsTile> liftType;
    static final BlockPos POS = new BlockPos(4, 64, 4);

    public static void bootstrap() {
        cpw.mods.fml.common.Loader.injectData("7", "99", "40", "1614", "1.7.10", "9.05", new java.io.File("."), Collections.emptyList());
        Bootstrap.func_151354_b();
        initializeModContext();
        TileEntity.addMapping(BlockStateTileEntity.class, "mtr:test_state");
        extended = new ProbeBlock(true);
        compact = new ProbeBlock(false);
        registerProbe("test_extended", extended);
        registerProbe("test_compact", compact);
        functional = new FunctionalBlock();
        registerProbe("test_lift", functional);
        liftType = BlockEntityFactoryRegistry.register(new Identifier("mtr", "test_lift"), LiftSettingsTile::new, () -> new Block(functional));
        stairs = new StairBlock();
        registerProbe("test_stairs", stairs);
    }

    static void initializeModContext() {
        try {
            cpw.mods.fml.common.Loader loader = cpw.mods.fml.common.Loader.instance();
            cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(cpw.mods.fml.common.Loader.class,loader,
                new java.util.HashMap<String,cpw.mods.fml.common.ModContainer>(),"namedMods");
            cpw.mods.fml.common.LoadController controller = new cpw.mods.fml.common.LoadController(loader);
            java.lang.reflect.Field active = cpw.mods.fml.common.LoadController.class.getDeclaredField("activeContainer");
            active.setAccessible(true);
            cpw.mods.fml.common.ModMetadata metadata = new cpw.mods.fml.common.ModMetadata();
            metadata.modId = "mtr"; metadata.name = "MTR native integration tests";
            active.set(controller, new cpw.mods.fml.common.DummyModContainer(metadata));
            java.lang.reflect.Field control = cpw.mods.fml.common.Loader.class.getDeclaredField("modController");
            control.setAccessible(true); control.set(loader, controller);
            for (Class<?> type : new Class<?>[]{cpw.mods.fml.relauncher.FMLLaunchHandler.class, cpw.mods.fml.relauncher.FMLRelaunchLog.class}) {
                java.lang.reflect.Field side = type.getDeclaredField("side");
                side.setAccessible(true); side.set(null, cpw.mods.fml.relauncher.Side.SERVER);
            }
            cpw.mods.fml.server.FMLServerHandler.instance();
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    static void registerProbe(String name, BlockExtension block) {
        org.mtr.mapping.registry.Registry registry = new org.mtr.mapping.registry.Registry();
        registry.registerBlock(new Identifier("mtr", name), () -> new Block(block));
        registry.init();
    }

    static class ProbeBlock extends BlockExtension {
        static final BooleanProperty RUNNING = BooleanProperty.of("running");
        static final IntegerProperty VARIANT = IntegerProperty.of("variant", 0, 5);
        final boolean extendedState;
        BlockState seenDuringAdd;
        Runnable duringAdd;
        ProbeBlock(boolean extendedState) {
            super(new BlockSettings(net.minecraft.block.material.Material.iron));
            this.extendedState = extendedState;
        }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) {
            properties.add(DirectionHelper.FACING);
            if (extendedState) {
                properties.add(RUNNING);
                properties.add(VARIANT);
            }
        }
        @Override public void onBlockAdded(net.minecraft.world.World world, int x, int y, int z) {
            super.onBlockAdded(world, x, y, z);
            seenDuringAdd = new World(world).getBlockState(new BlockPos(x, y, z));
            if (duringAdd != null) duringAdd.run();
        }
    }

    static class FunctionalBlock extends ProbeBlock implements BlockWithEntity {
        FunctionalBlock() { super(true); }
        public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) { return new LiftSettingsTile(pos, state); }
    }
    static class LiftSettingsTile extends BlockEntityExtension {
        String floor = "";
        long[] links = new long[0];
        LiftSettingsTile(BlockPos pos, BlockState state) { super(liftType, pos, state); }
        @Override public void writeCompoundTag(CompoundTag tag) { tag.putString("floor", floor); tag.putLongArray("links", links); }
        @Override public void readCompoundTag(CompoundTag tag) { floor = tag.getString("floor"); links = tag.getLongArray("links"); }
    }
    static class StairBlock extends ProbeBlock {
        StairBlock() { super(true); }
        @Override public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
            VoxelShape lower = Block.createCuboidShape(0, 0, 0, 16, 8, 16);
            return state.get(new Property<>(VARIANT.data)) == 0 ? lower
                : VoxelShapes.union(lower, Block.createCuboidShape(0, 8, 0, 16, 15, 8));
        }
        @Override public void onEntityCollision2(BlockState state, World world, BlockPos pos, org.mtr.mapping.holder.Entity entity) {
            if (state.get(new Property<>(RUNNING.data))) entity.addVelocity(0.1, 0, 0);
        }
    }

    public void placementCallbacksAndUpdatesSeeCompleteExtendedState() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState first = extended.getDefaultState2()
            .with(new Property<>(DirectionHelper.FACING.data), Direction.EAST.data)
            .with(new Property<>(ProbeBlock.RUNNING.data), true)
            .with(new Property<>(ProbeBlock.VARIANT.data), 5);
        assertTrue(world.setBlockState(POS, first, 3));
        assertEquals(first, extended.seenDuringAdd);
        assertEquals(first, nativeWorld.seenDuringNotify);
        assertEquals(first, world.getBlockState(POS));
        TileEntity tile = nativeWorld.getTileEntity(4, 64, 4);
        assertFalse(tile.canUpdate());
        BlockState second = first.with(new Property<>(ProbeBlock.VARIANT.data), 3);
        assertTrue(world.setBlockState(POS, second, 3));
        assertSame(tile, nativeWorld.getTileEntity(4, 64, 4));
        assertEquals(second, nativeWorld.seenDuringNotify);
        assertEquals(second, world.getBlockState(POS));
        assertFalse(world.setBlockState(POS, second, 3));
        assertFalse(world.setBlockState(new BlockPos(4, -1, 4), first, 3));
    }

    public void tileStateSurvivesNativeNbtLoadingAndDescriptionPackets() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState state = extended.getDefaultState2()
            .with(new Property<>(ProbeBlock.RUNNING.data), true)
            .with(new Property<>(ProbeBlock.VARIANT.data), 4);
        assertTrue(world.setBlockState(POS, state));
        TileEntity original = nativeWorld.getTileEntity(4, 64, 4);
        NBTTagCompound tag = new NBTTagCompound();
        original.writeToNBT(tag);
        TileEntity restored = TileEntity.createAndLoadEntity(tag);
        assertNotNull(restored);
        nativeWorld.removeTileEntity(4, 64, 4);
        nativeWorld.setTileEntity(4, 64, 4, restored);
        assertEquals(state, world.getBlockState(POS));
        assertNotNull(restored.getDescriptionPacket());
        assertTrue(world.setBlockState(POS, new Block(net.minecraft.init.Blocks.air).getDefaultState()));
        assertNull(nativeWorld.getTileEntity(4, 64, 4));
    }

    public void compactStateUsesNativeMetadataWithoutAllocatingTileEntities() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState state = compact.getDefaultState2().with(new Property<>(DirectionHelper.FACING.data), Direction.WEST.data);
        assertTrue(world.setBlockState(POS, state));
        assertNull(nativeWorld.getTileEntity(4, 64, 4));
        assertEquals(state, world.getBlockState(POS));
        assertEquals(state, nativeWorld.seenDuringNotify);
    }

    public void nestedCallbackUpdatesAreVisibleBeforeOuterPlacementReturns() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState first = extended.getDefaultState2();
        BlockState changed = first.with(new Property<>(ProbeBlock.VARIANT.data), 2);
        extended.duringAdd = () -> {
            assertTrue(world.setBlockState(POS, changed));
            assertEquals(changed, world.getBlockState(POS));
        };
        try {
            assertTrue(world.setBlockState(POS, first));
            assertEquals(changed, nativeWorld.seenDuringNotify);
            assertEquals(changed, world.getBlockState(POS));
        } finally { extended.duringAdd = null; }
    }

    public void queryingNeighborDuringPlacementDoesNotCopyThePlacedState() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockPos neighbor = POS.east();
        BlockState initial = extended.getDefaultState2();
        world.setBlockState(neighbor, initial);
        nativeWorld.removeTileEntity(neighbor.getX(), neighbor.getY(), neighbor.getZ());
        extended.duringAdd = () -> assertEquals(initial, world.getBlockState(neighbor));
        try {
            world.setBlockState(POS, initial.with(new Property<>(ProbeBlock.VARIANT.data), 5));
        } finally { extended.duringAdd = null; }
    }

    public void entireChunkSurvivesCompressedAnvilSerialization() throws Exception {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState state = functional.getDefaultState2()
            .with(new Property<>(DirectionHelper.FACING.data), Direction.SOUTH.data)
            .with(new Property<>(ProbeBlock.RUNNING.data), false)
            .with(new Property<>(ProbeBlock.VARIANT.data), 5);
        world.setBlockState(POS, state);
        ((LiftSettingsTile) nativeWorld.getTileEntity(4, 64, 4)).floor = "Верхний этаж";
        AnvilChunkLoader anvil = new AnvilChunkLoader(new java.io.File("build/unused-memory-region"));
        java.lang.reflect.Method write = AnvilChunkLoader.class.getDeclaredMethod("writeChunkToNBT", Chunk.class, net.minecraft.world.World.class, NBTTagCompound.class);
        java.lang.reflect.Method read = AnvilChunkLoader.class.getDeclaredMethod("readChunkFromNBT", net.minecraft.world.World.class, NBTTagCompound.class);
        write.setAccessible(true);
        read.setAccessible(true);
        NBTTagCompound tag = new NBTTagCompound();
        write.invoke(anvil, nativeWorld.getChunkFromChunkCoords(0, 0), nativeWorld, tag);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(tag, bytes);
        tag = CompressedStreamTools.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        MemoryWorld reopened = new MemoryWorld();
        Chunk chunk = (Chunk) read.invoke(anvil, reopened, tag);
        anvil.loadEntities(reopened, tag, chunk);
        reopened.chunks.put(0L, chunk);
        chunk.onChunkLoad();
        assertEquals(state, new World(reopened).getBlockState(POS));
        assertEquals("Верхний этаж", ((LiftSettingsTile) reopened.getTileEntity(4, 64, 4)).floor);
        assertTrue(reopened.getTileEntity(4, 64, 4).canUpdate());
    }

    public void descriptionPacketUpdatesExtendedClientState() {
        MemoryWorld server = new MemoryWorld();
        MemoryWorld client = new MemoryWorld();
        client.isRemote = true;
        BlockState state = extended.getDefaultState2().with(new Property<>(ProbeBlock.VARIANT.data), 5);
        new World(server).setBlockState(POS, state);
        new World(client).setBlockState(POS, extended.getDefaultState2());
        BlockStateTileEntity clientTile = (BlockStateTileEntity) client.getTileEntity(4, 64, 4);
        clientTile.onDataPacket(null, (S35PacketUpdateTileEntity) server.getTileEntity(4, 64, 4).getDescriptionPacket());
        assertEquals(state, new World(client).getBlockState(POS));
    }

    public void clientPlacementKeepsTheRequestedExtendedState() {
        MemoryWorld client = new MemoryWorld();
        client.isRemote = true;
        World world = new World(client);
        BlockState state = extended.getDefaultState2().with(new Property<>(ProbeBlock.VARIANT.data), 5);
        assertTrue(world.setBlockState(POS, state));
        assertEquals(state, world.getBlockState(POS));
    }

    public void functionalTileKeepsLiftSettingsAndExtendedStateAcrossNativeReload() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState first = functional.getDefaultState2().with(new Property<>(ProbeBlock.VARIANT.data), 5);
        world.setBlockState(POS, first);
        LiftSettingsTile original = (LiftSettingsTile) nativeWorld.getTileEntity(4, 64, 4);
        original.floor = "Подвал";
        original.links = new long[]{Long.MIN_VALUE, -3841L, Long.MAX_VALUE};
        original.markDirty2();
        assertTrue(original.canUpdate());
        int[] ticks = {0};
        LiftSettingsTile ticking = new LiftSettingsTile(POS, first) {
            @Override public void blockEntityTick() { ticks[0]++; }
        };
        assertTrue(ticking.canUpdate());
        ticking.updateEntity();
        assertEquals(1, ticks[0]);
        BlockState changed = first.with(new Property<>(ProbeBlock.VARIANT.data), 3);
        world.setBlockState(POS, changed);
        assertSame(original, nativeWorld.getTileEntity(4, 64, 4));
        NBTTagCompound saved = new NBTTagCompound();
        original.writeToNBT(saved);
        LiftSettingsTile restored = (LiftSettingsTile) TileEntity.createAndLoadEntity(saved);
        assertEquals("Подвал", restored.floor);
        assertArrayEquals(original.links, restored.links);
        assertEquals(POS, restored.getPos2());
        assertEquals(changed, restored.getCachedState2());
        NBTTagCompound vanilla = new NBTTagCompound();
        vanilla.setString("id", "Furnace");
        assertInstanceOf(net.minecraft.tileentity.TileEntityFurnace.class, TileEntity.createAndLoadEntity(vanilla));
        saved.setString("mtr_type", "mtr:missing_type");
        assertThrows(IllegalArgumentException.class, () -> TileEntity.createAndLoadEntity(saved));
    }

    public void nativeCollisionAndRayTracingRespectStepShapeAndStateChanges() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState state = stairs.getDefaultState2().with(new Property<>(ProbeBlock.VARIANT.data), 5);
        world.setBlockState(POS, state);
        List<net.minecraft.util.AxisAlignedBB> collisions = new ArrayList<>();
        net.minecraft.util.AxisAlignedBB behind = net.minecraft.util.AxisAlignedBB.getBoundingBox(4.25, 64.8, 4.7, 4.75, 65.4, 4.95);
        stairs.addCollisionBoxesToList(nativeWorld, 4, 64, 4, behind, collisions, null);
        assertTrue(collisions.isEmpty());
        net.minecraft.util.AxisAlignedBB ahead = net.minecraft.util.AxisAlignedBB.getBoundingBox(4.25, 64.8, 4.1, 4.75, 65.4, 4.4);
        stairs.addCollisionBoxesToList(nativeWorld, 4, 64, 4, ahead, collisions, null);
        assertEquals(1, collisions.size());
        net.minecraft.util.MovingObjectPosition hit = stairs.collisionRayTrace(nativeWorld, 4, 64, 4,
            net.minecraft.util.Vec3.createVectorHelper(4.5, 66, 4.75), net.minecraft.util.Vec3.createVectorHelper(4.5, 63, 4.75));
        assertNotNull(hit);
        assertEquals(64.5, hit.hitVec.yCoord, 1e-9);
        world.setBlockState(POS, state.with(new Property<>(ProbeBlock.VARIANT.data), 0));
        collisions.clear();
        stairs.addCollisionBoxesToList(nativeWorld, 4, 64, 4, ahead, collisions, null);
        assertTrue(collisions.isEmpty());
    }

    public void nativeEntityCallbackCanMoveRidersAndRespectsStoppedState() {
        MemoryWorld nativeWorld = new MemoryWorld();
        World world = new World(nativeWorld);
        BlockState moving = stairs.getDefaultState2().with(new Property<>(ProbeBlock.RUNNING.data), true);
        world.setBlockState(POS, moving);
        net.minecraft.entity.item.EntityItem rider = new net.minecraft.entity.item.EntityItem(nativeWorld);
        rider.setPosition(4.5, 64.5, 4.5);
        rider.motionX = rider.motionY = rider.motionZ = 0;
        rider.moveEntity(0, 0, 0);
        assertEquals(0.1, rider.motionX, 1e-9);
        world.setBlockState(POS, moving.with(new Property<>(ProbeBlock.RUNNING.data), false));
        rider.motionX = 0;
        rider.moveEntity(0, 0, 0);
        assertEquals(0, rider.motionX, 1e-9);
    }

    public static class MemoryWorld extends net.minecraft.world.World {
        BlockState seenDuringNotify;
        final Map<Long, Chunk> chunks = new HashMap<>();
        public MemoryWorld() {
            super(new SaveHandlerMP(), "MTR-test", new WorldProviderSurface(),
                new WorldSettings(1, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT), new Profiler());
            provider.registerWorld(this);
            chunkProvider = createChunkProvider();
        }
        @Override protected int func_152379_p() { return 3; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public void notifyBlockChange(int x, int y, int z, net.minecraft.block.Block old) {
            seenDuringNotify = new World(this).getBlockState(new BlockPos(x, y, z));
            super.notifyBlockChange(x, y, z, old);
        }
        @Override protected IChunkProvider createChunkProvider() {
            return new IChunkProvider() {
                long key(int x, int z) { return (long)x << 32 | z & 0xFFFFFFFFL; }
                public boolean chunkExists(int x, int z) { return chunks.containsKey(key(x, z)); }
                public Chunk provideChunk(int x, int z) {
                    return chunks.computeIfAbsent(key(x, z), k -> {
                        Chunk chunk = new Chunk(MemoryWorld.this, x, z);
                        chunk.onChunkLoad();
                        return chunk;
                    });
                }
                public Chunk loadChunk(int x, int z) { return provideChunk(x, z); }
                public void populate(IChunkProvider provider, int x, int z) {}
                public boolean saveChunks(boolean all, IProgressUpdate progress) { return true; }
                public boolean unloadQueuedChunks() { return false; }
                public boolean canSave() { return false; }
                public String makeString() { return "MTR memory test"; }
                public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) { return Collections.emptyList(); }
                public ChunkPosition func_147416_a(net.minecraft.world.World world, String name, int x, int y, int z) { return null; }
                public int getLoadedChunkCount() { return chunks.size(); }
                public void recreateStructures(int x, int z) {}
                public void saveExtraData() {}
            };
        }
    }
}
