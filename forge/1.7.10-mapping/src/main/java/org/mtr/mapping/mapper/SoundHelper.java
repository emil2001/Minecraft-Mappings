package org.mtr.mapping.mapper;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.SoundEvent;

public final class SoundHelper {
    private SoundHelper() {}
    public static SoundEvent createSoundEvent(Identifier id) { return new SoundEvent(id.data); }
}
