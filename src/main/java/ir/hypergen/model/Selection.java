package ir.hypergen.model;

import lombok.Data;
import org.bukkit.World;

@Data
public class Selection {
    private World world;
    private Shape shape;
    private Pattern pattern;
    private int centerX;
    private int centerZ;
    private int radius;
    
    public Selection() {
        this.shape = Shape.SQUARE;
        this.pattern = Pattern.SPIRAL;
    }
    
    public int getTotalChunks() {
        if (shape == Shape.SQUARE) {
            int chunks = (radius * 2 + 1);
            return chunks * chunks;
        } else {
            return (int) (Math.PI * radius * radius);
        }
    }
    
    public boolean isValid() {
        return world != null && radius > 0;
    }
    
    public enum Shape {
        SQUARE, CIRCLE
    }
    
    public enum Pattern {
        SPIRAL, CONCENTRIC
    }
}