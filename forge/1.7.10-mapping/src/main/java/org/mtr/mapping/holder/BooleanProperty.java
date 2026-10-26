package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.PropertyDefinition;
import java.util.Arrays;

public final class BooleanProperty extends HolderBase<PropertyDefinition<Boolean>> {
    public BooleanProperty(PropertyDefinition<Boolean> data) { super(data); }
    public static BooleanProperty of(String name) {
        return new BooleanProperty(new PropertyDefinition<>(name, Arrays.asList(true, false), Object::toString));
    }
}
