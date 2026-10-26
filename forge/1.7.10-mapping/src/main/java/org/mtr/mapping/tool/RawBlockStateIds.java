package org.mtr.mapping.tool;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import java.util.*;

/** Opaque, deterministic state IDs for the pinned registry, independent of native numeric IDs. */
public final class RawBlockStateIds {
    private static Palette cached;
    private static int registrySize=-1;
    private RawBlockStateIds() {}

    public static int encode(BlockStateDefinition.State state) { return current().toId(state); }
    public static BlockStateDefinition.State decode(int id) { return current().fromId(id); }

    private static synchronized Palette current() {
        Set<?> names=Block.blockRegistry.getKeys();
        // The game registry is fixed before gameplay. Rebuild for test/bootstrap registrations.
        if(cached==null || registrySize!=names.size()) {
            Map<String,Block> blocks=new HashMap<>();
            for(Object key:names) {
                String name=(String)key;
                blocks.put(name,(Block)Block.blockRegistry.getObject(name));
            }
            cached=new Palette(blocks);
            registrySize=names.size();
        }
        return cached;
    }

    static final class Palette {
        private final Range[] ranges;
        private final Map<Block,Range> byBlock=new IdentityHashMap<>();
        private final long size;

        Palette(Map<String,Block> registry) {
            if(registry.get("minecraft:air")!=Blocks.air) throw new IllegalStateException("The state registry must contain canonical air");
            List<String> names=new ArrayList<>(registry.keySet());
            Collections.sort(names);
            names.remove("minecraft:air"); names.add(0,"minecraft:air");
            ranges=new Range[names.size()]; long next=0;
            for(int i=0;i<names.size();i++) {
                Block block=Objects.requireNonNull(registry.get(names.get(i)));
                BlockStateDefinition definition=BlockStateDefinition.forBlock(block);
                int count=definition.getRawStateCount();
                if(count<=0 || next+count>0x100000000L) throw new IllegalStateException("Registered block states exceed unsigned32 IDs");
                Range range=new Range(definition,next,count);
                if(byBlock.put(block,range)!=null) throw new IllegalStateException("One block has multiple canonical registry names");
                ranges[i]=range; next+=count;
            }
            size=next;
        }

        int toId(BlockStateDefinition.State state) {
            Range range=byBlock.get(state.definition.block);
            if(range==null) throw new IllegalArgumentException("Block is not registered in the state registry");
            int index=state.getRawStateIndex();
            if(index<0 || index>=range.count || !state.equals(range.definition.readRawStateIndex(index)))
                throw new IllegalArgumentException("State does not belong to the registered block state domain");
            return (int)(range.start+index);
        }

        BlockStateDefinition.State fromId(int id) {
            long unsigned=Integer.toUnsignedLong(id);
            if(unsigned>=size) return ranges[0].definition.defaultState();
            int low=0,high=ranges.length-1;
            while(low<=high) {
                int middle=(low+high)>>>1; Range range=ranges[middle];
                if(unsigned<range.start) high=middle-1;
                else if(unsigned>=range.start+range.count) low=middle+1;
                else return range.definition.readRawStateIndex((int)(unsigned-range.start));
            }
            throw new IllegalStateException("State registry contains a gap");
        }
    }

    private static final class Range {
        final BlockStateDefinition definition;
        final long start;
        final int count;
        Range(BlockStateDefinition definition,long start,int count) { this.definition=definition; this.start=start; this.count=count; }
    }
}
