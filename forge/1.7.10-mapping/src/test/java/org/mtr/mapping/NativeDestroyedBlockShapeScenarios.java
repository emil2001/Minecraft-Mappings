package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.BlockStateTileEntity;
import org.mtr.mod.block.BlockLiftPanelBase;
import org.mtr.mod.block.BlockLiftPanelEven1;
import org.mtr.mod.block.BlockLiftPanelEven2;
import org.mtr.mod.block.BlockLiftPanelOdd1;
import org.mtr.mod.block.BlockLiftPanelOdd2;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class NativeDestroyedBlockShapeScenarios {
    private static final BlockPos POS = new BlockPos(4, 64, 4);

    public void originalLiftPanelsAllowLateDestroyCollisionQueriesAfterAirOrReplacement() {
        ClientSnapshotWorld world = new ClientSnapshotWorld();
        for (BlockLiftPanelBase panel : panels("destroy_collision")) {
            AxisAlignedBB expected = bounds(panel, Direction.NORTH);
            for (net.minecraft.block.Block replacement : replacements()) {
                world.show(new Block(replacement).getDefaultState());
                // Et Futurum asks the old block for destroy-particle collision after S28 event 2001,
                // when the client world already contains air or a replacement at this position.
                assertCollision(panel, world, expected);
                assertSame(replacement, world.getBlock(POS.getX(), POS.getY(), POS.getZ()),
                    "A late shape query must not restore the destroyed panel into the world");
            }
        }
    }

    public void originalLiftPanelsAllowLateRayTraceAndSelectionAfterAirOrReplacement() {
        ClientSnapshotWorld world = new ClientSnapshotWorld();
        for (BlockLiftPanelBase panel : panels("destroy_outline")) {
            AxisAlignedBB expected = bounds(panel, Direction.NORTH);
            for (net.minecraft.block.Block replacement : replacements()) {
                world.show(new Block(replacement).getDefaultState());
                assertOutline(panel, world, expected);
            }
        }
    }

    public void originalLiftPanelsKeepRotatedLiveStateInAllNativeShapeQueries() {
        ClientSnapshotWorld world = new ClientSnapshotWorld();
        for (BlockLiftPanelBase panel : panels("live_shapes")) {
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockState live = panel.getDefaultState2().with(new Property<>(DirectionHelper.FACING.data), facing.data);
                world.show(live);
                AxisAlignedBB expected = bounds(panel, facing);
                assertCollision(panel, world, expected);
                assertOutline(panel, world, expected);
            }
        }
    }

    private static BlockLiftPanelBase[] panels(String prefix) {
        BlockLiftPanelBase[] panels = {
            new BlockLiftPanelEven1(), new BlockLiftPanelEven2(), new BlockLiftPanelOdd1(), new BlockLiftPanelOdd2()
        };
        for (int i = 0; i < panels.length; i++) BlockPlacementScenarios.registerProbe("native_" + prefix + "_" + i, panels[i]);
        return panels;
    }

    private static net.minecraft.block.Block[] replacements() {
        return new net.minecraft.block.Block[]{net.minecraft.init.Blocks.air, net.minecraft.init.Blocks.stone};
    }

    private static AxisAlignedBB bounds(BlockLiftPanelBase panel, Direction facing) {
        double thickness = panel instanceof BlockLiftPanelEven2 || panel instanceof BlockLiftPanelOdd2 ? 1.0 / 16 : 4.0 / 16;
        AxisAlignedBB local;
        switch (facing) {
            case EAST: local = AxisAlignedBB.getBoundingBox(1 - thickness, 0, 0, 1, 1, 1); break;
            case SOUTH: local = AxisAlignedBB.getBoundingBox(0, 0, 1 - thickness, 1, 1, 1); break;
            case WEST: local = AxisAlignedBB.getBoundingBox(0, 0, 0, thickness, 1, 1); break;
            default: local = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, thickness);
        }
        return local.getOffsetBoundingBox(POS.getX(), POS.getY(), POS.getZ());
    }

    private static void assertCollision(net.minecraft.block.Block panel, ClientSnapshotWorld world, AxisAlignedBB expected) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        panel.addCollisionBoxesToList(world, POS.getX(), POS.getY(), POS.getZ(),
            AxisAlignedBB.getBoundingBox(3, 63, 3, 6, 66, 6), boxes, null);
        assertEquals(1, boxes.size(), "Native destroy-effect queries must receive the panel's own shape");
        assertBounds(expected, boxes.get(0));
    }

    private static void assertOutline(net.minecraft.block.Block panel, ClientSnapshotWorld world, AxisAlignedBB expected) {
        assertBounds(expected, panel.getSelectedBoundingBoxFromPool(world, POS.getX(), POS.getY(), POS.getZ()));
        double centerX = (expected.minX + expected.maxX) / 2;
        double centerZ = (expected.minZ + expected.maxZ) / 2;
        MovingObjectPosition hit = panel.collisionRayTrace(world, POS.getX(), POS.getY(), POS.getZ(),
            Vec3.createVectorHelper(centerX, 66, centerZ), Vec3.createVectorHelper(centerX, 63, centerZ));
        assertNotNull(hit, "Native ray tracing must retain the panel's current orientation");
        assertEquals(expected.maxY, hit.hitVec.yCoord, 1e-9);
        assertEquals(POS.getX(), hit.blockX);
        assertEquals(POS.getY(), hit.blockY);
        assertEquals(POS.getZ(), hit.blockZ);
    }

    private static void assertBounds(AxisAlignedBB expected, AxisAlignedBB actual) {
        assertNotNull(actual);
        assertEquals(expected.minX, actual.minX, 1e-9);
        assertEquals(expected.minY, actual.minY, 1e-9);
        assertEquals(expected.minZ, actual.minZ, 1e-9);
        assertEquals(expected.maxX, actual.maxX, 1e-9);
        assertEquals(expected.maxY, actual.maxY, 1e-9);
        assertEquals(expected.maxZ, actual.maxZ, 1e-9);
    }

    /** Models the world state already received before a late native destroy-effect query. */
    private static final class ClientSnapshotWorld extends BlockPlacementScenarios.MemoryWorld {
        private BlockState visible;
        private int metadata;
        private BlockStateTileEntity stateTile;

        ClientSnapshotWorld() { isRemote = true; }

        void show(BlockState state) {
            visible = state;
            metadata = state.data.write(new NBTTagCompound());
            stateTile = new BlockStateTileEntity();
            stateTile.setMtrState(state);
        }

        private boolean selected(int x, int y, int z) {
            return visible != null && x == POS.getX() && y == POS.getY() && z == POS.getZ();
        }

        @Override public net.minecraft.block.Block getBlock(int x, int y, int z) {
            return selected(x, y, z) ? visible.getBlock().data : super.getBlock(x, y, z);
        }
        @Override public int getBlockMetadata(int x, int y, int z) {
            return selected(x, y, z) ? metadata : super.getBlockMetadata(x, y, z);
        }
        @Override public TileEntity getTileEntity(int x, int y, int z) {
            return selected(x, y, z) ? stateTile : super.getTileEntity(x, y, z);
        }
    }
}
