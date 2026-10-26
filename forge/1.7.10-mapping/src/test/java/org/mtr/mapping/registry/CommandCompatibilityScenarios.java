package org.mtr.mapping.registry;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.*;
import net.minecraft.util.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class CommandCompatibilityScenarios {
    public void nativeCommandsKeepAliasesQuotedNamesPermissionsAndLifecycleCallbacks() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture()) {
            Registry registry = new Registry();
            List<String> seen = new ArrayList<>();
            registry.registerCommand("mtr_probe", root -> {
                root.then("depot", depot -> {
                    depot.permissionLevel(2);
                    depot.then("name", StringArgumentType.greedyString(), name -> name.executes(context -> {
                        assertSame(fixture.server, context.getServer().data); assertSame(fixture.world, context.getWorld().data);
                        assertNull(context.getServerPlayer());
                        seen.add(context.getString("name"));
                        context.sendSuccess("mtr.test.arg", false, context.getString("name"));
                        return 1;
                    }));
                });
                root.then("path", path -> path.then("value", StringArgumentType.string(), value -> value.executes(context -> {
                    seen.add(context.getString("value")); return 1;
                })));
                root.then("restore", restore -> {
                    restore.permissionLevel(4); restore.executes(context -> { seen.add("admin"); return 1; });
                });
            }, "mtr_probe_alias");
            registry.eventRegistry.registerServerStarted(server -> seen.add("started"));
            registry.eventRegistry.serverStarted(fixture.server);
            Map<String, ICommand> commands = fixture.server.getCommandManager().getCommands();
            ICommand nativeCommand = commands.get("mtr_probe");
            assertNotNull(nativeCommand); assertSame(nativeCommand, commands.get("mtr_probe_alias"));
            Sender user = new Sender(0, fixture.world), operator = new Sender(2, fixture.world), admin = new Sender(4, fixture.world);
            assertThrows(CommandException.class, () -> nativeCommand.processCommand(user, new String[]{"depot", "Central"}));
            assertThrows(CommandException.class, () -> nativeCommand.processCommand(operator, new String[]{"restore"}));
            assertEquals(Collections.singletonList("started"), seen);
            nativeCommand.processCommand(operator, new String[]{"depot", "Центральное", "депо"});
            assertEquals("Центральное депо", ((ChatComponentTranslation)operator.messages.get(0)).getFormatArgs()[0]);
            nativeCommand.processCommand(user, new String[]{"path", "\"путь", "с", "пробелом\""});
            nativeCommand.processCommand(admin, new String[]{"restore"});
            assertEquals(Arrays.asList("started", "Центральное депо", "путь с пробелом", "admin"), seen);
            assertEquals(Collections.singletonList("depot"), nativeCommand.addTabCompletionOptions(operator, new String[]{"d"}));
            assertFalse(nativeCommand.addTabCompletionOptions(operator, new String[]{"r"}).contains("restore"));
            CommandException syntax = assertThrows(CommandException.class,
                () -> nativeCommand.processCommand(user, new String[]{"path", "\"%s"}));
            String feedback = assertDoesNotThrow(() -> new ChatComponentTranslation(syntax.getMessage(), syntax.getErrorOjbects()).getUnformattedText());
            assertTrue(feedback.contains("%s"));
            registry.eventRegistry.serverStopping(fixture.server);
        }
    }
    private static final class Sender implements ICommandSender {
        final int level; final net.minecraft.world.World world;
        final List<IChatComponent> messages = new ArrayList<>();
        Sender(int level, net.minecraft.world.World world) { this.level = level; this.world = world; }
        public String getCommandSenderName() { return "MtrCommandTest"; }
        public IChatComponent func_145748_c_() { return new ChatComponentText(getCommandSenderName()); }
        public void addChatMessage(IChatComponent message) { messages.add(message); }
        public boolean canCommandSenderUseCommand(int level, String command) { return this.level >= level; }
        public ChunkCoordinates getPlayerCoordinates() { return new ChunkCoordinates(0, 64, 0); }
        public net.minecraft.world.World getEntityWorld() { return world; }
    }
}
