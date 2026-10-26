package org.mtr.mapping.registry;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.*;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import java.util.function.*;

public final class CommandBuilder<T extends ArgumentBuilder<ICommandSender, T>> {
    T builder;
    private final LegacyCommand command;
    CommandBuilder(T builder, LegacyCommand command) { this.builder = builder; this.command = command; }
    public void permissionLevel(int level) { builder = builder.requires(source -> source.canCommandSenderUseCommand(level, command.getCommandName())); }
    public void then(String name, Consumer<CommandBuilder<?>> consumer) {
        CommandBuilder<LiteralArgumentBuilder<ICommandSender>> child = new CommandBuilder<>(LiteralArgumentBuilder.literal(name), command);
        consumer.accept(child); builder = builder.then(child.builder);
    }
    public <U> void then(String name, ArgumentType<U> type, Consumer<CommandBuilder<?>> consumer) {
        CommandBuilder<RequiredArgumentBuilder<ICommandSender, U>> child = new CommandBuilder<>(RequiredArgumentBuilder.argument(name, type), command);
        consumer.accept(child); builder = builder.then(child.builder);
    }
    public void executes(ToIntFunction<ContextHandler> execute) {
        builder = builder.executes(context -> execute.applyAsInt(new ContextHandler(context, command)));
    }
    public static final class ContextHandler {
        private final CommandContext<ICommandSender> context;
        private final LegacyCommand command;
        private ContextHandler(CommandContext<ICommandSender> context, LegacyCommand command) { this.context = context; this.command = command; }
        public boolean getBoolean(String name) { return BoolArgumentType.getBool(context, name); }
        public double getDouble(String name) { return DoubleArgumentType.getDouble(context, name); }
        public float getFloat(String name) { return FloatArgumentType.getFloat(context, name); }
        public int getInteger(String name) { return IntegerArgumentType.getInteger(context, name); }
        public long getLong(String name) { return LongArgumentType.getLong(context, name); }
        public String getString(String name) { return StringArgumentType.getString(context, name); }
        public MinecraftServer getServer() { return new MinecraftServer(command.getServer()); }
        public World getWorld() { return new World(context.getSource().getEntityWorld()); }
        public ServerPlayerEntity getServerPlayer() {
            return context.getSource() instanceof net.minecraft.entity.player.EntityPlayerMP
                ? new ServerPlayerEntity((net.minecraft.entity.player.EntityPlayerMP) context.getSource()) : null;
        }
        public void sendSuccess(String message, boolean broadcastToOps, Object... arguments) {
            if (broadcastToOps) net.minecraft.command.CommandBase.func_152373_a(context.getSource(), command, message, arguments);
            else context.getSource().addChatMessage(new ChatComponentTranslation(message, arguments));
        }
        public void sendFailure(String message, Object... arguments) {
            IChatComponent text = new ChatComponentTranslation(message, arguments);
            text.getChatStyle().setColor(EnumChatFormatting.RED); context.getSource().addChatMessage(text);
        }
    }
}
