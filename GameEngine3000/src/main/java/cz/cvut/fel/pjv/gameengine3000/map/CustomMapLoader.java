package cz.cvut.fel.pjv.gameengine3000.map; // Adjust package if needed

import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
import cz.cvut.fel.pjv.gameengine3000.utils.SpriteSheet; // Assuming this helper exists
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.application.Platform; // Needed for UI updates

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap; // Preserves layer order
import java.util.List;
import java.util.Map;

/**
 * Loads and manages a custom layered map from a .map text file.
 * Uses DEFINE_SHEET directives and an AssetManager to handle multiple spritesheets.
 */
public class CustomMapLoader {

    // Helper class to store data for a single map layer
    private static class MapLayerData {
        final String name;
        // Grid stores [SheetID, Column, Row] for each tile. [0]=SheetID, [1]=Col, [2]=Row.
        double[][][] gridData; // Initialized later
        final int widthTiles;  // Final dimensions in tiles
        final int heightTiles;
        final Pane visualPane; // JavaFX Pane holding visuals for this layer

        MapLayerData(String name, int widthTiles, int heightTiles) {
            this.name = name;
            this.widthTiles = widthTiles;
            this.heightTiles = heightTiles;
            // Initialize gridData with final dimensions. Default 0 means empty/no sheet.
            this.gridData = new double[heightTiles][widthTiles][3];
            this.visualPane = new Pane();
        }
    }

    private final AssetManager assetManager;
    // Store SpriteSheet objects directly, mapped by ID defined in the map file
    private final Map<Integer, SpriteSheet> spriteSheets;
    private final Map<String, MapLayerData> layersByName; // Access layers by name
    private final List<MapLayerData> layersInOrder; // Keep render order
    private final Pane mapContainerPane; // Parent pane holding all layer panes

    private int mapWidthTiles = 0; // Use int for tile counts
    private int mapHeightTiles = 0;
    private double baseTileWidth = 32; // Default, can be overridden by first sheet
    private double baseTileHeight = 32;

    /**
     * Loads the map layout and initializes tileset handlers.
     * @param mapResourcePath Path to the custom .map file (e.g., "/maps/level1.map").
     * @param assetManager Shared AssetManager instance.
     */
    public CustomMapLoader(String mapResourcePath, AssetManager assetManager) {
        if (assetManager == null) throw new IllegalArgumentException("AssetManager cannot be null.");
        this.assetManager = assetManager;
        this.spriteSheets = new HashMap<>();
        this.layersByName = new LinkedHashMap<>(); // Use LinkedHashMap to preserve layer order
        this.layersInOrder = new ArrayList<>();
        this.mapContainerPane = new Pane();

        try {
            loadMapFile(mapResourcePath);
            if (!layersInOrder.isEmpty()) { // Only create visuals if layers were loaded
                createVisuals();
            } else {
                System.err.println("CustomMapLoader: No layers were loaded from the map file.");
            }
        } catch (Exception e) {
            System.err.println("CustomMapLoader: FATAL error loading map: " + e.getMessage());
            // Optionally re-throw or handle appropriately
            throw new RuntimeException("Failed to initialize CustomMapLoader", e);
        }

    }

    private void loadMapFile(String mapResourcePath) {
        System.out.println("CustomMapLoader: Loading map: " + mapResourcePath);
        String resourcePath = mapResourcePath.startsWith("/") ? mapResourcePath : "/" + mapResourcePath;

        String currentParsingLayerName = null;
        List<String[]> currentRowData = new ArrayList<>();
        int expectedWidth = -1; // Use int

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Cannot find map file resource: " + resourcePath);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\s+", 2); // Split command/first word

