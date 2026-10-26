package org.mtr.mapping.mapper;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.model.*;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.tool.LegacyMatrices;
import java.util.ArrayList;
import java.util.List;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ModelPartExtension {
    private final ModelBase model;
    private final Pose attachment;
    private final List<Element> definitions = new ArrayList<>();
    private final List<ModelPartExtension> children = new ArrayList<>();
    private Element[] baked;
    private float pivotX, pivotY, pivotZ, rotationX, rotationY, rotationZ;
    private float x, y, z, rx, ry, rz;
    private int u, v;
    ModelPartExtension(ModelBase model) { this(model, new Pose(0,0,0,0,0,0)); }
    private ModelPartExtension(ModelBase model, Pose attachment) { this.model = model; this.attachment = attachment; }
    public void setPivot(float x, float y, float z) { pivotX=x; pivotY=y; pivotZ=z; }
    public void setRotation(float x, float y, float z) { rotationX=x; rotationY=y; rotationZ=z; }
    public ModelPartExtension setTextureUVOffset(int u, int v) { this.u=u; this.v=v; return this; }
    private Pose pose() { return new Pose(pivotX,pivotY,pivotZ,rotationX,rotationY,rotationZ); }
    public ModelPartExtension addChild() {
        ModelPartExtension child = new ModelPartExtension(model, pose()); children.add(child);
        definitions.add(child::renderNode); return child;
    }
    public void addCuboid(float x, float y, float z, int sizeX, int sizeY, int sizeZ, float inflation, boolean mirrored) {
        ModelRenderer uv = new ModelRenderer(model); uv.mirror = mirrored;
        ModelBox box = new ModelBox(uv,u,v,x,y,z,sizeX,sizeY,sizeZ,inflation);
        TexturedQuad[] quads = ReflectionHelper.getPrivateValue(ModelBox.class, box, "quadList", "field_78254_i");
        definitions.add(new Cube(pose(), quads, mirrored));
    }
    void bake() {
        baked = definitions.toArray(new Element[0]); children.forEach(ModelPartExtension::bake);
        x=attachment.x; y=attachment.y; z=attachment.z; rx=attachment.rx; ry=attachment.ry; rz=attachment.rz;
    }
    public void render(GraphicsHolder graphics, float x, float z, float rotateY, int light, int overlay) {
        render(graphics,x,0,z,rotateY,light,overlay);
    }
    public void render(GraphicsHolder graphics, float x, float y, float z, float rotateY, int light, int overlay) {
        this.x=x; this.y=y; this.z=z; ry=rotateY;
        renderNode(graphics.matrices,(matrix,p,normal) -> graphics.drawTextureInWorld(
            (float)p[0].vector3D.xCoord/16,(float)p[0].vector3D.yCoord/16,(float)p[0].vector3D.zCoord/16,
            (float)p[1].vector3D.xCoord/16,(float)p[1].vector3D.yCoord/16,(float)p[1].vector3D.zCoord/16,
            (float)p[2].vector3D.xCoord/16,(float)p[2].vector3D.yCoord/16,(float)p[2].vector3D.zCoord/16,
            (float)p[3].vector3D.xCoord/16,(float)p[3].vector3D.yCoord/16,(float)p[3].vector3D.zCoord/16,
            p[0].texturePositionX,p[2].texturePositionY,p[1].texturePositionX,p[0].texturePositionY,normal,-1,light));
    }
    void visit(LegacyMatrices matrices,FaceConsumer faces) { renderNode(matrices,faces); }
    private void renderNode(LegacyMatrices matrices,FaceConsumer faces) {
        if (baked == null) return;
        matrices.push();
        try {
            transform(matrices,x,y,z,rx,ry,rz);
            for (Element element : baked) element.draw(matrices,faces);
        } finally { matrices.pop(); }
    }
    private static void transform(LegacyMatrices matrices,float x,float y,float z,float rx,float ry,float rz) {
        matrices.translate(x/16F,y/16F,z/16F);
        if (rz != 0) matrices.rotate(rz,0,0,1);
        if (ry != 0) matrices.rotate(ry,0,1,0);
        if (rx != 0) matrices.rotate(rx,1,0,0);
    }
    interface FaceConsumer { void face(LegacyMatrices matrices,PositionTextureVertex[] vertices,Direction normal); }
    private interface Element { void draw(LegacyMatrices matrices,FaceConsumer faces); }
    private static final class Pose {
        final float x,y,z,rx,ry,rz;
        Pose(float x, float y, float z, float rx, float ry, float rz) { this.x=x; this.y=y; this.z=z; this.rx=rx; this.ry=ry; this.rz=rz; }
    }
    private static final class Cube implements Element {
        private static final Direction[] NORMALS = {Direction.EAST,Direction.WEST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH};
        private final Pose pose;
        private final TexturedQuad[] faces;
        private final boolean mirrored;
        Cube(Pose pose, TexturedQuad[] faces, boolean mirrored) { this.pose=pose; this.faces=faces; this.mirrored=mirrored; }
        public void draw(LegacyMatrices matrices,FaceConsumer consumer) {
            matrices.push();
            try {
                transform(matrices,pose.x,pose.y,pose.z,pose.rx,pose.ry,pose.rz);
                for (int i = 0; i < faces.length; i++) {
                    PositionTextureVertex[] p = faces[i].vertexPositions;
                    Direction normal = mirrored && i < 2 ? NORMALS[1-i] : NORMALS[i];
                    consumer.face(matrices,p,normal);
                }
            } finally { matrices.pop(); }
        }
    }
}
