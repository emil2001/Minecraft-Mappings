package org.mtr.mod;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.registry.ItemRegistryObject;

/** Test-only external item dependencies; all block behavior comes from upstream sources. */
public final class Items {
    public static final ItemRegistryObject BRUSH = Blocks.REGISTRY.registerItem(new Identifier("mtr", "brush"), settings -> new Item(new ItemExtension(settings)));
    public static final ItemRegistryObject ESCALATOR = Blocks.REGISTRY.registerItem(new Identifier("mtr", "escalator"), settings -> new Item(new ItemExtension(settings)));
    public static void init() {}
}
