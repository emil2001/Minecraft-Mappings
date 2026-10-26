package org.mtr.mapping.tool;

import com.google.gson.*;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/** CPU decoder for the block model and state resources used by MTR. A missing resource returns null. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyBlockModels {
    // Tangent axes for increasing face U and V, in Direction order.
    private static final int[][][] UV_AXES={{{1,0,0},{0,0,-1}},{{1,0,0},{0,0,1}},{{-1,0,0},{0,-1,0}},
        {{1,0,0},{0,-1,0}},{{0,0,1},{0,-1,0}},{{0,0,-1},{0,-1,0}}};
    private final Function<Identifier,String> resources;
    private final Map<Identifier,Definition> definitions=new ConcurrentHashMap<>();
    private final Map<Identifier,List<Quad>> models=new ConcurrentHashMap<>();
    private final Map<Identifier,JsonObject> states=new ConcurrentHashMap<>();
    public LegacyBlockModels(Function<Identifier,String> resources) { this.resources=resources; }
    public Set<Identifier> textures(Identifier block) {
        JsonObject state=states.computeIfAbsent(block,id -> read(new Identifier(id.getNamespace(),"blockstates/"+id.getPath()+".json")));
        Set<Identifier> result=new LinkedHashSet<>(); collectTextures(state,result); return result;
    }
    private void collectTextures(JsonElement element,Set<Identifier> result) {
        if (element.isJsonObject()) for (Map.Entry<String,JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (entry.getKey().equals("model")) {
                Identifier id=new Identifier(entry.getValue().getAsString());
                for (Quad quad : model(id)) result.add(quad.texture);
                Identifier particle=modelParticle(id); if (particle!=null) result.add(particle);
            }
            else collectTextures(entry.getValue(),result);
        } else if (element.isJsonArray()) for (JsonElement child : element.getAsJsonArray()) collectTextures(child,result);
    }
    public List<Quad> resolve(Identifier block,Map<String,String> properties,long seed) {
        List<Quad> result=new ArrayList<>();
        for (JsonElement choice : choices(block,properties)) append(result,select(choice,seed));
        return Collections.unmodifiableList(result);
    }
    public Identifier particle(Identifier block,Map<String,String> properties,long seed) {
        List<JsonElement> choices=choices(block,properties);
        return choices.isEmpty()?null:modelParticle(new Identifier(select(choices.get(0),seed).get("model").getAsString()));
    }
    private List<JsonElement> choices(Identifier block,Map<String,String> properties) {
        JsonObject state=states.computeIfAbsent(block,id -> read(new Identifier(id.getNamespace(),"blockstates/"+id.getPath()+".json")));
        List<JsonElement> result=new ArrayList<>();
        if (state.has("variants")) {
            boolean matched=false;
            for (Map.Entry<String,JsonElement> entry : state.getAsJsonObject("variants").entrySet()) if (variantMatches(entry.getKey(),properties)) {
                result.add(entry.getValue()); matched=true; break;
            }
            if (!matched) throw new IllegalArgumentException("No block model variant for "+block+properties);
        } else if (state.has("multipart")) {
            for (JsonElement element : state.getAsJsonArray("multipart")) {
                JsonObject part=element.getAsJsonObject();
                if (!part.has("when") || matches(part.getAsJsonObject("when"),properties)) result.add(part.get("apply"));
            }
        } else throw new IllegalArgumentException("Block state has neither variants nor multipart: "+block);
        return Collections.unmodifiableList(result);
    }
    public List<Quad> model(Identifier id) {
        List<Quad> cached=models.get(id); if (cached!=null) return cached;
        Definition definition=definition(id,new HashSet<>()); List<Quad> faces=new ArrayList<>();
        for (JsonElement value : definition.elements) {
            JsonObject element=value.getAsJsonObject(); float[] from=vector(element,"from"),to=vector(element,"to");
            LegacyMatrices matrices=new LegacyMatrices();
            if (element.has("rotation")) {
                JsonObject rotation=element.getAsJsonObject("rotation"); float[] origin=vector(rotation,"origin");
                String axis=rotation.get("axis").getAsString(); float angle=rotation.get("angle").getAsFloat();
                if (!Arrays.asList("x","y","z").contains(axis) || !Float.isFinite(angle) || Math.abs(angle)>45) throw new IllegalArgumentException("Invalid element rotation");
                matrices.translate(origin[0]/16,origin[1]/16,origin[2]/16);
                matrices.rotate((float)Math.toRadians(angle),axis.equals("x")?1:0,axis.equals("y")?1:0,axis.equals("z")?1:0);
                if (rotation.has("rescale") && rotation.get("rescale").getAsBoolean()) {
                    float scale=(float)(1/Math.cos(Math.toRadians(angle)));
                    matrices.scale(axis.equals("x")?1:scale,axis.equals("y")?1:scale,axis.equals("z")?1:scale);
                }
                matrices.translate(-origin[0]/16,-origin[1]/16,-origin[2]/16);
            }
            boolean shade=!element.has("shade") || element.get("shade").getAsBoolean();
            for (Map.Entry<String,JsonElement> faceEntry : element.getAsJsonObject("faces").entrySet()) {
                Direction side=direction(faceEntry.getKey()); JsonObject face=faceEntry.getValue().getAsJsonObject();
                float[] uv=face.has("uv")?array(face.getAsJsonArray("uv"),4):defaultUv(side,from,to);
                int rotation=face.has("rotation")?face.get("rotation").getAsInt():0;
                if (rotation%90!=0) throw new IllegalArgumentException("Invalid face UV rotation: "+rotation);
                float[] vertices=vertices(side,from,to,uv,rotation/90); transform(vertices,matrices);
                faces.add(new Quad(texture(id,definition,face.get("texture").getAsString()),vertices,face.has("cullface")?cullDirection(face.get("cullface").getAsString()):null,
                    face.has("tintindex")?face.get("tintindex").getAsInt():-1,shade,definition.ambientOcclusion,side,rotation/90));
            }
        }
        List<Quad> result=Collections.unmodifiableList(faces); models.put(id,result); return result;
    }
    private Identifier modelParticle(Identifier id) {
        Definition definition=definition(id,new HashSet<>());
        return definition.textures.containsKey("particle")?texture(id,definition,"#particle"):null;
    }
    private static Identifier texture(Identifier id,Definition definition,String texture) {
        Set<String> visited=new HashSet<>();
        while (texture.startsWith("#")) {
            if (!visited.add(texture)) throw new IllegalArgumentException("Cyclic texture: "+id+texture);
            String resolved=definition.textures.get(texture.substring(1));
            if (resolved==null) throw new IllegalArgumentException("Missing texture: "+id+texture);
            texture=resolved;
        }
        return new Identifier(texture);
    }
    private static JsonObject select(JsonElement choice,long seed) {
        if (choice.isJsonArray()) {
            JsonArray choices=choice.getAsJsonArray(); long total=0;
            for (JsonElement value : choices) total=Math.addExact(total,weight(value.getAsJsonObject()));
            if (total==0) throw new IllegalArgumentException("Empty model choices"); long selected=Math.floorMod(seed,total);
            for (JsonElement value : choices) { selected-=weight(value.getAsJsonObject()); if (selected<0) { choice=value; break; } }
        }
        return choice.getAsJsonObject();
    }
    private void append(List<Quad> output,JsonObject variant) {
        boolean uvlock=variant.has("uvlock") && variant.get("uvlock").getAsBoolean();
        float x=variant.has("x")?variant.get("x").getAsFloat():0,y=variant.has("y")?variant.get("y").getAsFloat():0;
        if (uvlock && (!Float.isFinite(x) || !Float.isFinite(y) || x%90!=0 || y%90!=0)) throw new IllegalArgumentException("Invalid UV locked block rotation");
        LegacyMatrices matrices=new LegacyMatrices(); matrices.translate(.5,.5,.5);
        matrices.rotate((float)Math.toRadians(-y),0,1,0); matrices.rotate((float)Math.toRadians(-x),1,0,0); matrices.translate(-.5,-.5,-.5);
        for (Quad source : model(new Identifier(variant.get("model").getAsString()))) {
            float[] vertices=source.vertices.clone();
            if (uvlock) lockUv(vertices,source,matrices);
            transform(vertices,matrices);
            Direction cull=source.cull==null?null:closest(matrices.normal(source.cull.getOffsetX(),source.cull.getOffsetY(),source.cull.getOffsetZ()));
            output.add(new Quad(source.texture,vertices,cull,source.tint,source.shade,source.ambientOcclusion,source.side,source.rotation));
        }
    }
    private static void lockUv(float[] vertices,Quad source,LegacyMatrices matrices) {
        // Use the JSON face, before element rotation, as modern FaceBakery does.
        Direction target=closest(matrices.normal(source.side.getOffsetX(),source.side.getOffsetY(),source.side.getOffsetZ()));
        int[] u=UV_AXES[source.side.ordinal()][0],targetU=UV_AXES[target.ordinal()][0],targetV=UV_AXES[target.ordinal()][1];
        Vector3f rotatedU=matrices.normal(u[0],u[1],u[2]);
        int cos=Math.round(dot(rotatedU,targetU)),sin=Math.round(dot(rotatedU,targetV));
        int first=Math.floorMod(-source.rotation,4)*8,opposite=Math.floorMod(2-source.rotation,4)*8;
        float u0=vertices[first+3],v0=vertices[first+4],u1=vertices[opposite+3],v1=vertices[opposite+4];
        float a=cos*(u0-.5F)-sin*(v0-.5F)+.5F,b=sin*(u0-.5F)+cos*(v0-.5F)+.5F;
        float c=cos*(u1-.5F)-sin*(v1-.5F)+.5F,d=sin*(u1-.5F)+cos*(v1-.5F)+.5F;
        // Keep endpoint signs for mirrored UV rectangles, then account for face rotation.
        float[] uv={Math.signum(u1-u0)==Math.signum(c-a)?a:c,Math.signum(v1-v0)==Math.signum(d-b)?b:d,
            Math.signum(u1-u0)==Math.signum(c-a)?c:a,Math.signum(v1-v0)==Math.signum(d-b)?d:b};
        int rotation=Math.floorMod(-source.rotation-(int)Math.round(Math.atan2(sin,cos)/(Math.PI/2)),4);
        for (int i=0;i<4;i++) {
            int index=(i+rotation)%4; vertices[i*8+3]=uv[index<2?0:2]; vertices[i*8+4]=uv[index==0 || index==3?1:3];
        }
    }
    private static float dot(Vector3f vector,int[] axis) { return vector.x*axis[0]+vector.y*axis[1]+vector.z*axis[2]; }
    private static int weight(JsonObject value) {
        int weight=value.has("weight")?value.get("weight").getAsInt():1;
        if (weight<=0) throw new IllegalArgumentException("Non-positive model weight"); return weight;
    }
    private Definition definition(Identifier id,Set<Identifier> visiting) {
        Definition cached=definitions.get(id); if (cached!=null) return cached;
        if (!visiting.add(id)) throw new IllegalArgumentException("Cyclic model parent: "+id);
        JsonObject json=read(new Identifier(id.getNamespace(),"models/"+id.getPath()+".json"));
        Definition parent=json.has("parent")?definition(new Identifier(json.get("parent").getAsString()),visiting):new Definition(new JsonArray(),new HashMap<>(),true);
        Map<String,String> textures=new HashMap<>(parent.textures);
        if (json.has("textures")) json.getAsJsonObject("textures").entrySet().forEach(entry -> textures.put(entry.getKey(),entry.getValue().getAsString()));
        Definition result=new Definition(json.has("elements")?json.getAsJsonArray("elements"):parent.elements,textures,
            json.has("ambientocclusion")?json.get("ambientocclusion").getAsBoolean():parent.ambientOcclusion);
        definitions.put(id,result); visiting.remove(id); return result;
    }
    private JsonObject read(Identifier id) {
        String content=resources.apply(id);
        if (content!=null) return new JsonParser().parse(content).getAsJsonObject();
        if (id.getNamespace().equals("minecraft") && id.getPath().startsWith("models/block/")) {
            String name=id.getPath().substring("models/block/".length()).replace(".json","");
            if (name.equals("block")) return new JsonObject();
            if (name.equals("air")) { JsonObject air=new JsonObject(); air.add("elements",new JsonArray()); return air; }
            if (name.equals("cube_all") || name.equals("leaves") || name.equals("slab") || name.equals("slab_top")) return builtin(name);
        }
        throw new IllegalArgumentException("Missing block model resource: "+id);
    }
    private static JsonObject builtin(String name) {
        JsonObject model=new JsonObject(),element=new JsonObject(),faces=new JsonObject();
        JsonObject textures=new JsonObject(); textures.addProperty("particle",name.startsWith("slab")?"#side":"#all"); model.add("textures",textures);
        element.add("from",new JsonParser().parse("[0,"+(name.equals("slab_top")?8:0)+",0]"));
        element.add("to",new JsonParser().parse("[16,"+(name.equals("slab")?8:16)+",16]"));
        for (Direction side : Direction.values()) {
            JsonObject face=new JsonObject(); face.addProperty("texture",name.startsWith("slab")?(side==Direction.UP?"#top":side==Direction.DOWN?"#bottom":"#side"):"#all");
            if (name.equals("leaves")) face.addProperty("tintindex",0);
            if (!(name.equals("slab") && side==Direction.UP) && !(name.equals("slab_top") && side==Direction.DOWN)) face.addProperty("cullface",side.name().toLowerCase(Locale.ROOT));
            faces.add(side.name().toLowerCase(Locale.ROOT),face);
        }
        element.add("faces",faces); JsonArray elements=new JsonArray(); elements.add(element); model.add("elements",elements); return model;
    }
    private static boolean variantMatches(String key,Map<String,String> properties) {
        if (key.isEmpty()) return true;
        for (String condition : key.split(",")) { String[] pair=condition.split("=",2); if (pair.length!=2 || !Objects.equals(properties.get(pair[0]),pair[1])) return false; }
        return true;
    }
    private static boolean matches(JsonObject when,Map<String,String> properties) {
        for (Map.Entry<String,JsonElement> entry : when.entrySet()) {
            if (entry.getKey().equals("OR") || entry.getKey().equals("AND")) {
                boolean or=entry.getKey().equals("OR"),result=!or;
                for (JsonElement value : entry.getValue().getAsJsonArray()) { boolean match=matches(value.getAsJsonObject(),properties); result=or?result||match:result&&match; }
                if (!result) return false;
            } else {
                String expression=entry.getValue().getAsString(); boolean negate=expression.startsWith("!");
                if (negate) expression=expression.substring(1);
                boolean contained=Arrays.asList(expression.split("\\|")).contains(properties.get(entry.getKey()));
                if (negate==contained) return false;
            }
        }
        return true;
    }
    private static float[] vertices(Direction side,float[] a,float[] b,float[] uv,int rotation) {
        int[][][] corners={{{0,1,5},{0,1,2},{3,1,2},{3,1,5}},{{0,4,2},{0,4,5},{3,4,5},{3,4,2}},
            {{3,4,2},{3,1,2},{0,1,2},{0,4,2}},{{0,4,5},{0,1,5},{3,1,5},{3,4,5}},{{0,4,2},{0,1,2},{0,1,5},{0,4,5}},{{3,4,5},{3,1,5},{3,1,2},{3,4,2}}};
        float[] xyz={a[0],a[1],a[2],b[0],b[1],b[2]},face=new float[32];
        for (int i=0;i<4;i++) {
            int index=Math.floorMod(i+rotation,4),offset=i*8;
            for (int axis=0;axis<3;axis++) face[offset+axis]=xyz[corners[side.ordinal()][i][axis]]/16;
            face[offset+3]=uv[index<2?0:2]/16; face[offset+4]=uv[index==0 || index==3?1:3]/16;
            face[offset+5]=side.getOffsetX(); face[offset+6]=side.getOffsetY(); face[offset+7]=side.getOffsetZ();
        }
        return face;
    }
    private static float[] defaultUv(Direction side,float[] a,float[] b) {
        switch (side) {
            case DOWN: return new float[]{a[0],16-b[2],b[0],16-a[2]};
            case UP: return new float[]{a[0],a[2],b[0],b[2]};
            case NORTH: return new float[]{16-b[0],16-b[1],16-a[0],16-a[1]};
            case SOUTH: return new float[]{a[0],16-b[1],b[0],16-a[1]};
            case WEST: return new float[]{a[2],16-b[1],b[2],16-a[1]};
            default: return new float[]{16-b[2],16-b[1],16-a[2],16-a[1]};
        }
    }
    private static void transform(float[] vertices,LegacyMatrices matrices) {
        Matrix4f m=matrices.position();
        for (int i=0;i<32;i+=8) {
            float x=vertices[i],y=vertices[i+1],z=vertices[i+2];
            vertices[i]=m.m00*x+m.m10*y+m.m20*z+m.m30; vertices[i+1]=m.m01*x+m.m11*y+m.m21*z+m.m31; vertices[i+2]=m.m02*x+m.m12*y+m.m22*z+m.m32;
            Vector3f normal=matrices.normal(vertices[i+5],vertices[i+6],vertices[i+7]); vertices[i+5]=normal.x; vertices[i+6]=normal.y; vertices[i+7]=normal.z;
        }
    }
    private static float[] vector(JsonObject json,String key) { return array(json.getAsJsonArray(key),3); }
    private static float[] array(JsonArray array,int size) {
        if (array.size()!=size) throw new IllegalArgumentException("Invalid model vector length");
        float[] result=new float[size]; for (int i=0;i<size;i++) { result[i]=array.get(i).getAsFloat(); if (!Float.isFinite(result[i])) throw new IllegalArgumentException("Non-finite model coordinate"); } return result;
    }
    private static Direction direction(String value) { return Direction.valueOf(value.toUpperCase(Locale.ROOT)); }
    private static Direction cullDirection(String value) {
        // Modern Direction.byName returns null for unknown cullface values, including MTR's "#up".
        try { return direction(value); } catch (IllegalArgumentException exception) { return null; }
    }
    private static Direction closest(Vector3f normal) {
        Direction result=Direction.UP; float best=-Float.MAX_VALUE;
        for (Direction direction : Direction.values()) { float dot=normal.x*direction.getOffsetX()+normal.y*direction.getOffsetY()+normal.z*direction.getOffsetZ(); if (dot>best) { best=dot; result=direction; } } return result;
    }
    private static final class Definition {
        final JsonArray elements; final Map<String,String> textures; final boolean ambientOcclusion;
        Definition(JsonArray elements,Map<String,String> textures,boolean ambientOcclusion) { this.elements=elements; this.textures=textures; this.ambientOcclusion=ambientOcclusion; }
    }
    public static final class Quad {
        public final Identifier texture; public final float[] vertices; public final Direction cull; public final int tint; public final boolean shade,ambientOcclusion;
        private final Direction side; private final int rotation;
        Quad(Identifier texture,float[] vertices,Direction cull,int tint,boolean shade,boolean ambientOcclusion,Direction side,int rotation) {
            this.texture=texture; this.vertices=vertices; this.cull=cull; this.tint=tint; this.shade=shade; this.ambientOcclusion=ambientOcclusion; this.side=side; this.rotation=rotation;
        }
    }
}
