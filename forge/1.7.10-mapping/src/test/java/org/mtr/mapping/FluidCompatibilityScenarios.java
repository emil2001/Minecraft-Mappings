package org.mtr.mapping;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.*;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.PlaceableOnWaterItemExtension;
import org.mtr.mapping.registry.*;
import static org.junit.jupiter.api.Assertions.*;

public class FluidCompatibilityScenarios {
    public void nativeFluidsRetainTheirIdentityAndLevels() {
        BlockPlacementScenarios.MemoryWorld world=new BlockPlacementScenarios.MemoryWorld();
        BlockPos pos=new BlockPos(4,64,4);
        assertTrue(FluidState.fromWorld(world,pos).isEmpty());
        assertEquals(0,FluidState.fromWorld(world,pos).getLevel());
        assertSame(Fluids.getEmptyMapped().data,FluidState.fromWorld(world,pos).getFluid().data);
        for (net.minecraft.block.Block block:new net.minecraft.block.Block[]{net.minecraft.init.Blocks.water,net.minecraft.init.Blocks.flowing_water,
                net.minecraft.init.Blocks.lava,net.minecraft.init.Blocks.flowing_lava}) {
            for (int meta:new int[]{0,1,7,8,15}) {
                world.setBlock(4,64,4,block,meta,0);
                FluidState state=FluidState.fromWorld(world,pos);
                assertFalse(state.isEmpty());
                assertSame(block.getMaterial()==Material.water?FluidRegistry.WATER:FluidRegistry.LAVA,state.getFluid().data);
                assertEquals(meta==0,state.isStill());
                assertEquals(meta>=8?8:8-meta,state.getLevel());
                assertEquals(meta>=8,state.isFalling());
                assertEquals(new BlockView(world).getBlockState(pos),state.getBlockState());
            }
        }
        FluidState source=Fluids.getWaterMapped().getStill(false);
        assertSame(FluidRegistry.WATER,source.getFluid().data);
        assertTrue(source.isStill()); assertEquals(8,source.getLevel());
        assertSame(net.minecraft.init.Blocks.water,source.getBlockState().getBlock().data);
        net.minecraftforge.fluids.Fluid oil=new net.minecraftforge.fluids.Fluid("mtr_fluid_probe_oil");
        assertTrue(FluidRegistry.registerFluid(oil));
        BlockFluidClassic oilBlock=new BlockFluidClassic(oil,Material.water);
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(oilBlock,"fluid_probe_oil");
        world.setBlock(4,64,4,oilBlock,0,0);
        assertSame(oil,FluidState.fromWorld(world,pos).getFluid().data,"Forge fluid identity must not be guessed from Material.water");
        assertTrue(FluidState.fromWorld(world,pos).isStill());
        world.setBlock(4,64,4,oilBlock,4,0);
        assertEquals(4,FluidState.fromWorld(world,pos).getLevel());
        assertFalse(FluidState.fromWorld(world,pos).isStill());
        net.minecraftforge.fluids.Fluid finite=new net.minecraftforge.fluids.Fluid("mtr_fluid_probe_finite");
        assertTrue(FluidRegistry.registerFluid(finite));
        BlockFluidFinite finiteBlock=new BlockFluidFinite(finite,Material.water);
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(finiteBlock,"fluid_probe_finite");
        world.setBlock(4,64,4,finiteBlock,1,0);
        assertEquals(2,FluidState.fromWorld(world,pos).getLevel(),"Finite fluid metadata counts upwards, unlike vanilla decay");
        assertFalse(FluidState.fromWorld(world,pos).isStill(),"Finite volume is not an infinite source");
    }

