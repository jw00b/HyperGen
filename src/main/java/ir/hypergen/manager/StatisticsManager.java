package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticsManager {
    private final HyperGen plugin;
    private final Map<String, WorldStatistics> worldStats;
    private final File statsFile;
    
    public StatisticsManager(HyperGen plugin) {
        this.plugin = plugin;
        this.worldStats = new ConcurrentHashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "statistics.yml");
        loadStatistics();
        startAutoSave();
    }
    
    public void recordChunkGeneration(World world, int chunks, long timeMs) {
        WorldStatistics stats = worldStats.computeIfAbsent(world.getName(), k -> new WorldStatistics());
        stats.totalChunksGenerated += chunks;
        stats.totalTimeSpent += timeMs;
        stats.lastGenerationTime = System.currentTimeMillis();
        stats.generationSessions++;
        
        double speed = (double) chunks / (timeMs / 1000.0);
        if (speed > stats.peakSpeed) {
            stats.peakSpeed = speed;
        }
        
        stats.averageSpeed = (double) stats.totalChunksGenerated / (stats.totalTimeSpent / 1000.0);
    }
    
    public void recordTaskStart(GenerationTask task) {
        WorldStatistics stats = worldStats.computeIfAbsent(task.getWorld().getName(), k -> new WorldStatistics());
        stats.currentTask = task;
        stats.taskStartTime = System.currentTimeMillis();
    }
    
    public void recordTaskComplete(World world) {
        WorldStatistics stats = worldStats.get(world.getName());
        if (stats != null) {
            stats.completedTasks++;
            stats.currentTask = null;
        }
    }
    
    public void recordTaskCancel(World world) {
        WorldStatistics stats = worldStats.get(world.getName());
        if (stats != null) {
            stats.cancelledTasks++;
            stats.currentTask = null;
        }
    }
    
    public WorldStatistics getStatistics(String worldName) {
        return worldStats.getOrDefault(worldName, new WorldStatistics());
    }
    
    public Map<String, WorldStatistics> getAllStatistics() {
        return new HashMap<>(worldStats);
    }
    
    public String generateReport(String worldName) {
        WorldStatistics stats = getStatistics(worldName);
        StringBuilder report = new StringBuilder();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", worldName);
        
        report.append(plugin.getConfigManager().getMessage("stats-header", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("chunks", String.valueOf(stats.totalChunksGenerated));
        report.append(plugin.getConfigManager().getMessage("stats-total-chunks", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("time", formatTime(stats.totalTimeSpent));
        report.append(plugin.getConfigManager().getMessage("stats-total-time", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("sessions", String.valueOf(stats.generationSessions));
        report.append(plugin.getConfigManager().getMessage("stats-sessions", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("completed", String.valueOf(stats.completedTasks));
        report.append(plugin.getConfigManager().getMessage("stats-completed", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("cancelled", String.valueOf(stats.cancelledTasks));
        report.append(plugin.getConfigManager().getMessage("stats-cancelled", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("speed", String.format("%.2f", stats.averageSpeed));
        report.append(plugin.getConfigManager().getMessage("stats-avg-speed", placeholders)).append("\n");
        
        placeholders.clear();
        placeholders.put("speed", String.format("%.2f", stats.peakSpeed));
        report.append(plugin.getConfigManager().getMessage("stats-peak-speed", placeholders)).append("\n");
        
        if (stats.lastGenerationTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            placeholders.clear();
            placeholders.put("date", sdf.format(new Date(stats.lastGenerationTime)));
            report.append(plugin.getConfigManager().getMessage("stats-last-generation", placeholders)).append("\n");
        }
        
        report.append(plugin.getConfigManager().getMessage("stats-footer"));
        
        return report.toString();
    }
    
    private void loadStatistics() {
        if (!statsFile.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(statsFile))) {
            Map<String, WorldStatistics> loaded = (Map<String, WorldStatistics>) ois.readObject();
            worldStats.putAll(loaded);
            plugin.getLogger().info("Loaded statistics for " + worldStats.size() + " worlds");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load statistics: " + e.getMessage());
        }
    }
    
    public void saveStatistics() {
        try {
            if (!statsFile.exists()) {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            }
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(statsFile))) {
                Map<String, WorldStatistics> toSave = new HashMap<>();
                worldStats.forEach((k, v) -> {
                    WorldStatistics copy = new WorldStatistics();
                    copy.totalChunksGenerated = v.totalChunksGenerated;
                    copy.totalTimeSpent = v.totalTimeSpent;
                    copy.generationSessions = v.generationSessions;
                    copy.completedTasks = v.completedTasks;
                    copy.cancelledTasks = v.cancelledTasks;
                    copy.averageSpeed = v.averageSpeed;
                    copy.peakSpeed = v.peakSpeed;
                    copy.lastGenerationTime = v.lastGenerationTime;
                    toSave.put(k, copy);
                });
                oos.writeObject(toSave);
                plugin.getLogger().info("Saved statistics for " + toSave.size() + " worlds");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startAutoSave() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveStatistics, 6000L, 6000L);
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    @Data
    public static class WorldStatistics implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private long totalChunksGenerated;
        private long totalTimeSpent;
        private int generationSessions;
        private int completedTasks;
        private int cancelledTasks;
        private double averageSpeed;
        private double peakSpeed;
        private long lastGenerationTime;
        private long taskStartTime;
        
        private transient GenerationTask currentTask;
    }
}