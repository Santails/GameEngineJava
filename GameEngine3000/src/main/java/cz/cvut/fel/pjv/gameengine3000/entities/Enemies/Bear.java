package cz.cvut.fel.pjv.gameengine3000.entities.Enemies;

import cz.cvut.fel.pjv.gameengine3000.animation.AnimationManager;
import cz.cvut.fel.pjv.gameengine3000.animation.EnemyAnimationManager;
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;

import javafx.geometry.Point2D;
import java.util.Random;

public class Bear extends Enemy {

    private static final int BEAR_HEALTH = 150;
    private static final double BEAR_SPEED = 45;
    private static final double DETECTION_RANGE = 200.0;
    private static final double ATTACK_RANGE = 45.0;
    private static final int ATTACK_DAMAGE = 10;
    private static final double ATTACK_WINDUP_TIME = 0.5;
    private static final double ATTACK_ACTIVE_TIME = 0.3;
    private static final double ATTACK_COOLDOWN = 2.0;

    private static final String BEAR_IDLE_SHEET_PATH = "/enemies/bear/bear.png";
    private static final String BEAR_WALK_SHEET_PATH = "/enemies/bear/bear.png";
    private static final String BEAR_ATTACK_SHEET_PATH = "/enemies/bear/bear.png";

    private final Random random = new Random();
    private double stateTimer = 0.0;
    private double wanderTargetX, wanderTargetY;
    private final double WANDER_DISTANCE = 80.0;
    private final double WANDER_INTERVAL_MIN = 2.0;
    private final double WANDER_INTERVAL_MAX = 5.0;
    private double currentWanderInterval;

    private double attackActionTimer = 0.0;
    private double attackCooldownTimer = 0.0;
    private boolean hasDamagedThisAttack = false;

    public Bear(int networkId, double startX, double startY,
                double displayWidth, double displayHeight,
                AssetManager assetManager, CustomMapLoader mapLoader) {
        super(networkId, "bear", startX, startY, displayWidth, displayHeight, assetManager, mapLoader);

        this.maxHealth = BEAR_HEALTH;
        this.health = BEAR_HEALTH;
        this.speed = BEAR_SPEED;

        int nativeFrameW = 16;
        int nativeFrameH = 16;
        double idleFrameDur = 0.25;
        double walkFrameDur = 0.18;
        double attackFrameDur = (ATTACK_WINDUP_TIME + ATTACK_ACTIVE_TIME) / 4.0;
        int idleFrames = 4;
        int walkFrames = 4;
        int attackFrames = 4;

        this.animationManager = new EnemyAnimationManager(this.enemyImageView, this.assetManager,
                BEAR_IDLE_SHEET_PATH, BEAR_WALK_SHEET_PATH,
                nativeFrameW, nativeFrameH,
                idleFrames, walkFrames,
                idleFrameDur, walkFrameDur);

        this.animationManager.defineAnimation(EnemyAnimationManager.ATTACK, AnimationManager.Direction.DOWN, BEAR_ATTACK_SHEET_PATH, nativeFrameW, nativeFrameH, 8, 1, attackFrames, attackFrameDur, false);
        this.animationManager.defineAnimation(EnemyAnimationManager.ATTACK, AnimationManager.Direction.UP, BEAR_ATTACK_SHEET_PATH, nativeFrameW, nativeFrameH, 8, 3, attackFrames, attackFrameDur, false);
        this.animationManager.defineAnimation(EnemyAnimationManager.ATTACK, AnimationManager.Direction.LEFT, BEAR_ATTACK_SHEET_PATH, nativeFrameW, nativeFrameH, 8, 4, attackFrames, attackFrameDur, false);
        this.animationManager.defineAnimation(EnemyAnimationManager.ATTACK, AnimationManager.Direction.RIGHT, BEAR_ATTACK_SHEET_PATH, nativeFrameW, nativeFrameH, 8, 2, attackFrames, attackFrameDur, false);

        this.animationManager.setVisualState(EnemyAnimationManager.IDLE, AnimationManager.Direction.DOWN);
        pickNewWanderTarget();
        resetWanderInterval();
    }

