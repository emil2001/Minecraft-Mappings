package org.mtr.mapping.registry;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockItemExtension;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.tool.HolderBase;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public final class RandomTickGlintScenarios {
    public static final class Probe extends BlockExtension {
        static final BooleanProperty ACTIVE=BooleanProperty.of("active");
        int randomTicks,scheduledTicks,displayTicks; BlockState seen;
        Probe() {
            super(new BlockSettings(net.minecraft.block.material.Material.rock));
            setDefaultState2(getDefaultState2().with(new Property<>(ACTIVE.data),false));
        }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(ACTIVE); }
        public boolean hasRandomTicks2(BlockState state) { return state.get(new Property<>(ACTIVE.data)); }
        public void randomTick2(BlockState state,ServerWorld world,BlockPos pos,org.mtr.mapping.holder.Random random) {
            assertSame(world.data.rand,random.data); assertTrue(hasRandomTicks2(state)); randomTicks++; seen=state;
        }
        @Override public void scheduledTick2(BlockState state,ServerWorld world,BlockPos pos,org.mtr.mapping.holder.Random random) { scheduledTicks++; seen=state; }
        public void randomDisplayTick2(BlockState state,World world,BlockPos pos,org.mtr.mapping.holder.Random random) { displayTicks++; seen=state; }
    }
    private static final class NativeProbe extends net.minecraft.block.Block {
        int ticks;
        NativeProbe() { super(net.minecraft.block.material.Material.rock); setTickRandomly(true); }
        @Override public void updateTick(net.minecraft.world.World world,int x,int y,int z,java.util.Random random) { ticks++; }
    }
    private static final class RandomWorld extends net.minecraft.world.WorldServer {
        RandomWorld(ServerLifecycleScenarios.ServerFixture fixture) {
            super(fixture.server,fixture.world.getSaveHandler(),"random-test",0,
                new WorldSettings(123,WorldSettings.GameType.CREATIVE,false,false,WorldType.FLAT),new Profiler());
        }
        @Override protected void setActivePlayerChunksAndCheckLight() { activeChunkSet.clear(); activeChunkSet.add(new net.minecraft.world.ChunkCoordIntPair(0,0)); }
        void nativeRandomTick() { func_147456_g(); }
    }
    public void randomAndScheduledTicksUseDistinctNativePathsAndCurrentState() throws Exception {
        Registry registry=new Registry();
        BlockRegistryObject registration=registry.registerBlock(new Identifier("mtr:random_tick_probe"),() -> new Block(new Probe()));
        BlockRegistryObject nativeRegistration=registry.registerBlock(new Identifier("mtr:random_native_probe"),() -> new Block(new NativeProbe())); registry.init();
        Probe block=(Probe)registration.get().data;
        assertTrue(block.getTickRandomly(),"A state-sensitive override must opt the native chunk section into random sampling even when its default state is false");
        assertFalse(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.rock)).getTickRandomly());
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            RandomWorld nativeWorld=new RandomWorld(fixture);
            ExtendedBlockStorage section=new ExtendedBlockStorage(64,true);
            nativeWorld.getChunkFromChunkCoords(0,0).getBlockStorageArray()[4]=section;
            for (int x=0;x<16;x++) for (int y=0;y<16;y++) for (int z=0;z<16;z++) {
                section.func_150818_a(x,y,z,block); section.setExtBlockMetadata(x,y,z,0);
            }
            assertTrue(section.getNeedsRandomTick()); nativeWorld.nativeRandomTick();
            assertEquals(3,block.randomTicks,"Native random loop samples three positions per section"); assertEquals(0,block.scheduledTicks);
            assertTrue(block.seen.get(new Property<>(Probe.ACTIVE.data)));
            for (int x=0;x<16;x++) for (int y=0;y<16;y++) for (int z=0;z<16;z++) section.setExtBlockMetadata(x,y,z,1);
            nativeWorld.nativeRandomTick(); assertEquals(3,block.randomTicks,"The predicate must use each live state, not the default or a cached state");
            for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) nativeWorld.getChunkFromChunkCoords(x,z);
            nativeWorld.scheduleBlockUpdate(1,65,1,block,1); nativeWorld.getWorldInfo().incrementTotalWorldTime(1); nativeWorld.tickUpdates(true);
            assertEquals(1,block.scheduledTicks,"Scheduled ticks still run while the random-tick predicate is false"); assertEquals(3,block.randomTicks);
            assertFalse(block.seen.get(new Property<>(Probe.ACTIVE.data)));
            block.randomDisplayTick(nativeWorld,1,65,1,nativeWorld.rand);
            assertEquals(1,block.displayTicks); assertFalse(block.seen.get(new Property<>(Probe.ACTIVE.data)));
            NativeProbe nativeBlock=(NativeProbe)nativeRegistration.get().data;
            for (int x=0;x<16;x++) for (int y=0;y<16;y++) for (int z=0;z<16;z++) section.func_150818_a(x,y,z,nativeBlock);
            nativeWorld.nativeRandomTick(); assertEquals(3,nativeBlock.ticks,"Native and other-mod random callbacks keep updateTick behavior");
        }
    }
    public void originalRubbishBinAndCreativeKeyUseNativeTickAndGlintBridges() throws Exception {
        Registry registry=new Registry();
        BlockRegistryObject registration=registry.registerBlockWithBlockItem(new Identifier("mtr:rubbish_bin_1"),
            () -> new Block(new org.mtr.mod.block.BlockRubbishBin(new BlockSettings(net.minecraft.block.material.Material.iron))));
        registry.init(); org.mtr.mod.block.BlockRubbishBin bin=(org.mtr.mod.block.BlockRubbishBin)registration.get().data;
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            World world=new World(fixture.world); BlockPos pos=new BlockPos(2,65,2);
            world.setBlockState(pos,bin.getDefaultState2().with(new Property<>(org.mtr.mod.block.BlockRubbishBin.FILLED.data),3));
            org.mtr.mapping.tool.RandomBlockTicks.tick(bin,fixture.world,2,65,2,fixture.world.rand);
            assertEquals(2,world.getBlockState(pos).get(new Property<>(org.mtr.mod.block.BlockRubbishBin.FILLED.data)));
            bin.updateTick(fixture.world,2,65,2,fixture.world.rand);
            assertEquals(2,world.getBlockState(pos).get(new Property<>(org.mtr.mod.block.BlockRubbishBin.FILLED.data)),"Original rubbish bin must not empty on the unrelated scheduled path");
        }
        assertTrue(new net.minecraft.item.ItemStack(new org.mtr.mod.item.ItemCreativeDriverKey(new ItemSettings())).hasEffect());
        assertTrue(new net.minecraft.item.ItemStack(new org.mtr.mod.item.ItemBlockEnchanted(new Block(net.minecraft.init.Blocks.stone),new ItemSettings())).hasEffect());
    }
    public void nativeGlintDispatchUsesMappingOverridesAndEnchantmentsForBothItemKinds() {
        ItemExtension forced=new ItemExtension(new ItemSettings()) { public boolean hasGlint2(ItemStack stack) { return true; } };
        net.minecraft.item.ItemStack plainForced=new net.minecraft.item.ItemStack(forced);
        assertTrue(plainForced.hasEffect(),"Native hasEffect must dispatch hasGlint2 for creative keys without enchantment NBT");
        assertTrue(plainForced.hasEffect(1));
        BlockItemExtension enchantedBlock=new BlockItemExtension(new Block(net.minecraft.init.Blocks.stone),new ItemSettings()) {
            public boolean hasGlint2(ItemStack stack) { return true; }
        };
        assertTrue(new net.minecraft.item.ItemStack(enchantedBlock).hasEffect());
        ItemExtension ordinary=new ItemExtension(new ItemSettings());
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(ordinary); assertFalse(stack.hasEffect());
        stack.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1); assertTrue(stack.hasEffect());
        BlockItemExtension ordinaryBlock=new BlockItemExtension(new Block(net.minecraft.init.Blocks.stone),new ItemSettings());
        net.minecraft.item.ItemStack blockStack=new net.minecraft.item.ItemStack(ordinaryBlock); assertFalse(blockStack.hasEffect());
        blockStack.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1); assertTrue(blockStack.hasEffect());
        ItemExtension suppressed=new ItemExtension(new ItemSettings()) { public boolean hasGlint2(ItemStack value) { return false; } };
        net.minecraft.item.ItemStack suppressedStack=new net.minecraft.item.ItemStack(suppressed);
        suppressedStack.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1); assertFalse(suppressedStack.hasEffect(),"An explicit mapping override can suppress an enchanted stack's effect");
    }
}
