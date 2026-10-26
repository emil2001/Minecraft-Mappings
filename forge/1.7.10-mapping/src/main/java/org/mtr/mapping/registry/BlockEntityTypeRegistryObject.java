package org.mtr.mapping.registry;

import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.tool.RegistryObject;
import java.util.function.Consumer;

public final class BlockEntityTypeRegistryObject<T extends BlockEntityExtension> extends RegistryObject<BlockEntityType<T>> {
    BlockEntityType<T> value;
    public BlockEntityType<T> get() {
        if (value == null) throw new IllegalStateException("Block entity registration has not run");
        return value;
    }
    public boolean isPresent() { return value != null; }
    public void ifPresent(Consumer<BlockEntityType<T>> consumer) { if (isPresent()) consumer.accept(value); }
}