    public void waterPlacementUsesNativeRayTraceAndPreservesHeldStack() {
        Fixture fixture=new Fixture("float_success",false);
        assertSame(fixture.block,Block.getBlockFromItem(new Item(fixture.item)).data,"Rail-related item detection needs the native ItemBlock relationship");
        assertSame(fixture.item,net.minecraft.item.Item.getItemFromBlock(fixture.block),"Native loot must resolve the floating block item");
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(fixture.item,3);
        assertFalse(fixture.item.onItemUse(stack,fixture.player,fixture.world,4,64,4,1,.5F,1,.5F),"Only liquid ray tracing performs this item's placement");
        stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound()); stack.getTagCompound().setString("keep","original");
        assertSame(stack,fixture.item.onItemRightClick(stack,fixture.world,fixture.player));
        assertSame(fixture.block,fixture.world.getBlock(4,65,4));
        FluidState remainingWater=FluidState.fromWorld(fixture.world,new BlockPos(4,64,4));
        assertSame(Fluids.getWaterMapped().data,remainingWater.getFluid().data);
        assertTrue(remainingWater.isStill());
        assertEquals(0,fixture.world.getBlockMetadata(4,64,4));
        assertEquals(2,stack.stackSize); assertEquals("original",stack.getTagCompound().getString("keep"));
        fixture.world.setBlockToAir(4,65,4); fixture.player.capabilities.isCreativeMode=true;
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player);
        assertSame(fixture.block,fixture.world.getBlock(4,65,4)); assertEquals(2,stack.stackSize);
    }

    public void waterPlacementHonorsGuardsAndForgeCancellation() {
        Fixture fixture=new Fixture("float_guards",true);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(fixture.item,2);
        fixture.world.mineAllowed=false;
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player); assertUnplaced(fixture,stack);
        fixture.world.mineAllowed=true; fixture.editAllowed[0]=false;
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player); assertUnplaced(fixture,stack);
        fixture.editAllowed[0]=true;
        fixture.world.setBlock(4,64,4,net.minecraft.init.Blocks.flowing_water,1,0);
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player); assertUnplaced(fixture,stack);
        fixture.world.setBlock(4,64,4,net.minecraft.init.Blocks.lava,0,0);
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player); assertUnplaced(fixture,stack);
        fixture.world.setBlock(4,64,4,net.minecraft.init.Blocks.water,0,0);
        fixture.world.setBlock(4,65,4,net.minecraft.init.Blocks.stone);
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player);
        assertSame(net.minecraft.init.Blocks.stone,fixture.world.getBlock(4,65,4)); assertEquals(2,stack.stackSize);
        fixture.world.setBlockToAir(4,65,4);
        net.minecraft.entity.item.EntityItem collider=new net.minecraft.entity.item.EntityItem(fixture.world,4.5,65.1,4.5,new net.minecraft.item.ItemStack(net.minecraft.init.Items.apple));
        collider.preventEntitySpawning=true; fixture.world.spawnEntityInWorld(collider);
        fixture.item.onItemRightClick(stack,fixture.world,fixture.player); assertUnplaced(fixture,stack); collider.setDead();
        CancelPlacement cancel=new CancelPlacement(); MinecraftForge.EVENT_BUS.register(cancel);
        try { fixture.item.onItemRightClick(stack,fixture.world,fixture.player); assertUnplaced(fixture,stack); assertEquals(1,cancel.calls); }
        finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        stack.stackSize=0; fixture.item.onItemRightClick(stack,fixture.world,fixture.player);
        assertTrue(fixture.world.isAirBlock(4,65,4)); assertEquals(0,stack.stackSize);
    }
    private static void assertUnplaced(Fixture fixture,net.minecraft.item.ItemStack stack) {
        assertTrue(fixture.world.isAirBlock(4,65,4)); assertEquals(2,stack.stackSize);
    }
    public static class CancelPlacement {
        int calls;
        @cpw.mods.fml.common.eventhandler.SubscribeEvent public void onPlace(BlockEvent.PlaceEvent event) { calls++; event.setCanceled(true); }
    }
    private static class WaterWorld extends BlockPlacementScenarios.MemoryWorld {
        boolean mineAllowed=true;
        @Override public boolean canMineBlock(EntityPlayer player,int x,int y,int z) { return mineAllowed; }
    }
    private static class Fixture {
        final WaterWorld world=new WaterWorld();
        final boolean[] editAllowed={true};
        final BlockExtension block;
        final PlaceableOnWaterItemExtension item;
        final EntityPlayer player;
        Fixture(String name,boolean solid) {
            block=new BlockExtension(new BlockSettings(Material.iron)) {
                @Override public VoxelShape getCollisionShape2(BlockState state,BlockView view,BlockPos pos,ShapeContext context) {
                    return solid?VoxelShapes.fullCube():VoxelShapes.empty();
                }
            };
            item=new PlaceableOnWaterItemExtension(new Block(block),new ItemSettings());
            Registry registry=new Registry(); registry.registerBlock(new Identifier("mtr:"+name),() -> new Block(block));
            registry.registerItem(new Identifier("mtr:"+name),settings -> new Item(item)); registry.init();
            player=new EntityPlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FluidProbe")) {
                public void addChatMessage(net.minecraft.util.IChatComponent text) {}
                public boolean canCommandSenderUseCommand(int level,String command) { return true; }
                public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return new net.minecraft.util.ChunkCoordinates(4,66,4); }
                @Override public boolean canPlayerEdit(int x,int y,int z,int side,net.minecraft.item.ItemStack stack) { return y!=65 || editAllowed[0]; }
            };
            player.setPosition(4.5,66,4.5); player.rotationPitch=90; player.prevRotationPitch=90;
            world.setBlock(4,64,4,net.minecraft.init.Blocks.water,0,0);
        }
    }
}
