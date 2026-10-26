package org.mtr.mapping.tool;

import com.google.gson.*;
import org.mtr.mapping.holder.Identifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/** Resource-only item model decoder; a new instance is created for each atlas reload. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyItemModels {
    private final Function<Identifier,String> resources;
    private final LegacyBlockModels blocks;
    private final Map<Identifier,Definition> definitions=new HashMap<>();
    private final Map<Identifier,Model> models=new HashMap<>();
    public LegacyItemModels(Function<Identifier,String> resources) { this.resources=resources; blocks=new LegacyBlockModels(resources); }
    public Model select(Identifier root,ToDoubleFunction<Identifier> predicates) {
        Definition definition=definition(root,new HashSet<>());
        for (int i=definition.overrides.size()-1;i>=0;i--) {
            JsonObject override=definition.overrides.get(i).getAsJsonObject(); boolean matches=true;
            for (Map.Entry<String,JsonElement> condition:override.getAsJsonObject("predicate").entrySet()) {
                double value=predicates.applyAsDouble(new Identifier(condition.getKey()));
                if (!Double.isFinite(value) || value<condition.getValue().getAsDouble()) { matches=false; break; }
            }
            if (matches) return model(new Identifier(override.get("model").getAsString()));
        }
        return model(root);
    }
    public Model model(Identifier id) {
        Model cached=models.get(id); if (cached!=null) return cached;
        Definition definition=definition(id,new HashSet<>()); List<Identifier> layers=new ArrayList<>();
        if (definition.generated) for (int i=0;i<5 && definition.textures.containsKey("layer"+i);i++) layers.add(texture(definition,"#layer"+i));
        Model model=new Model(layers,definition.generated?Collections.emptyList():blocks.model(id),definition.generated,definition.handheld,definition.hiddenFirstPerson);
        models.put(id,model); return model;
    }
    public Set<Identifier> textures(Identifier root) {
        Set<Identifier> result=new LinkedHashSet<>(); collect(root,result,new HashSet<>()); return result;
    }
    private void collect(Identifier id,Set<Identifier> textures,Set<Identifier> seen) {
        if (!seen.add(id)) return;
        Model model=model(id); textures.addAll(model.layers); for (LegacyBlockModels.Quad face:model.faces) textures.add(face.texture);
        for (JsonElement override:definition(id,new HashSet<>()).overrides) collect(new Identifier(override.getAsJsonObject().get("model").getAsString()),textures,seen);
    }
    private Definition definition(Identifier id,Set<Identifier> visiting) {
        Definition cached=definitions.get(id); if (cached!=null) return cached;
        if (!visiting.add(id)) throw new IllegalArgumentException("Cyclic item model parent: "+id);
        String source=resources.apply(new Identifier(id.getNamespace(),"models/"+id.getPath()+".json"));
        Definition result;
        if (source==null && id.getNamespace().equals("minecraft") && (id.getPath().equals("item/generated") || id.getPath().equals("builtin/generated") || id.getPath().equals("item/handheld"))) {
            result=new Definition(true,id.getPath().equals("item/handheld"),false,new LinkedHashMap<>(),new JsonArray());
        } else if (source==null && id.getNamespace().equals("minecraft") && id.getPath().startsWith("block/")) {
            // The existing block decoder supplies the supported vanilla block parents and geometry.
            result=new Definition(false,false,false,new LinkedHashMap<>(),new JsonArray());
        } else {
            if (source==null) throw new IllegalArgumentException("Missing item model: "+id);
            JsonObject json=new JsonParser().parse(source).getAsJsonObject();
            Definition parent=json.has("parent")?definition(new Identifier(json.get("parent").getAsString()),visiting):new Definition(false,false,false,new LinkedHashMap<>(),new JsonArray());
            Map<String,String> textures=new LinkedHashMap<>(parent.textures);
            if (json.has("textures")) for (Map.Entry<String,JsonElement> entry:json.getAsJsonObject("textures").entrySet()) textures.put(entry.getKey(),entry.getValue().getAsString());
            boolean hidden=parent.hiddenFirstPerson;
            if (json.has("display")) {
                JsonObject display=json.getAsJsonObject("display");
                if (display.has("firstperson_righthand")) hidden=zeroScale(display.getAsJsonObject("firstperson_righthand"));
            }
            result=new Definition(parent.generated,parent.handheld,hidden,textures,json.has("overrides")?json.getAsJsonArray("overrides"):parent.overrides);
        }
        visiting.remove(id); definitions.put(id,result); return result;
    }
    private static boolean zeroScale(JsonObject transform) {
        if (!transform.has("scale")) return false;
        JsonArray scale=transform.getAsJsonArray("scale");
        return scale.size()==3 && scale.get(0).getAsFloat()==0 && scale.get(1).getAsFloat()==0 && scale.get(2).getAsFloat()==0;
    }
    private static Identifier texture(Definition definition,String name) {
        Set<String> seen=new HashSet<>();
        while (name.startsWith("#")) {
            if (!seen.add(name)) throw new IllegalArgumentException("Cyclic item texture alias");
            String value=definition.textures.get(name.substring(1));
            if (value==null) throw new IllegalArgumentException("Missing item texture: "+name);
            name=value;
        }
        return new Identifier(name);
    }
    private static final class Definition {
        final boolean generated,handheld,hiddenFirstPerson; final Map<String,String> textures; final JsonArray overrides;
        Definition(boolean generated,boolean handheld,boolean hiddenFirstPerson,Map<String,String> textures,JsonArray overrides) {
            this.generated=generated; this.handheld=handheld; this.hiddenFirstPerson=hiddenFirstPerson; this.textures=textures; this.overrides=overrides;
        }
    }
    public static final class Model {
        public final List<Identifier> layers;
        public final List<LegacyBlockModels.Quad> faces;
        public final boolean generated,handheld,hiddenFirstPerson;
        Model(List<Identifier> layers,List<LegacyBlockModels.Quad> faces,boolean generated,boolean handheld,boolean hiddenFirstPerson) {
            this.layers=Collections.unmodifiableList(layers); this.faces=faces; this.generated=generated; this.handheld=handheld; this.hiddenFirstPerson=hiddenFirstPerson;
        }
    }
}
