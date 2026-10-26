package org.mtr.mapping.tool;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class RawStateRangeScenarios {
    private static final class MillionStates extends BlockExtension {
        static final IntegerProperty[] VALUES={IntegerProperty.of("a",0,15),IntegerProperty.of("b",0,15),
            IntegerProperty.of("c",0,15),IntegerProperty.of("d",0,15),IntegerProperty.of("e",0,15)};
        MillionStates() { super(new BlockSettings(net.minecraft.block.material.Material.rock)); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { Collections.addAll(properties,VALUES); }
    }
    public void prefixRangesAreDeterministicUnsignedAndCapacityChecked() {
        assertEquals(65536,BlockStateDefinition.forBlock(net.minecraft.init.Blocks.stone).getRawStateCount());
        BlockExtension empty=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.rock));
        assertEquals(16,empty.getStateDefinition().getRawStateCount());
        assertEquals(1048576,new MillionStates().getStateDefinition().getRawStateCount());
        Map<String,net.minecraft.block.Block> emptyRegistry=new LinkedHashMap<>();
        emptyRegistry.put("minecraft:air",net.minecraft.init.Blocks.air); emptyRegistry.put("mtr:empty",empty);
        RawBlockStateIds.Palette emptyPalette=new RawBlockStateIds.Palette(emptyRegistry);
        for(int metadata:new int[]{0,15}) {
            BlockStateDefinition.State state=empty.getStateDefinition().read(metadata,new NBTTagCompound());
            assertEquals(state,emptyPalette.fromId(emptyPalette.toId(state)));
        }
        assertThrows(IllegalArgumentException.class,()->empty.getStateDefinition().read(16,new NBTTagCompound()));
        Map<String,net.minecraft.block.Block> registry=new LinkedHashMap<>();
        registry.put("z:last",net.minecraft.init.Blocks.stone);
        registry.put("a:first",net.minecraft.init.Blocks.planks);
        registry.put("minecraft:air",net.minecraft.init.Blocks.air);
        RawBlockStateIds.Palette first=new RawBlockStateIds.Palette(registry);
        List<String> keys=new ArrayList<>(registry.keySet()); Collections.reverse(keys);
        Map<String,net.minecraft.block.Block> reverse=new LinkedHashMap<>(); for(String key:keys) reverse.put(key,registry.get(key));
        RawBlockStateIds.Palette restart=new RawBlockStateIds.Palette(reverse);
        for(net.minecraft.block.Block block:registry.values()) for(int metadata:new int[]{0,16,255,256,65535}) {
            BlockStateDefinition.State state=BlockStateDefinition.forBlock(block).read(metadata,new NBTTagCompound());
            int token=first.toId(state); assertEquals(token,restart.toId(state)); assertEquals(state,restart.fromId(token));
        }
        assertEquals(0,first.toId(BlockStateDefinition.forBlock(net.minecraft.init.Blocks.air).defaultState()));
        assertEquals(65536,first.toId(BlockStateDefinition.forBlock(net.minecraft.init.Blocks.planks).defaultState()));
        assertEquals(131072,first.toId(BlockStateDefinition.forBlock(net.minecraft.init.Blocks.stone).defaultState()));
        assertSame(net.minecraft.init.Blocks.air,first.fromId(-1).definition.block);

        Map<String,net.minecraft.block.Block> large=new LinkedHashMap<>(); large.put("minecraft:air",net.minecraft.init.Blocks.air);
        MillionStates last=null;
        for(int i=0;i<2049;i++) { last=new MillionStates(); large.put(String.format(java.util.Locale.ROOT,"mtr:wide_%04d",i),last); }
        RawBlockStateIds.Palette unsigned=new RawBlockStateIds.Palette(large);
        int negative=unsigned.toId(last.getDefaultState2().data);
        assertTrue(negative<0,"Every unsigned32 token bit must survive the int API");
        NBTTagCompound item=new NBTTagCompound(); item.setInteger("block_id",negative);
        assertEquals(last.getDefaultState2().data,unsigned.fromId(item.getInteger("block_id")));
        for(int i=2049;i<4096;i++) large.put(String.format(java.util.Locale.ROOT,"mtr:wide_%04d",i),new MillionStates());
        assertThrows(IllegalStateException.class,()->new RawBlockStateIds.Palette(large),"Overflow must fail before ranges wrap onto another block");
    }
}
