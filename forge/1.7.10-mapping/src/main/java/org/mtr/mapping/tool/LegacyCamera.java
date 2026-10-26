package org.mtr.mapping.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.mtr.mapping.registry.LegacyClientEvents;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyCamera {
    private static final FloatBuffer MODEL_VIEW = BufferUtils.createFloatBuffer(16);
    private static WeakReference<EntityLivingBase> capturedView = new WeakReference<>(null);
    private static State captured;
    private static int capturedEyes;
    private LegacyCamera() {}
    public static final class State {
        public final double x, y, z;
        public final float yaw, pitch;
        private State(double x, double y, double z, float yaw, float pitch) {
            this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }
    public static void reset() { capturedView.clear(); captured = null; capturedEyes = 0; }
    public static void beginFrame() { capturedEyes = 0; }
    private static int eyeMask(Minecraft client) {
        return 1 << (client.gameSettings.anaglyph ? net.minecraft.client.renderer.EntityRenderer.anaglyphField : 0);
    }
    private static boolean alreadyCaptured(Minecraft client) {
        return capturedView.get() == client.renderViewEntity && (capturedEyes & eyeMask(client)) != 0;
    }
    public static void capture(float partialTick) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.renderViewEntity == null) { reset(); return; }
        if (alreadyCaptured(client)) return;
        MODEL_VIEW.clear(); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW);
        Matrix4f matrix = new Matrix4f(); matrix.load(MODEL_VIEW);
        updateFromViewMatrix(client, partialTick, matrix);
    }
    public static void updateFromViewMatrix(Minecraft client, float partialTick, Matrix4f modelView) {
        EntityLivingBase view = client.renderViewEntity;
        if (view == null) { reset(); return; }
        // Only the first fog callback per eye has the camera matrix; later callbacks can carry cloud scaling.
        if (alreadyCaptured(client)) return;
        Matrix4f inverse = Matrix4f.invert(modelView, null);
        if (inverse == null) throw new IllegalArgumentException("Singular native camera matrix");
        double length = Math.sqrt(inverse.m20 * inverse.m20 + inverse.m21 * inverse.m21 + inverse.m22 * inverse.m22);
        // Native scene geometry uses interpolated pos as its origin; inverse translation includes eye offset, bob and third-person collision.
        captured = new State(interpolate(view.lastTickPosX, view.posX, partialTick) + inverse.m30,
            interpolate(view.lastTickPosY, view.posY, partialTick) + inverse.m31,
            interpolate(view.lastTickPosZ, view.posZ, partialTick) + inverse.m32,
            (float)Math.toDegrees(Math.atan2(inverse.m20, -inverse.m22)),
            (float)Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, inverse.m21 / length)))));
        capturedView = new WeakReference<>(view);
        capturedEyes |= eyeMask(client);
    }
    public static State getState(Minecraft client) {
        EntityLivingBase view = client.renderViewEntity;
        if (view == null) return new State(0, 0, 0, 0, 0);
        if (captured != null && capturedView.get() == view) return captured;
        float partial = LegacyClientEvents.FRAME.getPartialTick();
        boolean front = client.gameSettings.thirdPersonView == 2;
        return new State(interpolate(view.lastTickPosX, view.posX, partial),
            interpolate(view.lastTickPosY, view.posY, partial) - view.yOffset + 1.62F,
            interpolate(view.lastTickPosZ, view.posZ, partial),
            view.prevRotationYaw + (view.rotationYaw - view.prevRotationYaw) * partial + (front ? 180 : 0),
            (view.prevRotationPitch + (view.rotationPitch - view.prevRotationPitch) * partial) * (front ? -1 : 1));
    }
    private static double interpolate(double previous, double current, float partial) { return previous + (current - previous) * partial; }
}
