package org.mtr.mapping.registry;

import org.mtr.gtnh.RidingPermissions;
import org.mtr.core.data.*;
import org.mtr.core.operation.*;
import org.mtr.core.serializer.*;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.tool.PlayerPhysics;
import org.mtr.mod.item.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public final class RidingPermissionScenarios {
    public void freshCoreMembershipKeepsFrozenNativeRiderThroughRefreshAndCurrentCarRecovery() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            fixture.player.setPosition(8.5,5,30);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=boardFrozenRider(core,handler);
                freezeRider(core,train);
                assertEquals(30,fixture.player.posZ,0); assertTrue(fixture.player.noClip); assertEquals(0,PlayerPhysics.gravity(.08,fixture.player),0);
                assertEquals(0,handler.corrections); assertFalse(appliedPassenger(train,core.player.getUuid()).getDoorOverride());
                handler.processPlayer(position(8.5,5,81.2));
                assertEquals(81.2,fixture.player.posZ,1E-6); assertEquals(0,handler.corrections);
                assertTrue(RidingPermissions.refresh(core.player)); assertTrue(PlayerPhysics.authorized(fixture.player));
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }
    public void frozenNativeRiderConsumesNoMotionPacketButCannotMoveAtOldOrArbitraryPosition() throws Exception {
        for(int boundary=0;boundary<3;boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                fixture.player.setPosition(8.5,5,30);
                io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                try {
                    HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=boardFrozenRider(core,handler); freezeRider(core,train);
                    handler.processPlayer(position(boundary==0?8.5:boundary==1?8.50001:1000,5,30));
                    if(boundary==0) {
                        assertEquals(30,fixture.player.posZ,0); assertEquals(8.5,fixture.player.posX,0);
                        assertTrue(PlayerPhysics.authorized(fixture.player)); assertEquals(0,handler.corrections);
                    } else {
                        core.drain(); fixture.world.func_82738_a(82); core.poll();
                        assertFalse(PlayerPhysics.authorized(fixture.player),"A different out-of-current-cabin C03 must end frozen privileges");
                        assertFalse(fixture.player.noClip); assertEquals(.08,PlayerPhysics.gravity(.08,fixture.player),0);
                        assertFalse(RidingPermissions.refresh(core.player));
                        if(boundary==2) assertEquals(8.5,fixture.player.posX,0);
                    }
                } finally { channel.finish(); }
            } finally { RidingPermissions.clear(); }
        }
    }
    public void frozenNativeRiderFailsClosedOnNativeCoreAndLifecycleBoundaries() throws Exception {
        for(int boundary=0;boundary<13;boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                fixture.player.setPosition(8.5,5,30);
                io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                try {
                    HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=boardFrozenRider(core,handler); freezeRider(core,train);
                    if(boundary==0) fixture.player.setPosition(8.50001,5,30);
                    if(boundary==1) { PlayerPhysics.revoke(fixture.player); }
                    if(boundary==2) fixture.world.func_82738_a(85);
                    if(boundary>=3&&boundary<=5) {
                        UpdateVehicleRidingEntities remove=new UpdateVehicleRidingEntities(51,train.getId());
                        remove.add(new VehicleRidingEntity(core.player.getUuid(),-1,0,0,0,false,false,false,false,false,false,false)); queuePassenger(core,remove); drainPassengerQueue(core);
                        if(boundary!=3) {
                            UpdateVehicleRidingEntities wrong=new UpdateVehicleRidingEntities(51,train.getId());
                            wrong.add(new VehicleRidingEntity(boundary==5?UUID.randomUUID():core.player.getUuid(),1,0,0,0,false,false,false,false,false,false,false)); queuePassenger(core,wrong); drainPassengerQueue(core);
                        }
                        sampleTrain(core,train,82,82.48);
                    }
                    if(boundary==6) new RideHandler(fixture);
                    if(boundary==7) RidingPermissions.revoke(core.player);
                    if(boundary==8) RidingPermissions.disconnect(core.player);
                    if(boundary==9) RidingPermissions.clear();
                    if(boundary==10) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.world));
                    if(boundary==11) RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(core.player.getUuid(),new Data(){}),CoreFixture::path);
                    if(boundary==12) fixture.world.playerEntities.remove(fixture.player);
                    assertFalse(RidingPermissions.refresh(core.player),"Frozen proof must fail boundary "+boundary);
                    assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
                } finally { channel.finish(); }
            } finally { RidingPermissions.clear(); }
        }
    }
    public void frozenNativeRiderRequiresAnActualValidatedNativePacketAndCoreMembership() throws Exception {
        for(int boundary=0;boundary<2;boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                fixture.player.setPosition(8.5,5,30);
                io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                try {
                    HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=vehicle(core,true);
                    for(Siding siding:core.simulator.sidings) core.simulator.sidingIdMap.put(siding.getId(),siding);
                    sampleTrain(core,train,0,30); UpdateVehicleRidingEntities admitted=RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false);
                    core.player.setInTeleportationState(true); core.player.setNoGravity(true); core.player.setNoClipMapped(true);
                    if(boundary==0) { queuePassenger(core,admitted); drainPassengerQueue(core); }
                    else handler.processPlayer(position(8.5,5,30));
                    for(int tick=2;tick<=80;tick+=2) sampleTrain(core,train,tick,30+.64*tick);
                    assertFalse(RidingPermissions.refresh(core.player),"Client admission alone or absent native C03 cannot create frozen proof, boundary "+boundary);
                    assertFalse(PlayerPhysics.authorized(fixture.player));
                } finally { channel.finish(); }
            } finally { RidingPermissions.clear(); }
        }
    }
    private static Vehicle boardFrozenRider(CoreFixture core,HandoffHandler handler) throws Exception {
        Vehicle train=vehicle(core,true); for(Siding siding:core.simulator.sidings) core.simulator.sidingIdMap.put(siding.getId(),siding);
        sampleTrain(core,train,0,30);
        queuePassenger(core,RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false)); drainPassengerQueue(core);
        assertNotNull(appliedPassenger(train,core.player.getUuid()));
        core.player.setInTeleportationState(true); core.player.setNoGravity(true); core.player.setNoClipMapped(true);
        handler.processPlayer(position(8.5,5,30)); assertEquals(0,handler.corrections);
        return train;
    }
    public void frozenNativeRiderBudgetCannotBeRenewedByFreshCoreOrIdenticalStalePackets() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            fixture.player.setPosition(8.5,5,30);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=boardFrozenRider(core,handler);
                for(int tick=2;tick<=198;tick+=2) {
                    sampleTrain(core,train,tick,30+.64*tick);
                    if(tick>=12) handler.processPlayer(position(8.5,5,30));
                    assertTrue(RidingPermissions.refresh(core.player),"Frozen episode must remain usable before its original native budget, tick "+tick);
                    assertEquals(30,fixture.player.posZ,0); assertEquals(0,handler.corrections);
                }
                // The validated current-cabin packet was at tick1; fresh Core and consumed old C03 cannot move that deadline.
                sampleTrain(core,train,200,158);
                assertFalse(RidingPermissions.refresh(core.player)); assertFalse(PlayerPhysics.authorized(fixture.player));
                assertFalse(fixture.player.noClip); assertEquals(.08,PlayerPhysics.gravity(.08,fixture.player),0);
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }
    public void frozenNativeRiderWallBudgetExpiresWhileNativeTicksRemainFresh() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            fixture.player.setPosition(8.5,5,30);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=boardFrozenRider(core,handler); freezeRider(core,train);
                // Advance only the private monotonic deadline, avoiding a ten-second test sleep.
                java.lang.reflect.Field riders=RidingPermissions.class.getDeclaredField("RIDERS"); riders.setAccessible(true);
                Object selection=((java.util.Map<?,?>)riders.get(null)).get(fixture.player);
                java.lang.reflect.Field anchorField=selection.getClass().getDeclaredField("anchor"); anchorField.setAccessible(true);
                Object anchor=anchorField.get(selection); assertNotNull(anchor);
                java.lang.reflect.Field untilNanos=anchor.getClass().getDeclaredField("untilNanos"); untilNanos.setAccessible(true); untilNanos.setLong(anchor,System.nanoTime()-1);
                assertFalse(RidingPermissions.refresh(core.player)); assertFalse(PlayerPhysics.authorized(fixture.player));
                assertFalse(fixture.player.noClip); assertEquals(.08,PlayerPhysics.gravity(.08,fixture.player),0);
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }
    public void actualCoreMembershipCannotRescueAnExpiredDeferredNativeMovement() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            fixture.player.setPosition(8.5,5,30);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                HandoffHandler handler=handoffHandler(fixture,channel); Vehicle train=boardFrozenRider(core,handler);
                fixture.world.func_82738_a(2); core.poll();
                JsonObject update=new JsonObject(); update.addProperty("railProgress",44); train.updateData(new JsonReader(update));
                handler.processPlayer(position(8.5,5,40));
                assertEquals(30,fixture.player.posZ,0); assertEquals(0,handler.corrections);
                assertNotNull(appliedPassenger(train,core.player.getUuid()));
                fixture.world.func_82738_a(6); core.poll(); core.drain(); fixture.world.func_82738_a(7); core.poll();
                assertEquals(30,fixture.player.posZ,0); assertEquals(1,handler.corrections);
                assertFalse(RidingPermissions.refresh(core.player)); assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }
    private static void freezeRider(CoreFixture core,Vehicle train) throws Exception {
        for(int tick=2;tick<=80;tick+=2) {
            sampleTrain(core,train,tick,30+.64*tick);
            assertTrue(RidingPermissions.refresh(core.player),"Fresh actual Core rider with exactly frozen native feet must survive client reload, tick "+tick);
            assertEquals(30,core.fixture.player.posZ,0);
        }
    }

    public void previousLiftBoardingPoseRetainsItsOriginalExpiry() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); core.simulator.sync(); fixture.player.setPosition(0,64,0);
            sampleLift(core,lift,0,0); sampleLift(core,lift,2,6);
            RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false);
            core.player.setInTeleportationState(true); core.player.setNoGravity(true); core.player.setNoClipMapped(true);
            for(int tick=4;tick<=38;tick+=2) {
                sampleLift(core,lift,tick,8);
                assertTrue(RidingPermissions.refresh(core.player),"Previous lift pose must survive until its original age reaches forty ticks, at "+tick);
            }
            assertEquals(39,fixture.world.getTotalWorldTime()); fixture.world.func_82738_a(40);
            assertFalse(RidingPermissions.refresh(core.player),"Timestamp zero must not renew to the boarding snapshot's timestamp two");
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
        } finally { RidingPermissions.clear(); }
    }
    public void movingLiftRetainsDelayedNativeRiderThenExpiresItsOldPositions() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator);
            lift.setFloors(ObjectArrayList.of(new LiftFloor(new Position(0,64,0),"1",""),new LiftFloor(new Position(0,160,0),"2","")));
            core.simulator.lifts.add(lift); core.simulator.sync(); fixture.player.setPosition(0,64,0);
            sampleLift(core,lift,0,0);
            RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false);
            core.player.setInTeleportationState(true); core.player.setNoGravity(true); core.player.setNoClipMapped(true);
            RideHandler handler=new RideHandler(fixture);
            for(int tick=2;tick<=100;tick+=2) {
                double clientY=64+.36*Math.max(0,tick-12);
                handler.processPlayer(position(0,clientY,0));
                sampleLift(core,lift,tick,.36*tick);
                assertTrue(RidingPermissions.refresh(core.player),"A selected lift passenger with twelve ticks of native delay lost authority at "+tick);
                assertEquals(0,handler.corrections); assertEquals(clientY,fixture.player.posY,1E-5);
                assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
            }
            for(int tick=102;tick<=160;tick+=2) sampleLift(core,lift,tick,.36*tick);
            assertFalse(RidingPermissions.refresh(core.player),"Fresh lift snapshots cannot renew an old stationary passenger position");
            assertFalse(PlayerPhysics.authorized(fixture.player));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "Expired selected history must never grant new remote lift boarding");
        } finally { RidingPermissions.clear(); }
    }
    private static void sampleLift(CoreFixture core,Lift lift,long requestedAt,double progress) throws Exception {
        core.fixture.world.func_82738_a(requestedAt);
        JsonObject update=new JsonObject(); update.addProperty("railProgress",progress); lift.updateData(new JsonReader(update));
        assertEquals(64+progress,lift.getPosition(CoreFixture::path).y,1E-5);
        core.observe(lift);
        core.fixture.world.func_82738_a(requestedAt+1); core.poll();
    }
    public void previousPoseBoardingRetainsItsAuthorityThroughNextCoreSampleAndNativeRecovery() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle train=boardThroughPreviousPose(core);
            sampleTrain(core,train,3135,66.9041);
            assertTrue(RidingPermissions.refresh(core.player),"New boarding through the authoritative previous pose must survive its replacement two ticks later");
            RideHandler handler=new RideHandler(fixture);
            handler.processPlayer(position(7.9519,5,58.9));
            assertEquals(0,handler.corrections); assertEquals(58.9,fixture.player.posZ,1E-5);
            handler.processPlayer(position(7.9519,5,66.9041));
            assertEquals(0,handler.corrections); assertEquals(66.9041,fixture.player.posZ,1E-5);
            assertTrue(RidingPermissions.refresh(core.player));
        } finally { RidingPermissions.clear(); }
    }
    public void previousPoseBoardingExpiresFromItsOriginalTimestampWhileCurrentSamplesStayFresh() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle train=boardThroughPreviousPose(core);
            for(long tick=3135;tick<=3169;tick+=2) {
                sampleTrain(core,train,tick,66.9041);
                assertTrue(RidingPermissions.refresh(core.player),"Previous boarding pose remains usable only while its original age is below40 at tick "+(tick+1));
            }
            // Original pose3131 is now39ticks old; admission timestamp3133 must not renew it.
            assertEquals(3170,fixture.world.getTotalWorldTime());
            fixture.world.func_82738_a(3171);
            assertFalse(RidingPermissions.refresh(core.player),"Previous pose must expire exactly at3131+40, not at3133+40");
            assertFalse(PlayerPhysics.authorized(fixture.player));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false),
                "The expired boarding pose cannot authorize new remote boarding");
        } finally { RidingPermissions.clear(); }
    }
    private static Vehicle boardThroughPreviousPose(CoreFixture core) throws Exception {
        Vehicle train=vehicle(core,true); core.fixture.player.setPosition(7.9519,5,58.6486);
        sampleTrain(core,train,3131,59.7764); sampleTrain(core,train,3133,65.6208);
        JsonObject packet=json(request(core.player.getUuid(),51,train.getId(),0));
        JsonObject row=packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
        row.addProperty("x",.5481); row.addProperty("y",1);
        assertNotNull(RidingPermissions.validate(core.player,new JsonReader(packet),false),"Existing onboarding admits only the previous pose in this sequence");
        core.player.setInTeleportationState(true); core.player.setNoClipMapped(true);
        return train;
    }

    public void movingTrainRetainsAuthenticatedRiderThroughObservedClientPauseAndNativeRecovery() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle train=vehicle(core,true);
            fixture.player.setPosition(7.9519,5,58.3764);
            sampleTrain(core,train,3131,59.7764);
            JsonObject riding=json(request(core.player.getUuid(),51,train.getId(),0));
            JsonObject row=riding.getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
            row.addProperty("x",.5481); row.addProperty("y",1); row.addProperty("z",0);
            RidingPermissions.validate(core.player,new JsonReader(riding),false);
            core.player.setInTeleportationState(true); core.player.setNoGravity(true); core.player.setNoClipMapped(true);
            RideHandler handler=new RideHandler(fixture);
            handler.processPlayer(position(7.9519,5,58.6486));
            sampleTrain(core,train,3133,65.6208);
            sampleTrain(core,train,3135,66.9041);
            assertTrue(RidingPermissions.refresh(core.player),"Observed train44d71852: fresh poses must retain an already boarded rider at Z58.6486 after a 1.278s client pause");
            assertTrue(fixture.player.noClip);
            // Next native C03 is still behind the newest and previous cabins, then catches up.
            handler.processPlayer(position(7.9519,5,58.9));
            assertEquals(0,handler.corrections,"Historical permission must reach native C03 bounds");
            assertEquals(58.9,fixture.player.posZ,1E-5);
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,new JsonReader(riding),false));
            handler.processPlayer(position(7.9519,5,66.9041));
            assertEquals(0,handler.corrections); assertEquals(66.9041,fixture.player.posZ,1E-5);
            assertTrue(RidingPermissions.refresh(core.player));
        } finally { RidingPermissions.clear(); }
    }
    public void movingTrainHistorySupportsPersistentLagThenExpiresWithoutPassengerHeartbeats() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle train=vehicle(core,true); fixture.player.setPosition(8.5,5,30);
            sampleTrain(core,train,0,30); RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false);
            core.player.setInTeleportationState(true); core.player.setNoClipMapped(true); RideHandler handler=new RideHandler(fixture);
            for(int tick=2;tick<=100;tick+=2) {
                double clientZ=30+.64*Math.max(0,tick-26);
                handler.processPlayer(position(8.5,5,clientZ));
                sampleTrain(core,train,tick,30+.64*tick);
                assertTrue(RidingPermissions.refresh(core.player),"Continuous 26-tick client delay must survive at tick "+tick);
                assertEquals(0,handler.corrections); assertEquals(clientZ,fixture.player.posZ,1E-5);
            }
            // Freeze the player while fresh Core poses continue: the last covering sample must age out.
            for(int tick=102;tick<=148;tick+=2) sampleTrain(core,train,tick,30+.64*tick);
            assertFalse(RidingPermissions.refresh(core.player),"Fresh Core samples cannot renew old historical positions");
            assertFalse(PlayerPhysics.authorized(fixture.player));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false),
                "Historical evidence must never become remote boarding permission");
        } finally { RidingPermissions.clear(); }
    }
    public void movingTrainHistoryRejectsEscapesAndCannotTransferToANewSelection() throws Exception {
        for(int boundary=0;boundary<10;boundary++) {
            try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                Vehicle train=boardLaggingTrain(core); RideHandler handler=new RideHandler(fixture);
                if(boundary<5) {
                    double x=boundary==0?30:8.5, y=boundary==1?30:boundary==2?-10:5, z=boundary==3?0:boundary==4?100:30;
                    handler.processPlayer(position(x,y,z));
                    assertEquals(1,handler.corrections,"Escaping C03 must hit the actual native guard");
                } else {
                    JsonObject packet=json(request(core.player.getUuid(),51,train.getId(),0));
                    JsonObject row=packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
                    if(boundary==5) packet.addProperty("vehicleId",train.getId()+1);
                    if(boundary==6) packet.addProperty("sidingId",52);
                    if(boundary==7) row.addProperty("ridingCar",1);
                    if(boundary==8) row.addProperty("isOnGangway",true);
                    if(boundary==9) row.addProperty("x",Double.NaN);
                    assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,new JsonReader(packet),false));
                }
                assertFalse(PlayerPhysics.authorized(fixture.player),"Boundary "+boundary);
                assertFalse(RidingPermissions.refresh(core.player),"No resurrection after native rejection");
            } finally { RidingPermissions.clear(); }
        }
    }
    public void movingTrainHistoryClearsOnLifecycleChangesAndTeleportDiscontinuity() throws Exception {
        for(int boundary=0;boundary<10;boundary++) {
            try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                Vehicle train=boardLaggingTrain(core);
                if(boundary==0) RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(core.player.getUuid(),core.simulator),CoreFixture::path);
                if(boundary==1) RidingPermissions.disconnect(core.player);
                if(boundary==2) RidingPermissions.clear();
                if(boundary==3) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.world));
                if(boundary==4) { fixture.player.dimension=1; core.poll(); }
                if(boundary==5) { for(Siding siding:core.simulator.sidings) siding.clearVehicles(); fixture.world.func_82738_a(10); core.poll(); core.drain(); core.poll(); }
                if(boundary==6) sampleTrain(core,train,8,150);
                if(boundary==7) fixture.world.func_82738_a(46);
                if(boundary==8) {
                    VehicleLiftResponse untrusted=new VehicleLiftResponse(core.player.getUuid(),new Data(){}); untrusted.addVehicleToUpdate(new VehicleUpdate(train,train.vehicleExtraData));
                    RidingPermissions.observe(new ServerWorld(fixture.world),untrusted,CoreFixture::path);
                }
                if(boundary==9) RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),-1),true);
                assertFalse(RidingPermissions.refresh(core.player),"History must clear on boundary "+boundary);
                assertFalse(PlayerPhysics.authorized(fixture.player));
                fixture.player.dimension=0;
                assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false));
            } finally { RidingPermissions.clear(); }
        }
    }
    private static Vehicle boardLaggingTrain(CoreFixture core) throws Exception {
        Vehicle train=vehicle(core,true); core.fixture.player.setPosition(8.5,5,30);
        sampleTrain(core,train,0,30); RidingPermissions.validate(core.player,request(core.player.getUuid(),51,train.getId(),0),false);
        core.player.setInTeleportationState(true); core.player.setNoClipMapped(true);
        sampleTrain(core,train,2,35); sampleTrain(core,train,4,42); sampleTrain(core,train,6,44);
        assertTrue(RidingPermissions.refresh(core.player),"Precondition: valid selected rider behind latest two poses");
        return train;
    }

    private static void sampleTrain(CoreFixture core,Vehicle train,long requestedAt,double centerZ) throws Exception {
        core.fixture.world.func_82738_a(requestedAt);
        JsonObject update=new JsonObject(); update.addProperty("railProgress",centerZ+4); train.updateData(new JsonReader(update));
        assertEquals(centerZ,vehicleCenter(train).z,1E-5);
        VehicleLiftResponse response=new VehicleLiftResponse(core.player.getUuid(),core.simulator);
        response.addVehicleToUpdate(new VehicleUpdate(train,train.vehicleExtraData));
        RidingPermissions.observe(new ServerWorld(core.fixture.world),response,CoreFixture::path);
        core.poll(); core.drain(); core.fixture.world.func_82738_a(requestedAt+1); core.poll();
    }
    private static net.minecraft.network.play.client.C03PacketPlayer position(double x,double y,double z) {
        return new net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition(x,y,y+1.62,z,false);
    }
    private static final class RideHandler extends net.minecraft.network.NetHandlerPlayServer {
        int corrections;
        RideHandler(ServerLifecycleScenarios.ServerFixture fixture) { this(fixture,new net.minecraft.network.NetworkManager(false)); }
        RideHandler(ServerLifecycleScenarios.ServerFixture fixture,net.minecraft.network.NetworkManager manager) { super(fixture.server,manager,fixture.player); }
        @Override public void sendPacket(net.minecraft.network.Packet packet) {}
        @Override public void setPlayerLocation(double x,double y,double z,float yaw,float pitch) { corrections++; super.setPlayerLocation(x,y,z,yaw,pitch); }
        @Override public void kickPlayerFromServer(String reason) { fail(reason); }
    }

    public void frequentOriginalKeepUpdatesDoNotStarveInitialCoreRead() throws Exception {
        frequentOriginalObservations(false);
    }
    public void frequentOriginalKeepUpdatesDoNotExpireActiveLease() throws Exception {
        frequentOriginalObservations(true);
    }
    private static void frequentOriginalObservations(boolean active) throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); Vehicle dirtyVehicle=vehicle(core);
            fixture.player.setPosition(0,64,0);
            if(active) {
                core.observe(lift); RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false);
            }
            VehicleLiftResponse initial=new VehicleLiftResponse(core.player.getUuid(),core.simulator);
            initial.addLiftToUpdate(lift); initial.addVehicleToUpdate(new VehicleUpdate(dirtyVehicle,dirtyVehicle.vehicleExtraData));
            RidingPermissions.observe(new ServerWorld(fixture.world),initial,CoreFixture::path); core.poll(); core.drain();
            for(int tick=1;tick<=64;tick++) {
                fixture.world.func_82738_a(tick);
                VehicleLiftResponse traffic=new VehicleLiftResponse(core.player.getUuid(),core.simulator);
                traffic.addLiftToKeep(lift.getId()); traffic.addVehicleToUpdate(new VehicleUpdate(dirtyVehicle,dirtyVehicle.vehicleExtraData));
                // Exact production order: original S2C first, then native polling. Core completes between ticks.
                RidingPermissions.observe(new ServerWorld(fixture.world),traffic,CoreFixture::path); core.poll();
                if(active) assertTrue(PlayerPhysics.authorized(fixture.player),"Ordinary S2C must not starve an active lease at tick "+tick);
                else assertFalse(PlayerPhysics.authorized(fixture.player),"Fresh numeric reads alone must not authorize initial boarding");
                core.drain();
            }
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "A stream of original keep updates must not discard every completed Core read beyond TTL40");
            assertTrue(PlayerPhysics.authorized(fixture.player));
        } finally { RidingPermissions.clear(); }
    }
    public void differentValidCoreSourceCannotAdoptACompletedOldGeneration() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0); core.observe(lift);
            RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false);
            fixture.world.func_82738_a(5); core.poll(); core.drain();
            org.mtr.core.simulation.Simulator replacement=new org.mtr.core.simulation.Simulator(core.simulator.dimension,new String[]{core.simulator.dimension},java.nio.file.Files.createTempDirectory("mtr-riding-replacement-core"),false);
            try {
                Client client=new Client(core.player.getUuid()); client.setPositionAndUpdateRadius(new Position(0,64,0),128); replacement.clients.add(client);
                VehicleLiftResponse response=new VehicleLiftResponse(core.player.getUuid(),replacement); response.addLiftToKeep(lift.getId());
                RidingPermissions.observe(new ServerWorld(fixture.world),response,CoreFixture::path); core.poll();
                assertFalse(PlayerPhysics.authorized(fixture.player),"A different valid Core source must invalidate old numeric authority");
                assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
                CoreFixture.drain(replacement); core.poll();
                assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                    "The replacement has no registered lift; a completed old-source callback must not restore it");
            } finally { replacement.stop(); }
        } finally { RidingPermissions.clear(); }
    }

    public void idleCoreQueryRefreshesARegisteredLiftWithoutOriginalS2C() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); core.simulator.sync();
            fixture.player.setPosition(0,64,0); core.observe(lift);
            fixture.world.func_82738_a(104);
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
            core.poll(); core.drain(); core.poll();
            assertFalse(PlayerPhysics.authorized(fixture.player),"A numeric query alone must not grant riding");
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "An idle registered Core lift needs a fresh numeric sample even when delta S2C emits nothing");
            assertTrue(PlayerPhysics.authorized(fixture.player));
        } finally { RidingPermissions.clear(); }
    }

    public void coreCopiesAreDetachedAndCurrentMovementAndRemovalReplaceOldPose() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0); core.observe(lift);
            fixture.world.func_82738_a(5); core.poll(); core.drain();
            lift.setDimensions(3,3,3,60,0,0); core.poll();
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "Mutating the original after the Core read must not mutate the detached result");
            fixture.world.func_82738_a(10); core.poll(); core.drain(); core.poll();
            assertFalse(PlayerPhysics.authorized(fixture.player));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
            fixture.player.setPosition(60,64,0);
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
            core.simulator.lifts.remove(lift); fixture.world.func_82738_a(15); core.poll(); core.drain(); core.poll();
            assertFalse(PlayerPhysics.authorized(fixture.player),"A completed read of a removed ID must revoke immediately");
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
        } finally { RidingPermissions.clear(); }
    }
    public void lateCoreResultsCannotRenewAnExpiredRequestAndPendingReadsAreBounded() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0); core.observe(lift);
            fixture.world.func_82738_a(5); core.poll();
            for(int tick=6;tick<45;tick++) { fixture.world.func_82738_a(tick); core.poll(); }
            assertEquals(1,core.queuedCount(),"A stopped Core must not accumulate polling tasks");
            fixture.world.func_82738_a(45); core.drain(); core.poll();
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "A callback delivered40ticks after request cannot stamp old evidence as fresh");
            core.drain(); core.poll();
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
        } finally { RidingPermissions.clear(); }
    }
    public void originalMembershipRemovalDisconnectClearAndWorldUnloadInvalidateQueuedResults() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0);
            for(int boundary=0;boundary<5;boundary++) {
                fixture.player.dimension=0; fixture.world.func_82738_a(boundary*10); core.observe(lift);
                fixture.world.func_82738_a(boundary*10+5); core.poll(); assertEquals(1,core.queuedCount());
                if(boundary==0) RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(core.player.getUuid(),core.simulator),CoreFixture::path);
                if(boundary==1) RidingPermissions.disconnect(core.player);
                if(boundary==2) { RidingPermissions.clear();
                    VehicleLiftResponse response=new VehicleLiftResponse(core.player.getUuid(),core.simulator); response.addLiftToUpdate(lift);
                    RidingPermissions.observe(new ServerWorld(fixture.world),response,CoreFixture::path); core.poll();
                    assertEquals(1,core.queuedCount(),"Clear/rebind cannot enqueue a second task while an old generation is pending"); }
                if(boundary==3) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.world));
                if(boundary==4) { fixture.player.dimension=1; core.poll(); }
                core.drain(); core.poll();
                assertFalse(PlayerPhysics.authorized(fixture.player));
                assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                    "An old query must not restore invalidated view generation "+boundary);
                core.drain(); RidingPermissions.clear();
            }
        } finally { RidingPermissions.clear(); }
    }
    public void keepOnlyPacketsDoNotRenewCachedPoseAndCoreSubscriptionRemovalFailsClosed() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0); core.observe(lift);
            fixture.world.func_82738_a(104);
            VehicleLiftResponse keep=new VehicleLiftResponse(core.player.getUuid(),core.simulator); keep.addLiftToKeep(lift.getId());
            RidingPermissions.observe(new ServerWorld(fixture.world),keep,CoreFixture::path);
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "Original keep carries no numeric pose and cannot stretch TTL");
            core.poll(); core.drain(); core.poll();
            RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false);
            core.simulator.clients.clear(); fixture.world.func_82738_a(110); core.poll(); core.drain(); core.poll();
            assertFalse(PlayerPhysics.authorized(fixture.player));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
        } finally { RidingPermissions.clear(); }
    }
    public void stoppedRegisteredVehicleGetsCurrentGeometryAndRemovalInvalidatesIt() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle vehicle=vehicle(core); Vector center=vehicleCenter(vehicle); fixture.player.setPosition(center.x,center.y,center.z);
            VehicleLiftResponse response=new VehicleLiftResponse(core.player.getUuid(),core.simulator); response.addVehicleToUpdate(new VehicleUpdate(vehicle,vehicle.vehicleExtraData));
            RidingPermissions.observe(new ServerWorld(fixture.world),response,CoreFixture::path); core.poll(); core.drain(); core.poll();
            fixture.world.func_82738_a(104); core.poll(); core.drain(); core.poll();
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),51,vehicle.getId(),0),false));
            for(Siding siding:core.simulator.sidings) siding.clearVehicles();
            fixture.world.func_82738_a(110); core.poll(); core.drain(); core.poll();
            assertFalse(PlayerPhysics.authorized(fixture.player));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),51,vehicle.getId(),0),false));
        } finally { RidingPermissions.clear(); }
    }
    public void noKeyPassengerInOriginalStockDoorwayHoldsDepartureWithoutControls() throws Exception {
        JsonObject profile = passengerProfile("sp1900_trailer");
        for (double x : new double[]{1.375, -1.375}) {
            try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
                Vehicle train = passengerVehicle(core, profile, TransportMode.TRAIN, "sp1900_trailer");
                passengerSample(core, train);
                Vector feet = passengerWorld(train, 0, x, 1, 0); fixture.player.setPosition(feet.x, feet.y, feet.z);
                JsonObject packet = passengerRequest(core, train, x, 1, 0);
                JsonObject accepted = passengerRow(RidingPermissions.validate(core.player, new JsonReader(packet), false));
                assertTrue(accepted.get("doorOverride").getAsBoolean(), "Original zero-height stock doorway at native feet must retain no-key passenger hold, x=" + x);
                for (String key : new String[]{"isDriver", "manualAccelerate", "manualBrake", "manualToggleDoors", "manualToggleAto"}) assertFalse(accepted.get(key).getAsBoolean(), key);
                packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject().addProperty("doorOverride", false);
                assertFalse(passengerRow(RidingPermissions.validate(core.player, new JsonReader(packet), false)).get("doorOverride").getAsBoolean(), "Release clears the Core departure hold");
            } finally { RidingPermissions.clear(); }
        }
    }
    public void passengerHoldRejectsForgedSeatStaleWorldCarModelAndMovingClaims() throws Exception {
        for (int boundary = 0; boundary < 12; boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
                Vehicle train = passengerVehicle(core, passengerProfile("sp1900_trailer"), boundary == 6 ? TransportMode.BOAT : TransportMode.TRAIN, boundary == 5 ? "unknown_addon_train" : "sp1900_trailer");
                if (boundary == 7) { JsonObject moving = new JsonObject(); moving.addProperty("speed",0.01); train.updateData(new JsonReader(moving)); assertTrue(train.isMoving()); }
                passengerSample(core, train);
                double x = boundary <= 1 ? 0 : boundary == 10 ? 1.8001 : 1.375;
                double y = boundary == 9 ? 1.00001 : 1;
                double z = boundary == 11 ? 1.1751 : 0;
                Vector feet = passengerWorld(train,0,x,y,z); fixture.player.setPosition(feet.x,feet.y,feet.z);
                JsonObject packet = passengerRequest(core,train,boundary == 1 ? 0 : 1.375,1,0);
                JsonObject row = packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
                if (boundary == 2) fixture.world.func_82738_a(5);
                if (boundary == 3) fixture.player.dimension = 1;
                if (boundary == 4) row.addProperty("ridingCar",1);
                if (boundary == 8) row.addProperty("clientId",UUID.randomUUID().toString());
                if (boundary == 3 || boundary == 4 || boundary == 8) {
                    assertThrows(RidingPermissions.Rejected.class, () -> RidingPermissions.validate(core.player,new JsonReader(packet),false), "authority boundary " + boundary);
                } else {
                    JsonObject accepted = passengerRow(RidingPermissions.validate(core.player,new JsonReader(packet),false));
                    assertFalse(accepted.get("doorOverride").getAsBoolean(), "Native feet, model, mode, four-tick freshness and stopped train must independently authorize hold, boundary " + boundary);
                    for (String key : new String[]{"isDriver","manualAccelerate","manualBrake","manualToggleDoors","manualToggleAto"}) assertFalse(accepted.get(key).getAsBoolean(), key);
                }
            } finally { RidingPermissions.clear(); }
        }
    }
    public void passengerHoldUsesPreviousPoseOnlyThroughItsOriginalFourTickWindow() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
            Vehicle train = passengerVehicle(core,passengerProfile("sp1900_trailer"),TransportMode.TRAIN,"sp1900_trailer");
            passengerSample(core,train);
            Vector feet = passengerWorld(train,0,1.375,1,0); fixture.player.setPosition(feet.x,feet.y,feet.z);
            JsonObject packet = passengerRequest(core,train,1.375,1,0);
            RidingPermissions.validate(core.player,new JsonReader(packet),false);
            fixture.world.func_82738_a(2); JsonObject moved = new JsonObject(); moved.addProperty("railProgress",102); train.updateData(new JsonReader(moved)); passengerSample(core,train);
            assertTrue(passengerRow(RidingPermissions.validate(core.player,new JsonReader(packet),false)).get("doorOverride").getAsBoolean(), "Fresh original previous pose may hold doors");
            fixture.world.func_82738_a(5);
            assertFalse(passengerRow(RidingPermissions.validate(core.player,new JsonReader(packet),false)).get("doorOverride").getAsBoolean(), "A fresh current sample and forty-tick riding trail cannot renew previous doorway authority");
            Vector current = passengerWorld(train,0,1.375,1,0); fixture.player.setPosition(current.x,current.y,current.z);
            assertTrue(passengerRow(RidingPermissions.validate(core.player,new JsonReader(packet),false)).get("doorOverride").getAsBoolean());
            fixture.world.func_82738_a(7);
            assertFalse(passengerRow(RidingPermissions.validate(core.player,new JsonReader(packet),false)).get("doorOverride").getAsBoolean(), "Current doorway pose itself expires after four ticks");
        } finally { RidingPermissions.clear(); }
    }
    public void passengerHoldNeverBroadensBasicAdvancedGuardOrDepotKeyControls() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
            Vehicle train = passengerVehicle(core,passengerProfile("sp1900_trailer"),TransportMode.TRAIN,"sp1900_trailer"); passengerSample(core,train);
            Vector feet = passengerWorld(train,0,0,1,2); fixture.player.setPosition(feet.x,feet.y,feet.z);
            JsonObject packet = passengerRequest(core,train,0,1,2);
            for (int key = 0; key < 6; key++) {
                boolean drive = key == 0 || key == 1 || key == 3, doors = key == 1 || key == 2 || key == 3;
                ItemDriverKey item = key == 3 ? new ItemCreativeDriverKey(new ItemSettings()) : new ItemDepotDriverKey(new ItemSettings(),key >= 4 || drive,key >= 4 || doors,false,0);
                net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
                if (key != 3) { stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound()); stack.getTagCompound().setLong("depot_id",key == 4 ? 999 : 52); stack.getTagCompound().setLong("expiry_time",key == 5 ? 1 : System.currentTimeMillis()+60000); }
                fixture.player.inventory.setInventorySlotContents(0,stack);
                JsonObject accepted = passengerRow(RidingPermissions.validate(core.player,new JsonReader(packet),false));
                assertEquals(drive || doors,accepted.get("isDriver").getAsBoolean(),"key " + key);
                for (String name : new String[]{"manualAccelerate","manualBrake","manualToggleAto"}) assertEquals(drive,accepted.get(name).getAsBoolean(),name);
                assertEquals(doors,accepted.get("manualToggleDoors").getAsBoolean()); assertEquals(doors,accepted.get("doorOverride").getAsBoolean());
            }
        } finally { RidingPermissions.clear(); }
    }
    public void passengerHoldSupportsOffsetAndBothActualBodyModels() throws Exception {
        for (int variant = 0; variant < 3; variant++) {
            String id = variant == 0 ? "s700_low_floor_sound_transit_1" : "class_802_gwr_trailer";
            double x = variant == 0 ? 1.375 : 1.28125, y = variant == 0 ? 0.5 : 1, z = variant == 0 ? -0.125 : variant == 1 ? -9.96875 : 9.96875;
            try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
                Vehicle train = passengerVehicle(core,passengerProfile(id),TransportMode.TRAIN,id); passengerSample(core,train);
                Vector feet = passengerWorld(train,0,x,y,z); fixture.player.setPosition(feet.x,feet.y,feet.z);
                assertTrue(passengerRow(RidingPermissions.validate(core.player,new JsonReader(passengerRequest(core,train,x,y,z)),false)).get("doorOverride").getAsBoolean(), "Original offset/body doorway " + variant);
            } finally { RidingPermissions.clear(); }
        }
    }
    public void bundledPassengerDoorwayCacheCoversStockModesAndKeepsFloorOnlyEmpty() throws Exception {
        java.lang.reflect.Method method = RidingPermissions.class.getDeclaredMethod("doorways",TransportMode.class,String.class); method.setAccessible(true);
        int vehicles = 0, withDoorways = 0;
        for (JsonElement entry : cableBundledJson("mtr_custom_resources.json").getAsJsonArray("vehicles")) {
            JsonObject profile = entry.getAsJsonObject(); String id = profile.get("id").getAsString();
            TransportMode mode = TransportMode.valueOf(profile.get("transportMode").getAsString());
            java.util.List<?> boxes = (java.util.List<?>)method.invoke(null,mode,id); vehicles++;
            if (java.util.Arrays.asList("s700_sound_transit_2","s700_low_floor_sound_transit_2","london_underground_1938_trailer","london_underground_1938_cab_1","london_underground_1938_cab_2","london_underground_1938_cab_3").contains(id)) assertTrue(boxes.isEmpty());
            else { assertFalse(boxes.isEmpty(),mode + ":" + id); withDoorways++; }
            assertSame(boxes,method.invoke(null,mode,id),"No geometry parsing or resource IO on repeat cabin reads");
        }
        assertEquals(423,vehicles); assertEquals(417,withDoorways);
        assertTrue(((java.util.List<?>)method.invoke(null,TransportMode.TRAIN,"unknown_addon_train")).isEmpty());
        assertTrue(((java.util.List<?>)method.invoke(null,TransportMode.BOAT,"sp1900_trailer")).isEmpty());
        assertEquals(4,((java.util.List<?>)method.invoke(null,TransportMode.TRAIN,"class_802_gwr_trailer")).size(),"Keep both end body models; ignore original missing doorway_2 names");
    }
    public void numericDoorwayExtractionMatchesPlacementAndRejectsUnsupportedGeometry() throws Exception {
        String propertyJson = "{\"modelYOffset\":0.5,\"parts\":[{\"type\":\"DOORWAY\",\"names\":[\"door\"],\"positionDefinitions\":[\"main\"]}]}";
        String placementJson = "{\"positionDefinitions\":[{\"name\":\"main\",\"positions\":[{\"x\":8,\"y\":16,\"z\":32}],\"positionsFlipped\":[{\"x\":8,\"y\":16,\"z\":32}]}]}";
        String meshJson = "{\"outliner\":[{\"name\":\"door\",\"origin\":[4,8,12],\"children\":[\"uuid\"]}],\"elements\":[{\"uuid\":\"uuid\",\"origin\":[5,7,9],\"from\":[20,0,-14],\"to\":[24,0,10]}]}";
        java.util.List<net.minecraft.util.AxisAlignedBB> boxes = parsedDoorways(propertyJson,placementJson,meshJson);
        assertEquals(2,boxes.size()); doorwayBox(boxes.get(0),1.75,1.5,1.125,2,1.5,2.625); doorwayBox(boxes.get(1),-2,1.5,1.375,-1.75,1.5,2.875);
        for (int boundary = 0; boundary < 9; boundary++) {
            com.google.gson.JsonObject properties = new com.google.gson.JsonParser().parse(propertyJson).getAsJsonObject();
            com.google.gson.JsonObject positions = new com.google.gson.JsonParser().parse(placementJson).getAsJsonObject();
            com.google.gson.JsonObject mesh = new com.google.gson.JsonParser().parse(meshJson).getAsJsonObject();
            com.google.gson.JsonObject element = mesh.getAsJsonArray("elements").get(0).getAsJsonObject(), group = mesh.getAsJsonArray("outliner").get(0).getAsJsonObject();
            if (boundary == 0) element.add("rotation",new com.google.gson.JsonParser().parse("[0,10,0]"));
            if (boundary == 1) group.add("rotation",new com.google.gson.JsonParser().parse("[0,0,1]"));
            if (boundary == 2) group.add("children",new com.google.gson.JsonParser().parse("[{}]"));
            if (boundary == 3) element.addProperty("inflate",0.1);
            if (boundary == 4) element.add("from",new com.google.gson.JsonParser().parse("[0,0]"));
            if (boundary == 5) element.add("to",new com.google.gson.JsonParser().parse("[0,\"NaN\",0]"));
            if (boundary == 6) properties.addProperty("modelYOffset",Double.POSITIVE_INFINITY);
            if (boundary == 7) positions.getAsJsonArray("positionDefinitions").get(0).getAsJsonObject().getAsJsonArray("positions").get(0).getAsJsonObject().addProperty("x",Double.NaN);
            if (boundary == 8) group.add("origin",new com.google.gson.JsonParser().parse("[0,0,\"NaN\"]"));
            assertThrows(IllegalArgumentException.class,() -> parsedDoorways(properties.toString(),positions.toString(),mesh.toString()),"Unsupported numeric geometry " + boundary);
        }
    }
    private static java.util.List<net.minecraft.util.AxisAlignedBB> parsedDoorways(String properties, String positions, String mesh) throws Exception {
        java.lang.reflect.Method method = RidingPermissions.class.getDeclaredMethod("parseDoorways",com.google.gson.JsonObject.class,com.google.gson.JsonObject.class,com.google.gson.JsonObject.class); method.setAccessible(true);
        try {
            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            return (java.util.List<net.minecraft.util.AxisAlignedBB>)method.invoke(null,parser.parse(properties).getAsJsonObject(),parser.parse(positions).getAsJsonObject(),parser.parse(mesh).getAsJsonObject());
        } catch (java.lang.reflect.InvocationTargetException failure) {
            if (failure.getCause() instanceof IllegalArgumentException) throw (IllegalArgumentException)failure.getCause();
            throw failure;
        }
    }
    private static void doorwayBox(net.minecraft.util.AxisAlignedBB box, double x1, double y1, double z1, double x2, double y2, double z2) {
        assertEquals(x1,box.minX,1E-9); assertEquals(y1,box.minY,1E-9); assertEquals(z1,box.minZ,1E-9);
        assertEquals(x2,box.maxX,1E-9); assertEquals(y2,box.maxY,1E-9); assertEquals(z2,box.maxZ,1E-9);
    }
    public void passengerHoldExpiresInAppliedCoreAfterSilenceWalkoutAndRevocation() throws Exception {
        for (int boundary = 0; boundary < 11; boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
                Vehicle train = passengerVehicle(core,passengerProfile("sp1900_trailer"),TransportMode.TRAIN,"sp1900_trailer"); passengerSample(core,train);
                Vector feet = passengerWorld(train,0,1.375,1,0); fixture.player.setPosition(feet.x,feet.y,feet.z);
                UpdateVehicleRidingEntities update = RidingPermissions.validate(core.player,new JsonReader(passengerRequest(core,train,1.375,1,0)),false);
                queuePassenger(core,update); drainPassengerQueue(core);
                assertTrue(appliedPassenger(train,core.player.getUuid()).getDoorOverride(),"The original Core operation must actually apply hold=true before checking expiry");
                if (boundary == 0) { fixture.world.func_82738_a(5); core.poll(); }
                if (boundary == 1) { Vector seat = passengerWorld(train,0,0,1,0); fixture.player.setPosition(seat.x,seat.y,seat.z); core.poll(); }
                if (boundary == 2) RidingPermissions.revoke(core.player);
                if (boundary == 3) RidingPermissions.disconnect(core.player);
                if (boundary == 4) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.world));
                if (boundary == 5) RidingPermissions.clear();
                if (boundary == 6) { fixture.player.dimension = 1; core.poll(); }
                if (boundary == 7) RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(core.player.getUuid(),core.simulator),CoreFixture::path);
                if (boundary == 8) { new RideHandler(fixture); core.poll(); }
                if (boundary == 9) { fixture.player.worldObj = new org.mtr.mapping.BlockPlacementScenarios.MemoryWorld(); core.poll(); }
                if (boundary == 10) RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(core.player.getUuid(),new Data(){}),CoreFixture::path);
                drainPassengerQueue(core);
                VehicleRidingEntity current = appliedPassenger(train,core.player.getUuid());
                assertTrue(current == null || !current.getDoorOverride(),"Original Core cannot retain hold after native boundary " + boundary);
                if (boundary == 1) {
                    fixture.player.setPosition(feet.x,feet.y,feet.z); core.poll(); drainPassengerQueue(core);
                    assertFalse(appliedPassenger(train,core.player.getUuid()).getDoorOverride(),"Returning native feet cannot resurrect an intent already cleared by actual doorway exit");
                }
                int queued = passengerQueueSize(core); core.poll(); drainPassengerQueue(core);
                assertEquals(0,queued,"The one cleanup message must already be consumed");
                assertEquals(0,passengerQueueSize(core),"No repeated cleanup traffic after the lease was removed");
            } finally { RidingPermissions.clear(); }
        }
    }
    public void passengerHoldReleaseFollowsQueuedTrueWithoutOverwritingLaterKeyControls() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
            Vehicle train = passengerVehicle(core,passengerProfile("sp1900_trailer"),TransportMode.TRAIN,"sp1900_trailer"); passengerSample(core,train);
            Vector feet = passengerWorld(train,0,1.375,1,0); fixture.player.setPosition(feet.x,feet.y,feet.z);
            JsonObject packet = passengerRequest(core,train,1.375,1,0);
            queuePassenger(core,RidingPermissions.validate(core.player,new JsonReader(packet),false));
            RidingPermissions.revoke(core.player); drainPassengerQueue(core);
            assertNull(appliedPassenger(train,core.player.getUuid()),"A revoke queued before Core applies its pending original true must still remove it afterwards");
            queuePassenger(core,RidingPermissions.validate(core.player,new JsonReader(packet),false));
            fixture.world.func_82738_a(5); core.poll(); // The passenger expiry release is queued after true.
            fixture.player.inventory.setInventorySlotContents(0,new net.minecraft.item.ItemStack(new ItemCreativeDriverKey(new ItemSettings())));
            queuePassenger(core,RidingPermissions.validate(core.player,new JsonReader(packet),false));
            drainPassengerQueue(core); core.poll(); drainPassengerQueue(core);
            VehicleRidingEntity driver = appliedPassenger(train,core.player.getUuid());
            assertNotNull(driver); assertTrue(driver.isDriver()); assertTrue(driver.getDoorOverride(),"An old passenger lease must not overwrite a later key-authorized row");
        } finally { RidingPermissions.clear(); }
    }
    public void stationaryOriginalPassengerRenewsFromNativeProofWithoutRidingHeartbeat() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
            Vehicle train = passengerVehicle(core,passengerProfile("sp1900_trailer"),TransportMode.TRAIN,"sp1900_trailer"); passengerSample(core,train);
            Vector feet = passengerWorld(train,0,1.375,1,0); fixture.player.setPosition(feet.x,feet.y,feet.z);
            queuePassenger(core,RidingPermissions.validate(core.player,new JsonReader(passengerRequest(core,train,1.375,1,0)),false)); drainPassengerQueue(core);
            for (long tick = 2; tick <= 20; tick += 2) {
                fixture.world.func_82738_a(tick); passengerSample(core,train); drainPassengerQueue(core);
                assertTrue(appliedPassenger(train,core.player.getUuid()).getDoorOverride(),"Original standing client sends no synthetic riding heartbeat; fresh native doorway proof must keep hold at tick " + tick);
            }
            fixture.world.func_82738_a(22); core.poll(); // Keep the original tick22 read pending during a short Core queue pause.
            fixture.world.func_82738_a(25); core.poll(); drainPassengerQueue(core);
            assertFalse(appliedPassenger(train,core.player.getUuid()).getDoorOverride(),"Stale pose must clear Core hold even though the player has not moved");
            queuePassenger(core,RidingPermissions.validate(core.player,new JsonReader(passengerRequest(core,train,1.375,1,0)),false)); drainPassengerQueue(core);
            assertFalse(appliedPassenger(train,core.player.getUuid()).getDoorOverride(),"A valid original true request cannot grant hold from stale geometry");
            fixture.world.func_82738_a(26); core.poll(); drainPassengerQueue(core);
            assertTrue(appliedPassenger(train,core.player.getUuid()).getDoorOverride(),"Fresh authoritative proof may recover active intent after a short Core-read gap");
            RidingPermissions.revoke(core.player); drainPassengerQueue(core);
            fixture.world.func_82738_a(28); passengerSample(core,train); drainPassengerQueue(core);
            assertNull(appliedPassenger(train,core.player.getUuid()),"A refreshed pose must never reactivate a revoked intent");
        } finally { RidingPermissions.clear(); }
    }
    public void passengerHoldRecoveryCannotRestoreDriverAfterServerKeyRemoval() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
            Vehicle train = passengerVehicle(core,passengerProfile("sp1900_trailer"),TransportMode.TRAIN,"sp1900_trailer"); passengerSample(core,train);
            Vector feet = passengerWorld(train,0,1.375,1,0); fixture.player.setPosition(feet.x,feet.y,feet.z);
            net.minecraft.item.ItemStack key = new net.minecraft.item.ItemStack(new ItemDepotDriverKey(new ItemSettings(),true,false,false,0));
            key.setTagCompound(new net.minecraft.nbt.NBTTagCompound()); key.getTagCompound().setLong("depot_id",52); key.getTagCompound().setLong("expiry_time",System.currentTimeMillis()+60000);
            fixture.player.inventory.setInventorySlotContents(0,key);
            queuePassenger(core,RidingPermissions.validate(core.player,new JsonReader(passengerRequest(core,train,1.375,1,0)),false)); drainPassengerQueue(core);
            assertTrue(appliedPassenger(train,core.player.getUuid()).isDriver());
            fixture.world.func_82738_a(2); core.poll();
            fixture.player.inventory.setInventorySlotContents(0,null);
            fixture.world.func_82738_a(5); core.poll(); drainPassengerQueue(core);
            fixture.world.func_82738_a(6); core.poll(); drainPassengerQueue(core);
            VehicleRidingEntity restored = appliedPassenger(train,core.player.getUuid());
            assertTrue(restored.getDoorOverride()); assertFalse(restored.isDriver(),"Fresh passenger proof cannot re-grant a removed key's driver authority");
            assertFalse(restored.manualAccelerate()); assertFalse(restored.manualBrake()); assertFalse(restored.manualToggleDoors()); assertFalse(restored.manualToggleAto());
            Vector seat = passengerWorld(train,0,0,1,0); fixture.player.setPosition(seat.x,seat.y,seat.z); core.poll(); drainPassengerQueue(core);
            assertFalse(appliedPassenger(train,core.player.getUuid()).getDoorOverride());
            assertFalse(appliedPassenger(train,core.player.getUuid()).isDriver(),"Later cleanup cannot restore the pre-recovery driver bit");
        } finally { RidingPermissions.clear(); }
    }
    private static void queuePassenger(CoreFixture core, UpdateVehicleRidingEntities update) {
        core.simulator.sendMessageC2S(new org.mtr.core.servlet.QueueObject(org.mtr.core.servlet.OperationProcessor.UPDATE_RIDING_ENTITIES,update,null,null));
    }
    private static org.mtr.core.servlet.MessageQueue<org.mtr.core.servlet.QueueObject> passengerQueue(CoreFixture core) throws Exception {
        java.lang.reflect.Field field = org.mtr.core.simulation.Simulator.class.getDeclaredField("messageQueueC2S"); field.setAccessible(true);
        return (org.mtr.core.servlet.MessageQueue<org.mtr.core.servlet.QueueObject>)field.get(core.simulator);
    }
    private static void drainPassengerQueue(CoreFixture core) throws Exception {
        org.mtr.core.servlet.MessageQueue<org.mtr.core.servlet.QueueObject> queue = passengerQueue(core);
        core.simulator.run(() -> queue.process(row -> org.mtr.core.servlet.OperationProcessor.process(row.key,row.data,core.simulator)));
        core.drain();
    }
    private static int passengerQueueSize(CoreFixture core) throws Exception {
        java.lang.reflect.Field field = org.mtr.core.servlet.MessageQueue.class.getDeclaredField("linkedBlockingDeque"); field.setAccessible(true);
        return ((java.util.Collection<?>)field.get(passengerQueue(core))).size();
    }
    private static VehicleRidingEntity appliedPassenger(Vehicle train, UUID uuid) {
        final VehicleRidingEntity[] found = {null}; train.vehicleExtraData.iterateRidingEntities(row -> { if (uuid.equals(row.uuid)) found[0] = row; }); return found[0];
    }
    private static JsonObject passengerProfile(String id) throws Exception {
        for (JsonElement entry : cableBundledJson("mtr_custom_resources.json").getAsJsonArray("vehicles"))
            if (id.equals(entry.getAsJsonObject().get("id").getAsString())) return entry.getAsJsonObject();
        throw new AssertionError("Missing original stock " + id);
    }
    private static Vehicle passengerVehicle(CoreFixture core, JsonObject profile, TransportMode mode, String id) throws Exception {
        Data data = core.simulator; Position p1 = new Position(0,64,0), p2 = new Position(0,64,300);
        Rail rail = Rail.newRail(p1, Angle.S, p2, Angle.N, Rail.Shape.QUADRATIC, 0, new ObjectArrayList<>(), 80, 80, true, false, false, false, false, mode);
        PathData path = new PathData(rail,51,0,0,p1,p2);
        JsonObject extra = new JsonObject(); extra.addProperty("sidingId",51); extra.addProperty("depotId",52); extra.addProperty("totalVehicleLength",profile.get("length").getAsDouble());
        JsonArray cars = new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar(id,profile.get("length").getAsDouble(),profile.get("width").getAsDouble(),profile.get("bogie1Position").getAsDouble(),profile.get("bogie2Position").getAsDouble(),0,0))); extra.add("vehicleCars",cars);
        JsonArray paths = new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        VehicleExtraData details = new VehicleExtraData(new JsonReader(extra)); PathData.writePathCache(details.immutablePath,data,mode);
        JsonObject definition = new JsonObject(); definition.addProperty("id",77); definition.addProperty("transportMode",mode.name()); definition.addProperty("railProgress",100);
        Siding seed = new Siding(p1,p2,300,mode,data); JsonObject sidingJson = Utilities.getJsonObjectFromData(seed); sidingJson.addProperty("id",51);
        Siding siding = new Siding(new JsonReader(sidingJson),data); core.simulator.sidings.add(siding); core.simulator.sidingIdMap.put(51,siding);
        Vehicle vehicle = new Vehicle(details,siding,new JsonReader(definition),data);
        java.lang.reflect.Field vehicles = Siding.class.getDeclaredField("vehicles"); vehicles.setAccessible(true); ((java.util.Collection<Vehicle>)vehicles.get(siding)).add(vehicle);
        return vehicle;
    }
    private static void passengerSample(CoreFixture core, Vehicle train) throws Exception {
        VehicleLiftResponse response = new VehicleLiftResponse(core.player.getUuid(),core.simulator); response.addVehicleToUpdate(new VehicleUpdate(train,train.vehicleExtraData));
        RidingPermissions.observe(new ServerWorld(core.fixture.world),response,CoreFixture::path); core.poll(); core.drain(); core.poll();
    }
    private static Vector passengerWorld(Vehicle train, int carNumber, double x, double y, double z) {
        VehicleCar car = train.getVehicleCarsAndPositions().get(carNumber).left();
        Vector first = Vector.getAverage(train.getVehicleCarsAndPositions().get(carNumber).right().get(0).left(),train.getVehicleCarsAndPositions().get(carNumber).right().get(0).right());
        Vector second = Vector.getAverage(train.getVehicleCarsAndPositions().get(carNumber).right().get(1).left(),train.getVehicleCarsAndPositions().get(carNumber).right().get(1).right());
        double yaw = Math.atan2(second.x-first.x,second.z-first.z);
        Vector center = Vector.getAverage(first,second);
        double pivot = (car.getBogie1Position()+car.getBogie2Position())/2;
        return new Vector(x,y,z-pivot).rotateY(yaw).add(center);
    }
    private static JsonObject passengerRequest(CoreFixture core, Vehicle train, double x, double y, double z) {
        JsonObject packet = json(request(core.player.getUuid(),51,train.getId(),0));
        JsonObject row = packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
        row.addProperty("x",x); row.addProperty("y",y); row.addProperty("z",z);
        for (String key : new String[]{"isDriver","manualAccelerate","manualBrake","manualToggleDoors","manualToggleAto","doorOverride"}) row.addProperty(key,true);
        return packet;
    }
    private static JsonObject passengerRow(UpdateVehicleRidingEntities update) {
        return Utilities.getJsonObjectFromData(update).getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
    }

    public void originalNgongPingFloorBoardsWithoutExpandingOtherCabinsOrControls() throws Exception {
        JsonObject catalog = cableBundledJson("mtr_custom_resources.json"), stock = null;
        for (JsonElement entry : catalog.getAsJsonArray("vehicles")) {
            JsonObject vehicle = entry.getAsJsonObject();
            if ("ngong_ping_360_normal_red_lht".equals(vehicle.get("id").getAsString())) stock = vehicle;
        }
        assertNotNull(stock); assertEquals("CABLE_CAR", stock.get("transportMode").getAsString());
        JsonObject model = stock.getAsJsonArray("models").get(0).getAsJsonObject();
        assertEquals("mtr:properties/vehicle/ngong_ping_360_lht.json", model.get("modelPropertiesResource").getAsString());
        JsonObject properties = cableBundledJson(model.get("modelPropertiesResource").getAsString().substring(4));
        JsonObject mesh = cableBundledJson(model.get("modelResource").getAsString().substring(4));
        assertEquals(-5, properties.get("modelYOffset").getAsDouble(), 0);
        String floorUuid = null;
        for (JsonElement entry : mesh.getAsJsonArray("outliner")) if (entry.isJsonObject()) {
            JsonObject group = entry.getAsJsonObject();
            if ("floor".equals(group.get("name").getAsString())) floorUuid = group.getAsJsonArray("children").get(0).getAsString();
        }
        assertNotNull(floorUuid); boolean plane = false;
        for (JsonElement entry : mesh.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            if (floorUuid.equals(element.get("uuid").getAsString())) {
                assertEquals(0, element.getAsJsonArray("from").get(1).getAsDouble(), 0);
                assertEquals(0, element.getAsJsonArray("to").get(1).getAsDouble(), 0); plane = true;
            }
        }
        assertTrue(plane, "Read the selected original FLOOR plane, not a synthetic client hint");
        final JsonObject profile = stock;
        for (int boundary = 0; boundary < 7; boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture(); CoreFixture core = new CoreFixture(fixture)) {
                TransportMode mode = boundary == 3 ? TransportMode.TRAIN : TransportMode.CABLE_CAR;
                String id = boundary == 4 ? "unknown_addon_cable" : profile.get("id").getAsString();
                Vehicle cabin = originalCableVehicle(core, profile, mode, id);
                Vector center = Vector.getAverage(cabin.getVehicleCarsAndPositions().get(0).right().get(0).left(), cabin.getVehicleCarsAndPositions().get(0).right().get(0).right());
                VehicleLiftResponse response = new VehicleLiftResponse(core.player.getUuid(), core.simulator);
                response.addVehicleToUpdate(new VehicleUpdate(cabin, cabin.vehicleExtraData));
                RidingPermissions.observe(new ServerWorld(fixture.world), response, CoreFixture::path); core.poll(); core.drain(); core.poll();
                double y = boundary == 1 ? -6.5001 : boundary == 2 ? 2.5001 : -5;
                fixture.player.setPosition(center.x + (boundary == 5 ? 2.5001 : 0), center.y + y, center.z);
                JsonObject packet = json(request(core.player.getUuid(), 51, cabin.getId(), 0));
                JsonObject rider = packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject(); rider.addProperty("y", y);
                if (boundary == 6) rider.addProperty("clientId", UUID.randomUUID().toString());
                for (String key : new String[]{"isDriver","manualAccelerate","manualBrake","manualToggleDoors","manualToggleAto","doorOverride"}) rider.addProperty(key, true);
                if (boundary == 0) {
                    // Pre-fix RED must be this real native player at Y7 being rejected by bounds starting at Y10.5.
                    JsonObject accepted = Utilities.getJsonObjectFromData(RidingPermissions.validate(core.player, new JsonReader(packet), false)).getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
                    assertEquals(-5, accepted.get("y").getAsDouble(), 0, "Preserve the original local tuple");
                    assertTrue(PlayerPhysics.authorized(fixture.player));
                    for (String key : new String[]{"isDriver","manualAccelerate","manualBrake","manualToggleDoors","manualToggleAto","doorOverride"}) assertFalse(accepted.has(key) && accepted.get(key).getAsBoolean(), key);
                    core.player.setNoGravity(true); core.player.setNoClipMapped(true); core.player.setInTeleportationState(true);
                    assertTrue(RidingPermissions.refresh(core.player), "Shifted bounds must survive normal lease refresh");
                    fixture.world.func_82738_a(41); assertFalse(RidingPermissions.refresh(core.player), "Metadata cannot renew server freshness");
                } else {
                    assertThrows(RidingPermissions.Rejected.class, () -> RidingPermissions.validate(core.player, new JsonReader(packet), false), "boundary " + boundary);
                    assertFalse(PlayerPhysics.authorized(fixture.player));
                }
            } finally { RidingPermissions.clear(); }
        }
    }
    private static JsonObject cableBundledJson(String path) throws Exception {
        try (java.io.InputStream input = RidingPermissionScenarios.class.getResourceAsStream("/assets/mtr/" + path)) {
            assertNotNull(input, "Original bundled resource " + path);
            try (java.io.Reader reader = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
                return new JsonParser().parse(reader).getAsJsonObject();
            }
        }
    }
    private static Vehicle originalCableVehicle(CoreFixture core, JsonObject profile, TransportMode mode, String id) throws Exception {
        Data data = core.simulator; Position p1 = new Position(0,12,0), p2 = new Position(0,12,16);
        Rail rail = Rail.newRail(p1, Angle.S, p2, Angle.N, Rail.Shape.QUADRATIC, 0, new ObjectArrayList<>(), 2, 2, true, false, false, false, false, mode);
        PathData path = new PathData(rail,51,1,0,p1,p2);
        JsonObject extra = new JsonObject(); extra.addProperty("sidingId",51); extra.addProperty("depotId",52); extra.addProperty("totalVehicleLength",profile.get("length").getAsDouble());
        JsonArray cars = new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar(id,profile.get("length").getAsDouble(),profile.get("width").getAsDouble(),profile.get("bogie1Position").getAsDouble(),profile.get("bogie2Position").getAsDouble(),profile.has("couplingPadding1")?profile.get("couplingPadding1").getAsDouble():0,profile.has("couplingPadding2")?profile.get("couplingPadding2").getAsDouble():0))); extra.add("vehicleCars",cars);
        JsonArray paths = new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        VehicleExtraData details = new VehicleExtraData(new JsonReader(extra)); PathData.writePathCache(details.immutablePath,data,mode);
        JsonObject definition = new JsonObject(); definition.addProperty("id",77); definition.addProperty("transportMode",mode.name()); definition.addProperty("railProgress",8);
        Siding seed = new Siding(p1,p2,16,mode,data); JsonObject sidingJson = Utilities.getJsonObjectFromData(seed); sidingJson.addProperty("id",51);
        Siding siding = new Siding(new JsonReader(sidingJson),data); core.simulator.sidings.add(siding);
        Vehicle vehicle = new Vehicle(details,siding,new JsonReader(definition),data);
        java.lang.reflect.Field vehicles = Siding.class.getDeclaredField("vehicles"); vehicles.setAccessible(true); ((java.util.Collection<Vehicle>)vehicles.get(siding)).add(vehicle);
        return vehicle;
    }

    private static Vector vehicleCenter(Vehicle vehicle) {
        Vector first=Vector.getAverage(vehicle.getVehicleCarsAndPositions().get(0).right().get(0).left(),vehicle.getVehicleCarsAndPositions().get(0).right().get(0).right());
        Vector last=Vector.getAverage(vehicle.getVehicleCarsAndPositions().get(0).right().get(1).left(),vehicle.getVehicleCarsAndPositions().get(0).right().get(1).right());
        return Vector.getAverage(first,last);
    }

    public void invalidCoreSourceCannotReuseAnExistingWorldBinding() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0);
            org.mtr.core.simulation.Simulator wrong=new org.mtr.core.simulation.Simulator("wrong/dimension",new String[]{"wrong/dimension"},java.nio.file.Files.createTempDirectory("mtr-riding-wrong-core"),false);
            try {
                for(Data invalid:new Data[]{new Data(){},wrong}) {
                    core.observe(lift); RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false);
                    VehicleLiftResponse response=new VehicleLiftResponse(core.player.getUuid(),invalid); response.addLiftToUpdate(lift);
                    RidingPermissions.observe(new ServerWorld(fixture.world),response,CoreFixture::path);
                    assertFalse(PlayerPhysics.authorized(fixture.player),"An invalid source must not inherit a valid world's old lease");
                    assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
                }
            } finally { wrong.stop(); }
        } finally { RidingPermissions.clear(); }
    }
    public void coreReadExceptionsReleasePendingWithoutRefreshingOldJson() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); core.simulator.lifts.add(lift); fixture.player.setPosition(0,64,0); core.observe(lift);
            core.simulator.clients.iterator().next().setPositionAndUpdateRadius(null,128);
            fixture.world.func_82738_a(104); core.poll(); core.drain(); core.poll();
            assertEquals(0,core.queuedCount());
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false));
            core.simulator.clients.iterator().next().setPositionAndUpdateRadius(new Position(0,64,0),128);
            fixture.world.func_82738_a(110); core.poll(); core.drain(); core.poll();
            assertDoesNotThrow(()->RidingPermissions.validate(core.player,request(core.player.getUuid(),0,lift.getId(),0),false),
                "A failed Core read must release the per-UUID gate for a later actual read");
        } finally { RidingPermissions.clear(); }
    }

    public void freshUpdateCannotReviveAnExpiredCabinAtItsOldPosition() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            fixture.player.setPosition(0,64,0); observe(fixture,lift);
            fixture.world.func_82738_a(61); lift.setDimensions(3,3,3,60,0,0); observe(fixture,lift);
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false),
                "A fresh update must not revive a cabin whose previous authoritative position expired long ago");
            assertFalse(PlayerPhysics.authorized(fixture.player));
            fixture.player.setPosition(60,64,0);
            assertNotNull(RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false));
            assertTrue(PlayerPhysics.authorized(fixture.player),"The current authoritative cabin remains usable");
        } finally { RidingPermissions.clear(); }
    }
    public void ordinaryDismountCannotResetFallDistanceOrAnotherModsFlags() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            fixture.player.fallDistance=7; fixture.player.noClip=true;
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(player.getUuid(),0,0,-1),true),
                "A non-rider must not reach Init.updatePlayer(false), which resets fall distance");
            assertEquals(7,fixture.player.fallDistance,0); assertTrue(fixture.player.noClip);
        } finally { RidingPermissions.clear(); }
    }
    public void rejectedOriginalPacketDoesNotEnqueueOrResetOrdinaryPlayerPhysics() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            int[] forwarded={0}; org.mtr.mod.Init.forward=(world,request)->forwarded[0]++;
            fixture.player.fallDistance=7; fixture.player.noClip=true; // Simulate an independent mod's ordinary-player flag.
            java.lang.reflect.Constructor<org.mtr.mod.packet.PacketUpdateVehicleRidingEntities> constructor=
                org.mtr.mod.packet.PacketUpdateVehicleRidingEntities.class.getDeclaredConstructor(String.class,boolean.class);
            constructor.setAccessible(true);
            String invalid=json(request(UUID.randomUUID(),0,12345,0)).toString();
            org.mtr.mod.packet.PacketUpdateVehicleRidingEntities packet=constructor.newInstance(invalid,false);
            assertDoesNotThrow(()->packet.runServer(new org.mtr.mapping.holder.MinecraftServer(fixture.server),player));
            assertEquals(0,forwarded[0],"The actual packet data factory must reject before Init.sendMessageC2S can forward to core");
            assertEquals(7,fixture.player.fallDistance,0,"Rejected packets must not become a no-fall exploit");
            assertTrue(fixture.player.noClip,"Revoking absent MTR state must not alter another mod's flag");
        } finally { org.mtr.mod.Init.forward=null; RidingPermissions.clear(); }
    }
    public void actualLiftWithSidingZeroBoardsMovesAndDismounts() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            fixture.player.setPosition(0,64,0);
            observe(fixture,lift);
            UpdateVehicleRidingEntities bound=RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false);
            assertNotNull(bound); assertTrue(PlayerPhysics.authorized(fixture.player));
            player.setNoGravity(true); player.setInTeleportationState(true); player.setNoClipMapped(true);
            assertTrue(fixture.player.noClip);
            lift.setDimensions(3,3,3,0,1,0); fixture.world.func_82738_a(1);
            observe(fixture,lift); fixture.player.setPosition(0,65,0);
            assertTrue(RidingPermissions.refresh(player));
            assertNotNull(RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),-1),true));
            assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
        } finally { RidingPermissions.clear(); }
    }
    public void forgedRemoteMissingAndMalformedRequestsNeverGrantOrReachCore() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); ServerPlayerEntity player=new ServerPlayerEntity(fixture.player); fixture.player.setPosition(0,64,0); observe(fixture,lift);
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(UUID.randomUUID(),0,lift.getId(),0),false));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId()+1,0),false));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),1),false));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),-1),false));
            JsonObject multiple=json(request(player.getUuid(),0,lift.getId(),0));
            multiple.getAsJsonArray("ridingEntities").add(multiple.getAsJsonArray("ridingEntities").get(0));
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,new JsonReader(multiple),false));
            JsonObject invalid=json(request(player.getUuid(),0,lift.getId(),0)); invalid.getAsJsonArray("ridingEntities").get(0).getAsJsonObject().addProperty("x",Double.NaN);
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,new JsonReader(invalid),false));
            fixture.player.setPosition(1000,64,0);
            assertThrows(RidingPermissions.Rejected.class,()->RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false));
            assertFalse(PlayerPhysics.authorized(fixture.player));
        } finally { RidingPermissions.clear(); }
    }
    public void packetsCannotRefreshStaleGeometryAndRemovalRevokesImmediately() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Lift lift=lift(core.simulator); ServerPlayerEntity player=new ServerPlayerEntity(fixture.player); fixture.player.setPosition(0,64,0); observe(fixture,lift);
            RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false);
            fixture.world.func_82738_a(39);
            RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false);
            fixture.world.func_82738_a(61);
            assertFalse(RidingPermissions.refresh(player),"An untrusted repeated packet must not renew server snapshot freshness");
            assertFalse(PlayerPhysics.authorized(fixture.player));
            observe(fixture,lift); RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false);
            RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(player.getUuid(),core.simulator),(a,b)->ObjectArrayList.of(new Vector(0,64,0),new Vector(0,80,0)));
            assertFalse(PlayerPhysics.authorized(fixture.player),"A removed lift must immediately revoke the old lease");
            observe(fixture,lift); RidingPermissions.validate(player,request(player.getUuid(),0,lift.getId(),0),false);
            fixture.player.dimension=1;
            assertFalse(RidingPermissions.refresh(player)); fixture.player.dimension=0;
        } finally { RidingPermissions.clear(); }
    }
    public void actualVehicleGeometryAndServerHeldKeysAuthorizeOnlyTheirOwnControls() throws Exception {
        try(ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle vehicle=vehicle(core); ServerPlayerEntity player=new ServerPlayerEntity(fixture.player);
            Vector first=Vector.getAverage(vehicle.getVehicleCarsAndPositions().get(0).right().get(0).left(),vehicle.getVehicleCarsAndPositions().get(0).right().get(0).right());
            Vector last=Vector.getAverage(vehicle.getVehicleCarsAndPositions().get(0).right().get(1).left(),vehicle.getVehicleCarsAndPositions().get(0).right().get(1).right());
            Vector center=Vector.getAverage(first,last); fixture.player.setPosition(center.x,center.y,center.z);
            VehicleLiftResponse response=new VehicleLiftResponse(player.getUuid(),core.simulator); response.addVehicleToUpdate(new VehicleUpdate(vehicle,vehicle.vehicleExtraData));
            RidingPermissions.observe(new ServerWorld(fixture.world),response,(a,b)->new ObjectArrayList<>()); core.poll(); core.drain(); core.poll();
            JsonObject packet=json(request(player.getUuid(),51,vehicle.getId(),0));
            JsonObject rider=packet.getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
            for(String key:new String[]{"isDriver","manualAccelerate","manualBrake","manualToggleDoors","manualToggleAto","doorOverride"}) rider.addProperty(key,true);
            JsonObject passenger=Utilities.getJsonObjectFromData(RidingPermissions.validate(player,new JsonReader(packet),false)).getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
            assertFalse(passenger.get("isDriver").getAsBoolean()); assertFalse(passenger.get("manualAccelerate").getAsBoolean()); assertFalse(passenger.get("doorOverride").getAsBoolean());
            assertEquals(player.getUuid().toString(),passenger.get("clientId").getAsString());
            fixture.player.inventory.setInventorySlotContents(0,new net.minecraft.item.ItemStack(new ItemCreativeDriverKey(new ItemSettings())));
            JsonObject driver=Utilities.getJsonObjectFromData(RidingPermissions.validate(player,new JsonReader(packet),false)).getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
            assertTrue(driver.get("isDriver").getAsBoolean()); assertTrue(driver.get("manualAccelerate").getAsBoolean()); assertTrue(driver.get("manualToggleDoors").getAsBoolean());
            ItemDepotDriverKey expired=new ItemDepotDriverKey(new ItemSettings(),true,true,true,0);
            net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(expired); stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            stack.getTagCompound().setLong("depot_id",52); stack.getTagCompound().setLong("expiry_time",1);
            fixture.player.inventory.setInventorySlotContents(0,stack);
            JsonObject denied=Utilities.getJsonObjectFromData(RidingPermissions.validate(player,new JsonReader(packet),false)).getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
            assertFalse(denied.get("isDriver").getAsBoolean());
            stack.getTagCompound().setLong("expiry_time",System.currentTimeMillis()+60000);
            stack.getTagCompound().setLong("depot_id",999);
            JsonObject wrongDepot=Utilities.getJsonObjectFromData(RidingPermissions.validate(player,new JsonReader(packet),false)).getAsJsonArray("ridingEntities").get(0).getAsJsonObject();
            assertFalse(wrongDepot.get("isDriver").getAsBoolean());
        } finally { RidingPermissions.clear(); }
    }
    public void pendingBoatCorePoseDefersLatestNativeC03UntilFreshNumericRead() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle boat=boardSmallBoat(core);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                RideHandler handler=openRideHandler(fixture,channel);
                fixture.world.func_82738_a(3082); core.poll();
                assertEquals(1,core.queuedCount());
                moveBoat(boat,58.52659266666665);
                handler.processPlayer(position(8.637499988079071,4,57.35159266666665));
                assertEquals(0,handler.corrections,"A selected rider must wait for its pending numeric Core read instead of receiving a premature native S08");
                assertEquals(51.405387777777804,fixture.player.posZ,1E-6,"A future client position must remain unapplied until authoritative evidence arrives");
                handler.processPlayer(position(8.637499988079071,4,57.5));
                handler.processPlayer(new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(90,0,false));
                assertEquals(51.405387777777804,fixture.player.posZ,1E-6);
                assertEquals(1,core.queuedCount(),"A burst must retain one pending query and one latest positional C03");
                core.drain(); fixture.world.func_82738_a(3083); core.poll();
                assertEquals(0,handler.corrections);
                assertEquals(57.5,fixture.player.posZ,1E-6,"The latest positional packet must replay through the transformed native handler");
                assertEquals(90,fixture.player.rotationYaw,0,"A look-only packet must not erase the held position");
                assertTrue(PlayerPhysics.authorized(fixture.player)); assertTrue(fixture.player.noClip);
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }

    public void completedBoatCoreReadIsAcceptedAtNativeMovementBoundary() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle boat=boardSmallBoat(core);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                RideHandler handler=openRideHandler(fixture,channel);
                fixture.world.func_82738_a(3082); core.poll(); moveBoat(boat,58.52659266666665); core.drain();
                // The detached read completed after the last World END poll, before native networkTick.
                handler.processPlayer(position(8.637499988079071,4,57.35159266666665));
                assertEquals(0,handler.corrections);
                assertEquals(57.35159266666665,fixture.player.posZ,1E-6);
                assertTrue(PlayerPhysics.authorized(fixture.player));
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }

    public void deferredBoatMovementCannotEscapeFreshBoundsOrResurrectAfterLifecycle() throws Exception {
        for (int boundary=0;boundary<7;boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                Vehicle boat=boardSmallBoat(core);
                io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                try {
                    RideHandler handler=openRideHandler(fixture,channel);
                    fixture.world.func_82738_a(3082); core.poll(); moveBoat(boat,58.52659266666665);
                    handler.processPlayer(position(8.637499988079071,4,boundary==0?1000:57.35159266666665));
                    assertEquals(51.405387777777804,fixture.player.posZ,1E-6);
                    assertEquals(0,handler.corrections);
                    if (boundary==1) { fixture.world.func_82738_a(3086); core.poll(); }
                    if (boundary==2) RidingPermissions.disconnect(core.player);
                    if (boundary==3) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.world));
                    if (boundary==4) fixture.player.dimension=1;
                    if (boundary==5) new RideHandler(fixture);
                    if (boundary==6) RidingPermissions.observe(new ServerWorld(fixture.world),new VehicleLiftResponse(core.player.getUuid(),core.simulator),CoreFixture::path);
                    core.drain(); fixture.world.func_82738_a(boundary==1?3087:3083); core.poll();
                    assertEquals(51.405387777777804,fixture.player.posZ,1E-6,"An unverified packet must never become movement, boundary "+boundary);
                    assertFalse(PlayerPhysics.authorized(fixture.player),"No resurrection after rejection/lifecycle, boundary "+boundary);
                    assertFalse(fixture.player.noClip);
                    if (boundary<=1) assertEquals(1,handler.corrections,"Fresh bounds rejection or timeout must use the ordinary correction");
                } finally { channel.finish(); }
            } finally { RidingPermissions.clear(); }
        }
    }

    public void heldBoatC06DoesNotOverwriteNewerNativeC05Look() throws Exception {
        heldBoatLookOrdering(true);
    }
    public void heldBoatC06LookSurvivesNewerPositionOnlyC04() throws Exception {
        heldBoatLookOrdering(false);
    }
    private static void heldBoatLookOrdering(boolean newerLook) throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle boat=boardSmallBoat(core);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                RideHandler handler=openRideHandler(fixture,channel);
                fixture.world.func_82738_a(3082); core.poll(); moveBoat(boat,58.52659266666665);
                handler.processPlayer(new net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook(
                    8.637499988079071,4,5.62,57.35159266666665,newerLook?0:90,newerLook?10:20,false));
                if (newerLook) handler.processPlayer(new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(90,20,false));
                else handler.processPlayer(position(8.637499988079071,4,57.5));
                assertEquals(51.405387777777804,fixture.player.posZ,1E-6,"Held client positions must remain unapplied");
                assertEquals(0,handler.corrections);
                core.drain(); fixture.world.func_82738_a(3083); core.poll();
                assertEquals(newerLook?57.35159266666665:57.5,fixture.player.posZ,1E-6);
                assertEquals(90,fixture.player.rotationYaw,0,"Deferred positional replay must preserve the latest native look");
                assertEquals(20,fixture.player.rotationPitch,0);
                assertEquals(0,handler.corrections); assertTrue(PlayerPhysics.authorized(fixture.player));
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }
    public void authenticatedBoatRequestsNumericPoseBetweenRegularPolls() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            Vehicle boat=boardSmallBoat(core);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                RideHandler handler=openRideHandler(fixture,channel);
                assertEquals(3081,fixture.world.getTotalWorldTime());
                assertEquals(0,core.queuedCount(),"The previous Core read completed; the next regular poll is not due until3082");
                moveBoat(boat,58.52659266666665);
                handler.processPlayer(position(8.637499988079071,4,57.35159266666665));
                assertEquals(0,handler.corrections,"An authenticated rider needs a fresh numeric query when caught between the regular polls");
                assertEquals(51.405387777777804,fixture.player.posZ,1E-6);
                assertEquals(1,core.queuedCount(),"Native movement must request one detached numeric read");
                handler.processPlayer(position(8.637499988079071,4,57.5));
                assertEquals(1,core.queuedCount(),"Further positional packets must reuse that read");
                core.drain(); fixture.world.func_82738_a(3082); core.poll();
                assertEquals(57.5,fixture.player.posZ,1E-6);
                assertEquals(0,handler.corrections); assertTrue(PlayerPhysics.authorized(fixture.player));
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }

    public void validCabinWalkoffHandsC03ToOrdinaryMovementWithoutBounce() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
            boardSmallBoat(core);
            io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            try {
                HandoffHandler handler=handoffHandler(fixture,channel); prepareHandoff(core);
                handler.processPlayer(position(11.11101867887199,4,51.405387777777804));
                assertEquals(0,handler.corrections,"A passable step beyond finite riding authority must continue through ordinary native movement");
                assertEquals(11.11101867887199,fixture.player.posX,1E-6);
                assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
                assertEquals(.08,PlayerPhysics.gravity(.08,fixture.player),0);
                assertEquals(100,PlayerPhysics.movementLimit(handler),0); assertFalse(PlayerPhysics.allowFlight(false,handler));
                assertFalse(RidingPermissions.refresh(core.player),"A lingering Core rider cannot restore privileges after walkoff");
                fixture.world.func_82738_a(3084); core.poll(); core.drain(); core.poll();
                assertFalse(RidingPermissions.refresh(core.player));
                assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
                handler.processPlayer(position(11.2,4,51.405387777777804));
                assertEquals(0,handler.corrections); assertEquals(11.2,fixture.player.posX,1E-6);
                assertNull(handler.kick);
            } finally { channel.finish(); }
        } finally { RidingPermissions.clear(); }
    }
    public void cabinWalkoffRetainsNativeSurvivalWallSpeedAndFiniteChecks() throws Exception {
        for (int boundary=0;boundary<3;boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                boardSmallBoat(core);
                io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                try {
                    HandoffHandler handler=handoffHandler(fixture,channel); prepareHandoff(core);
                    double before=fixture.player.posX;
                    if (boundary==0) for (int y=4;y<=5;y++) fixture.world.setBlock(11,y,51,net.minecraft.init.Blocks.stone);
                    handler.processPlayer(position(boundary==1?1000:boundary==2?Double.NaN:11.11101867887199,4,51.405387777777804));
                    assertEquals(before,fixture.player.posX,1E-6,"Wall/far/nonfinite control "+boundary);
                    if (boundary==2) assertNotNull(handler.kick,"Nonfinite movement must still be kicked before any handoff");
                    else {
                        assertEquals(1,handler.corrections,"Native survival wall/speed checks must still correct the packet");
                        assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
                        assertFalse(RidingPermissions.refresh(core.player));
                    }
                } finally { channel.finish(); }
            } finally { RidingPermissions.clear(); }
        }
    }
    public void callbackRevocationAndStaleHandlersCannotUseOrdinaryHandoff() throws Exception {
        java.lang.reflect.Field field=PlayerPhysics.class.getDeclaredField("movementDeferrer"); field.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.function.BiPredicate<net.minecraft.network.NetHandlerPlayServer,net.minecraft.network.play.client.C03PacketPlayer> original=
            (java.util.function.BiPredicate<net.minecraft.network.NetHandlerPlayServer,net.minecraft.network.play.client.C03PacketPlayer>)field.get(null);
        for (int boundary=0;boundary<4;boundary++) {
            try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture(); CoreFixture core=new CoreFixture(fixture)) {
                boardSmallBoat(core);
                io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
                try {
                    HandoffHandler handler=handoffHandler(fixture,channel); prepareHandoff(core);
                    if (boundary==0) PlayerPhysics.setMovementDeferrer((nativeHandler,packet) -> {
                        PlayerPhysics.revoke(nativeHandler.playerEntity); return false;
                    });
                    if (boundary==1) new RideHandler(fixture);
                    if (boundary==2) fixture.player.worldObj=new org.mtr.mapping.BlockPlacementScenarios.MemoryWorld();
                    if (boundary==3) channel.close();
                    double before=fixture.player.posX;
                    handler.processPlayer(position(11.11101867887199,4,51.405387777777804));
                    assertEquals(before,fixture.player.posX,1E-6,"A lifecycle/identity failure cannot continue as a legal walkoff, boundary "+boundary);
                    assertEquals(1,handler.corrections);
                    assertFalse(PlayerPhysics.authorized(fixture.player)); assertFalse(fixture.player.noClip);
                    assertFalse(RidingPermissions.refresh(core.player));
                } finally {
                    fixture.player.worldObj=fixture.world;
                    PlayerPhysics.setMovementDeferrer(original); channel.finish();
                }
            } finally { RidingPermissions.clear(); }
        }
    }
    private static void prepareHandoff(CoreFixture core) throws Exception {
        ServerLifecycleScenarios.ServerFixture fixture=core.fixture;
        fixture.player.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
        assertFalse(fixture.server.isSinglePlayer(),"Use a non-owner fixture so vanilla's SP-owner speed exemption cannot hide a failure");
        assertFalse(fixture.player.capabilities.isCreativeMode);
        for (int x=5;x<=13;x++) for (int z=48;z<=54;z++) fixture.world.setBlock(x,3,z,net.minecraft.init.Blocks.stone);
        fixture.world.func_82738_a(3082); core.poll(); core.drain();
        // Core has completed a fresh static cabin read before the C03 entry consumes it.
    }
    private static HandoffHandler handoffHandler(ServerLifecycleScenarios.ServerFixture fixture,io.netty.channel.embedded.EmbeddedChannel channel) throws Exception {
        net.minecraft.network.NetworkManager manager=new net.minecraft.network.NetworkManager(false);
        java.lang.reflect.Field field=net.minecraft.network.NetworkManager.class.getDeclaredField("channel"); field.setAccessible(true); field.set(manager,channel);
        return new HandoffHandler(fixture,manager);
    }
    private static final class HandoffHandler extends net.minecraft.network.NetHandlerPlayServer {
        int corrections; String kick;
        HandoffHandler(ServerLifecycleScenarios.ServerFixture fixture,net.minecraft.network.NetworkManager manager) { super(fixture.server,manager,fixture.player); }
        @Override public void sendPacket(net.minecraft.network.Packet packet) {}
        @Override public void setPlayerLocation(double x,double y,double z,float yaw,float pitch) { corrections++; super.setPlayerLocation(x,y,z,yaw,pitch); }
        @Override public void kickPlayerFromServer(String reason) { kick=reason; }
    }

    private static RideHandler openRideHandler(ServerLifecycleScenarios.ServerFixture fixture,io.netty.channel.embedded.EmbeddedChannel channel) throws Exception {
        net.minecraft.network.NetworkManager manager=new net.minecraft.network.NetworkManager(false);
        java.lang.reflect.Field field=net.minecraft.network.NetworkManager.class.getDeclaredField("channel"); field.setAccessible(true); field.set(manager,channel);
        return new RideHandler(fixture,manager);
    }
    private static Vehicle boardSmallBoat(CoreFixture core) throws Exception {
        Data data=core.simulator; Position first=new Position(8,4,0),last=new Position(8,4,300);
        Rail rail=Rail.newRail(first,Angle.S,last,Angle.N,Rail.Shape.QUADRATIC,0,new ObjectArrayList<>(),80,80,true,false,false,false,false,TransportMode.BOAT);
        PathData path=new PathData(rail,51,0,0,first,last);
        JsonObject extra=new JsonObject(); extra.addProperty("sidingId",51); extra.addProperty("depotId",52); extra.addProperty("totalVehicleLength",2);
        JsonArray cars=new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar("oak_boat_small",2,2,-1,1,0,0))); extra.add("vehicleCars",cars);
        JsonArray paths=new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        VehicleExtraData details=new VehicleExtraData(new JsonReader(extra)); PathData.writePathCache(details.immutablePath,data,TransportMode.BOAT);
        Siding seed=new Siding(first,last,300,TransportMode.BOAT,data);
        JsonObject sidingJson=Utilities.getJsonObjectFromData(seed); sidingJson.addProperty("id",51);
        Siding siding=new Siding(new JsonReader(sidingJson),data); core.simulator.sidings.add(siding);
        JsonObject definition=new JsonObject(); definition.addProperty("id",77); definition.addProperty("transportMode","BOAT"); definition.addProperty("railProgress",53.471287111111096);
        Vehicle boat=new Vehicle(details,siding,new JsonReader(definition),data);
        java.lang.reflect.Field vehicles=Siding.class.getDeclaredField("vehicles"); vehicles.setAccessible(true); ((java.util.Collection<Vehicle>)vehicles.get(siding)).add(boat);
        moveBoat(boat,52.971037111111094);
        core.fixture.player.setPosition(8.637499988079071,4,51.405387777777804);
        core.fixture.world.func_82738_a(3080);
        VehicleLiftResponse response=new VehicleLiftResponse(core.player.getUuid(),core.simulator); response.addVehicleToUpdate(new VehicleUpdate(boat,boat.vehicleExtraData));
        RidingPermissions.observe(new ServerWorld(core.fixture.world),response,CoreFixture::path); core.poll(); core.drain();
        core.fixture.world.func_82738_a(3081); core.poll();
        JsonObject riding=json(request(core.player.getUuid(),51,boat.getId(),0));
        JsonObject row=riding.getAsJsonArray("ridingEntities").get(0).getAsJsonObject(); row.addProperty("x",-.13749998807907104); row.addProperty("y",0); row.addProperty("z",.375);
        RidingPermissions.validate(core.player,new JsonReader(riding),false);
        core.player.setInTeleportationState(true); core.player.setNoGravity(true); core.player.setNoClipMapped(true);
        return boat;
    }
    private static void moveBoat(Vehicle boat,double centerZ) {
        double progress=Utilities.getJsonObjectFromData(boat).get("railProgress").getAsDouble();
        JsonObject update=new JsonObject(); update.addProperty("railProgress",progress+centerZ-vehicleCenter(boat).z); boat.updateData(new JsonReader(update));
        assertEquals(centerZ,vehicleCenter(boat).z,1E-5);
    }

    private static final class CoreFixture implements AutoCloseable {
        static final java.util.Map<ServerLifecycleScenarios.ServerFixture,CoreFixture> FIXTURES=new java.util.IdentityHashMap<>();
        final ServerLifecycleScenarios.ServerFixture fixture;
        final org.mtr.core.simulation.Simulator simulator;
        final ServerPlayerEntity player;
        CoreFixture(ServerLifecycleScenarios.ServerFixture fixture) throws Exception {
            this.fixture=fixture; player=new ServerPlayerEntity(fixture.player); FIXTURES.put(fixture,this); RidingPermissions.initialize();
            fixture.world.playerEntities.add(fixture.player);
            org.mtr.mapping.holder.Identifier identifier=org.mtr.mapping.mapper.MinecraftServerHelper.getWorldId(new org.mtr.mapping.holder.World(fixture.world));
            String dimension=identifier.getNamespace()+"/"+identifier.getPath();
            simulator=new org.mtr.core.simulation.Simulator(dimension,new String[]{dimension},java.nio.file.Files.createTempDirectory("mtr-riding-core"),false);
            Client client=new Client(player.getUuid()); client.setPositionAndUpdateRadius(new Position(0,64,0),128); simulator.clients.add(client);
        }
        void observe(Lift lift) throws Exception {
            VehicleLiftResponse response=new VehicleLiftResponse(player.getUuid(),simulator); response.addLiftToUpdate(lift);
            RidingPermissions.observe(new ServerWorld(fixture.world),response,CoreFixture::path);
            poll(); drain(); poll();
        }
        void poll() { RidingPermissions.tick(new ServerWorld(fixture.world),CoreFixture::path); }
        int queuedCount() throws Exception {
            java.lang.reflect.Field runs=org.mtr.core.simulation.Simulator.class.getDeclaredField("queuedRuns"); runs.setAccessible(true);
            java.lang.reflect.Field queue=org.mtr.core.servlet.MessageQueue.class.getDeclaredField("linkedBlockingDeque"); queue.setAccessible(true);
            return ((java.util.Collection<?>)queue.get(runs.get(simulator))).size();
        }
        void drain() throws Exception { drain(simulator); }
        static void drain(org.mtr.core.simulation.Simulator simulator) throws Exception {
            java.lang.reflect.Field field=org.mtr.core.simulation.Simulator.class.getDeclaredField("queuedRuns"); field.setAccessible(true);
            org.mtr.core.servlet.MessageQueue<Runnable> queue=(org.mtr.core.servlet.MessageQueue<Runnable>)field.get(simulator);
            Throwable[] failure={null}; Thread thread=new Thread(()->{try { queue.process(Runnable::run); } catch(Throwable problem) { failure[0]=problem; }},"riding-core-query-test");
            thread.start(); thread.join(5000); assertFalse(thread.isAlive()); if(failure[0]!=null)throw new AssertionError(failure[0]);
        }
        static ObjectArrayList<Vector> path(Position a,Position b) {
            assertFalse(Thread.currentThread().getName().equals("riding-core-query-test"),"Native world/path reads must stay off the Core thread");
            return ObjectArrayList.of(new Vector(a.getX(),a.getY(),a.getZ()),new Vector(b.getX(),b.getY(),b.getZ()));
        }
        @Override public void close() { FIXTURES.remove(fixture); simulator.stop(); }
    }
    private static Lift lift(Data data) {
        Lift lift=new Lift(data); lift.setDimensions(3,3,3,0,0,0); lift.setAngle(Angle.S);
        lift.setFloors(ObjectArrayList.of(new LiftFloor(new Position(0,64,0),"1",""),new LiftFloor(new Position(0,80,0),"2",""))); return lift;
    }
    private static void observe(ServerLifecycleScenarios.ServerFixture f,Lift lift) throws Exception {
        CoreFixture core=CoreFixture.FIXTURES.get(f); core.simulator.lifts.add(lift); core.simulator.sync(); core.observe(lift);
    }
    private static JsonReader request(UUID uuid,long siding,long vehicle,long car) {
        UpdateVehicleRidingEntities request=new UpdateVehicleRidingEntities(siding,vehicle); request.add(new VehicleRidingEntity(uuid,car,0,0,0,false,false,false,false,false,false,false));
        return new JsonReader(Utilities.getJsonObjectFromData(request));
    }
    private static JsonObject json(JsonReader reader) {
        // Reconstruct only test input through the real pinned schema, preserving its exact field names.
        return Utilities.getJsonObjectFromData(new UpdateVehicleRidingEntities(reader));
    }
    private static Vehicle vehicle(CoreFixture core) throws Exception {
        return vehicle(core,false);
    }
    private static Vehicle vehicle(CoreFixture core,boolean replay) throws Exception {
        Data data=core.simulator; Position p1=new Position(replay?8:0,replay?4:64,0),p2=new Position(replay?8:0,replay?4:64,replay?300:100);
        Rail rail=Rail.newRail(p1,Angle.S,p2,Angle.N,Rail.Shape.QUADRATIC,0,new ObjectArrayList<>(),80,80,true,false,false,false,false,TransportMode.TRAIN);
        PathData path=new PathData(rail,51,0,0,p1,p2);
        JsonObject extra=new JsonObject(); extra.addProperty("sidingId",51); extra.addProperty("depotId",52); extra.addProperty("totalVehicleLength",10);
        JsonArray cars=new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar("test",replay?9:10,replay?2:3,-3,3,0,0))); extra.add("vehicleCars",cars);
        JsonArray paths=new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        VehicleExtraData details=new VehicleExtraData(new JsonReader(extra)); PathData.writePathCache(details.immutablePath,data,TransportMode.TRAIN);
        JsonObject definition=new JsonObject(); definition.addProperty("id",77); definition.addProperty("transportMode","TRAIN"); definition.addProperty("railProgress",50);
        Siding seed=new Siding(p1,p2,100,TransportMode.TRAIN,data);
        JsonObject sidingJson=Utilities.getJsonObjectFromData(seed); sidingJson.addProperty("id",51);
        Siding siding=new Siding(new JsonReader(sidingJson),data); core.simulator.sidings.add(siding);
        Vehicle vehicle=new Vehicle(details,siding,new JsonReader(definition),data);
        java.lang.reflect.Field vehicles=Siding.class.getDeclaredField("vehicles"); vehicles.setAccessible(true);
        ((java.util.Collection<Vehicle>)vehicles.get(siding)).add(vehicle);
        return vehicle;
    }
}
