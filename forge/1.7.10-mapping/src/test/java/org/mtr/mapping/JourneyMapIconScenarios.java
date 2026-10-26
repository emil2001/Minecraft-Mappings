package org.mtr.mapping;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.registry.LegacyBlockRenderers;
import org.mtr.mapping.tool.BlockStateTileEntity;
import org.mtr.mapping.tool.HolderBase;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class JourneyMapIconScenarios {
    public static final class Extended extends BlockExtension {
        static final IntegerProperty VALUE=IntegerProperty.of("value",0,31);
        Extended() { super(new BlockSettings(net.minecraft.block.material.Material.rock)); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(VALUE); }
    }
    public static final class Compact extends BlockExtension {
        static final IntegerProperty VALUE=IntegerProperty.of("value",0,1);
        Compact() { super(new BlockSettings(net.minecraft.block.material.Material.rock)); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(VALUE); }
    }

    public void originalEscalatorMetadataIconsDoNotRequirePersistentState() throws Exception {
        org.mtr.mod.Blocks.init();
        BlockAbstractMapping original=(BlockAbstractMapping)org.mtr.mod.Blocks.ESCALATOR_STEP.get().data;
        assertTrue(original.getStateDefinition().requiresTileEntity());
        IIcon first;
        try(Atlas fixture=new Atlas(new Resources(),original)) {
            for(int metadata:new int[]{0,1,5,9,13,15}) {
                assertEquals(metadata,original.getStateDefinition().readRawStateIndex(metadata).write(new NBTTagCompound()));
                assertSourceIcon(fixture,original.getIcon(1,metadata),"mtr:block/escalator");
            }
            first=original.getIcon(1,0);
            assertThrows(IllegalArgumentException.class,()->original.getIcon(1,-1));
            assertThrows(IllegalArgumentException.class,()->original.getIcon(1,16));
            assertThrows(IllegalArgumentException.class,()->original.getStateDefinition().read(0,new NBTTagCompound()));
        }
        try(Atlas replacement=new Atlas(new Resources(),original)) {
            assertSourceIcon(replacement,original.getIcon(1,0),"mtr:block/escalator");
            assertNotSame(first,original.getIcon(1,0),"A replacement atlas context must return its own sprite");
        }
    }

    public void metadataProjectionKeepsIconsAndStrictWorldStateSeparate() throws Exception {
        Extended extended=new Extended(); Compact compact=new Compact();
        BlockPlacementScenarios.registerProbe("jm_icon_extended",extended);
        BlockPlacementScenarios.registerProbe("jm_icon_compact",compact);
        Resources resources=new Resources(); resources.variants("jm_icon_extended",32); resources.variants("jm_icon_compact",2);
        try(Atlas fixture=new Atlas(resources,extended,compact)) {
            assertSourceIcon(fixture,extended.getIcon(0,1),"mtr:block/jm_icon_1");
            assertSourceIcon(fixture,extended.getIcon(0,13),"mtr:block/jm_icon_13");
            assertSourceIcon(fixture,compact.getIcon(0,1),"mtr:block/jm_icon_1");
            assertThrows(IllegalArgumentException.class,()->compact.getIcon(0,2));
            assertThrows(IllegalArgumentException.class,()->extended.getIcon(0,16));

            BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
            World world=new World(nativeWorld); BlockPos pos=new BlockPos(4,64,4);
            BlockState full=extended.getDefaultState2().with(new Property<>(Extended.VALUE.data),29);
            assertTrue(world.setBlockState(pos,full,2));
            assertEquals(13,nativeWorld.getBlockMetadata(4,64,4));
            assertSourceIcon(fixture,extended.getIcon(nativeWorld,4,64,4,0),"mtr:block/jm_icon_29");
            assertSourceIcon(fixture,extended.getIcon(0,13),"mtr:block/jm_icon_13");

            BlockStateTileEntity tile=(BlockStateTileEntity)nativeWorld.getTileEntity(4,64,4);
            NBTTagCompound valid=new NBTTagCompound(); tile.writeToNBT(valid);
            tile.readFromNBT(new NBTTagCompound());
            assertThrows(IllegalArgumentException.class,()->extended.getIcon(nativeWorld,4,64,4,0));
            NBTTagCompound invalid=(NBTTagCompound)valid.copy(); invalid.getCompoundTag("mtr_block_state").setString("value","32");
            tile.readFromNBT(invalid);
            assertThrows(IllegalArgumentException.class,()->extended.getIcon(nativeWorld,4,64,4,0));
            tile.readFromNBT(valid);
            assertSourceIcon(fixture,extended.getIcon(nativeWorld,4,64,4,0),"mtr:block/jm_icon_29");
            // Native World may recreate a removed TE; exercise the strict reader's explicit null boundary.
            net.minecraft.world.IBlockAccess missingTile=(net.minecraft.world.IBlockAccess)Proxy.newProxyInstance(
                net.minecraft.world.IBlockAccess.class.getClassLoader(),new Class[]{net.minecraft.world.IBlockAccess.class},(proxy,method,args)-> {
                    if(method.getName().equals("getTileEntity")) return null;
                    try { return method.invoke(nativeWorld,args); }
                    catch(InvocationTargetException failure) { throw failure.getCause(); }
                });
            assertThrows(IllegalStateException.class,()->extended.getIcon(missingTile,4,64,4,0));
        }
    }

    private static void assertSourceIcon(Atlas fixture,IIcon icon,String name) {
        assertNotNull(icon); assertEquals(name,icon.getIconName());
        assertSame(fixture.atlas.getTextureExtry(name),icon,"Resolve the registered native particle sprite, not missing/default fallback");
    }

    /** CPU atlas registration/model selection only; no GL texture upload or client window. */
    private static final class Atlas implements AutoCloseable {
        final TextureMap atlas=new TextureMap(2,"textures/blocks");
        final Field active=field("active"),preparing=field("preparing");
        final Object oldActive,oldPreparing;
        Atlas(IResourceManager resources,BlockAbstractMapping... blocks) throws Exception {
            oldActive=active.get(null); oldPreparing=preparing.get(null);
            Class<?> type=Class.forName("org.mtr.mapping.registry.LegacyBlockRenderers$Context");
            Constructor<?> constructor=type.getDeclaredConstructor(TextureMap.class,IResourceManager.class); constructor.setAccessible(true);
            Object context=constructor.newInstance(atlas,resources);
            preparing.set(null,context);
            try { for(BlockAbstractMapping block:blocks) block.registerBlockIcons(atlas); }
            finally { preparing.set(null,oldPreparing); }
            active.set(null,context);
            for(Object value:registeredSprites(atlas).values()) {
                TextureAtlasSprite sprite=(TextureAtlasSprite)value;
                sprite.setIconWidth(16); sprite.setIconHeight(16); sprite.initSprite(16,16,0,0,false);
            }
        }
        public void close() throws Exception { active.set(null,oldActive); preparing.set(null,oldPreparing); }
        private static Field field(String name) {
            try { Field field=LegacyBlockRenderers.class.getDeclaredField(name); field.setAccessible(true); return field; }
            catch(ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        private static Map registeredSprites(TextureMap atlas) {
            return cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(TextureMap.class,atlas,"mapRegisteredSprites","field_110574_e");
        }
    }

    private static final class Resources implements IResourceManager {
        final Map<String,String> generated=new HashMap<>();
        final Path original=Paths.get(System.getProperty("mtr.modelResources")).getParent();
        void variants(String name,int count) {
            List<String> variants=new ArrayList<>();
            for(int value=0;value<count;value++) {
                variants.add("\"value="+value+"\":{\"model\":\"mtr:block/jm_icon_"+value+"\"}");
                generated.put("models/block/jm_icon_"+value+".json","{\"textures\":{\"particle\":\"mtr:block/jm_icon_"+value+"\"},\"elements\":[]}");
            }
            generated.put("blockstates/"+name+".json","{\"variants\":{"+String.join(",",variants)+"}}");
        }
        public Set getResourceDomains() { return Collections.singleton("mtr"); }
        public List getAllResources(ResourceLocation location) throws IOException { return Collections.singletonList(getResource(location)); }
        public IResource getResource(ResourceLocation location) throws IOException {
            if(!"mtr".equals(location.getResourceDomain())) throw new FileNotFoundException(location.toString());
            String text=generated.get(location.getResourcePath());
            final byte[] bytes=text==null?Files.readAllBytes(original.resolve(location.getResourcePath())):text.getBytes(StandardCharsets.UTF_8);
            return new IResource() {
                public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
                public boolean hasMetadata() { return false; }
                public IMetadataSection getMetadata(String name) { return null; }
            };
        }
    }
}
