package org.mtr.mapping.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class LegacyCommand extends CommandBase {
    private final String name;
    private final List<String> aliases;
    private final CommandDispatcher<ICommandSender> dispatcher = new CommandDispatcher<>();
    private MinecraftServer server;
    LegacyCommand(String name, Consumer<CommandBuilder<?>> consumer, String... aliases) {
        this.name = name; this.aliases = Collections.unmodifiableList(Arrays.asList(aliases.clone()));
        CommandBuilder<LiteralArgumentBuilder<ICommandSender>> root = new CommandBuilder<>(LiteralArgumentBuilder.literal(name), this);
        consumer.accept(root); dispatcher.register(root.builder);
    }
    void install(MinecraftServer server) {
        this.server = server; ((CommandHandler) server.getCommandManager()).registerCommand(this);
    }
    void detach(MinecraftServer server) { if (this.server == server) this.server = null; }
    MinecraftServer getServer() { return Objects.requireNonNull(server, "Command is not attached to a running server"); }
    @Override public String getCommandName() { return name; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/" + name; }
    @Override public List<String> getCommandAliases() { return aliases; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return dispatcher.getRoot().getChild(name).canUse(sender);
    }
    @Override public void processCommand(ICommandSender sender, String[] arguments) {
        try { dispatcher.execute(name + " " + String.join(" ", arguments), sender); }
        catch (CommandSyntaxException exception) { throw new CommandException("%s", exception.getMessage()); }
    }
    @Override public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        String input = name + " " + String.join(" ", arguments);
        ParseResults<ICommandSender> parsed = dispatcher.parse(input, sender);
        SuggestionContext<ICommandSender> context = parsed.getContext().findSuggestionContext(input.length());
        List<Suggestions> suggestions = new ArrayList<>();
        // Modern clients receive a permission-filtered command tree; the legacy server must filter suggestions itself.
        for (CommandNode<ICommandSender> child : context.parent.getChildren()) {
            if (!child.canUse(sender)) continue;
            try { suggestions.add(child.listSuggestions(parsed.getContext().build(input), new SuggestionsBuilder(input, context.startPos)).join()); }
            catch (CommandSyntaxException ignored) {}
        }
        return Suggestions.merge(input, suggestions).getList().stream().map(suggestion -> suggestion.getText()).collect(Collectors.toList());
    }
}
