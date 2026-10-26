package org.mtr.mapping.tool;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C03PacketPlayer;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.util.AxisAlignedBB;

public final class PlayerPhysics {
    // Riding flags belong to the current player object and must never survive a save/reconnect.
    private static final Map<EntityPlayerMP,State> FLAGS=new WeakHashMap<>();
    private static Entity clientNoGravity;
    private static net.minecraft.world.World clientNoGravityWorld;
    private static int clientNoGravityDimension;
    private static java.util.function.BiPredicate<NetHandlerPlayServer,C03PacketPlayer> movementDeferrer=(handler,packet)->false;
    public static void setMovementDeferrer(java.util.function.BiPredicate<NetHandlerPlayServer,C03PacketPlayer> deferrer) {
        movementDeferrer=java.util.Objects.requireNonNull(deferrer);
    }
    private PlayerPhysics() {}
    private static synchronized void set(EntityPlayerMP player,int bit,boolean value) {
        State state=FLAGS.get(player);
        if (state!=null) state.flags=value && authorized(player)?state.flags|bit:state.flags&~bit;
    }
    public static synchronized boolean authorized(EntityPlayerMP player) {
        State state=FLAGS.get(player);
        if (state==null) return false;
        if (state.dimension!=player.dimension || player.worldObj.getTotalWorldTime()>=state.until) { revoke(player); return false; }
        return true;
    }
    public static synchronized void authorize(EntityPlayerMP player,Collection<AxisAlignedBB> bounds,long until) {
        long now=player.worldObj.getTotalWorldTime();
        if (until<=now || bounds.isEmpty()) { revoke(player); return; }
        List<AxisAlignedBB> copy=new ArrayList<>();
        for (AxisAlignedBB box:bounds) {
            if (!Double.isFinite(box.minX) || !Double.isFinite(box.maxX) || !Double.isFinite(box.minY) || !Double.isFinite(box.maxY)
                    || !Double.isFinite(box.minZ) || !Double.isFinite(box.maxZ) || box.minX>=box.maxX || box.minY>=box.maxY || box.minZ>=box.maxZ)
                throw new IllegalArgumentException("Invalid authoritative riding bounds");
            copy.add(box.copy());
        }
        State state=FLAGS.get(player);
        FLAGS.put(player,new State(player.dimension,Math.min(until,now+60),copy,state==null?0:state.flags));
    }
    public static synchronized void revoke(EntityPlayerMP player) {
        if (FLAGS.remove(player)!=null) {
            player.noClip=false; player.fallDistance=0;
            org.mtr.mapping.registry.LegacyPlayerPhysics.syncNoGravity(player,false);
        }
    }
    private static synchronized boolean has(EntityPlayerMP player,int bit) {
        return authorized(player) && (FLAGS.get(player).flags&bit)!=0;
    }
    public static synchronized void setNoGravity(EntityPlayerMP player,boolean value) {
        boolean previous=has(player,1);
        set(player,1,value);
        boolean current=has(player,1);
        if (previous!=current) org.mtr.mapping.registry.LegacyPlayerPhysics.syncNoGravity(player,current);
    }
    public static void updateClientNoGravity(Entity player,int dimension,int entityId,boolean value) {
        if (player==null || !player.worldObj.isRemote || player.dimension!=dimension || player.getEntityId()!=entityId) return;
        clientNoGravity=value?player:null;
        clientNoGravityWorld=value?player.worldObj:null;
        clientNoGravityDimension=dimension;
    }
    public static void clearClientNoGravity() { clientNoGravity=null; clientNoGravityWorld=null; }
    public static void setTeleporting(EntityPlayerMP player,boolean value) { set(player,2,value); }
    public static double gravity(double amount,Entity entity) {
        if (entity.worldObj.isRemote) return entity==clientNoGravity && entity.worldObj==clientNoGravityWorld
            && entity.dimension==clientNoGravityDimension?0:amount;
        return entity instanceof EntityPlayerMP && has((EntityPlayerMP)entity,1)?0:amount;
    }
    public static double movementLimit(NetHandlerPlayServer handler) { return has(handler.playerEntity,2)?Double.POSITIVE_INFINITY:100; }
    public static boolean allowFlight(boolean vanilla,NetHandlerPlayServer handler) { return vanilla || has(handler.playerEntity,2); }
    public static boolean validate(NetHandlerPlayServer handler,C03PacketPlayer packet) {
        if (packet.func_149466_j() && (!Double.isFinite(packet.func_149464_c()) || !Double.isFinite(packet.func_149467_d())
                || !Double.isFinite(packet.func_149471_f()) || !Double.isFinite(packet.func_149472_e()))
                || packet.func_149463_k() && (!Float.isFinite(packet.func_149462_g()) || !Float.isFinite(packet.func_149470_h()))) {
            handler.kickPlayerFromServer("Invalid movement");
            return false;
        }
        if (packet.func_149466_j() && has(handler.playerEntity,2)) {
            if (movementDeferrer.test(handler,packet)) return false;
            boolean stillAuthorized=has(handler.playerEntity,2);
            if (!stillAuthorized || !contains(handler.playerEntity,packet.func_149464_c(),packet.func_149467_d(),packet.func_149472_e())) {
            EntityPlayerMP player=handler.playerEntity;
            revoke(player);
            // Riding privileges end here; native movement decides whether the walkoff is legal.
            if (stillAuthorized && player.playerNetServerHandler==handler && handler.netManager.isChannelOpen()) return true;
            handler.setPlayerLocation(player.posX,player.posY,player.posZ,player.rotationYaw,player.rotationPitch);
            return false;
            }
        }
        return true;
    }
    private static synchronized boolean contains(EntityPlayerMP player,double x,double y,double z) {
        State state=FLAGS.get(player);
        if (state==null) return false;
        for (AxisAlignedBB box:state.bounds)
            if (x>=box.minX && x<=box.maxX && y>=box.minY && y<=box.maxY && z>=box.minZ && z<=box.maxZ) return true;
        return false;
    }
    private static final class State {
        final int dimension; final long until; final List<AxisAlignedBB> bounds; int flags;
        State(int dimension,long until,List<AxisAlignedBB> bounds,int flags) { this.dimension=dimension; this.until=until; this.bounds=bounds; this.flags=flags; }
    }
}
