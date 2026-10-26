package org.mtr.mapping.holder;

import net.minecraft.block.material.Material;
import org.mtr.mapping.tool.HolderBase;
import java.util.Objects;
import java.util.function.ToIntFunction;

public final class BlockSettings extends HolderBase<Material> {
    float hardness;
    boolean opaque = true;
    final boolean blockPiston;
    ToIntFunction<BlockState> luminance;
    public BlockSettings(Material material) { this(material,false); }
    public BlockSettings(Material material,boolean blockPiston) { super(material); this.blockPiston=blockPiston; }
    public BlockSettings strength(float value) { hardness = value; return this; }
    public BlockSettings nonOpaque() { opaque = false; return this; }
    public BlockSettings luminance(ToIntFunction<BlockState> function) { luminance=Objects.requireNonNull(function); return this; }
}
