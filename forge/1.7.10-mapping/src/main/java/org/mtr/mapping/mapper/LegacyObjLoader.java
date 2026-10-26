package org.mtr.mapping.mapper;

import de.javagl.obj.*;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.DummyClass;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;

/** Loads the same patched OBJ/MTL format as modern MTR, without a GL context. */
final class LegacyObjLoader {
    static Map<String,OptimizedModel.ObjModel> load(String content,Function<String,String> mtlResolver,Function<String,Identifier> textureResolver,boolean split,boolean flipV) {
        Map<String,OptimizedModel.ObjModel> result=new LinkedHashMap<>();
        try {
            Obj obj=ObjReader.read(new StringReader(content)); Map<String,Mtl> materials=new HashMap<>();
            for (String path : obj.getMtlFileNames()) try {
                for (Mtl mtl : MtlReader.read(new StringReader(mtlResolver.apply(path.replace('\\','/'))))) materials.put(mtl.getName(),mtl);
            } catch (Exception e) { DummyClass.logException(e); }
            Map<String,Obj> groups=split?ObjSplitting.splitByGroups(obj):Collections.singletonMap("",obj);
            for (Map.Entry<String,Obj> group : groups.entrySet()) result.put(group.getKey(),loadPart(group.getValue(),materials,textureResolver,flipV));
        } catch (Exception e) { DummyClass.logException(e); }
        return result;
    }
    private static OptimizedModel.ObjModel loadPart(Obj obj,Map<String,Mtl> materials,Function<String,Identifier> textures,boolean globalFlip) {
        List<OptimizedModel.Mesh> meshes=new ArrayList<>();
        float[] bounds={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        Map<String,Obj> groups=ObjSplitting.splitByMaterialGroups(obj);
        if (groups.isEmpty() && obj.getNumFaces()>0) groups.put("",obj);
        for (Map.Entry<String,Obj> group : groups.entrySet()) {
            if (group.getValue().getNumFaces()==0) continue;
            Map<String,String> options=options(group.getKey());
            String texture=""; int color=-1; Mtl mtl=materials.get(group.getKey());
            if (materials.isEmpty()) texture=options.get("").equals("_")?"":options.get("");
            else if (mtl!=null) {
                if (mtl.getMapKd()!=null) texture=mtl.getMapKd();
                FloatTuple kd=mtl.getKd();
                if (kd!=null) color=((mtl.getD()==null?255:(int)(mtl.getD()*255))<<24) | ((int)(kd.getX()*255)<<16) | ((int)(kd.getY()*255)<<8) | (int)(kd.getZ()*255);
            }
            OptimizedModel.Material material=new OptimizedModel.Material(shader(options.getOrDefault("#","")),textures.apply(texture.replace('\\','/')),color);
            Obj renderable=ObjUtils.convertToRenderable(group.getValue());
            float[][] vertices=new float[renderable.getNumVertices()][8];
            boolean flipV=globalFlip^options.getOrDefault("flipv","0").equals("1");
            for (int i=0;i<vertices.length;i++) {
                FloatTuple position=renderable.getVertex(i); float[] vertex=vertices[i];
                // The upstream loader rotates X by 180 degrees before calculating bounds.
                vertex[0]=position.getX(); vertex[1]=-position.getY(); vertex[2]=-position.getZ();
                if (i<renderable.getNumTexCoords()) {
                    FloatTuple uv=renderable.getTexCoord(i); vertex[3]=uv.getX(); vertex[4]=uv.getY();
                }
                if (flipV) vertex[4]=1-vertex[4];
                if (i<renderable.getNumNormals()) {
                    FloatTuple normal=renderable.getNormal(i); vertex[5]=normal.getX(); vertex[6]=-normal.getY(); vertex[7]=-normal.getZ();
                }
                for (int axis=0;axis<3;axis++) { bounds[axis]=Math.min(bounds[axis],vertex[axis]); bounds[axis+3]=Math.max(bounds[axis+3],vertex[axis]); }
            }
            List<float[]> faces=new ArrayList<>();
            for (int i=0;i<renderable.getNumFaces();i++) {
                ObjFace triangle=renderable.getFace(i); float[] face=new float[32];
                // A degenerate quad preserves native 1.7.10 tessellator sorting for triangles.
                for (int j=0;j<4;j++) System.arraycopy(vertices[triangle.getVertexIndex(Math.min(j,2))],0,face,j*8,8);
                faces.add(face);
            }
            meshes.add(new OptimizedModel.Mesh(material,faces));
        }
        return new OptimizedModel.ObjModel(meshes,bounds);
    }
    private static Map<String,String> options(String name) {
        Map<String,String> options=new HashMap<>(); String[] major=name.split("#",2); options.put("",major[0]);
        if (major.length>1) for (String option : major[1].split(",")) {
            String[] pair=option.split("=",2);
            if (pair.length>1) options.put(pair[0],pair[1]);
            else if (!options.containsKey("#")) options.put("#",pair[0]);
            else options.put(pair[0].toLowerCase(Locale.ROOT),"1");
        }
        return options;
    }
    private static OptimizedModel.ShaderType shader(String value) {
        switch (value.toLowerCase(Locale.ENGLISH)) {
            case "exteriortranslucent": return OptimizedModel.ShaderType.TRANSLUCENT;
            case "light": return OptimizedModel.ShaderType.CUTOUT_GLOWING;
            case "always_on_light": case "lighttranslucent": return OptimizedModel.ShaderType.TRANSLUCENT_GLOWING;
            case "interior": return OptimizedModel.ShaderType.CUTOUT_BRIGHT;
            case "interior_translucent": case "interiortranslucent": return OptimizedModel.ShaderType.TRANSLUCENT_BRIGHT;
            default: return OptimizedModel.ShaderType.CUTOUT;
        }
    }
}
