package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

public final class LivingEntity extends NativeHolder<net.minecraft.entity.EntityLivingBase> {
    public LivingEntity(net.minecraft.entity.EntityLivingBase data) { super(data); }
    public static LivingEntity cast(HolderBase<?> holder) { return new LivingEntity((net.minecraft.entity.EntityLivingBase) holder.data); }
    public static boolean isInstance(HolderBase<?> holder) { return holder.data instanceof net.minecraft.entity.EntityLivingBase; }
}
