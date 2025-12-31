package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final HyperGen plugin;
    private FileConfiguration messages;
    
    public ConfigManager(HyperGen plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        String language = plugin.getConfig().getString("language", "en");
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages_" + language + ".yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String key) {
        return colorize(messages.getString(key, "&cMessage not found: " + key));
    }
    
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
    
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    public void reload() {
        plugin.reloadConfig();
        loadMessages();
    }
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public int getUpdateInterval() {
        return plugin.getConfig().getInt("update-interval", 5);
    }
    
    public int getAutoSaveInterval() {
        return plugin.getConfig().getInt("auto-save-interval", 300);
    }
    
    public int getMaxChunksPerTick() {
        return plugin.getConfig().getInt("max-chunks-per-tick", 4);
    }
    
    public int getNormalModeChunksPerSecond() {
        return plugin.getConfig().getInt("normal-mode.chunks-per-second", 20);
    }
    
    public double getProModeTargetTPS() {
        return plugin.getConfig().getDouble("pro-mode.target-tps", 19.0);
    }
    
    public double getProModeMinTPS() {
        return plugin.getConfig().getDouble("pro-mode.min-tps", 18.0);
    }
    
    public int getProModeMaxChunksPerTick() {
        return plugin.getConfig().getInt("pro-mode.max-chunks-per-tick", 8);
    }
    
    public int getProModeMinChunksPerTick() {
        return plugin.getConfig().getInt("pro-mode.min-chunks-per-tick", 1);
    }
    
    public int getFastModeChunksPerTick() {
        return plugin.getConfig().getInt("fast-mode.chunks-per-tick", 16);
    }
    
    public boolean isFastModeDisableSpawning() {
        return plugin.getConfig().getBoolean("fast-mode.disable-spawning", true);
    }
    
    public boolean isFastModeDisableEvents() {
        return plugin.getConfig().getBoolean("fast-mode.disable-events", true);
    }
    
    public boolean isFastModeKickPlayers() {
        return plugin.getConfig().getBoolean("fast-mode.kick-players", true);
    }
    
    public String getFastModeKickMessage() {
        return colorize(plugin.getConfig().getString("fast-mode.kick-message", "Server is processing chunks"));
    }
    
    public String getDefaultMode() {
        return plugin.getConfig().getString("default-mode", "normal");
    }
    
    public String getDefaultShape() {
        return plugin.getConfig().getString("default-shape", "square");
    }
    
    public String getDefaultPattern() {
        return plugin.getConfig().getString("default-pattern", "spiral");
    }
}