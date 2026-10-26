package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mod.block.*;
import static org.junit.jupiter.api.Assertions.*;

public class MtrEscalatorScenarios {
    public void bothNativePickOverloadsReturnOriginalEscalatorWithoutItemBlock() {
        org.mtr.mod.Blocks.init();
        org.mtr.mapping.tool.LegacyTranslations.loadDefaults();
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        net.minecraft.entity.player.EntityPlayer player = player(nativeWorld);
        net.minecraft.item.Item escalator = org.mtr.mod.Items.ESCALATOR.get().data;
        Block[] originals = {org.mtr.mod.Blocks.ESCALATOR_STEP.get(), org.mtr.mod.Blocks.ESCALATOR_SIDE.get()};
        for (int index = 0; index < originals.length; index++) {
            Block original = originals[index];
            BlockPos pos = new BlockPos(4 + index * 2, 64, 4);
            assertNull(net.minecraft.item.Item.getItemFromBlock(original.data), "Internal block must have no ItemBlock fallback");
            assertTrue(world.setBlockState(pos, original.getDefaultState(), 2));
            net.minecraft.util.MovingObjectPosition target = target(pos);
            // Real Forge native dispatch through a Block reference, not a mapping method call.
            net.minecraft.item.ItemStack modern = original.data.getPickBlock(target, nativeWorld, pos.getX(), pos.getY(), pos.getZ(), player);
            assertOriginalEscalatorPick(modern, escalator);
            net.minecraft.item.ItemStack legacy = original.data.getPickBlock(target, nativeWorld, pos.getX(), pos.getY(), pos.getZ());
            assertOriginalEscalatorPick(legacy, escalator);
            assertEquals("Escalator", original.data.getLocalizedName());
        }
    }

    public void bothNativePickOverloadsPreserveMappedIdentityInputsAndFailureSemantics() {
        org.mtr.mod.Blocks.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        BlockPos pos = new BlockPos(4, 64, 4);
        PickProbe probe = new PickProbe();
        BlockPlacementScenarios.registerProbe("test_native_pick_bridge", probe);
        assertNull(net.minecraft.item.Item.getItemFromBlock(probe));
        assertTrue(world.setBlockState(pos, probe.getDefaultState2().with(new Property<>(PickProbe.MARKED.data), true), 2));
        net.minecraft.block.Block block = probe;
        net.minecraft.util.MovingObjectPosition target = target(pos);
        net.minecraft.entity.player.EntityPlayer player = player(nativeWorld);
        net.minecraft.item.ItemStack original = new net.minecraft.item.ItemStack(org.mtr.mod.Items.ESCALATOR.get().data, 3, 7);
        original.setStackDisplayName("Original mapped pick");
        probe.result = new ItemStack(original);
        assertSame(original, block.getPickBlock(target, nativeWorld, 4, 64, 4, player));
        assertSame(original, block.getPickBlock(target, nativeWorld, 4, 64, 4));
        assertEquals(2, probe.calls);
        assertSame(nativeWorld, probe.seenWorld.data);
        assertEquals(pos.getX(), probe.seenPos.getX());
        assertEquals(pos.getY(), probe.seenPos.getY());
        assertEquals(pos.getZ(), probe.seenPos.getZ());
        assertTrue(probe.seenState.isOf(new Block(probe)));
        assertTrue(IBlock.getStatePropertySafe(probe.seenState, PickProbe.MARKED));

        original.stackSize = 0;
        assertSame(original, block.getPickBlock(target, nativeWorld, 4, 64, 4, player), "Do not normalize a mapped zero-count native object");
        assertSame(original, block.getPickBlock(target, nativeWorld, 4, 64, 4));
        probe.result = ItemStack.getEmptyMapped();
        assertNull(block.getPickBlock(target, nativeWorld, 4, 64, 4, player));
        assertNull(block.getPickBlock(target, nativeWorld, 4, 64, 4));
        probe.result = null;
        assertThrows(NullPointerException.class, () -> block.getPickBlock(target, nativeWorld, 4, 64, 4, player));
        assertThrows(NullPointerException.class, () -> block.getPickBlock(target, nativeWorld, 4, 64, 4));
        IllegalStateException rejected = new IllegalStateException("Mapped pick rejected");
        probe.failure = rejected;
        assertSame(rejected, assertThrows(IllegalStateException.class, () -> block.getPickBlock(target, nativeWorld, 4, 64, 4, player)));
        assertSame(rejected, assertThrows(IllegalStateException.class, () -> block.getPickBlock(target, nativeWorld, 4, 64, 4)));
    }

    private static net.minecraft.util.MovingObjectPosition target(BlockPos pos) {
        return new net.minecraft.util.MovingObjectPosition(pos.getX(), pos.getY(), pos.getZ(), 1,
            net.minecraft.util.Vec3.createVectorHelper(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5));
    }

    private static void assertOriginalEscalatorPick(net.minecraft.item.ItemStack stack, net.minecraft.item.Item escalator) {
        assertNotNull(stack, "Both native pick signatures must use the original mapped escalator item");
        assertSame(escalator, stack.getItem());
        assertEquals(1, stack.stackSize);
        assertEquals(0, stack.getItemDamage());
        assertEquals("Escalator", stack.getDisplayName());
    }

