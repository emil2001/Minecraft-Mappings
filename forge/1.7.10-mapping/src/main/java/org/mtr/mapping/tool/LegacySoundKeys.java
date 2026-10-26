package org.mtr.mapping.tool;

import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.SoundCategory;
import org.mtr.mapping.holder.SoundEvent;
import java.util.Locale;

public final class LegacySoundKeys {
    public static final String NAMESPACE = "mtr_compat";
    private LegacySoundKeys() {}
    public static ResourceLocation key(SoundEvent sound, SoundCategory category) {
        if (NAMESPACE.equals(sound.data.getResourceDomain())) throw new IllegalArgumentException("Reserved compatibility sound namespace");
        return new ResourceLocation(NAMESPACE,category.name().toLowerCase(Locale.ROOT)+"/"+sound.data.getResourceDomain()+"/"+sound.data.getResourcePath());
    }
    public static final class Key {
        public final ResourceLocation source;
        public final SoundCategory category;
        private Key(ResourceLocation source, SoundCategory category) { this.source=source; this.category=category; }
    }
    public static Key decode(ResourceLocation id) {
        if (!NAMESPACE.equals(id.getResourceDomain())) return null;
        String path = id.getResourcePath(); int first=path.indexOf('/'), second=path.indexOf('/',first+1);
        if (first <= 0 || second <= first+1 || second == path.length()-1) return null;
        try {
            ResourceLocation source = new ResourceLocation(path.substring(first+1,second),path.substring(second+1));
            if (NAMESPACE.equals(source.getResourceDomain())) return null;
            return new Key(source,SoundCategory.valueOf(path.substring(0,first).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) { return null; }
    }
}
