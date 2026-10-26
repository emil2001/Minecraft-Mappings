package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacySoundKeys;
import java.util.function.Consumer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class AbstractSoundInstanceExtension extends net.minecraft.client.audio.PositionedSound {
    protected AbstractSoundInstanceExtension(SoundEvent sound, SoundCategory category) { super(LegacySoundKeys.key(sound,category)); }
    protected void setIsRepeatableMapped(boolean value) { repeat=value; }
    public boolean isRepeatable() { return repeat; }
    public boolean isRelative() { return false; }
    public double getXMapped() { return xPosF; }
    public double getYMapped() { return yPosF; }
    public double getZMapped() { return zPosF; }
    protected void setXMapped(double value) { xPosF=(float)value; }
    protected void setYMapped(double value) { yPosF=(float)value; }
    protected void setZMapped(double value) { zPosF=(float)value; }
    public boolean canPlay2() { return true; }
    public boolean shouldAlwaysPlay2() { return false; }
    public static void iterateSoundIds(Consumer<Identifier> consumer) { org.mtr.mapping.tool.LegacySounds.iterateIds(consumer); }
}
