package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.LegacyItemModels;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemModelCompatibilityTest {
    @Test void originalSelectedItemsKeepEveryLayerAndDriverKeysHideOnlyInFirstPerson() throws Exception {
        LegacyItemModels models=new LegacyItemModels(id -> {
            if (!id.getNamespace().equals("mtr")) return null;
            Path file=Paths.get(System.getProperty("mtr.modelResources")).getParent().resolve(id.getPath());
            try { return Files.exists(file)?new String(Files.readAllBytes(file),StandardCharsets.UTF_8):null; }
            catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
        });
        Identifier rail=new Identifier("mtr:item/rail_connector_20");
        assertEquals(Collections.singletonList(new Identifier("mtr:item/rail_connector_20")),models.select(rail,key -> 0).layers);
        assertEquals(Arrays.asList(new Identifier("mtr:item/rail_connector_20"),new Identifier("mtr:item/rail_connector_selected")),models.select(rail,key -> 1).layers);
        Identifier remover=new Identifier("mtr:item/signal_remover_red");
        assertEquals(3,models.select(remover,key -> 1).layers.size());
        LegacyItemModels.Model driver=models.select(new Identifier("mtr:item/basic_driver_key"),key -> 0);
        assertTrue(driver.handheld); assertTrue(driver.hiddenFirstPerson); assertEquals(Collections.singletonList(new Identifier("mtr:item/driver_key")),driver.layers);
        LegacyItemModels.Model boat=models.select(new Identifier("mtr:item/boat_node"),key -> 0);
        assertFalse(boat.generated); assertEquals(18,boat.faces.size());
        assertTrue(models.textures(rail).contains(new Identifier("mtr:item/rail_connector_selected")));
    }
    @Test void all176RegisteredOriginalBlockItemsDecodeWithoutBuiltinEntityModels() throws Exception {
        Path assets=Paths.get(System.getProperty("mtr.modelResources")).getParent().getParent();
        String registrations=new String(Files.readAllBytes(assets.getParent().getParent().resolve("java/org/mtr/mod/Blocks.java")),StandardCharsets.UTF_8);
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("registerBlockWithBlockItem\\(new Identifier\\(Init\\.(MOD_ID(?:_NTE)?), \"([^\"]+)\"").matcher(registrations);
        LegacyItemModels models=new LegacyItemModels(id -> {
            Path file=assets.resolve(id.getNamespace()).resolve(id.getPath());
            try { return Files.exists(file)?new String(Files.readAllBytes(file),StandardCharsets.UTF_8):null; }
            catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        });
        int count=0;
        while (matcher.find()) {
            Identifier id=new Identifier(matcher.group(1).equals("MOD_ID")?"mtr":"mtrsteamloco","item/"+matcher.group(2));
            assertNotNull(models.model(id),id.toString()); assertFalse(models.textures(id).isEmpty(),id.toString()); count++;
        }
        assertEquals(176,count);
    }
    @Test void lastMatchingOverrideThresholdAndInheritedTextureAliasUseOriginalResourcePriority() {
        Map<String,String> resources=new HashMap<>();
        resources.put("mtr:models/item/base.json","{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"#paint\",\"paint\":\"mtr:item/red\"}}");
        resources.put("mtr:models/item/probe.json","{\"parent\":\"mtr:item/base\",\"textures\":{\"paint\":\"mtr:item/blue\"},\"overrides\":[{\"predicate\":{\"mtr:selected\":0.5},\"model\":\"mtr:item/a\"},{\"predicate\":{\"mtr:selected\":1},\"model\":\"mtr:item/b\"}]}");
        resources.put("mtr:models/item/a.json","{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"mtr:item/a\"}}");
        resources.put("mtr:models/item/b.json","{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"mtr:item/b\"}}");
        LegacyItemModels models=new LegacyItemModels(id -> resources.get(id.toString())); Identifier probe=new Identifier("mtr:item/probe");
        assertEquals("mtr:item/blue",models.select(probe,key -> .49).layers.get(0).toString());
        assertEquals("mtr:item/a",models.select(probe,key -> .5).layers.get(0).toString());
        assertEquals("mtr:item/b",models.select(probe,key -> 1).layers.get(0).toString());
        assertEquals("mtr:item/blue",models.select(probe,key -> Double.NEGATIVE_INFINITY).layers.get(0).toString());
        resources.put("mtr:models/item/probe.json","{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"mtr:item/pack_override\"}}");
        assertEquals("mtr:item/pack_override",new LegacyItemModels(id -> resources.get(id.toString())).select(probe,key -> 1).layers.get(0).toString());
    }
}
