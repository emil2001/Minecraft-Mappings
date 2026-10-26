package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.PropertyDefinition;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class IntegerProperty extends HolderBase<PropertyDefinition<Integer>> {
    public IntegerProperty(PropertyDefinition<Integer> data) { super(data); }
    public static IntegerProperty of(String name, int min, int max) {
        if (min < 0 || max <= min) throw new IllegalArgumentException("Invalid integer property range");
        return new IntegerProperty(new PropertyDefinition<>(name,
            IntStream.rangeClosed(min, max).boxed().collect(Collectors.toList()), Object::toString));
    }
}
