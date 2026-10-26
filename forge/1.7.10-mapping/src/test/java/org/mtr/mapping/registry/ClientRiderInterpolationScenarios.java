package org.mtr.mapping.registry;

import java.lang.reflect.Field;
import org.lwjgl.util.vector.Matrix4f;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacyCamera;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderVehicles;
import static org.junit.jupiter.api.Assertions.*;

public final class ClientRiderInterpolationScenarios {
    public void outsideObserverKeepsAbsoluteLiftPoseWithoutPassengerCompensation() throws Exception {
        try (ClientRidingGeometryScenarios.Fixture f = new ClientRidingGeometryScenarios.Fixture()) {
            assertNull(VehicleRidingMovement.getRidingVehicleCarNumberAndOffset(88));
            PositionAndRotation absolute = new PositionAndRotation(new Vector(100, 64.5, 200), .25, 0);
            PositionAndRotation rendered = RenderVehicles.getRenderPositionAndRotation(null, null, null, absolute, new Vector3d(5, -12, 7));
            assertEquals(100, rendered.position.x, 0); assertEquals(64.5, rendered.position.y, 0); assertEquals(200, rendered.position.z, 0);
            assertEquals(.25, rendered.yaw, 0);
        }
    }

    public void stationaryPassengerCameraRemainsFixedToMovingLift(double speed, int fps) throws Exception {
        try (ClientRidingGeometryScenarios.Fixture f = new ClientRidingGeometryScenarios.Fixture()) {
            net.minecraft.client.entity.EntityClientPlayerMP player = f.client.client.thePlayer;
            f.client.client.renderViewEntity = player;
            move(50, 77, 64);
            snapshotTick(player);
            int previousTick = 0;
            for (int frame = 1; frame <= 24; frame++) {
                double seconds = (frame - 1) / (double) fps;
                int tick = (int) Math.floor(seconds * 20);
                if (tick != previousTick) {
                    snapshotTick(player);
                    Field scheduled = InitClient.class.getDeclaredField("movePlayer"); scheduled.setAccessible(true);
                    Runnable moveAtTickEnd = (Runnable) scheduled.get(null);
                    if (moveAtTickEnd != null) { moveAtTickEnd.run(); scheduled.set(null, null); }
                    previousTick = tick;
                }
                VehicleRidingMovement.tick();
                PositionAndRotation lift = new PositionAndRotation(new Vector(100, 64 + speed * frame / fps, 200), 0, 0);
                Vector3d proxyOffset = new Vector3d(97, 70, 205);
                // Native camera setup precedes MainRenderer and its next frame-driven passenger move.
                for (float partial : new float[]{0, .25F, .5F, .75F, 1}) {
                    LegacyCamera.reset();
                    LegacyClientEvents.FRAME.beginFrame(frame * 1_000_000_000L / fps, partial);
                    LegacyCamera.updateFromViewMatrix(f.client.client, partial, new Matrix4f());
                    double sampledCameraY = LegacyCamera.getState(f.client.client).y;
                    Vector3d relativePassenger = VehicleRidingMovement.getRidingVehicleCarNumberAndOffset(77).right().left();
                    PositionAndRotation rendered = RenderVehicles.getRenderPositionAndRotation(relativePassenger, null, lift, lift,
                        new ClientPlayerEntity(player).getPos().subtract(proxyOffset));
                    double floorRelativeToCamera = rendered.position.y + 1 + proxyOffset.getYMapped() - sampledCameraY;
                    assertEquals(-1.62F, floorRelativeToCamera, 1E-6,
                        "Stationary rider: speed=" + speed + ", fps=" + fps + ", frame=" + frame + ", partial=" + partial);
                }
                double sampledCameraY = LegacyCamera.getState(f.client.client).y;
                move(Math.round(1000D / fps), 77, lift.position.y);
                assertEquals(sampledCameraY, LegacyCamera.getState(f.client.client).y, 0,
                    "Moving the passenger later in the render pass must not change the already sampled camera");
                assertEquals(65 + speed * frame / fps, player.boundingBox.minY, 1E-6);
            }
        } finally { LegacyCamera.reset(); }
    }

    public void walkingAndDismountKeepTheirExistingMovementContracts() throws Exception {
        try (ClientRidingGeometryScenarios.Fixture f = new ClientRidingGeometryScenarios.Fixture()) {
            net.minecraft.client.entity.EntityClientPlayerMP player = f.client.client.thePlayer;
            player.moveForward = 1;
            move(50, 77, 64);
            assertEquals(200.25, player.posZ, 1E-6);
            assertEquals(65, player.boundingBox.minY, 1E-6);
            VehicleRidingMovement.movePlayer(50, 77, 0, new ObjectArrayList<>(), null, null, null,
                new PositionAndRotation(new Vector(100, 64, 200), 0, 0));
            assertFalse(VehicleRidingMovement.isRiding(77));
            double x = player.posX, y = player.posY, z = player.posZ;
            player.motionY = -.0784;
            move(50, 77, 90);
            assertEquals(x, player.posX, 0); assertEquals(y, player.posY, 0); assertEquals(z, player.posZ, 0);
            assertEquals(-.0784, player.motionY, 0, "An unmounted player retains vanilla gravity");
            player.lastTickPosY = player.prevPosY = y - 1;
            LegacyCamera.reset(); f.client.client.renderViewEntity = player;
            LegacyClientEvents.FRAME.beginFrame(1_000_000_000L, .25F);
            assertEquals(y - .75, LegacyCamera.getState(f.client.client).y, 1E-6,
                "Ordinary movement after dismount retains native tick interpolation");
        } finally { LegacyCamera.reset(); }
    }

    private static void move(long millis, long vehicle, double y) {
        ObjectArrayList<ObjectBooleanImmutablePair<Box>> floors = new ObjectArrayList<>();
        floors.add(new ObjectBooleanImmutablePair<>(new Box(-2, 1, -5, 2, 1, 5), true));
        VehicleRidingMovement.movePlayer(millis, vehicle, 0, floors, null, null, null,
            new PositionAndRotation(new Vector(100, y, 200), 0, 0));
    }
    private static void snapshotTick(net.minecraft.entity.Entity player) {
        player.prevPosX = player.lastTickPosX = player.posX;
        player.prevPosY = player.lastTickPosY = player.posY;
        player.prevPosZ = player.lastTickPosZ = player.posZ;
    }
}
