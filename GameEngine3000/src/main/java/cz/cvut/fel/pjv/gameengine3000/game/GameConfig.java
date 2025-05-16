package cz.cvut.fel.pjv.gameengine3000.game;

public class GameConfig {

    public static final double ENEMY_SPAWN_INTERVAL = 5.0;
    public static final int MAX_ENEMIES = 5;
    public static final int PLAYER_ATTACK_DAMAGE = 25;
    public static final int ENEMY_COLLISION_DAMAGE = 0;
    public static final int PROJECTILE_DAMAGE = 50;
    public static final int SCORE_PER_KILL = 10;

    public static final String KNIGHT_WALK_SHEET = "/player/character/knight/Walk.png";
    public static final String KNIGHT_IDLE_SHEET = "/player/character/knight/Idle.png";
    public static final String KNIGHT_ATTACK_SHEET = "/player/character/knight/Attack.png";

    public static final String MAGE_WALK_SHEET = "/player/character/knight/Walk.png";
    public static final String MAGE_IDLE_SHEET = "/player/character/knight/Idle.png";
    public static final String MAGE_ATTACK_SHEET = "/player/character/knight/Attack.png";

    public static final String DEFAULT_ENEMY_WALK_SHEET = "/enemies/slime/slime_all_anims.png";
    public static final String BEAR_ENEMY_SHEET = "/enemies/bear/bear.png";


    public static final String DEFAULT_MAP_PATH = "/maps/level1.map";

    public static final String MAIN_MENU_BACKGROUND_SHEET = "/ui/menu_background.png"; // Example
    public static final int MAIN_MENU_BACKGROUND_FRAME_X = 0; // Col on sheet
    public static final int MAIN_MENU_BACKGROUND_FRAME_Y = 0; // Row on sheet
    public static final int MAIN_MENU_BACKGROUND_WIDTH = 1280; // Pixel width
    public static final int MAIN_MENU_BACKGROUND_HEIGHT = 720; // Pixel height

    public static final String BUTTON_SPRITESHEET = "/ui/buttons_sheet.png"; // A sheet with various button states

    // Example: "Play" Button (normal, hover, pressed)
    public static final int PLAY_BUTTON_NORMAL_COL = 0;
    public static final int PLAY_BUTTON_NORMAL_ROW = 0;
    public static final int PLAY_BUTTON_HOVER_COL = 1;
    public static final int PLAY_BUTTON_HOVER_ROW = 0;
    public static final int PLAY_BUTTON_PRESSED_COL = 2;
    public static final int PLAY_BUTTON_PRESSED_ROW = 0;
    public static final int BUTTON_WIDTH = 190; // Pixel width of one button sprite
    public static final int BUTTON_HEIGHT = 49; // Pixel height

    // Example: "Options" Button (could use same rows, different start cols or a different sheet)
    public static final int OPTIONS_BUTTON_NORMAL_COL = 0;
    public static final int OPTIONS_BUTTON_NORMAL_ROW = 1; // Assuming options button is on row 1 of same sheet
    public static final int OPTIONS_BUTTON_HOVER_COL = 1;
    public static final int OPTIONS_BUTTON_HOVER_ROW = 1;
    public static final int OPTIONS_BUTTON_PRESSED_COL = 2;
    public static final int OPTIONS_BUTTON_PRESSED_ROW = 1;

    // Pause Menu
    public static final String PAUSE_MENU_PANEL_SHEET = "/ui/pause_panel.png";
    public static final int PAUSE_MENU_PANEL_FRAME_X = 0;
    public static final int PAUSE_MENU_PANEL_FRAME_Y = 0;
    public static final int PAUSE_MENU_PANEL_WIDTH = 400;
    public static final int PAUSE_MENU_PANEL_HEIGHT = 300;

    // Lobby Scene
    public static final String LOBBY_BACKGROUND_SHEET = "/ui/lobby_background.png"; // Example

    private GameConfig() {}
}