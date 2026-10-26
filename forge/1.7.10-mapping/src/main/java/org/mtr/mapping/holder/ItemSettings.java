package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

public final class ItemSettings extends HolderBase<ItemSettings.Values> {
    public ItemSettings() { super(new Values()); }
    public ItemSettings maxCount(int count) {
        if (count < 1 || count > 64) throw new IllegalArgumentException("Legacy stack limit must be 1..64");
        data.maxCount = count;
        return this;
    }
    public int getMaxCount() { return data.maxCount; }
    public static final class Values { private int maxCount = 64; }
}
