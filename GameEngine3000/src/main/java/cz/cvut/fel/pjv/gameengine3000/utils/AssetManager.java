package cz.cvut.fel.pjv.gameengine3000.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AssetManager {

    private final Map<String, Image> spritesheetCache;
    private final Map<String, Image> spriteCache;

    public AssetManager() {
        this.spritesheetCache = new HashMap<>();
        this.spriteCache = new HashMap<>();
    }

    public Image loadSpritesheet(String resourcePath) {
        Objects.requireNonNull(resourcePath, "Resource path cannot be null.");
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;

        if (spritesheetCache.containsKey(normalizedPath)) {
            return spritesheetCache.get(normalizedPath);
        }

        try (InputStream is = getClass().getResourceAsStream(normalizedPath)) {
            if (is == null) {
                System.err.println("AssetManager: Cannot find resource: " + normalizedPath);
                return null;
            }
            Image sheet = new Image(is);
            if (sheet.isError()) {
                System.err.println("AssetManager: Error loading image: " + normalizedPath);
                if (sheet.getException() != null) {
                    sheet.getException().printStackTrace();
                }
                return null;
            }
            spritesheetCache.put(normalizedPath, sheet);
            return sheet;
        } catch (Exception e) {
            System.err.println("AssetManager: Exception loading image stream: " + normalizedPath + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Image getSpriteImage(String sheetResourcePath, int spriteX, int spriteY, int spriteWidth, int spriteHeight) {
        Objects.requireNonNull(sheetResourcePath, "Sheet resource path cannot be null.");
        String normalizedSheetPath = sheetResourcePath.startsWith("/") ? sheetResourcePath : "/" + sheetResourcePath;

        String spriteCacheKey = normalizedSheetPath + ":" + spriteX + "," + spriteY + "," + spriteWidth + "," + spriteHeight;

        if (spriteCache.containsKey(spriteCacheKey)) {
            return spriteCache.get(spriteCacheKey);
        }

        Image spritesheet = loadSpritesheet(normalizedSheetPath);
        if (spritesheet == null) {
            return null;
        }

        if (spriteX < 0 || spriteY < 0 ||
                spriteWidth <= 0 || spriteHeight <= 0 ||
                spriteX + spriteWidth > spritesheet.getWidth() + 0.001 ||
                spriteY + spriteHeight > spritesheet.getHeight() + 0.001) {
            System.err.println("AssetManager: Invalid sprite coordinates/dimensions for " + spriteCacheKey +
                    " on sheet " + normalizedSheetPath + " (Size: " + spritesheet.getWidth() + "x" + spritesheet.getHeight() + ")");
            return null;
        }

        try {
            PixelReader reader = spritesheet.getPixelReader();
            if (reader == null) {
                System.err.println("AssetManager: PixelReader is null for " + normalizedSheetPath);
                return null;
            }
            WritableImage sprite = new WritableImage(reader, spriteX, spriteY, spriteWidth, spriteHeight);
            spriteCache.put(spriteCacheKey, sprite);
            return sprite;
        } catch (Exception e) {
            System.err.println("AssetManager: Error extracting sprite for " + spriteCacheKey + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Image getSprite(String sheetResourcePath, int col, int row, int spriteWidth, int spriteHeight) {
        if (spriteWidth <= 0 || spriteHeight <= 0) {
            System.err.println("AssetManager: Sprite width and height must be positive for grid-based getSprite.");
            return null;
        }
        int spriteX = col * spriteWidth;
        int spriteY = row * spriteHeight;
        return getSpriteImage(sheetResourcePath, spriteX, spriteY, spriteWidth, spriteHeight);
    }


    public void clearCache() {
        spritesheetCache.clear();
        spriteCache.clear();
    }
}