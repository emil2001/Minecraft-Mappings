package org.mtr.mapping.holder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.BlockSnapshot;
import org.mtr.mapping.tool.BlockStateTileEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;


public final class World extends NativeHolder<net.minecraft.world.World> {
    private static final ThreadLocal<Placement> PLACEMENT = new ThreadLocal<>();
    public World(net.minecraft.world.World data) { super(data); }

    public BlockState getBlockState(BlockPos pos) {
        return new BlockView(data).getBlockState(pos);
    }
    public BlockEntity getBlockEntity(BlockPos pos) { return new BlockView(data).getBlockEntity(pos); }
    public FluidState getFluidState(BlockPos pos) { return new BlockView(data).getFluidState(pos); }
    public int getTopY(HeightMapType type,int x,int z) {
        java.util.Objects.requireNonNull(type);
        for (int y=data.getChunkFromBlockCoords(x,z).getTopFilledSegment()+15;y>=0;y--) {
            net.minecraft.block.material.Material material=data.getBlock(x,y,z).getMaterial();
            if (material.blocksMovement() || material.isLiquid()) return y+1;
        }
        return 0;
    }

    public boolean setBlockState(BlockPos pos, BlockState state) { return setBlockState(pos, state, 3); }
    public boolean setBlockState(BlockPos pos, BlockState state, int flags) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (y < 0 || y >= 256 || x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) return false;
        BlockState previous = getBlockState(pos);
        if (previous.equals(state)) return false;
        int metadata = state.data.write(new NBTTagCompound());
        if (previous.getBlock().equals(state.getBlock())) {
            BlockStateTileEntity tile = state.data.definition.requiresTileEntity()
                ? (BlockStateTileEntity) data.getTileEntity(x, y, z) : null;
            BlockSnapshot snapshot = null;
            if (data.captureBlockSnapshots && !data.isRemote) {
                snapshot = BlockSnapshot.getBlockSnapshot(data, x, y, z, flags);
                data.capturedBlockSnapshots.add(snapshot);
            }
            if (tile != null) tile.setMtrState(state);
            if (!data.setBlockMetadataWithNotify(x, y, z, metadata, 0) && tile == null) {
                if (snapshot != null) data.capturedBlockSnapshots.remove(snapshot);
                return false;
            }
            if (tile != null) tile.markDirty();
            data.func_147451_t(x, y, z);
            if (!data.captureBlockSnapshots) data.markAndNotifyBlock(x, y, z, null, previous.getBlock().data, state.getBlock().data, flags);
            org.mtr.mapping.tool.Waterlogging.changed(data,x,y,z);
            return true;
        }
        if (data.isRemote && state.data.definition.requiresTileEntity()) {
            if (!data.setBlock(x, y, z, state.getBlock().data, metadata, 0)) return false;
            ((BlockStateTileEntity) data.getTileEntity(x, y, z)).setMtrState(state);
            data.func_147451_t(x, y, z);
            data.markAndNotifyBlock(x, y, z, null, previous.getBlock().data, state.getBlock().data, flags);
            org.mtr.mapping.tool.Waterlogging.changed(data,x,y,z);
            return true;
        }
        Placement previousPlacement = PLACEMENT.get();
        PLACEMENT.set(new Placement(data, pos, state));
        try {
            return data.setBlock(x, y, z, state.getBlock().data, metadata, flags);
        } finally {
            if (previousPlacement == null) PLACEMENT.remove();
            else PLACEMENT.set(previousPlacement);
        }
    }
    public boolean isClient() { return data.isRemote; }
    public MinecraftServer getServer() {
        return !data.isRemote && data instanceof net.minecraft.world.WorldServer
            ? new MinecraftServer(((net.minecraft.world.WorldServer)data).func_73046_m()) : null;
    }
    public boolean getIsClientMapped() { return data.isRemote; }
    public boolean isEmittingRedstonePower(BlockPos pos,Direction direction) {
        return data.getIndirectPowerLevelTo(pos.getX(),pos.getY(),pos.getZ(),direction.ordinal())>0;
    }
    public void updateNeighbors(BlockPos pos,Block source) {
        data.notifyBlocksOfNeighborChange(pos.getX(),pos.getY(),pos.getZ(),source.data);
    }
    public void playSound(PlayerEntity excluded, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        String key = org.mtr.mapping.tool.LegacySoundKeys.key(sound,category).toString();
        double x=pos.getX()+.5, y=pos.getY()+.5, z=pos.getZ()+.5;
        if (excluded == null) data.playSoundEffect(x,y,z,key,volume,pitch);
        else {
            java.util.List<?> listeners = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.world.World.class,data,"worldAccesses","field_73021_x");
            for (Object listener : listeners) ((net.minecraft.world.IWorldAccess)listener).playSoundToNearExcept(excluded.data,key,x,y,z,volume,pitch);
        }
    }
    public static World cast(HolderBase<?> holder) { return new World((net.minecraft.world.World) holder.data); }
    public ChunkManager getChunkManager() { return new ChunkManager(data.getChunkProvider()); }
    public Scoreboard getScoreboard() { return new Scoreboard(data.getScoreboard()); }
    public int getLightLevel(LightType type, BlockPos pos) {
        if (type.data==net.minecraft.world.EnumSkyBlock.Sky && data.provider.hasNoSky) return 0;
        return data.getSavedLightValue(type.data, pos.getX(), pos.getY(), pos.getZ());
    }
    public boolean isRegionLoaded(BlockPos from, BlockPos to) {
        if (Math.max(from.getY(), to.getY()) < 0 || Math.min(from.getY(), to.getY()) >= 256) return false;
        ChunkManager chunks = getChunkManager();
        for (int x = Math.min(from.getX(), to.getX()) >> 4; x <= Math.max(from.getX(), to.getX()) >> 4; x++) {
            for (int z = Math.min(from.getZ(), to.getZ()) >> 4; z <= Math.max(from.getZ(), to.getZ()) >> 4; z++) {
                if (!chunks.isChunkLoaded(x, z)) return false;
            }
        }
        return true;
    }
    public void syncWorldEvent(PlayerEntity player, int event, BlockPos pos, int value) {
        if (event==2001) {
            BlockState state=Block.getStateFromRawId(value);
            value=(net.minecraft.block.Block.getIdFromBlock(state.getBlock().data)&0xFFFFFF)
                | (state.data.write(new NBTTagCompound())&255)<<24;
        }
        data.playAuxSFXAtEntity(player == null ? null : player.data, event, pos.getX(), pos.getY(), pos.getZ(), value);
    }
    public static BlockState getPlacementState(net.minecraft.block.Block block, net.minecraft.world.World world, BlockPos pos) {
        Placement placement = PLACEMENT.get();
        return placement != null && placement.world == world && placement.pos.equals(pos)
            && placement.state.getBlock().data == block ? placement.state : null;
    }
    private static final class Placement {
        final net.minecraft.world.World world;
        final BlockPos pos;
        final BlockState state;
        Placement(net.minecraft.world.World world, BlockPos pos, BlockState state) {
            this.world = world; this.pos = pos; this.state = state;
        }
    }
}
