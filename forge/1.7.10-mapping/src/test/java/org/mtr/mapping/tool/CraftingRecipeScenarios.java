package org.mtr.mapping.tool;

import com.google.gson.*;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.*;
import org.mtr.mapping.registry.Registry;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real native recipe matching with clearly synthetic MTR/backport registry items, never a GTNH runtime proof. */
public final class CraftingRecipeScenarios {
    private static final Path DATA=Paths.get(System.getProperty("mtr.lootResources"));
    private static boolean bootstrapped;

    private static void bootstrap(boolean omitSandstone)throws Exception {
        if(bootstrapped)return;bootstrapped=true;
        cpw.mods.fml.common.Loader.injectData("7","99","40","1614","1.7.10","9.05",new File("."),Collections.emptyList());
        Bootstrap.func_151354_b();
        // Use the existing native fixture's mod context, without starting a world/client.
        Method context=Class.forName("org.mtr.mapping.BlockPlacementScenarios").getDeclaredMethod("initializeModContext");context.setAccessible(true);context.invoke(null);
        for(Path path:recipePaths())collectItems(read(path));
        String[] backports={"chisel:andesite","chisel:diorite","Botania:prismarine","etfuturum:blackstone","etfuturum:bone","etfuturum:chain","etfuturum:purpur_block","etfuturum:purpur_pillar","etfuturum:quartz_bricks","etfuturum:smooth_quartz","etfuturum:smooth_stone","etfuturum:concrete","etfuturum:concrete_powder","gregtech:gt.metaitem.01","etfuturum:cherry_sign","etfuturum:bark","etfuturum:log_stripped","etfuturum:wood_stripped"};
        for(String id:backports)register(id);
        if(!omitSandstone)register("chisel:sandstone");
        String[] colors={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
        for(String color:colors)if(!color.equals("red"))register("etfuturum:"+color+"_bed");
        for(String wood:new String[]{"spruce","birch","jungle","acacia","dark_oak"})register("etfuturum:item_sign_"+wood);
        if(OreDictionary.getOres("logWood").isEmpty()) {
            OreDictionary.registerOre("logWood",stack("minecraft:log",OreDictionary.WILDCARD_VALUE));
            OreDictionary.registerOre("logWood",stack("minecraft:log2",OreDictionary.WILDCARD_VALUE));
        }
        if(OreDictionary.getOres("plankWood").isEmpty())OreDictionary.registerOre("plankWood",stack("minecraft:planks",OreDictionary.WILDCARD_VALUE));
    }
    private static void register(String id) {
        if(Item.itemRegistry.getObject(id)!=null)return;
        try {
            // Forge's deprecated addObject adds the active MTR prefix to foreign fixture namespaces.
            Class<?> data=cpw.mods.fml.common.registry.GameData.class;
            Method main=data.getDeclaredMethod("getMain");main.setAccessible(true);
            Method register=data.getDeclaredMethod("registerItem",Item.class,String.class,int.class);register.setAccessible(true);
            Item item=new Item().setHasSubtypes(true).setUnlocalizedName(id);
            register.invoke(main.invoke(null),item,id,-1);
            String[] parts=id.split(":",2);assertSame(item,GameRegistry.findItem(parts[0],parts[1]),"Fixture must retain its exact native namespace");
        } catch(ReflectiveOperationException error) {throw new IllegalStateException("Cannot register synthetic recipe fixture "+id,error);}
    }
    private static void collectItems(JsonElement value) {
        if(value.isJsonObject())for(Map.Entry<String,JsonElement> entry:value.getAsJsonObject().entrySet()) {
            if(entry.getKey().equals("item")&&entry.getValue().isJsonPrimitive()) {
                String id=entry.getValue().getAsString();if(id.startsWith("mtr:")||id.startsWith("mtrsteamloco:"))register(id);
            }
            collectItems(entry.getValue());
        } else if(value.isJsonArray())for(JsonElement child:value.getAsJsonArray())collectItems(child);
    }
    private static List<Path> recipePaths()throws IOException {
        try(Stream<Path> paths=Files.walk(DATA)) {return paths.filter(p -> p.getParent().getFileName().toString().equals("recipes")&&p.toString().endsWith(".json")).sorted().collect(Collectors.toList());}
    }
    private static JsonObject read(Path path)throws IOException {try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){return new JsonParser().parse(reader).getAsJsonObject();}}
    private static IRecipe recipe(String id)throws Exception {try(Reader reader=Files.newBufferedReader(DATA.resolve("mtr/recipes/"+id+".json"),StandardCharsets.UTF_8)){return parse("mtr:"+id,reader);}}
    private static IRecipe parse(String id,Reader reader) {LegacyRecipeIngredients resolver=new LegacyRecipeIngredients();IRecipe recipe=CraftingRecipes.parse(id,new JsonParser().parse(reader).getAsJsonObject(),resolver);resolver.publish();return recipe;}
    private static ItemStack stack(String id,int meta) {Item item=(Item)Item.itemRegistry.getObject(id);assertNotNull(item,id);return new ItemStack(item,1,meta);}
    private static InventoryCrafting grid(ItemStack... slots) {
        InventoryCrafting grid=new InventoryCrafting(new Container(){public boolean canInteractWith(EntityPlayer player){return true;}},3,3);
        for(int i=0;i<slots.length;i++)grid.setInventorySlotContents(i,slots[i]);return grid;
    }
    private static void output(IRecipe recipe,InventoryCrafting grid,String id,int count) {
        assertTrue(recipe.matches(grid,null),id);ItemStack output=recipe.getCraftingResult(grid);
        assertSame(Item.itemRegistry.getObject(id),output.getItem());assertEquals(count,output.stackSize);assertEquals(0,output.getItemDamage());
        assertNotSame(recipe.getRecipeOutput(),output,"Crafting must return a copy");
    }

