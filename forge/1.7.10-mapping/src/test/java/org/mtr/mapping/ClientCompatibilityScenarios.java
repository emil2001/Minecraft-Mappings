package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.KeyCodes;
import static org.junit.jupiter.api.Assertions.*;

public class ClientCompatibilityScenarios {
    public void dimensionsWithoutSkyKeepRailAndVehicleSkyLightDark() {
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        World world=new World(nativeWorld);
        ClientWorld clientWorld=new ClientWorld(nativeWorld);
        BlockPos emptySection=new BlockPos(0,200,0),litBlock=new BlockPos(0,64,0);
        nativeWorld.getChunkFromBlockCoords(0,0);
        assertEquals(15,world.getLightLevel(LightType.getSkyMapped(),emptySection));
        assertEquals(15,clientWorld.getLightLevel(LightType.getSkyMapped(),emptySection));
        nativeWorld.provider.hasNoSky=true;
        nativeWorld.setLightValue(net.minecraft.world.EnumSkyBlock.Block,0,64,0,11);
        assertEquals(0,world.getLightLevel(LightType.getSkyMapped(),emptySection));
        assertEquals(0,clientWorld.getLightLevel(LightType.getSkyMapped(),emptySection));
        assertEquals(0,world.getLightLevel(LightType.getSkyMapped(),new BlockPos(0,256,0)));
        assertEquals(0,clientWorld.getLightLevel(LightType.getSkyMapped(),new BlockPos(0,256,0)));
        assertEquals(11,world.getLightLevel(LightType.getBlockMapped(),litBlock));
        assertEquals(11,clientWorld.getLightLevel(LightType.getBlockMapped(),litBlock));
        nativeWorld.provider.hasNoSky=false;
        assertEquals(15,world.getLightLevel(LightType.getSkyMapped(),emptySection));
        assertEquals(15,clientWorld.getLightLevel(LightType.getSkyMapped(),emptySection));
    }
    public void optionalShaderClassesOnTheClasspathDoNotActivateAnUnloadedMod() throws Exception {
        assertNotNull(Class.forName("net.irisshaders.iris.api.v0.IrisApi",false,getClass().getClassLoader()));
        assertFalse(cpw.mods.fml.common.Loader.isModLoaded("angelica"));
        assertFalse(org.mtr.mapping.tool.LegacyShaderHooks.isRenderingShadowPass());
    }
    public void motionBlockingHeightIncludesTransparentSolidsAndWaterButIgnoresPlants() {
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld();
        World world=new World(nativeWorld);
        assertEquals(0,world.getTopY(HeightMapType.getMotionBlockingMapped(),0,0));
        nativeWorld.setBlock(0,64,0,net.minecraft.init.Blocks.stone);
        nativeWorld.setBlock(0,80,0,net.minecraft.init.Blocks.glass);
        nativeWorld.setBlock(0,90,0,net.minecraft.init.Blocks.tallgrass);
        assertEquals(81,world.getTopY(HeightMapType.getMotionBlockingMapped(),0,0),"Transparent solids must not be skipped by the native skylight heightmap");
        nativeWorld.setBlock(0,100,0,net.minecraft.init.Blocks.water);
        assertEquals(101,world.getTopY(HeightMapType.getMotionBlockingMapped(),0,0));
        nativeWorld.setBlock(0,255,0,net.minecraft.init.Blocks.leaves);
        assertEquals(256,world.getTopY(HeightMapType.getMotionBlockingMapped(),0,0),"Motion-blocking height includes leaves, unlike native getTopSolidOrLiquidBlock");
    }
    public void originalVehicleTransformRoundTripsNativeVectorsWithoutMutation() throws Exception {
        try (NativeClientFixture fixture=new NativeClientFixture()) {
            org.mtr.mod.render.PositionAndRotation transform=new org.mtr.mod.render.PositionAndRotation(new org.mtr.core.tool.Vector(-34.5,65,.75),.83,.23);
            Vector3d source=new Vector3d(.25,.5,1.75);
            Vector3d transformed=transform.transformForwards(source,Vector3d::rotateX,Vector3d::rotateY,Vector3d::add);
            org.mtr.core.tool.Vector expected=transform.transformForwards(new org.mtr.core.tool.Vector(.25,.5,1.75),
                (v,a) -> v.rotateX(a),(v,a) -> v.rotateY(a),(v,x,y,z) -> v.add(x,y,z));
            assertEquals(expected.x,transformed.getXMapped(),1E-3); assertEquals(expected.y,transformed.getYMapped(),1E-3); assertEquals(expected.z,transformed.getZMapped(),1E-3);
            Vector3d restored=transform.transformBackwards(transformed,Vector3d::rotateX,Vector3d::rotateY,Vector3d::add);
            assertEquals(.25,restored.getXMapped(),1E-3); assertEquals(.5,restored.getYMapped(),1E-3); assertEquals(1.75,restored.getZMapped(),1E-3);
            assertEquals(.25,source.getXMapped()); assertEquals(.5,source.getYMapped()); assertEquals(1.75,source.getZMapped());
        }
    }
    public void localGameModeUsesAuthoritativeControllerAndRejectsStaleConnections() throws Exception {
        try (NativeClientFixture fixture=new NativeClientFixture()) {
            net.minecraft.client.Minecraft nativeClient=fixture.client; MinecraftClient client=MinecraftClient.getInstance();
            assertNull(client.getNetworkHandler());
            net.minecraft.client.network.NetHandlerPlayClient handler=new net.minecraft.client.network.NetHandlerPlayClient(nativeClient,null,null);
            BlockPlacementScenarios.MemoryWorld world=new BlockPlacementScenarios.MemoryWorld(); world.isRemote=true;
            nativeClient.thePlayer=new net.minecraft.client.entity.EntityClientPlayerMP(nativeClient,world,
                new net.minecraft.util.Session("MtrModeTest",java.util.UUID.randomUUID().toString(),"","legacy"),handler,new net.minecraft.stats.StatFileWriter()) {
                @Override public String getCommandSenderName() { return ""; }
            };
            nativeClient.playerController=new net.minecraft.client.multiplayer.PlayerControllerMP(nativeClient,handler);
            java.util.UUID uuid=nativeClient.thePlayer.getUniqueID(); ClientPlayNetworkHandler connection=client.getNetworkHandler();
            assertSame(handler,connection.data); assertNull(connection.getPlayerListEntry(java.util.UUID.randomUUID()));
            net.minecraft.world.WorldSettings.GameType[] modes={net.minecraft.world.WorldSettings.GameType.SURVIVAL,
                net.minecraft.world.WorldSettings.GameType.CREATIVE,net.minecraft.world.WorldSettings.GameType.ADVENTURE};
            GameMode[] expected={GameMode.getSurvivalMapped(),GameMode.getCreativeMapped(),GameMode.ADVENTURE};
            PlayerListEntry entry=connection.getPlayerListEntry(uuid); assertNotNull(entry);
            for (int i=0;i<modes.length;i++) {
                nativeClient.playerController.setGameType(modes[i]); nativeClient.thePlayer.capabilities.isCreativeMode=false;
                assertSame(expected[i],entry.getGameMode(),"Permission must use the native game mode, not transient player capabilities");
            }
            nativeClient.playerController=null; assertNull(connection.getPlayerListEntry(uuid));
            nativeClient.playerController=new net.minecraft.client.multiplayer.PlayerControllerMP(nativeClient,handler);
            ClientPlayNetworkHandler stale=new ClientPlayNetworkHandler(new net.minecraft.client.network.NetHandlerPlayClient(nativeClient,null,null));
            assertNull(stale.getPlayerListEntry(uuid));
            nativeClient.thePlayer=null; assertNull(client.getNetworkHandler()); assertNull(connection.getPlayerListEntry(uuid));
        }
    }
    public void nativeClientTasksSurviveWorldUnloadButPacketWorkDoesNot() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            boolean[] general = {false}, packet = {false};
            org.mtr.mapping.registry.GameThreadQueues.init();
            org.mtr.mapping.registry.LegacyClientEvents.init();
            Thread worker = new Thread(() -> MinecraftClient.getInstance().execute(() -> general[0] = true));
            worker.start(); worker.join(); assertFalse(general[0]);
            org.mtr.mapping.registry.GameThreadQueues.client(() -> packet[0] = true);
            BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld(); world.isRemote = true;
            org.mtr.mapping.mapper.EntityHelper.HIDDEN_PLAYERS.add(java.util.UUID.randomUUID());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(world));
            assertTrue(org.mtr.mapping.mapper.EntityHelper.HIDDEN_PLAYERS.isEmpty(),"Passenger offsets must not leak into the next world");
            fixture.runScheduledTasks();
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().post(new cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent(
                cpw.mods.fml.common.gameevent.TickEvent.Phase.START));
            assertTrue(general[0]); assertFalse(packet[0]);
        }
    }
    public void riderFeetStayOnCabinFloorWithNativeClientEyeOffsetAndSneakSmoothing() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld(); world.isRemote = true;
            net.minecraft.client.entity.EntityPlayerSP nativePlayer = new net.minecraft.client.entity.EntityPlayerSP(
                fixture.client, world, new net.minecraft.util.Session("MtrFeetTest", java.util.UUID.randomUUID().toString(), "", "legacy"), 0) {
                // Avoid the unrelated skin download during this native movement test.
                @Override public String getCommandSenderName() { return ""; }
            };
            assertEquals(1.62F, nativePlayer.yOffset);
            ClientPlayerEntity rider = new ClientPlayerEntity(nativePlayer);
            for (float smoothing : new float[]{0, 0.2F}) {
                nativePlayer.ySize = smoothing;
                rider.updatePosition(-17.25, 65, 0.75);
                assertEquals(65, nativePlayer.boundingBox.minY, 0.000001);
                assertEquals(65, rider.getY(), 0.000001);
                assertEquals(65, rider.getPos().getYMapped(), 0.000001);
                assertEquals(65, Entity.cast(rider).getYMapped(), 0.000001);
                Entity entity=Entity.cast(rider);
                assertEquals(-17.25,entity.getX()); assertEquals(65,entity.getY(),1E-6); assertEquals(.75,entity.getZ());
                assertEquals(65,entity.getPos().getYMapped(),1E-6);
                assertEquals(rider.getUuid(),entity.getUuid());
                assertEquals(new BlockPos(-18, 65, 0), PlayerEntity.cast(rider).getBlockPos());
            }
            nativePlayer.yOffset = 0; nativePlayer.ySize = 0;
            rider.updatePosition(-17.25, 65, 0.75);
            assertEquals(65, nativePlayer.posY);
            assertEquals(65, Entity.cast(rider).getYMapped());
        }
    }
    public void registryKeysClientScalingAndRenderFrameUseNativeState() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            net.minecraft.client.Minecraft nativeClient = fixture.client;
            MinecraftClient client = MinecraftClient.getInstance();
            assertSame(nativeClient, client.data);
            assertNull(client.getPlayerMapped()); assertNull(client.getWorldMapped());
            assertNull(client.getCurrentScreenMapped()); assertNull(client.getCrosshairTargetMapped());
            nativeClient.displayWidth = 1920; nativeClient.displayHeight = 1080;
            nativeClient.gameSettings.guiScale = 3;
            assertEquals(640, client.getWindow().getScaledWidth());
            assertEquals(360, client.getWindow().getScaledHeight());
            nativeClient.gameSettings.forceUnicodeFont = true;
            assertEquals(960, client.getWindow().getScaledWidth());
            assertEquals(540, client.getWindow().getScaledHeight());
            assertTrue(client.isRunning());
            cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.client.Minecraft.class, nativeClient,
                false, "running", "field_71425_J");
            assertFalse(client.isRunning());
            nativeClient.currentScreen = new net.minecraft.client.gui.GuiScreen();
            assertSame(nativeClient.currentScreen, client.getCurrentScreenMapped().data);
            nativeClient.objectMouseOver = new net.minecraft.util.MovingObjectPosition(-18, 64, 3, 2,
                net.minecraft.util.Vec3.createVectorHelper(-17.5, 64.25, 3));
            assertEquals(-17.5, client.getCrosshairTargetMapped().getPos().getXMapped());
            org.mtr.mapping.registry.RegistryClient registry = new org.mtr.mapping.registry.RegistryClient(
                new org.mtr.mapping.registry.Registry());
            int before = nativeClient.gameSettings.keyBindings.length;
            KeyBinding key = registry.registerKeyBinding("key.mtr.client_test", 90, "category.mtr.test");
            assertEquals(before + 1, nativeClient.gameSettings.keyBindings.length);
            assertSame(key.data, nativeClient.gameSettings.keyBindings[before]);
            assertEquals(org.lwjgl.input.Keyboard.KEY_Z, key.data.getKeyCode());
            org.mtr.mapping.registry.LegacyClientEvents.init();
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().post(
                new cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent(cpw.mods.fml.common.gameevent.TickEvent.Phase.START, 0.25F));
            assertEquals(0.25F, org.mtr.mapping.registry.LegacyClientEvents.FRAME.getPartialTick());
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().post(
                new cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent(cpw.mods.fml.common.gameevent.TickEvent.Phase.END, 0.75F));
            assertEquals(0.25F, org.mtr.mapping.registry.LegacyClientEvents.FRAME.getPartialTick());
            boolean[] ran = {false};
            Thread worker = new Thread(() -> client.execute(() -> ran[0] = true));
            worker.start(); worker.join(); assertFalse(ran[0]);
            fixture.runScheduledTasks();
            assertTrue(ran[0]);
        }
    }
    public void clientWorldAndRiderWrappersRetainNativeStateAndLightValues() {
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        nativeWorld.isRemote = true;
        ClientWorld world = new ClientWorld(nativeWorld);
        BlockPos pos = new BlockPos(-17, 64, 0);
        new World(nativeWorld).setBlockState(pos, new Block(net.minecraft.init.Blocks.stone).getDefaultState());
        assertEquals(new World(nativeWorld), world);
        assertEquals(net.minecraft.init.Blocks.stone, world.getBlockState(pos).getBlock().data);
        nativeWorld.setLightValue(net.minecraft.world.EnumSkyBlock.Block, -17, 64, 0, 7);
        nativeWorld.setLightValue(net.minecraft.world.EnumSkyBlock.Sky, -17, 64, 0, 11);
        assertEquals(7, world.getLightLevel(LightType.getBlockMapped(), pos));
        assertEquals(11, world.getLightLevel(LightType.getSkyMapped(), pos));
        assertEquals(0x00b00070, LightmapTextureManager.pack(7, 11));
        net.minecraft.entity.player.EntityPlayer nativePlayer = MtrEscalatorScenarios.player(nativeWorld);
        nativeWorld.playerEntities.add(nativePlayer);
        ClientPlayerEntity player = new ClientPlayerEntity(nativePlayer);
        nativePlayer.moveForward = 0.5F; nativePlayer.moveStrafing = -0.25F;
        assertEquals(0.5F, player.getForwardSpeedMapped()); assertEquals(-0.25F, player.getSidewaysSpeedMapped());
        assertTrue(player.getOffHandStack().isEmpty());
        assertEquals(Hand.MAIN_HAND, player.getActiveHand());
        player.updatePosition(-17.25, 65, 0.75); player.setVelocity(1, 2, 3); player.setFallDistanceMapped(0);
        assertEquals(-17.25, player.getX()); assertEquals(0.75, player.getZ());
        assertEquals(1, nativePlayer.motionX); assertEquals(2, nativePlayer.motionY); assertEquals(3, nativePlayer.motionZ);
        assertEquals(new BlockPos(-18, 65, 0), player.getBlockPos());
        assertEquals(new PlayerEntity(nativePlayer), world.getPlayerByUuid(player.getUuid()));
    }
    public void glfwDefaultsTranslateToLegacyKeysAndPressedDoesNotConsumeInput() {
        assertEquals(org.lwjgl.input.Keyboard.KEY_Z, KeyCodes.toLegacy(90));
        assertEquals(org.lwjgl.input.Keyboard.KEY_UP, KeyCodes.toLegacy(265));
        assertEquals(org.lwjgl.input.Keyboard.KEY_DOWN, KeyCodes.toLegacy(264));
        assertEquals(org.lwjgl.input.Keyboard.KEY_LEFT, KeyCodes.toLegacy(263));
        assertEquals(org.lwjgl.input.Keyboard.KEY_NUMPAD4, KeyCodes.toLegacy(324));
        assertEquals(org.lwjgl.input.Keyboard.KEY_NUMPAD9, KeyCodes.toLegacy(329));
        assertEquals(org.lwjgl.input.Keyboard.KEY_SUBTRACT, KeyCodes.toLegacy(333));
        assertEquals(org.lwjgl.input.Keyboard.KEY_ADD, KeyCodes.toLegacy(334));
        assertEquals(90, KeyCodes.toGlfw(org.lwjgl.input.Keyboard.KEY_Z));
        int code = KeyCodes.toLegacy(90);
        net.minecraft.client.settings.KeyBinding nativeKey = new net.minecraft.client.settings.KeyBinding("mtr.test.key", code, "mtr.test");
        KeyBinding key = new KeyBinding(nativeKey);
        net.minecraft.client.settings.KeyBinding.setKeyBindState(code, true);
        net.minecraft.client.settings.KeyBinding.onTick(code);
        try {
            assertTrue(key.isPressed()); assertTrue(key.isPressed());
            assertTrue(key.wasPressed()); assertFalse(key.wasPressed());
            assertTrue(key.isPressed());
        } finally { net.minecraft.client.settings.KeyBinding.setKeyBindState(code, false); }
    }
}
