# CommandsAPI for Paper 1.21+

A lightweight and intuitive API library for creating Brigadier-based commands in PaperMC 1.21+ plugins. This library simplifies the command registration process by providing an object-oriented approach with built-in support for subcommands, arguments, permissions, and exception handling.

[![](https://jitpack.io/v/SirImperivm/CommandsAPI.svg)](https://jitpack.io/#SirImperivm/CommandsAPI)

---

## 📦 Installation

### Gradle (Groovy)
```groovy
repositories { 
    maven { url 'https://jitpack.io' } 
}

dependencies {
    implementation 'com.github.SirImperivm:CommandsAPI:<latest-version>'
}
```

### Gradle (Kotlin DSL)
```kotlin
repositories { 
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.SirImperivm:commandsapi-paper-1.21:<latest-version>")
}
```

### Maven
```xml
<repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```xml
<dependencies>
    <dependency>
        <groupId>com.github.SirImperivm</groupId>
        <artifactId>commandsapi-paper-1.21</artifactId>
        <version>latest-version</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

> **Note:** Replace `<latest-version>` with the actual version number (e.g., `0.0.2`).

---

## 🚀 Quick Start

### 1. Create a Command
```java
import me.sirimperivm.commandsAPI.command.Argument; 
import me.sirimperivm.commandsAPI.command.CommandEntity; 
import me.sirimperivm.commandsAPI.command.SubCommand; 
import me.sirimperivm.commandsAPI.command.model.ArgType; 
import me.sirimperivm.commandsAPI.command.model.ExecutorType;

import java.util.List;
public class HelloCommand extends CommandEntity {
    public HelloCommand() {
        super("hello", "myplugin.hello", "Say hello to someone", List.of("hi", "greet"), ExecutorType.BOTH);

        registerArgument(Argument.builder("name").type(ArgType.STRING).build());
    }

    @Override
    public void run() {
        String name = getArgument("name").getAsString();

        if (name == null) {
            sender.sendMessage("Hello, World!");
        } else {
            sender.sendMessage("Hello, " + name + "!");
        }
    }
}
```

### 2. Register the Command
```java
import me.sirimperivm.commandsAPI.CommandsAPI; 
import me.sirimperivm.commandsAPI.registry.CommandRegistry; 
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        CommandRegistry registry = CommandsAPI.createRegistry(this);

        registry.register(new HelloCommand());
    }
}
```

---

## 📖 Core Concepts

### CommandEntity

The base class for creating commands. Extend this class to define your own commands.

#### Constructor Parameters

| Parameter      | Type         | Required | Default | Description                         |
|----------------|--------------|----------|---------|-------------------------------------|
| `name`         | String       | ✅        | -       | The command name (e.g., "points")   |
| `permission`   | String       | ❌        | null    | Permission node required to execute |
| `description`  | String       | ❌        | null    | Command description shown in help   |
| `aliases`      | List<String> | ❌        | empty   | Alternative command names           |
| `executorType` | ExecutorType | ❌        | BOTH    | Who can execute this command        |

#### Available Constructors
```java
// Minimal super("mycommand");
// With permission super("mycommand", "myplugin.mycommand");
// With permission and description super("mycommand", "myplugin.mycommand", "My command description");
// With aliases super("mycommand", "myplugin.mycommand", "Description", List.of("mc", "mycmd"));
// Full super("mycommand", "myplugin.mycommand", "Description", List.of("mc"), ExecutorType.PLAYER);
```

---

### SubCommand

Subcommands allow you to split command functionality into logical parts.

**Important:** All subcommand logic is handled in the parent command's `run()` method. Use `getExecutedSubCommand()` to identify which subcommand was invoked and handle it accordingly.

#### Basic SubCommand Example

```java
public class PointsCommand extends CommandEntity {
    public PointsCommand() {
        super("points", "myplugin.points");

        // Register subcommand: /points add <player> <amount>
        SubCommand addSubCommand = new SubCommand("add", "myplugin.points.add", "Add points to a player");
        addSubCommand.registerArgument(Argument.builder("player").type(ArgType.PLAYER).build());
        addSubCommand.registerArgument(Argument.builder("amount").type(ArgType.INTEGER).min(1).build());
        registerSubCommand(addSubCommand);

        // Register subcommand: /points remove <player> <amount>
        SubCommand removeSubCommand = new SubCommand("remove", "myplugin.points.remove", "Remove points from a player");
        removeSubCommand.registerArgument(Argument.builder("player").type(ArgType.PLAYER).build());
        removeSubCommand.registerArgument(Argument.builder("amount").type(ArgType.INTEGER).min(1).build());
        registerSubCommand(removeSubCommand);
    }

    @Override
    public void run() {
        SubCommand subCommand = getExecutedSubCommand();

        if (subCommand == null) {
            sender.sendMessage("Usage: /points <add|remove> <player> <amount>");
            return;
        }

        switch (subCommand.getName()) {
            case "add" -> {
                Player target = subCommand.getArgument("player").getAsPlayer();
                Integer amount = subCommand.getArgument("amount").getAsInt();

                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return;
                }

                // Your logic here
                sender.sendMessage("§aAdded " + amount + " points to " + target.getName());
            }
            case "remove" -> {
                Player target = subCommand.getArgument("player").getAsPlayer();
                Integer amount = subCommand.getArgument("amount").getAsInt();

                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return;
                }

                // Your logic here
                sender.sendMessage("§cRemoved " + amount + " points from " + target.getName());
            }
        }
    }
}
```

#### Key Points

- **No separate `run()` method**: SubCommands no longer have their own `run()` method. All logic is centralized in the parent command.
- **Identify subcommands**: Use `getExecutedSubCommand()` in your main `run()` method to check which subcommand was executed.
- **Access arguments**: Get arguments from the subcommand using `subCommand.getArgument("name")`.
- **Null check**: If `getExecutedSubCommand()` returns `null`, the main command was executed without any subcommand.
- **Nested subcommands**: For nested subcommands (e.g., `/admin user ban`):
  - `getExecutedSubCommand()` returns **only the last subcommand** in the chain (e.g., `"ban"`)
  - `getSubCommandChain()` returns **the full chain** of subcommands (e.g., `[user, ban]`)

#### Nested SubCommands

SubCommands can contain other SubCommands, allowing you to create complex command hierarchies:

```java
public class AdminCommand extends CommandEntity {

    public AdminCommand() {
        super("admin", "myplugin.admin", "Administration commands");

        // Create user management subcommand
        SubCommand userSubCommand = new SubCommand("user", "myplugin.admin.user", "User management");

        // /admin user ban <player> <reason>
        SubCommand banSubCommand = new SubCommand("ban", "myplugin.admin.user.ban", "Ban a player");
        banSubCommand.registerArgument(Argument.builder("player").type(ArgType.PLAYER).build());
        banSubCommand.registerArgument(Argument.builder("reason").type(ArgType.STRING).greedy(true).build());
        userSubCommand.registerSubCommand(banSubCommand);

        // /admin user kick <player>
        SubCommand kickSubCommand = new SubCommand("kick", "myplugin.admin.user.kick", "Kick a player");
        kickSubCommand.registerArgument(Argument.builder("player").type(ArgType.PLAYER).build());
        userSubCommand.registerSubCommand(kickSubCommand);

        registerSubCommand(userSubCommand);

        // /admin reload
        SubCommand reloadSubCommand = new SubCommand("reload", "myplugin.admin.reload", "Reload configuration", ExecutorType.CONSOLE);
        registerSubCommand(reloadSubCommand);
    }

    @Override
    public void run() {
        SubCommand subCommand = getExecutedSubCommand();

        if (subCommand == null) {
            sender.sendMessage("§6=== Admin Commands ===");
            sender.sendMessage("§e/admin user §7- User management");
            sender.sendMessage("§e/admin reload §7- Reload configuration (console only)");
            return;
        }

        switch (subCommand.getName()) {
            case "user" -> {
                sender.sendMessage("§6=== User Management ===");
                sender.sendMessage("§e/admin user ban <player> <reason>");
                sender.sendMessage("§e/admin user kick <player>");
            }
            case "ban" -> {
                Player target = subCommand.getArgument("player").getAsPlayer();
                String reason = subCommand.getArgument("reason").getAsString();

                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return;
                }

                // Ban logic here
                sender.sendMessage("§cBanned §e" + target.getName() + " §cfor: §7" + reason);
            }
            case "kick" -> {
                Player target = subCommand.getArgument("player").getAsPlayer();

                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return;
                }

                // Kick logic here
                sender.sendMessage("§cKicked §e" + target.getName());
            }
            case "reload" -> {
                // Reload logic here
                sender.sendMessage("§aConfiguration reloaded!");
            }
        }
    }
}
```

---

### Argument

Arguments define the parameters that commands and subcommands accept.

#### Builder Methods

| Method              | Type    | Default           | Description                               |
|---------------------|---------|-------------------|-------------------------------------------|
| `type(ArgType)`     | ArgType | STRING            | The argument data type                    |
| `min(long)`         | long    | Long.MIN_VALUE    | Minimum value (for numbers)               |
| `max(long)`         | long    | Long.MAX_VALUE    | Maximum value (for numbers)               |
| `minLen(int)`       | int     | 0                 | Minimum length (for strings)              |
| `maxLen(int)`       | int     | Integer.MAX_VALUE | Maximum length (for strings)              |
| `greedy(boolean)`   | boolean | false             | Capture all remaining input as one string |
| `optional(boolean)` | boolean | false             | Whether the argument is optional          |

#### ArgType Enum

| Type      | Description                                      | Getter Method    |
|-----------|--------------------------------------------------|------------------|
| `STRING`  | Text value                                       | `getAsString()`  |
| `INTEGER` | 32-bit integer                                   | `getAsInt()`     |
| `LONG`    | 64-bit integer                                   | `getAsLong()`    |
| `DOUBLE`  | Decimal number                                   | `getAsDouble()`  |
| `BOOLEAN` | true/false                                       | `getAsBoolean()` |
| `PLAYER`  | Online player name with tab-completion support   | `getAsPlayer()`  |

#### Examples
```java
// Simple string argument Argument.builder("player").build();
// Integer with range Argument.builder("amount").type(ArgType.INTEGER).min(1).max(1000).build();
// Greedy string (captures everything after) Argument.builder("message").type(ArgType.STRING).greedy(true).build();
// Optional argument Argument.builder("target").optional(true).build();
```

---

### ExecutorType

Controls who can execute a command or subcommand.

| Type      | Description                                    |
|-----------|------------------------------------------------|
| `BOTH`    | Both players and console can execute (default) |
| `PLAYER`  | Only in-game players can execute               |
| `CONSOLE` | Only the server console can execute            |

#### Examples
```java
// Player-only command
public class FlyCommand extends CommandEntity {
    public FlyCommand() {
        super("fly", "myplugin.fly", "Toggle flight mode", null, ExecutorType.PLAYER);
    }

    @Override
    public void run() {
        Player player = (Player) sender;
        player.setAllowFlight(!player.getAllowFlight());
        player.sendMessage("§aFlight " + (player.getAllowFlight() ? "enabled" : "disabled"));
    }
}

// Console-only subcommand
SubCommand reloadSubCommand = new SubCommand("reload", "myplugin.reload", "Reload configuration", ExecutorType.CONSOLE);
registerSubCommand(reloadSubCommand);
```

---

## 🔄 Migration Guide

If you're upgrading from a previous version where SubCommands had their own `run()` method, here's how to migrate:

### Before (Old API)
```java
registerSubCommand(new SubCommand("add", "myplugin.points.add") {
    {
        registerArgument(Argument.builder("player").type(ArgType.PLAYER).build());
        registerArgument(Argument.builder("amount").type(ArgType.INTEGER).build());
    }

    @Override
    public void run() {
        Player target = getArgument("player").getAsPlayer();
        Integer amount = getArgument("amount").getAsInt();
        // Logic here
    }
});
```

### After (New API)
```java
SubCommand addSubCommand = new SubCommand("add", "myplugin.points.add");
addSubCommand.registerArgument(Argument.builder("player").type(ArgType.PLAYER).build());
addSubCommand.registerArgument(Argument.builder("amount").type(ArgType.INTEGER).build());
registerSubCommand(addSubCommand);

@Override
public void run() {
    SubCommand subCommand = getExecutedSubCommand();
    if (subCommand != null && subCommand.getName().equals("add")) {
        Player target = subCommand.getArgument("player").getAsPlayer();
        Integer amount = subCommand.getArgument("amount").getAsInt();
        // Logic here
    }
}
```

**Benefits of the new approach:**
- Centralized logic in one place
- Easier to share code between subcommands
- Better overview of all command behavior
- Simplified debugging and maintenance

---

## ⚠️ Exception Handling

The library provides a custom exception system that allows you to handle errors gracefully with customizable messages.

### CommandException

Throw a `CommandException` anywhere in your command's `run()` method:
```java
    @Override 
    public void run() {
        String playerName = getArgument("player").getAsString();
        Player target = getArgument("player").getAsPlayer();
        if (target == null) {
            throw new CommandException("player.not-found")
                    .with("player", playerName);
        }
    
        if (target.getHealth() <= 0) {
            throw new CommandException("player.dead")
                    .with("player", playerName)
                    .with("health", target.getHealth());
        }
    
    // Continue with logic...
    }
```

### CommandExceptionHandler

Set up a global exception handler to convert error IDs into user-friendly messages:
```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandRegistry registry = CommandsAPI.createRegistry(this);
        registry.setExceptionHandler((sender, ex) -> {
            String msg = switch (ex.getErrorId()) {
                case "sender.no-permission" -> "§cYou don't have permission: " + ex.get("permission");
                case "executor-type.player" -> "§cCommand only for in-game players!";
                case "executor-type.console" -> "§cCommand only for console!";
                case "player.not-found" -> "§cPlayer " + ex.get("name") + " not found!";
                default -> "§cError: " + ex.getErrorId();
            };
            sender.sendMessage(msg);
        });
    }
}
```

### Built-in Error IDs

| Error ID                | Context Keys | Description                        |
|-------------------------|--------------|------------------------------------|
| `sender.no-permission`  | `permission` | Sender lacks required permission   |
| `executor-type.player`  | -            | Command requires a player executor |
| `executor-type.console` | -            | Command requires console executor  |

---
