package me.sirimperivm.commandsAPI;

import me.sirimperivm.commandsAPI.registry.CommandRegistry;
import org.bukkit.plugin.Plugin;

public final class CommandsAPI {

    /**
     * Creates and returns a new instance of {@code CommandRegistry} for the specified plugin.
     *
     * @param plugin the plugin instance for which the command registry will be created
     * @return a new {@code CommandRegistry} instance associated with the provided plugin
     */
    public static CommandRegistry createRegistry(Plugin plugin) {
        return new CommandRegistry(plugin);
    }
}
