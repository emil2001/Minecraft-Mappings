package org.mtr.mapping.tool;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Identifier;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.imageio.ImageIO;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyModelSprite extends TextureAtlasSprite {
    private final ResourceLocation source;
    private final int mipLevels;
    private int logicalWidth,logicalHeight;
    public LegacyModelSprite(Identifier texture,int mipLevels) {
        super(texture.toString()); source=new ResourceLocation(texture.getNamespace(),"textures/"+texture.getPath()+".png"); this.mipLevels=mipLevels;
    }
    @Override public boolean hasCustomLoader(IResourceManager manager,ResourceLocation location) { return true; }
    @Override public boolean load(IResourceManager manager,ResourceLocation location) {
        try {
            IResource resource=resource(manager); BufferedImage image;
            try (InputStream stream=resource.getInputStream()) { image=ImageIO.read(stream); }
            if (image==null) throw new IllegalArgumentException("Invalid block texture: "+source);
            AnimationMetadataSection animation=(AnimationMetadataSection)resource.getMetadata("animation");
            logicalWidth=image.getWidth(); logicalHeight=animation==null?image.getHeight():image.getWidth();
            if (animation==null && image.getWidth()!=image.getHeight()) {
                // Native stitching rotates rectangles, which IIcon's independent U/V coordinates cannot express.
                int side=net.minecraft.util.MathHelper.roundUpToPowerOfTwo(Math.max(logicalWidth,logicalHeight));
                BufferedImage padded=new BufferedImage(side,side,BufferedImage.TYPE_INT_ARGB);
                for (int y=0;y<side;y++) for (int x=0;x<side;x++) padded.setRGB(x,y,image.getRGB(Math.min(x,logicalWidth-1),Math.min(y,logicalHeight-1)));
                image=padded;
            }
            BufferedImage[] levels=new BufferedImage[mipLevels+1]; levels[0]=image; loadSprite(levels,animation,false);
            alignInitialFrame(animation);
            return false;
        } catch (Exception exception) {
            logicalWidth=logicalHeight=0; DummyClass.logException(new IllegalArgumentException("Cannot load block texture: "+source,exception)); return true;
        }
    }
    private IResource resource(IResourceManager manager) throws IOException {
        try { return manager.getResource(source); }
        catch (FileNotFoundException exception) {
            if (!source.getResourceDomain().equals("minecraft") || !(source.getResourcePath().startsWith("textures/block/") || source.getResourcePath().startsWith("textures/item/"))) throw exception;
            return manager.getResource(new ResourceLocation("mtr_compat",source.getResourcePath()));
        }
    }
    private void alignInitialFrame(AnimationMetadataSection animation) {
        if (animation==null || animation.getFrameCount()==0) return;
        int first=animation.getFrameIndex(0);
        if (first!=0) {
            // Native TextureMap initially uploads physical frame0, so remap the metadata and pixel slots together.
            Collections.swap(framesTextureData,0,first); List<AnimationFrame> frames=new ArrayList<>();
            for (int i=0;i<animation.getFrameCount();i++) {
                int index=animation.getFrameIndex(i); frames.add(new AnimationFrame(index==0?first:index==first?0:index,animation.frameHasTime(i)?animation.getFrameTimeSingle(i):-1));
            }
            cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(TextureAtlasSprite.class,this,
                new AnimationMetadataSection(frames,animation.getFrameWidth(),animation.getFrameHeight(),animation.getFrameTime()),"animationMetadata","field_110982_k");
        }
        // Native mipmap generation drops null slots; preserve indices for valid animations that skip source frames.
        for (int i=0;i<framesTextureData.size();i++) if (framesTextureData.get(i)==null) framesTextureData.set(i,framesTextureData.get(0));
    }
    @Override public float getMaxU() { return logicalWidth==0?super.getMaxU():super.getMinU()+(super.getMaxU()-super.getMinU())*logicalWidth/width; }
    @Override public float getMaxV() { return logicalHeight==0?super.getMaxV():super.getMinV()+(super.getMaxV()-super.getMinV())*logicalHeight/height; }
    @Override public float getInterpolatedU(double value) { return getMinU()+(getMaxU()-getMinU())*(float)value/16; }
    @Override public float getInterpolatedV(double value) { return getMinV()+(getMaxV()-getMinV())*(float)value/16; }
}
