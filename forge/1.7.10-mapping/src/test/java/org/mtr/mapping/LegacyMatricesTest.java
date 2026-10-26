package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.*;
import org.mtr.mapping.tool.LegacyMatrices;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMatricesTest {
    @Test void nestedTransformsKeepParentAndInverseTransposeNormals() {
        LegacyMatrices matrices = new LegacyMatrices();
        matrices.translate(10, 20, 30); matrices.push();
        matrices.scale(2, 1, 0.5F); matrices.rotate((float)(Math.PI / 4), 0, 0, 1);
        Vector3f normal = matrices.normal(1, 0, 0);
        assertEquals(1 / Math.sqrt(5), normal.x, 0.00001);
        assertEquals(2 / Math.sqrt(5), normal.y, 0.00001);
        assertEquals(0, normal.z);
        matrices.pop();
        Vector4f point = Matrix4f.transform(matrices.position(), new Vector4f(1, 2, 3, 1), null);
        assertEquals(11, point.x); assertEquals(22, point.y); assertEquals(33, point.z);
        assertThrows(IllegalStateException.class, matrices::pop);
        matrices.scale(0, 0, 0);
        Vector3f collapsed = matrices.normal(0, 1, 0);
        assertEquals(0, collapsed.lengthSquared());
    }
}
