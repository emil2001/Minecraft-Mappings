package org.mtr.mapping.holder;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import org.mtr.mapping.tool.NativeHolder;
import java.awt.image.BufferedImage;
import java.util.Objects;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class NativeImageBackedTexture extends NativeHolder<NativeImageBackedTexture.LegacyTexture> implements AutoCloseable {
    public NativeImageBackedTexture(NativeImage image) { super(new LegacyTexture(image)); }
    public NativeImage getImage() { return data.image; }
    public void upload() { data.upload(); }
    @Override public void close() { data.deleteGlTexture(); }

    public static final class LegacyTexture extends net.minecraft.client.renderer.texture.AbstractTexture {
        private NativeImage image;
        private boolean allocated;
        private LegacyTexture(NativeImage image) { this.image = Objects.requireNonNull(image); }
        @Override public void loadTexture(IResourceManager resources) { upload(); }
        @Override public int getGlTextureId() {
            if (image == null) throw new IllegalStateException("Texture is closed");
            return super.getGlTextureId();
        }
        void upload() {
            if (image == null) throw new IllegalStateException("Texture is closed");
            BufferedImage pixels = image.data.getImage();
            if (allocated) TextureUtil.uploadTextureImageSub(getGlTextureId(), pixels, 0, 0, false, false);
            else {
                TextureUtil.uploadTextureImageAllocate(getGlTextureId(), pixels, false, false);
                allocated = true;
            }
        }
        @Override public void deleteGlTexture() {
            try { super.deleteGlTexture(); }
            finally {
                allocated = false;
                NativeImage previous = image; image = null;
                if (previous != null) previous.close();
            }
        }
    }
}
