package ir.hypergen.api;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import ir.hypergen.model.Selection;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class HyperGenAPI {
    private static HyperGen plugin;
    
    public static void initialize(HyperGen instance) {
        plugin = instance;
    }
    
    public static HyperGen getPlugin() {
        if (plugin == null) {
            plugin = JavaPlugin.getPlugin(HyperGen.class);
        }
        return plugin;
    }
    
    public static void startGeneration(World world, Selection selection, GenerationTask.GenerationMode mode) {
        plugin.getTaskManager().startTask(world, selection, mode);
    }
    
    public static void pauseGeneration(World world) {
        plugin.getTaskManager().pauseTask(world);
    }
    
    public static void continueGeneration(World world) {
        plugin.getTaskManager().continueTask(world);
    }
    
    public static void cancelGeneration(World world) {
        plugin.getTaskManager().cancelTask(world);
    }
    
    public static GenerationTask getTask(World world) {
        return plugin.getTaskManager().getTask(world);
    }
    
    public static Map<World, GenerationTask> getAllTasks() {
        return plugin.getTaskManager().getAllTasks();
    }
    
    public static boolean hasActiveTask(World world) {
        return plugin.getTaskManager().hasActiveTask(world);
    }
    
    public static double getProgress(World world) {
        GenerationTask task = getTask(world);
        return task != null ? task.getProgress() : 0;
    }
}