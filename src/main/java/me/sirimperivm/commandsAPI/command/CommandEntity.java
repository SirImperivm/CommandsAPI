package me.sirimperivm.commandsAPI.command;

import lombok.Getter;
import me.sirimperivm.commandsAPI.command.model.ExecutorType;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Represents an abstract base class for commands used within the system.
 *
 * This class acts as a blueprint for defining commands with attributes such as
 * name, permission, description, aliases, and execution type. It supports a
 * hierarchical structure of commands and subcommands, allowing complex command
 * systems to be implemented effectively.
 *
 * Key features include:
 * - Command metadata such as name, permission, description, and aliases.
 * - Support for different types of command executors (e.g., players, console, or both).
 * - Registration and retrieval of subcommands and arguments.
 * - Execution context management through a command sender and arguments.
 *
 * Subclasses are required to implement the `run` method to define the specific
 * behavior of the command during execution.
 */
@Getter
public abstract class CommandEntity {

    private final String name, permission, description;
    private final List<String> aliases;
    private final ExecutorType executorType;
    protected final Map<String, SubCommand> subcommands = new LinkedHashMap<>();
    protected final Map<String, Argument> arguments = new LinkedHashMap<>();

    protected CommandSender sender;
    protected String[] args;
    protected SubCommand executedSubCommand;

    /**
     * Constructs a CommandEntity with a specified name, while using default values
     * for other properties such as permission, description, aliases, and executor type.
     *
     * @param name The name of the command. It uniquely identifies the command and is required.
     */
    protected CommandEntity(String name) {
        this(name, null, null, Collections.emptyList(), ExecutorType.BOTH);
    }

    /**
     * Constructs a CommandEntity with a specified name and permission, while using
     * default values for other properties such as description, aliases, and executor type.
     *
     * @param name The name of the command. It uniquely identifies the command and is required.
     * @param permission The permission required to execute this command. It defines access control
     *                   for the command.
     */
    protected CommandEntity(String name, String permission) {
        this(name, permission, null, Collections.emptyList(), ExecutorType.BOTH);
    }

    /**
     * Constructs a CommandEntity with a specified name, permission, and description,
     * while using default values for other properties such as aliases and executor type.
     *
     * @param name The name of the command. It uniquely identifies the command and is required.
     * @param permission The permission required to execute this command. It defines access control for the command.
     * @param description A brief description of the command. It provides additional context or information.
     */
    protected CommandEntity(String name, String permission, String description) {
        this(name, permission, description, Collections.emptyList(), ExecutorType.BOTH);
    }

    /**
     * Constructs a CommandEntity with a specified name, permission, description, and aliases.
     * Uses a default executor type of BOTH.
     *
     * @param name The name of the command. It uniquely identifies the command and is required.
     * @param permission The permission required to execute this command. It defines access control for the command.
     * @param description A brief description of the command. It provides additional context or information.
     * @param aliases A list of alternative names for the command. These aliases allow users to invoke the command
     *                using different identifiers.
     */
    protected CommandEntity(String name, String permission, String description, List<String> aliases) {
        this(name, permission, description, aliases, ExecutorType.BOTH);
    }

    /**
     * Constructs a CommandEntity with specified name, permission, description, aliases, and executor type.
     *
     * @param name The name of the command. It uniquely identifies the command and is required.
     * @param permission The permission required to execute this command. It defines access control for the command.
     * @param description A brief description of the command. It provides additional context or information.
     * @param aliases A list of alternative names for the command. These aliases allow users to invoke the command
     *                using different identifiers.
     * @param executorType The type of executor allowed to run the command. If null, defaults to BOTH.
     */
    protected CommandEntity(String name, String permission, String description, List<String> aliases, ExecutorType executorType) {
        this.name = name;
        this.permission = permission;
        this.description = description;
        this.aliases = aliases != null ? new ArrayList<>(aliases) : new ArrayList<>();
        this.executorType = executorType != null ? executorType : ExecutorType.BOTH;
    }

    /**
     * Executes the command logic. This method must be implemented by subclasses
     * of the CommandEntity class to define the specific behavior of the command.
     *
     * This method serves as the entry point for performing the operation defined
     * by the command. The implementation should consider any required preconditions
     * such as proper access permissions or valid command arguments.
     */
    public abstract void run();

    /**
     * Sets the execution context for the command, including the sender and arguments.
     * This method is typically used to define the context in which the command will
     * execute, including identifying the entity issuing the command and any parameters
     * provided with it.
     *
     * @param sender The entity initiating the command. Could represent a player, console, or other invoker.
     * @param args An array of arguments passed with the command. These arguments provide
     *             additional input needed for processing the command.
     */
    public void setExecutionContext(CommandSender sender, String[] args) {
        this.sender = sender;
        this.args = args;
    }

    /**
     * Sets the subcommand that was executed.
     * This method is used internally by the command registry to track which subcommand
     * was invoked, allowing the parent command's run() method to determine the appropriate
     * logic to execute.
     *
     * @param subCommand The subcommand that was executed, or null if the main command was executed.
     */
    public void setExecutedSubCommand(SubCommand subCommand) {
        this.executedSubCommand = subCommand;
    }

    /**
     * Gets the subcommand that was executed.
     * This method allows the command's run() method to identify which subcommand was invoked
     * and execute the appropriate logic based on the subcommand's name.
     *
     * @return The subcommand that was executed, or null if the main command was executed
     *         without any subcommand.
     */
    public SubCommand getExecutedSubCommand() {
        return executedSubCommand;
    }

    /**
     * Registers a subcommand to the current command by adding it to the internal subcommand map.
     * The subcommand is stored using its name converted to lowercase as the key.
     *
     * @param subCommand The subcommand to be registered. Must not be null and should have
     *                   a valid name to serve as the identifier in the subcommand map.
     */
    public void registerSubCommand(SubCommand subCommand) {
        subcommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    /**
     * Registers an argument for the command by adding it to the internal argument map.
     * The argument is stored using its name, converted to lowercase, as the key.
     *
     * @param argument The argument to be registered. Must not be null and should have
     *                 a valid name to serve as the identifier in the argument map.
     */
    public void registerArgument(Argument argument) {
        arguments.put(argument.getName().toLowerCase(), argument);
    }

    /**
     * Retrieves a subcommand by its name from the internal subcommand map.
     * The name is converted to lowercase before performing the lookup.
     *
     * @param name The name of the subcommand to retrieve.
     *             Must not be null. The name is case-insensitive, and will
     *             be treated in lowercase during the lookup process.
     * @return The {@code SubCommand} associated with the given name, or {@code null}
     *         if no subcommand is found with that name.
     */
    public SubCommand getSubcommand(String name) {
        return subcommands.get(name.toLowerCase());
    }

    /**
     * Retrieves an argument by its name from the internal argument map.
     * The name is converted to lowercase before performing the lookup.
     *
     * @param name The name of the argument to retrieve. Must not be null.
     *             The name is case-insensitive and will be treated in lowercase during the lookup process.
     * @return The {@code Argument} associated with the given name, or {@code null}
     *         if no argument is found with that name.
     */
    public Argument getArgument(String name) {
        return arguments.get(name.toLowerCase());
    }

    /**
     * Checks if the command has a valid permission set.
     * A permission is considered valid if it is not null and not empty.
     *
     * @return {@code true} if the permission is set and not empty; {@code false} otherwise.
     */
    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }
}