                    if ("DEFINE_SHEET".equalsIgnoreCase(parts[0]) && parts.length > 1) {
                        parseDefineSheet(parts[1], lineNum);
                    } else if ("LAYER".equalsIgnoreCase(parts[0]) && parts.length > 1) {
                        // Finalize previous layer before starting new one
                        finalizeLayerData(currentParsingLayerName, currentRowData, expectedWidth);
                        // Start new layer parsing state
                        currentParsingLayerName = parts[1];
                        System.out.println("  Starting Layer: " + currentParsingLayerName);
                        currentRowData = new ArrayList<>();
                        expectedWidth = -1; // Reset expected width for the new layer
                    } else if (currentParsingLayerName != null) { // Assumed to be layer data row
                        String[] tileCodes = line.split(",");
                        if (expectedWidth == -1) {
                            expectedWidth = tileCodes.length; // Set expected width from first row
                        } else if (tileCodes.length != expectedWidth) {
                            // Handle inconsistent row length (padding or warning)
                            tileCodes = handleInconsistentRow(tileCodes, expectedWidth, currentParsingLayerName, lineNum);
                        }
                        currentRowData.add(tileCodes);
                    } else {
                        System.err.println("Warning: Unexpected line " + lineNum + " (ignored): " + line);
                    }
                }
                // Finalize the very last layer after loop ends
                finalizeLayerData(currentParsingLayerName, currentRowData, expectedWidth);

            } // BufferedReader closes automatically
        } catch (Exception e) {
            // Catch exceptions during reading/parsing
            throw new RuntimeException("Failed to load or parse custom map file: " + resourcePath, e);
        }

        // Determine overall map dimensions from loaded layers
        for (MapLayerData layer : layersInOrder) {
            mapWidthTiles = Math.max(mapWidthTiles, layer.widthTiles);
            mapHeightTiles = Math.max(mapHeightTiles, layer.heightTiles);
        }
        if (mapWidthTiles > 0 && mapHeightTiles > 0) {
            mapContainerPane.setPrefSize(mapWidthTiles * baseTileWidth, mapHeightTiles * baseTileHeight);
        }
        System.out.println("CustomMapLoader: Map loading finished. Dimensions: " + mapWidthTiles + "x" + mapHeightTiles + " tiles.");
    }

    private void parseDefineSheet(String definition, int lineNum) {
        String[] defParts = definition.split("\\s+");
        if (defParts.length == 4) {
            try {
                int id = Integer.parseInt(defParts[0]);
                if (id <= 0) throw new IllegalArgumentException("Sheet ID must be positive.");
                String path = defParts[1];
                int w = Integer.parseInt(defParts[2]);
                int h = Integer.parseInt(defParts[3]);
                if (w <= 0 || h <= 0) throw new IllegalArgumentException("Sheet dimensions must be positive.");

                // Create SpriteSheet wrapper, AssetManager handles actual loading
                spriteSheets.put(id, new SpriteSheet(assetManager, path, w, h));
                System.out.println("  Defined Sheet ID " + id + " -> " + path + " (" + w + "x" + h + ")");
                if (spriteSheets.size() == 1) { // Use first sheet for base tile size
                    baseTileWidth = w;
                    baseTileHeight = h;
                    System.out.println("  Base tile size set to: " + w + "x" + h);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing DEFINE_SHEET at line " + lineNum + ": " + definition + " - " + e.getMessage());
            }
        } else {
            System.err.println("Invalid DEFINE_SHEET format at line " + lineNum + ": Needs ID Path Width Height");
        }
    }

    private String[] handleInconsistentRow(String[] tileCodes, int expectedWidth, String layerName, int lineNum) {
        System.err.println("Warning: Inconsistent row length in layer '" + layerName + "' at line " + lineNum +
                ". Expected " + expectedWidth + ", got " + tileCodes.length);
        if (tileCodes.length < expectedWidth) {
            String[] padded = new String[expectedWidth];
            System.arraycopy(tileCodes, 0, padded, 0, tileCodes.length);
            // Fill remaining with empty codes
            for (int i = tileCodes.length; i < expectedWidth; i++) padded[i] = ".";
            return padded;
        } else {
            // Truncate if too long
            return java.util.Arrays.copyOf(tileCodes, expectedWidth);
        }
    }

    // Helper to process collected row data and finalize a layer's data grid
    private void finalizeLayerData(String layerName, List<String[]> rowData, int width) {
        // Check if there's actually data to finalize
        if (layerName == null || rowData == null || rowData.isEmpty() || width <= 0) return;

        int height = rowData.size();
        // Create the final layer object with correct dimensions NOW
        MapLayerData finalizedLayer = new MapLayerData(layerName, width, height);

        for (int y = 0; y < height; y++) {
            String[] tileCodes = rowData.get(y);
            // Ensure tileCodes array has expected width (should be handled by padding logic)
            if (tileCodes.length != width) {
                System.err.println("INTERNAL ERROR: Finalizing layer '" + layerName + "', row " + y + " has unexpected length " + tileCodes.length + " (expected " + width + "). Skipping row population.");
                continue; // Avoid potential ArrayOutOfBounds
            }
            for (int x = 0; x < width; x++) {
                // No need for x boundary check if padding worked correctly
                String code = tileCodes[x].trim();
                if (code.equals(".") || code.isEmpty()) continue; // Skip empty tiles represented by "."

                String[] idParts = code.split(":");
                if (idParts.length == 3) {
                    try {
                        int sheetId = Integer.parseInt(idParts[0]);
                        int col = Integer.parseInt(idParts[1]);
                        int row = Integer.parseInt(idParts[2]);
                        if (spriteSheets.containsKey(sheetId)) { // Check if sheet ID is valid
                            // Store parsed data in the grid
                            finalizedLayer.gridData[y][x][0] = sheetId;
                            finalizedLayer.gridData[y][x][1] = col;
                            finalizedLayer.gridData[y][x][2] = row;
                        } else { System.err.println("Warning: Undefined Sheet ID " + sheetId + " used in layer '" + finalizedLayer.name + "' at (" + x + "," + y + ")"); }
                    } catch (NumberFormatException e) { System.err.println("Warning: Invalid tile code number format '" + code + "' in layer '" + finalizedLayer.name + "' at (" + x + "," + y + ")"); }
                } else { System.err.println("Warning: Invalid tile code format '" + code + "' (expected SheetID:Col:Row) in layer '" + finalizedLayer.name + "' at (" + x + "," + y + ")"); }
            }
        }
        layersByName.put(finalizedLayer.name, finalizedLayer); // Store by name
        layersInOrder.add(finalizedLayer);                    // Store in order
        System.out.println("  Finalized layer data: '" + finalizedLayer.name + "' (" + finalizedLayer.widthTiles + "x" + finalizedLayer.heightTiles + ")");
    }

    // Create the JavaFX visuals based on loaded data
    private void createVisuals() {
        System.out.println("CustomMapLoader: Creating visuals...");
        mapContainerPane.getChildren().clear(); // Clear any previous visuals

        if (spriteSheets.isEmpty()) {
            System.err.println("CustomMapLoader: No sprite sheets defined or loaded. Cannot create visuals.");
            return;
        }

        for (MapLayerData layer : layersInOrder) { // Render using the ordered list
            System.out.println("  Rendering layer: " + layer.name);
            layer.visualPane.getChildren().clear(); // Clear previous visuals for this layer's pane

            for (int y = 0; y < layer.heightTiles; y++) {
                for (int x = 0; x < layer.widthTiles; x++) {
                    // Access grid data safely
                    double sheetId = layer.gridData[y][x][0];
                    if (sheetId > 0) { // Only render if sheet ID is valid (positive)
                        int col = (int) layer.gridData[y][x][1];
                        int row = (int) layer.gridData[y][x][2];

                        SpriteSheet sheet = spriteSheets.get((int)sheetId); // Cast sheetId to int for lookup
                        if (sheet != null) {
                            // Use the SpriteSheet helper to get the specific sprite Image
                            Image tileImage = sheet.getSprite(col, row); // Assuming getSprite takes col, row
                            if (tileImage != null) {
                                ImageView tileView = new ImageView(tileImage);
                                // Position based on overall map tile size
                                tileView.setX(x * baseTileWidth);
                                tileView.setY(y * baseTileHeight);
                                // No resizing needed if sprite dimensions match baseTileWidth/Height
                                layer.visualPane.getChildren().add(tileView);
                            } else {
                                System.err.println("Warning: SpriteSheet ID " + (int)sheetId + " returned null for sprite at [" + col + "," + row + "]");
                            }
                        }
                        // No warning here for missing sheet, already warned during parsing
                    }
                }
            }
            // Add the individual layer's pane to the main container
            // Use Platform.runLater if createVisuals might be called off FX thread
            Platform.runLater(() -> {
                if (!mapContainerPane.getChildren().contains(layer.visualPane)) {
                    mapContainerPane.getChildren().add(layer.visualPane);
                }
            });
        }
        System.out.println("CustomMapLoader: Visuals created.");
    }

    /** Adds the map visuals (all layers) to the specified root Pane on the JavaFX thread. */
    public void addToScene(Pane root) {
        Platform.runLater(() -> {
            if (!root.getChildren().contains(mapContainerPane)) {
                root.getChildren().add(0, mapContainerPane); // Add behind other game elements
            }
        });
    }

    /** Removes map visuals from the specified root Pane on the JavaFX thread. */
    public void removeFromScene(Pane root) {
        Platform.runLater(() -> {
            root.getChildren().remove(mapContainerPane);
        });
    }

    /**
     * Checks if a specific TILE location is passable.
     * Assumes layers named "collision" or "walls" define non-passable areas.
     * @param gridX The X coordinate of the tile (integer).
     * @param gridY The Y coordinate of the tile (integer).
     * @return true if the tile is passable, false otherwise.
     */
    public boolean isPassable(int gridX, int gridY) { // Changed parameters to int
        // Check overall map boundaries first
        if (gridX < 0 || gridX >= mapWidthTiles || gridY < 0 || gridY >= mapHeightTiles) {
            // System.out.println("Check Passable ("+gridX+","+gridY+"): Out of map bounds -> false"); // DEBUG
            return false; // Outside map bounds is not passable
        }

        // Look for a specific layer defining collisions
        MapLayerData collisionLayer = layersByName.get("collision");
        if (collisionLayer == null) {
            collisionLayer = layersByName.get("walls"); // Fallback name check
        }
        // Add more potential collision layer names if needed

        if (collisionLayer != null) {
            // Check if the coordinate is within the collision layer's specific grid
            if (gridX >= collisionLayer.widthTiles || gridY >= collisionLayer.heightTiles) {
                // System.out.println("Check Passable ("+gridX+","+gridY+"): Outside collision layer '" + collisionLayer.name + "' -> true"); // DEBUG
                return true; // Outside this specific layer's defined grid means passable by default
            }
            // Passable IF AND ONLY IF the tile index is 0 (empty) on the collision layer
            boolean hasBlockingTile = collisionLayer.gridData[gridY][gridX][0] > 0;
            // System.out.println("Check Passable ("+gridX+","+gridY+"): Collision layer '" + collisionLayer.name + "' has tile? " + hasBlockingTile + " -> " + !hasBlockingTile); // DEBUG
            return !hasBlockingTile;
        }

        // System.out.println("Check Passable ("+gridX+","+gridY+"): No collision layer found -> true"); // DEBUG
        return true; // Default passable if no specific collision layer exists
    }

    /**
     * Checks if a specific PIXEL location is passable by converting it to tile coordinates.
     * @param pixelX The X coordinate in pixels.
     * @param pixelY The Y coordinate in pixels.
     * @return true if the corresponding tile is passable, false otherwise.
     */
    public boolean isPassablePixel(double pixelX, double pixelY) {
        if (baseTileWidth <= 0 || baseTileHeight <= 0) {
            System.err.println("Warning: Cannot check pixel passability with invalid tile dimensions.");
            return false; // Cannot determine grid if tile size is invalid
        }
        int gridX = (int) Math.floor(pixelX / baseTileWidth);
        int gridY = (int) Math.floor(pixelY / baseTileHeight);
        return isPassable(gridX, gridY);
    }

    /**
     * Checks if a rectangular area defined by pixel coordinates is passable.
     * It checks the four corner points of the rectangle. A more robust check
     * might involve checking all tiles the rectangle overlaps.
     * @param pixelX Top-left X coordinate of the area in pixels.
     * @param pixelY Top-left Y coordinate of the area in pixels.
     * @param width Width of the area in pixels.
     * @param height Height of the area in pixels.
     * @return true if all corner points are on passable tiles, false otherwise.
     */
    public boolean isAreaPassable(double pixelX, double pixelY, double width, double height) {
        // Check the four corners of the bounding box
        boolean topLeftPassable = isPassablePixel(pixelX, pixelY);
        boolean topRightPassable = isPassablePixel(pixelX + width, pixelY);
        boolean bottomLeftPassable = isPassablePixel(pixelX, pixelY + height);
        boolean bottomRightPassable = isPassablePixel(pixelX + width, pixelY + height);

        // Consider adding center point check?
        // boolean centerPassable = isPassablePixel(pixelX + width / 2.0, pixelY + height / 2.0);

        return topLeftPassable && topRightPassable && bottomLeftPassable && bottomRightPassable; //&& centerPassable;
    }


    // --- Getters ---
    public double getTileWidth() { return baseTileWidth; }
    public double getTileHeight() { return baseTileHeight; }
    public int getMapWidthTiles() { return mapWidthTiles; } // Return int
    public int getMapHeightTiles() { return mapHeightTiles; } // Return int
    /** Gets the parent Pane containing all individual layer Panes. Add this to your scene. */
    public Pane getMapContainerPane() { return mapContainerPane; }

}
