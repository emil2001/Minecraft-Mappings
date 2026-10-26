package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.PropertyDefinition;
import java.util.Collection;

public final class Property<T extends Comparable<T>> extends HolderBase<PropertyDefinition<T>> {
    public Property(PropertyDefinition<T> data) { super(data); }
    public Collection<T> getValues() { return data.getValues(); }
}
