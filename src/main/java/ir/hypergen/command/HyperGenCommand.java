package ir.hypergen.command;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import ir.hypergen.model.QueuedTask;
import ir.hypergen.model.Selection;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class HyperGenCommand implements CommandExecutor, TabCompleter {
    private final HyperGen plugin;
    private final Map<UUID, Long> fastModeConfirmations;
    
    public HyperGenCommand(HyperGen plugin) {
        this.plugin = plugin;
        this.fastModeConfirmations = new HashMap<>();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            plugin.getConsoleCommandHandler().handleConsoleCommand(sender, args);
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players or console!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("hypergen.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                handleStart(player, args);
                break;
            case "pause":
                handlePause(player);
                break;
            case "continue":
                handleContinue(player);
                break;
            case "cancel":
                handleCancel(player);
                break;
            case "world":
                handleWorld(player, args);
                break;
            case "shape":
                handleShape(player, args);
                break;
            case "center":
                handleCenter(player, args);
                break;
            case "radius":
                handleRadius(player, args);
                break;
            case "worldborder":
                handleWorldBorder(player);
                break;
            case "spawn":
                handleSpawn(player);
                break;
            case "corners":
                handleCorners(player, args);
                break;
            case "pattern":
                handlePattern(player, args);
                break;
            case "selection":
                handleSelection(player);
                break;
            case "silent":
                handleSilent(player);
                break;
            case "quiet":
                handleQuiet(player, args);
                break;
            case "progress":
                handleProgress(player);
                break;
            case "map":
                handleMap(player);
                break;
            case "stats":
                handleStats(player, args);
                break;
            case "queue":
                handleQueue(player, args);
                break;
            case "info":
                handleInfo(player);
                break;
            case "version":
                handleVersion(player);
                break;
            case "list":
                handleList(player);
                break;
            case "reload":
                handleReload(player);
                break;
            case "trim":
                handleTrim(player);
                break;
            case "confirm":
                handleConfirm(player);
                break;
            case "speed":
                handleSpeed(player, args);
                break;
            case "eta":
                handleEta(player);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                break;
        }
        
        return true;
    }
    
    private void handleStart(Player player, String[] args) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        
        if (!selection.isValid()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-world-selected"));
            return;
        }
        
        GenerationTask.GenerationMode mode = GenerationTask.GenerationMode.NORMAL;
        
        if (args.length > 1) {
            try {
                mode = GenerationTask.GenerationMode.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-mode"));
                return;
            }
        }
        
        if (mode == GenerationTask.GenerationMode.FAST) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            
            if (!fastModeConfirmations.containsKey(playerId) || 
                currentTime - fastModeConfirmations.get(playerId) > 10000) {
                fastModeConfirmations.put(playerId, currentTime);
                player.sendMessage(plugin.getConfigManager().getMessage("fast-mode-warning"));
                player.sendMessage(plugin.getConfigManager().getMessage("fast-mode-confirm"));
                return;
            }
            
            fastModeConfirmations.remove(playerId);
            
            if (plugin.getConfigManager().isFastModeKickPlayers()) {
                String kickMessage = plugin.getConfigManager().getFastModeKickMessage();
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.hasPermission("hypergen.bypass")) {
                        p.kickPlayer(kickMessage);
                    }
                });
            }
        }
        
        plugin.getTaskManager().startTask(selection.getWorld(), selection, mode);
        plugin.getStatisticsManager().recordTaskStart(
            new GenerationTask(selection.getWorld(), selection, mode)
        );
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", mode.name().toLowerCase());
        player.sendMessage(plugin.getConfigManager().getMessage("task-started", placeholders));
    }
    
    private void handlePause(Player player) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        World world = selection.getWorld();
        
        if (world == null || !plugin.getTaskManager().hasActiveTask(world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        plugin.getTaskManager().pauseTask(world);
        player.sendMessage(plugin.getConfigManager().getMessage("task-paused"));
    }
    
    private void handleContinue(Player player) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        World world = selection.getWorld();
        
        if (world == null || !plugin.getTaskManager().hasActiveTask(world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        plugin.getTaskManager().continueTask(world);
        player.sendMessage(plugin.getConfigManager().getMessage("task-continued"));
    }
    
    private void handleCancel(Player player) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        World world = selection.getWorld();
        
        if (world == null || !plugin.getTaskManager().hasActiveTask(world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        plugin.getTaskManager().cancelTask(world);
        plugin.getStatisticsManager().recordTaskCancel(world);
        player.sendMessage(plugin.getConfigManager().getMessage("task-cancelled"));
    }
    
    private void handleWorld(Player player, String[] args) {
        if (args.length < 2) {
            World world = player.getWorld();
            plugin.getSelectionManager().setWorld(player, world);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("world", world.getName());
            player.sendMessage(plugin.getConfigManager().getMessage("world-set", placeholders));
            return;
        }
        
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("world", args[1]);
            player.sendMessage(plugin.getConfigManager().getMessage("world-not-found", placeholders));
            return;
        }
        
        plugin.getSelectionManager().setWorld(player, world);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", world.getName());
        player.sendMessage(plugin.getConfigManager().getMessage("world-set", placeholders));
    }
    
    private void handleShape(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-shape"));
            return;
        }
        
        try {
            Selection.Shape shape = Selection.Shape.valueOf(args[1].toUpperCase());
            plugin.getSelectionManager().setShape(player, shape);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("shape", shape.name().toLowerCase());
            player.sendMessage(plugin.getConfigManager().getMessage("shape-set", placeholders));
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-shape"));
        }
    }
    
    private void handleCenter(Player player, String[] args) {
        if (args.length < 3) {
            int x = player.getLocation().getBlockX();
            int z = player.getLocation().getBlockZ();
            plugin.getSelectionManager().setCenter(player, x, z);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(x));
            placeholders.put("z", String.valueOf(z));
            player.sendMessage(plugin.getConfigManager().getMessage("center-set", placeholders));
            return;
        }
        
        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            plugin.getSelectionManager().setCenter(player, x, z);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(x));
            placeholders.put("z", String.valueOf(z));
            player.sendMessage(plugin.getConfigManager().getMessage("center-set", placeholders));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
        }
    }
    
    private void handleRadius(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            return;
        }
        
        try {
            int radius = Integer.parseInt(args[1]);
            plugin.getSelectionManager().setRadius(player, radius);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("radius", String.valueOf(radius));
            player.sendMessage(plugin.getConfigManager().getMessage("radius-set", placeholders));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
        }
    }
    
    private void handleWorldBorder(Player player) {
        plugin.getSelectionManager().setToWorldBorder(player);
        player.sendMessage(plugin.getConfigManager().getMessage("worldborder-set"));
    }
    
    private void handleSpawn(Player player) {
        plugin.getSelectionManager().setToSpawn(player);
        player.sendMessage(plugin.getConfigManager().getMessage("spawn-set"));
    }
    
    private void handleCorners(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            return;
        }
        
        try {
            int x1 = Integer.parseInt(args[1]);
            int z1 = Integer.parseInt(args[2]);
            int x2 = Integer.parseInt(args[3]);
            int z2 = Integer.parseInt(args[4]);
            
            plugin.getSelectionManager().setCorners(player, x1, z1, x2, z2);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x1", String.valueOf(x1));
            placeholders.put("z1", String.valueOf(z1));
            placeholders.put("x2", String.valueOf(x2));
            placeholders.put("z2", String.valueOf(z2));
            player.sendMessage(plugin.getConfigManager().getMessage("corners-set", placeholders));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
        }
    }
    
    private void handlePattern(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-pattern"));
            return;
        }
        
        try {
            Selection.Pattern pattern = Selection.Pattern.valueOf(args[1].toUpperCase());
            plugin.getSelectionManager().setPattern(player, pattern);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("pattern", pattern.name().toLowerCase());
            player.sendMessage(plugin.getConfigManager().getMessage("pattern-set", placeholders));
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-pattern"));
        }
    }
    
    private void handleSelection(Player player) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        
        player.sendMessage(plugin.getConfigManager().getMessage("selection-info"));
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", selection.getWorld() != null ? selection.getWorld().getName() : "None");
        player.sendMessage(plugin.getConfigManager().getMessage("selection-world", placeholders));
        
        placeholders.clear();
        placeholders.put("shape", selection.getShape().name().toLowerCase());
        player.sendMessage(plugin.getConfigManager().getMessage("selection-shape", placeholders));
        
        placeholders.clear();
        placeholders.put("x", String.valueOf(selection.getCenterX()));
        placeholders.put("z", String.valueOf(selection.getCenterZ()));
        player.sendMessage(plugin.getConfigManager().getMessage("selection-center", placeholders));
        
        placeholders.clear();
        placeholders.put("radius", String.valueOf(selection.getRadius()));
        player.sendMessage(plugin.getConfigManager().getMessage("selection-radius", placeholders));
        
        placeholders.clear();
        placeholders.put("pattern", selection.getPattern().name().toLowerCase());
        player.sendMessage(plugin.getConfigManager().getMessage("selection-pattern", placeholders));
        
        placeholders.clear();
        placeholders.put("chunks", String.valueOf(selection.getTotalChunks()));
        player.sendMessage(plugin.getConfigManager().getMessage("selection-chunks", placeholders));
    }
    
    private void handleSilent(Player player) {
        plugin.getTaskManager().toggleSilent();
        
        if (plugin.getTaskManager().isSilent()) {
            player.sendMessage(plugin.getConfigManager().getMessage("silent-enabled"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("silent-disabled"));
        }
    }
    
    private void handleQuiet(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            return;
        }
        
        try {
            int interval = Integer.parseInt(args[1]);
            plugin.getTaskManager().setQuietInterval(interval);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("interval", String.valueOf(interval));
            player.sendMessage(plugin.getConfigManager().getMessage("quiet-set", placeholders));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
        }
    }
    
    private void handleProgress(Player player) {
        Map<World, GenerationTask> tasks = plugin.getTaskManager().getAllTasks();
        
        if (tasks.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("progress-header"));
        
        for (Map.Entry<World, GenerationTask> entry : tasks.entrySet()) {
            GenerationTask task = entry.getValue();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("world", task.getWorld().getName());
            player.sendMessage(plugin.getConfigManager().getMessage("progress-world", placeholders));
            
            placeholders.clear();
            placeholders.put("mode", task.getMode().name().toLowerCase());
            player.sendMessage(plugin.getConfigManager().getMessage("progress-mode", placeholders));
            
            placeholders.clear();
            placeholders.put("percent", String.format("%.2f", task.getProgress()));
            player.sendMessage(plugin.getConfigManager().getMessage("progress-percent", placeholders));
            
            placeholders.clear();
            placeholders.put("current", String.valueOf(task.getCurrentChunk()));
            placeholders.put("total", String.valueOf(task.getTotalChunks()));
            player.sendMessage(plugin.getConfigManager().getMessage("progress-chunks", placeholders));
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("progress-footer"));
    }
    
    private void handleMap(Player player) {
        if (!player.hasPermission("hypergen.map")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        World world = selection.getWorld();
        
        if (world == null || !plugin.getTaskManager().hasActiveTask(world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        GenerationTask task = plugin.getTaskManager().getTask(world);
        plugin.getMapManager().openProgressMap(player, task);
    }
    
    private void handleStats(Player player, String[] args) {
        if (!player.hasPermission("hypergen.stats")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        String worldName = args.length > 1 ? args[1] : player.getWorld().getName();
        String report = plugin.getStatisticsManager().generateReport(worldName);
        player.sendMessage(report);
    }
    
    private void handleQueue(Player player, String[] args) {
        if (!player.hasPermission("hypergen.queue")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("queue-usage"));
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "add":
                Selection selection = plugin.getSelectionManager().getSelection(player);
                if (!selection.isValid()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-world-selected"));
                    return;
                }
                int priority = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                plugin.getQueueManager().addToQueue(selection.getWorld(), selection, GenerationTask.GenerationMode.NORMAL, priority);
                player.sendMessage(plugin.getConfigManager().getMessage("queue-added"));
                break;
                
            case "remove":
                if (args.length < 3) {
                    player.sendMessage(plugin.getConfigManager().getMessage("queue-usage"));
                    return;
                }
                try {
                    int index = Integer.parseInt(args[2]);
                    plugin.getQueueManager().removeFromQueue(index);
                    player.sendMessage(plugin.getConfigManager().getMessage("queue-removed"));
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                }
                break;
                
            case "list":
                player.sendMessage(plugin.getConfigManager().getMessage("queue-header"));
                
                List<QueuedTask> queue = plugin.getQueueManager().getQueue();
                if (queue.isEmpty()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("queue-empty"));
                } else {
                    for (int i = 0; i < queue.size(); i++) {
                        QueuedTask task = queue.get(i);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("index", String.valueOf(i));
                        placeholders.put("world", task.getWorld().getName());
                        placeholders.put("mode", task.getMode().name());
                        placeholders.put("priority", String.valueOf(task.getPriority()));
                        player.sendMessage(plugin.getConfigManager().getMessage("queue-item", placeholders));
                    }
                }
                break;
                
            case "clear":
                plugin.getQueueManager().cancelQueue();
                player.sendMessage(plugin.getConfigManager().getMessage("queue-cleared"));
                break;
        }
    }
    
    private void handleInfo(Player player) {
        if (!player.hasPermission("hypergen.info")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("info-header"));
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        player.sendMessage(plugin.getConfigManager().getMessage("info-version", placeholders));
        
        placeholders.clear();
        placeholders.put("author", plugin.getDescription().getAuthors().toString());
        player.sendMessage(plugin.getConfigManager().getMessage("info-author", placeholders));
        
        placeholders.clear();
        placeholders.put("website", plugin.getDescription().getWebsite());
        player.sendMessage(plugin.getConfigManager().getMessage("info-website", placeholders));
        
        placeholders.clear();
        placeholders.put("tasks", String.valueOf(plugin.getTaskManager().getAllTasks().size()));
        player.sendMessage(plugin.getConfigManager().getMessage("info-active-tasks", placeholders));
        
        player.sendMessage(plugin.getConfigManager().getMessage("info-footer"));
    }
    
    private void handleVersion(Player player) {
        if (!player.hasPermission("hypergen.version")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        player.sendMessage(plugin.getConfigManager().getMessage("info-version", placeholders));
    }
    
    private void handleList(Player player) {
        if (!player.hasPermission("hypergen.list")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        Map<World, GenerationTask> tasks = plugin.getTaskManager().getAllTasks();
        
        player.sendMessage(plugin.getConfigManager().getMessage("list-header"));
        
        if (tasks.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("list-empty"));
        } else {
            for (GenerationTask task : tasks.values()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("world", task.getWorld().getName());
                placeholders.put("mode", task.getMode().name());
                placeholders.put("percent", String.format("%.2f", task.getProgress()));
                placeholders.put("current", String.valueOf(task.getCurrentChunk()));
                placeholders.put("total", String.valueOf(task.getTotalChunks()));
                player.sendMessage(plugin.getConfigManager().getMessage("list-item", placeholders));
            }
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("list-footer"));
    }
    
    private void handleReload(Player player) {
        plugin.getConfigManager().reload();
        plugin.getNotificationManager().reload();
        player.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
    }
    
    private void handleTrim(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("trim-started"));
    }
    
    private void handleConfirm(Player player) {
        handleStart(player, new String[]{"start", "fast"});
    }
    
    private void handleSpeed(Player player, String[] args) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        World world = selection.getWorld();
        
        if (world == null || !plugin.getTaskManager().hasActiveTask(world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        GenerationTask task = plugin.getTaskManager().getTask(world);
        long elapsed = task.getElapsedTime();
        
        if (elapsed == 0) {
            player.sendMessage("&eSpeed: &f0 chunks/s");
            return;
        }
        
        double speed = task.getCurrentChunk() / (elapsed / 1000.0);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("speed", String.format("%.2f", speed));
        player.sendMessage(plugin.getConfigManager().getMessage("progress-speed", placeholders));
    }
    
    private void handleEta(Player player) {
        Selection selection = plugin.getSelectionManager().getSelection(player);
        World world = selection.getWorld();
        
        if (world == null || !plugin.getTaskManager().hasActiveTask(world)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-active-task"));
            return;
        }
        
        GenerationTask task = plugin.getTaskManager().getTask(world);
        
        if (task.getCurrentChunk() == 0) {
            player.sendMessage("&eETA: &fCalculating...");
            return;
        }
        
        long elapsed = task.getElapsedTime();
        double progress = task.getProgress() / 100.0;
        
        if (progress == 0) {
            player.sendMessage("&eETA: &fCalculating...");
            return;
        }
        
        long totalEstimated = (long) (elapsed / progress);
        long remaining = totalEstimated - elapsed;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("eta", formatTime(remaining));
        player.sendMessage(plugin.getConfigManager().getMessage("progress-eta", placeholders));
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("help-header"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-start"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-pause"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-continue"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-cancel"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-world"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-shape"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-center"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-radius"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-worldborder"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-spawn"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-corners"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-pattern"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-selection"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-progress"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-map"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-stats"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-queue"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-silent"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-quiet"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-speed"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-eta"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-info"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-version"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-list"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-reload"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-trim"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-footer"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "pause", "continue", "cancel", "world", 
                    "shape", "center", "radius", "worldborder", "spawn", "corners", "pattern", 
                    "selection", "silent", "quiet", "progress", "map", "stats", "queue", 
                    "info", "version", "list", "reload", "trim", "speed", "eta", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "world":
                    Bukkit.getWorlds().forEach(w -> completions.add(w.getName()));
                    break;
                case "shape":
                    completions.addAll(Arrays.asList("square", "circle"));
                    break;
                case "pattern":
                    completions.addAll(Arrays.asList("spiral", "concentric"));
                    break;
                case "start":
                    completions.addAll(Arrays.asList("normal", "pro", "fast"));
                    break;
                case "queue":
                    completions.addAll(Arrays.asList("add", "remove", "list", "clear"));
                    break;
            }
        }
        
        return completions;
    }
}