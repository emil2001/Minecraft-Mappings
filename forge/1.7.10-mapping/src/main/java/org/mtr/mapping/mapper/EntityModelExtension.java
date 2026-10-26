package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.EntityAbstractMapping;
import org.mtr.mapping.holder.EntityModelAbstractMapping;
import java.util.ArrayList;
import java.util.List;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class EntityModelExtension<T extends EntityAbstractMapping> extends EntityModelAbstractMapping<T> implements ModelHelper {
    private final List<ModelPartExtension> parts = new ArrayList<>();
    public EntityModelExtension(int textureWidth, int textureHeight) { this.textureWidth = textureWidth; this.textureHeight = textureHeight; }
    public final ModelPartExtension createModelPart() {
        ModelPartExtension part = new ModelPartExtension(this); parts.add(part); return part;
    }
    public final void buildModel() { parts.forEach(ModelPartExtension::bake); }
}
