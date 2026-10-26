package org.mtr.mapping.registry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.EntityExtension;
import java.util.*;
import java.util.function.*;

/** Retains modern factories; resolves the legacy class from the first real entity, never a dummy world. */
public final class EntityFactoryRegistry {
    private static final Map<String,Entry<?>> TYPES = new LinkedHashMap<>();
    private static final Map<Class<?>,Entry<?>> CLASSES = new HashMap<>();
    private static final Map<String,Integer> NEXT_IDS = new HashMap<>();
    private EntityFactoryRegistry() {}
    @FunctionalInterface public interface Visibility { boolean shouldRender(EntityExtension entity, double x, double y, double z); }
    public static <T extends EntityExtension> EntityType<T> register(Identifier id, BiFunction<EntityType<?>,World,T> factory, float width, float height) {
        if (TYPES.containsKey(id.toString()) || !(width > 0) || !(height > 0) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("Invalid or duplicate entity type " + id);
        }
        ModContainer owner = Objects.requireNonNull(Loader.instance().activeModContainer(), "Entity registration requires an active mod");
        int nativeId = NEXT_IDS.getOrDefault(owner.getModId(),0); NEXT_IDS.put(owner.getModId(),nativeId+1);
        Entry<T> entry = new Entry<>(id,Objects.requireNonNull(factory),width,height,owner,nativeId);
        TYPES.put(id.toString(),entry); return entry.type;
    }
    public static Entity byName(String name, net.minecraft.world.World world) {
        Entry<?> entry = TYPES.get(name); return entry == null ? null : entry.create(world);
    }
    public static Entity fromNbt(NBTTagCompound tag, net.minecraft.world.World world) {
        Entity entity = byName(tag.getString("id"),world);
        if (entity != null) entity.readFromNBT(tag);
        return entity;
    }
    public static EntityRegistry.EntityRegistration forSpawn(ModContainer owner, int networkId) {
        for (Entry<?> entry : TYPES.values()) {
            if (entry.nativeId == networkId && entry.owner.equals(owner.getModId()) && entry.clientSpawner != null) return entry.nativeRegistration;
        }
        return null;
    }
    public static final class Entry<T extends EntityExtension> {
        public final Identifier id;
        public final float width,height;
        public final EntityType<T> type;
        private final BiFunction<EntityType<?>,World,T> factory;
        private final String owner;
        private final int nativeId;
        private Class<? extends EntityExtension> entityClass;
        public Visibility visibility;
        private EntityRegistry.EntityRegistration nativeRegistration;
        private com.google.common.base.Function<cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage,Entity> clientSpawner;
        private final List<Consumer<Class<? extends EntityExtension>>> listeners = new ArrayList<>();
        private Entry(Identifier id, BiFunction<EntityType<?>,World,T> factory, float width, float height, ModContainer owner, int nativeId) {
            this.id=id; this.factory=factory; this.width=width; this.height=height; this.owner=owner.getModId(); this.nativeId=nativeId;
            // FML needs a spawn descriptor before the first entity reveals its class; the custom factory supplies that entity.
            nativeRegistration = EntityRegistry.instance().new EntityRegistration(owner,null,id.getPath(),nativeId,64,3,true);
            type = new EntityType<>(this);
        }
        public T create(net.minecraft.world.World world) {
            T entity = Objects.requireNonNull(factory.apply(type,new World(Objects.requireNonNull(world))));
            if (entity.getType2() != type || entity.worldObj != world) throw new IllegalArgumentException("Entity factory changed its type or world: " + id);
            return entity;
        }
        @SuppressWarnings("unchecked") public void bind(Class<? extends EntityExtension> actualClass) {
            synchronized (EntityFactoryRegistry.class) {
                if (entityClass != null) {
                    if (entityClass != actualClass) throw new IllegalArgumentException("Entity factory changed class: " + id);
                    return;
                }
                if (CLASSES.containsKey(actualClass)) throw new IllegalArgumentException("Entity class already registered: " + actualClass.getName());
                EntityRegistry.registerModEntity(actualClass,id.getPath(),nativeId,owner,64,3,true);
                EntityRegistry.EntityRegistration registration = EntityRegistry.instance().lookupModSpawn(actualClass,false);
                if (registration == null || !owner.equals(registration.getContainer().getModId()) || registration.getModEntityId() != nativeId) {
                    throw new IllegalStateException("Forge entity registration failed: " + id);
                }
                if (clientSpawner != null) registration.setCustomSpawning(clientSpawner,false);
                nativeRegistration=registration;
                EntityList.classToStringMapping.put(actualClass,id.toString());
                EntityList.stringToClassMapping.put(id.toString(),actualClass);
                entityClass=actualClass; CLASSES.put(actualClass,this);
                listeners.forEach(listener -> listener.accept(actualClass)); listeners.clear();
            }
        }
        public void setClientSpawner(com.google.common.base.Function<cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage,Entity> spawner) {
            clientSpawner=Objects.requireNonNull(spawner); nativeRegistration.setCustomSpawning(spawner,false);
        }
        public void whenClassKnown(Consumer<Class<? extends EntityExtension>> listener) {
            synchronized (EntityFactoryRegistry.class) {
                if (entityClass == null) listeners.add(listener); else listener.accept(entityClass);
            }
        }
    }
}
