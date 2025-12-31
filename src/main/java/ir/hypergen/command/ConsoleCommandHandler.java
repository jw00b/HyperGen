package ir.hypergen.command;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Map;

public class ConsoleCommandHandler {
    private final HyperGen plugin;
    
    public ConsoleCommandHandler(HyperGen plugin) {
        this.plugin = plugin;
    }
    
    public void handleConsoleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            return;
        }
        
        if (args.length == 0) {
            sendConsoleHelp(sender);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                handleConsoleStart(sender, args);
                break;
            case "pause":
                handleConsolePause(sender, args);
                break;
            case "continue":
            case "resume":
                handleConsoleContinue(sender, args);
                break;
            case "cancel":
            case "stop":
                handleConsoleCancel(sender, args);
                break;
            case "progress":
            case "status":
                handleConsoleProgress(sender);
                break;
            case "silent":
                handleConsoleSilent(sender);
                break;
            case "reload":
                handleConsoleReload(sender);
                break;
            default:
                sender.sendMessage("Unknown command. Use 'hypergen help' for available commands.");
                break;
        }
    }
    
    private void handleConsoleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: hypergen start <world> [mode] [radius]");
            return;
        }
        
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage("World not found: " + args[1]);
            return;
        }
        
        GenerationTask.GenerationMode mode = GenerationTask.GenerationMode.NORMAL;
        if (args.length > 2) {
            try {
                mode = GenerationTask.GenerationMode.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("Invalid mode. Available: NORMAL, PRO, FAST");
                return;
            }
        }
        
        int radius = 100;
        if (args.length > 3) {
            try {
                radius = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid radius number");
                return;
            }
        }
        
        var selection = plugin.getSelectionManager().createConsoleSelection(world, radius);
        plugin.getTaskManager().startTask(world, selection, mode);
        
        sender.sendMessage("Chunk generation started for world: " + world.getName());
        sender.sendMessage("Mode: " + mode + " | Radius: " + radius + " chunks");
    }
    
    private void handleConsolePause(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getTaskManager().pauseAllTasks();
            sender.sendMessage("All tasks paused");
            return;
        }
        
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage("World not found: " + args[1]);
            return;
        }
        
        plugin.getTaskManager().pauseTask(world);
        sender.sendMessage("Task paused for world: " + world.getName());
    }
    
    private void handleConsoleContinue(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getTaskManager().continueAllTasks();
            sender.sendMessage("All tasks resumed");
            return;
        }
        
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage("World not found: " + args[1]);
            return;
        }
        
        plugin.getTaskManager().continueTask(world);
        sender.sendMessage("Task resumed for world: " + world.getName());
    }
    
    private void handleConsoleCancel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getTaskManager().cancelAllTasks();
            sender.sendMessage("All tasks cancelled");
            return;
        }
        
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage("World not found: " + args[1]);
            return;
        }
        
        plugin.getTaskManager().cancelTask(world);
        sender.sendMessage("Task cancelled for world: " + world.getName());
    }
    
    private void handleConsoleProgress(CommandSender sender) {
        Map<World, GenerationTask> tasks = plugin.getTaskManager().getAllTasks();
        
        if (tasks.isEmpty()) {
            sender.sendMessage("No active generation tasks");
            return;
        }
        
        sender.sendMessage("=== Active Generation Tasks ===");
        for (Map.Entry<World, GenerationTask> entry : tasks.entrySet()) {
            GenerationTask task = entry.getValue();
            sender.sendMessage(String.format("World: %s | Mode: %s | Progress: %.2f%% | Chunks: %d/%d | Status: %s",
                task.getWorld().getName(),
                task.getMode(),
                task.getProgress(),
                task.getCurrentChunk(),
                task.getTotalChunks(),
                task.isPaused() ? "PAUSED" : "RUNNING"
            ));
        }
        sender.sendMessage("==============================");
    }
    
    private void handleConsoleSilent(CommandSender sender) {
        plugin.getTaskManager().toggleSilent();
        sender.sendMessage("Silent mode: " + (plugin.getTaskManager().isSilent() ? "ENABLED" : "DISABLED"));
    }
    
    private void handleConsoleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        sender.sendMessage("Configuration reloaded successfully");
    }
    
    private void sendConsoleHelp(CommandSender sender) {
        sender.sendMessage("=== HyperGen Console Commands ===");
        sender.sendMessage("hypergen start <world> [mode] [radius] - Start generation");
        sender.sendMessage("hypergen pause [world] - Pause generation");
        sender.sendMessage("hypergen continue [world] - Resume generation");
        sender.sendMessage("hypergen cancel [world] - Cancel generation");
        sender.sendMessage("hypergen progress - View all tasks progress");
        sender.sendMessage("hypergen silent - Toggle silent mode");
        sender.sendMessage("hypergen reload - Reload configuration");
        sender.sendMessage("================================");
    }
}