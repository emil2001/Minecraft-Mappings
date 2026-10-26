package org.mtr.mapping.holder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;
import java.util.Objects;

public final class CompoundTag extends HolderBase<NBTTagCompound> {
    public CompoundTag(NBTTagCompound data) {
        super(Objects.requireNonNull(data, "NBT compound"));
    }

    public CompoundTag() { this(new NBTTagCompound()); }

    public void putLongArray(String key, long[] values) {
        NBTTagList list = new NBTTagList();
        for (long value : values) list.appendTag(new NBTTagLong(value));
        data.setTag(key, list);
    }

    public void putLongArray(String key, List<Long> values) {
        NBTTagList list = new NBTTagList();
        for (long value : values) list.appendTag(new NBTTagLong(value));
        data.setTag(key, list);
    }

    public long[] getLongArray(String key) {
        // 1.7.10 has no indexed long getter; consume a copy so repeated reads preserve links.
        NBTTagList list = (NBTTagList) data.getTagList(key, 4).copy();
        long[] result = new long[list.tagCount()];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = ((NBTTagLong) list.removeTag(i)).func_150291_c();
        }
        return result;
    }

    public void putIntArray(String key, List<Integer> values) {
        data.setIntArray(key, values.stream().mapToInt(Integer::intValue).toArray());
    }

    public void putIntArray(String key, int[] values) { data.setIntArray(key, values); }
    public int[] getIntArray(String key) { return data.getIntArray(key); }
    public boolean contains(String key) { return data.hasKey(key); }
    public boolean contains(String key, int type) { return data.hasKey(key, type); }
    public void remove(String key) { data.removeTag(key); }
    public boolean getBoolean(String key) { return data.getBoolean(key); }
    public void putBoolean(String key, boolean value) { data.setBoolean(key, value); }
    public int getInt(String key) { return data.getInteger(key); }
    public void putInt(String key, int value) { data.setInteger(key, value); }
    public long getLong(String key) { return data.getLong(key); }
    public void putLong(String key, long value) { data.setLong(key, value); }
    public float getFloat(String key) { return data.getFloat(key); }
    public void putFloat(String key, float value) { data.setFloat(key, value); }
    public String getString(String key) { return data.getString(key); }
    public void putString(String key, String value) { data.setString(key, value); }
}
