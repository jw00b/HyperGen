package ir.hypergen;

import ir.hypergen.api.HyperGenAPI;
import ir.hypergen.command.HyperGenCommand;
import ir.hypergen.command.ConsoleCommandHandler;
import ir.hypergen.listener.FastModeListener;
import ir.hypergen.listener.MapInventoryListener;
import ir.hypergen.manager.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class HyperGen extends JavaPlugin {
    
    private ConfigManager configManager;
    private SelectionManager selectionManager;
    private TaskManager taskManager;
    private FastModeListener fastModeListener;
    private MapManager mapManager;
    private StatisticsManager statisticsManager;
    private NotificationManager notificationManager;
    private QueueManager queueManager;
    private ConsoleCommandHandler consoleCommandHandler;
    private PlaceholderManager placeholderManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        selectionManager = new SelectionManager(this);
        taskManager = new TaskManager(this);
        fastModeListener = new FastModeListener(this);
        mapManager = new MapManager(this);
        statisticsManager = new StatisticsManager(this);
        notificationManager = new NotificationManager(this);
        queueManager = new QueueManager(this);
        consoleCommandHandler = new ConsoleCommandHandler(this);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderManager = new PlaceholderManager(this);
            placeholderManager.register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        }
        
        HyperGenCommand hypergenCommand = new HyperGenCommand(this);
        getCommand("hypergen").setExecutor(hypergenCommand);
        getCommand("hypergen").setTabCompleter(hypergenCommand);
        
        getServer().getPluginManager().registerEvents(fastModeListener, this);
        getServer().getPluginManager().registerEvents(new MapInventoryListener(this), this);
        
        HyperGenAPI.initialize(this);
        
        getLogger().info("HyperGen v2.0.0 enabled successfully!");
        getLogger().info("Running on Java " + System.getProperty("java.version"));
    }
    
    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.shutdown();
        }
        
        if (statisticsManager != null) {
            statisticsManager.saveStatistics();
        }
        
        if (queueManager != null) {
            queueManager.shutdown();
        }
        
        if (mapManager != null) {
            mapManager.shutdown();
        }
        
        if (placeholderManager != null) {
            placeholderManager.unregister();
        }
        
        getLogger().info("HyperGen disabled!");
    }
}