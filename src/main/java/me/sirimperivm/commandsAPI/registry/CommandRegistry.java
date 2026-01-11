package me.sirimperivm.commandsAPI.registry;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.sirimperivm.commandsAPI.command.Argument;
import me.sirimperivm.commandsAPI.command.CommandEntity;
import me.sirimperivm.commandsAPI.command.SubCommand;
import me.sirimperivm.commandsAPI.command.exception.CommandException;
import me.sirimperivm.commandsAPI.command.exception.CommandExceptionHandler;
import me.sirimperivm.commandsAPI.command.model.ExecutorType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The CommandRegistry class is responsible for registering and managing commands for a given plugin.
 * It supports creating command hierarchies with subcommands and arguments, as well as handling command execution logic.
 * Additionally, it provides support for validation of permissions and executor types (player or console).
 */
public class CommandRegistry {

    private final Plugin plugin;
    private CommandExceptionHandler exceptionHandler;

    /**
     * Map of registered commands, keyed by command name (lowercase).
     * This allows updating commands at runtime.
     */
    private final Map<String, CommandEntity> registeredCommands = new ConcurrentHashMap<>();

    /**
     * Tracks whether the lifecycle event has already been registered.
     */
    private boolean lifecycleRegistered = false;

    /**
     * Constructs a new {@code CommandRegistry} instance, which is used to manage the
     * registration and execution of commands within the context of the specified plugin.
     *
     * @param plugin the plugin associated with this command registry, providing the
     *               necessary context and resources for command management
     */
    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the exception handler for the {@code CommandRegistry}, enabling custom handling
     * of {@link CommandException} instances that occur during command execution.
     *
     * @param handler the exception handler to be used, allowing for the customization
     *                of error management, logging, and user feedback during command execution
     * @return the current instance of {@code CommandRegistry}, enabling method chaining
     */
    public CommandRegistry setExceptionHandler(CommandExceptionHandler handler) {
        this.exceptionHandler = handler;
        return this;
    }

    /**
     * Registers a command to the plugin's lifecycle manager, allowing it to be
     * integrated into the command framework. If a command with the same name
     * already exists, the old command will be replaced with the new one.
     *
     * <p><b>Note:</b> For commands to be properly registered with Brigadier, this method
     * must be called during plugin initialization (before server fully starts).
     * However, the command logic can be updated at any time by calling this method
     * again with a new CommandEntity instance.</p>
     *
     * @param command the {@code CommandEntity} object representing the command to
     *                be registered, including its structure, behavior, and metadata
     */
    public void register(CommandEntity command) {
        String commandName = command.getName().toLowerCase();

        CommandEntity oldCommand = registeredCommands.put(commandName, command);

        if (!lifecycleRegistered) {
            registerLifecycleHandler();
        }
    }

