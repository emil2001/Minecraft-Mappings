package org.mtr.mapping.mapper;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.mtr.mapping.holder.Vector3d;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class BlockEntityRenderer<T extends BlockEntityExtension> extends TileEntitySpecialRenderer {
    public BlockEntityRenderer(Argument argument) { func_147497_a(argument.dispatcher); }
    @Override @SuppressWarnings("unchecked") public final void renderTileEntityAt(TileEntity tile, double x, double y, double z, float delta) {
        T entity=(T)tile;
        if (!isInRenderDistance(entity,new Vector3d(field_147501_a.field_147560_j,field_147501_a.field_147561_k,field_147501_a.field_147558_l))) return;
        int light=(int)OpenGlHelper.lastBrightnessX | (int)OpenGlHelper.lastBrightnessY<<16;
        GraphicsHolder.createInstanceSafe(graphics -> { graphics.translate(x,y,z); render(entity,delta,graphics,light,0); });
    }
    public abstract void render(T entity, float delta, GraphicsHolder graphics, int light, int overlay);
    public boolean rendersOutsideBoundingBox2(T entity) { return false; }
    public int getRenderDistance2() { return 64; }
    public boolean isInRenderDistance(T entity, Vector3d position) {
        double x=entity.xCoord+.5-position.getXMapped(), y=entity.yCoord+.5-position.getYMapped(), z=entity.zCoord+.5-position.getZMapped();
        return x*x+y*y+z*z<(double)getRenderDistance2()*getRenderDistance2();
    }
    public static final class Argument {
        private final TileEntityRendererDispatcher dispatcher;
        public Argument(TileEntityRendererDispatcher dispatcher) { this.dispatcher=dispatcher; }
    }
}
