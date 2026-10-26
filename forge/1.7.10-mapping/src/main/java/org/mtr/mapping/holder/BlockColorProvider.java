package org.mtr.mapping.holder;

@FunctionalInterface
public interface BlockColorProvider {
    int getColor2(BlockState state,BlockView world,BlockPos pos,int tint);
}
