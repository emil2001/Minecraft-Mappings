package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.PropertyDefinition;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class EnumProperty<T extends Enum<T> & StringIdentifiable> extends HolderBase<PropertyDefinition<T>> {
    public EnumProperty(PropertyDefinition<T> data) { super(data); }
    public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(String name, Class<T> type) {
        return of(name, type, value -> true);
    }
    public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(String name, Class<T> type, Predicate<T> filter) {
        return new EnumProperty<>(new PropertyDefinition<>(name,
            Arrays.stream(type.getEnumConstants()).filter(filter).collect(Collectors.toList()), StringIdentifiable::asString2));
    }
}
