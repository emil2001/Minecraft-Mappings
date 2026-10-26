package org.mtr.mapping.mapper;
import org.mtr.mapping.holder.SoundCategory;
import org.mtr.mapping.holder.SoundEvent;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class MovingSoundInstanceExtension extends AbstractSoundInstanceExtension implements TickableSoundInstanceExtension {
    private boolean done;
    protected MovingSoundInstanceExtension(SoundEvent sound, SoundCategory category) { super(sound,category); }
    protected void setVolume(float value) { volume=value; }
    protected void setPitch(float value) { field_147663_c=value; }
    protected void setRepeatDelay(int value) { field_147665_h=value; }
    protected void setX(double value) { setXMapped(value); }
    protected void setY(double value) { setYMapped(value); }
    protected void setZ(double value) { setZMapped(value); }
    public void setDone2() { done=true; }
    public boolean isDone() { return done; }
}
