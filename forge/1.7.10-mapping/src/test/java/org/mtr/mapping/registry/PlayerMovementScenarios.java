package org.mtr.mapping.registry;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import static org.junit.jupiter.api.Assertions.*;

public final class PlayerMovementScenarios {
    public void nativeRiderGravityAndMovementGuardsRestoreOnDismount() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            fixture.player.theItemInWorldManager.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            fixture.player.setPosition(0,100,0);
            fixture.player.noClip=true;
            fixture.player.motionY=.4;
            authorize(fixture);
            player.setNoGravity(true);
            fixture.player.moveEntityWithHeading(0,0);
            assertEquals(.4*.9800000190734863,fixture.player.motionY,1E-8,"Rider gravity must reach the native living-entity physics path");
            player.setNoGravity(false);
            fixture.player.motionY=.4;
            fixture.player.moveEntityWithHeading(0,0);
            assertEquals((.4-.08)*.9800000190734863,fixture.player.motionY,1E-8);

            Handler handler=new Handler(fixture);
            fixture.player.setPosition(0,100,0);
            handler.processPlayer(position(20,100,0));
            assertEquals(1,handler.corrections,"Ordinary movement retains the native speed limit");
            handler.processPlayer(position(0,100,0)); // Acknowledge the ordinary correction before boarding.
            player.setNoGravity(true);
            player.setInTeleportationState(true);
            handler.corrections=0;
            handler.processPlayer(position(20,100,0));
            assertEquals(0,handler.corrections,"The server's riding state permits the vehicle's movement");
            assertEquals(20,fixture.player.posX,0);
            for (int i=0;i<90;i++) handler.processPlayer(position(20,100,0));
            assertNull(handler.kick,"A hovering rider must not be kicked for flying");
            assertFalse(fixture.player.capabilities.allowFlying,"Riding must not grant flight capability");
            player.setInTeleportationState(false);
            player.setNoGravity(false);
            fixture.player.noClip=false;
            handler.processPlayer(position(40,100,0));
            assertEquals(1,handler.corrections,"Dismount restores native movement validation");
        }
    }

    public void movementRejectsNonFinitePositionsAndAnglesEvenDuringRiding() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            authorize(fixture);
            player.setNoGravity(true); player.setInTeleportationState(true);
            Handler handler=new Handler(fixture);
            handler.processPlayer(position(Double.NaN,100,0));
            assertNotNull(handler.kick);
            handler.kick=null;
            handler.processPlayer(new C03PacketPlayer.C05PacketPlayerLook(Float.POSITIVE_INFINITY,0,false));
            assertNotNull(handler.kick);
            assertTrue(Double.isFinite(fixture.player.posX));
        }
    }

    private static C03PacketPlayer position(double x,double y,double z) {
        return new C03PacketPlayer.C04PacketPlayerPosition(x,y,y+1.62,z,false);
    }
    private static void authorize(ServerLifecycleScenarios.ServerFixture fixture) {
        org.mtr.mapping.tool.PlayerPhysics.authorize(fixture.player,java.util.Collections.singletonList(
            net.minecraft.util.AxisAlignedBB.getBoundingBox(-2,95,-2,25,110,2)),fixture.world.getTotalWorldTime()+60);
    }
    public void ridingNeedsFreshServerGeometryAndCannotMoveOutsideTheSelectedCar() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            fixture.player.setPosition(0,100,0);
            ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            Handler handler=new Handler(fixture);
            player.setNoGravity(true); player.setInTeleportationState(true); player.setNoClipMapped(true);
            assertFalse(fixture.player.noClip,"An unchecked ride notification cannot grant collision bypass");
            handler.processPlayer(position(20,100,0));
            assertEquals(1,handler.corrections);
            handler.processPlayer(position(0,100,0));
            authorize(fixture);
            player.setNoGravity(true); player.setInTeleportationState(true); player.setNoClipMapped(true);
            handler.processPlayer(position(20,100,0));
            assertEquals(20,fixture.player.posX,0);
            handler.processPlayer(position(1000,100,0));
            assertEquals(20,fixture.player.posX,0,"A rider cannot teleport outside authoritative vehicle geometry");
            assertFalse(fixture.player.noClip);
            handler.processPlayer(position(20,100,0));
            authorize(fixture);
            player.setInTeleportationState(true); player.setNoClipMapped(true);
            fixture.world.getWorldInfo().incrementTotalWorldTime(61);
            player.setNoClipMapped(true);
            assertFalse(fixture.player.noClip,"Without fresh server snapshots the lease must expire");
            handler.processPlayer(position(40,100,0));
            assertEquals(20,fixture.player.posX,0);
        }
    }
    private static final class Handler extends NetHandlerPlayServer {
        int corrections; String kick;
        Handler(ServerLifecycleScenarios.ServerFixture fixture) { super(fixture.server,new NetworkManager(false),fixture.player); }
        @Override public void sendPacket(Packet packet) {}
        @Override public void setPlayerLocation(double x,double y,double z,float yaw,float pitch) { corrections++; super.setPlayerLocation(x,y,z,yaw,pitch); }
        @Override public void kickPlayerFromServer(String reason) { kick=reason; }
    }
}
