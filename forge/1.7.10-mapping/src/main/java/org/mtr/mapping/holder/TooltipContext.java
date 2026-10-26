package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

public final class TooltipContext extends HolderBase<Boolean> {
    public TooltipContext(boolean advanced) { super(advanced); }
    public boolean isAdvanced() { return data; }
}
