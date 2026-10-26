package org.mtr.mapping;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacyCamera;
import static org.junit.jupiter.api.Assertions.*;

public class CameraCompatibilityScenarios {
    public void cloudFogDoesNotOverwriteCameraAndEachStereoEyeCanCapture() throws Exception {
        int previousEye = net.minecraft.client.renderer.EntityRenderer.anaglyphField;
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            net.minecraft.entity.player.EntityPlayer view = MtrEscalatorScenarios.player(new BlockPlacementScenarios.MemoryWorld());
            view.lastTickPosX = view.posX = 10; view.lastTickPosY = view.posY = 150; view.lastTickPosZ = view.posZ = 20;
            fixture.client.renderViewEntity = view;
            org.mtr.mapping.registry.LegacyClientEvents.init();
            startFrame();
            Matrix4f clean = new Matrix4f();
            Matrix4f.rotate((float)(Math.PI / 6), new Vector3f(1, 0, 0), clean, clean);
            Matrix4f.rotate((float)Math.PI, new Vector3f(0, 1, 0), clean, clean);
            Matrix4f.translate(new Vector3f(-3, -2, -4), clean, clean);
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, clean);
            Camera camera = MinecraftClient.getInstance().getGameRendererMapped().getCamera();
            Matrix4f clouds = new Matrix4f(clean);
            Matrix4f.scale(new Vector3f(12, 1, 12), clouds, clouds);
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, clouds);
            assertEquals(30, camera.getPitch(), 0.0001);
            assertEquals(13, camera.getPos().getXMapped(), 0.00001);
            assertEquals(24, camera.getPos().getZMapped(), 0.00001);
            fixture.client.gameSettings.anaglyph = true;
            startFrame();
            net.minecraft.client.renderer.EntityRenderer.anaglyphField = 0;
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, clean);
            net.minecraft.client.renderer.EntityRenderer.anaglyphField = 1;
            Matrix4f secondEye = new Matrix4f();
            Matrix4f.translate(new Vector3f(-1, -2, -4), secondEye, secondEye);
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, secondEye);
            assertEquals(180, Math.abs(camera.getYaw()), 0.0001);
            assertEquals(11, camera.getPos().getXMapped(), 0.00001);
            startFrame();
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, clean);
            assertEquals(30, camera.getPitch(), 0.0001);
        } finally { net.minecraft.client.renderer.EntityRenderer.anaglyphField = previousEye; }
    }
    private static void startFrame() {
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().post(new cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent(
            cpw.mods.fml.common.gameevent.TickEvent.Phase.START, 0.5F));
    }
    public void cameraUsesInverseNativeViewMatrixAndInterpolatedWorldOrigin() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            org.mtr.mapping.registry.LegacyClientEvents.init(); startFrame();
            BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
            net.minecraft.entity.player.EntityPlayer view = MtrEscalatorScenarios.player(world);
            fixture.client.renderViewEntity = view;
            view.lastTickPosX = -20; view.lastTickPosY = 66; view.lastTickPosZ = 4;
            view.prevPosX = 100; view.prevPosY = 100; view.prevPosZ = 100;
            view.posX = -16; view.posY = 74; view.posZ = 6;
            Matrix4f matrix = new Matrix4f();
            Matrix4f.rotate((float)Math.PI, new Vector3f(0, 1, 0), matrix, matrix);
            Matrix4f.translate(new Vector3f(-3.25F, -2.5F, -4.25F), matrix, matrix);
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, matrix);
            Camera camera = MinecraftClient.getInstance().getGameRendererMapped().getCamera();
            assertEquals(-14.75, camera.getPos().getXMapped(), 0.00001);
            assertEquals(72.5, camera.getPos().getYMapped(), 0.00001);
            assertEquals(9.25, camera.getPos().getZMapped(), 0.00001);
            assertEquals(0, camera.getYaw(), 0.0001); assertEquals(0, camera.getPitch(), 0.0001);
            assertEquals(new BlockPos(-15, 72, 9), camera.getBlockPos());
            assertFalse(camera.isThirdPerson());
            fixture.client.gameSettings.thirdPersonView = 2;
            startFrame();
            Matrix4f front = new Matrix4f();
            Matrix4f.translate(new Vector3f(0, -2, -4), front, front);
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, front);
            assertEquals(180, Math.abs(camera.getYaw()), 0.0001);
            assertTrue(camera.isThirdPerson());
            assertEquals(-18, camera.getPos().getXMapped(), 0.00001);
            assertEquals(9, camera.getPos().getZMapped(), 0.00001);
            Matrix4f pitched = new Matrix4f();
            startFrame();
            Matrix4f.rotate((float)(Math.PI / 6), new Vector3f(1, 0, 0), pitched, pitched);
            Matrix4f.rotate((float)Math.PI, new Vector3f(0, 1, 0), pitched, pitched);
            LegacyCamera.updateFromViewMatrix(fixture.client, 0.5F, pitched);
            assertEquals(30, camera.getPitch(), 0.0001);
            assertEquals(0, camera.getYaw(), 0.0001);
            fixture.client.renderViewEntity = null;
            assertEquals(0, camera.getPos().squaredDistanceTo(new Vector3d(0, 0, 0)));
        }
    }
}
