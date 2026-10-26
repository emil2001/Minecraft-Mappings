package org.mtr.mapping.registry;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.tool.LegacyItemModels;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class NativeItemPredicateScenarios {
    public void originalRegisteredPredicateHandlesNativeZeroCountZoomStack() throws Exception {
        try (Fixture fixture=new Fixture("zero_count")) {
            net.minecraft.item.ItemStack zoom=new net.minecraft.item.ItemStack(fixture.item.get().data,0,0);
            assertEquals(1,assertDoesNotThrow(()->fixture.select(zoom),"NEI ItemZoom renders zero-count copies through the original selected predicate").layers.size());
            assertFalse(zoom.hasTagCompound(),"Rendering the zoom icon must not attach NBT");
        }
    }
    public void originalRegisteredPredicateReadsNormalSelectionWithoutMutatingNbt() throws Exception {
        try (Fixture fixture=new Fixture("normal_count")) {
            net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(fixture.item.get().data,1,0);
            assertEquals(1,fixture.select(stack).layers.size());
            assertFalse(stack.hasTagCompound(),"Rendering an ordinary inventory icon must not create NBT");
            NBTTagCompound tag=new NBTTagCompound(); tag.setString("unrelated","keep"); stack.setTagCompound(tag);
            NBTTagCompound before=(NBTTagCompound)tag.copy();
            assertEquals(1,fixture.select(stack).layers.size()); assertEquals(before,tag);
            tag.setLong("pos",42); before=(NBTTagCompound)tag.copy();
            assertEquals(Arrays.asList(new Identifier("mtr:item/rail_connector_300_one_way"),new Identifier("mtr:item/rail_connector_selected")),fixture.select(stack).layers);
            assertSame(tag,stack.getTagCompound()); assertEquals(before,tag);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture client=new NativeClientFixture();
        final ItemRegistryObject item;
        final RegistryClient.ModelPredicateProvider provider;
        final Map<net.minecraft.item.Item,Map<Identifier,RegistryClient.ModelPredicateProvider>> registered;
        final LegacyItemModels models;
        @SuppressWarnings("unchecked") Fixture(String suffix) throws Exception {
            Registry registry=new Registry();
            item=registry.registerItem(new Identifier("mtr:original_item_predicate_"+suffix),settings->new Item(new ItemExtension(settings))); registry.init();
            Method factory=org.mtr.mod.InitClient.class.getDeclaredMethod("checkItemPredicateTag"); factory.setAccessible(true);
            RegistryClient.ModelPredicateProvider original=(RegistryClient.ModelPredicateProvider)factory.invoke(null);
            RegistryClient clientRegistry=new RegistryClient(registry);
            Identifier selected=new Identifier("mtr:selected"); clientRegistry.registerItemModelPredicate(item,selected,original);
            Field renderers=RegistryClient.class.getDeclaredField("renderers"); renderers.setAccessible(true);
            for (Runnable registration:(List<Runnable>)renderers.get(clientRegistry)) registration.run();
            Field predicates=LegacyItemRenderers.class.getDeclaredField("predicates"); predicates.setAccessible(true);
            registered=(Map<net.minecraft.item.Item,Map<Identifier,RegistryClient.ModelPredicateProvider>>)predicates.get(null);
            provider=registered.get(item.get().data).get(selected);
            Path assets=Paths.get(System.getProperty("mtr.modelResources")).getParent().getParent();
            models=new LegacyItemModels(id->{
                Path file=assets.resolve(id.getNamespace()).resolve(id.getPath());
                try { return Files.exists(file)?new String(Files.readAllBytes(file),StandardCharsets.UTF_8):null; }
                catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            });
        }
        LegacyItemModels.Model select(net.minecraft.item.ItemStack stack) {
            return models.select(new Identifier("mtr:item/rail_connector_300_one_way"),key->provider.call(new ItemStack(stack),null,null));
        }
        public void close() { registered.remove(item.get().data); client.close(); }
    }
}
