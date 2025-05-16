package cz.cvut.fel.pjv.gameengine3000.utils;

import javafx.scene.image.Image;
import java.util.Objects;

public class SpriteSheet {

    private final AssetManager assetManager;
    private final String sheetResourcePath;
    private final int spriteWidth;
    private final int spriteHeight;
    private Image fullSheetImage;

    private int columns = -1;
    private int rows = -1;

    public SpriteSheet(AssetManager assetManager, String sheetResourcePath, int spriteWidth, int spriteHeight) {
        this.assetManager = Objects.requireNonNull(assetManager, "AssetManager cannot be null.");
        this.sheetResourcePath = Objects.requireNonNull(sheetResourcePath, "Sheet resource path cannot be null.").startsWith("/") ? sheetResourcePath : "/" + sheetResourcePath;
        if (spriteWidth <= 0 || spriteHeight <= 0) {
            throw new IllegalArgumentException("Sprite width and height must be positive.");
        }
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
    }

    public Image getFullSheetImage() {
        if (fullSheetImage == null) {
            fullSheetImage = assetManager.loadSpritesheet(this.sheetResourcePath);
            if (fullSheetImage == null) {
                System.err.println("SpriteSheet: Failed to load backing image: " + this.sheetResourcePath);
            }
        }
        return fullSheetImage;
    }

    public Image getSprite(int col, int row) {
        return assetManager.getSprite(this.sheetResourcePath, col, row, this.spriteWidth, this.spriteHeight);
    }

    public int getColumns() {
        if (columns == -1) {
            calculateDimensions();
        }
        return columns;
    }

    public int getRows() {
        if (rows == -1) {
            calculateDimensions();
        }
        return rows;
    }

    private void calculateDimensions() {
        Image sheet = getFullSheetImage();
        if (sheet != null) {
            if (this.spriteWidth > 0) {
                this.columns = (int) (sheet.getWidth() / this.spriteWidth);
            }
            if (this.spriteHeight > 0) {
                this.rows = (int) (sheet.getHeight() / this.spriteHeight);
            }
        } else {
            this.columns = 0;
            this.rows = 0;
        }
    }

    public String getSheetResourcePath() {
        return sheetResourcePath;
    }

    public int getSpriteWidth() {
        return spriteWidth;
    }

    public int getSpriteHeight() {
        return spriteHeight;
    }
}