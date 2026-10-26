package org.mtr.mapping.registry;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockTicketBarrier;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

/** Original gate callback reached through ordinary native packet movement; no rider/physics bypass. */
public final class GateCrossingScenarios {
    public void ordinaryPlayerCrossesOriginalGateWithoutCollisionRollback() throws Exception {
        for (Direction facing:new Direction[]{Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST})
            for (EnumTicketBarrierOpen open:new EnumTicketBarrierOpen[]{EnumTicketBarrierOpen.OPEN,EnumTicketBarrierOpen.OPEN_CONCESSIONARY})
                cross(facing,open);
    }

    private static void cross(Direction facing,EnumTicketBarrierOpen admitted) throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            BlockTicketBarrier gate=new BlockTicketBarrier(true);
            Registry registry=new Registry();
            registry.registerBlock(new Identifier("mtr","native_gate_crossing_"+facing.name().toLowerCase(Locale.ROOT)+"_"+admitted.name().toLowerCase(Locale.ROOT)),()->new Block(gate));registry.init();
            World world=new World(fixture.world);BlockPos pos=new BlockPos(4,64,4);
            for(int x=2;x<=6;x++)for(int z=2;z<=6;z++)fixture.world.setBlock(x,63,z,net.minecraft.init.Blocks.stone,0,2);
            fixture.world.setBlockToAir(4,65,4);
            world.setBlockState(pos,gate.getDefaultState2().with(new Property<>(BlockTicketBarrier.FACING.data),facing.data).with(new Property<>(BlockTicketBarrier.OPEN.data),admitted),2);
            assertTrue(fixture.world.isAirBlock(4,65,4),"Use the real one-block gate with AIR above; native corner push-out is not this regression");
            fixture.player.theItemInWorldManager.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            fixture.player.noClip=false;fixture.player.onGround=true;
            float width=fixture.player.width;
            assertFalse(fixture.player.capabilities.isFlying);
            double first=-.55;
            fixture.player.setPosition(4.5+facing.getOffsetX()*first,64,4.5+facing.getOffsetZ()*first);
            Handler handler=new Handler(fixture);
            double clearance=fixture.player.width/2D+1/16D;
            for(double along:new double[]{-.55,-.35,-.15,.05,.25,.35,.45,.65,.85,.95}) {
                double x=4.5+facing.getOffsetX()*along,z=4.5+facing.getOffsetZ()*along;
                handler.processPlayer(new C03PacketPlayer.C04PacketPlayerPosition(x,64,65.62,z,true));
                String step=facing+"/"+admitted+"/width="+width+"/along="+along;
                assertNull(handler.kick,step);
                assertEquals(0,handler.corrections,"An admitted ordinary crossing must not generate native collision rollback: "+step);
                assertEquals(x,fixture.player.posX,1E-7,step);assertEquals(z,fixture.player.posZ,1E-7,step);
                assertFalse(fixture.player.noClip,"The regression must retain native collision validation");
                assertEquals(facing,IBlock.getStatePropertySafe(world.getBlockState(pos),BlockTicketBarrier.FACING),"Movement must not change the gate's facing");
                assertEquals(along<=clearance?admitted:EnumTicketBarrierOpen.CLOSED,IBlock.getStatePropertySafe(world.getBlockState(pos),new Property<>(BlockTicketBarrier.OPEN.data)),"Close only after the actual trailing body edge clears the crossbar: "+step);
                assertTrue(fixture.world.getCollidingBoundingBoxes(fixture.player,fixture.player.boundingBox.copy()).isEmpty(),"Closing the gate must never create collision inside the admitted player's full body: "+step);
            }
            assertEquals(EnumTicketBarrierOpen.CLOSED,IBlock.getStatePropertySafe(world.getBlockState(pos),new Property<>(BlockTicketBarrier.OPEN.data)),"The gate still closes behind the passenger");
            fixture.player.setPosition(4.5,64,4.5);
            assertFalse(fixture.world.getCollidingBoundingBoxes(fixture.player,fixture.player.boundingBox.copy()).isEmpty(),"Closed original crossbar retains full physical collision");
        }
    }

    private static final class Handler extends NetHandlerPlayServer {
        int corrections;String kick;
        Handler(ServerLifecycleScenarios.ServerFixture fixture){super(fixture.server,new NetworkManager(false),fixture.player);}
        @Override public void sendPacket(Packet packet) {}
        @Override public void setPlayerLocation(double x,double y,double z,float yaw,float pitch){corrections++;super.setPlayerLocation(x,y,z,yaw,pitch);}
        @Override public void kickPlayerFromServer(String reason){kick=reason;}
    }
}
