package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import net.minecraft.client.resources.IResourceManager;
import static org.junit.jupiter.api.Assertions.*;

public class TextureCompatibilityScenarios {
    public void replacingAndDestroyingTexturesReleasesOldObjectsAndRemovesNativeEntries() {
        net.minecraft.client.renderer.texture.TextureManager nativeManager = new net.minecraft.client.renderer.texture.TextureManager(null);
        TextureManager manager = new TextureManager(nativeManager);
        Identifier id = new Identifier("mtr", "texture_lifetime_test");
        ProbeTexture first = new ProbeTexture(), replacement = new ProbeTexture();
        manager.registerTexture(id, new AbstractTexture(first));
        assertSame(first, nativeManager.getTexture(id.data));
        manager.registerTexture(id, new AbstractTexture(replacement));
        assertEquals(1, first.closed); assertEquals(1, replacement.loaded);
        ProbeTexture failed = new ProbeTexture(); failed.fail = true;
        assertThrows(net.minecraft.util.ReportedException.class, () -> manager.registerTexture(id, new AbstractTexture(failed)));
        assertSame(replacement, nativeManager.getTexture(id.data));
        assertEquals(1, failed.closed); assertEquals(0, replacement.closed);
        manager.destroyTexture(id);
        assertNull(nativeManager.getTexture(id.data));
        assertEquals(1, replacement.closed);
        manager.destroyTexture(id);
        assertEquals(1, replacement.closed);
        NativeImage image = new NativeImage(1, 1, false);
        NativeImageBackedTexture lazy = new NativeImageBackedTexture(image);
        assertSame(image, lazy.getImage(), "Creating a texture on an upload worker must not require a GL context");
        lazy.close(); lazy.close();
        assertNull(lazy.getImage());
        assertThrows(IllegalStateException.class, () -> image.getColor(0, 0));
        assertThrows(IllegalStateException.class, () -> lazy.data.getGlTextureId());
    }
    static final class ProbeTexture extends net.minecraft.client.renderer.texture.AbstractTexture {
        int closed, loaded;
        boolean fail;
        public void loadTexture(IResourceManager manager) {
            loaded++;
            if (fail) throw new IllegalStateException("Upload failed before registration");
        }
        @Override public void deleteGlTexture() { closed++; }
    }
}
