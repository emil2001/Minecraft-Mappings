package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacyBlockModels;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BlockModelCompatibilityTest {
    @Test void particleAliasesInheritAndParticipateInTextureStitching() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/block/base.json","{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"mtr:block/red\",\"particle\":\"#dust\"}}");
        resources.put("mtr:models/block/child.json","{\"parent\":\"mtr:block/base\",\"textures\":{\"dust\":\"mtr:block/green\"}}");
        resources.put("mtr:blockstates/probe.json","{\"variants\":{\"\":{\"model\":\"mtr:block/child\"}}}");
        LegacyBlockModels models=new LegacyBlockModels(id -> resources.get(id.toString()));
        assertEquals(new Identifier("mtr:block/green"),models.particle(new Identifier("mtr:probe"),Collections.emptyMap(),0));
        assertEquals(new HashSet<>(Arrays.asList(new Identifier("mtr:block/red"),new Identifier("mtr:block/green"))),models.textures(new Identifier("mtr:probe")));
        resources.put("mtr:models/block/child.json","{\"parent\":\"mtr:block/base\",\"textures\":{\"dust\":\"#particle\"}}");
        LegacyBlockModels cyclic=new LegacyBlockModels(id -> resources.get(id.toString()));
        assertThrows(IllegalArgumentException.class,() -> cyclic.particle(new Identifier("mtr:probe"),Collections.emptyMap(),0));
    }
    @Test void independentChunkWorkersCanDecodeResourcesConcurrently() throws Exception {
        java.util.concurrent.CountDownLatch firstStarted=new java.util.concurrent.CountDownLatch(1),secondStarted=new java.util.concurrent.CountDownLatch(1),firstFinished=new java.util.concurrent.CountDownLatch(1);
        LegacyBlockModels models=new LegacyBlockModels(id -> {
            try {
                if (id.getPath().equals("blockstates/concurrent_a.json")) { firstStarted.countDown(); assertTrue(secondStarted.await(5,java.util.concurrent.TimeUnit.SECONDS)); }
                if (id.getPath().equals("blockstates/concurrent_b.json")) { secondStarted.countDown(); assertTrue(firstFinished.await(5,java.util.concurrent.TimeUnit.SECONDS)); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            return id.getPath().startsWith("blockstates/")?"{\"variants\":{\"\":{\"model\":\"mtr:block/shared\"}}}":"{\"elements\":[]}";
        });
        java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<List<LegacyBlockModels.Quad>> a=workers.submit(() -> {
                try { return models.resolve(new Identifier("mtr:concurrent_a"),Collections.emptyMap(),0); }
                finally { firstFinished.countDown(); }
            });
            assertTrue(firstStarted.await(5,java.util.concurrent.TimeUnit.SECONDS));
            java.util.concurrent.Future<List<LegacyBlockModels.Quad>> b=workers.submit(() -> models.resolve(new Identifier("mtr:concurrent_b"),Collections.emptyMap(),0));
            assertTrue(a.get(6,java.util.concurrent.TimeUnit.SECONDS).isEmpty()); assertTrue(b.get(6,java.util.concurrent.TimeUnit.SECONDS).isEmpty());
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS)); }
    }
    @Test void ambientOcclusionAndWeightedVariantsSurviveInheritance() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/block/base.json","{\"ambientocclusion\":false,\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"mtr:block/red\"}}");
        resources.put("mtr:models/block/child.json","{\"parent\":\"mtr:block/base\",\"textures\":{\"all\":\"mtr:block/blue\"}}");
        resources.put("mtr:blockstates/probe.json","{\"variants\":{\"\":[{\"model\":\"mtr:block/base\",\"weight\":2},{\"model\":\"mtr:block/child\",\"weight\":1}]}}");
        LegacyBlockModels models=new LegacyBlockModels(id -> resources.get(id.toString()));
        for (int seed=0;seed<3;seed++) {
            LegacyBlockModels.Quad quad=models.resolve(new Identifier("mtr:probe"),Collections.emptyMap(),seed).get(0);
            assertEquals(new Identifier(seed==2?"mtr:block/blue":"mtr:block/red"),quad.texture); assertFalse(quad.ambientOcclusion);
        }
    }
    @Test void parentTexturesFaceUvsAndStateRotationKeepGeometry() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/block/parent.json","{\"textures\":{\"paint\":\"#all\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,8,4],\"faces\":{\"north\":{\"texture\":\"#paint\",\"cullface\":\"north\",\"tintindex\":2,\"rotation\":90}}}]}");
        resources.put("mtr:models/block/child.json","{\"parent\":\"mtr:block/parent\",\"textures\":{\"all\":\"mtr:block/red\"}}");
        resources.put("mtr:blockstates/probe.json","{\"variants\":{\"facing=north\":{\"model\":\"mtr:block/child\"},\"facing=east\":{\"model\":\"mtr:block/child\",\"y\":90}}}");
        LegacyBlockModels models=new LegacyBlockModels(id -> resources.get(id.toString()));
        LegacyBlockModels.Quad north=models.resolve(new Identifier("mtr:probe"),Collections.singletonMap("facing","north"),0).get(0);
        assertEquals(new Identifier("mtr:block/red"),north.texture); assertEquals(Direction.NORTH,north.cull); assertEquals(2,north.tint); assertTrue(north.shade);
        assertEquals(1,north.vertices[0],1E-5); assertEquals(.5,north.vertices[1],1E-5); assertEquals(0,north.vertices[2],1E-5);
        assertEquals(0,north.vertices[3],1E-5); assertEquals(1,north.vertices[4],1E-5);
        LegacyBlockModels.Quad east=models.resolve(new Identifier("mtr:probe"),Collections.singletonMap("facing","east"),0).get(0);
        assertEquals(Direction.EAST,east.cull); assertEquals(1,east.vertices[0],1E-5); assertEquals(.5,east.vertices[1],1E-5); assertEquals(1,east.vertices[2],1E-5);
        assertEquals(1,east.vertices[5],1E-5); assertEquals(0,east.vertices[7],1E-5);
    }
    @Test void multipartOrAndPropertyAlternativesSelectOnlyMatchingParts() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/block/base.json","{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"mtr:block/white\"}}");
        resources.put("mtr:blockstates/probe.json","{\"multipart\":[{\"apply\":{\"model\":\"mtr:block/base\"}},{\"when\":{\"OR\":[{\"side\":\"left|right\",\"open\":\"true\"},{\"facing\":\"north\"}]},\"apply\":{\"model\":\"mtr:block/base\",\"y\":180}}]}");
        LegacyBlockModels models=new LegacyBlockModels(id -> resources.get(id.toString()));
        Map<String,String> state=new HashMap<>(); state.put("side","left"); state.put("open","false"); state.put("facing","south");
        assertEquals(6,models.resolve(new Identifier("mtr:probe"),state,0).size());
        state.put("open","true"); assertEquals(12,models.resolve(new Identifier("mtr:probe"),state,0).size());
        state.put("side","middle"); assertEquals(6,models.resolve(new Identifier("mtr:probe"),state,0).size());
        state.put("facing","north"); assertEquals(12,models.resolve(new Identifier("mtr:probe"),state,0).size());
        resources.put("mtr:models/block/cycle.json","{\"parent\":\"mtr:block/cycle\"}");
        assertThrows(IllegalArgumentException.class,() -> models.model(new Identifier("mtr:block/cycle")));
    }
    @Test void elementRotationRescaleAndExplicitUvsArePreserved() {
        LegacyBlockModels models=new LegacyBlockModels(id -> "{\"textures\":{\"all\":\"mtr:block/white\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,0],\"shade\":false,\"rotation\":{\"angle\":45,\"axis\":\"z\",\"origin\":[8,8,0],\"rescale\":true},\"faces\":{\"south\":{\"texture\":\"#all\",\"uv\":[-8,0,8,16]}}}]}");
        LegacyBlockModels.Quad quad=models.model(new Identifier("mtr:block/probe")).get(0);
        assertFalse(quad.shade); assertEquals(-.5,quad.vertices[0],1E-5); assertEquals(.5,quad.vertices[1],1E-5);
        assertEquals(-.5,quad.vertices[3],1E-5); assertEquals(0,quad.vertices[4],1E-5);
        assertEquals(0,quad.vertices[5],1E-5); assertEquals(1,quad.vertices[7],1E-5);
    }
    @Test void allReferencedOriginalBlockModelsAndVariantsLoadWithoutModernMinecraftResources() throws Exception {
        Path assets=Paths.get(System.getProperty("mtr.modelResources")).getParent();
        LegacyBlockModels models=new LegacyBlockModels(id -> {
            if (!id.getNamespace().equals("mtr")) return null;
            try { return new String(Files.readAllBytes(assets.resolve(id.getPath())),StandardCharsets.UTF_8); }
            catch (Exception e) { throw new IllegalArgumentException(id.toString(),e); }
        });
        int count=0;
        try (java.util.stream.Stream<Path> files=Files.list(assets.resolve("models/block"))) {
            for (Path path : (Iterable<Path>)files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                assertTrue(new com.google.gson.JsonParser().parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).isJsonObject());
                count++;
            }
        }
        assertEquals(576,count);
        com.google.gson.JsonParser parser=new com.google.gson.JsonParser(); int states=0;
        try (java.util.stream.Stream<Path> files=Files.list(assets.resolve("blockstates"))) {
            for (Path path : (Iterable<Path>)files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                com.google.gson.JsonObject json=parser.parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject();
                validateReferences(json,models);
                Identifier id=new Identifier("mtr:"+path.getFileName().toString().replace(".json",""));
                assertNotNull(models.textures(id),path.toString());
                if (json.has("variants")) for (Map.Entry<String,com.google.gson.JsonElement> entry : json.getAsJsonObject("variants").entrySet()) {
                    Map<String,String> state=new HashMap<>();
                    if (!entry.getKey().isEmpty()) for (String property : entry.getKey().split(",")) { String[] pair=property.split("=",2); state.put(pair[0],pair[1]); }
                    assertNotNull(models.resolve(id,state,0),path.toString());
                } else assertNotNull(models.resolve(id,Collections.emptyMap(),0),path.toString());
                states++;
            }
        }
        assertEquals(193,states);
        List<LegacyBlockModels.Quad> station=models.resolve(new Identifier("mtr:station_color_concrete"),Collections.emptyMap(),0);
        assertEquals(6,station.size()); station.forEach(quad -> assertEquals(0,quad.tint,"Vanilla leaves parent carries the station color tint"));
    }
    @Test void uvLockKeepsEveryCubeFaceWorldAlignedThroughAllBlockRotations() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/block/locked.json","{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"mtr:block/paint\"}}");
        StringJoiner variants=new StringJoiner(",","{\"variants\":{","}}");
        for (int x=0;x<360;x+=90) for (int y=0;y<360;y+=90) for (boolean lock : new boolean[]{false,true})
            variants.add("\"case="+x+"_"+y+"_"+lock+"\":{\"model\":\"mtr:block/locked\",\"x\":"+x+",\"y\":"+y+",\"uvlock\":"+lock+"}");
        resources.put("mtr:blockstates/probe.json",variants.toString());
        LegacyBlockModels models=new LegacyBlockModels(id -> resources.get(id.toString()));
        List<LegacyBlockModels.Quad> source=models.model(new Identifier("mtr:block/locked"));
        List<float[]> cached=new ArrayList<>(); source.forEach(quad -> cached.add(quad.vertices.clone()));
        for (int x=0;x<360;x+=90) for (int y=0;y<360;y+=90) for (boolean lock : new boolean[]{false,true}) {
            String key=x+"_"+y+"_"+lock;
            List<LegacyBlockModels.Quad> actual=models.resolve(new Identifier("mtr:probe"),Collections.singletonMap("case",key),0);
            assertEquals(6,actual.size(),key);
            for (int face=0;face<source.size();face++) {
                LegacyBlockModels.Quad quad=actual.get(face); assertRotatedGeometry(source.get(face),quad,x,y,key);
                for (int i=0;i<32;i+=8) {
                    float[] uv=lock?faceUv(quad.cull,quad.vertices[i],quad.vertices[i+1],quad.vertices[i+2]):new float[]{source.get(face).vertices[i+3],source.get(face).vertices[i+4]};
                    assertEquals(uv[0],quad.vertices[i+3],1E-5,key+" U"); assertEquals(uv[1],quad.vertices[i+4],1E-5,key+" V");
                }
            }
        }
        for (int face=0;face<source.size();face++) assertArrayEquals(cached.get(face),source.get(face).vertices,0F,"Cached model must remain unrotated");
    }
    @Test void uvLockKeepsCroppedMirroredAndRotatedFaceUvsCompatible() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/block/locked.json","{\"ambientocclusion\":false,\"textures\":{\"all\":\"mtr:block/paint\"},\"elements\":["
            +"{\"from\":[0,0,0],\"to\":[16,16,16],\"shade\":false,\"faces\":{\"up\":{\"texture\":\"#all\",\"uv\":[2,4,10,14],\"cullface\":\"west\",\"tintindex\":2}}},"
            +"{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{\"up\":{\"texture\":\"#all\",\"uv\":[10,4,2,14],\"rotation\":90}}},"
            +"{\"from\":[0,0,0],\"to\":[16,16,16],\"rotation\":{\"origin\":[8,8,8],\"axis\":\"y\",\"angle\":45},\"faces\":{\"north\":{\"texture\":\"#all\",\"uv\":[2,4,10,14],\"rotation\":270,\"cullface\":\"north\"}}}]}");
        resources.put("mtr:blockstates/probe.json","{\"variants\":{\"turn=y\":{\"model\":\"mtr:block/locked\",\"y\":90,\"uvlock\":true},\"turn=x\":{\"model\":\"mtr:block/locked\",\"x\":90,\"uvlock\":true}}}");
        LegacyBlockModels models=new LegacyBlockModels(id -> resources.get(id.toString()));
        List<LegacyBlockModels.Quad> source=models.model(new Identifier("mtr:block/locked"));
        List<float[]> cached=new ArrayList<>(); source.forEach(quad -> cached.add(quad.vertices.clone()));
        List<LegacyBlockModels.Quad> y=models.resolve(new Identifier("mtr:probe"),Collections.singletonMap("turn","y"),0);
        List<LegacyBlockModels.Quad> x=models.resolve(new Identifier("mtr:probe"),Collections.singletonMap("turn","x"),0);
        for (int i=0;i<source.size();i++) { assertRotatedGeometry(source.get(i),y.get(i),0,90,"Y90"); assertRotatedGeometry(source.get(i),x.get(i),90,0,"X90"); }
        // Fixed expectations include endpoint signs and the explicit face UV rotation, as in modern FaceBakery.
        assertUvs(y.get(0),new float[]{.75F,.125F,.125F,.125F,.125F,.625F,.75F,.625F});
        assertUvs(y.get(1),new float[]{.125F,.625F,.125F,.125F,.75F,.125F,.75F,.625F});
        // Element rotation tilts the normal, but UV-lock and cullface still use the JSON north face.
        assertUvs(x.get(2),new float[]{.875F,.125F,.375F,.125F,.375F,.75F,.875F,.75F});
        assertEquals(Direction.NORTH,y.get(0).cull); assertNull(y.get(1).cull); assertEquals(Direction.DOWN,x.get(2).cull);
        for (int i=0;i<source.size();i++) assertArrayEquals(cached.get(i),source.get(i).vertices,0F,"Locked resolve must not change cached UVs");
    }
    @Test void allBundledSignalUvLockedPartsKeepTheirRealModelsAndWorldFacingUvs() throws Exception {
        Path assets=Paths.get(System.getProperty("mtr.modelResources")).getParent();
        LegacyBlockModels models=new LegacyBlockModels(id -> {
            if (!id.getNamespace().equals("mtr")) return null;
            try { return new String(Files.readAllBytes(assets.resolve(id.getPath())),StandardCharsets.UTF_8); }
            catch (Exception e) { throw new IllegalArgumentException(id.toString(),e); }
        });
        String[] names={"signal_light_1","signal_light_2","signal_light_3","signal_light_4","signal_light_3_aspect_1","signal_light_3_aspect_2",
            "signal_light_4_aspect_1","signal_light_4_aspect_2","signal_semaphore_1","signal_semaphore_2"};
        com.google.gson.JsonParser parser=new com.google.gson.JsonParser(); int locked=0;
        for (String name : names) {
            com.google.gson.JsonArray parts=parser.parse(new String(Files.readAllBytes(assets.resolve("blockstates/"+name+".json")),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("multipart");
            for (com.google.gson.JsonElement value : parts) {
                com.google.gson.JsonObject target=value.getAsJsonObject(),apply=target.getAsJsonObject("apply");
                if (!apply.has("uvlock") || !apply.get("uvlock").getAsBoolean()) continue;
                Map<String,String> properties=new HashMap<>(); properties.put("facing","north"); properties.put("is_22_5","false"); properties.put("is_45","false");
                // A state property holds one value; a JSON condition may list alternatives such as north|south.
                target.getAsJsonObject("when").entrySet().forEach(entry -> properties.put(entry.getKey(),entry.getValue().getAsString().split("\\|")[0]));
                String label=name+properties;
                List<LegacyBlockModels.Quad> actual=models.resolve(new Identifier("mtr:"+name),properties,0); int offset=0; boolean targetReached=false;
                for (com.google.gson.JsonElement sibling : parts) {
                    com.google.gson.JsonObject part=sibling.getAsJsonObject(); if (!signalPartMatches(part,properties)) continue;
                    com.google.gson.JsonObject selected=part.getAsJsonObject("apply"); Identifier modelId=new Identifier(selected.get("model").getAsString());
                    List<LegacyBlockModels.Quad> source=models.model(modelId);
                    if (sibling==value) {
                        targetReached=true;
                        com.google.gson.JsonObject json=parser.parse(new String(Files.readAllBytes(assets.resolve("models/"+modelId.getPath()+".json")),StandardCharsets.UTF_8)).getAsJsonObject();
                        List<Direction> sides=new ArrayList<>();
                        for (com.google.gson.JsonElement element : json.getAsJsonArray("elements")) for (Map.Entry<String,com.google.gson.JsonElement> face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) sides.add(Direction.valueOf(face.getKey().toUpperCase(Locale.ROOT)));
                        assertEquals(source.size(),sides.size(),label);
                        int x=apply.has("x")?apply.get("x").getAsInt():0,y=apply.has("y")?apply.get("y").getAsInt():0;
                        for (int q=0;q<source.size();q++) {
                            LegacyBlockModels.Quad quad=actual.get(offset+q); assertRotatedGeometry(source.get(q),quad,x,y,label);
                            Direction worldFace=rotatedDirection(sides.get(q),x,y);
                            for (int vertex=0;vertex<32;vertex+=8) {
                                float[] point=facePoint(sides.get(q),source.get(q).vertices[vertex+3],source.get(q).vertices[vertex+4]);
                                float[] rotated=rotate(point[0],point[1],point[2],x,y,true),uv=faceUv(worldFace,rotated[0],rotated[1],rotated[2]);
                                assertEquals(uv[0],quad.vertices[vertex+3],1E-5,label+" U"); assertEquals(uv[1],quad.vertices[vertex+4],1E-5,label+" V");
                            }
                        }
                    }
                    offset+=source.size();
                }
                assertTrue(targetReached,label+" must select its locked apply"); assertEquals(offset,actual.size(),label); locked++;
            }
        }
        assertEquals(128,locked,"Reach every locked multipart selection in the ten bundled signal blockstates");
    }
    private static boolean signalPartMatches(com.google.gson.JsonObject part,Map<String,String> properties) {
        if (part.has("when")) for (Map.Entry<String,com.google.gson.JsonElement> condition : part.getAsJsonObject("when").entrySet())
            if (!Arrays.asList(condition.getValue().getAsString().split("\\|")).contains(properties.get(condition.getKey()))) return false;
        return true;
    }
    private static void assertUvs(LegacyBlockModels.Quad quad,float[] expected) {
        for (int i=0;i<4;i++) { assertEquals(expected[i*2],quad.vertices[i*8+3],1E-5,"U vertex "+i); assertEquals(expected[i*2+1],quad.vertices[i*8+4],1E-5,"V vertex "+i); }
    }
    private static void assertRotatedGeometry(LegacyBlockModels.Quad source,LegacyBlockModels.Quad actual,int x,int y,String label) {
        assertEquals(source.texture,actual.texture,label); assertEquals(source.tint,actual.tint,label); assertEquals(source.shade,actual.shade,label); assertEquals(source.ambientOcclusion,actual.ambientOcclusion,label);
        assertEquals(source.cull==null?null:rotatedDirection(source.cull,x,y),actual.cull,label+" cullface");
        for (int i=0;i<32;i+=8) {
            float[] position=rotate(source.vertices[i],source.vertices[i+1],source.vertices[i+2],x,y,true),normal=rotate(source.vertices[i+5],source.vertices[i+6],source.vertices[i+7],x,y,false);
            for (int axis=0;axis<3;axis++) { assertEquals(position[axis],actual.vertices[i+axis],1E-5,label+" position"); assertEquals(normal[axis],actual.vertices[i+5+axis],1E-5,label+" normal"); }
        }
    }
    private static Direction rotatedDirection(Direction side,int x,int y) {
        float[] normal=rotate(side.getOffsetX(),side.getOffsetY(),side.getOffsetZ(),x,y,false);
        for (Direction candidate : Direction.values()) if (candidate.getOffsetX()==Math.round(normal[0]) && candidate.getOffsetY()==Math.round(normal[1]) && candidate.getOffsetZ()==Math.round(normal[2])) return candidate;
        throw new AssertionError("Not a cardinal normal");
    }
    private static float[] rotate(float x,float y,float z,int rx,int ry,boolean position) {
        float center=position?.5F:0; x-=center; y-=center; z-=center;
        for (int i=0;i<rx;i+=90) { float old=y; y=z; z=-old; }
        for (int i=0;i<ry;i+=90) { float old=x; x=-z; z=old; }
        return new float[]{x+center,y+center,z+center};
    }
    private static float[] faceUv(Direction face,float x,float y,float z) {
        switch (face) {
            case DOWN: return new float[]{x,1-z}; case UP: return new float[]{x,z}; case NORTH: return new float[]{1-x,1-y};
            case SOUTH: return new float[]{x,1-y}; case WEST: return new float[]{z,1-y}; default: return new float[]{1-z,1-y};
        }
    }
    private static float[] facePoint(Direction face,float u,float v) {
        switch (face) {
            case DOWN: return new float[]{u,0,1-v}; case UP: return new float[]{u,1,v}; case NORTH: return new float[]{1-u,1-v,0};
            case SOUTH: return new float[]{u,1-v,1}; case WEST: return new float[]{0,1-v,u}; default: return new float[]{1,1-v,1-u};
        }
    }
    private static void validateReferences(com.google.gson.JsonElement element,LegacyBlockModels models) {
        if (element.isJsonObject()) for (Map.Entry<String,com.google.gson.JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (entry.getKey().equals("model")) {
                for (LegacyBlockModels.Quad quad : models.model(new Identifier(entry.getValue().getAsString())))
                    for (float value : quad.vertices) assertTrue(Float.isFinite(value),entry.getValue().getAsString());
            } else validateReferences(entry.getValue(),models);
        } else if (element.isJsonArray()) for (com.google.gson.JsonElement child : element.getAsJsonArray()) validateReferences(child,models);
    }
}
