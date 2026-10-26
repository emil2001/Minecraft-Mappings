package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import java.util.function.BiFunction;

public final class BlockEntityType<T extends BlockEntityAbstractMapping> extends HolderBase<BiFunction<BlockPos, BlockState, T>> {
    private final Identifier id;
    public boolean hasClientRenderer;
    public BlockEntityType(Identifier id, BiFunction<BlockPos, BlockState, T> factory) { super(factory); this.id = id; }
    public static Identifier getId(BlockEntityType<?> type) { return type.id; }
}
