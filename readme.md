# CommandsAPI for Paper 1.21+

A lightweight and intuitive API library for creating Brigadier-based commands in PaperMC 1.21+ plugins. This library simplifies the command registration process by providing an object-oriented approach with built-in support for subcommands, arguments, permissions, and exception handling.

[![](https://jitpack.io/v/SirImperivm/commandsapi-paper-1.21.svg)](https://jitpack.io/#SirImperivm/commandsapi-paper-1.21)

---

## 📦 Installation

### Gradle (Groovy)
```groovy
repositories { 
    maven { url 'https://jitpack.io' } 
}

dependencies {
    implementation 'com.github.SirImperivm:commandsapi-paper-1.21:<latest-version>'
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
        registry.registerAll();
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
```java
public class PointsCommand extends CommandEntity {
    public PointsCommand() {
        super("points", "myplugin.points");

        // Register subcommand: /points add <player> <amount>
        registerSubCommand(new SubCommand("add", "myplugin.points.add", "Add points to a player") {
            {
                registerArgument(Argument.builder("player").type(ArgType.STRING).build());
                registerArgument(Argument.builder("amount").type(ArgType.INTEGER).min(1).build());
            }

            @Override
            public void run() {
                String playerName = getArgument("player").getAsString();
                Integer amount = getArgument("amount").getAsInt();

                // Your logic here
                sender.sendMessage("Added " + amount + " points to " + playerName);
            }
        });
    }

    @Override
    public void run() {
        sender.sendMessage("Usage: /points add <player> <amount>");
    }
}
```

#### Nested SubCommands

SubCommands can contain other SubCommands, allowing you to create complex command hierarchies:

```java
public class AdminCommand extends CommandEntity {

    public AdminCommand() {
        super("admin", "myplugin.admin", "Administration commands");

        registerSubCommand(new SubCommand("user", "myplugin.admin.user", "User management") {
            {
                // /admin user ban <player> <reason>
                registerSubCommand(new SubCommand("ban", "myplugin.admin.user.ban", "Ban a player") {
                    {
                        registerArgument(Argument.builder("player").type(ArgType.STRING).build());
                        registerArgument(Argument.builder("reason").type(ArgType.STRING).greedy(true).build());
                    }

                    @Override
                    public void run() {
                        String playerName = getArgument("player").getAsString();
                        String reason = getArgument("reason").getAsString();

                        // Ban logic here
                        sender.sendMessage("§cBanned §e" + playerName + " §cfor: §7" + reason);
                    }
                });

                // /admin user kick <player>
                registerSubCommand(new SubCommand("kick", "myplugin.admin.user.kick", "Kick a player") {
                    {
                        registerArgument(Argument.builder("player").type(ArgType.STRING).build());
                    }

                    @Override
                    public void run() {
                        String playerName = getArgument("player").getAsString();

                        // Kick logic here
                        sender.sendMessage("§cKicked §e" + playerName);
                    }
                });
            }

            @Override
            public void run() {
                sender.sendMessage("§6=== User Management ===");
                sender.sendMessage("§e/admin user ban <player> <reason>");
                sender.sendMessage("§e/admin user kick <player>");
            }
        });

        // /admin reload
        registerSubCommand(new SubCommand("reload", "myplugin.admin.reload", "Reload configuration", null, ExecutorType.CONSOLE) {
            @Override
            public void run() {
                // Reload logic here
                sender.sendMessage("§aConfiguration reloaded!");
            }
        });
    }

    @Override
    public void run() {
        sender.sendMessage("§6=== Admin Commands ===");
        sender.sendMessage("§e/admin user §7- User management");
        sender.sendMessage("§e/admin reload §7- Reload configuration (console only)");
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

| Type      | Description    | Getter Method    |
|-----------|----------------|------------------|
| `STRING`  | Text value     | `getAsString()`  |
| `INTEGER` | 32-bit integer | `getAsInt()`     |
| `LONG`    | 64-bit integer | `getAsLong()`    |
| `DOUBLE`  | Decimal number | `getAsDouble()`  |
| `BOOLEAN` | true/false     | `getAsBoolean()` |

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
```java
// Player-only command super("fly", "myplugin.fly", "Toggle flight mode", null, ExecutorType.PLAYER);
// Console-only subcommand registerSubCommand(new SubCommand("reload", "myplugin.reload", "Reload configuration", null, ExecutorType.CONSOLE) { @Override public void run() { // Reload logic } });
```

---

## ⚠️ Exception Handling

The library provides a custom exception system that allows you to handle errors gracefully with customizable messages.

### CommandException

Throw a `CommandException` anywhere in your command's `run()` method:
```java
    @Override 
    public void run() {
        String playerName = getArgument("player").getAsString();
        Player target = Bukkit.getPlayer(playerName);
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
