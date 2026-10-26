package org.mtr.mapping.holder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class EntityModelAbstractMapping<T extends EntityAbstractMapping> extends net.minecraft.client.model.ModelBase {
    public abstract void setAngles2(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch);
    @Override @SuppressWarnings("unchecked")
    public final void setRotationAngles(float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch,
            float scale, net.minecraft.entity.Entity entity) {
        setAngles2((T) entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
    }
}
