package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.registry.*;
import static org.junit.jupiter.api.Assertions.*;

public final class TileRendererCompatibilityScenarios {
    public static final class Panel extends BlockEntityExtension {
        static int constructed;
        Panel(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); constructed++; }
    }
    private static final class PanelRenderer extends BlockEntityRenderer<Panel> {
        final int color; final boolean outside; int renders;
        PanelRenderer(Argument argument, int color, boolean outside) { super(argument); this.color=color; this.outside=outside; }
        @Override public void render(Panel panel, float delta, GraphicsHolder graphics, int light, int overlay) {
            renders++; assertEquals(0,overlay); assertEquals(.25F,delta);
            GuiDrawing drawing=new GuiDrawing(graphics); drawing.beginDrawingRectangle();
            drawing.drawRectangle(-2,-2,2,2,color); drawing.finishDrawingRectangle();
        }
        @Override public boolean rendersOutsideBoundingBox2(Panel panel) { return outside; }
        @Override public boolean isInRenderDistance(Panel panel, Vector3d camera) { return outside || super.isInRenderDistance(panel,camera); }
    }
    @SuppressWarnings("unchecked") public static void run(Minecraft client) {
        Registry registry=new Registry();
        BlockRegistryObject block=registry.registerBlock(new Identifier("mtr:tile_renderer_probe"),
            () -> new Block(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron))));
        BlockEntityTypeRegistryObject<Panel>[] types=new BlockEntityTypeRegistryObject[2];
        types[0]=registry.registerBlockEntityType(new Identifier("mtr:tile_renderer_a"),(pos,state) -> new Panel(types[0].get(),pos,state),block::get);
        types[1]=registry.registerBlockEntityType(new Identifier("mtr:tile_renderer_b"),(pos,state) -> new Panel(types[1].get(),pos,state),block::get);
        registry.init();
        TileEntityRendererDispatcher dispatcher=TileEntityRendererDispatcher.instance;
        assertNull(dispatcher.getSpecialRendererByClass(Panel.class),"Exercise the native cached-null lookup before registration");
        RegistryClient registrations=new RegistryClient(registry); PanelRenderer[] renderers=new PanelRenderer[2];
        registrations.registerBlockEntityRenderer(types[0],argument -> renderers[0]=new PanelRenderer(argument,0xFFFF0000,true));
        registrations.registerBlockEntityRenderer(types[1],argument -> renderers[1]=new PanelRenderer(argument,0xFF0000FF,false));
        registrations.init(); assertEquals(0,Panel.constructed,"Renderer registration must not create a throwaway tile or world");
        assertThrows(IllegalStateException.class,() -> LegacyBlockEntityRenderers.register(types[0].get(),
            new PanelRenderer(new BlockEntityRenderer.Argument(dispatcher),0xFF00FF00,false)));
        BlockState state=block.get().getDefaultState();
        Panel first=types[0].get().data.apply(new BlockPos(4000,64,0),state);
        Panel second=types[1].get().data.apply(new BlockPos(4001,64,0),state);
        NBTTagCompound tag=new NBTTagCompound(); first.writeToNBT(tag);
        Panel restored=(Panel)TileEntity.createAndLoadEntity(tag);
        assertSame(types[0].get(),restored.getType2());
        BlockPlacementScenarios.MemoryWorld world=new BlockPlacementScenarios.MemoryWorld();
        new World(world).setBlockState(restored.getPos2(),state,2); new World(world).setBlockState(second.getPos2(),state,2);
        restored.setWorldObj(world); second.setWorldObj(world);
        assertNotNull(dispatcher.getSpecialRenderer(restored));
        assertSame(TileEntity.INFINITE_EXTENT_AABB,restored.getRenderBoundingBox());
        assertEquals(4001,second.getRenderBoundingBox().minX); assertEquals(4002,second.getRenderBoundingBox().maxX);
        dispatcher.field_147550_f=world; dispatcher.field_147560_j=0; dispatcher.field_147561_k=0; dispatcher.field_147558_l=0;
        TileEntityRendererDispatcher.staticPlayerX=3980; TileEntityRendererDispatcher.staticPlayerY=34; TileEntityRendererDispatcher.staticPlayerZ=10;
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
        dispatcher.renderTileEntity(restored,.25F);
        assertEquals(1,renderers[0].renders,"The renderer's distance override must survive native 64-block gating");
        assertPixel(20,30,0xFF0000);
        dispatcher.renderTileEntity(second,.25F); assertEquals(0,renderers[1].renders);
        dispatcher.field_147560_j=4001.5; dispatcher.field_147561_k=64.5; dispatcher.field_147558_l=.5;
        TileEntityRendererDispatcher.staticPlayerX=3981;
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT); dispatcher.renderTileEntity(second,.25F);
        assertEquals(1,renderers[1].renders); assertEquals(1,renderers[0].renders);
        assertPixel(20,30,0x0000FF);
        assertTrue(dispatcher.getSpecialRenderer(new net.minecraft.tileentity.TileEntityChest()) instanceof net.minecraft.client.renderer.tileentity.TileEntityChestRenderer);
        System.out.println("Native TESR factory type dispatch, NBT reload, distance/bounds and translated pixels: PASS");
    }
    private static void assertPixel(int x, int y, int rgb) {
        java.nio.ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
        org.lwjgl.opengl.GL11.glReadPixels(x,64-y-1,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixel);
        assertEquals(rgb,(pixel.get(0)&255)<<16 | (pixel.get(1)&255)<<8 | (pixel.get(2)&255));
    }
}
