package cz.cvut.fel.pjv.gameengine3000.map.levels;

import cz.cvut.fel.pjv.gameengine3000.map.Tile;
import cz.cvut.fel.pjv.gameengine3000.map.TileType;
import javafx.scene.layout.Pane;

import java.io.BufferedReader;
import java.io.IOException;       // Import IOException
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets; // Import StandardCharsets
import java.util.ArrayList;
import java.util.List;

public class BasicLevel { // Consider renaming to LevelMap

    // Use a 2D array for efficient grid access
    private Tile[][] tiles;
    private int width;  // Width in number of tiles
    private int height; // Height in number of tiles
    private final double tileSize;

    // Optional: Store player start position found during loading
    private int playerStartX = -1;
    private int playerStartY = -1;


    public BasicLevel(String mapFileName, double tileSize) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("Tile size must be positive.");
        }
        this.tileSize = tileSize;
        loadFromFile(mapFileName); // Call the corrected loading method
    }

    // Updated loadFromFile method in LV1 class
    private void loadFromFile(String mapFileName) {
        List<String> lines = new ArrayList<>();
        String mapPath = "/maps/" + mapFileName;

        try (InputStream is = getClass().getResourceAsStream(mapPath)) {
            if (is == null) {
                throw new RuntimeException("Cannot find map file resource: " + mapPath + ". Check path and build configuration.");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) { // Ignore empty lines
                        lines.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException while reading map file: " + mapPath, e);
        }

        if (lines.isEmpty()) {
            throw new RuntimeException("Map file is empty or could not be read: " + mapPath);
        }

        // Process CSV format
        this.height = lines.size();

        // Split the first line to determine width (assuming all lines have same number of elements)
        String[] firstLineParts = lines.get(0).split(",");
        this.width = firstLineParts.length;

        // Initialize the 2D tile grid
        this.tiles = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            String currentLine = lines.get(y);
            String[] tileCodes = currentLine.split(",");

            if (tileCodes.length != width) {
                System.err.println("Warning: Map line " + (y + 1) + " has inconsistent length. Expected " + width + ". Found " + tileCodes.length);
            }

            for (int x = 0; x < width; x++) {
                if (x >= tileCodes.length) {
                    System.err.println("Warning: Map line " + (y+1) + " is too short. Filling with GRASS at x=" + x);
                    tiles[y][x] = new Tile(TileType.GRASS, x, y, tileSize);
                    continue;
                }

                String symbol = tileCodes[x].trim();
                TileType type = TileType.fromSymbol(symbol);

                tiles[y][x] = new Tile(type, x, y, tileSize);

                // Record player start position if found
                if (symbol.equals("P")) {
                    if(playerStartX != -1) System.err.println("Warning: Multiple 'P' found. Using last one.");
                    playerStartX = x;
                    playerStartY = y;
                }
            }
        }
        System.out.println("Map loaded: " + width + "x" + height + " tiles.");
    }

    // Iterate over the 2D array for scene operations
    public void addToScene(Pane root) {
        if (tiles == null) return;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (tiles[y][x] != null && tiles[y][x].getView() != null) {
                    root.getChildren().add(tiles[y][x].getView());
                }
            }
        }
    }

    // Iterate over the 2D array for scene operations
    public void removeFromScene(Pane root) {
        if (tiles == null) return;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (tiles[y][x] != null && tiles[y][x].getView() != null) {
                    root.getChildren().remove(tiles[y][x].getView());
                }
            }
        }
    }

    /**
     * Checks passability using efficient 2D array lookup.
     * @param gridX Column index
     * @param gridY Row index
     * @return true if passable, false otherwise or if out of bounds.
     */
    public boolean isPassable(int gridX, int gridY) {
        // Bounds check
        if (gridX < 0 || gridX >= width || gridY < 0 || gridY >= height) {
            return false;
        }
        // Check tile exists and get its type's passability
        if (tiles[gridY][gridX] == null || tiles[gridY][gridX].getType() == null) {
            System.err.println("Warning: Checking passability for null tile at ("+gridX+","+gridY+")");
            return false; // Should not happen if loaded correctly
        }
        return tiles[gridY][gridX].getType().isPassable();
    }

    public double getTileSize() {
        return tileSize;
    }

    // Optional getters for dimensions / player start
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getPlayerStartX() { return playerStartX; }
    public int getPlayerStartY() { return playerStartY; }

}