    /**
     * Registers the lifecycle event handler that will register all pending commands
     * with Paper's Brigadier system.
     */
    private void registerLifecycleHandler() {
        if (lifecycleRegistered) return;
        lifecycleRegistered = true;

        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            for (CommandEntity cmd : registeredCommands.values()) {
                LiteralArgumentBuilder<CommandSourceStack> builder = buildCommand(cmd);
                commands.register(builder.build(), cmd.getDescription(), cmd.getAliases());
            }
        });
    }

    /**
     * Checks if a command with the given name is already registered.
     *
     * @param commandName the name of the command to check
     * @return true if a command with that name exists, false otherwise
     */
    public boolean isRegistered(String commandName) {
        return registeredCommands.containsKey(commandName.toLowerCase());
    }

    /**
     * Gets a registered command by name.
     *
     * @param commandName the name of the command
     * @return the CommandEntity, or null if not found
     */
    public CommandEntity getCommand(String commandName) {
        return registeredCommands.get(commandName.toLowerCase());
    }

    /**
     * Gets all registered command names.
     *
     * @return an unmodifiable set of command names
     */
    public Set<String> getRegisteredCommandNames() {
        return Collections.unmodifiableSet(registeredCommands.keySet());
    }

    /**
     * Builds a command using the provided {@code CommandEntity} object. This method
     * constructs a {@code LiteralArgumentBuilder} with the command's name, its subcommands,
     * and its argument chain. Additionally, it defines the execution logic associated with
     * the command.
     *
     * <p>The execution logic uses a lookup to the {@link #registeredCommands} map,
     * allowing command implementations to be updated at runtime.</p>
     *
     * @param command the {@code CommandEntity} representing the command to be built, including
     *                its name, subcommands, arguments, and associated behavior
     * @return a {@code LiteralArgumentBuilder<CommandSourceStack>} that encapsulates the
     *         structure and logic of the command
     */
    private LiteralArgumentBuilder<CommandSourceStack> buildCommand(CommandEntity command) {
        String commandName = command.getName().toLowerCase();
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal(command.getName());

        for (SubCommand subCommand : command.getSubcommands().values()) {
            builder.then(buildSubCommand(subCommand, commandName));
        }

        if (!command.getArguments().isEmpty()) {
            builder.then(buildArgumentChain(command.getArguments().values().iterator(), commandName, null));
        }

        builder.executes(ctx -> {
            CommandEntity currentCommand = registeredCommands.get(commandName);
            if (currentCommand == null) {
                plugin.getLogger().warning("[CommandsAPI] Command '/" + commandName + "' not found in registry!");
                return 0;
            }
            return executeCommand(ctx, currentCommand, null, Collections.emptyMap());
        });

        return builder;
    }

    /**
     * Builds a {@code LiteralArgumentBuilder<CommandSourceStack>} that defines the structure,
     * subcommands, argument chain, and execution logic for the provided {@code SubCommand}.
     *
     * @param subCommand the {@code SubCommand} representing the subcommand to be built,
     *                   including its name, subcommands, arguments, and associated behavior
     * @param rootCommandName the name of the root command for looking up in the registry
     * @return a {@code LiteralArgumentBuilder<CommandSourceStack>} that encapsulates the
     *         structure and execution logic of the specified subcommand
     */
    private LiteralArgumentBuilder<CommandSourceStack> buildSubCommand(SubCommand subCommand, String rootCommandName) {
        String subCommandName = subCommand.getName().toLowerCase();
        LiteralArgumentBuilder<CommandSourceStack> subBuilder = LiteralArgumentBuilder.literal(subCommand.getName());

        for (SubCommand nested : subCommand.getSubcommands().values()) {
            subBuilder.then(buildSubCommand(nested, rootCommandName));
        }

        if (!subCommand.getArguments().isEmpty()) {
            subBuilder.then(buildArgumentChain(subCommand.getArguments().values().iterator(), rootCommandName, subCommandName));
        }

        subBuilder.executes(ctx -> {
            CommandEntity currentCommand = registeredCommands.get(rootCommandName);
            if (currentCommand == null) return 0;
            SubCommand currentSub = currentCommand.getSubcommand(subCommandName);
            return executeCommand(ctx, currentCommand, currentSub, Collections.emptyMap());
        });

        return subBuilder;
    }

    /**
     * Constructs a chain of argument nodes for a command based on the provided iterator
     * of arguments, root command, and subcommand.
     */
    private CommandNode<CommandSourceStack> buildArgumentChain(
            Iterator<Argument> iterator,
            String rootCommandName,
            String subCommandName
    ) {
        if (!iterator.hasNext()) {
            return null;
        }

        Argument arg = iterator.next();
        RequiredArgumentBuilder<CommandSourceStack, ?> argBuilder = createArgumentBuilder(arg);

        if (iterator.hasNext()) {
            CommandNode<CommandSourceStack> next = buildArgumentChain(iterator, rootCommandName, subCommandName);
            if (next != null) {
                argBuilder.then(next);
            }
        }

        argBuilder.executes(ctx -> {
            CommandEntity currentCommand = registeredCommands.get(rootCommandName);
            if (currentCommand == null) return 0;

            SubCommand currentSub = subCommandName != null ? currentCommand.getSubcommand(subCommandName) : null;
            Object target = currentSub != null ? currentSub : currentCommand;

            Map<String, Object> parsedArgs = extractArguments(ctx, target);
            return executeCommand(ctx, currentCommand, currentSub, parsedArgs);
        });

        return argBuilder.build();
    }

    /**
     * Creates a {@code RequiredArgumentBuilder} for the given argument, mapping its type, range,
     * and other properties to a suitable argument type within the command framework.
     *
     * @param arg the {@code Argument} object containing details about the argument, such as its
     *            name, type, range, and whether it supports greedy parsing for strings
     * @return a {@code RequiredArgumentBuilder<CommandSourceStack, ?>} representing the argument
     *         definition, ready to be added to a command's argument chain
     */
    private RequiredArgumentBuilder<CommandSourceStack, ?> createArgumentBuilder(Argument arg) {
        return switch (arg.getType()) {
            case INTEGER -> {
                int min = (int) Math.max(arg.getMin(), Integer.MIN_VALUE);
                int max = (int) Math.min(arg.getMax(), Integer.MAX_VALUE);
                yield RequiredArgumentBuilder.argument(arg.getName(), IntegerArgumentType.integer(min, max));
            }
            case LONG -> RequiredArgumentBuilder.argument(arg.getName(),
                    LongArgumentType.longArg(arg.getMin(), arg.getMax()));
            case DOUBLE -> {
                double min = Math.max(arg.getMin(), -Double.MAX_VALUE);
                double max = Math.min(arg.getMax(), Double.MAX_VALUE);
                yield RequiredArgumentBuilder.argument(arg.getName(), DoubleArgumentType.doubleArg(min, max));
            }
            case BOOLEAN -> RequiredArgumentBuilder.argument(arg.getName(),
                    BoolArgumentType.bool());
            case PLAYER -> {
                RequiredArgumentBuilder<CommandSourceStack, String> playerArg =
                        RequiredArgumentBuilder.argument(arg.getName(), StringArgumentType.word());
                playerArg.suggests((context, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(remaining)) {
                            builder.suggest(player.getName());
                        }
                    }
                    return builder.buildFuture();
                });
                yield playerArg;
            }
            default -> {
                if (arg.isGreedy()) {
                    yield RequiredArgumentBuilder.argument(arg.getName(), StringArgumentType.greedyString());
                } else {
                    yield RequiredArgumentBuilder.argument(arg.getName(), StringArgumentType.word());
                }
            }
        };
    }

    /**
     * Extracts and converts the arguments provided in the {@code CommandContext} into a map
     * of argument names and their corresponding values. The types of the arguments are determined
     * dynamically based on the argument definitions of the given command or subcommand.
     *
     * @param ctx the {@code CommandContext<CommandSourceStack>} containing the context of the
     *            currently executing command, including parsed arguments and execution state
     * @param commandOrSub an {@code Object} representing the command or subcommand from which
     *                     to retrieve the argument definitions, expected to be either a
     *                     {@code CommandEntity} or {@code SubCommand}
     * @return a {@code Map<String, Object>} containing argument names as lowercase keys and their
     *         associated converted values, or an empty map if no arguments are successfully parsed
     */
    private Map<String, Object> extractArguments(CommandContext<CommandSourceStack> ctx, Object commandOrSub) {
        Map<String, Argument> args = commandOrSub instanceof CommandEntity ce
                ? ce.getArguments()
                : ((SubCommand) commandOrSub).getArguments();

        Map<String, Object> result = new HashMap<>();
        for (Argument arg : args.values()) {
            try {
                Object value = switch (arg.getType()) {
                    case INTEGER -> IntegerArgumentType.getInteger(ctx, arg.getName());
                    case LONG -> LongArgumentType.getLong(ctx, arg.getName());
                    case DOUBLE -> DoubleArgumentType.getDouble(ctx, arg.getName());
                    case BOOLEAN -> BoolArgumentType.getBool(ctx, arg.getName());
                    case PLAYER -> StringArgumentType.getString(ctx, arg.getName());
                    default -> StringArgumentType.getString(ctx, arg.getName());
                };
                result.put(arg.getName().toLowerCase(), value);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    /**
     * Executes a command or sub-command by validating permissions, setting context,
     * and invoking the appropriate command logic.
     *
     * @param ctx The {@link CommandContext} providing the source of the command invocation.
     * @param command The primary {@link CommandEntity} that is being executed.
     * @param subCommand The optional {@link SubCommand} to execute; can be null if no specific sub-command is provided.
     * @param parsedArgs A map of parsed argument keys to their respective values for execution.
     * @return An integer indicating the result code of execution; typically 1 for success and 0 for failure.
     * @throws CommandException If the command encounters an error, such as a missing permission or invalid context.
     */
    private int executeCommand(
            CommandContext<CommandSourceStack> ctx,
            CommandEntity command,
            SubCommand subCommand,
            Map<String, Object> parsedArgs
    ) throws CommandException{
        CommandSender sender = ctx.getSource().getSender();
        ExecutorType executorType = subCommand != null ? subCommand.getExecutorType() : command.getExecutorType();

        try {
            String permission = subCommand != null && subCommand.hasPermission()
                    ? subCommand.getPermission()
                    : command.getPermission();

            if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                throw new CommandException("sender.no-permission").with("permission", permission);
            }

            checkExecutorType(sender, executorType);

            Map<String, Argument> targetArgs = subCommand != null ? subCommand.getArguments() : command.getArguments();
            for (Map.Entry<String, Object> entry : parsedArgs.entrySet()) {
                Argument arg = targetArgs.get(entry.getKey());
                if (arg != null) {
                    arg.setValue(entry.getValue());
                }
            }

            if (subCommand != null) {
                subCommand.setExecutionContext(sender, new String[0]);
                subCommand.run();
            } else {
                command.setExecutionContext(sender, new String[0]);
                command.run();
            }
            return 1;
        } catch (CommandException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(sender, e);
            } else {
                plugin.getLogger().warning("[CommandException] " + e.getErrorId());
            }
            return 0;
        } catch (Exception e) {
            plugin.getLogger().severe("Command execution error: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Verifies if the provided {@code CommandSender} matches the specified {@code ExecutorType}.
     * Throws a {@code CommandException} if the sender's type does not align with the required executor type.
     *
     * @param sender the {@code CommandSender} that initiated the command, which can be either
     *               a {@code Player} or {@code ConsoleCommandSender}.
     * @param type the {@code ExecutorType} against which the {@code CommandSender} will be validated,
     *             indicating whether the command must be executed by a player or console.
     * @throws CommandException if the {@code CommandSender} does not match the specified {@code ExecutorType}.
     */
    private void checkExecutorType(CommandSender sender, ExecutorType type) throws CommandException {
        switch (type) {
            case PLAYER -> {
                if (sender instanceof Player) return;
                throw new CommandException("executor-type.player");
            }
            case CONSOLE -> {
                if (sender instanceof ConsoleCommandSender) return;
                throw new CommandException("executor-type.console");
            }
        }
    }
}
