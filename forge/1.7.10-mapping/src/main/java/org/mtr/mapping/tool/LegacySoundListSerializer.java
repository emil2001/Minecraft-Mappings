package org.mtr.mapping.tool;

import com.google.gson.*;
import net.minecraft.client.audio.SoundList;
import net.minecraft.client.audio.SoundListSerializer;
import net.minecraft.util.JsonUtils;
import java.lang.reflect.Type;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacySoundListSerializer extends SoundListSerializer {
    @Override @SuppressWarnings("unchecked") public SoundList deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        SoundList list = super.deserialize(json,type,context);
        JsonArray sounds = json.getAsJsonObject().getAsJsonArray("sounds");
        if (sounds != null) for (int i=0;i<sounds.size();i++) {
            if (!sounds.get(i).isJsonObject() || !sounds.get(i).getAsJsonObject().has("attenuation_distance")) continue;
            int distance = JsonUtils.getJsonObjectIntegerFieldValue(sounds.get(i).getAsJsonObject(),"attenuation_distance");
            if (distance <= 0) throw new JsonParseException("Invalid attenuation_distance: " + distance);
            list.getSoundList().set(i,new Entry((SoundList.SoundEntry)list.getSoundList().get(i),distance));
        }
        return list;
    }
    static final class Entry extends SoundList.SoundEntry {
        final int distance;
        Entry(SoundList.SoundEntry source, int distance) {
            this.distance=distance;
            setSoundEntryName(source.getSoundEntryName()); setSoundEntryType(source.getSoundEntryType());
            setSoundEntryPitch(source.getSoundEntryPitch()); setSoundEntryVolume(source.getSoundEntryVolume());
            setSoundEntryWeight(source.getSoundEntryWeight()); setStreaming(source.isStreaming());
        }
    }
}
