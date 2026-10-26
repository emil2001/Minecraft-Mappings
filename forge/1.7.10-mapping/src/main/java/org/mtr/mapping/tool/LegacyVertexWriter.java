package org.mtr.mapping.tool;

import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.mtr.mapping.holder.Direction;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyVertexWriter {
    private LegacyVertexWriter() {}
    public static void modelFace(Tessellator tessellator,LegacyMatrices matrices,float[] face,int color,int light) {
        if (face.length!=32) throw new IllegalArgumentException("A model face must contain four position/UV/normal vertices");
        tessellator.setColorRGBA_I(color&0xFFFFFF,color>>>24); tessellator.setBrightness(light);
        for (int i=0;i<32;i+=8) {
            Vector3f normal=matrices.normal(face[i+5],face[i+6],face[i+7]); tessellator.setNormal(normal.x,normal.y,normal.z);
            vertex(tessellator,matrices.position(),face[i],face[i+1],face[i+2],face[i+3],face[i+4]);
        }
    }
    public static void line(Tessellator tessellator, LegacyMatrices matrices, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        tessellator.setColorRGBA_I(color & 0xFFFFFF, color >>> 24);
        Vector3f normal = matrices.normal(0, 1, 0); tessellator.setNormal(normal.x, normal.y, normal.z);
        position(tessellator, matrices.position(), x1,y1,z1); position(tessellator, matrices.position(), x2,y2,z2);
    }
    public static void quad(Tessellator tessellator, LegacyMatrices matrices,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
            float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
        Vector3f normal = matrices.normal(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
        tessellator.setNormal(normal.x, normal.y, normal.z);
        tessellator.setColorRGBA_I(color & 0xFFFFFF, color >>> 24); tessellator.setBrightness(light);
        Matrix4f matrix = matrices.position();
        vertex(tessellator, matrix, x1, y1, z1, u1, v2);
        vertex(tessellator, matrix, x2, y2, z2, u2, v2);
        vertex(tessellator, matrix, x3, y3, z3, u2, v1);
        vertex(tessellator, matrix, x4, y4, z4, u1, v1);
    }
    private static void vertex(Tessellator tessellator, Matrix4f matrix, float x, float y, float z, float u, float v) {
        tessellator.setTextureUV(u,v); position(tessellator,matrix,x,y,z);
    }
    private static void position(Tessellator tessellator, Matrix4f matrix, float x, float y, float z) {
        tessellator.addVertex(matrix.m00*x + matrix.m10*y + matrix.m20*z + matrix.m30,
            matrix.m01*x + matrix.m11*y + matrix.m21*z + matrix.m31,
            matrix.m02*x + matrix.m12*y + matrix.m22*z + matrix.m32);
    }
}
