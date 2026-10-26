package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.EntityExtension;
import org.mtr.mapping.registry.Registry;
import org.mtr.mapping.registry.EntityTypeRegistryObject;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityCompatibilityScenarios {
    public static final class Carrier extends EntityExtension {
        static int constructed;
        int ticks;
        Carrier(EntityType<?> type, World world) { super(type,world); constructed++; }
        @Override protected void initDataTracker2() {}
        @Override public void tick2() { ticks++; }
    }
    public void entityFactoriesUseTheRealWorldKeepNativeNbtAndDoNotNeedLegacyConstructors() {
        installModLookup();
        Registry registry = new Registry();
        EntityTypeRegistryObject<Carrier> registration = registry.registerEntityType(new Identifier("mtr:factory_carrier_probe"),
            Carrier::new, Float.MIN_VALUE, Float.MIN_VALUE);
        assertThrows(IllegalStateException.class, registration::get);
        int before = Carrier.constructed;
        registry.init();
        assertEquals(before, Carrier.constructed, "Registration must not construct an entity with a fake world");
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.Entity entity = EntityList.createEntityByName("mtr:factory_carrier_probe", nativeWorld);
        assertTrue(entity instanceof Carrier);
        Carrier carrier = (Carrier) entity;
        assertSame(nativeWorld, carrier.getEntityWorld2().data);
        carrier.setPosition2(-34.5,65,18.25);
        carrier.onUpdate(); assertEquals(1, carrier.ticks);
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(carrier.writeToNBTOptional(tag));
        assertEquals("mtr:factory_carrier_probe", tag.getString("id"));
        Carrier restored = (Carrier) EntityList.createEntityFromNBT(tag, nativeWorld);
        assertNotSame(carrier, restored); assertEquals(carrier.getUniqueID(), restored.getUniqueID());
        assertEquals(-34.5, restored.getPos2().getXMapped());
        assertEquals(65, restored.getPos2().getYMapped());
        assertEquals(18.25, restored.getPos2().getZMapped());
        assertTrue(EntityList.createEntityByName("Pig", nativeWorld) instanceof net.minecraft.entity.passive.EntityPig);
        restored.kill2(); assertTrue(restored.isDead);
        cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration nativeType =
            cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn(Carrier.class, false);
        assertNotNull(nativeType, "Forge tracking must use the actual carrier class");
        assertEquals("mtr", nativeType.getContainer().getModId());
    }
    static void installModLookup() {
        cpw.mods.fml.common.Loader loader = cpw.mods.fml.common.Loader.instance();
        Map<String,cpw.mods.fml.common.ModContainer> current = ReflectionHelper.getPrivateValue(cpw.mods.fml.common.Loader.class, loader, "namedMods");
        Map<String,cpw.mods.fml.common.ModContainer> mods = current == null ? new HashMap<>() : new HashMap<>(current);
        mods.put("mtr", loader.activeModContainer());
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.Loader.class, loader, mods, "namedMods");
    }
}
