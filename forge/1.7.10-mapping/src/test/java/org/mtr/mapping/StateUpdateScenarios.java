package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public final class StateUpdateScenarios {
    public void stateNeighborUpdatesUseTheProvidedStateWithoutDuplicatingLegacyNotifications() {
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        World world=new World(nativeWorld); BlockPos pos=new BlockPos(4,64,4);
        BlockPlacementScenarios.DirectionalBlock block=new BlockPlacementScenarios.DirectionalBlock();
        BlockPlacementScenarios.registerProbe("state_update_neighbor",block);
        world.setBlockState(pos,block.getDefaultState2(),2);
        new Block(net.minecraft.init.Blocks.stone).getDefaultState().updateNeighbors(new WorldAccess(nativeWorld),pos.up(),2);
        assertEquals(Collections.singletonList(Direction.UP),block.directions);
        assertEquals(0,block.legacyCalls,"Modern state updates do not also dispatch native legacy callbacks");
        assertTrue(world.getBlockState(pos).isOf(new Block(block)),"The supplied source state, rather than the source's current air, must reach the neighbor");
        new Block(net.minecraft.init.Blocks.air).getDefaultState().updateNeighbors(new WorldAccess(nativeWorld),pos.up(),3);
        assertTrue(world.getBlockState(pos).isAir());
    }
}
