package me.sirimperivm.commandsAPI.command;

import lombok.Getter;
import me.sirimperivm.commandsAPI.command.model.ExecutorType;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Represents a subcommand in a command hierarchy.
 *
 * This abstract class provides a framework for defining and managing subcommands.
 * Subcommands can have their own name, permission, description, aliases, and execution context.
 * Additionally, subcommands can have nested subcommands and required arguments.
 *
 * Subclasses are required to implement the {@code run} method, which encapsulates
 * the logic executed when the subcommand is called.
 *
 * Features include:
 * - Support for hierarchical subcommands.
 * - Permission-based execution.
 * - Alias matching for command names.
 * - Execution type validation (e.g., player, console, or both).
 * - Mechanisms to register subcommands and arguments.
 */
@Getter
public abstract class SubCommand {

    private final String name, permission, description;
    private final ExecutorType executorType;
    protected final Map<String, SubCommand> subcommands = new LinkedHashMap<>();
    protected final Map<String, Argument> arguments = new LinkedHashMap<>();

    protected CommandSender sender;
    protected String[] args;

    /**
     * Constructs a new SubCommand with the specified name.
     *
     * This constructor initializes a SubCommand with only the name specified.
     * Other properties, such as permission, description, aliases, and executor type,
     * are assigned default values. The executor type defaults to {@code ExecutorType.BOTH}.
     *
     * @param name the name of the subcommand. This is used to identify the subcommand
     *             and must be unique within the context of its parent command.
     */
    protected SubCommand(String name) {
        this(name, null, null, ExecutorType.BOTH);
    }

    /**
     * Constructs a new SubCommand with the specified name and permission.
     *
     * This constructor initializes a SubCommand with a name and a required permission.
     * Other properties, such as description, aliases, and executor type, are assigned
     * default values. Specifically, the description is set to {@code null}, aliases
     * are initialized as an empty list, and the executor type defaults to {@code ExecutorType.BOTH}.
     *
     * @param name       the name of the subcommand. This name is used to identify the subcommand
     *                   and must be unique within the context of its parent command.
     * @param permission the permission string required to execute the subcommand. This is used
     *                   to restrict access to users with the appropriate permissions.
     */
    protected SubCommand(String name, String permission) {
        this(name, permission, null, ExecutorType.BOTH);
    }

    /**
     * Constructs a new SubCommand with the specified name, permission, and description.
     *
     * This constructor initializes a SubCommand with a name, a required permission, and
     * a brief description. Other properties, such as aliases and executor type, are
     * assigned default values. The aliases default to an empty list, and the executor
     * type defaults to {@code ExecutorType.BOTH}.
     *
     * @param name        the name of the subcommand. This name is used to identify the
     *                    subcommand and must be unique within the context of its parent command.
     * @param permission  the permission string required to execute the subcommand. This is used
     *                    to restrict access to users with the appropriate permissions.
     * @param description a brief description of the subcommand. This provides additional clarity
     *                    about the subcommand's purpose or functionality.
     */
    protected SubCommand(String name, String permission, String description) {
        this(name, permission, description, ExecutorType.BOTH);
    }

    /**
     * Constructs a new SubCommand with the specified name, permission, description,
     * aliases, and executor type.
     *
     * This constructor initializes a SubCommand with all major properties explicitly provided.
     * The aliases list is deep-copied to ensure the immutability of the passed list.
     * If the aliases list is {@code null}, it is initialized to an empty list.
     * The executor type defaults to {@code ExecutorType.BOTH} if {@code null} is provided.
     *
     * @param name         the name of the subcommand. This name is used to identify the
     *                     subcommand and must be unique within the context of its parent command.
     * @param permission   the permission string required to execute the subcommand. This is used
     *                     to restrict access to users with the appropriate permissions.
     * @param description  a brief description of the subcommand. This provides additional
     *                     clarity about the subcommand's purpose or functionality.
     * @param executorType specifies the type of executor allowed to run the subcommand. If {@code null},
     *                     the default value {@code ExecutorType.BOTH} is used, allowing both players
     *                     and the console to execute the subcommand.
     */
    protected SubCommand(String name, String permission, String description, ExecutorType executorType) {
        this.name = name;
        this.permission = permission;
        this.description = description;
        this.executorType = executorType != null ? executorType : ExecutorType.BOTH;
    }

    /**
     * Executes the specific functionality of the subcommand.
     *
     * This method serves as the entry point for the execution logic of the subcommand.
     * It needs to be implemented by concrete subclasses to define the specific actions
     * to be performed when the subcommand is invoked.
     */
    public abstract void run();

    /**
     * Sets the execution context for the subcommand, including the command sender
     * and the arguments provided.
     *
     * This method is typically called before the subcommand's execution logic is
     * invoked, allowing the necessary runtime context (such as who triggered the
     * command and any parameters passed with it) to be set.
     *
     * @param sender the entity sending the command. This is typically a player,
     *               console, or command block.
     * @param args   the array of arguments supplied with the command when executed.
     *               This may include additional parameters or options for the subcommand.
     */
    public void setExecutionContext(CommandSender sender, String[] args) {
        this.sender = sender;
        this.args = args;
    }

    /**
     * Registers a subcommand by adding it to the subcommand map for tracking and access.
     * The subcommand is stored using its name (converted to lowercase) as the key.
     *
     * @param subCommand the subcommand to be registered. This must not be null and must
     *                   have a valid name that will be used as a unique identifier within
     *                   the context of the parent command.
     */
    public void registerSubCommand(SubCommand subCommand) {
        subcommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    /**
     * Registers an argument by adding it to the internal argument map.
     * The argument is stored using its name (converted to lowercase) as the key.
     *
     * @param argument the argument to be registered. This must not be null and must
     *                 have a valid name that will be used as a unique identifier
     *                 within the context of the command.
     */
    public void registerArgument(Argument argument) {
        arguments.put(argument.getName().toLowerCase(), argument);
    }

    /**
     * Retrieves a {@code SubCommand} instance associated with the specified name.
     * The name is looked up in a case-insensitive manner by converting it to lowercase.
     *
     * @param name the name of the subcommand to retrieve. This name is compared
     *             in a case-insensitive manner to locate the corresponding subcommand.
     * @return the {@code SubCommand} instance associated with the given name,
     *         or {@code null} if no matching subcommand is found.
     */
    public SubCommand getSubcommand(String name) {
        return subcommands.get(name.toLowerCase());
    }

    /**
     * Retrieves an {@code Argument} instance associated with the specified name.
     * The name is processed in a case-insensitive manner by converting it to lowercase
     * before performing the lookup.
     *
     * @param name the name of the argument to retrieve. This name is compared in
     *             a case-insensitive manner to locate the corresponding argument.
     * @return the {@code Argument} instance associated with the given name,
     *         or {@code null} if no matching argument is found.
     */
    public Argument getArgument(String name) {
        return arguments.get(name.toLowerCase());
    }

    /**
     * Checks if the permission is set and non-empty.
     *
     * @return true if the permission is not null and not empty, false otherwise.
     */
    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }
}
