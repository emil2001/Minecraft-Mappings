package org.mtr.mapping.registry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.mtr.mapping.holder.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class BlockEntityFactoryRegistry {
    private static final String FACTORY_ID = "mtr:factory";
    private static final Map<Identifier, Entry<?>> TYPES = new LinkedHashMap<>();
    private BlockEntityFactoryRegistry() {}

    @SafeVarargs
    public static <T extends BlockEntityAbstractMapping> BlockEntityType<T> register(Identifier id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<Block>... blocks) {
        if (TYPES.containsKey(id) || blocks.length == 0) throw new IllegalArgumentException("Invalid or duplicate block entity type " + id);
        BlockEntityType<T> type = new BlockEntityType<>(id, factory);
        TYPES.put(id, new Entry<>(type, blocks.clone()));
        return type;
    }

    public static TileEntity load(NBTTagCompound tag) {
        if (!FACTORY_ID.equals(tag.getString("id"))) return null;
        Identifier id = new Identifier(tag.getString("mtr_type"));
        Entry<?> entry = TYPES.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown MTR block entity type " + id);
        net.minecraft.block.Block block = (net.minecraft.block.Block) net.minecraft.block.Block.blockRegistry.getObject(tag.getString("mtr_block"));
        if (!(block instanceof BlockAbstractMapping) || !entry.accepts(block)) throw new IllegalArgumentException("Invalid block for MTR block entity " + id);
        BlockState state = new BlockState(((BlockAbstractMapping) block).getStateDefinition().read(tag.getInteger("mtr_meta"), tag));
        BlockEntityAbstractMapping entity = entry.type.data.apply(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")), state);
        entity.readFromNBT(tag);
        return entity;
    }

    public static void write(BlockEntityAbstractMapping entity, BlockEntityType<?> type, BlockState state, NBTTagCompound tag) {
        Entry<?> entry = TYPES.get(BlockEntityType.getId(type));
        if (entry == null || entry.type != type || !entry.accepts(state.getBlock().data)) throw new IllegalArgumentException("Unregistered MTR block entity");
        tag.setString("id", FACTORY_ID);
        tag.setString("mtr_type", BlockEntityType.getId(type).toString());
        tag.setString("mtr_block", net.minecraft.block.Block.blockRegistry.getNameForObject(state.getBlock().data));
        tag.setInteger("mtr_meta", state.data.write(tag));
        tag.setInteger("x", entity.xCoord);
        tag.setInteger("y", entity.yCoord);
        tag.setInteger("z", entity.zCoord);
    }

    private static final class Entry<T extends BlockEntityAbstractMapping> {
        final BlockEntityType<T> type;
        final Supplier<Block>[] blocks;
        Entry(BlockEntityType<T> type, Supplier<Block>[] blocks) { this.type = type; this.blocks = blocks; }
        boolean accepts(net.minecraft.block.Block block) { return Arrays.stream(blocks).anyMatch(supplier -> supplier.get().data == block); }
    }
}
