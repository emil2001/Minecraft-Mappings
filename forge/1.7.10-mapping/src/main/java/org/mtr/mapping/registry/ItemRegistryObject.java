package org.mtr.mapping.registry;

import org.mtr.mapping.holder.Item;
import org.mtr.mapping.tool.RegistryObject;
import java.util.function.Consumer;

public final class ItemRegistryObject extends RegistryObject<Item> {
    Item value;
    public Item get() {
        if (value == null) throw new IllegalStateException("Item registration has not run");
        return value;
    }
    public boolean isPresent() { return value != null; }
    public void ifPresent(Consumer<Item> consumer) { if (isPresent()) consumer.accept(value); }
}
