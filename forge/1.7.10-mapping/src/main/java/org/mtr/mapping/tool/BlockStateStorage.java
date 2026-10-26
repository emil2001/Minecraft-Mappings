package org.mtr.mapping.tool;

import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Persists MTR property names and values alongside the legacy block metadata. */
public final class BlockStateStorage {
    private static final String STATE_KEY = "mtr_block_state";
    private final Map<String, List<String>> domains = new LinkedHashMap<>();
    private final boolean extended;

    public BlockStateStorage(Map<String, List<String>> properties) {
        int combinations = 1;
        for (Map.Entry<String, List<String>> entry : new TreeMap<>(properties).entrySet()) {
            String name = Objects.requireNonNull(entry.getKey());
            List<String> values = new ArrayList<>(entry.getValue());
            if (name.isEmpty() || values.isEmpty() || values.contains(null)
                    || new HashSet<>(values).size() != values.size()) {
                throw new IllegalArgumentException("Invalid block property " + name);
            }
            domains.put(name, Collections.unmodifiableList(values));
            combinations = combinations > 16 / values.size() ? 17 : combinations * values.size();
        }
        extended = combinations > 16;
    }

    public boolean requiresTileEntity() { return extended; }

    public int write(Map<String, String> state, NBTTagCompound blockEntityTag) {
        if (!state.keySet().equals(domains.keySet())) {
            throw new IllegalArgumentException("Block state properties do not match its definition");
        }
        NBTTagCompound stored = new NBTTagCompound();
        int metadata = 0;
        int multiplier = 1;
        for (Map.Entry<String, List<String>> property : domains.entrySet()) {
            String value = state.get(property.getKey());
            int index = property.getValue().indexOf(value);
            if (index < 0) throw new IllegalArgumentException("Invalid value for " + property.getKey());
            metadata = (metadata + index * multiplier) & 15;
            multiplier = (multiplier * property.getValue().size()) & 15;
            if (extended) stored.setString(property.getKey(), value);
        }
        if (extended) blockEntityTag.setTag(STATE_KEY, stored);
        else blockEntityTag.removeTag(STATE_KEY);
        return metadata;
    }

    public Map<String, String> read(int metadata, NBTTagCompound blockEntityTag) {
        if (metadata < 0 || metadata > 15) throw new IllegalArgumentException("Metadata must fit four bits");
        if (extended && !blockEntityTag.hasKey(STATE_KEY, 10)) {
            throw new IllegalArgumentException("Extended MTR block state is missing");
        }
        NBTTagCompound stored = blockEntityTag.getCompoundTag(STATE_KEY);
        Map<String, String> state = new LinkedHashMap<>();
        int remaining = metadata;
        for (Map.Entry<String, List<String>> property : domains.entrySet()) {
            String value;
            if (extended) {
                value = stored.getString(property.getKey());
                if (!property.getValue().contains(value)) {
                    throw new IllegalArgumentException("Invalid stored value for " + property.getKey());
                }
            } else {
                value = property.getValue().get(remaining % property.getValue().size());
                remaining /= property.getValue().size();
            }
            state.put(property.getKey(), value);
        }
        if (!extended && remaining != 0) throw new IllegalArgumentException("Invalid metadata for this block");
        return Collections.unmodifiableMap(state);
    }
}
