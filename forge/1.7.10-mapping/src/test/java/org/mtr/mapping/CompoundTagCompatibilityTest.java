package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompressedStreamTools;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.CompoundTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CompoundTagCompatibilityTest {
    @Test
    void liftLinksPreserveFullWidthCoordinatesAndOrdering() throws Exception {
        NBTTagCompound nativeTag = new NBTTagCompound();
        CompoundTag tag = new CompoundTag(nativeTag);
        long[] links = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
        tag.putLongArray("track_floor_pos", links);
        ByteArrayOutputStream saved = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(nativeTag, saved);
        CompoundTag loaded = new CompoundTag(CompressedStreamTools.readCompressed(
            new ByteArrayInputStream(saved.toByteArray())));
        assertArrayEquals(links, loaded.getLongArray("track_floor_pos"));
        assertArrayEquals(links, loaded.getLongArray("track_floor_pos"), "Reading must not consume links");
    }

    @Test
    void listOverloadsAndExistingSettingsSurviveAlongsideLiftLinks() {
        CompoundTag tag = new CompoundTag();
        tag.putLongArray("links", Arrays.asList(-1L, Long.MAX_VALUE));
        tag.putIntArray("colors", Arrays.asList(-1, 0, Integer.MAX_VALUE));
        tag.putString("floor_number", "Подвал");
        tag.putFloat("offset", 0.25F);
        tag.putBoolean("active", true);
        CompoundTag loaded = new CompoundTag((NBTTagCompound) tag.data.copy());
        assertArrayEquals(new long[] {-1, Long.MAX_VALUE}, loaded.getLongArray("links"));
        assertArrayEquals(new int[] {-1, 0, Integer.MAX_VALUE}, loaded.getIntArray("colors"));
        assertEquals("Подвал", loaded.getString("floor_number"));
        assertEquals(0.25F, loaded.getFloat("offset"));
        assertTrue(loaded.getBoolean("active"));
        loaded.remove("active");
        assertFalse(loaded.contains("active"));
    }

    @Test
    void emptyAndAbsentLinksAreEmpty() {
        CompoundTag tag = new CompoundTag(new NBTTagCompound());
        assertArrayEquals(new long[0], tag.getLongArray("missing"));
        tag.putLongArray("track_floor_pos", new long[0]);
        assertArrayEquals(new long[0], tag.getLongArray("track_floor_pos"));
        tag.putString("wrong_type", "not a long array");
        assertArrayEquals(new long[0], tag.getLongArray("wrong_type"));
    }
}