    @Override
    public void update(double elapsedSeconds, Player targetPlayer) {
        if (!alive) {
            if (currentState != BehaviorState.DEAD) {
                currentState = BehaviorState.DEAD;
                if (animationManager != null) animationManager.setVisualState(EnemyAnimationManager.DEATH, lastDirection);
            }
            if (animationManager != null) animationManager.update(elapsedSeconds);
            return;
        }

        stateTimer += elapsedSeconds;
        if (attackCooldownTimer > 0) attackCooldownTimer -= elapsedSeconds;
        if (attackActionTimer > 0) attackActionTimer -= elapsedSeconds;

        Player currentTarget = null;
        double distanceToTarget = Double.MAX_VALUE;

        if (targetPlayer != null && targetPlayer.isAlive()) {
            double dx = targetPlayer.getX() - this.x;
            double dy = targetPlayer.getY() - this.y;
            distanceToTarget = Math.sqrt(dx * dx + dy * dy);
            if (distanceToTarget <= DETECTION_RANGE) {
                currentTarget = targetPlayer;
            }
        }

        BehaviorState previousState = currentState;

        if (currentState == BehaviorState.ATTACKING) {
            if (attackActionTimer <= 0) {
                currentState = BehaviorState.IDLE;
                attackCooldownTimer = ATTACK_COOLDOWN;
            } else if (attackActionTimer <= ATTACK_ACTIVE_TIME && !hasDamagedThisAttack) {
                if (currentTarget != null && distanceToTarget <= ATTACK_RANGE * 1.1) {
                    currentTarget.takeDamage(ATTACK_DAMAGE);
                    hasDamagedThisAttack = true;
                }
            }
        } else if (currentTarget != null) {
            if (distanceToTarget <= ATTACK_RANGE && attackCooldownTimer <= 0) {
                currentState = BehaviorState.ATTACKING;
                attackActionTimer = ATTACK_WINDUP_TIME + ATTACK_ACTIVE_TIME;
                hasDamagedThisAttack = false;
                if (this.x > currentTarget.getX()) this.lastDirection = AnimationManager.Direction.LEFT;
                else this.lastDirection = AnimationManager.Direction.RIGHT;
            } else {
                currentState = BehaviorState.CHASING;
            }
        } else {
            if (currentState == BehaviorState.CHASING || (currentState == BehaviorState.IDLE && stateTimer > currentWanderInterval)) {
                currentState = BehaviorState.WANDERING;
                pickNewWanderTarget();
                resetWanderInterval();
                stateTimer = 0;
            } else if (currentState == BehaviorState.WANDERING) {
                double dxW = wanderTargetX - x;
                double dyW = wanderTargetY - y;
                if (Math.sqrt(dxW * dxW + dyW * dyW) < 10.0 || stateTimer > WANDER_INTERVAL_MAX * 1.5) {
                    currentState = BehaviorState.IDLE;
                    stateTimer = 0;
                    resetWanderInterval();
                }
            }
        }
    }

    @Override
    public Point2D calculateMovementIntent(double elapsedSeconds, Player targetPlayer) {
        if (!alive || currentState == BehaviorState.ATTACKING || currentState == BehaviorState.DEAD) {
            return Point2D.ZERO;
        }

        double moveX = 0, moveY = 0;

        if (currentState == BehaviorState.CHASING && targetPlayer != null && targetPlayer.isAlive()) {
            double dx = targetPlayer.getX() - x;
            double dy = targetPlayer.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > ATTACK_RANGE * 0.8) {
                moveX = (dx / distance) * speed * elapsedSeconds;
                moveY = (dy / distance) * speed * elapsedSeconds;
            }
        } else if (currentState == BehaviorState.WANDERING) {
            double dx = wanderTargetX - x;
            double dy = wanderTargetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > 1.0) {
                moveX = (dx / distance) * speed * 0.7 * elapsedSeconds;
                moveY = (dy / distance) * speed * 0.7 * elapsedSeconds;
            }
        }
        return new Point2D(moveX, moveY);
    }

    @Override
    protected void updateAnimationAfterMovement(double actualDX, double actualDY, double elapsedSeconds) {
        if (animationManager == null) return;

        String animToPlay;
        switch (currentState) {
            case ATTACKING:
                animToPlay = EnemyAnimationManager.ATTACK;
                break;
            case CHASING:
            case WANDERING:
                animToPlay = (actualDX != 0 || actualDY != 0) ? EnemyAnimationManager.WALK : EnemyAnimationManager.IDLE;
                break;
            case DEAD:
                animToPlay = EnemyAnimationManager.DEATH;
                break;
            case IDLE:
            default:
                animToPlay = EnemyAnimationManager.IDLE;
                break;
        }

        animationManager.setVisualState(animToPlay, this.lastDirection);
        animationManager.update(elapsedSeconds);
    }

    private void pickNewWanderTarget() {
        double angle = random.nextDouble() * 2 * Math.PI;
        wanderTargetX = x + Math.cos(angle) * WANDER_DISTANCE;
        wanderTargetY = y + Math.sin(angle) * WANDER_DISTANCE;
    }

    private void resetWanderInterval() {
        currentWanderInterval = WANDER_INTERVAL_MIN + random.nextDouble() * (WANDER_INTERVAL_MAX - WANDER_INTERVAL_MIN);
    }
}