package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.SlabBlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.registry.*;
import static org.junit.jupiter.api.Assertions.*;

public class SlabCompatibilityScenarios {
    public static class ExtendedSlab extends SlabBlockExtension {
        static final IntegerProperty SIDE=IntegerProperty.of("side",0,4);
        ExtendedSlab() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) {
            super.addBlockProperties(properties); properties.add(DirectionHelper.FACING); properties.add(SIDE);
        }
    }
    public static class RawStateBlock extends BlockExtension {
        static final BooleanProperty ALPHA=BooleanProperty.of("alpha");
        static final IntegerProperty KIND=IntegerProperty.of("kind",0,5),PHASE=IntegerProperty.of("phase",0,15);
        RawStateBlock() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) {
            properties.add(PHASE); properties.add(ALPHA); properties.add(KIND);
        }
    }
    public static class LargeRawStateBlock extends BlockExtension {
        static final IntegerProperty[] VALUES={IntegerProperty.of("a",0,15),IntegerProperty.of("b",0,15),IntegerProperty.of("c",0,15),
            IntegerProperty.of("d",0,15),IntegerProperty.of("e",0,15),IntegerProperty.of("f",0,15)};
        LargeRawStateBlock() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) {
            java.util.Collections.addAll(properties,VALUES);
        }
    }
    public void slabsExposeBottomGeometryInNativeCollision() throws Exception {
        BlockExtension slab=slab();
        Registry registry=new Registry();
        BlockRegistryObject registered=registry.registerBlockWithBlockItem(new Identifier("mtr:slab_geometry_probe"),() -> new Block(slab));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        new World(nativeWorld).setBlockState(new BlockPos(4,64,4),registered.get().getDefaultState());
        net.minecraft.util.AxisAlignedBB bounds=slab.getCollisionBoundingBoxFromPool(nativeWorld,4,64,4);
        assertEquals(64,bounds.minY);
        assertEquals(64.5,bounds.maxY);
        assertFalse(slab.isOpaqueCube());
        assertFalse(slab.isNormalCube(nativeWorld,4,64,4));
    }

    public void slabsPlaceTopMergeBothHalvesAndKeepOriginalDoubleLoot() throws Exception {
        BlockExtension slab=slab();
        Registry registry=new Registry();
        BlockRegistryObject registered=registry.registerBlockWithBlockItem(new Identifier("mtr:metal_slab"),() -> new Block(slab));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.player.EntityPlayer player=MtrEscalatorScenarios.player(nativeWorld);
        net.minecraft.item.Item item=net.minecraft.item.Item.getItemFromBlock(registered.get().data);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(item,8);
        World world=new World(nativeWorld);
        nativeWorld.setBlock(4,63,4,net.minecraft.init.Blocks.stone);
        assertTrue(item.onItemUse(stack,player,nativeWorld,4,63,4,1,.5F,1,.5F));
        assertEquals("bottom",world.getBlockState(new BlockPos(4,64,4)).data.getSerializedProperties().get("type"));
        assertEquals(7,stack.stackSize);
        assertTrue(item.onItemUse(stack,player,nativeWorld,4,64,4,1,.5F,.5F,.5F));
        assertEquals("double",world.getBlockState(new BlockPos(4,64,4)).data.getSerializedProperties().get("type"));
        assertEquals("false",world.getBlockState(new BlockPos(4,64,4)).data.getSerializedProperties().get("waterlogged"));
        assertEquals(6,stack.stackSize);
        assertEquals(65,slab.getCollisionBoundingBoxFromPool(nativeWorld,4,64,4).maxY);
        assertTrue(slab.isNormalCube(nativeWorld,4,64,4));
        assertTrue(slab.isSideSolid(nativeWorld,4,64,4,net.minecraftforge.common.util.ForgeDirection.NORTH));
        assertEquals(2,slab.getDrops(nativeWorld,4,64,4,nativeWorld.getBlockMetadata(4,64,4),0).get(0).stackSize);
        nativeWorld.setBlock(7,65,4,net.minecraft.init.Blocks.stone);
        assertTrue(item.onItemUse(stack,player,nativeWorld,7,65,4,0,.5F,0,.5F));
        assertEquals("top",world.getBlockState(new BlockPos(7,64,4)).data.getSerializedProperties().get("type"));
        assertEquals(64.5,slab.getCollisionBoundingBoxFromPool(nativeWorld,7,64,4).minY);
        assertTrue(slab.isSideSolid(nativeWorld,7,64,4,net.minecraftforge.common.util.ForgeDirection.UP));
        assertFalse(slab.isSideSolid(nativeWorld,7,64,4,net.minecraftforge.common.util.ForgeDirection.DOWN));
        assertTrue(item.onItemUse(stack,player,nativeWorld,7,64,4,0,.5F,.5F,.5F));
        assertEquals("double",world.getBlockState(new BlockPos(7,64,4)).data.getSerializedProperties().get("type"));
        assertEquals(4,stack.stackSize);
        nativeWorld.setBlock(10,63,4,net.minecraft.init.Blocks.stone);
        item.onItemUse(stack,player,nativeWorld,10,63,4,1,.5F,1,.5F);
        nativeWorld.setBlock(9,64,4,net.minecraft.init.Blocks.stone);
        assertTrue(item.onItemUse(stack,player,nativeWorld,9,64,4,5,1,.75F,.5F));
        assertEquals("double",world.getBlockState(new BlockPos(10,64,4)).data.getSerializedProperties().get("type"),"Adjacent half also merges");
        assertEquals(2,stack.stackSize);
    }

    public void slabMergeRejectsBlockedSpaceEmptyStacksPermissionsAndWorldCeiling() throws Exception {
        BlockExtension slab=slab();
        Registry registry=new Registry();
        BlockRegistryObject registered=registry.registerBlockWithBlockItem(new Identifier("mtr:slab_guards_probe"),() -> new Block(slab),
            (block,settings) -> new org.mtr.mapping.mapper.BlockItemExtension(block,settings) {
                @Override public boolean canItemEditBlocks() { return false; }
            });
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.player.EntityPlayer player=MtrEscalatorScenarios.player(nativeWorld);
        net.minecraft.item.Item item=net.minecraft.item.Item.getItemFromBlock(registered.get().data);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(item,3);
        World world=new World(nativeWorld);
        BlockPos pos=new BlockPos(4,64,4);
        world.setBlockState(pos,registered.get().getDefaultState());
        player.capabilities.allowEdit=false;
        assertFalse(item.onItemUse(stack,player,nativeWorld,4,64,4,1,.5F,.5F,.5F));
        player.capabilities.allowEdit=true;
        net.minecraft.entity.item.EntityItem collider=new net.minecraft.entity.item.EntityItem(nativeWorld,4.5,64.75,4.5,new net.minecraft.item.ItemStack(net.minecraft.init.Items.apple));
        collider.preventEntitySpawning=true;
        assertTrue(nativeWorld.spawnEntityInWorld(collider));
        assertFalse(item.onItemUse(stack,player,nativeWorld,4,64,4,1,.5F,.5F,.5F));
        assertEquals("bottom",world.getBlockState(pos).data.getSerializedProperties().get("type"));
        assertEquals(3,stack.stackSize);
        collider.setDead();
        player.capabilities.isCreativeMode=true;
        assertTrue(item.onItemUse(stack,player,nativeWorld,4,64,4,1,.5F,.5F,.5F));
        assertEquals(3,stack.stackSize,"Creative merge preserves held items even before the interaction manager restores its snapshot");
        BlockPos ceiling=new BlockPos(4,255,4);
        world.setBlockState(ceiling,registered.get().getDefaultState());
        assertTrue(item.onItemUse(stack,player,nativeWorld,4,255,4,1,.5F,.5F,.5F));
        assertEquals("double",world.getBlockState(ceiling).data.getSerializedProperties().get("type"),"The highest valid block can still merge, like native ItemSlab");
        item.onItemUse(stack,player,nativeWorld,4,255,4,1,.5F,1,.5F);
        assertSame(net.minecraft.init.Blocks.air,nativeWorld.getBlock(4,256,4));
        assertEquals(3,stack.stackSize);
        stack.stackSize=0;
        world.setBlockState(pos,registered.get().getDefaultState());
        assertFalse(item.onItemUse(stack,player,nativeWorld,4,64,4,1,.5F,.5F,.5F));
        assertEquals("bottom",world.getBlockState(pos).data.getSerializedProperties().get("type"));
    }

    public void extendedSlabMergeKeepsUnrelatedPropertiesAndTileIdentity() {
        ExtendedSlab slab=new ExtendedSlab();
        Registry registry=new Registry();
        BlockRegistryObject registered=registry.registerBlockWithBlockItem(new Identifier("mtr:extended_slab_probe"),() -> new Block(slab));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        World world=new World(nativeWorld);
        BlockPos pos=new BlockPos(4,64,4);
        BlockState single=registered.get().getDefaultState().with(new Property<>(DirectionHelper.FACING.data),Direction.WEST.data)
            .with(new Property<>(ExtendedSlab.SIDE.data),4);
        world.setBlockState(pos,single);
        net.minecraft.tileentity.TileEntity original=nativeWorld.getTileEntity(4,64,4);
        assertNotNull(original);
        net.minecraft.item.Item item=net.minecraft.item.Item.getItemFromBlock(slab);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(item,2);
        assertTrue(item.onItemUse(stack,MtrEscalatorScenarios.player(nativeWorld),nativeWorld,4,64,4,1,.5F,.5F,.5F));
        BlockState merged=world.getBlockState(pos);
        assertEquals("double",merged.data.getSerializedProperties().get("type"));
        assertEquals(Direction.WEST.data,merged.get(new Property<>(DirectionHelper.FACING.data)));
        assertEquals(4,merged.get(new Property<>(ExtendedSlab.SIDE.data)));
        assertSame(original,nativeWorld.getTileEntity(4,64,4));
        net.minecraft.nbt.NBTTagCompound saved=new net.minecraft.nbt.NBTTagCompound();
        original.writeToNBT(saved);
        org.mtr.mapping.tool.BlockStateTileEntity restored=(org.mtr.mapping.tool.BlockStateTileEntity)net.minecraft.tileentity.TileEntity.createAndLoadEntity(saved);
        assertEquals(merged,restored.readMtrState(slab.getStateDefinition(),nativeWorld.getBlockMetadata(4,64,4)));
        assertEquals(1,stack.stackSize);
    }

    public void slabReplacementOnlyFillsMissingHalf() throws Exception {
        BlockExtension slab=slab();
        Registry registry=new Registry();
        BlockRegistryObject registered=registry.registerBlockWithBlockItem(new Identifier("mtr:slab_replace_probe"),() -> new Block(slab));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.player.EntityPlayer player=MtrEscalatorScenarios.player(nativeWorld);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(net.minecraft.item.Item.getItemFromBlock(slab));
        BlockState bottom=registered.get().getDefaultState();
        BlockState top=bottom.with(new Property<>(SlabBlockExtension.TYPE),SlabType.TOP.data);
        BlockState full=bottom.with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE.data);
        BlockPos pos=new BlockPos(4,64,4);
        ItemPlacementContext up=new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,stack,4,64,4,1,.5F,.5F,.5F),pos);
        ItemPlacementContext down=new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,stack,4,64,4,0,.5F,.5F,.5F),pos);
        assertTrue(bottom.canReplace(up)); assertFalse(top.canReplace(up));
        assertTrue(top.canReplace(down)); assertFalse(bottom.canReplace(down));
        assertFalse(full.canReplace(up));
        assertTrue(bottom.canReplace(new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,stack,4,64,4,2,.5F,.75F,0),pos)));
        assertFalse(bottom.canReplace(new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,stack,4,64,4,2,.5F,.5F,0),pos)));
        assertTrue(top.canReplace(new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,stack,4,64,4,2,.5F,.5F,0),pos)));
        assertTrue(top.canReplace(new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,stack,4,63,4,1,.5F,1,.5F),pos)));
        assertFalse(bottom.canReplace(new ItemPlacementContext(new ItemUsageContext(nativeWorld,player,new net.minecraft.item.ItemStack(net.minecraft.init.Items.apple),4,64,4,1,.5F,.5F,.5F),pos)));
        stack.stackSize=0;
        assertFalse(bottom.canReplace(up));
    }

    public void rawBlockIdsRoundTripAll192ExtendedStatesWithoutDependingOnVisitOrder() {
        Registry registry=new Registry();
        RawStateBlock probe=new RawStateBlock();
        registry.registerBlock(new Identifier("mtr:raw_extended_states_probe"),() -> new Block(probe)); registry.init();
        java.util.Set<Integer> ids=new java.util.HashSet<>();
        for (boolean alpha:new boolean[]{false,true}) for (int kind=5;kind>=0;kind--) for (int phase=15;phase>=0;phase--) {
            BlockState state=probe.getDefaultState2().with(new Property<>(RawStateBlock.ALPHA.data),alpha)
                .with(new Property<>(RawStateBlock.KIND.data),kind).with(new Property<>(RawStateBlock.PHASE.data),phase);
            int raw=Block.getRawIdFromState(state);
            assertEquals((alpha?0:1)+2*kind+12*phase,state.data.getRawStateIndex(),"The property domains retain a stable state index");
            assertTrue(ids.add(raw));
            assertEquals(state,Block.getStateFromRawId(raw));
        }
        assertEquals(192,ids.size());
        assertThrows(IllegalArgumentException.class,() -> probe.getStateDefinition().readRawStateIndex(192));
        assertThrows(IllegalArgumentException.class,() -> org.mtr.mapping.tool.BlockStateDefinition.forBlock(net.minecraft.init.Blocks.stone).readRawStateIndex(65536));
        assertEquals(Blocks.getAirMapped().getDefaultState(),Block.getStateFromRawId(-1));
        LargeRawStateBlock large=new LargeRawStateBlock();
        Registry largeRegistry=new Registry(); largeRegistry.registerBlock(new Identifier("mtr:raw_twenty_bits_probe"),() -> new Block(large)); largeRegistry.init();
        BlockState largest=large.getDefaultState2();
        for (int i=0;i<5;i++) largest=largest.with(new Property<>(LargeRawStateBlock.VALUES[i].data),15);
        assertEquals(0xFFFFF,largest.data.getRawStateIndex());
        assertEquals(largest,Block.getStateFromRawId(Block.getRawIdFromState(largest)),"Opaque IDs retain the full supported mapped state");
        BlockState overflowing=large.getDefaultState2().with(new Property<>(LargeRawStateBlock.VALUES[5].data),1);
        assertThrows(IllegalArgumentException.class,() -> Block.getRawIdFromState(overflowing));
    }

    public void nativeSlabTypesKeepVariantBitsAndSwitchOnlyThePairedBlockId() {
        for (net.minecraft.block.Block[] pair:new net.minecraft.block.Block[][]{{net.minecraft.init.Blocks.stone_slab,net.minecraft.init.Blocks.double_stone_slab},
                {net.minecraft.init.Blocks.wooden_slab,net.minecraft.init.Blocks.double_wooden_slab}}) {
            for (int variant=0;variant<8;variant++) {
                BlockState bottom=new BlockState(org.mtr.mapping.tool.BlockStateDefinition.forBlock(pair[0]).read(variant,new net.minecraft.nbt.NBTTagCompound()));
                assertTrue(SlabBlock.isInstance(bottom.getBlock()));
                assertEquals(SlabType.BOTTOM,SlabBlockExtension.getType(bottom));
                BlockState top=bottom.with(new Property<>(SlabBlockExtension.TYPE),SlabType.TOP.data);
                assertSame(pair[0],top.getBlock().data);
                assertEquals(variant|8,top.data.write(new net.minecraft.nbt.NBTTagCompound()));
                assertEquals(SlabType.TOP,SlabBlockExtension.getType(top));
                BlockState doubled=top.with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE.data);
                assertSame(pair[1],doubled.getBlock().data);
                assertEquals(variant,doubled.data.write(new net.minecraft.nbt.NBTTagCompound()));
                assertEquals(SlabType.DOUBLE,SlabBlockExtension.getType(doubled));
                assertEquals(bottom,doubled.with(new Property<>(SlabBlockExtension.TYPE),SlabType.BOTTOM.data));
                assertEquals(top,Block.getStateFromRawId(Block.getRawIdFromState(top)));
                assertEquals(doubled,Block.getStateFromRawId(Block.getRawIdFromState(doubled)));
                assertNotEquals(bottom,bottom.with(new Property<>(SlabBlockExtension.TYPE),SlabType.TOP.data));
            }
            assertEquals(SlabType.DOUBLE,SlabBlockExtension.getType(new Block(pair[1]).getDefaultState()));
            BlockState seamless=new BlockState(org.mtr.mapping.tool.BlockStateDefinition.forBlock(pair[1]).read(15,new net.minecraft.nbt.NBTTagCompound()));
            assertEquals(seamless,Block.getStateFromRawId(Block.getRawIdFromState(seamless)),"An untouched native double slab retains its variant metadata");
        }
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        nativeWorld.setBlock(4,64,4,net.minecraft.init.Blocks.stone_slab,10,3);
        BlockState inWorld=new BlockView(nativeWorld).getBlockState(new BlockPos(4,64,4));
        assertEquals(SlabType.TOP,SlabBlockExtension.getType(inWorld));
        assertEquals(10,inWorld.data.write(new net.minecraft.nbt.NBTTagCompound()));
        net.minecraft.block.Block unknown=new net.minecraft.block.BlockSlab(false,net.minecraft.block.material.Material.iron) {
            @Override public String func_150002_b(int metadata) { return "unmapped_mod_slab"; }
        };
        assertFalse(SlabBlock.isInstance(new Block(unknown)),"Unknown mod slab pairs require an explicit adapter");
        assertFalse(new Block(unknown).getDefaultState().contains(new Property<>(SlabBlockExtension.TYPE)));
        assertSame(net.minecraft.init.Blocks.stone_slab,Block.getBlockFromItem(new Item(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone_slab))).data);
        assertSame(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone_slab),new Block(net.minecraft.init.Blocks.stone_slab).asItem().data);
        assertSame(net.minecraft.init.Blocks.stone_slab.getMapColor(0),new Block(net.minecraft.init.Blocks.stone_slab).getDefaultMapColor().data);
        assertSame(net.minecraft.init.Blocks.air,Block.getBlockFromItem(new Item(net.minecraft.init.Items.apple)).data);
        assertSame(net.minecraft.init.Blocks.air,Block.getBlockFromItem(new Item(null)).data);
    }

    public void nativePlacementPreflightChecksTheProposedHalfBeforeItExists() throws Exception {
        BlockExtension slab=slab();
        Registry registry=new Registry();
        registry.registerBlockWithBlockItem(new Identifier("mtr:slab_preflight_probe"),() -> new Block(slab)); registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.player.EntityPlayer player=MtrEscalatorScenarios.player(nativeWorld);
        net.minecraft.item.Item item=net.minecraft.item.Item.getItemFromBlock(slab);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(item,3);
        nativeWorld.setBlock(4,65,4,net.minecraft.init.Blocks.stone);
        collider(nativeWorld,4.5,64.1,4.5);
        assertTrue(item.onItemUse(stack,player,nativeWorld,4,65,4,0,.5F,0,.5F));
        assertSame(slab,nativeWorld.getBlock(4,64,4));
        assertEquals(64.5,slab.getCollisionBoundingBoxFromPool(nativeWorld,4,64,4).minY);
        assertEquals(2,stack.stackSize);
        nativeWorld.setBlock(7,63,4,net.minecraft.init.Blocks.stone);
        collider(nativeWorld,7.5,64.1,4.5);
        assertFalse(item.onItemUse(stack,player,nativeWorld,7,63,4,1,.5F,1,.5F));
        assertSame(net.minecraft.init.Blocks.air,nativeWorld.getBlock(7,64,4));
        assertEquals(2,stack.stackSize);
        nativeWorld.setBlock(10,65,4,net.minecraft.init.Blocks.stone);
        collider(nativeWorld,10.5,64.75,4.5);
        assertFalse(item.onItemUse(stack,player,nativeWorld,10,65,4,0,.5F,0,.5F));
        assertSame(net.minecraft.init.Blocks.air,nativeWorld.getBlock(10,64,4));
        assertEquals(2,stack.stackSize);
    }

    private static void collider(net.minecraft.world.World world,double x,double y,double z) {
        net.minecraft.entity.item.EntityItem entity=new net.minecraft.entity.item.EntityItem(world,x,y,z,new net.minecraft.item.ItemStack(net.minecraft.init.Items.apple));
        entity.preventEntitySpawning=true;
        assertTrue(world.spawnEntityInWorld(entity));
    }

    private static BlockExtension slab() { return new org.mtr.mod.block.BlockStationColorSlab(); }
}
