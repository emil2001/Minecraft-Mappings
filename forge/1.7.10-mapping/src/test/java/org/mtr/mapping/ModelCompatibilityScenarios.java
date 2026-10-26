package org.mtr.mapping;

import org.mtr.mapping.holder.EntityAbstractMapping;
import org.mtr.mapping.mapper.EntityModelExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ModelPartExtension;
import static org.junit.jupiter.api.Assertions.*;

public final class ModelCompatibilityScenarios {
    private static final class Model extends EntityModelExtension<EntityAbstractMapping> {
        Model() { super(64, 32); }
        @Override public void setAngles2(EntityAbstractMapping entity, float a, float b, float c, float d, float e) {}
        @Override public void render(GraphicsHolder graphics, int light, int overlay, float red, float green, float blue, float alpha) {}
    }
    public void modelPartsKeepNestedPosesPixelUnitsAndParentGraphicsTransform() throws Exception {
        Model model = new Model();
        ModelPartExtension root = model.createModelPart();
        root.setPivot(16, 0, 0); root.setRotation(0, 0, (float)(Math.PI / 2));
        root.setTextureUVOffset(2, 3).addCuboid(0, 0, 0, 16, 16, 16, 0, false);
        ModelPartExtension child = root.addChild(); child.setPivot(0, 16, 0);
        child.setTextureUVOffset(0, 0).addCuboid(0, 0, 0, 16, 16, 16, 0, false);
        model.buildModel();
        NativeGraphicsFixture fixture = new NativeGraphicsFixture();
        fixture.graphics.translate(10, 20, 30);
        root.render(fixture.graphics, 32, 0, 0, 0, 0x00B00070, 0);
        assertEquals(48, fixture.count());
        int[] data = fixture.buffer();
        assertBounds(data, 0, 24, new float[]{12,20,30,13,21,31});
        assertBounds(data, 24, 48, new float[]{11,20,30,12,21,31});
        assertEquals(0x7F00, data[6], "Model face normals must follow the part rotation");
        assertEquals(0x00B00070, data[7]);
        assertEquals(0.78125F, Float.intBitsToFloat(data[3]));
        assertEquals(0.59375F, Float.intBitsToFloat(data[4]));
        org.lwjgl.util.vector.Matrix4f matrix = fixture.graphics.copyPositionMatrix().data;
        assertEquals(10, matrix.m30); assertEquals(20, matrix.m31); assertEquals(30, matrix.m32);
        assertEquals(1, matrix.m00); assertEquals(1, matrix.m11); assertEquals(1, matrix.m22);
    }
    public void liftPlanesKeepBothFaceNormalsAndMirroredCuboidsKeepUvsAndInflation() throws Exception {
        Model model = new Model();
        ModelPartExtension floor = model.createModelPart(); floor.setPivot(0, 24, 0);
        floor.setTextureUVOffset(0, 0).addCuboid(0, 0, 0, 16, 0, 16, 0, false);
        ModelPartExtension mirrored = model.createModelPart();
        mirrored.setTextureUVOffset(2, 3).addCuboid(0, 0, 0, 16, 16, 16, 0.5F, true);
        model.buildModel();
        NativeGraphicsFixture fixture = new NativeGraphicsFixture();
        floor.render(fixture.graphics, 0, 0, 0, 0xF000F0, 0);
        mirrored.render(fixture.graphics, 0, 0, 0, 0xF000F0, 0);
        assertEquals(48, fixture.count());
        int[] data = fixture.buffer();
        assertBounds(data, 0, 24, new float[]{0,1.5F,0,1,1.5F,1});
        assertEquals(0x8100, data[8*8+6], "A flat floor still needs a downward normal");
        assertEquals(0x7F00, data[12*8+6], "A flat floor still needs an upward normal");
        assertBounds(data, 24, 48, new float[]{-0.03125F,-0.03125F,-0.03125F,1.03125F,1.03125F,1.03125F});
        assertEquals(0x81, data[24*8+6], "Mirroring flips the X face normal");
        assertEquals(0.78125F, Float.intBitsToFloat(data[24*8+3]));
        assertEquals(1.09375F, Float.intBitsToFloat(data[24*8+4]));
    }
    private static void assertBounds(int[] data, int start, int end, float[] expected) {
        for (int axis = 0; axis < 3; axis++) {
            float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
            for (int vertex = start; vertex < end; vertex++) {
                float value = Float.intBitsToFloat(data[vertex*8+axis]); min = Math.min(min, value); max = Math.max(max, value);
            }
            assertEquals(expected[axis], min, 0.00001); assertEquals(expected[axis+3], max, 0.00001);
        }
    }
}
