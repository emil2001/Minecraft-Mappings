package org.mtr.mapping.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Typed values and stable names for properties absent from Minecraft 1.7.10. */
public final class PropertyDefinition<T extends Comparable<T>> {
    private final String name;
    private final List<T> values;
    private final Map<String, T> parsed = new LinkedHashMap<>();
    private final Map<T, String> names = new LinkedHashMap<>();

    public PropertyDefinition(String name, List<T> values, Function<T, String> naming) {
        if (name == null || !name.matches("[a-z0-9_]+") || values.isEmpty()) {
            throw new IllegalArgumentException("Invalid property: " + name);
        }
        this.name = name;
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
        for (T value : values) {
            if (value == null) throw new IllegalArgumentException("Null property value");
            String serialized = naming.apply(value);
            if (serialized == null || !serialized.matches("[a-z0-9_]+")
                    || names.put(value, serialized) != null || parsed.put(serialized, value) != null) {
                throw new IllegalArgumentException("Duplicate or invalid property value for " + name);
            }
        }
    }
    public String getName() { return name; }
    public List<T> getValues() { return values; }
    public List<String> getSerializedValues() { return Collections.unmodifiableList(new ArrayList<>(parsed.keySet())); }
    public Optional<T> parse(String serialized) { return Optional.ofNullable(parsed.get(serialized)); }
    public String name(T value) {
        String serialized = names.get(value);
        if (serialized == null) throw new IllegalArgumentException("Invalid value for " + name);
        return serialized;
    }
}
