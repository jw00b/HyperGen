package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {
    private final HyperGen plugin;
    
    public PlaceholderManager(HyperGen plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "hypergen";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("active_tasks")) {
            return String.valueOf(plugin.getTaskManager().getAllTasks().size());
        }
        
        if (params.equalsIgnoreCase("queue_size")) {
            return String.valueOf(plugin.getQueueManager().getQueueSize());
        }
        
        if (params.startsWith("world_")) {
            String worldName = params.substring(6);
            World world = plugin.getServer().getWorld(worldName);
            
            if (world == null) {
                return "N/A";
            }
            
            GenerationTask task = plugin.getTaskManager().getTask(world);
            if (task == null) {
                return "0";
            }
            
            return String.format("%.2f", task.getProgress());
        }
        
        if (player != null) {
            World world = player.getWorld();
            GenerationTask task = plugin.getTaskManager().getTask(world);
            
            if (task == null) {
                return switch (params.toLowerCase()) {
                    case "progress" -> "0";
                    case "status" -> "Inactive";
                    case "chunks" -> "0/0";
                    case "speed" -> "0";
                    case "eta" -> "N/A";
                    default -> null;
                };
            }
            
            return switch (params.toLowerCase()) {
                case "progress" -> String.format("%.2f", task.getProgress());
                case "status" -> task.isPaused() ? "Paused" : "Running";
                case "chunks" -> task.getCurrentChunk() + "/" + task.getTotalChunks();
                case "speed" -> {
                    long elapsed = task.getElapsedTime();
                    if (elapsed == 0) yield "0";
                    double speed = task.getCurrentChunk() / (elapsed / 1000.0);
                    yield String.format("%.2f", speed);
                }
                case "eta" -> estimateRemainingTime(task);
                case "mode" -> task.getMode().name();
                default -> null;
            };
        }
        
        return null;
    }
    
    private String estimateRemainingTime(GenerationTask task) {
        if (task.getCurrentChunk() == 0) return "Calculating...";
        
        long elapsed = task.getElapsedTime();
        double progress = task.getProgress() / 100.0;
        
        if (progress == 0) return "Calculating...";
        
        long totalEstimated = (long) (elapsed / progress);
        long remaining = totalEstimated - elapsed;
        
        return formatTime(remaining);
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
}