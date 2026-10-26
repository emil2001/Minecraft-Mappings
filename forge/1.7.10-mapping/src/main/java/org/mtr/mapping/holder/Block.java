package org.mtr.mapping.holder;

import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mapping.tool.HolderBase;

public final class Block extends HolderBase<net.minecraft.block.Block> {
    public Block(net.minecraft.block.Block data) { super(data); }
    public String getTranslationKey() { return org.mtr.mapping.tool.LegacyTranslations.getTranslationKey(data.getUnlocalizedName()); }
    public static int getRawIdFromState(BlockState state) {
        return org.mtr.mapping.tool.RawBlockStateIds.encode(state.data);
    }
    public static BlockState getStateFromRawId(int rawId) {
        return new BlockState(org.mtr.mapping.tool.RawBlockStateIds.decode(rawId));
    }
    public static Block getBlockFromItem(Item item) {
        return item==null || !(item.data instanceof net.minecraft.item.ItemBlock)?Blocks.getAirMapped()
            :new Block(((net.minecraft.item.ItemBlock)item.data).field_150939_a);
    }
    public Item asItem() { return new Item(net.minecraft.item.Item.getItemFromBlock(data)); }
    public MapColor getDefaultMapColor() { return new MapColor(data.getMapColor(0)); }
    public static VoxelShape createCuboidShape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return VoxelShapes.cuboid(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }
    public BlockState getDefaultState() {
        return data instanceof BlockAbstractMapping ? ((BlockAbstractMapping) data).getDefaultState2()
            : new BlockState(BlockStateDefinition.forBlock(data).defaultState());
    }
    @Override public boolean equals(Object other) { return other instanceof Block && data == ((Block) other).data; }
    @Override public int hashCode() { return data.hashCode(); }
}
