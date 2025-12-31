package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MapManager {
    private final HyperGen plugin;
    private final Map<UUID, MapView> playerMaps;
    private final Map<UUID, Inventory> playerInventories;
    private final Map<UUID, BukkitTask> refreshTasks;
    
    public MapManager(HyperGen plugin) {
        this.plugin = plugin;
        this.playerMaps = new HashMap<>();
        this.playerInventories = new HashMap<>();
        this.refreshTasks = new HashMap<>();
    }
    
    public void openProgressMap(Player player, GenerationTask task) {
        Inventory inv = Bukkit.createInventory(null, 54, colorize("&6&lHyperGen - Progress Map"));
        
        ItemStack mapItem = createProgressMap(player, task);
        inv.setItem(13, mapItem);
        
        Material paperMat = XMaterial.PAPER.parseMaterial();
        Material clockMat = XMaterial.CLOCK.parseMaterial();
        Material redstoneMat = XMaterial.REDSTONE.parseMaterial();
        Material compassMat = XMaterial.COMPASS.parseMaterial();
        Material limeDyeMat = XMaterial.LIME_DYE.parseMaterial();
        Material orangeDyeMat = XMaterial.ORANGE_DYE.parseMaterial();
        Material redDyeMat = XMaterial.RED_DYE.parseMaterial();
        Material barrierMat = XMaterial.BARRIER.parseMaterial();
        
        inv.setItem(19, createInfoItem(paperMat, "&e&lGeneral Info", Arrays.asList(
            "&7World: &f" + task.getWorld().getName(),
            "&7Mode: &f" + task.getMode().name(),
            "&7Shape: &f" + task.getSelection().getShape().name(),
            "&7Pattern: &f" + task.getSelection().getPattern().name()
        )));
        
        inv.setItem(21, createInfoItem(clockMat, "&e&lProgress", Arrays.asList(
            "&7Percent: &a" + String.format("%.2f", task.getProgress()) + "%",
            "&7Chunks: &f" + task.getCurrentChunk() + "/" + task.getTotalChunks(),
            "&7Elapsed: &f" + formatTime(task.getElapsedTime()),
            "&7Remaining: &f" + estimateRemainingTime(task)
        )));
        
        inv.setItem(23, createInfoItem(redstoneMat, "&e&lPerformance", Arrays.asList(
            "&7TPS: &f" + getCurrentTPS(),
            "&7Memory: &f" + getMemoryUsage() + "%",
            "&7Speed: &f" + calculateSpeed(task) + " chunks/s",
            "&7Status: " + (task.isPaused() ? "&cPaused" : "&aRunning")
        )));
        
        inv.setItem(25, createInfoItem(compassMat, "&e&lLocation", Arrays.asList(
            "&7Center: &f" + task.getSelection().getCenterX() + ", " + task.getSelection().getCenterZ(),
            "&7Radius: &f" + task.getSelection().getRadius() + " chunks",
            "&7Area: &f" + task.getTotalChunks() + " chunks"
        )));
        
        if (task.isPaused()) {
            inv.setItem(45, createActionItem(limeDyeMat, "&a&lContinue", "hypergen-continue"));
        } else {
            inv.setItem(45, createActionItem(orangeDyeMat, "&e&lPause", "hypergen-pause"));
        }
        
        inv.setItem(49, createActionItem(redDyeMat, "&c&lCancel", "hypergen-cancel"));
        inv.setItem(53, createActionItem(barrierMat, "&c&lClose", "hypergen-close"));
        
        playerInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        
        startAutoRefresh(player, task);
    }
    
    private ItemStack createProgressMap(Player player, GenerationTask task) {
        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new ProgressMapRenderer(task));
        
        MapView oldMap = playerMaps.put(player.getUniqueId(), mapView);
        if (oldMap != null) {
            oldMap.getRenderers().clear();
        }
        
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(mapView);
        meta.setDisplayName(colorize("&6&lProgress Map"));
        mapItem.setItemMeta(meta);
        
        return mapItem;
    }
    
    private ItemStack createInfoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colorize(line));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createActionItem(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Click to execute"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void startAutoRefresh(Player player, GenerationTask task) {
        BukkitTask oldTask = refreshTasks.remove(player.getUniqueId());
        if (oldTask != null) {
            oldTask.cancel();
        }
        
        BukkitTask newTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !playerInventories.containsKey(player.getUniqueId())) {
                BukkitTask t = refreshTasks.remove(player.getUniqueId());
                if (t != null) t.cancel();
                return;
            }
            
            Inventory inv = playerInventories.get(player.getUniqueId());
            if (player.getOpenInventory().getTopInventory().equals(inv)) {
                openProgressMap(player, task);
            } else {
                BukkitTask t = refreshTasks.remove(player.getUniqueId());
                if (t != null) t.cancel();
            }
        }, 20L, 20L);
        
        refreshTasks.put(player.getUniqueId(), newTask);
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
    
    private String estimateRemainingTime(GenerationTask task) {
        if (task.getCurrentChunk() == 0) return "Calculating...";
        
        long elapsed = task.getElapsedTime();
        double progress = task.getProgress() / 100.0;
        
        if (progress == 0) return "Calculating...";
        
        long totalEstimated = (long) (elapsed / progress);
        long remaining = totalEstimated - elapsed;
        
        return formatTime(remaining);
    }
    
    private double getCurrentTPS() {
        try {
            Object server = plugin.getServer().getClass().getMethod("getServer").invoke(plugin.getServer());
            double[] recentTps = (double[]) server.getClass().getField("recentTps").get(server);
            return Math.min(recentTps[0], 20.0);
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    private int getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (int) ((used * 100) / max);
    }
    
    private double calculateSpeed(GenerationTask task) {
        if (task.getElapsedTime() == 0) return 0;
        return (double) task.getCurrentChunk() / (task.getElapsedTime() / 1000.0);
    }
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public void closeMap(Player player) {
        playerMaps.remove(player.getUniqueId());
        playerInventories.remove(player.getUniqueId());
        BukkitTask task = refreshTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public void shutdown() {
        refreshTasks.values().forEach(BukkitTask::cancel);
        refreshTasks.clear();
        playerMaps.clear();
        playerInventories.clear();
    }
    
    private static class ProgressMapRenderer extends MapRenderer {
        private final GenerationTask task;
        private boolean rendered = false;
        
        public ProgressMapRenderer(GenerationTask task) {
            this.task = task;
        }
        
        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (rendered) return;
            
            int centerX = 64;
            int centerZ = 64;
            int radius = task.getSelection().getRadius();
            int scale = Math.max(1, radius / 50);
            
            for (int x = 0; x < 128; x++) {
                for (int z = 0; z < 128; z++) {
                    canvas.setPixel(x, z, (byte) 0);
                }
            }
            
            Set<GenerationTask.ChunkCoord> processed = task.getProcessedChunks();
            
            for (GenerationTask.ChunkCoord coord : processed) {
                int relX = coord.getX() - (task.getSelection().getCenterX() >> 4);
                int relZ = coord.getZ() - (task.getSelection().getCenterZ() >> 4);
                
                int mapX = centerX + (relX / scale);
                int mapZ = centerZ + (relZ / scale);
                
                if (mapX >= 0 && mapX < 128 && mapZ >= 0 && mapZ < 128) {
                    canvas.setPixel(mapX, mapZ, (byte) 30);
                }
            }
            
            canvas.setPixel(centerX, centerZ, (byte) 14);
            
            rendered = true;
        }
    }
}