    public static void fullBridge()throws Exception {
        bootstrap(false);Registry registry=new Registry();
        // The historical missing lifecycle hook must fail the count assertion, not merely fail compilation.
        Method registration=null;try{registration=Registry.class.getMethod("registerRecipes");}catch(NoSuchMethodException missingBridge){}
        if(registration!=null)assertThrows(InvocationTargetException.class,() -> Registry.class.getMethod("registerRecipes").invoke(registry));
        registry.init();int before=CraftingManager.getInstance().getRecipeList().size();
        if(registration!=null)registration.invoke(registry);
        List<?> all=CraftingManager.getInstance().getRecipeList();assertEquals(308,all.size()-before,"All original JSON recipes must reach native CraftingManager");
        int shaped=0,shapeless=0;Set<String> outputs=new HashSet<>();
        for(Object value:all.subList(before,all.size())) {
            IRecipe r=(IRecipe)value;if(r instanceof ShapedOreRecipe)shaped++;if(r instanceof ShapelessOreRecipe)shapeless++;
            outputs.add(Item.itemRegistry.getNameForObject(r.getRecipeOutput().getItem()));
        }
        assertEquals(167,shaped);assertEquals(141,shapeless);assertEquals(261,outputs.size());
        if(registration!=null)registration.invoke(registry);assertEquals(before+308,all.size(),"A repeated lifecycle callback must add no duplicates");
        InventoryCrafting escalator=grid(stack("minecraft:glass_pane",0),stack("minecraft:iron_block",0),stack("minecraft:redstone",0),stack("minecraft:stone_brick_stairs",0));
        ItemStack crafted=CraftingManager.getInstance().findMatchingRecipe(escalator,null);assertNotNull(crafted);assertSame(Item.itemRegistry.getObject("mtr:escalator"),crafted.getItem());assertEquals(8,crafted.stackSize);
        for(Path path:recipePaths()) {
            JsonObject json=read(path);String id=path.getParent().getParent().getFileName()+":"+path.getFileName().toString().replace(".json","");
            try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
                IRecipe parsed=parse(id,reader);JsonObject expected=json.getAsJsonObject("result");
                assertEquals(expected.has("count")?expected.get("count").getAsInt():1,parsed.getRecipeOutput().stackSize,id);
                assertSame(Item.itemRegistry.getObject(expected.get("item").getAsString()),parsed.getRecipeOutput().getItem(),id);
            }
        }
    }
    public static void privateAlternativesAreLiveAfterNativeConstruction()throws Exception {
        bootstrap(false);LegacyRecipeIngredients resolver=new LegacyRecipeIngredients();
        IRecipe escalator=CraftingRecipes.parse("mtr:escalator",read(DATA.resolve("mtr/recipes/escalator.json")),resolver);
        IRecipe brush=CraftingRecipes.parse("mtr:brush",read(DATA.resolve("mtr/recipes/brush.json")),resolver);
        InventoryCrafting escalatorGrid=grid(stack("minecraft:glass_pane",0),stack("minecraft:iron_block",0),stack("minecraft:redstone",0),stack("minecraft:stone_brick_stairs",0));
        ItemStack wool=stack("minecraft:wool",0),stick=stack("minecraft:stick",0);InventoryCrafting brushGrid=grid(wool,wool,wool,wool,stick,wool,null,stick,null);
        assertFalse(escalator.matches(escalatorGrid,null));assertFalse(brush.matches(brushGrid,null));
        resolver.publish();
        output(escalator,escalatorGrid,"mtr:escalator",8);output(brush,brushGrid,"mtr:brush",1);
    }
    public static void shapedMirrorsOffsetsAndModernPadding()throws Exception {
        IRecipe dashboard=recipe("airplane_dashboard");ItemStack feather=stack("minecraft:feather",0),redstone=stack("minecraft:redstone",0),glass=stack("minecraft:glass_pane",0),glow=stack("minecraft:glowstone_dust",0);
        output(dashboard,grid(feather,feather,feather,redstone,glass,glow,feather,feather,feather),"mtr:dashboard_4",1);
        output(dashboard,grid(feather,feather,feather,glow,glass,redstone,feather,feather,feather),"mtr:dashboard_4",1);
        assertFalse(dashboard.matches(grid(feather,feather,feather,redstone,glow,glass,feather,feather,feather),null));
        IRecipe r=recipe("station_name_tall_standing");ItemStack stick=stack("minecraft:stick",0),name=stack("mtr:station_name_tall_wall",0);
        InventoryCrafting top=grid(stick,name,stick,stick,null,stick);
        output(r,top,"mtr:station_name_tall_standing",1);
        InventoryCrafting bottom=grid(null,null,null,stick,name,stick,stick,null,stick);
        output(r,bottom,"mtr:station_name_tall_standing",1);
    }
    public static void exactColorsAndStainedGlassExcludePlainGlass()throws Exception {
        IRecipe brush=recipe("brush");ItemStack wool=stack("minecraft:wool",0),stick=stack("minecraft:stick",0);
        InventoryCrafting grid=grid(wool,wool,wool,wool,stick,wool,null,stick,null);assertTrue(brush.matches(grid,null));
        grid.setInventorySlotContents(0,stack("minecraft:wool",14));assertFalse(brush.matches(grid,null));
        IRecipe stained=recipe("station_color_stained_glass");
        for(int color=0;color<16;color++) {
            ItemStack glass=stack("minecraft:stained_glass",color);InventoryCrafting mix=grid(glass,glass,glass,glass,stack("minecraft:dye",color),glass,glass,glass,glass);
            output(stained,mix,"mtr:station_color_stained_glass",8);mix.setInventorySlotContents(0,stack("minecraft:glass",0));assertFalse(stained.matches(mix,null));
        }
        IRecipe concrete=recipe("station_color_concrete");
        for(int color=0;color<16;color++) {
            ItemStack block=stack("etfuturum:concrete",color);InventoryCrafting mix=grid(block,block,block,block,stack("minecraft:dye",15-color),block,block,block,block);
            output(concrete,mix,"mtr:station_color_concrete",8);mix.setInventorySlotContents(0,stack("etfuturum:concrete_powder",color));assertFalse(concrete.matches(mix,null));
        }
        IRecipe birch=recipe("station_color_birch_wood");
        for(String id:new String[]{"minecraft:log","etfuturum:bark","etfuturum:log_stripped","etfuturum:wood_stripped"}) {
            ItemStack block=stack(id,2);InventoryCrafting wood=grid(block,block,block,block,stack("minecraft:dye",0),block,block,block,block);
            output(birch,wood,"mtr:station_color_birch_wood",8);wood.setInventorySlotContents(0,stack(id,0));assertFalse(birch.matches(wood,null),"Other wood species must not match birch_logs");
        }
    }
    public static void shapelessCountsDuplicatesAndLegacyVariants()throws Exception {
        IRecipe marble=recipe("marble_sandy");
        for(int polished:new int[]{1,10}) {
            InventoryCrafting valid=grid(stack("chisel:diorite",polished),stack("chisel:sandstone",8),stack("chisel:diorite",polished),stack("chisel:sandstone",8),stack("chisel:diorite",polished),stack("chisel:sandstone",8));
            output(marble,valid,"mtr:marble_sandy",6);
            valid.setInventorySlotContents(1,stack("chisel:sandstone",0));assertFalse(marble.matches(valid,null));
            valid.setInventorySlotContents(1,stack("minecraft:sandstone",2));assertFalse(marble.matches(valid,null));
            valid.setInventorySlotContents(1,stack("chisel:sandstone",8));valid.setInventorySlotContents(0,stack("chisel:diorite",0));assertFalse(marble.matches(valid,null));
        }
        InventoryCrafting missing=grid(stack("chisel:sandstone",8),stack("chisel:sandstone",8),stack("chisel:diorite",1),stack("chisel:diorite",1),stack("chisel:diorite",1));assertFalse(marble.matches(missing,null));
        missing.getStackInSlot(0).stackSize=3;assertFalse(marble.matches(missing,null),"A larger input stack cannot replace three separate recipe slots");
        IRecipe dispenser=recipe("driver_key_dispenser");output(dispenser,grid(stack("minecraft:dispenser",0),stack("etfuturum:chain",0),stack("gregtech:gt.metaitem.01",9032),stack("minecraft:iron_ingot",0)),"mtr:driver_key_dispenser",1);
        InventoryCrafting wrong=grid(stack("minecraft:dispenser",0),stack("etfuturum:chain",0),stack("gregtech:gt.metaitem.01",9031),stack("minecraft:iron_ingot",0));assertFalse(dispenser.matches(wrong,null));
        IRecipe signal=recipe("signal_light_2_aspect_1");
        InventoryCrafting colors=grid(stack("etfuturum:blackstone",0),stack("minecraft:dye",4),stack("minecraft:dye",1),stack("minecraft:redstone",0),stack("minecraft:glowstone_dust",0));
        output(signal,colors,"mtr:signal_light_1",2);colors.setInventorySlotContents(1,stack("minecraft:dye",2));assertFalse(signal.matches(colors,null));
    }
    public static void invalidAndMissingInputsFailWithRecipeId() {
        String[] invalid={"{\"type\":\"minecraft:smelting\"}","{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"tag\":\"c:no_such_tag\"}],\"result\":{\"item\":\"mtr:brush\"}}","{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"missing:item\"}],\"result\":{\"item\":\"mtr:brush\"}}","{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:stick\"}],\"result\":{\"item\":\"missing:result\"}}","{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"X\"],\"key\":{},\"result\":{\"item\":\"mtr:brush\"}}"};
        int before=CraftingManager.getInstance().getRecipeList().size();
        for(String json:invalid) {IllegalArgumentException error=assertThrows(IllegalArgumentException.class,() -> parse("mtr:invalid_fixture",new StringReader(json)));assertTrue(error.getMessage().contains("mtr:invalid_fixture"),error.toString());}
        assertEquals(before,CraftingManager.getInstance().getRecipeList().size());
    }
    public static void missingBackportFailsBeforeRegistration()throws Exception {
        bootstrap(true);Registry registry=new Registry();registry.init();int before=CraftingManager.getInstance().getRecipeList().size();
        IllegalArgumentException error=assertThrows(IllegalArgumentException.class,registry::registerRecipes);
        assertTrue(error.getMessage().contains("mtr:marble_sandy"),error.toString());assertTrue(error.toString().contains("chisel:sandstone"));
        assertEquals(before,CraftingManager.getInstance().getRecipeList().size(),"Missing ingredients must not leave partially registered recipes");
    }
}
