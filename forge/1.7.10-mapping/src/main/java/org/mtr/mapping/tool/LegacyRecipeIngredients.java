package org.mtr.mapping.tool;

import com.google.gson.*;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import java.util.*;

/** Explicit legacy equivalents for ingredients used by the original MTR recipes. */
final class LegacyRecipeIngredients {
    private static final String[] COLORS={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    private static final Map<String,Alias> ALIASES=new HashMap<>();
    private final Map<String,List<ItemStack>> groups=new LinkedHashMap<>();
    private final Map<String,List<ItemStack>> tags=new HashMap<>();
    static {
        alias("bricks","minecraft:brick_block",0);alias("snow_block","minecraft:snow",0);
        alias("stone_bricks","minecraft:stonebrick",0);alias("chiseled_stone_bricks","minecraft:stonebrick",3);alias("cracked_stone_bricks","minecraft:stonebrick",2);
        alias("chiseled_quartz_block","minecraft:quartz_block",1);alias("quartz_pillar","minecraft:quartz_block",2);alias("smooth_stone_slab","minecraft:stone_slab",0);
        alias("andesite","chisel:andesite",0,9);alias("polished_andesite","chisel:andesite",1,10);
        alias("diorite","chisel:diorite",0,9);alias("polished_diorite","chisel:diorite",1,10);
        alias("prismarine","Botania:prismarine",0);alias("dark_prismarine","Botania:prismarine",2);
        alias("blackstone","etfuturum:blackstone",0);alias("bone_block","etfuturum:bone",0);alias("chain","etfuturum:chain",0);
        for(String id:new String[]{"purpur_block","purpur_pillar","quartz_bricks","smooth_quartz","smooth_stone"})alias(id,"etfuturum:"+id,0);
        alias("smooth_sandstone","chisel:sandstone",8);alias("iron_nugget","gregtech:gt.metaitem.01",9032);
        alias("birch_log","minecraft:log",2);alias("birch_wood","etfuturum:bark",2);
        alias("stripped_birch_log","etfuturum:log_stripped",2);alias("stripped_birch_wood","etfuturum:wood_stripped",2);
        for(int color=0;color<COLORS.length;color++) {
            alias(COLORS[color]+"_wool","minecraft:wool",color);alias(COLORS[color]+"_dye","minecraft:dye",15-color);
            alias(COLORS[color]+"_stained_glass","minecraft:stained_glass",color);alias(COLORS[color]+"_stained_glass_pane","minecraft:stained_glass_pane",color);
            alias(COLORS[color]+"_concrete","etfuturum:concrete",color);alias(COLORS[color]+"_concrete_powder","etfuturum:concrete_powder",color);
        }
    }
    Object resolve(JsonObject ingredient) {
        CraftingRecipes.keys(ingredient,"item","tag");CraftingRecipes.require(ingredient.entrySet().size()==1,"Expected exactly one ingredient item/tag");
        if(ingredient.has("item")) {
            String id=CraftingRecipes.string(ingredient.get("item"));List<ItemStack> alternatives=items(id);
            return alternatives.size()==1?alternatives.get(0):group("item",id,alternatives);
        }
        String id=CraftingRecipes.string(ingredient.get("tag"));CraftingRecipes.identifier(id);
        if(id.equals("minecraft:logs")||id.equals("minecraft:planks")) {
            String ore=id.endsWith(":logs")?"logWood":"plankWood";
            CraftingRecipes.require(!OreDictionary.getOres(ore).isEmpty(),"Empty native recipe OreDictionary group "+ore+" for "+id);return ore;
        }
        List<ItemStack> alternatives=tags.get(id);
        if(alternatives==null){alternatives=tag(id);tags.put(id,alternatives);}
        return group("tag",id,alternatives);
    }
    private String group(String type,String id,List<ItemStack> alternatives) {
        CraftingRecipes.require(!alternatives.isEmpty(),"Empty recipe alternatives "+id);
        String name="mtrRecipe_"+type+"_"+id.replace(':','_').replace('/','_');groups.put(name,alternatives);return name;
    }
    private List<ItemStack> tag(String id) {
        List<ItemStack> result=new ArrayList<>();
        if(id.startsWith("c:")) {
            JsonObject definition=CraftingRecipes.json("data/c/tags/items/"+id.substring(2)+".json").getAsJsonObject();CraftingRecipes.keys(definition,"values","replace");
            for(JsonElement value:definition.getAsJsonArray("values"))result.addAll(items(CraftingRecipes.string(value)));
            return result;
        }
        switch(id) {
            case "minecraft:wool":for(int i=0;i<16;i++)result.add(nativeStack("minecraft:wool",i));break;
            case "minecraft:rails":for(String rail:new String[]{"rail","golden_rail","detector_rail","activator_rail"})result.add(nativeStack("minecraft:"+rail,0));break;
            case "minecraft:beds":for(String color:COLORS)result.add(nativeStack(color.equals("red")?"minecraft:bed":"etfuturum:"+color+"_bed",0));break;
            case "minecraft:fences":result.add(nativeStack("minecraft:fence",0));result.add(nativeStack("minecraft:nether_brick_fence",0));break;
            case "minecraft:birch_logs":for(String birch:new String[]{"birch_log","birch_wood","stripped_birch_log","stripped_birch_wood"})result.addAll(items("minecraft:"+birch));break;
            case "minecraft:signs":
                result.add(nativeStack("minecraft:sign",0));
                for(String wood:new String[]{"spruce","birch","jungle","acacia","dark_oak"})result.add(nativeStack("etfuturum:item_sign_"+wood,0));
                result.add(nativeStack("etfuturum:cherry_sign",0));break;
            default:throw new IllegalArgumentException("Unsupported recipe tag "+id);
        }
        return result;
    }
    private static List<ItemStack> items(String id) {
        CraftingRecipes.identifier(id);Alias alias=ALIASES.get(id);List<ItemStack> result=new ArrayList<>();
        if(alias==null)result.add(nativeStack(id,0));else for(int meta:alias.metadata)result.add(nativeStack(alias.id,meta));return result;
    }
    static ItemStack nativeStack(String id,int metadata) {
        String[] parts=id.split(":",2);CraftingRecipes.require(parts.length==2,"Invalid native recipe item "+id);
        Item item=GameRegistry.findItem(parts[0],parts[1]);CraftingRecipes.require(item!=null,"Unregistered recipe item "+id+":"+metadata);return new ItemStack(item,1,metadata);
    }
    void publish() {
        for(Map.Entry<String,List<ItemStack>> group:groups.entrySet()) {
            List<ItemStack> existing=OreDictionary.getOres(group.getKey());
            for(ItemStack entry:existing)CraftingRecipes.require(contains(group.getValue(),entry),"Foreign alternative in private recipe group "+group.getKey());
            for(ItemStack entry:group.getValue())if(!contains(existing,entry))OreDictionary.registerOre(group.getKey(),entry.copy());
        }
    }
    private static boolean contains(List<ItemStack> values,ItemStack target) {
        for(ItemStack value:values)if(value.getItem()==target.getItem()&&value.getItemDamage()==target.getItemDamage())return true;return false;
    }
    private static void alias(String modern,String nativeId,int... metadata) {ALIASES.put("minecraft:"+modern,new Alias(nativeId,metadata));}
    private static final class Alias {final String id;final int[] metadata;Alias(String id,int[] metadata){this.id=id;this.metadata=metadata;}}
}
