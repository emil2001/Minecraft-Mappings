package org.mtr.mapping.holder;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.tool.NativeHolder;
import java.util.Map;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class TextureManager extends NativeHolder<net.minecraft.client.renderer.texture.TextureManager> {
    public TextureManager(net.minecraft.client.renderer.texture.TextureManager data) { super(data); }
    public void registerTexture(Identifier id, AbstractTexture texture) {
        ITextureObject previous = data.getTexture(id.data);
        try {
            if (!data.loadTexture(id.data, texture.data)) {
                textures().remove(id.data);
                throw new IllegalStateException("Cannot load MTR texture " + id);
            }
        } catch (RuntimeException | Error exception) {
            if (texture.data != previous) close(texture.data);
            throw exception;
        } finally {
            if (previous != null && previous != texture.data && data.getTexture(id.data) != previous) close(previous);
        }
    }
    public Identifier registerDynamicTexture(String name, NativeImageBackedTexture texture) {
        Map<String, Integer> counters = ReflectionHelper.getPrivateValue(net.minecraft.client.renderer.texture.TextureManager.class,
            data, "mapTextureCounters", "field_110584_c");
        int count = counters.containsKey(name) ? counters.get(name) + 1 : 1;
        counters.put(name, count);
        Identifier id = new Identifier("dynamic/" + name + "_" + count);
        registerTexture(id, new AbstractTexture(texture.data));
        return id;
    }
    public void destroyTexture(Identifier id) {
        ITextureObject texture = textures().remove(id.data);
        if (texture != null) close(texture);
    }
    private Map<ResourceLocation, ITextureObject> textures() {
        return ReflectionHelper.getPrivateValue(net.minecraft.client.renderer.texture.TextureManager.class,
            data, "mapTextureObjects", "field_110585_a");
    }
    private static void close(ITextureObject texture) {
        if (texture instanceof DynamicTexture && texture == TextureUtil.missingTexture) return;
        if (texture instanceof net.minecraft.client.renderer.texture.AbstractTexture) {
            ((net.minecraft.client.renderer.texture.AbstractTexture) texture).deleteGlTexture();
        } else TextureUtil.deleteTexture(texture.getGlTextureId());
    }
}
