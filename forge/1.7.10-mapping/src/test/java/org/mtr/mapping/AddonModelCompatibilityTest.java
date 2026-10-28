package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.LegacyBlockModels;
import org.mtr.mapping.tool.LegacyItemModels;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddonModelCompatibilityTest {
    @Test void unresolvedTextureOnlyUsesMissingSpriteOnItsOwnFace() {
        Map<String, String> resources = new HashMap<>();
        resources.put("addon:blockstates/panel.json", "{\"variants\":{\"\":{\"model\":\"addon:block/panel\"}}}");
        resources.put("addon:models/block/panel.json", "{\"textures\":{\"shell\":\"addon:block/metal\",\"particle\":\"#missing\"},"
            + "\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,1],\"faces\":{"
            + "\"north\":{\"texture\":\"#shell\"},\"south\":{\"texture\":\"#missing\"}}}]}");
        LegacyBlockModels models = new LegacyBlockModels(id -> resources.get(id.toString()));
        Identifier block = new Identifier("addon:panel");
        List<LegacyBlockModels.Quad> faces = models.resolve(block, Collections.emptyMap(), 0);
        assertEquals(2, faces.size(), "One unresolved texture must not replace the complete model with a cube");
        assertEquals(new Identifier("addon:block/metal"), faces.get(0).texture);
        assertEquals(new Identifier("minecraft:missingno"), faces.get(1).texture);
        assertEquals(Collections.singleton(new Identifier("addon:block/metal")), models.textures(block),
            "The built-in missing sprite must never be queued as a PNG to load");
        assertEquals(new Identifier("minecraft:missingno"), models.particle(block, Collections.emptyMap(), 0));
    }

    @Test void partialTextureItemNeverQueuesTheBuiltInMissingSpriteAsPng() {
        Map<String, String> resources = new HashMap<>();
        resources.put("addon:models/item/panel.json", "{\"textures\":{\"shell\":\"addon:item/metal\"},"
            + "\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,1],\"faces\":{"
            + "\"north\":{\"texture\":\"#shell\"},\"south\":{\"texture\":\"#missing\"}}}]}");
        LegacyItemModels models = new LegacyItemModels(id -> resources.get(id.toString()));
        Identifier item = new Identifier("addon:item/panel");
        LegacyItemModels.Model model = models.model(item);
        assertEquals(false, model.generated);
        assertEquals(2, model.faces.size());
        assertEquals(new Identifier("addon:item/metal"), model.faces.get(0).texture);
        assertEquals(new Identifier("minecraft:missingno"), model.faces.get(1).texture);
        assertEquals(Collections.singleton(new Identifier("addon:item/metal")), models.textures(item),
            "Item stitching must use the native missing sprite without loading a nonexistent PNG");
    }

    public static void main(String[] args) {
        new AddonModelCompatibilityTest().unresolvedTextureOnlyUsesMissingSpriteOnItsOwnFace();
        System.out.println("PASS unresolved texture keeps valid faces and uses the built-in missing sprite");
        new AddonModelCompatibilityTest().partialTextureItemNeverQueuesTheBuiltInMissingSpriteAsPng();
        System.out.println("PASS item texture collection excludes the built-in missing sprite");
    }
}
