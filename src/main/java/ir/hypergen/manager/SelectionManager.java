package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.Selection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final HyperGen plugin;
    private final Map<UUID, Selection> selections;
    
    public SelectionManager(HyperGen plugin) {
        this.plugin = plugin;
        this.selections = new HashMap<>();
    }
    
    public Selection getSelection(Player player) {
        return selections.computeIfAbsent(player.getUniqueId(), k -> {
            Selection selection = new Selection();
            selection.setWorld(player.getWorld());
            Location loc = player.getLocation();
            selection.setCenterX(loc.getBlockX());
            selection.setCenterZ(loc.getBlockZ());
            selection.setRadius(100);
            
            String defaultShape = plugin.getConfigManager().getDefaultShape();
            selection.setShape(Selection.Shape.valueOf(defaultShape.toUpperCase()));
            
            String defaultPattern = plugin.getConfigManager().getDefaultPattern();
            selection.setPattern(Selection.Pattern.valueOf(defaultPattern.toUpperCase()));
            
            return selection;
        });
    }
    
    public Selection createConsoleSelection(World world, int radius) {
        Selection selection = new Selection();
        selection.setWorld(world);
        Location spawn = world.getSpawnLocation();
        selection.setCenterX(spawn.getBlockX());
        selection.setCenterZ(spawn.getBlockZ());
        selection.setRadius(radius);
        
        String defaultShape = plugin.getConfigManager().getDefaultShape();
        selection.setShape(Selection.Shape.valueOf(defaultShape.toUpperCase()));
        
        String defaultPattern = plugin.getConfigManager().getDefaultPattern();
        selection.setPattern(Selection.Pattern.valueOf(defaultPattern.toUpperCase()));
        
        return selection;
    }
    
    public void setWorld(Player player, World world) {
        Selection selection = getSelection(player);
        selection.setWorld(world);
    }
    
    public void setShape(Player player, Selection.Shape shape) {
        Selection selection = getSelection(player);
        selection.setShape(shape);
    }
    
    public void setCenter(Player player, int x, int z) {
        Selection selection = getSelection(player);
        selection.setCenterX(x);
        selection.setCenterZ(z);
    }
    
    public void setRadius(Player player, int radius) {
        Selection selection = getSelection(player);
        selection.setRadius(radius);
    }
    
    public void setPattern(Player player, Selection.Pattern pattern) {
        Selection selection = getSelection(player);
        selection.setPattern(pattern);
    }
    
    public void setToWorldBorder(Player player) {
        Selection selection = getSelection(player);
        World world = selection.getWorld();
        if (world == null) return;
        
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        selection.setCenterX(center.getBlockX());
        selection.setCenterZ(center.getBlockZ());
        selection.setRadius((int) (border.getSize() / 2 / 16));
    }
    
    public void setToSpawn(Player player) {
        Selection selection = getSelection(player);
        World world = selection.getWorld();
        if (world == null) return;
        
        Location spawn = world.getSpawnLocation();
        selection.setCenterX(spawn.getBlockX());
        selection.setCenterZ(spawn.getBlockZ());
    }
    
    public void setCorners(Player player, int x1, int z1, int x2, int z2) {
        Selection selection = getSelection(player);
        int centerX = (x1 + x2) / 2;
        int centerZ = (z1 + z2) / 2;
        int radiusX = Math.abs(x2 - x1) / 2 / 16;
        int radiusZ = Math.abs(z2 - z1) / 2 / 16;
        int radius = Math.max(radiusX, radiusZ);
        
        selection.setCenterX(centerX);
        selection.setCenterZ(centerZ);
        selection.setRadius(radius);
    }
    
    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }
}