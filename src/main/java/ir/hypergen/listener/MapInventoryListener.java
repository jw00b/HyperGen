package ir.hypergen.listener;

import ir.hypergen.HyperGen;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class MapInventoryListener implements Listener {
    private final HyperGen plugin;
    
    public MapInventoryListener(HyperGen plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.contains("HyperGen - Progress Map")) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        if (displayName.contains("Continue")) {
            player.performCommand("hypergen continue");
            player.closeInventory();
        } else if (displayName.contains("Pause")) {
            player.performCommand("hypergen pause");
            player.closeInventory();
        } else if (displayName.contains("Cancel")) {
            player.performCommand("hypergen cancel");
            player.closeInventory();
        } else if (displayName.contains("Close")) {
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.contains("HyperGen - Progress Map")) return;
        
        Player player = (Player) event.getPlayer();
        plugin.getMapManager().closeMap(player);
    }
}