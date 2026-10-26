package org.mtr.mapping.tool;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacySounds {
    private LegacySounds() {}
    private static SoundManager manager(SoundHandler handler) { return ReflectionHelper.getPrivateValue(SoundHandler.class,handler,"sndManager","field_147694_f"); }
    public static boolean isPlaying(SoundHandler handler, ISound sound) {
        Map<?,?> playing = ReflectionHelper.getPrivateValue(SoundManager.class,manager(handler),"invPlayingSounds","field_148630_i");
        // The native 20-tick grace period also covers sources waiting for the audio thread.
        return playing.containsKey(sound);
    }
    public static void stopMtrSounds(SoundHandler handler) {
        Map<ISound,?> playing = ReflectionHelper.getPrivateValue(SoundManager.class,manager(handler),"invPlayingSounds","field_148630_i");
        // Vanilla world unload clears owners without removing their Paulscode sources.
        for (ISound sound : new ArrayList<>(playing.keySet())) {
            String domain = sound.getPositionedSoundLocation().getResourceDomain();
            if (domain.equals("mtr") || domain.equals(LegacySoundKeys.NAMESPACE)) stop(handler,sound);
        }
    }
    public static void stop(SoundHandler handler, ISound sound) {
        SoundManager manager = manager(handler);
        handler.stopSound(sound);
        Map<?,?> delayed = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"delayedSounds","field_148626_m");
        delayed.remove(sound);
        Map<?,?> playing = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"invPlayingSounds","field_148630_i");
        String channel = (String)playing.remove(sound);
        if (channel == null) return;
        Map<?,?> samples = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"playingSoundPoolEntries","field_148627_j");
        Map<?,?> stopTimes = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"playingSoundsStopTime","field_148624_n");
        List<?> tickable = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"tickableSounds","field_148625_l");
        com.google.common.collect.Multimap<?,?> categories = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"categorySounds","field_148628_k");
        samples.remove(sound); stopTimes.remove(channel); tickable.remove(sound); categories.values().remove(channel);
        paulscode.sound.SoundSystem system = ReflectionHelper.getPrivateValue(SoundManager.class,manager,"sndSystem","field_148620_e");
        system.removeSource(channel);
    }
    public static ISoundEventAccessor withDistance(final ISoundEventAccessor accessor, SoundList.SoundEntry entry) {
        if (!(entry instanceof LegacySoundListSerializer.Entry) || entry.getSoundEntryType() != SoundList.SoundEntry.Type.FILE) return accessor;
        final int distance = ((LegacySoundListSerializer.Entry)entry).distance;
        return new ISoundEventAccessor() {
            public int func_148721_a() { return accessor.func_148721_a(); }
            public SoundPoolEntry func_148720_g() { return new DistantSample((SoundPoolEntry)accessor.func_148720_g(),distance); }
        };
    }
    private static final class DistantSample extends SoundPoolEntry {
        final int distance;
        DistantSample(SoundPoolEntry sample, int distance) { super(sample); this.distance=distance; }
    }
    public static float distance(SoundPoolEntry sample) { return sample instanceof DistantSample ? ((DistantSample)sample).distance : 16; }
    private static SoundRegistry registry(SoundHandler handler) { return ReflectionHelper.getPrivateValue(SoundHandler.class,handler,"sndRegistry","field_147697_e"); }
    public static SoundEventAccessorComposite resolve(SoundHandler handler, ResourceLocation id) {
        LegacySoundKeys.Key key = LegacySoundKeys.decode(id);
        if (key == null) return null;
        SoundRegistry registry = registry(handler);
        SoundEventAccessorComposite source = (SoundEventAccessorComposite)registry.getObject(key.source);
        if (source == null) return null;
        SoundEventAccessorComposite cached = (SoundEventAccessorComposite)registry.getObject(id);
        if (cached != null) return cached;
        SoundEventAccessorComposite alias = new SoundEventAccessorComposite(id,1,1,SoundCategory.valueOf(key.category.name()));
        alias.addSoundToEventPool(source); registry.registerSound(alias); return alias;
    }
    @SuppressWarnings("unchecked") public static void iterateIds(Consumer<Identifier> consumer) {
        SoundRegistry registry = registry(net.minecraft.client.Minecraft.getMinecraft().getSoundHandler());
        for (Object key : new ArrayList<>(registry.getKeys())) {
            ResourceLocation id = (ResourceLocation)key;
            if (!LegacySoundKeys.NAMESPACE.equals(id.getResourceDomain())) consumer.accept(new Identifier(id.toString()));
        }
    }
}