    private static final class PickProbe extends org.mtr.mapping.mapper.BlockExtension {
        static final BooleanProperty MARKED = BooleanProperty.of("marked");
        ItemStack result;
        IllegalStateException failure;
        BlockView seenWorld;
        BlockPos seenPos;
        BlockState seenState;
        int calls;
        PickProbe() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) { properties.add(MARKED); }
        @Override public ItemStack getPickStack2(BlockView world, BlockPos pos, BlockState state) {
            ++calls; seenWorld = world; seenPos = pos; seenState = state;
            if (failure != null) throw failure;
            return result;
        }
    }

    public void originalEscalatorLootDropsOnlyLeftHalfAndKeepsStateDuringHarvest() {
        org.mtr.mod.Blocks.init();
        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
        World mapped = new World(world);
        BlockPos pos = new BlockPos(4, 64, 4);
        net.minecraft.block.Block block = org.mtr.mod.Blocks.ESCALATOR_STEP.get().data;
        BlockState step = org.mtr.mod.Blocks.ESCALATOR_STEP.get().getDefaultState();
        mapped.setBlockState(pos, step.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.LEFT), 2);
        mapped.setBlockState(pos.offset(Direction.EAST), step.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.RIGHT), 2);
        java.util.List<net.minecraft.item.ItemStack> left = block.getDrops(world, 4, 64, 4, world.getBlockMetadata(4, 64, 4), 0);
        assertEquals(1, left.size());
        assertSame(org.mtr.mod.Items.ESCALATOR.get().data, left.get(0).getItem());
        assertTrue(block.getDrops(world, 5, 64, 4, world.getBlockMetadata(5, 64, 4), 0).isEmpty());
        net.minecraft.entity.player.EntityPlayer player = player(world);
        int metadata = world.getBlockMetadata(4, 64, 4);
        block.onBlockHarvested(world, 4, 64, 4, metadata, player);
        assertTrue(block.removedByPlayer(world, player, 4, 64, 4, true));
        assertSame(block, world.getBlock(4, 64, 4), "Full state must remain until drops are evaluated");
        block.harvestBlock(world, player, 4, 64, 4, metadata);
        assertSame(net.minecraft.init.Blocks.air, world.getBlock(4, 64, 4));
        int count = 0;
        for (Object entity : world.loadedEntityList) {
            if (entity instanceof net.minecraft.entity.item.EntityItem) {
                net.minecraft.item.ItemStack stack = ((net.minecraft.entity.item.EntityItem) entity).getEntityItem();
                if (stack.getItem() == org.mtr.mod.Items.ESCALATOR.get().data) count += stack.stackSize;
            }
        }
        assertEquals(1, count, "Breaking an escalator pair must not duplicate or lose its item");
    }

    public void originalEscalatorSurvivesPlacementMovesRidersAndChangesDirectionWithBrush() {
        org.mtr.mod.Blocks.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        BlockPos left = new BlockPos(4, 64, 4);
        BlockPos right = left.offset(Direction.EAST);
        BlockState step = org.mtr.mod.Blocks.ESCALATOR_STEP.get().getDefaultState().with(new Property<>(BlockEscalatorStep.FACING.data), Direction.NORTH.data);
        BlockState side = org.mtr.mod.Blocks.ESCALATOR_SIDE.get().getDefaultState().with(new Property<>(BlockEscalatorSide.FACING.data), Direction.NORTH.data);
        world.setBlockState(left, step.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.LEFT));
        world.setBlockState(right, step.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.RIGHT));
        world.setBlockState(left.up(), side.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.LEFT));
        world.setBlockState(right.up(), side.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.RIGHT));
        assertTrue(world.getBlockState(left).isOf(org.mtr.mod.Blocks.ESCALATOR_STEP.get()));
        assertTrue(world.getBlockState(right).isOf(org.mtr.mod.Blocks.ESCALATOR_STEP.get()));
        net.minecraft.entity.item.EntityItem rider = new net.minecraft.entity.item.EntityItem(nativeWorld);
        rider.setPosition(4.5, 64.96, 4.5);
        rider.moveEntity(0, 0, 0);
        assertEquals(-.1F, rider.motionZ, 1e-7);
        net.minecraft.entity.player.EntityPlayer player = player(nativeWorld);
        player.inventory.mainInventory[0] = org.mtr.mod.Items.BRUSH.get().getDefaultStack().data;
        net.minecraft.block.Block nativeStep = org.mtr.mod.Blocks.ESCALATOR_STEP.get().data;
        assertTrue(nativeStep.onBlockActivated(nativeWorld, 4, 64, 4, player, 1, .5F, 1, .5F));
        assertFalse(IBlock.getStatePropertySafe(world, left, BlockEscalatorStep.DIRECTION));
        assertFalse(IBlock.getStatePropertySafe(world, right, BlockEscalatorStep.DIRECTION));
        rider.motionZ = 0;
        rider.moveEntity(0, 0, 0);
        assertEquals(.1F, rider.motionZ, 1e-7);
        assertTrue(nativeStep.onBlockActivated(nativeWorld, 4, 64, 4, player, 1, .5F, 1, .5F));
        assertFalse(IBlock.getStatePropertySafe(world, left, BlockEscalatorStep.STATUS));
        rider.motionZ = 0;
        rider.moveEntity(0, 0, 0);
        assertEquals(0, rider.motionZ);
        player.capabilities.isCreativeMode = true;
        org.mtr.mod.Blocks.ESCALATOR_SIDE.get().data.onBlockHarvested(nativeWorld, 5, 65, 4, 0, player);
        nativeWorld.setBlockToAir(5, 65, 4);
        for (BlockPos blockPos : new BlockPos[]{left, right, left.up(), right.up()}) {
            assertEquals(net.minecraft.init.Blocks.air, world.getBlockState(blockPos).getBlock().data);
        }
    }

    static net.minecraft.entity.player.EntityPlayer player(net.minecraft.world.World world) {
        return new net.minecraft.entity.player.EntityPlayer(world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MtrBrush")) {
            public void addChatMessage(net.minecraft.util.IChatComponent text) {}
            public boolean canCommandSenderUseCommand(int level, String command) { return true; }
            public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return new net.minecraft.util.ChunkCoordinates(0, 64, 0); }
        };
    }
}
