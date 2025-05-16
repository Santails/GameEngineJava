package cz.cvut.fel.pjv.gameengine3000.entities.Enemies;

import cz.cvut.fel.pjv.gameengine3000.animation.AnimationManager;
import cz.cvut.fel.pjv.gameengine3000.animation.EnemyAnimationManager;
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.multiplayer.EnemyState;
import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;

import java.util.Objects;

public abstract class Enemy {

    protected final ImageView enemyImageView;
    protected EnemyAnimationManager animationManager;
    protected final CustomMapLoader mapLoader;
    protected AssetManager assetManager;

    protected final int networkId;
    protected final String enemyType;

    protected double x, y;
    protected double displayWidth, displayHeight;
    protected double speed;
    public int health;
    protected int maxHealth;
    public boolean alive = true;

    public AnimationManager.Direction lastDirection = AnimationManager.Direction.DOWN;

    protected enum BehaviorState { IDLE, WANDERING, CHASING, ATTACKING, DEAD }
    protected BehaviorState currentState = BehaviorState.IDLE;

    private final double collisionBoxWidthFactor = 0.7;
    private final double collisionBoxHeightFactor = 0.9;
    private final double collisionBoxYOffsetFactor = 0.05;


    public Enemy(int networkId, String enemyType, double startX, double startY,
                 double displayWidth, double displayHeight,
                 AssetManager assetManager, CustomMapLoader mapLoader) {
        this.networkId = networkId;
        this.enemyType = enemyType;
        this.x = startX;
        this.y = startY;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.assetManager = Objects.requireNonNull(assetManager);
        this.mapLoader = mapLoader;

        this.enemyImageView = new ImageView();
        this.enemyImageView.setFitWidth(this.displayWidth);
        this.enemyImageView.setFitHeight(this.displayHeight);
        this.enemyImageView.setPreserveRatio(true);

        updateVisualPosition();
        updateAliveVisualState();
    }

    public abstract void update(double elapsedSeconds, Player targetPlayer);
    public abstract Point2D calculateMovementIntent(double elapsedSeconds, Player targetPlayer);

    public void applyActualMovement(double actualDX, double actualDY, double elapsedSeconds, double screenWidth, double screenHeight) {
        double nextX = this.x + actualDX;
        double nextY = this.y + actualDY;

        Rectangle2D currentCollisionBounds = getCollisionBounds();
        double checkWidth = currentCollisionBounds.getWidth();
        double checkHeight = currentCollisionBounds.getHeight();

        if (mapLoader != null) {
            if (actualDX != 0) {
                double checkCollisionX = (actualDX > 0) ? nextX + checkWidth / 2.0 -1 : nextX - checkWidth / 2.0;
                if (!mapLoader.isAreaPassable(checkCollisionX - checkWidth/2.0, y - checkHeight/2.0 + displayHeight * collisionBoxYOffsetFactor, checkWidth, checkHeight)) {
                    actualDX = 0;
                    nextX = this.x;
                }
            }
            this.x = nextX;

            if (actualDY != 0) {
                double checkCollisionY = (actualDY > 0) ? nextY + checkHeight / 2.0 -1 : nextY - checkHeight / 2.0;
                if (!mapLoader.isAreaPassable(this.x - checkWidth/2.0, checkCollisionY - checkHeight/2.0 + displayHeight * collisionBoxYOffsetFactor, checkWidth, checkHeight)) {
                    actualDY = 0;
                    nextY = this.y;
                }
            }
            this.y = nextY;
        } else {
            this.x = nextX;
            this.y = nextY;
        }

        clampToScreenBounds(screenWidth, screenHeight);

        if (actualDX > 0) this.lastDirection = AnimationManager.Direction.RIGHT;
        else if (actualDX < 0) this.lastDirection = AnimationManager.Direction.LEFT;

        if (actualDY > 0 && Math.abs(actualDY) > Math.abs(actualDX)) this.lastDirection = AnimationManager.Direction.DOWN;
        else if (actualDY < 0 && Math.abs(actualDY) > Math.abs(actualDX)) this.lastDirection = AnimationManager.Direction.UP;

        updateAnimationAfterMovement(actualDX, actualDY, elapsedSeconds);
        updateVisualPosition();
    }

    protected abstract void updateAnimationAfterMovement(double actualDX, double actualDY, double elapsedSeconds);

    protected void clampToScreenBounds(double screenWidth, double screenHeight) {
        x = Math.max(displayWidth / 2.0, Math.min(x, screenWidth - displayWidth / 2.0));
        y = Math.max(displayHeight / 2.0, Math.min(y, screenHeight - displayHeight / 2.0));
    }

    protected void updateVisualPosition() {
        Platform.runLater(() -> {
            enemyImageView.setX(this.x - this.displayWidth / 2.0);
            enemyImageView.setY(this.y - this.displayHeight / 2.0);
        });
    }

    public void updateAliveVisualState() {
        Platform.runLater(() -> {
            enemyImageView.setVisible(this.alive);
            enemyImageView.setOpacity(this.alive ? 1.0 : 0.7);
        });
    }

    public void takeDamage(int amount) {
        if (!alive) return;
        this.health -= amount;
        if (this.health <= 0) {
            this.health = 0;
            this.alive = false;
            this.currentState = BehaviorState.DEAD;
        }
        updateAliveVisualState();
    }

    public void applyNetworkState(EnemyState state) {
        this.x = state.x;
        this.y = state.y;
        boolean previousAlive = this.alive;
        this.alive = state.alive;
        if(previousAlive != this.alive) updateAliveVisualState();

        this.lastDirection = AnimationManager.directionFromString(state.direction);

        String animStateToPlay = EnemyAnimationManager.IDLE;
        if (!this.alive) {
            animStateToPlay = EnemyAnimationManager.DEATH;
        } else if (state.chasing) {
            animStateToPlay = EnemyAnimationManager.WALK;
        }

        if (this.animationManager != null) {
            if (state.type.equalsIgnoreCase(this.enemyType) && this.animationManager.getCurrentStateName().equals(EnemyAnimationManager.ATTACK) && !state.chasing) {

            } else {
                this.animationManager.setVisualState(animStateToPlay, this.lastDirection);
            }
        }
        updateVisualPosition();
    }


    public ImageView getEnemyImageView() { return enemyImageView; }
    public int getNetworkId() { return networkId; }
    public String getEnemyType() { return enemyType; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHealth() { return health; }
    public boolean isAlive() { return alive; }
    public double getDisplayWidth() { return displayWidth; }
    public double getDisplayHeight() { return displayHeight; }

    public Rectangle2D getCollisionBounds() {
        double colWidth = displayWidth * collisionBoxWidthFactor;
        double colHeight = displayHeight * collisionBoxHeightFactor;
        double colX = x - colWidth / 2.0;
        double colY = y - colHeight / 2.0 + displayHeight * collisionBoxYOffsetFactor;
        return new Rectangle2D(colX, colY, colWidth, colHeight);
    }
}