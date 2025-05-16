package cz.cvut.fel.pjv.gameengine3000.entities;

import cz.cvut.fel.pjv.gameengine3000.animation.AnimationManager;
import cz.cvut.fel.pjv.gameengine3000.animation.PlayerAnimationManager;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class Player {

    private final ImageView playerImageView;
    private final PlayerAnimationManager animationManager;
    private CustomMapLoader mapLoader;

    private final int networkID;
    private final boolean isLocalPlayer;

    private double x, y;
    private double speed = 150;
    private double displayWidth, displayHeight;
    public boolean goUp, goDown, goLeft, goRight;

    private int health = 100;
    private boolean alive = true;
    private boolean isLogicallyAttacking = false;
    private final double attackLogicDuration = 0.3;
    private double attackLogicTimer = 0;
    private final double attackCooldown = 0.5;
    private double attackCooldownTimer = 0;
    private boolean performAttackRequest = false;
    private final double attackRange = 40;
    private final double attackWidth = 30;

    private final double collisionBoxWidthFactor = 0.6;
    private final double collisionBoxHeightFactor = 0.8;
    private final double collisionBoxYOffsetFactor = 0.1;


    public Player(int networkID, boolean isLocal,
                  double startX, double startY,
                  AssetManager assetManager, CustomMapLoader mapLoader,
                  double targetDisplayWidth, double targetDisplayHeight,
                  String playerWalkSheetPath, String playerIdleSheetPath, String playerAttackSheetPath) {

        this.networkID = networkID;
        this.isLocalPlayer = isLocal;
        this.x = startX;
        this.y = startY;
        this.mapLoader = mapLoader;
        this.displayWidth = targetDisplayWidth;
        this.displayHeight = targetDisplayHeight;

        this.playerImageView = new ImageView();
        this.playerImageView.setFitWidth(this.displayWidth);
        this.playerImageView.setFitHeight(this.displayHeight);
        this.playerImageView.setPreserveRatio(true);

        Objects.requireNonNull(assetManager, "AssetManager cannot be null for Player constructor.");

        this.animationManager = new PlayerAnimationManager(this.playerImageView, assetManager,
                playerWalkSheetPath, playerIdleSheetPath, playerAttackSheetPath,
                16, 16,
                0.12, 0.075,
                this.attackLogicDuration);

        updateVisualPosition();
        updateAliveVisualState();
    }

    public void update(double elapsedSeconds, double screenWidth, double screenHeight) {
        if (!alive) {
            if (animationManager != null) animationManager.update(elapsedSeconds);
            return;
        }

        if (isLogicallyAttacking) {
            attackLogicTimer -= elapsedSeconds;
            if (attackLogicTimer <= 0) {
                isLogicallyAttacking = false;
            }
        }

        if (isLocalPlayer) {
            if (attackCooldownTimer > 0) attackCooldownTimer -= elapsedSeconds;
            if (performAttackRequest && !isLogicallyAttacking && attackCooldownTimer <= 0) startLogicalAttack();
            performAttackRequest = false;

            handleLocalMovement(elapsedSeconds);
            clampToScreenBounds(screenWidth, screenHeight);
            updateLocalAnimationState();
        }

        if (animationManager != null) animationManager.update(elapsedSeconds);
        updateVisualPosition();
    }

    private void handleLocalMovement(double elapsedSeconds) {
        double deltaX = 0, deltaY = 0;
        boolean isMovingInput = (goUp || goDown || goLeft || goRight);

        boolean movementDuringAttackAllowed = isLogicallyAttacking && (attackLogicTimer > attackLogicDuration * 0.7 || attackLogicTimer < attackLogicDuration * 0.3);
        boolean movementAllowed = (!isLogicallyAttacking || movementDuringAttackAllowed) && isMovingInput;


        if (movementAllowed) {
            double actualSpeed = speed;
            double dxInput = 0, dyInput = 0;
            if (goLeft) dxInput--;
            if (goRight) dxInput++;
            if (goUp) dyInput--;
            if (goDown) dyInput++;

            if (dxInput != 0 && dyInput != 0) {
                actualSpeed /= Math.sqrt(2.0);
            }
            deltaX = dxInput * actualSpeed * elapsedSeconds;
            deltaY = dyInput * actualSpeed * elapsedSeconds;
        }

        double nextX = x + deltaX;
        double nextY = y + deltaY;

        Rectangle2D currentCollisionBounds = getCollisionBounds();
        double checkWidth = currentCollisionBounds.getWidth();
        double checkHeight = currentCollisionBounds.getHeight();

        if (mapLoader != null) {
            if (deltaX != 0) {
                double checkCollisionX = (deltaX > 0) ? nextX + checkWidth / 2.0 -1 : nextX - checkWidth / 2.0;
                if (!mapLoader.isAreaPassable(checkCollisionX - checkWidth/2.0, y - checkHeight/2.0 + displayHeight * collisionBoxYOffsetFactor, checkWidth, checkHeight)) {
                    deltaX = 0;
                    nextX = x;
                }
            }
            x = nextX;

            if (deltaY != 0) {
                double checkCollisionY = (deltaY > 0) ? nextY + checkHeight / 2.0 -1 : nextY - checkHeight / 2.0;
                if (!mapLoader.isAreaPassable(x - checkWidth/2.0, checkCollisionY - checkHeight/2.0 + displayHeight * collisionBoxYOffsetFactor, checkWidth, checkHeight)) {
                    deltaY = 0;
                    nextY = y;
                }
            }
            y = nextY;
        } else {
            x = nextX;
            y = nextY;
        }
    }

    private void clampToScreenBounds(double screenWidth, double screenHeight) {
        x = Math.max(displayWidth / 2.0, Math.min(x, screenWidth - displayWidth / 2.0));
        y = Math.max(displayHeight / 2.0, Math.min(y, screenHeight - displayHeight / 2.0));
    }

    private void updateLocalAnimationState() {
        PlayerAnimationManager.PlayerState targetState;
        AnimationManager.Direction facingDirection = animationManager.getLastFacingDirection();

        if (isLogicallyAttacking) {
            targetState = PlayerAnimationManager.PlayerState.ATTACKING;
            if (isMoving()) facingDirection = determineDirectionFromInput();
        } else if (isMoving()) {
            targetState = PlayerAnimationManager.PlayerState.WALKING;
            facingDirection = determineDirectionFromInput();
        } else {
            targetState = PlayerAnimationManager.PlayerState.IDLE;
        }
        animationManager.setVisualState(targetState, facingDirection);
    }

    private AnimationManager.Direction determineDirectionFromInput() {
        if (goLeft) return AnimationManager.Direction.LEFT;
        if (goRight) return AnimationManager.Direction.RIGHT;
        if (goUp) return AnimationManager.Direction.UP;
        if (goDown) return AnimationManager.Direction.DOWN;
        return animationManager.getLastFacingDirection();
    }

    public void applyNetworkState(double newX, double newY, String dirStr,
                                  boolean isNetAttacking, int newHealth, boolean isNetMoving) {
        if (isLocalPlayer) {
            if (this.health != newHealth) setHealth(newHealth);
            return;
        }

        this.x = newX;
        this.y = newY;
        setHealth(newHealth);

        if (isNetAttacking && !this.isLogicallyAttacking) startLogicalAttack();
        else if (!isNetAttacking && this.isLogicallyAttacking) {
            this.isLogicallyAttacking = false;
            this.attackLogicTimer = 0;
        }

        AnimationManager.Direction netDir = AnimationManager.directionFromString(dirStr);
        PlayerAnimationManager.PlayerState targetAnimState;
        if (this.isLogicallyAttacking) targetAnimState = PlayerAnimationManager.PlayerState.ATTACKING;
        else if (isNetMoving) targetAnimState = PlayerAnimationManager.PlayerState.WALKING;
        else targetAnimState = PlayerAnimationManager.PlayerState.IDLE;

        if (animationManager != null) {
            animationManager.setVisualState(targetAnimState, netDir);
        }
        updateVisualPosition();
    }

    private void updateVisualPosition() {
        Platform.runLater(() -> {
            playerImageView.setX(this.x - this.displayWidth / 2.0);
            playerImageView.setY(this.y - this.displayHeight / 2.0);
        });
    }

    private void updateAliveVisualState() {
        Platform.runLater(() -> {
            playerImageView.setVisible(this.alive);
            playerImageView.setOpacity(this.alive ? 1.0 : 0.6);
        });
    }

    public void requestAttack() {
        if (isLocalPlayer && alive && !isLogicallyAttacking && attackCooldownTimer <= 0) {
            this.performAttackRequest = true;
        }
    }

    private void startLogicalAttack() {
        if (!alive) return;
        isLogicallyAttacking = true;
        attackLogicTimer = attackLogicDuration;
        if (isLocalPlayer) {
            attackCooldownTimer = attackCooldown;
            updateLocalAnimationState();
        }
    }

    public void takeDamage(int amount) {
        if (!alive) return;
        this.health -= amount;
        if (this.health <= 0) {
            this.health = 0;
            this.alive = false;
            if (animationManager != null) {
                animationManager.setVisualState(PlayerAnimationManager.PlayerState.IDLE, animationManager.getLastFacingDirection());
            }
        }
        updateAliveVisualState();
    }

    public void setHealth(int newHealth) {
        boolean wasAlive = this.alive;
        this.health = Math.max(0, newHealth);
        this.alive = this.health > 0;
        if (wasAlive != this.alive) updateAliveVisualState();
    }

    public Rectangle2D getAttackHitbox() {
        if (!isLogicallyAttacking || attackLogicTimer <= 0 || !alive) return null;
        AnimationManager.Direction facing = animationManager.getLastFacingDirection();
        double hbX = x, hbY = y, hbW = 0, hbH = 0;
        double offsetX = displayWidth / 2.0;
        double offsetY = displayHeight / 2.0;
        switch (facing) {
            case UP:    hbX = x - attackWidth / 2.0; hbY = y - offsetY - attackRange; hbW = attackWidth; hbH = attackRange; break;
            case DOWN:  hbX = x - attackWidth / 2.0; hbY = y + offsetY; hbW = attackWidth; hbH = attackRange; break;
            case LEFT:  hbX = x - offsetX - attackRange; hbY = y - attackWidth / 2.0; hbW = attackRange; hbH = attackWidth; break;
            case RIGHT: hbX = x + offsetX; hbY = y - attackWidth / 2.0; hbW = attackRange; hbH = attackWidth; break;
            default: return null;
        }
        return new Rectangle2D(hbX, hbY, hbW, hbH);
    }

    public ImageView getPlayerImageView() { return playerImageView; }
    public int getNetworkID() { return networkID; }
    public boolean isLocalPlayer() { return isLocalPlayer; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHealth() { return health; }
    public boolean isAlive() { return alive; }
    public boolean isMoving() { return (goUp || goDown || goLeft || goRight); }
    public String getLastDirectionString() { return animationManager.getLastFacingDirection().toString(); }
    public boolean isCurrentlyLogicallyAttacking() { return this.isLogicallyAttacking && this.attackLogicTimer > 0; }

    public void setPosition(double x, double y) { this.x = x; this.y = y; updateVisualPosition(); }
    public void setMapLoader(CustomMapLoader mapLoader) { this.mapLoader = mapLoader; }

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
