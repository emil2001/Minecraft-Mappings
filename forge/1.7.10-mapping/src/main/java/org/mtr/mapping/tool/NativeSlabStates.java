package org.mtr.mapping.tool;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import org.mtr.mapping.holder.SlabType;

/** The four vanilla slab IDs have known pairs and share their low three variant bits. */
public final class NativeSlabStates {
    private NativeSlabStates() {}
    public static boolean supports(Block block) {
        return block==Blocks.stone_slab || block==Blocks.double_stone_slab || block==Blocks.wooden_slab || block==Blocks.double_wooden_slab;
    }
    public static SlabType type(Block block,int metadata) {
        return block==Blocks.double_stone_slab || block==Blocks.double_wooden_slab?SlabType.DOUBLE:(metadata&8)==0?SlabType.BOTTOM:SlabType.TOP;
    }
    public static Block blockForType(Block block,SlabType type) {
        boolean wood=block==Blocks.wooden_slab || block==Blocks.double_wooden_slab;
        return type==SlabType.DOUBLE?(wood?Blocks.double_wooden_slab:Blocks.double_stone_slab):(wood?Blocks.wooden_slab:Blocks.stone_slab);
    }
}
