package cz.cvut.fel.pjv.gameengine3000.map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Objects;

//represents a tile in the game map
public class Tile {
    //type of the tile (grass, wall, water etc.)
    private final TileType type;
    //visual representation of the tile
    private final ImageView view;
    //x coordinate in grid
    private final int x;
    //y coordinate in grid
    private final int y;

    //create tile with specified type and position
    public Tile(TileType type, int x, int y, double tileSize) {
        this.type = type;
        this.x = x;
        this.y = y;

        //load texture image and configure view
        this.view = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(type.getTexturePath()))));
        //set position and size
        this.view.setX(x * tileSize);
        this.view.setY(y * tileSize);
        this.view.setFitWidth(tileSize);
        this.view.setFitHeight(tileSize);
    }

    //get grid x position
    public int getX() { return x; }
    //get grid y position
    public int getY() { return y; }
    //get visual node for rendering
    public ImageView getView() { return view; }
    //check if tile can be walked through
    public boolean isPassable() { return type.isPassable(); }
    //get tile type enum
    public TileType getType() { return type; }
}
