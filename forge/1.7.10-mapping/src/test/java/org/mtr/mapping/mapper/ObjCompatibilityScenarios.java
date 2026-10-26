package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.Identifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class ObjCompatibilityScenarios {
    private static final String TRIANGLE="v 1 2 3\nv 5 2 3\nv 1 6 3\nvt .2 .3\nvt .8 .3\nvt .2 .9\nf -3/-3 -2/-2 -1/-1\n";
    private static final Identifier WHITE=new Identifier("mtr:test_white");
    public void groupsMaterialsAndBounds() {
        String obj="mtllib dir\\test.mtl\n"+TRIANGLE.replace("f -3", "g first\nusemtl paint#exteriortranslucent,flipv\nf -3")+
            "g second\nusemtl solid\nf 1/1 2/2 3/3\n";
        List<String> paths=new ArrayList<>();
        Map<String,OptimizedModel.ObjModel> models=OptimizedModel.ObjModel.loadModel(obj,path -> {
            assertEquals("dir/test.mtl",path);
            return "newmtl paint#exteriortranslucent,flipv\nKd .5 .25 0\nd .5\nmap_Kd dir\\paint.png\nnewmtl solid\nKd 1 1 1\n";
        },path -> { paths.add(path); return WHITE; },null,true,false);
        assertEquals(new HashSet<>(Arrays.asList("first","second")),models.keySet());
        assertTrue(paths.contains("dir/paint.png")); assertTrue(paths.contains(""));
        OptimizedModel.ObjModel first=models.get("first");
        assertEquals(1,first.getMinX(),1E-5); assertEquals(5,first.getMaxX(),1E-5);
        assertEquals(-6,first.getMinY(),1E-5); assertEquals(-2,first.getMaxY(),1E-5);
        assertEquals(-3,first.getMinZ(),1E-5); assertEquals(-3,first.getMaxZ(),1E-5);
        assertTrue(OptimizedModel.fromObjModels(Collections.singleton(first)).meshes.isEmpty(),"Definitions require explicit placement");
        first.addTransformation(OptimizedModel.ShaderType.CUTOUT_BRIGHT,0,0,0,false);
        OptimizedModel compiled=OptimizedModel.fromObjModels(Collections.singleton(first));
        assertEquals(1,compiled.meshes.size()); OptimizedModel.Mesh mesh=compiled.meshes.get(0);
        assertEquals(OptimizedModel.ShaderType.TRANSLUCENT,mesh.material.shader,"Explicit material shader survives placement override");
        assertEquals(0x7F7F3F00,mesh.material.color); assertEquals(WHITE,mesh.material.texture);
        float[] face=mesh.faces.get(0); assertEquals(.7,face[4],1E-5); assertEquals(-1,face[7],1E-5);
        assertArrayEquals(Arrays.copyOfRange(face,16,24),Arrays.copyOfRange(face,24,32));
        assertThrows(UnsupportedOperationException.class,() -> OptimizedModel.ObjModel.loadModel(TRIANGLE,p -> "",p -> WHITE,new Identifier("mtr:atlas"),false,false));
        assertTrue(OptimizedModel.ObjModel.loadModel("v broken",p -> "",p -> WHITE,null,false,false).isEmpty());
    }
    public void transformsAndPlacementSnapshots() {
        OptimizedModel.ObjModel model=load(TRIANGLE,false);
        model.applyRotation(90,90,90); model.applyScale(-2,3,4); model.applyTranslation(2,4,6);
        model.addTransformation(OptimizedModel.ShaderType.CUTOUT_GLOWING,10,20,30,true);
        OptimizedModel first=OptimizedModel.fromObjModels(Collections.singleton(model));
        float[] face=first.meshes.get(0).faces.get(0);
        // Upstream Face.flip reverses all three triangle corners.
        assertEquals(-18,face[0],1E-4); assertEquals(6,face[1],1E-4); assertEquals(-32,face[2],1E-4);
        assertEquals(OptimizedModel.ShaderType.CUTOUT_GLOWING,first.meshes.get(0).material.shader);
        float[] snapshot=face.clone(); model.applyTranslation(100,0,0);
        assertArrayEquals(snapshot,first.meshes.get(0).faces.get(0));
        assertEquals(1,model.getMinX(),1E-5); assertEquals(-6,model.getMinY(),1E-5);
        OptimizedModel.ObjModel mirrored=load(TRIANGLE,false); mirrored.applyMirror(true,false,false);
        mirrored.addTransformation(OptimizedModel.ShaderType.CUTOUT,0,0,0,false);
        mirrored.addTransformation(OptimizedModel.ShaderType.CUTOUT,0,0,0,false);
        OptimizedModel distinct=OptimizedModel.fromObjModels(Collections.singleton(mirrored));
        assertEquals(1,distinct.meshes.get(0).faces.size(),"Identical placements merge within a model");
        face=distinct.meshes.get(0).faces.get(0);
        assertEquals(-1,face[0],1E-5); assertEquals(-6,face[1],1E-5); assertEquals(-5,face[8],1E-5); assertEquals(-2,face[9],1E-5);
        assertEquals(-1,face[7],1E-5); assertEquals(.9,face[4],1E-5);
        OptimizedModel.ObjModel flipped=load(TRIANGLE,true); flipped.addTransformation(OptimizedModel.ShaderType.CUTOUT,0,0,0,false);
        assertEquals(.7,OptimizedModel.fromObjModels(Collections.singleton(flipped)).meshes.get(0).faces.get(0)[4],1E-5);
    }
    public void bundledModelsCompile() throws Exception {
        for (String name : Arrays.asList("object/teapot.obj","rail/rail.obj","rail/rail_siding.obj")) {
            String obj=new String(Files.readAllBytes(Paths.get(System.getProperty("mtr.modelResources"),name)),StandardCharsets.UTF_8);
            Map<String,OptimizedModel.ObjModel> parts=OptimizedModel.ObjModel.loadModel(obj,p -> "",p -> WHITE,null,true,false);
            assertFalse(parts.isEmpty(),name);
            parts.values().forEach(part -> part.addTransformation(OptimizedModel.ShaderType.CUTOUT,0,0,0,false));
            OptimizedModel compiled=OptimizedModel.fromObjModels(parts.values());
            assertTrue(compiled.meshes.stream().mapToInt(mesh -> mesh.faces.size()).sum()>0,name);
            for (OptimizedModel.Mesh mesh : compiled.meshes) for (float[] face : mesh.faces)
                for (float value : face) assertTrue(Float.isFinite(value),name);
        }
    }
    public void originalModelResourceLoaderAndWrapper() {
        String obj="mtllib colors.mtl\ng cabin\nusemtl body\n"+TRIANGLE;
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/probe.obj",obj);
        resources.put("mtr:models/colors.mtl","newmtl body\nKd 1 .5 .25\nmap_Kd default.png\n");
        Map<String,OptimizedModel.ObjModel> models=org.mtr.mod.resource.ModelResourceLoader.loadModel("mtr:models/probe.obj",WHITE,false,id -> {
            assertTrue(resources.containsKey(id.toString()),id.toString()); return resources.get(id.toString());
        });
        assertEquals(Collections.singleton("cabin"),models.keySet());
        OptimizedModel.ObjModel model=models.get("cabin");
        org.mtr.mod.resource.OptimizedModelWrapper.ObjModelWrapper wrapper=new org.mtr.mod.resource.OptimizedModelWrapper.ObjModelWrapper(model);
        wrapper.addTransformation(OptimizedModel.ShaderType.CUTOUT_BRIGHT,0,0,0,false);
        OptimizedModel compiled=OptimizedModel.fromObjModels(Collections.singleton(wrapper.objModel));
        assertEquals(WHITE,compiled.meshes.get(0).material.texture); assertEquals(0xFFFF7F3F,compiled.meshes.get(0).material.color);
        org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<org.mtr.mod.resource.OptimizedModelWrapper.ObjModelWrapper> wrapped=new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<>();
        wrapped.add(wrapper); assertNotNull(org.mtr.mod.resource.OptimizedModelWrapper.fromObjModels(wrapped));
        assertEquals(Collections.singletonList("cabin"),org.mtr.mod.resource.ModelResourceLoader.getModelParts("probe.obj",obj));
        Map<String,OptimizedModel.ObjModel> fallback=OptimizedModel.ObjModel.loadModel(obj,p -> { throw new IllegalArgumentException("Missing MTL fixture"); },p -> {
            assertEquals("body",p); return WHITE;
        },null,true,false);
        assertEquals(Collections.singleton("cabin"),fallback.keySet(),"Missing MTL must retain usemtl fallback");
    }
    private static OptimizedModel.ObjModel load(String obj,boolean flip) {
        return OptimizedModel.ObjModel.loadModel(obj,p -> "",p -> WHITE,null,false,flip).get("");
    }
}
