package cz.cvut.fel.pjv.gameengine3000.game;

import cz.cvut.fel.pjv.gameengine3000.entities.Enemies.Enemy;
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.entities.Projectile;
import cz.cvut.fel.pjv.gameengine3000.game.GameStatus;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.scene.layout.Pane;

import java.util.function.Consumer;

public class CollisionHandler {
    private final EntityManager entityManager;
    private final GameStatus gameStatus;
    private final Pane rootPane;
    private final Consumer<Player> onPlayerDeathCallback;

    public CollisionHandler(EntityManager entityManager, GameStatus gameStatus, Pane rootPane, Consumer<Player> onPlayerDeathCallback) {
        this.entityManager = entityManager;
        this.gameStatus = gameStatus;
        this.rootPane = rootPane;
        this.onPlayerDeathCallback = onPlayerDeathCallback;
    }

    public void checkAuthoritativeCollisions(double screenWidth, double screenHeight) {
        if (gameStatus.isGameIsOver()) return;

        for (Player p : entityManager.getAllPlayers()) {
            if (p != null && p.isAlive() && p.isCurrentlyLogicallyAttacking()) {
                checkPlayerAttackCollisions(p);
            }
        }
        if (gameStatus.isGameIsOver()) return;

        checkEnemyPlayerCollisions();
        if (gameStatus.isGameIsOver()) return;

        checkProjectileCollisions(screenWidth, screenHeight);
    }

    public void checkPlayerAttackCollisions(Player attacker) {
        Rectangle2D attackHitbox = attacker.getAttackHitbox();
        if (attackHitbox == null) return;

        for (Enemy e : entityManager.getNetworkedEnemies().values()) {
            if (e.isAlive() && e.getEnemyImageView() != null) {
                Rectangle2D enemyBounds = e.getCollisionBounds();
                if (attackHitbox.intersects(enemyBounds.getMinX(), enemyBounds.getMinY(), enemyBounds.getWidth(), enemyBounds.getHeight())) {
                    e.takeDamage(GameConfig.PLAYER_ATTACK_DAMAGE);
                    if (!e.isAlive()) {
                        gameStatus.incrementScore(GameConfig.SCORE_PER_KILL);
                    }
                }
            }
        }
    }

    private void checkEnemyPlayerCollisions() {
        for (Enemy e : entityManager.getNetworkedEnemies().values()) {
            if (!e.isAlive() || e.getEnemyImageView() == null || gameStatus.isGameIsOver()) continue;
            Rectangle2D enemyBounds = e.getCollisionBounds();

            for (Player p : entityManager.getAllPlayers()) {
                if (p != null && p.isAlive() && p.getPlayerImageView() != null) {
                    Rectangle2D playerBounds = p.getCollisionBounds();
                    if (playerBounds.intersects(enemyBounds.getMinX(), enemyBounds.getMinY(), enemyBounds.getWidth(), enemyBounds.getHeight())) {
                        p.takeDamage(GameConfig.ENEMY_COLLISION_DAMAGE);
                        if (!p.isAlive()) {
                            onPlayerDeathCallback.accept(p);
                            if (gameStatus.isGameIsOver()) return;
                        }
                    }
                }
            }
        }
    }

    private void checkProjectileCollisions(double screenWidth, double screenHeight) {
        entityManager.getProjectiles().removeIf(projectile -> {
            boolean hit = false;
            if (!projectile.doesExist() || projectile.isOutOfBounds(screenWidth, screenHeight)) {
                if (projectile.getCircle() != null) Platform.runLater(() -> rootPane.getChildren().remove(projectile.getCircle()));
                return true;
            }
            if (projectile.getCircle() == null) return true;

            Bounds projectileBounds = projectile.getBounds();

            for (Enemy enemy : entityManager.getNetworkedEnemies().values()) {
                Rectangle2D enemyCollisionRect = enemy.getCollisionBounds();
                if (enemy.isAlive() && enemy.getEnemyImageView() != null &&
                        projectileBounds.intersects(enemyCollisionRect.getMinX(), enemyCollisionRect.getMinY(), enemyCollisionRect.getWidth(), enemyCollisionRect.getHeight())) {
                    enemy.takeDamage(GameConfig.PROJECTILE_DAMAGE);
                    if (!enemy.isAlive()) {
                        gameStatus.incrementScore(GameConfig.SCORE_PER_KILL);
                    }
                    hit = true;
                    break;
                }
            }

            if (hit) {
                Platform.runLater(() -> rootPane.getChildren().remove(projectile.getCircle()));
            }
            return hit;
        });
    }
}