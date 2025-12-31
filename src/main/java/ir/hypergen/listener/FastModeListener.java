package ir.hypergen.listener;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FastModeListener implements Listener {
    private final HyperGen plugin;
    
    public FastModeListener(HyperGen plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        boolean hasFastModeTask = plugin.getTaskManager().getAllTasks().values().stream()
                .anyMatch(task -> task.getMode() == GenerationTask.GenerationMode.FAST);
        
        if (hasFastModeTask && plugin.getConfigManager().isFastModeKickPlayers()) {
            String kickMessage = plugin.getConfigManager().getFastModeKickMessage();
            player.kickPlayer(kickMessage);
        }
    }
}