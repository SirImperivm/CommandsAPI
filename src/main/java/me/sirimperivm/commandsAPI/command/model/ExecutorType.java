package me.sirimperivm.commandsAPI.command.model;

/**
 * Defines the type of executor that can run a command.
 *
 * This enumeration helps to specify the source context from which a command
 * can be executed. It ensures proper handling and validation of commands
 * based on their executor type.
 *
 * The possible executor types are:
 *
 * - PLAYER: Indicates that the command must be executed by a player in-game.
 * - CONSOLE: Indicates that the command must be executed from the server console.
 * - BOTH: Indicates that the command can be executed by both the player and the console.
 */
public enum ExecutorType {

    PLAYER,
    CONSOLE,
    BOTH,
}
