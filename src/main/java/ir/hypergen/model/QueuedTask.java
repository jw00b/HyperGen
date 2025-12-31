package ir.hypergen.model;

import lombok.Data;
import org.bukkit.World;

@Data
public class QueuedTask {
    private final World world;
    private final Selection selection;
    private final GenerationTask.GenerationMode mode;
    private final int priority;
    private final long queueTime;
    
    public QueuedTask(World world, Selection selection, GenerationTask.GenerationMode mode, int priority) {
        this.world = world;
        this.selection = selection;
        this.mode = mode;
        this.priority = priority;
        this.queueTime = System.currentTimeMillis();
    }
}