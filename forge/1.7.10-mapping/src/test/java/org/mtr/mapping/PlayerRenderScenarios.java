package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import org.mtr.mapping.mapper.EntityHelper;
import static org.junit.jupiter.api.Assertions.*;

public final class PlayerRenderScenarios {
    private static final class SceneProbe extends RenderPlayer {
        double sceneY=Double.NaN;
        @Override protected void renderLivingAt(AbstractClientPlayer player,double x,double y,double z) { sceneY=y; }
        @Override protected void renderModel(EntityLivingBase entity,float a,float b,float c,float d,float e,float f) {}
        @Override protected void renderEquippedItems(AbstractClientPlayer player,float tick) {}
        @Override protected void passSpecialRender(EntityLivingBase entity,double x,double y,double z) {}
        @Override public void doRenderShadowAndFire(net.minecraft.entity.Entity entity,double x,double y,double z,float yaw,float tick) {}
    }
    public static void run(Minecraft client) {
        BlockPlacementScenarios.MemoryWorld world=new BlockPlacementScenarios.MemoryWorld(); world.isRemote=true;
        net.minecraft.client.entity.EntityPlayerSP player=new net.minecraft.client.entity.EntityPlayerSP(client,world,
            new net.minecraft.util.Session("MtrRenderTest",java.util.UUID.randomUUID().toString(),"","legacy"),0) {
                @Override public String getCommandSenderName() { return ""; }
            };
        player.movementInput=new net.minecraft.util.MovementInput();
        AbstractClientPlayer remote=new net.minecraft.client.entity.EntityOtherPlayerMP(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MtrRemote")) {
            @Override public String getCommandSenderName() { return ""; }
        };
        for (boolean sneaking : new boolean[]{false,true}) {
            player.movementInput.sneak=sneaking; remote.setSneaking(sneaking);
            verifyPlayer(client,player); verifyPlayer(client,remote);
        }
        System.out.println("Native local/remote player offset, MTR +1000 feet compensation, sneaking and ordinary player restoration: PASS");
    }
    private static void verifyPlayer(Minecraft client,AbstractClientPlayer player) {
        double nativeOffset=player.yOffset+(player.isSneaking() && !(player instanceof net.minecraft.client.entity.EntityPlayerSP)?.125:0);
        SceneProbe renderer=new SceneProbe(); renderer.setRenderManager(net.minecraft.client.renderer.entity.RenderManager.instance);
        net.minecraft.client.renderer.entity.RenderManager manager=net.minecraft.client.renderer.entity.RenderManager.instance;
        Object previous=manager.entityRenderMap.put(player.getClass(),renderer);
        net.minecraft.client.renderer.texture.TextureManager previousTextures=manager.renderEngine; manager.renderEngine=client.getTextureManager();
        try {
            renderer.doRender(player,0,0,0,0,0); assertEquals(-nativeOffset,renderer.sceneY,1E-6);
            org.mtr.mapping.mapper.GraphicsHolder.createInstanceSafe(graphics -> graphics.renderEntity(new org.mtr.mapping.holder.Entity(player),0,0,0,0,0,org.mtr.mapping.mapper.GraphicsHolder.getDefaultLight()));
            assertEquals(player.yOffset-nativeOffset,renderer.sceneY,1E-6,"The modern draw bridge receives feet coordinates and preserves ordinary sneak offset");
            EntityHelper.HIDDEN_PLAYERS.add(player.getUniqueID());
            renderer.sceneY=Double.NaN; renderer.doRender(player,0,0,0,0,0); assertEquals(-1000-player.yOffset,renderer.sceneY,1E-4,"The native world draw must honor MTR's player offset");
            renderer.sceneY=Double.NaN; renderer.doRender(player,0,1000,0,0,0); assertEquals(-player.yOffset,renderer.sceneY,1E-6,"MTR's explicit rider draw at+1000 must still render at the cabin floor");
            org.mtr.mapping.mapper.GraphicsHolder.createInstanceSafe(graphics -> graphics.renderEntity(new org.mtr.mapping.holder.Entity(player),0,1000,0,0,0,org.mtr.mapping.mapper.GraphicsHolder.getDefaultLight()));
            assertEquals(0,renderer.sceneY,1E-6,"The actual MTR bridge must put the passenger at the cabin floor");
            EntityHelper.HIDDEN_PLAYERS.clear(); renderer.doRender(player,0,0,0,0,0); assertEquals(-nativeOffset,renderer.sceneY,1E-6);
        } finally {
            EntityHelper.HIDDEN_PLAYERS.clear(); manager.renderEngine=previousTextures;
            if (previous==null) manager.entityRenderMap.remove(player.getClass()); else manager.entityRenderMap.put(player.getClass(),previous);
        }
    }
}
