package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayClient;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class LegacyNetworkScenarios {
    public void playerFeedbackUsesNativeOverlayWithoutOrdinaryChat() throws Exception {
        java.util.function.BiConsumer<org.mtr.mapping.holder.ServerPlayerEntity, org.mtr.mapping.holder.Text> previousServer = LegacyPlayerMessages.serverSink;
        java.util.function.Consumer<org.mtr.mapping.holder.Text> previousClient = LegacyPlayerMessages.clientSink;
        try (ServerLifecycleScenarios.ServerFixture server = new ServerLifecycleScenarios.ServerFixture();
                org.mtr.mapping.NativeClientFixture client = new org.mtr.mapping.NativeClientFixture()) {
            Registry registry = new Registry(); registry.setupPackets(new Identifier("mtr", "actionbar_test"));
            assertNotNull(LegacyPlayerMessages.serverSink, "Packet setup must bind the existing server channel");
            List<LegacyPlayerMessages.ActionBarPacket> outgoing = new ArrayList<>();
            List<net.minecraft.entity.player.EntityPlayerMP> recipients = new ArrayList<>();
            LegacyPlayerMessages.serverSink = (recipient, text) -> {
                recipients.add(recipient.data); outgoing.add(new LegacyPlayerMessages.ActionBarPacket(text));
            };
            List<net.minecraft.util.IChatComponent> chat = new ArrayList<>();
            net.minecraft.entity.player.EntityPlayerMP player = new net.minecraft.entity.player.EntityPlayerMP(server.server, server.world,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "MtrOverlay"), new net.minecraft.server.management.ItemInWorldManager(server.world)) {
                @Override public void addChatMessage(net.minecraft.util.IChatComponent text) { chat.add(text); }
            };
            new org.mtr.mapping.holder.PlayerEntity(player).sendMessage(
                org.mtr.mod.generated.lang.TranslationProvider.GUI_MTR_LIFT_BUTTONS_UNLOCKED.getText(), true);
            assertEquals(1, outgoing.size()); assertTrue(chat.isEmpty(), "Lift lock feedback must not enter ordinary chat");

            org.mtr.mapping.holder.Text ordinary = new org.mtr.mapping.holder.Text(new net.minecraft.util.ChatComponentText("Обычный чат"));
            new org.mtr.mapping.holder.PlayerEntity(player).sendMessage(ordinary, false);
            assertEquals(Collections.singletonList(ordinary.data), chat);
            net.minecraft.util.IChatComponent styled = new net.minecraft.util.ChatComponentTranslation("mtr.test.overlay", "Этаж 3");
            styled.getChatStyle().setColor(net.minecraft.util.EnumChatFormatting.GOLD);
            String expected = styled.getFormattedText();
            new org.mtr.mapping.holder.ServerPlayerEntity(player).sendMessage(new org.mtr.mapping.holder.Text(styled), true);
            assertEquals(2, outgoing.size()); assertEquals(Arrays.asList(player, player), recipients);
            assertEquals(1, chat.size(), "ServerPlayerEntity must preserve the action-bar flag too");
            styled.appendText(" changed after send");

            client.client.ingameGUI = new net.minecraft.client.gui.GuiIngame(client.client);
            RegistryClient.installPlayerMessages(client.client);
            net.minecraft.world.World localWorld = new org.mtr.mapping.BlockPlacementScenarios.MemoryWorld(); localWorld.isRemote = true;
            List<net.minecraft.util.IChatComponent> localChat = new ArrayList<>();
            net.minecraft.entity.player.EntityPlayer local = new net.minecraft.entity.player.EntityPlayer(localWorld,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "MtrLocalOverlay")) {
                public void addChatMessage(net.minecraft.util.IChatComponent text) { localChat.add(text); }
                public boolean canCommandSenderUseCommand(int level, String command) { return true; }
                public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return new net.minecraft.util.ChunkCoordinates(0, 64, 0); }
            };
            org.mtr.mapping.holder.Text localText = new org.mtr.mapping.holder.Text(new net.minecraft.util.ChatComponentText("Локальная подсказка"));
            new org.mtr.mapping.holder.PlayerEntity(local).sendMessage(localText, true);
            assertEquals(localText.data.getFormattedText(), overlayText(client.client)); assertTrue(localChat.isEmpty());
            new org.mtr.mapping.holder.PlayerEntity(local).sendMessage(ordinary, false);
            assertEquals(Collections.singletonList(ordinary.data), localChat);

            Field busField = FMLEventChannel.class.getDeclaredField("eventBus"); busField.setAccessible(true);
            EventBus bus = (EventBus) busField.get(registry.network.channel);
            NetworkManager manager = new NetworkManager(true);
            EmbeddedChannel channel = new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            Field nativeChannel = NetworkManager.class.getDeclaredField("channel"); nativeChannel.setAccessible(true); nativeChannel.set(manager, channel);
            manager.setNetHandler((INetHandlerPlayClient) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{INetHandlerPlayClient.class}, (proxy, method, arguments) -> null));
            try {
                try (PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer)) {
                    LegacyPlayerMessages.ActionBarPacket packet = outgoing.get(1);
                    sender.writeString(packet.getClass().getName()); packet.write(sender);
                    sender.send(frame -> {
                        try { bus.post(new FMLNetworkEvent.ClientCustomPacketEvent(manager, new FMLProxyPacket(frame, "mtr:actionbar_test"))); }
                        finally { frame.release(); }
                    }, Runnable::run);
                }
                assertEquals(localText.data.getFormattedText(), overlayText(client.client), "FML receipt must wait for the client tick");
                FMLCommonHandler.instance().bus().post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
                assertEquals(expected, overlayText(client.client), "Unicode, translation and style must survive the actual packet codec");
                assertEquals(60, (Integer) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.gui.GuiIngame.class,
                    client.client.ingameGUI, "recordPlayingUpFor", "field_73845_h"));
                assertFalse((Boolean) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.gui.GuiIngame.class,
                    client.client.ingameGUI, "recordIsPlaying", "field_73844_j"), "Action bars must not use the rainbow record flag");
                assertTrue(((List<?>) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.gui.GuiNewChat.class,
                    client.client.ingameGUI.getChatGUI(), "chatLines", "field_146252_h")).isEmpty(), "Overlay delivery must not create a chat line");
            } finally {
                FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager)); channel.finish();
            }
        } finally {
            LegacyPlayerMessages.serverSink = previousServer; LegacyPlayerMessages.clientSink = previousClient;
        }
    }
    private static String overlayText(net.minecraft.client.Minecraft client) {
        return cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.gui.GuiIngame.class,
            client.ingameGUI, "recordPlaying", "field_73838_g");
    }

    public void originalPressLiftPacketReachesCoreOnlyAfterServerTick() throws Exception {
        Registry registry = new Registry();
        registry.setupPackets(new Identifier("mtr", "lift_test"));
        registry.registerPacket(org.mtr.mod.packet.PacketPressLiftButton.class, org.mtr.mod.packet.PacketPressLiftButton::new);
        Field busField = FMLEventChannel.class.getDeclaredField("eventBus"); busField.setAccessible(true);
        EventBus bus = (EventBus) busField.get(registry.network.channel);
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture()) {
            org.mtr.core.simulation.Simulator simulator = new org.mtr.core.simulation.Simulator("minecraft/overworld",
                new String[]{"minecraft/overworld"}, fixture.world.getSaveHandler().getWorldDirectory().toPath().resolve("lift-test"), false);
            NetworkManager manager = new NetworkManager(false);
            EmbeddedChannel channel = new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
            Field nativeChannel = NetworkManager.class.getDeclaredField("channel");
            nativeChannel.setAccessible(true); nativeChannel.set(manager, channel);
            new NetHandlerPlayServer(fixture.server, manager, fixture.player);
            int[] forwarded = {0};
            org.mtr.mod.Init.forward = (world, request) -> {
                assertSame(fixture.world, world.data, "Request must use the authenticated player's server world");
                assertEquals(org.mtr.core.servlet.OperationProcessor.PRESS_LIFT, request.key);
                forwarded[0]++;
                simulator.sendMessageC2S(request);
            };
            try {
                org.mtr.core.data.Lift lift = new org.mtr.core.data.Lift(simulator);
                lift.setDimensions(3, 3, 3, 0, 0, 0);
                lift.setFloors(org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList.of(
                    new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(-17, 64, 0), "1", "Нижний"),
                    new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(-17, 80, 0), "2", "Средний"),
                    new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(-17, 96, 0), "3", "Верхний")));
                simulator.lifts.add(lift);
                for (int floor : new int[]{96, 64}) {
                    org.mtr.core.operation.PressLift call = new org.mtr.core.operation.PressLift();
                    call.add(new org.mtr.core.data.Position(-17, floor, 0), floor == 96
                        ? org.mtr.core.data.LiftDirection.DOWN : org.mtr.core.data.LiftDirection.UP);
                    org.mtr.mod.packet.PacketPressLiftButton packet = new org.mtr.mod.packet.PacketPressLiftButton(call);
                    int before = forwarded[0];
                    try (PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer)) {
                        sender.writeString(packet.getClass().getName()); packet.write(sender);
                        sender.send(frame -> {
                            try { bus.post(new FMLNetworkEvent.ServerCustomPacketEvent(manager, new FMLProxyPacket(frame, "mtr:lift_test"))); }
                            finally { frame.release(); }
                        }, Runnable::run);
                    }
                    assertEquals(before, forwarded[0]);
                    FMLCommonHandler.instance().bus().post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
                    assertEquals(before + 1, forwarded[0]);
                    simulator.tick();
                    for (int i = 0; i < 20000; i++) lift.tick(10);
                    assertEquals(floor == 96 ? "3" : "1", lift.getCurrentFloor().getNumber());
                }
            } finally {
                org.mtr.mod.Init.forward = null;
                FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ServerDisconnectionFromClientEvent(manager));
                channel.finish(); simulator.stop();
            }
        }
    }
    static int seen;
    public static class ProbePacket extends PacketHandler {
        final int value;
        ProbePacket(PacketBufferReceiver receiver) { value = receiver.readInt(); }
        public void write(PacketBufferSender sender) { sender.writeInt(value); }
        @Override public void runClient() { seen += value; }
    }

    public void realFmlChannelDispatchesOnlyOnGameTickAndRejectsTrailingData() throws Exception {
        Registry registry = new Registry();
        registry.setupPackets(new Identifier("mtr", "network_test"));
        registry.registerPacket(ProbePacket.class, ProbePacket::new);
        Field busField = FMLEventChannel.class.getDeclaredField("eventBus");
        busField.setAccessible(true);
        EventBus channelBus = (EventBus) busField.get(registry.network.channel);
        NetworkManager manager = new NetworkManager(true);
        EmbeddedChannel channel = new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        Field nativeChannel = NetworkManager.class.getDeclaredField("channel");
        nativeChannel.setAccessible(true); nativeChannel.set(manager, channel);
        manager.setNetHandler((INetHandlerPlayClient) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{INetHandlerPlayClient.class}, (proxy, method, arguments) -> null));
        seen = 0;
        try {
            deliver(channelBus, manager, false);
            assertEquals(0, seen);
            FMLCommonHandler.instance().bus().post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            assertEquals(7, seen);
            deliver(channelBus, manager, true);
            FMLCommonHandler.instance().bus().post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            assertEquals(7, seen, "Malformed data must be rejected before the handler changes game state");
            assertFalse(manager.isChannelOpen());
        } finally {
            FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager));
            channel.finish();
        }
    }

    static void deliver(EventBus bus, NetworkManager manager, boolean extra) {
        try (PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer)) {
            sender.writeString(ProbePacket.class.getName()); sender.writeInt(7);
            if (extra) sender.writeBoolean(true);
            sender.send(frame -> {
                try { bus.post(new FMLNetworkEvent.ClientCustomPacketEvent(manager, new FMLProxyPacket(frame, "mtr:network_test"))); }
                finally { frame.release(); }
            }, Runnable::run);
        }
    }

    public void disconnectedConnectionCannotRecreateSessionWhileNativeChannelIsStillOpen() throws Exception {
        Registry registry = new Registry();
        registry.setupPackets(new Identifier("mtr", "disconnect_test"));
        registry.registerPacket(ProbePacket.class, ProbePacket::new);
        Field busField = FMLEventChannel.class.getDeclaredField("eventBus");
        busField.setAccessible(true);
        EventBus bus = (EventBus) busField.get(registry.network.channel);
        NetworkManager manager = new NetworkManager(true);
        EmbeddedChannel channel = new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        Field nativeChannel = NetworkManager.class.getDeclaredField("channel");
        nativeChannel.setAccessible(true); nativeChannel.set(manager, channel);
        manager.setNetHandler((INetHandlerPlayClient) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{INetHandlerPlayClient.class}, (proxy, method, arguments) -> null));
        Field sessions = LegacyNetwork.class.getDeclaredField("sessions"); sessions.setAccessible(true);
        seen = 0;
        try {
            deliver(bus, manager, false);
            FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager));
            assertTrue(manager.isChannelOpen(), "FML disconnect is published before native channel close");
            deliver(bus, manager, false);
            FMLCommonHandler.instance().bus().post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            assertEquals(0, seen, "Neither queued nor late packets may act after disconnect");
            assertTrue(((Map<?, ?>) sessions.get(registry.network)).isEmpty(), "Late packet recreated a session");
        } finally { channel.finish(); }
    }
}
