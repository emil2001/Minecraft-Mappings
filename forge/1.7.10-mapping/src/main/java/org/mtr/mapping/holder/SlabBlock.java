package org.mtr.mapping.holder;

public final class SlabBlock {
    private SlabBlock() {}
    public static boolean isInstance(Block block) {
        // shortcut: other legacy mod slabs need an explicit pair adapter before exposing TYPE.
        return block.data instanceof org.mtr.mapping.mapper.SlabBlockExtension || org.mtr.mapping.tool.NativeSlabStates.supports(block.data);
    }
}
