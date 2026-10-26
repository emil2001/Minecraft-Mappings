package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mod.block.BlockEscalatorBase;
import org.mtr.mod.block.BlockEscalatorStep;
import org.mtr.mod.block.IBlock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class RawStateCodecScenarios {
    public void originalEscalatorStatesAndLargeMappedIndexesRoundTrip() {
        org.mtr.mod.Blocks.init();
        SlabCompatibilityScenarios.LargeRawStateBlock large=new SlabCompatibilityScenarios.LargeRawStateBlock();
        BlockPlacementScenarios.registerProbe("opaque_large_states",large);
        BlockState largest=large.getDefaultState2();
        for(int i=0;i<5;i++) largest=largest.with(new Property<>(SlabCompatibilityScenarios.LargeRawStateBlock.VALUES[i].data),15);
        final BlockState beyond=largest.with(new Property<>(SlabCompatibilityScenarios.LargeRawStateBlock.VALUES[5].data),1);
        assertEquals(0xFFFFF,largest.data.getRawStateIndex());
        assertThrows(IllegalArgumentException.class,()->Block.getRawIdFromState(beyond));
        assertEquals(largest,Block.getStateFromRawId(Block.getRawIdFromState(largest)));

        List<BlockState> states=new ArrayList<>();
        for(Direction facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST})
            for(boolean direction:new boolean[]{false,true}) for(boolean running:new boolean[]{false,true})
                for(BlockEscalatorBase.EnumEscalatorOrientation orientation:BlockEscalatorBase.EnumEscalatorOrientation.values())
                    for(IBlock.EnumSide side:new IBlock.EnumSide[]{IBlock.EnumSide.LEFT,IBlock.EnumSide.RIGHT})
                        states.add(org.mtr.mod.Blocks.ESCALATOR_STEP.get().getDefaultState()
                            .with(new Property<>(BlockEscalatorStep.FACING.data),facing.data)
                            .with(new Property<>(BlockEscalatorStep.DIRECTION.data),direction)
                            .with(new Property<>(BlockEscalatorStep.STATUS.data),running)
                            .with(new Property<>(BlockEscalatorBase.ORIENTATION.data),orientation)
                            .with(new Property<>(IBlock.SIDE.data),side));
        assertEquals(192,states.size());
        Map<BlockState,Integer> first=new HashMap<>();
        Collections.reverse(states);
        for(BlockState state:states) {
            int token=Block.getRawIdFromState(state);
            assertEquals(state,Block.getStateFromRawId(token)); first.put(state,token);
        }
        assertEquals(192,new HashSet<>(first.values()).size());
        Collections.reverse(states);
        for(BlockState state:states) assertEquals(first.get(state).intValue(),Block.getRawIdFromState(state));
        assertEquals(0,Block.getRawIdFromState(Blocks.getAirMapped().getDefaultState()));
        assertEquals(Blocks.getAirMapped().getDefaultState(),Block.getStateFromRawId(-1));
    }

    public void extendedNativeIdentityAndMetadataRemainOpaque() throws Exception {
        net.minecraft.block.Block nativeBlock=highNativeBlock();
        assertTrue(net.minecraft.block.Block.getIdFromBlock(nativeBlock)>4095);
        BlockStateDefinition definition=BlockStateDefinition.forBlock(nativeBlock);
        Set<Integer> tokens=new HashSet<>();
        for(int metadata:new int[]{0,15,16,255,256,0xFEDC,65535}) {
            BlockState state=new BlockState(definition.read(metadata,new NBTTagCompound()));
            int token=Block.getRawIdFromState(state); assertTrue(tokens.add(token));
            CompoundTag itemTag=new CompoundTag(new NBTTagCompound()); itemTag.putInt("block_id",token);
            BlockState restored=Block.getStateFromRawId(itemTag.getInt("block_id"));
            assertSame(nativeBlock,restored.getBlock().data);
            assertEquals(metadata,restored.data.write(new NBTTagCompound())); assertEquals(state,restored);
        }
        assertThrows(IllegalArgumentException.class,()->definition.read(-1,new NBTTagCompound()));
        assertThrows(IllegalArgumentException.class,()->definition.read(65536,new NBTTagCompound()));
        net.minecraft.block.Block unregistered=new net.minecraft.block.Block(net.minecraft.block.material.Material.rock) {};
        assertThrows(IllegalArgumentException.class,()->Block.getRawIdFromState(new Block(unregistered).getDefaultState()));
    }

    public void nativeWorldEventsUseTheInstalledEndlessIdsPayload() throws Exception {
        int[] seen={0};
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld() {
            @Override public void playAuxSFXAtEntity(net.minecraft.entity.player.EntityPlayer player,int event,int x,int y,int z,int value) { seen[0]=value; }
        };
        World world=new World(nativeWorld); BlockPos pos=new BlockPos(1,64,1);
        BlockEventScenarios.Extended mapped=new BlockEventScenarios.Extended();
        BlockPlacementScenarios.registerProbe("opaque_sfx_mapped",mapped);
        BlockState extended=mapped.getDefaultState2().with(new Property<>(BlockEventScenarios.Extended.VALUE.data),29);
        world.syncWorldEvent(null,2001,pos,Block.getRawIdFromState(extended));
        assertEquals(net.minecraft.block.Block.getIdFromBlock(mapped)|(13<<24),seen[0]);
        net.minecraft.block.Block high=highNativeBlock();
        BlockState state=new BlockState(BlockStateDefinition.forBlock(high).read(0xFEDC,new NBTTagCompound()));
        int token=Block.getRawIdFromState(state),id=net.minecraft.block.Block.getIdFromBlock(high);
        world.syncWorldEvent(null,2001,pos,token);
        assertEquals(id|(0xDC<<24),seen[0]);
        assertEquals(id,seen[0]&0xFFFFFF); assertEquals(0xDC,seen[0]>>>24);
        world.syncWorldEvent(null,2000,pos,token); assertEquals(token,seen[0]);
    }

    private static net.minecraft.block.Block highNativeBlock() throws Exception {
        Object existing=net.minecraft.block.Block.blockRegistry.getObject("mtr:opaque_high_native");
        if(existing!=net.minecraft.init.Blocks.air) return (net.minecraft.block.Block)existing;
        net.minecraft.block.Block block=new net.minecraft.block.Block(net.minecraft.block.material.Material.rock) {};
        // Faithful native registry seam: EndlessIDs raises FML's registration cap in the actual client.
        java.lang.reflect.Method add=net.minecraft.block.Block.blockRegistry.getClass().getDeclaredMethod("addObjectRaw",int.class,String.class,Object.class);
        add.setAccessible(true); add.invoke(net.minecraft.block.Block.blockRegistry,5000,"mtr:opaque_high_native",block);
        assertSame(block,net.minecraft.block.Block.getBlockById(5000));
        return block;
    }
}
