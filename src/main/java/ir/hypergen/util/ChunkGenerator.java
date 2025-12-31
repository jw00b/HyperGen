package ir.hypergen.util;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import ir.hypergen.model.Selection;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ChunkGenerator {
    private final HyperGen plugin;
    private final GenerationTask task;
    private final List<GenerationTask.ChunkCoord> chunkQueue;
    private int currentIndex;
    private boolean complete;
    
    public ChunkGenerator(HyperGen plugin, GenerationTask task) {
        this.plugin = plugin;
        this.task = task;
        this.chunkQueue = new ArrayList<>();
        this.currentIndex = 0;
        this.complete = false;
    }
    
    public void prepare() {
        Selection selection = task.getSelection();
        int centerChunkX = selection.getCenterX() >> 4;
        int centerChunkZ = selection.getCenterZ() >> 4;
        int radius = selection.getRadius();
        
        if (selection.getPattern() == Selection.Pattern.SPIRAL) {
            generateSpiralPattern(centerChunkX, centerChunkZ, radius, selection.getShape());
        } else {
            generateConcentricPattern(centerChunkX, centerChunkZ, radius, selection.getShape());
        }
        
        task.setTotalChunks(chunkQueue.size());
        task.getRemainingChunks().addAll(chunkQueue);
    }
    
    private void generateSpiralPattern(int centerX, int centerZ, int radius, Selection.Shape shape) {
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int maxSteps = (radius * 2 + 1) * (radius * 2 + 1);
        
        for (int i = 0; i < maxSteps; i++) {
            int chunkX = centerX + x;
            int chunkZ = centerZ + z;
            
            if (isInBounds(x, z, radius, shape)) {
                chunkQueue.add(new GenerationTask.ChunkCoord(chunkX, chunkZ));
            }
            
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            
            x += dx;
            z += dz;
        }
    }
    
    private void generateConcentricPattern(int centerX, int centerZ, int radius, Selection.Shape shape) {
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(z) == r) {
                        if (isInBounds(x, z, radius, shape)) {
                            chunkQueue.add(new GenerationTask.ChunkCoord(centerX + x, centerZ + z));
                        }
                    }
                }
            }
        }
    }
    
    private boolean isInBounds(int x, int z, int radius, Selection.Shape shape) {
        if (shape == Selection.Shape.SQUARE) {
            return Math.abs(x) <= radius && Math.abs(z) <= radius;
        } else {
            return (x * x + z * z) <= (radius * radius);
        }
    }
    
    public void processNextBatch() {
        if (complete || currentIndex >= chunkQueue.size()) {
            complete = true;
            return;
        }
        
        int chunksPerTick = getChunksPerTick();
        World world = task.getWorld();
        
        for (int i = 0; i < chunksPerTick && currentIndex < chunkQueue.size(); i++) {
            GenerationTask.ChunkCoord coord = chunkQueue.get(currentIndex);
            
            world.getChunkAtAsync(coord.getX(), coord.getZ(), (chunk) -> {
                if (chunk != null) {
                    chunk.load(true);
                    task.getProcessedChunks().add(coord);
                    task.getRemainingChunks().remove(coord);
                    task.setCurrentChunk(task.getCurrentChunk() + 1);
                    
                    plugin.getStatisticsManager().recordChunkGeneration(world, 1, System.currentTimeMillis() - task.getStartTime());
                }
            });
            
            currentIndex++;
        }
        
        if (currentIndex >= chunkQueue.size()) {
            complete = true;
        }
    }
    
    private int getChunksPerTick() {
        switch (task.getMode()) {
            case NORMAL:
                return plugin.getConfigManager().getNormalModeChunksPerSecond() / 20;
            case PRO:
                return calculateProModeChunks();
            case FAST:
                return plugin.getConfigManager().getFastModeChunksPerTick();
            default:
                return 4;
        }
    }
    
    private int calculateProModeChunks() {
        double currentTPS = getCurrentTPS();
        double targetTPS = plugin.getConfigManager().getProModeTargetTPS();
        double minTPS = plugin.getConfigManager().getProModeMinTPS();
        int maxChunks = plugin.getConfigManager().getProModeMaxChunksPerTick();
        int minChunks = plugin.getConfigManager().getProModeMinChunksPerTick();
        
        if (currentTPS >= targetTPS) {
            return maxChunks;
        } else if (currentTPS <= minTPS) {
            return minChunks;
        } else {
            double ratio = (currentTPS - minTPS) / (targetTPS - minTPS);
            return (int) (minChunks + (maxChunks - minChunks) * ratio);
        }
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
}