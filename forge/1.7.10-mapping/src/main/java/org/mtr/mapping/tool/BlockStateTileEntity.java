package org.mtr.mapping.tool;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.mtr.mapping.holder.BlockState;

/** Common persistent state for both state-only and functional MTR TileEntities. */
public class BlockStateTileEntity extends TileEntity {
    private NBTTagCompound state = new NBTTagCompound();

    public final void setMtrState(BlockState value) { value.data.write(state); }
    public final BlockState readMtrState(BlockStateDefinition definition, int metadata) {
        return new BlockState(definition.read(metadata, state));
    }
    @Override public boolean canUpdate() { return false; }
    @Override public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z) {
        return oldBlock != newBlock;
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (state.hasKey("mtr_block_state", 10)) tag.setTag("mtr_block_state", state.getCompoundTag("mtr_block_state").copy());
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        state = new NBTTagCompound();
        if (tag.hasKey("mtr_block_state", 10)) state.setTag("mtr_block_state", tag.getCompoundTag("mtr_block_state").copy());
        // Forge snapshot rollback restores an existing tile through NBT without relighting.
        if (worldObj != null) worldObj.func_147451_t(xCoord, yCoord, zCoord);
    }
    @Override public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, tag);
    }
    @Override public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
}
