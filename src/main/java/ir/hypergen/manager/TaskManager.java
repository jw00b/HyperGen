package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import ir.hypergen.model.Selection;
import ir.hypergen.util.ChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private final HyperGen plugin;
    private final Map<World, GenerationTask> activeTasks;
    private final Map<World, BukkitTask> runningTasks;
    private final Map<World, ChunkGenerator> generators;
    private final Map<World, Long> lastLogTime;
    private boolean silent;
    private int quietInterval;
    private long lastMemoryCheck;
    private static final long MEMORY_CHECK_INTERVAL = 5000;
    
    public TaskManager(HyperGen plugin) {
        this.plugin = plugin;
        this.activeTasks = new ConcurrentHashMap<>();
        this.runningTasks = new ConcurrentHashMap<>();
        this.generators = new ConcurrentHashMap<>();
        this.lastLogTime = new ConcurrentHashMap<>();
        this.silent = plugin.getConfig().getBoolean("logging.silent", false);
        this.quietInterval = plugin.getConfig().getInt("logging.interval", 10);
        this.lastMemoryCheck = System.currentTimeMillis();
    }
    
    public void startTask(World world, Selection selection, GenerationTask.GenerationMode mode) {
        if (activeTasks.containsKey(world)) {
            return;
        }
        
        GenerationTask task = new GenerationTask(world, selection, mode);
        activeTasks.put(world, task);
        lastLogTime.put(world, System.currentTimeMillis());
        
        ChunkGenerator generator = new ChunkGenerator(plugin, task);
        generator.prepare();
        generators.put(world, generator);
        
        plugin.getLogger().info(String.format("Starting chunk generation for world '%s' in %s mode", 
            world.getName(), mode.name()));
        plugin.getLogger().info(String.format("Total chunks to generate: %d", task.getTotalChunks()));
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!task.isPaused()) {
                if (shouldPauseForMemory()) {
                    task.setPaused(true);
                    notifyMemoryPause(world);
                    return;
                }
                
                generator.processNextBatch();
                
                logProgress(world, task);
                
                if (generator.isComplete()) {
                    completeTask(world);
                }
            }
        }, 0L, 1L);
        
        runningTasks.put(world, bukkitTask);
    }
    
    private void logProgress(World world, GenerationTask task) {
        if (silent || !plugin.getConfig().getBoolean("logging.console-updates", true)) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastLog = lastLogTime.getOrDefault(world, 0L);
        
        if (currentTime - lastLog >= quietInterval * 1000L) {
            double progress = task.getProgress();
            int current = task.getCurrentChunk();
            int total = task.getTotalChunks();
            long elapsed = task.getElapsedTime();
            double speed = current / (elapsed / 1000.0);
            
            String logMessage = String.format(
                "[HyperGen] World: %s | Progress: %.2f%% | Chunks: %d/%d | Speed: %.2f chunks/s | Status: %s",
                world.getName(),
                progress,
                current,
                total,
                speed,
                task.isPaused() ? "PAUSED" : "RUNNING"
            );
            
            plugin.getLogger().info(logMessage);
            lastLogTime.put(world, currentTime);
        }
    }
    
    private boolean shouldPauseForMemory() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL) {
            return false;
        }
        
        lastMemoryCheck = currentTime;
        
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        
        double memoryUsage = 1.0 - ((double) freeMemory / totalMemory);
        double threshold = plugin.getConfig().getDouble("performance.memory-threshold", 0.85);
        
        if (memoryUsage > threshold) {
            System.gc();
            return true;
        }
        
        return false;
    }
    
    private void notifyMemoryPause(World world) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", world.getName());
        String message = plugin.getConfigManager().getMessage("memory-pause", placeholders);
        
        plugin.getLogger().warning(message);
        
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("hypergen.admin")) {
                p.sendMessage(message);
            }
        });
    }
    
    public void pauseTask(World world) {
        GenerationTask task = activeTasks.get(world);
        if (task != null) {
            task.setPaused(true);
            plugin.getLogger().info("Task paused for world: " + world.getName());
        }
    }
    
    public void pauseAllTasks() {
        activeTasks.values().forEach(task -> task.setPaused(true));
        plugin.getLogger().info("All tasks paused");
    }
    
    public void continueTask(World world) {
        GenerationTask task = activeTasks.get(world);
        if (task != null) {
            task.setPaused(false);
            plugin.getLogger().info("Task resumed for world: " + world.getName());
        }
    }
    
    public void continueAllTasks() {
        activeTasks.values().forEach(task -> task.setPaused(false));
        plugin.getLogger().info("All tasks resumed");
    }
    
    public void cancelTask(World world) {
        BukkitTask task = runningTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
        activeTasks.remove(world);
        generators.remove(world);
        lastLogTime.remove(world);
        plugin.getLogger().info("Task cancelled for world: " + world.getName());
    }
    
    public void cancelAllTasks() {
        runningTasks.values().forEach(BukkitTask::cancel);
        runningTasks.clear();
        activeTasks.clear();
        generators.clear();
        lastLogTime.clear();
        plugin.getLogger().info("All tasks cancelled");
    }
    
    private void completeTask(World world) {
        GenerationTask task = activeTasks.get(world);
        if (task != null) {
            long elapsed = task.getElapsedTime();
            double speed = task.getCurrentChunk() / (elapsed / 1000.0);
            
            plugin.getLogger().info(String.format(
                "Chunk generation completed for world '%s' | Total chunks: %d | Time: %s | Average speed: %.2f chunks/s",
                world.getName(),
                task.getCurrentChunk(),
                formatTime(elapsed),
                speed
            ));
            
            plugin.getStatisticsManager().recordTaskComplete(world);
        }
        
        cancelTask(world);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", world.getName());
        String message = plugin.getConfigManager().getMessage("generation-complete", placeholders);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public GenerationTask getTask(World world) {
        return activeTasks.get(world);
    }
    
    public boolean hasActiveTask(World world) {
        return activeTasks.containsKey(world);
    }
    
    public Map<World, GenerationTask> getAllTasks() {
        return new HashMap<>(activeTasks);
    }
    
    public void toggleSilent() {
        silent = !silent;
        plugin.getLogger().info("Silent mode: " + (silent ? "ENABLED" : "DISABLED"));
    }
    
    public boolean isSilent() {
        return silent;
    }
    
    public void setQuietInterval(int interval) {
        this.quietInterval = interval;
        plugin.getLogger().info("Quiet interval set to: " + interval + " seconds");
    }
    
    public int getQuietInterval() {
        return quietInterval;
    }
    
    public void shutdown() {
        plugin.getLogger().info("Shutting down TaskManager...");
        runningTasks.values().forEach(BukkitTask::cancel);
        runningTasks.clear();
        activeTasks.clear();
        generators.clear();
        lastLogTime.clear();
    }
}