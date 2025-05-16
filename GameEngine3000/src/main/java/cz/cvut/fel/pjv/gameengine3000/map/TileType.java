package cz.cvut.fel.pjv.gameengine3000.map;

public enum TileType {
    GRASS("G", "/textures/grass.png", true),
    WATER("W", "/textures/water.png", false),
    STONE("S", "/textures/stone.png", true),
    SAND("D", "/textures/sand.png", true);

    private final String symbol;
    private final String texturePath;
    private final boolean passable;

    TileType(String symbol, String texturePath, boolean passable) {
        this.symbol = symbol;
        this.texturePath = texturePath;
        this.passable = passable;
    }

    // Getters
    public String getTexturePath() { return texturePath; }
    public boolean isPassable() { return passable; }

    public static TileType fromSymbol(String symbol) {
        for (TileType type : values()) {
            if (type.symbol.equals(symbol)) return type;
        }
        return GRASS; // default
    }
}
