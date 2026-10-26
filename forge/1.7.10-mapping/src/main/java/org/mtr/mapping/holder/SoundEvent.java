package org.mtr.mapping.holder;
import org.mtr.mapping.tool.HolderBase;

public final class SoundEvent extends HolderBase<net.minecraft.util.ResourceLocation> {
    public SoundEvent(net.minecraft.util.ResourceLocation id) { super(id); }
    public Identifier getId() { return new Identifier(data.toString()); }
}
