package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.ScreenExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class AddonHolderCompatibilityScenarios {
    public void outlineEnvelopeKeepsAllPartsAndRejectsEmptyBounds() throws Throwable {
        VoxelShape shape = VoxelShapes.union(VoxelShapes.cuboid(.125, 0, .25, .5, .75, .75),
            VoxelShapes.cuboid(.5, .25, .125, .875, .5, .875));
        assertFalse((Boolean) call(shape, "isEmpty", new Class<?>[0]));
        Box bounds = (Box) call(shape, "getBoundingBox", new Class<?>[0]);
        assertEquals(new Box(.125, 0, .125, .875, .75, .875), bounds);
        Vector3d center = (Vector3d) call(bounds, "getCenter", new Class<?>[0]);
        assertEquals(.5, center.getXMapped(), 1e-9);
        assertEquals(.375, center.getYMapped(), 1e-9);
        assertEquals(.5, center.getZMapped(), 1e-9);
        assertTrue((Boolean) call(VoxelShapes.empty(), "isEmpty", new Class<?>[0]));
        assertThrows(UnsupportedOperationException.class, () -> call(VoxelShapes.empty(), "getBoundingBox", new Class<?>[0]));
    }

    public void mappedOutlineAndNativeSlabBoundsStayInLocalUnits() throws Throwable {
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        BlockView view = new BlockView(nativeWorld);
        BlockPos pos = new BlockPos(20, 64, -19);
        BlockExtension panel = new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
            @Override public VoxelShape getOutlineShape2(BlockState state, BlockView blockView, BlockPos blockPos, ShapeContext context) {
                return VoxelShapes.union(VoxelShapes.cuboid(.125, 0, 0, .25, .5, 1), VoxelShapes.cuboid(.5, .25, .25, .75, .75, .5));
            }
            @Override public VoxelShape getCollisionShape2(BlockState state, BlockView blockView, BlockPos blockPos, ShapeContext context) {
                return VoxelShapes.empty();
            }
        };
        BlockPlacementScenarios.registerProbe("addon_outline_panel", panel);
        world.setBlockState(pos, panel.getDefaultState2());
        assertEquals(new Box(.125, 0, 0, .75, .75, 1), outlineBounds(world.getBlockState(pos), view, pos));

        nativeWorld.setBlock(pos.getX(), pos.getY(), pos.getZ(), net.minecraft.init.Blocks.stone_slab, 8, 0);
        assertEquals(new Box(0, .5, 0, 1, 1, 1), outlineBounds(world.getBlockState(pos), view, pos));
        nativeWorld.setBlockMetadataWithNotify(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        assertEquals(new Box(0, 0, 0, 1, .5, 1), outlineBounds(world.getBlockState(pos), view, pos));
        VoxelShape air = (VoxelShape) call(new Block(net.minecraft.init.Blocks.air).getDefaultState(), "getOutlineShape",
            new Class<?>[]{BlockView.class, BlockPos.class}, view, pos);
        assertTrue((Boolean) call(air, "isEmpty", new Class<?>[0]));
    }

    public void pngExportPreservesChannelsAndClosedImageCannotTruncateFiles() throws Throwable {
        Path path = Files.createTempFile("mtr-addon-image-", ".png");
        try {
            NativeImage image = new NativeImage(3, 1, false);
            try {
                image.setPixelColor(0, 0, 0x80332211);
                image.setPixelColor(1, 0, 0xff070809);
                image.setPixelColor(2, 0, 0x00123456);
                call(image, "writeTo", new Class<?>[]{File.class}, path.toFile());
                BufferedImage png = ImageIO.read(path.toFile());
                assertEquals(0x80112233, png.getRGB(0, 0));
                assertEquals(0xff090807, png.getRGB(1, 0));
                assertEquals(0x00563412, png.getRGB(2, 0));
                png.flush();
                try (FileInputStream input = new FileInputStream(path.toFile()); NativeImage restored = NativeImage.read(input)) {
                    assertEquals(3, restored.getWidth());
                    assertEquals(1, restored.getHeight());
                    assertEquals(0x80332211, restored.getColor(0, 0));
                    assertEquals(0xff070809, restored.getColor(1, 0));
                    assertEquals(0x00123456, restored.getColor(2, 0));
                }
            } finally { image.close(); }
            byte[] exported = Files.readAllBytes(path);
            assertThrows(IllegalStateException.class, () -> call(image, "writeTo", new Class<?>[]{File.class}, path.toFile()));
            assertArrayEquals(exported, Files.readAllBytes(path), "Closed image export must fail before opening the destination");
        } finally { Files.deleteIfExists(path); }
    }

    public void cameraInterpolationUsesRenderViewEntityAndChunkChecksDoNotLoad() throws Throwable {
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        assertFalse((Boolean) call(world, "isChunkLoaded", new Class<?>[]{int.class, int.class}, -2, 3));
        assertEquals(0, nativeWorld.getChunkProvider().getLoadedChunkCount());
        nativeWorld.getChunkFromChunkCoords(-2, 3);
        assertTrue((Boolean) call(world, "isChunkLoaded", new Class<?>[]{int.class, int.class}, -2, 3));
        assertEquals(1, nativeWorld.getChunkProvider().getLoadedChunkCount());

        try (NativeClientFixture fixture = new NativeClientFixture()) {
            MinecraftClient client = new MinecraftClient(fixture.client);
            assertNull(call(client, "getCameraEntityMapped", new Class<?>[0]));
            net.minecraft.entity.passive.EntityPig camera = new net.minecraft.entity.passive.EntityPig(nativeWorld);
            camera.prevRotationYaw = -20; camera.rotationYaw = 60;
            camera.prevRotationPitch = 10; camera.rotationPitch = -30;
            fixture.client.renderViewEntity = camera;
            Entity entity = (Entity) call(client, "getCameraEntityMapped", new Class<?>[0]);
            assertSame(camera, entity.data);
            assertEquals(0F, (Float) call(entity, "getYaw", new Class<?>[]{float.class}, .25F));
            assertEquals(0F, (Float) call(entity, "getPitch", new Class<?>[]{float.class}, .25F));
            assertEquals(-20F, (Float) call(entity, "getYaw", new Class<?>[]{float.class}, 0F));
            assertEquals(-30F, (Float) call(entity, "getPitch", new Class<?>[]{float.class}, 1F));
        }
    }

    public void clearingScreenFocusStopsKeyboardAndDragDispatch() throws Throwable {
        ScreenExtension screen = new ScreenExtension() {};
        int[] calls = new int[2];
        ClickableWidgetExtension child = new ClickableWidgetExtension(0, 0, 20, 20) {
            @Override public boolean mouseClicked2(double x, double y, int button) { return button == 0 && isMouseOver2(x, y); }
            @Override protected void onDrag2(double x, double y, double dx, double dy) { calls[0]++; }
            @Override public boolean charTyped2(char character, int modifiers) { calls[1]++; return true; }
        };
        screen.addChild(new ClickableWidget(child));
        assertTrue(screen.mouseClicked2(5, 5, 0));
        assertTrue(child.isFocused2());
        assertTrue(screen.mouseDragged2(6, 6, 0, 1, 1));
        assertTrue(screen.charTyped2('a', 0));
        call(screen, "setFocused", new Class<?>[]{ClickableWidget.class}, (Object) null);
        assertFalse(child.isFocused2());
        assertFalse(screen.mouseDragged2(7, 7, 0, 1, 1));
        assertFalse(screen.charTyped2('b', 0));
        assertArrayEquals(new int[]{1, 1}, calls);
    }

    private static Box outlineBounds(BlockState state, BlockView world, BlockPos pos) throws Throwable {
        VoxelShape shape = (VoxelShape) call(state, "getOutlineShape", new Class<?>[]{BlockView.class, BlockPos.class}, world, pos);
        return (Box) call(shape, "getBoundingBox", new Class<?>[0]);
    }

    private static Object call(Object target, String name, Class<?>[] parameters, Object... arguments) throws Throwable {
        try { return target.getClass().getMethod(name, parameters).invoke(target, arguments); }
        catch (InvocationTargetException exception) { throw exception.getCause(); }
    }
}
