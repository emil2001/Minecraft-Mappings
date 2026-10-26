package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.*;
import static org.junit.jupiter.api.Assertions.*;

class GeometryCompatibilityTest {
    @Test void modernVectorOperationsKeepTheirOperandUnchanged() {
        Vector3d value = new Vector3d(3, 4, 0);
        assertEquals(5, value.distanceTo(Vector3d.getZeroMapped()), 1e-12);
        assertEquals(25, value.squaredDistanceTo(Vector3d.getZeroMapped()), 1e-12);
        Vector3d difference = value.subtract(new Vector3d(1, 2, 3));
        assertEquals(2, difference.getXMapped());
        assertEquals(2, difference.getYMapped());
        assertEquals(-3, difference.getZMapped());
        Vector3d rotated = new Vector3d(1, 0, 0).rotateY((float) (Math.PI / 2));
        assertEquals(0, rotated.getXMapped(), 1e-6);
        assertEquals(-1, rotated.getZMapped(), 1e-6);
        assertEquals(3, value.getXMapped());
        assertEquals(4, value.getYMapped());
    }

    @Test void boxCoordinatesNormalizeAndEqualityUsesCoordinates() {
        Box box = new Box(4, 5, 6, 1, 2, 3);
        assertEquals(new Box(1, 2, 3, 4, 5, 6), box);
        assertEquals(new Box(1, 2, 3, 4, 5, 6).hashCode(), box.hashCode());
        assertTrue(box.intersects(new Box(3, 3, 3, 5, 5, 5)));
        assertFalse(box.intersects(new Box(4, 2, 3, 5, 5, 6)));
    }

    @Test void escalatorIntersectionRetainsRaisedStepAndClearance() {
        VoxelShape lower = Block.createCuboidShape(0, 0, 0, 16, 8, 16);
        VoxelShape upper = Block.createCuboidShape(0, 8, 0, 16, 15, 8);
        VoxelShape step = VoxelShapes.union(lower, upper);
        VoxelShape inset = Block.createCuboidShape(1, 0, 1, 15, 16, 15);
        VoxelShape clipped = VoxelShapes.combine(inset, step, BooleanBiFunction.getAndMapped());
        assertEquals(2, clipped.getBoundingBoxes().size());
        assertTrue(contains(clipped, 0.5, 0.9, 0.25));
        assertFalse(contains(clipped, 0.5, 0.9, 0.75));
        assertFalse(contains(clipped, 0, 0.25, 0.5));
        assertEquals(1, VoxelShapes.union(lower, lower).getBoundingBoxes().size());
        assertTrue(VoxelShapes.combine(step, VoxelShapes.empty(), BooleanBiFunction.getAndMapped()).getBoundingBoxes().isEmpty());
    }

    @Test void touchingShapesHaveNoCollisionVolume() {
        VoxelShape left = Block.createCuboidShape(0, 0, 0, 8, 16, 16);
        VoxelShape right = Block.createCuboidShape(8, 0, 0, 16, 16, 16);
        assertTrue(VoxelShapes.combine(left, right, BooleanBiFunction.getAndMapped()).getBoundingBoxes().isEmpty());
        assertTrue(Block.createCuboidShape(0, 0, 0, 0, 16, 16).getBoundingBoxes().isEmpty());
        assertEquals(1, VoxelShapes.fullCube().getBoundingBoxes().size());
    }

    private static boolean contains(VoxelShape shape, double x, double y, double z) {
        return shape.getBoundingBoxes().stream().anyMatch(box -> x > box.getMinXMapped() && x < box.getMaxXMapped()
            && y > box.getMinYMapped() && y < box.getMaxYMapped() && z > box.getMinZMapped() && z < box.getMaxZMapped());
    }
}
