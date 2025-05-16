package cz.cvut.fel.pjv.gameengine3000.game;

public class GameStatus {
    private MultiplayerRole currentRole = MultiplayerRole.NONE;
    private int localPlayerId = -1;
    private boolean gameIsOver = false;
    private boolean isPaused = false;
    private int score = 0;

    public MultiplayerRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(MultiplayerRole currentRole) { this.currentRole = currentRole; }

    public int getLocalPlayerId() { return localPlayerId; }
    public void setLocalPlayerId(int localPlayerId) { this.localPlayerId = localPlayerId; }

    public boolean isGameIsOver() { return gameIsOver; }
    public void setGameIsOver(boolean gameIsOver) { this.gameIsOver = gameIsOver; }

    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void incrementScore(int amount) { this.score += amount; }

    public void reset() {
        currentRole = MultiplayerRole.NONE;
        localPlayerId = -1;
        gameIsOver = false;
        isPaused = false;
        score = 0;
    }
}