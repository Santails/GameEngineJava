package cz.cvut.fel.pjv.gameengine3000.game;

import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.entities.Projectile;
import cz.cvut.fel.pjv.gameengine3000.multiplayer.MultiplayerCoordinator;
import cz.cvut.fel.pjv.gameengine3000.game.GameStatus;
import javafx.scene.input.KeyCode;

public class InputHandler {
    private final EntityManager entityManager;
    private final GameStatus gameStatus;
    private final MultiplayerCoordinator multiplayerCoordinator;

    public InputHandler(EntityManager entityManager, GameStatus gameStatus, MultiplayerCoordinator multiplayerCoordinator) {
        this.entityManager = entityManager;
        this.gameStatus = gameStatus;
        this.multiplayerCoordinator = multiplayerCoordinator;
    }

    public void handleKeyPress(KeyCode code) {
        Player p = entityManager.getLocalPlayer();
        if (p == null || gameStatus.isPaused() || gameStatus.isGameIsOver()) return;
        switch (code) {
            case W: case UP:    p.goUp = true; break;
            case S: case DOWN:  p.goDown = true; break;
            case A: case LEFT:  p.goLeft = true; break;
            case D: case RIGHT: p.goRight = true; break;
            case SPACE:         p.requestAttack(); break;
            default: break;
        }
    }

    public void handleKeyRelease(KeyCode code) {
        Player p = entityManager.getLocalPlayer();
        if (p == null) return;
        switch (code) {
            case W: case UP:    p.goUp = false; break;
            case S: case DOWN:  p.goDown = false; break;
            case A: case LEFT:  p.goLeft = false; break;
            case D: case RIGHT: p.goRight = false; break;
            default: break;
        }
    }

    public void clearLocalPlayerMovementFlags() {
        Player p = entityManager.getLocalPlayer();
        if (p != null) { p.goUp=false; p.goDown=false; p.goLeft=false; p.goRight=false; }
    }

    public void handleMouseClick(double sceneX, double sceneY) {
        if (multiplayerCoordinator == null || !multiplayerCoordinator.isAuthoritative() || gameStatus.isPaused() || gameStatus.isGameIsOver()) return;
        Player p = entityManager.getLocalPlayer();
        if (p == null || !p.isAlive()) return;

        Projectile proj = new Projectile(p.getX(), p.getY(), sceneX, sceneY);
        entityManager.addProjectile(proj);
    }
}