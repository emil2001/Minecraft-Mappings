package org.mtr.mapping.registry;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import java.util.IdentityHashMap;
import java.util.Map;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyBlockEntityRenderers {
    private static final Map<BlockEntityType<?>,BlockEntityRenderer<?>> RENDERERS=new IdentityHashMap<>();
    private static boolean installed;
    private LegacyBlockEntityRenderers() {}
    @SuppressWarnings("unchecked") public static <T extends BlockEntityExtension> void register(BlockEntityType<T> type, BlockEntityRenderer<T> renderer) {
        if (RENDERERS.containsKey(type)) throw new IllegalStateException("Duplicate block entity renderer");
        RENDERERS.put(type,renderer);
        if (!installed) {
            cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(BlockEntityExtension.class,new TileEntitySpecialRenderer() {
                @Override public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float delta) {
                    BlockEntityRenderer<?> selected=RENDERERS.get(((BlockEntityExtension)tile).getType2());
                    if (selected!=null) selected.renderTileEntityAt(tile,x,y,z,delta);
                }
            });
            Map<Class<?>,TileEntitySpecialRenderer> nativeRenderers=TileEntityRendererDispatcher.instance.mapSpecialRenderers;
            nativeRenderers.entrySet().removeIf(entry -> entry.getValue()==null && BlockEntityExtension.class.isAssignableFrom(entry.getKey()));
            installed=true;
        }
        type.hasClientRenderer=true;
    }
    @SuppressWarnings("unchecked") public static AxisAlignedBB bounds(BlockEntityExtension entity) {
        BlockEntityRenderer<BlockEntityExtension> renderer=(BlockEntityRenderer<BlockEntityExtension>)RENDERERS.get(entity.getType2());
        return renderer!=null && renderer.rendersOutsideBoundingBox2(entity) ? TileEntity.INFINITE_EXTENT_AABB
            : AxisAlignedBB.getBoundingBox(entity.xCoord,entity.yCoord,entity.zCoord,entity.xCoord+1,entity.yCoord+1,entity.zCoord+1);
    }
}
