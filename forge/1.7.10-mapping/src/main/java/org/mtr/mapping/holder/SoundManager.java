package org.mtr.mapping.holder;
import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class SoundManager extends NativeHolder<net.minecraft.client.audio.SoundHandler> {
    public SoundManager(net.minecraft.client.audio.SoundHandler data) { super(data); }
    public boolean isPlaying(SoundInstance sound) { return org.mtr.mapping.tool.LegacySounds.isPlaying(data,sound.data); }
    public void stop(SoundInstance sound) { org.mtr.mapping.tool.LegacySounds.stop(data,sound.data); }
    public void play(SoundInstance sound) {
        if (isPlaying(sound)) return;
        if (sound.data instanceof org.mtr.mapping.mapper.AbstractSoundInstanceExtension
                && !((org.mtr.mapping.mapper.AbstractSoundInstanceExtension)sound.data).canPlay2()) return;
        // shortcut: native fully muted sources do not start; MTR starts vehicle loops after positive volume, revisit for silent pre-roll.
        data.playSound(sound.data);
    }
}
