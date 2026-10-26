package org.mtr.mapping;

import com.google.gson.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeCompatibilityTest {
    @Test void recipeIndexContainsEveryOriginalRecipeAndBothNamespaces() throws Exception {
        Path data=Paths.get(System.getProperty("mtr.lootResources"));
        Set<String> expected;
        try(Stream<Path> paths=Files.walk(data)) {
            expected=paths.filter(p -> p.getParent().getFileName().toString().equals("recipes")&&p.toString().endsWith(".json"))
                .map(p -> p.getParent().getParent().getFileName()+":"+p.getFileName().toString().replace(".json",""))
                .collect(Collectors.toSet());
        }
        Set<String> indexed=new HashSet<>();
        try(Reader reader=new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/META-INF/mtr/recipes.json")),StandardCharsets.UTF_8)) {
            for(JsonElement id:new JsonParser().parse(reader).getAsJsonArray()) assertTrue(indexed.add(id.getAsString()),"Duplicate recipe ID");
        }
        assertEquals(308,indexed.size());assertEquals(expected,indexed);
        assertTrue(indexed.contains("mtrsteamloco:eye_candy"));
    }

    @Test void originalRecipesRegisterOnceAndCraftWithNativeMetadataAndAlternatives() throws Throwable {
        try(LaunchClassLoader loader=forgeLoader()) {
            invoke(loader,"fullBridge");
            invoke(loader,"shapedMirrorsOffsetsAndModernPadding");
            invoke(loader,"exactColorsAndStainedGlassExcludePlainGlass");
            invoke(loader,"shapelessCountsDuplicatesAndLegacyVariants");
            invoke(loader,"invalidAndMissingInputsFailWithRecipeId");
        }
    }

    @Test void unresolvedFullBridgeAddsNoPartialRecipeSet() throws Throwable {
        try(LaunchClassLoader loader=forgeLoader()) { invoke(loader,"missingBackportFailsBeforeRegistration"); }
    }
    @Test void nativeRecipeConstructorsRetainPrivateAlternativesUntilPublish() throws Throwable {
        try(LaunchClassLoader loader=forgeLoader()) { invoke(loader,"privateAlternativesAreLiveAfterNativeConstruction"); }
    }

    @Test void pinnedVanillaTexturesKeepBothOriginalAndCompatibilityResourceNames() throws Exception {
        JsonObject manifest;
        try(Reader reader=new InputStreamReader(getClass().getResourceAsStream("/META-INF/mtr/vanilla-textures.json"),StandardCharsets.UTF_8)){manifest=new JsonParser().parse(reader).getAsJsonObject();}
        for(JsonElement value:manifest.getAsJsonArray("textures")) {
            String path="/assets/minecraft/textures/"+value.getAsString()+".png";
            assertArrayEquals(bytes(path.replace("/minecraft/","/mtr_compat/")),bytes(path),path);
        }
    }
    @Test void everyBundledBoatWoodTextureExistsAtTheModernModelUvSize() throws Exception {
        JsonObject resources,manifest;
        try(Reader reader=new InputStreamReader(getClass().getResourceAsStream("/assets/mtr/mtr_custom_resources.json"),StandardCharsets.UTF_8)){resources=new JsonParser().parse(reader).getAsJsonObject();}
        try(Reader reader=new InputStreamReader(getClass().getResourceAsStream("/META-INF/mtr/vanilla-textures.json"),StandardCharsets.UTF_8)){manifest=new JsonParser().parse(reader).getAsJsonObject();}
        Set<String> textures=new TreeSet<>(),models=new TreeSet<>(),prepared=new HashSet<>(),contents=new HashSet<>();int boats=0;
        for(JsonElement path:manifest.getAsJsonArray("textures"))prepared.add(path.getAsString());
        for(JsonElement value:resources.getAsJsonArray("vehicles")) {
            JsonObject vehicle=value.getAsJsonObject();if(!vehicle.get("transportMode").getAsString().equals("BOAT"))continue;boats++;
            for(String section:new String[]{"models","bogie1Models","bogie2Models"})if(vehicle.has(section))for(JsonElement model:vehicle.getAsJsonArray(section)) {
                JsonObject part=model.getAsJsonObject();String texture=part.get("textureResource").getAsString();
                assertTrue(texture.startsWith("minecraft:textures/entity/boat/"),vehicle.get("id").getAsString());
                textures.add(texture.substring("minecraft:textures/".length(),texture.length()-4));models.add(part.get("modelResource").getAsString());
            }
        }
        String[] woods={"acacia","birch","cherry","dark_oak","jungle","mangrove","oak","spruce"};Set<String> expected=new TreeSet<>();
        for(String wood:woods)expected.add("entity/boat/"+wood);
        assertEquals(32,boats);assertEquals(expected,textures);
        for(String texture:textures) {
            assertTrue(prepared.contains(texture),"Missing reachable boat texture in the pinned preparation manifest: "+texture);
            String path="/assets/minecraft/textures/"+texture+".png";byte[] data=bytes(path);
            assertArrayEquals(data,bytes(path.replace("/minecraft/","/mtr_compat/")),path);
            java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(new ByteArrayInputStream(data));assertNotNull(image,path);
            assertEquals(128,image.getWidth(),path);assertEquals(64,image.getHeight(),path);contents.add(Base64.getEncoder().encodeToString(data));
        }
        assertEquals(8,contents.size(),"Every wood must keep its own upstream texture");
        assertEquals(new TreeSet<>(Arrays.asList("mtr:models/vehicle/boat_small.bbmodel","mtr:models/vehicle/boat_medium.bbmodel")),models);
        for(String model:models)try(Reader reader=new InputStreamReader(getClass().getResourceAsStream("/assets/mtr/"+model.substring("mtr:".length())),StandardCharsets.UTF_8)) {
            JsonObject resolution=new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("resolution");
            assertEquals(128,resolution.get("width").getAsInt(),model);assertEquals(64,resolution.get("height").getAsInt(),model);
        }
    }
    private static byte[] bytes(String path)throws IOException {
        try(InputStream input=Objects.requireNonNull(CraftingRecipeCompatibilityTest.class.getResourceAsStream(path),path);ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int size;while((size=input.read(buffer))>=0)output.write(buffer,0,size);return output.toByteArray();
        }
    }

    private static LaunchClassLoader forgeLoader() throws Exception {
        List<URL> urls=new ArrayList<>();
        // One staged RED run loads the historical Registry before the current backend classes.
        String baseline=System.getProperty("mtr.recipeBaselineClasses");if(baseline!=null)urls.add(new File(baseline).toURI().toURL());
        for(String path:System.getProperty("mtr.testClasspath").split(File.pathSeparator))urls.add(new File(path).toURI().toURL());
        LaunchClassLoader loader=new LaunchClassLoader(urls.toArray(new URL[0]));
        loader.addClassLoaderExclusion("org.junit.");loader.addClassLoaderExclusion("org.opentest4j.");loader.addClassLoaderExclusion("org.apiguardian.");
        loader.addTransformerExclusion("jss.");Launch.classLoader=loader;Launch.blackboard=new HashMap<>();
        loader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        return loader;
    }
    private static void invoke(LaunchClassLoader loader,String method)throws Throwable {
        try { loader.loadClass("org.mtr.mapping.tool.CraftingRecipeScenarios").getMethod(method).invoke(null); }
        catch(InvocationTargetException error){throw error.getCause();}
    }
}
