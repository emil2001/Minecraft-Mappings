package org.mtr.mapping.mapper;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Frustum;
import org.mtr.mapping.holder.Identifier;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class EntityRenderer<T extends EntityExtension> extends net.minecraft.client.renderer.entity.Render {
    public EntityRenderer(Argument argument) { setRenderManager(argument.manager); }
    @Override @SuppressWarnings("unchecked")
    public final void doRender(net.minecraft.entity.Entity entity, double x, double y, double z, float yaw, float partialTick) {
        int light = (int)OpenGlHelper.lastBrightnessX | ((int)OpenGlHelper.lastBrightnessY << 16);
        GraphicsHolder.createInstanceSafe(graphics -> {
            graphics.translate(x,y,z); render((T)entity,yaw,partialTick,graphics,light);
        });
    }
    public abstract void render(T entity, float yaw, float partialTick, GraphicsHolder graphics, int light);
    public abstract Identifier getTexture2(T entity);
    @Override @SuppressWarnings("unchecked")
    protected final ResourceLocation getEntityTexture(net.minecraft.entity.Entity entity) { return getTexture2((T)entity).data; }
    public boolean shouldRender2(T entity, Frustum frustum, double x, double y, double z) {
        return entity.isInRangeToRenderDist(entity.getDistanceSq(x,y,z)) && frustum.data.isBoundingBoxInFrustum(entity.boundingBox);
    }
    public static final class Argument {
        private final RenderManager manager;
        public Argument(RenderManager manager) { this.manager=manager; }
    }
}
