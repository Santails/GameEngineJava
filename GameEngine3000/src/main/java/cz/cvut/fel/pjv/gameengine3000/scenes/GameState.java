package cz.cvut.fel.pjv.gameengine3000.scenes;

import cz.cvut.fel.pjv.gameengine3000.game.MultiplayerRole;

public enum GameState {
    INITIALIZING,
    MAIN_MENU,
    MULTIPLAYER_LOBBY,
    PLAYING_SP,
    PLAYING_MP_HOST,
    PLAYING_MP_CLIENT,
    PAUSED,
    GAME_OVER,
    EXITING;

    public static MultiplayerRole getRequiredRole(GameState state) {
        if (state == null) {
            return null;
        }
        switch (state) {
            case PLAYING_SP:
                return MultiplayerRole.NONE;
            case PLAYING_MP_HOST:
                return MultiplayerRole.HOST;
            case PLAYING_MP_CLIENT:
                return MultiplayerRole.CLIENT;
            default:
                return null;
        }
    }
}