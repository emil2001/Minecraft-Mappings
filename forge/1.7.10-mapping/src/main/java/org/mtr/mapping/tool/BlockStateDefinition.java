package org.mtr.mapping.tool;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.holder.BlockAbstractMapping;
import org.mtr.mapping.holder.SlabType;
import org.mtr.mapping.mapper.SlabBlockExtension;

import java.util.*;

public final class BlockStateDefinition {
    public final Block block;
    private final Map<String, PropertyDefinition<?>> properties = new TreeMap<>();
    private final BlockStateStorage storage;

    public BlockStateDefinition(Block block, List<HolderBase<?>> holders) {
        this.block = Objects.requireNonNull(block);
        if (NativeSlabStates.supports(block)) holders=Collections.singletonList(new Property<>(SlabBlockExtension.TYPE));
        Map<String, List<String>> domains = new TreeMap<>();
        for (HolderBase<?> holder : holders) {
            if (!(holder.data instanceof PropertyDefinition)) throw new IllegalArgumentException("Not a block property");
            PropertyDefinition<?> property = (PropertyDefinition<?>) holder.data;
            if (properties.put(property.getName(), property) != null) throw new IllegalArgumentException("Duplicate property " + property.getName());
            domains.put(property.getName(), property.getSerializedValues());
        }
        storage = new BlockStateStorage(domains);
    }

    public static BlockStateDefinition forBlock(Block block) {
        return block instanceof BlockAbstractMapping?((BlockAbstractMapping)block).getStateDefinition():new BlockStateDefinition(block,Collections.emptyList());
    }

    public boolean requiresTileEntity() { return storage.requiresTileEntity(); }
    public int getRawStateCount() {
        if (!(block instanceof BlockAbstractMapping)) return 65536;
        if (properties.isEmpty()) return 16;
        long count=1;
        for (PropertyDefinition<?> property:properties.values())
            count=Math.min(0x100000L,count*property.getSerializedValues().size());
        return (int)count;
    }
    public State defaultState() {
        if (NativeSlabStates.supports(block)) return read(0,new NBTTagCompound());
        Map<String, String> values = new TreeMap<>();
        properties.forEach((name, property) -> values.put(name, property.getSerializedValues().get(0)));
        return new State(values, 0);
    }
    public State read(int metadata, NBTTagCompound tag) {
        // EndlessIDs extends native metadata; MTR's own state storage still uses four bits.
        if (metadata < 0 || metadata > (block instanceof BlockAbstractMapping ? 15 : 65535)) throw new IllegalArgumentException("Invalid block metadata: " + metadata);
        if (NativeSlabStates.supports(block)) return new State(Collections.singletonMap("type",NativeSlabStates.type(block,metadata).asString2()),metadata);
        return new State(properties.isEmpty() ? Collections.emptyMap() : storage.read(metadata, tag), metadata);
    }
    public State readRawStateIndex(int index) {
        if (index<0 || index>0xFFFFF) throw new IllegalArgumentException("Raw block state index exceeds twenty bits");
        if (properties.isEmpty() || NativeSlabStates.supports(block)) return read(index,new NBTTagCompound());
        Map<String,String> values=new TreeMap<>();
        int remaining=index;
        for (PropertyDefinition<?> property:properties.values()) {
            List<String> domain=property.getSerializedValues();
            values.put(property.getName(),domain.get(remaining%domain.size()));
            remaining/=domain.size();
        }
        if (remaining!=0) throw new IllegalArgumentException("Invalid raw state index for this block");
        return new State(values,index&15);
    }

    public final class State {
        public final BlockStateDefinition definition = BlockStateDefinition.this;
        private final Map<String, String> values;
        private final int nativeMetadata;
        private State(Map<String, String> values, int nativeMetadata) {
            this.values = Collections.unmodifiableMap(new TreeMap<>(values));
            this.nativeMetadata = nativeMetadata;
        }
        public <T extends Comparable<T>> boolean contains(Property<T> property) {
            return properties.get(property.data.getName()) == property.data;
        }
        public <T extends Comparable<T>> T get(Property<T> property) {
            if (!contains(property)) throw new IllegalArgumentException("Property does not belong to this block");
            return property.data.parse(values.get(property.data.getName())).get();
        }
        public <T extends Comparable<T>, V extends T> State with(Property<T> property, V value) {
            if (!contains(property)) throw new IllegalArgumentException("Property does not belong to this block");
            if (NativeSlabStates.supports(block) && property.data==SlabBlockExtension.TYPE) {
                SlabType type=(SlabType)value;
                if (NativeSlabStates.type(block,nativeMetadata)==type) return this;
                return forBlock(NativeSlabStates.blockForType(block,type)).read((nativeMetadata&~8)|(type==SlabType.TOP?8:0),new NBTTagCompound());
            }
            Map<String, String> changed = new TreeMap<>(values);
            changed.put(property.data.getName(), property.data.name(value));
            return new State(changed, nativeMetadata);
        }
        public int write(NBTTagCompound tag) {
            return properties.isEmpty() || NativeSlabStates.supports(block) ? nativeMetadata : storage.write(values, tag);
        }
        public int getRawStateIndex() {
            if (properties.isEmpty() || NativeSlabStates.supports(block)) return nativeMetadata;
            long index=0,multiplier=1;
            for (PropertyDefinition<?> property:properties.values()) {
                List<String> domain=property.getSerializedValues();
                index+=domain.indexOf(values.get(property.getName()))*multiplier;
                if (index>0xFFFFF) throw new IllegalArgumentException("Raw block state index exceeds twenty bits");
                multiplier=Math.min(0x100000L,multiplier*domain.size());
            }
            return (int)index;
        }
        public Map<String, String> getSerializedProperties() { return values; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof BlockStateDefinition.State)) return false;
            State state = (State) other;
            return block == state.definition.block && values.equals(state.values)
                && (!properties.isEmpty() && !NativeSlabStates.supports(block) || nativeMetadata == state.nativeMetadata);
        }
        @Override public int hashCode() {
            return Objects.hash(block, values, properties.isEmpty() || NativeSlabStates.supports(block) ? nativeMetadata : 0);
        }
    }
}
