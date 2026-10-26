package org.mtr.mapping.tool;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The two crafting recipe types in the pinned MTR 4.0.5 resources. */
public final class CraftingRecipes {
    private static boolean started,registered;
    private CraftingRecipes() {}

    @SuppressWarnings("unchecked")
    public static synchronized void register() {
        if(registered)return;
        require(!started,"Crafting registration is already running or failed");started=true;
        LegacyRecipeIngredients ingredients=new LegacyRecipeIngredients();List<IRecipe> recipes=new ArrayList<>();
        Set<String> ids=new HashSet<>(),outputs=new HashSet<>();int shaped=0,shapeless=0;
        JsonArray index=json("META-INF/mtr/recipes.json").getAsJsonArray();
        for(JsonElement value:index) {
            String id=string(value);identifier(id);require(ids.add(id),"Duplicate recipe "+id);
            String[] parts=id.split(":",2);String path="data/"+parts[0]+"/recipes/"+parts[1]+".json";
            IRecipe recipe=parse(id,json(path).getAsJsonObject(),ingredients);recipes.add(recipe);
            if(recipe instanceof ShapedOreRecipe)shaped++;else shapeless++;
            outputs.add(Item.itemRegistry.getNameForObject(recipe.getRecipeOutput().getItem()));
        }
        require(recipes.size()==308&&shaped==167&&shapeless==141&&outputs.size()==261,"Incomplete MTR crafting resources: "+recipes.size()+" recipes, "+shaped+" shaped, "+shapeless+" shapeless, "+outputs.size()+" outputs");
        ingredients.publish();
        CraftingManager.getInstance().getRecipeList().addAll(recipes);
        registered=true;
    }

    static IRecipe parse(String id,JsonObject root,LegacyRecipeIngredients ingredients) {
        try {
            identifier(id);String type=string(root.get("type"));
            require(type.equals("minecraft:crafting_shaped")||type.equals("minecraft:crafting_shapeless"),"Unsupported crafting type "+type);
            JsonObject result=root.getAsJsonObject("result");
            keys(result,"item","count");String output=string(result.get("item"));identifier(output);
            require(!result.has("count")||result.get("count").isJsonPrimitive()&&result.get("count").getAsJsonPrimitive().isNumber(),"Expected numeric result count");
            int count=result.has("count")?result.get("count").getAsBigDecimal().intValueExact():1;
            require(count>0&&count<=64,"Invalid result count "+count);
            ItemStack stack=LegacyRecipeIngredients.nativeStack(output,0);stack.stackSize=count;
            if(type.equals("minecraft:crafting_shapeless")) {
                keys(root,"type","ingredients","result");JsonArray input=root.getAsJsonArray("ingredients");
                require(input.size()>0&&input.size()<=9,"Invalid shapeless ingredient count");List<Object> arguments=new ArrayList<>();
                for(JsonElement ingredient:input)arguments.add(ingredients.resolve(ingredient.getAsJsonObject()));
                return new ShapelessOreRecipe(stack,arguments.toArray());
            }
            keys(root,"type","pattern","key","result");JsonArray pattern=root.getAsJsonArray("pattern");
            require(pattern.size()>0&&pattern.size()<=3,"Invalid pattern height");String[] rows=new String[pattern.size()];int width=-1,left=3,right=-1,top=-1,bottom=-1;
            Set<Character> used=new HashSet<>();
            for(int y=0;y<rows.length;y++) {
                rows[y]=string(pattern.get(y));if(width<0)width=rows[y].length();
                require(width>0&&width<=3&&rows[y].length()==width,"Invalid pattern width");
                for(int x=0;x<width;x++)if(rows[y].charAt(x)!=' ') {
                    used.add(rows[y].charAt(x));left=Math.min(left,x);right=Math.max(right,x);if(top<0)top=y;bottom=y;
                }
            }
            require(top>=0,"Empty crafting pattern");
            // Modern ShapedRecipe removes empty outer rows/columns before matching offsets.
            String[] trimmed=new String[bottom-top+1];for(int y=top;y<=bottom;y++)trimmed[y-top]=rows[y].substring(left,right+1);
            JsonObject key=root.getAsJsonObject("key");Set<Character> defined=new HashSet<>();List<Object> arguments=new ArrayList<>();arguments.add(trimmed);
            for(Map.Entry<String,JsonElement> entry:key.entrySet()) {
                require(entry.getKey().length()==1&&entry.getKey().charAt(0)!=' ',"Invalid pattern key "+entry.getKey());char symbol=entry.getKey().charAt(0);
                defined.add(symbol);arguments.add(symbol);arguments.add(ingredients.resolve(entry.getValue().getAsJsonObject()));
            }
            require(used.equals(defined),"Pattern keys do not match used symbols");
            return new ShapedOreRecipe(stack,arguments.toArray());
        } catch(RuntimeException error) {throw invalid(id,error);}
    }
    private static IllegalArgumentException invalid(String id,RuntimeException cause) {return new IllegalArgumentException("Invalid crafting recipe "+id+": "+cause.getMessage(),cause);}
    static JsonElement json(String path) {
        InputStream input=CraftingRecipes.class.getClassLoader().getResourceAsStream(path);require(input!=null,"Missing crafting resource "+path);
        try(Reader reader=new InputStreamReader(input,StandardCharsets.UTF_8)){return new JsonParser().parse(reader);}
        catch(IOException|RuntimeException error){throw new IllegalArgumentException("Invalid crafting resource "+path+": "+error.getMessage(),error);}
    }
    static void keys(JsonObject object,String... allowed) {
        Set<String> fields=new HashSet<>(Arrays.asList(allowed));for(Map.Entry<String,JsonElement> entry:object.entrySet())require(fields.contains(entry.getKey()),"Unsupported crafting field "+entry.getKey());
    }
    static String string(JsonElement value) {require(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString(),"Expected crafting string");return value.getAsString();}
    static void identifier(String id) {require(id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&!id.contains("..")&&!id.endsWith("/")&&!id.contains(":/"),"Invalid crafting identifier "+id);}
    static void require(boolean condition,String message) {if(!condition)throw new IllegalArgumentException(message);}
}
