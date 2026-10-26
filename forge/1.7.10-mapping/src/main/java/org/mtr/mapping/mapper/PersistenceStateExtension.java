package org.mtr.mapping.mapper;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ServerWorld;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class PersistenceStateExtension extends WorldSavedData {
    public PersistenceStateExtension(String key) { super(key); }
    public abstract void readNbt(CompoundTag tag);
    public abstract CompoundTag writeNbt2(CompoundTag tag);
    public void markDirty2() { markDirty(); }
    @Override public final void readFromNBT(NBTTagCompound tag) { readNbt(new CompoundTag(tag)); }
    @Override public final void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound written=Objects.requireNonNull(writeNbt2(new CompoundTag(tag))).data;
        if (written!=tag) for (Object key : written.func_150296_c()) tag.setTag((String)key,written.getTag((String)key).copy());
    }
    public static PersistenceStateExtension register(ServerWorld world,Supplier<PersistenceStateExtension> supplier,String key) {
        if (key==null || !key.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid persistent data key");
        MapStorage storage=world.data.perWorldStorage;
        WorldSavedData saved=storage.loadData(Snapshot.class,key);
        if (saved instanceof PersistenceStateExtension) return (PersistenceStateExtension)saved;
        if (saved!=null && (!(saved instanceof Snapshot) || ((Snapshot)saved).tag==null))
            throw new IllegalStateException("Cannot load persistent data: "+key);
        PersistenceStateExtension state=Objects.requireNonNull(supplier.get());
        if (!key.equals(state.mapName)) throw new IllegalArgumentException("Persistent data key differs from constructor key");
        if (saved!=null) state.readFromNBT(((Snapshot)saved).tag);
        else state.markDirty2();
        storage.setData(key,state);
        return state;
    }
    /** Native MapStorage requires a String constructor; MTR supplies factory-only subclasses. */
    public static final class Snapshot extends WorldSavedData {
        private NBTTagCompound tag;
        public Snapshot(String key) { super(key); }
        @Override public void readFromNBT(NBTTagCompound tag) { this.tag=(NBTTagCompound)tag.copy(); }
        @Override public void writeToNBT(NBTTagCompound tag) { throw new IllegalStateException("Unbound persistent data snapshot"); }
    }
}
