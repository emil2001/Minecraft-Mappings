package org.mtr.mapping.holder;

@FunctionalInterface
public interface ItemColorProvider {
    int getColor2(ItemStack stack,int tintIndex);
}
