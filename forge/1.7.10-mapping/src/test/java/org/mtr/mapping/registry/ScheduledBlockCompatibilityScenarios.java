package org.mtr.mapping.registry;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.tool.HolderBase;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public final class ScheduledBlockCompatibilityScenarios {
    public static final class Pulse extends BlockExtension {
        static final IntegerProperty PHASE=IntegerProperty.of("phase",0,2);
        int ticks,neighbors; BlockPos source;
        Pulse() { super(new BlockSettings(net.minecraft.block.material.Material.rock)); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(PHASE); }
        @Override public void scheduledTick2(BlockState state,ServerWorld world,BlockPos pos,Random random) {
            assertSame(world.data.rand,random.data); ticks++;
            assertFalse(hasScheduledBlockTick(World.cast(world),pos,new Block(this)),"An executing tick is no longer queued");
            int phase=state.get(new Property<>(PHASE.data)); world.setBlockState(pos,state.with(new Property<>(PHASE.data),phase+1));
            if (phase==0) scheduleBlockTick(World.cast(world),pos,new Block(this),2);
        }
        @Override public boolean emitsRedstonePower2(BlockState state) { return true; }
        @Override public int getWeakRedstonePower2(BlockState state,BlockView world,BlockPos pos,Direction direction) {
            return direction==Direction.EAST?state.get(new Property<>(PHASE.data))*7:0;
        }
        @Override public int getStrongRedstonePower2(BlockState state,BlockView world,BlockPos pos,Direction direction) {
            return direction==Direction.UP?3:0;
        }
        @Override public void neighborUpdate2(BlockState state,World world,BlockPos pos,Block sourceBlock,BlockPos sourcePos,boolean notify) {
            neighbors++; source=sourcePos;
        }
    }
    public void queuedTicksAndDirectionalPowerUseNativeWorld() throws Exception {
        Registry registry=new Registry(); BlockRegistryObject registered=registry.registerBlock(new Identifier("mtr:scheduled_pulse_probe"),() -> new Block(new Pulse())); registry.init();
        Pulse block=(Pulse)registered.get().data;
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            World world=new World(fixture.world); BlockPos pos=new BlockPos(-1,64,-1);
            world.setBlockState(pos,block.getDefaultState2()); Block target=new Block(block);
            BlockExtension.scheduleBlockTick(world,pos,target,4); BlockExtension.scheduleBlockTick(world,pos,target,4);
            assertTrue(BlockExtension.hasScheduledBlockTick(world,pos,target),"Must find future ticks, not only the native active-tick list");
            fixture.world.getWorldInfo().incrementTotalWorldTime(3); fixture.world.tickUpdates(false); assertEquals(0,block.ticks);
            fixture.world.getWorldInfo().incrementTotalWorldTime(4); fixture.world.tickUpdates(false); assertEquals(1,block.ticks);
            assertTrue(BlockExtension.hasScheduledBlockTick(world,pos,target)); assertTrue(block.canProvidePower());
            assertTrue(world.isEmittingRedstonePower(pos,Direction.EAST)); assertFalse(world.isEmittingRedstonePower(pos,Direction.WEST));
            assertEquals(7,fixture.world.getIndirectPowerLevelTo(-1,64,-1,Direction.EAST.ordinal()));
            assertEquals(3,fixture.world.isBlockProvidingPowerTo(-1,64,-1,Direction.UP.ordinal()));
            fixture.world.getWorldInfo().incrementTotalWorldTime(6); fixture.world.tickUpdates(false); assertEquals(2,block.ticks);
            assertFalse(BlockExtension.hasScheduledBlockTick(world,pos,target));
            BlockPos neighbor=pos.west(); world.updateNeighbors(neighbor,new Block(net.minecraft.init.Blocks.stone));
            assertEquals(neighbor,block.source); assertEquals(1,block.neighbors);
            fixture.world.notifyBlockOfNeighborChange(pos.getX(),pos.getY(),pos.getZ(),net.minecraft.init.Blocks.stone);
            assertEquals(pos,block.source,"Legacy direct notifications have no source coordinates"); assertEquals(2,block.neighbors);
            BlockExtension.scheduleBlockTick(world,pos,target,2); world.setBlockState(pos,new Block(net.minecraft.init.Blocks.air).getDefaultState());
            fixture.world.getWorldInfo().incrementTotalWorldTime(8); fixture.world.tickUpdates(false); assertEquals(2,block.ticks,"Removed blocks must not tick");
        }
    }
}
