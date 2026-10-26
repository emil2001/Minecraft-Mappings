package org.mtr.mapping.registry;

import org.mtr.mapping.holder.Block;
import org.mtr.mapping.tool.RegistryObject;
import java.util.function.Consumer;

public final class BlockRegistryObject extends RegistryObject<Block> {
    Block value;
    public Block get() {
        if (value == null) throw new IllegalStateException("Block registration has not run");
        return value;
    }
    public boolean isPresent() { return value != null; }
    public void ifPresent(Consumer<Block> consumer) { if (isPresent()) consumer.accept(value); }
}
