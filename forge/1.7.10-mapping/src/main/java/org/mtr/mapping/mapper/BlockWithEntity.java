package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;

public interface BlockWithEntity {
    BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state);
}
