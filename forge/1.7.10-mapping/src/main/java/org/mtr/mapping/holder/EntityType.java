package org.mtr.mapping.holder;

import org.mtr.mapping.mapper.EntityExtension;
import org.mtr.mapping.registry.EntityFactoryRegistry;
import org.mtr.mapping.tool.HolderBase;

public final class EntityType<T extends EntityExtension> extends HolderBase<EntityFactoryRegistry.Entry<T>> {
    public EntityType(EntityFactoryRegistry.Entry<T> data) { super(data); }
}
