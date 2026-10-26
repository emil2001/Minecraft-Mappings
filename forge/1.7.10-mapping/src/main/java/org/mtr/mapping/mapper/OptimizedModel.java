package org.mtr.mapping.mapper;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.tool.LegacyMatrices;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Function;

/** Immutable CPU geometry; rendering uses the native legacy tessellator. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class OptimizedModel implements AutoCloseable {
    List<Mesh> meshes;
    private OptimizedModel(List<Mesh> meshes) { this.meshes=Collections.unmodifiableList(meshes); }
    public OptimizedModel(OptimizedModel... models) {
        List<Mesh> meshes=new ArrayList<>(); for (OptimizedModel model : models) meshes.addAll(model.meshes);
        this.meshes=Collections.unmodifiableList(meshes);
    }
    public static OptimizedModel fromMaterialGroups(Collection<MaterialGroup> groups) {
        List<Mesh> meshes=new ArrayList<>();
        for (MaterialGroup group : groups) {
            List<float[]> faces=new ArrayList<>(); group.parts.forEach(part -> part.accept(faces));
            meshes.add(new Mesh(group.material,faces));
        }
        return new OptimizedModel(meshes);
    }
    public static OptimizedModel fromObjModels(Collection<ObjModel> models) {
        List<Mesh> meshes=new ArrayList<>();
        for (ObjModel model : models) {
            Map<Material,Map<FloatBuffer,float[]>> groups=new LinkedHashMap<>();
            for (Mesh mesh : model.placed) for (float[] source : mesh.faces) {
                float[] face=source.clone();
                Vector3f a=new Vector3f(face[8]-face[0],face[9]-face[1],face[10]-face[2]);
                Vector3f b=new Vector3f(face[16]-face[0],face[17]-face[1],face[18]-face[2]);
                Vector3f normal=Vector3f.cross(a,b,null);
                if (normal.lengthSquared()!=0) normal.normalise();
                for (int i=0;i<32;i+=8) if (face[i+5]==0 && face[i+6]==0 && face[i+7]==0) {
                    face[i+5]=normal.x; face[i+6]=normal.y; face[i+7]=normal.z;
                }
                groups.computeIfAbsent(mesh.material,key -> new LinkedHashMap<>()).put(FloatBuffer.wrap(face),face);
            }
            groups.forEach((material,faces) -> meshes.add(new Mesh(material,new ArrayList<>(faces.values()))));
        }
        return new OptimizedModel(meshes);
    }
    @Override public void close() { meshes=Collections.emptyList(); }
    public static final class MaterialGroup {
        private final Material material;
        private final List<java.util.function.Consumer<List<float[]>>> parts=new ArrayList<>();
        public MaterialGroup(ShaderType shader,Identifier texture) { material=new Material(shader,texture,-1); }
        public void addCube(ModelPartExtension part,double x,double y,double z,boolean flipped,int light) {
            parts.add(faces -> {
                LegacyMatrices matrices=new LegacyMatrices(); matrices.translate(x,y,z);
                if (flipped) matrices.rotate((float)Math.PI,0,1,0);
                part.visit(matrices,(transform,vertices,normal) -> {
                    float[] face=new float[32]; Matrix4f matrix=transform.position();
                    Vector3f n=transform.normal(normal.getOffsetX(),normal.getOffsetY(),normal.getOffsetZ());
                    for (int i=0;i<4;i++) {
                        float px=(float)vertices[i].vector3D.xCoord/16, py=(float)vertices[i].vector3D.yCoord/16, pz=(float)vertices[i].vector3D.zCoord/16;
                        int offset=i*8;
                        face[offset]=matrix.m00*px+matrix.m10*py+matrix.m20*pz+matrix.m30;
                        face[offset+1]=matrix.m01*px+matrix.m11*py+matrix.m21*pz+matrix.m31;
                        face[offset+2]=matrix.m02*px+matrix.m12*py+matrix.m22*pz+matrix.m32;
                        face[offset+3]=vertices[i].texturePositionX; face[offset+4]=vertices[i].texturePositionY;
                        face[offset+5]=n.x; face[offset+6]=n.y; face[offset+7]=n.z;
                    }
                    faces.add(face);
                });
            });
        }
    }
    public static final class ObjModel {
        private final List<Mesh> raw,placed=new ArrayList<>();
        private final float[] bounds;
        ObjModel(List<Mesh> raw,float[] bounds) { this.raw=raw; this.bounds=bounds; }
        public static Map<String,ObjModel> loadModel(String obj,Function<String,String> mtlResolver,Function<String,Identifier> textureResolver,Identifier atlasIndex,boolean splitModel,boolean flipTextureV) {
            // shortcut: built-in MTR callers use no atlas; implement addon atlas mapping when needed.
            if (atlasIndex!=null) throw new UnsupportedOperationException("OBJ texture atlases are not supported on 1.7.10");
            return LegacyObjLoader.load(obj,mtlResolver,textureResolver,splitModel,flipTextureV);
        }
        public void addTransformation(ShaderType shader,double x,double y,double z,boolean flipped) {
            LegacyMatrices matrices=new LegacyMatrices();
            if (flipped) matrices.rotate((float)Math.PI,0,1,0);
            matrices.translate(x,y,z);
            for (Mesh mesh : raw) {
                Material material=mesh.material.shader==ShaderType.CUTOUT?new Material(shader,mesh.material.texture,mesh.material.color):mesh.material;
                List<float[]> faces=new ArrayList<>(); mesh.faces.forEach(face -> faces.add(face.clone()));
                Mesh copy=new Mesh(material,faces); transform(Collections.singletonList(copy),matrices,false); placed.add(copy);
            }
        }
        public void applyTranslation(double x,double y,double z) {
            LegacyMatrices matrices=new LegacyMatrices(); matrices.translate(x,y,z); transform(raw,matrices,false);
        }
        public void applyRotation(double x,double y,double z) {
            LegacyMatrices matrices=new LegacyMatrices();
            matrices.rotate((float)Math.toRadians(z),0,0,1); matrices.rotate((float)Math.toRadians(y),0,1,0); matrices.rotate((float)Math.toRadians(x),1,0,0);
            transform(raw,matrices,false);
        }
        public void applyScale(double x,double y,double z) {
            LegacyMatrices matrices=new LegacyMatrices(); matrices.scale((float)x,(float)y,(float)z); transform(raw,matrices,x*y*z<0);
        }
        public void applyMirror(boolean x,boolean y,boolean z) { applyScale(x?-1:1,y?-1:1,z?-1:1); }
        public float getMinX() { return bounds[0]; }
        public float getMinY() { return bounds[1]; }
        public float getMinZ() { return bounds[2]; }
        public float getMaxX() { return bounds[3]; }
        public float getMaxY() { return bounds[4]; }
        public float getMaxZ() { return bounds[5]; }
        private static void transform(List<Mesh> meshes,LegacyMatrices matrices,boolean reverse) {
            Matrix4f matrix=matrices.position();
            for (Mesh mesh : meshes) for (float[] face : mesh.faces) {
                for (int i=0;i<24;i+=8) {
                    float x=face[i],y=face[i+1],z=face[i+2];
                    face[i]=matrix.m00*x+matrix.m10*y+matrix.m20*z+matrix.m30;
                    face[i+1]=matrix.m01*x+matrix.m11*y+matrix.m21*z+matrix.m31;
                    face[i+2]=matrix.m02*x+matrix.m12*y+matrix.m22*z+matrix.m32;
                    Vector3f normal=matrices.normal(face[i+5],face[i+6],face[i+7]);
                    face[i+5]=normal.x; face[i+6]=normal.y; face[i+7]=normal.z;
                }
                if (reverse) for (int i=0;i<8;i++) { float value=face[i]; face[i]=face[16+i]; face[16+i]=value; }
                System.arraycopy(face,16,face,24,8);
            }
        }
    }
    static final class Mesh {
        final Material material; final List<float[]> faces;
        Mesh(Material material,List<float[]> faces) { this.material=material; this.faces=Collections.unmodifiableList(faces); }
    }
    static final class Material {
        final ShaderType shader; final Identifier texture; final int color;
        Material(ShaderType shader,Identifier texture,int color) { this.shader=Objects.requireNonNull(shader); this.texture=texture; this.color=color; }
        RenderLayer layer() {
            return RenderLayer.forCompiledModel(texture,shader.translucent,shader.glowing);
        }
        @Override public boolean equals(Object value) {
            if (!(value instanceof Material)) return false; Material other=(Material)value;
            return shader==other.shader && color==other.color && Objects.equals(texture,other.texture);
        }
        @Override public int hashCode() { return Objects.hash(shader,texture,color); }
    }
    public enum ShaderType {
        CUTOUT(false,false,false),TRANSLUCENT(true,false,false),CUTOUT_BRIGHT(false,true,false),TRANSLUCENT_BRIGHT(true,true,false),CUTOUT_GLOWING(false,true,true),TRANSLUCENT_GLOWING(true,true,true);
        final boolean translucent,bright,glowing;
        ShaderType(boolean translucent,boolean bright,boolean glowing) { this.translucent=translucent; this.bright=bright; this.glowing=glowing; }
    }
}
