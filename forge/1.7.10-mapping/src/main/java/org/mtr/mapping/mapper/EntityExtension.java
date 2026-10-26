package org.mtr.mapping.mapper;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;

public abstract class EntityExtension extends EntityAbstractMapping {
    private final EntityType<?> type;
    public EntityExtension(EntityType<?> type, World world) {
        super(world.data); this.type = java.util.Objects.requireNonNull(type);
        setSize(type.data.width,type.data.height); type.data.bind(getClass());
    }
    public final EntityType<?> getType2() { return type; }
    @Override protected final void entityInit() { initDataTracker2(); }
    protected abstract void initDataTracker2();
    @Override public final void onUpdate() { tick2(); }
    public void tick2() { super.onUpdate(); }
    @Override public boolean isInRangeToRender3d(double x, double y, double z) {
        if (worldObj.isRemote && type.data.visibility != null) {
            ignoreFrustumCheck=true;
            return type.data.visibility.shouldRender(this,x,y,z);
        }
        return super.isInRangeToRender3d(x,y,z);
    }
    @Override protected final void readEntityFromNBT(NBTTagCompound tag) {}
    @Override protected final void writeEntityToNBT(NBTTagCompound tag) {}
    public void setPosition2(double x, double y, double z) { super.setPosition(x,y+yOffset-ySize,z); }
}
