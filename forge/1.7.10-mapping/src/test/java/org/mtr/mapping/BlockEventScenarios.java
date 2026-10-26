package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.tool.HolderBase;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public final class BlockEventScenarios {
    public static final class Extended extends BlockExtension {
        static final IntegerProperty VALUE=IntegerProperty.of("value",0,31);
        Extended() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(VALUE); }
    }
    public void blockBreakParticlesReceiveNativeMetadataInsteadOfFullStateIndex() {
        int[] seen={-1};
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld() {
            @Override public void playAuxSFXAtEntity(net.minecraft.entity.player.EntityPlayer player,int event,int x,int y,int z,int value) { seen[0]=value; }
        };
        Extended block=new Extended(); BlockPlacementScenarios.registerProbe("break_event_extended",block);
        BlockState state=block.getDefaultState2().with(new Property<>(Extended.VALUE.data),29);
        int rawId=Block.getRawIdFromState(state);
        int nativeId=net.minecraft.block.Block.getIdFromBlock(block);
        assertEquals(state,Block.getStateFromRawId(rawId));
        World world=new World(nativeWorld); BlockPos pos=new BlockPos(1,64,1);
        world.syncWorldEvent(null,2001,pos,rawId);
        assertEquals(nativeId|(13<<24),seen[0],"EndlessIDs break particles take native metadata in the upper eight bits");
        world.syncWorldEvent(null,2000,pos,rawId);
        assertEquals(rawId,seen[0],"Unrelated event payloads must remain unchanged");
    }
}
