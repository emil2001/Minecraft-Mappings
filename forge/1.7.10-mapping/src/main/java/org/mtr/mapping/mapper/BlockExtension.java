package org.mtr.mapping.mapper;

import net.minecraft.tileentity.TileEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.BlockStateTileEntity;

public class BlockExtension extends BlockAbstractMapping {
    public BlockExtension(BlockSettings settings) { super(settings); }
    public static void scheduleBlockTick(World world,BlockPos pos,Block block,int ticks) {
        world.data.scheduleBlockUpdate(pos.getX(),pos.getY(),pos.getZ(),block.data,ticks);
    }
    public static boolean hasScheduledBlockTick(World world,BlockPos pos,Block block) {
        if (!(world.data instanceof net.minecraft.world.WorldServer)) return false;
        java.util.Set<?> pending=cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.world.WorldServer.class,
            (net.minecraft.world.WorldServer)world.data,"pendingTickListEntriesHashSet","field_73064_N");
        return pending.contains(new net.minecraft.world.NextTickListEntry(pos.getX(),pos.getY(),pos.getZ(),block.data));
    }
    public static void scheduleFluidTick(World world,BlockPos pos,Fluid fluid,int ticks) {
        org.mtr.mapping.tool.Waterlogging.schedule(world,pos,fluid,ticks);
    }
    public static boolean hasScheduledFluidTick(World world,BlockPos pos,Fluid fluid) {
        return fluid.data==net.minecraftforge.fluids.FluidRegistry.WATER && org.mtr.mapping.tool.Waterlogging.isScheduled(world.data,pos);
    }
    public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBlockHarvested(world.data, pos.getX(), pos.getY(), pos.getZ(), world.data.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ()),
            player == null ? null : player.data);
    }
    @Override public final void onBlockHarvested(net.minecraft.world.World world, int x, int y, int z, int metadata,
            net.minecraft.entity.player.EntityPlayer player) {
        BlockPos pos = new BlockPos(x, y, z);
        World mapped = new World(world);
        onBreak2(mapped, pos, mapped.getBlockState(pos), player == null ? null : new PlayerEntity(player));
    }
    public ItemStack getPickStack2(BlockView world, BlockPos pos, BlockState state) { return asItem2().getDefaultStack(); }
    @Override public final net.minecraft.item.ItemStack getPickBlock(net.minecraft.util.MovingObjectPosition target,
            net.minecraft.world.World world, int x, int y, int z, net.minecraft.entity.player.EntityPlayer player) {
        return getPickBlock(target, world, x, y, z);
    }
    @Override public final net.minecraft.item.ItemStack getPickBlock(net.minecraft.util.MovingObjectPosition target,
            net.minecraft.world.World world, int x, int y, int z) {
        BlockView view = new BlockView(world);
        BlockPos pos = new BlockPos(x, y, z);
        return getPickStack2(view, pos, view.getBlockState(pos)).data;
    }
    @Override public boolean hasTileEntity(int metadata) { return this instanceof BlockWithEntity || getStateDefinition().requiresTileEntity(); }
    @Override public TileEntity createTileEntity(net.minecraft.world.World world, int metadata) {
        if (!hasTileEntity(metadata)) return null;
        BlockStateTileEntity tile = this instanceof BlockWithEntity
            ? ((BlockWithEntity) this).createBlockEntity(new BlockPos(0, 0, 0), getDefaultState2()) : new BlockStateTileEntity();
        tile.setMtrState(getDefaultState2());
        return tile;
    }
    @Override public void onBlockAdded(net.minecraft.world.World world, int x, int y, int z) {
        // Chunk invokes this before creating its TileEntity and notifying neighbors.
        BlockState state = World.getPlacementState(this, world, new BlockPos(x, y, z));
        if (state != null && getStateDefinition().requiresTileEntity()) {
            ((BlockStateTileEntity) world.getTileEntity(x, y, z)).setMtrState(state);
        }
        super.onBlockAdded(world, x, y, z);
    }
}
