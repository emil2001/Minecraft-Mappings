package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class Frustum extends HolderBase<net.minecraft.client.renderer.culling.ICamera> {
    public Frustum(net.minecraft.client.renderer.culling.ICamera data) { super(data); }
    public boolean isVisible(Box box) { return data.isBoundingBoxInFrustum(box.data); }
}
