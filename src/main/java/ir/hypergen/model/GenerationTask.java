package ir.hypergen.model;

import lombok.Data;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

@Data
public class GenerationTask {
    private final World world;
    private final Selection selection;
    private final GenerationMode mode;
    private final Set<ChunkCoord> processedChunks;
    private final Set<ChunkCoord> remainingChunks;
    private long startTime;
    private boolean paused;
    private int totalChunks;
    private int currentChunk;
    
    public GenerationTask(World world, Selection selection, GenerationMode mode) {
        this.world = world;
        this.selection = selection;
        this.mode = mode;
        this.processedChunks = new HashSet<>();
        this.remainingChunks = new HashSet<>();
        this.startTime = System.currentTimeMillis();
        this.paused = false;
        this.totalChunks = selection.getTotalChunks();
        this.currentChunk = 0;
    }
    
    public double getProgress() {
        if (totalChunks == 0) return 0;
        return (double) currentChunk / totalChunks * 100;
    }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    public enum GenerationMode {
        NORMAL, PRO, FAST
    }
    
    @Data
    public static class ChunkCoord {
        private final int x;
        private final int z;
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkCoord)) return false;
            ChunkCoord that = (ChunkCoord) o;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + z;
        }
    }
}