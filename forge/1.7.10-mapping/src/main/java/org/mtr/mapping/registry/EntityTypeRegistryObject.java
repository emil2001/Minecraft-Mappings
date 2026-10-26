package org.mtr.mapping.registry;

import org.mtr.mapping.holder.EntityType;
import org.mtr.mapping.mapper.EntityExtension;
import org.mtr.mapping.tool.RegistryObject;
import java.util.function.Consumer;

public final class EntityTypeRegistryObject<T extends EntityExtension> extends RegistryObject<EntityType<T>> {
    EntityType<T> value;
    public EntityType<T> get() {
        if (value == null) throw new IllegalStateException("Entity registration has not run");
        return value;
    }
    public boolean isPresent() { return value != null; }
    public void ifPresent(Consumer<EntityType<T>> consumer) { if (isPresent()) consumer.accept(value); }
}
