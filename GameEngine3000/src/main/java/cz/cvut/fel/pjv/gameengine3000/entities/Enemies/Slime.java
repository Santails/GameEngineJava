//package cz.cvut.fel.pjv.gameengine3000.entities.Enemies;
//
//import cz.cvut.fel.pjv.gameengine3000.animation.AnimationManager;
//import cz.cvut.fel.pjv.gameengine3000.animation.EnemyAnimationManager;
//import cz.cvut.fel.pjv.gameengine3000.entities.Player;
//import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
//import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
//
//import java.util.Random;
//
//public class Slime extends Enemy {
//
//    // --- Bear Specific Constants ---
//    private static final String ENEMY_TYPE = "bear";
//    private static final int BEAR_HEALTH = 150;
//    private static final double BEAR_SPEED = 60; // Bears are a bit slower but hit harder
//    private static final double DETECTION_RANGE = 250.0; // How far the bear can see the player
//    private static final double ATTACK_RANGE = 55.0;     // How close the player needs to be to attack
//    private static final double ATTACK_DAMAGE = 25;      // How much damage the bear deals
//    private static final double ATTACK_WINDUP = 0.4;     // Time before attack hits during animation
//    private static final double ATTACK_COOLDOWN = 1.8;   // Time between attacks
//
//    // Placeholder Asset Paths - REPLACE THESE
//    private static final String BEAR_IDLE_SHEET = "/enemies/bear/idle.png";
//    private static final String BEAR_WALK_SHEET = "/enemies/bear/walk.png";
//    private static final String BEAR_ATTACK_SHEET = "/enemies/bear/attack.png";
//    // Add paths for other animations like death if you have them
//
//    // --- AI State ---
//    private final Random random = new Random();
//    private double stateTimer = 0.0; // Timer for current state (e.g., wandering duration)
//    private double wanderTargetX, wanderTargetY;
//    private final double WANDER_DISTANCE = 100.0; // How far to wander
//    private final double WANDER_INTERVAL = 3.0;   // How often to pick a new wander spot (roughly)
//
//    private double attackTimer = 0.0; // Timer for attack animation/action
//    private double attackCooldownTimer = 0.0; // Cooldown tracker
//    private boolean didDamageThisAttack = false; // Ensure damage only happens once per attack action
//
//    /**
//     * Constructor for the Bear enemy.
//     */
//    public Slime(int networkId, double startX, double startY,
//                double displayWidth, double displayHeight,
//                AssetManager assetManager, CustomMapLoader mapLoader) {
//
//        // Call the base class constructor
//        super(networkId, ENEMY_TYPE, startX, startY, displayWidth, displayHeight, assetManager, mapLoader);
//
//        // Override base stats with Bear specifics
//        this.maxHealth = BEAR_HEALTH;
//        this.health = BEAR_HEALTH;
//        this.speed = BEAR_SPEED;
//
//        // --- Initialize AnimationManager ---
//        // IMPORTANT: Adjust frame counts, positions, durations based on YOUR spritesheets
//        int nativeFrameW = 32; // Example native frame width
//        int nativeFrameH = 32; // Example native frame height
//        double idleFrameDur = 0.2;
//        double walkFrameDur = 0.15;
//        double attackFrameDur = 0.12; // Duration of each *visual* frame in attack anim
//
//        this.animationManager = new EnemyAnimationManager(this.enemyImageView, this.assetManager, "/enemies/bear/bear.png", "/enemies/bear/bear.png", 16, 16, 1, 1, 1, 1);
//
//        // Define animations (adjust frame counts and sheet layout)
//        // Assuming 4 directions (Down, Up, Left, Right) map to rows 0, 1, 2, 3
//        int idleFrames = 4;
//        this.animationManager.defineAnimation("IDLE", AnimationManager.Direction.DOWN, BEAR_IDLE_SHEET, nativeFrameW, nativeFrameH, 0, 0, idleFrames, idleFrameDur, true);
//        this.animationManager.defineAnimation("IDLE", AnimationManager.Direction.UP, BEAR_IDLE_SHEET, nativeFrameW, nativeFrameH, 0, 1, idleFrames, idleFrameDur, true);
//        this.animationManager.defineAnimation("IDLE", AnimationManager.Direction.LEFT, BEAR_IDLE_SHEET, nativeFrameW, nativeFrameH, 0, 2, idleFrames, idleFrameDur, true);
//        this.animationManager.defineAnimation("IDLE", AnimationManager.Direction.RIGHT, BEAR_IDLE_SHEET, nativeFrameW, nativeFrameH, 0, 3, idleFrames, idleFrameDur, true);
//
//        int walkFrames = 6;
//        this.animationManager.defineAnimation("WALK", AnimationManager.Direction.DOWN, BEAR_WALK_SHEET, nativeFrameW, nativeFrameH, 0, 0, walkFrames, walkFrameDur, true);
//        this.animationManager.defineAnimation("WALK", AnimationManager.Direction.UP, BEAR_WALK_SHEET, nativeFrameW, nativeFrameH, 0, 1, walkFrames, walkFrameDur, true);
//        this.animationManager.defineAnimation("WALK", AnimationManager.Direction.LEFT, BEAR_WALK_SHEET, nativeFrameW, nativeFrameH, 0, 2, walkFrames, walkFrameDur, true);
//        this.animationManager.defineAnimation("WALK", AnimationManager.Direction.RIGHT, BEAR_WALK_SHEET, nativeFrameW, nativeFrameH, 0, 3, walkFrames, walkFrameDur, true);
//
//        int attackFrames = 5; // Example
//        this.animationManager.defineAnimation("ATTACK", AnimationManager.Direction.DOWN, BEAR_ATTACK_SHEET, nativeFrameW, nativeFrameH, 0, 0, attackFrames, attackFrameDur, false); // Non-looping
//        this.animationManager.defineAnimation("ATTACK", AnimationManager.Direction.UP, BEAR_ATTACK_SHEET, nativeFrameW, nativeFrameH, 0, 1, attackFrames, attackFrameDur, false);
//        this.animationManager.defineAnimation("ATTACK", AnimationManager.Direction.LEFT, BEAR_ATTACK_SHEET, nativeFrameW, nativeFrameH, 0, 2, attackFrames, attackFrameDur, false);
//        this.animationManager.defineAnimation("ATTACK", AnimationManager.Direction.RIGHT, BEAR_ATTACK_SHEET, nativeFrameW, nativeFrameH, 0, 3, attackFrames, attackFrameDur, false);
//
//        // Set initial state
//        this.animationManager.setVisualState("IDLE", AnimationManager.Direction.DOWN);
//        this.currentState = BehaviorState.IDLE;
//        pickNewWanderTarget(); // Pick initial wander target
//    }
//
//    /**
//     * Bear-specific AI logic. Executed only by Host/SP.
//     */
//    @Override
//    public void update(double elapsedSeconds, Player targetPlayer) {
//        if (!alive || animationManager == null) {
//            if (animationManager != null) animationManager.update(elapsedSeconds); // Update dying animation if any
//            return;
//        }
//
//        // Update timers
//        stateTimer += elapsedSeconds;
//        if (attackCooldownTimer > 0) attackCooldownTimer -= elapsedSeconds;
//        if (attackTimer > 0) attackTimer -= elapsedSeconds;
//
//        // --- Determine Target ---
//        Player currentTarget = null;
//        double distanceToTarget = Double.MAX_VALUE;
//        if (targetPlayer != null && targetPlayer.isAlive()) {
//            double dx = targetPlayer.getX() - this.x;
//            double dy = targetPlayer.getY() - this.y;
//            distanceToTarget = Math.sqrt(dx * dx + dy * dy);
//            if (distanceToTarget <= DETECTION_RANGE) {
//                currentTarget = targetPlayer;
//            }
//        }
//
//        // --- State Transitions ---
//        BehaviorState previousState = currentState;
//
//        if (currentState == BehaviorState.ATTACKING) {
//            if (attackTimer <= 0) { // If attack animation/logic finished
//                currentState = BehaviorState.IDLE; // Default back to IDLE after attack
//                attackCooldownTimer = ATTACK_COOLDOWN; // Start cooldown
//                // Re-evaluate immediately if player still in range
//                if (currentTarget != null && distanceToTarget <= DETECTION_RANGE) {
//                    currentState = BehaviorState.CHASING;
//                }
//            }
//            // Stay in attacking state until timer runs out
//        } else if (currentTarget != null) { // If player is detected
//            if (distanceToTarget <= ATTACK_RANGE && attackCooldownTimer <= 0) {
//                currentState = BehaviorState.ATTACKING; // Close enough to attack
//                attackTimer = ATTACK_WINDUP + 0.1; // Duration of attack logic/animation (adjust)
//                didDamageThisAttack = false; // Reset damage flag for new attack
//            } else if (distanceToTarget <= DETECTION_RANGE) {
//                currentState = BehaviorState.CHASING; // Player detected, chase them
//            } else {
//                // Player detected before but now out of range, wander
//                currentState = BehaviorState.WANDERING;
//                stateTimer = 0; // Reset timer for new wander
//                pickNewWanderTarget();
//            }
//        } else { // Player not detected (or dead/null)
//            if (currentState == BehaviorState.CHASING) {
//                // Was chasing, now lost target
//                currentState = BehaviorState.WANDERING;
//                stateTimer = 0;
//                pickNewWanderTarget();
//            } else if (currentState == BehaviorState.IDLE && stateTimer > WANDER_INTERVAL * (0.5 + random.nextDouble())) {
//                currentState = BehaviorState.WANDERING;
//                stateTimer = 0;
//                pickNewWanderTarget();
//            } else if (currentState == BehaviorState.WANDERING && stateTimer > WANDER_INTERVAL * 1.5) { // Wander for a bit longer
//                currentState = BehaviorState.IDLE;
//                stateTimer = 0;
//            }
//            // Stay IDLE or WANDERING otherwise
//        }
//
//
//        // --- Actions based on State ---
//        double moveX = 0, moveY = 0;
//        String animState = "IDLE";
//
//        switch (currentState) {
//            case IDLE:
//                // Do nothing actively
//                stateTimer += elapsedSeconds;
//                animState = "IDLE";
//                break;
//
//            case WANDERING:
//                // Move towards wanderTarget
//                double dxWander = wanderTargetX - x;
//                double dyWander = wanderTargetY - y;
//                double distWander = Math.sqrt(dxWander * dxWander + dyWander * dyWander);
//
//                if (distWander < 10.0) { // Reached wander target (or close enough)
//                    currentState = BehaviorState.IDLE; // Stop wandering for now
//                    stateTimer = 0;
//                } else {
//                    // Normalize direction
//                    moveX = (dxWander / distWander) * speed * elapsedSeconds;
//                    moveY = (dyWander / distWander) * speed * elapsedSeconds;
//                    animState = "WALK";
//                }
//                break;
//
//            case CHASING:
//                // Move towards currentTarget
//                if (currentTarget != null) {
//                    double dxChase = currentTarget.getX() - x;
//                    double dyChase = currentTarget.getY() - y;
//                    double distChase = Math.sqrt(dxChase*dxChase + dyChase*dyChase); // Recalculate needed for normalization
//
//                    if (distChase > 1.0) { // Avoid division by zero / jittering when very close
//                        moveX = (dxChase / distChase) * speed * elapsedSeconds;
//                        moveY = (dyChase / distChase) * speed * elapsedSeconds;
//                    }
//                    animState = "WALK";
//                } else {
//                    // Target lost mid-chase? Switch back (should be handled by transitions)
//                    currentState = BehaviorState.IDLE;
//                    animState = "IDLE";
//                }
//                break;
//
//            case ATTACKING:
//                // Animation handles visual, logic handles damage
//                animState = "ATTACK";
//                // Attempt to deal damage near the windup point
//                // Check if player is still close enough when the 'hit' should occur
//                if (!didDamageThisAttack && attackTimer <= 0.1) { // Check near end of windup
//                    if (currentTarget != null && distanceToTarget <= ATTACK_RANGE * 1.2) { // Allow slightly larger range for hit
//                        currentTarget.takeDamage((int) ATTACK_DAMAGE);
//                        System.out.println("Bear "+networkId+" hit Player "+currentTarget.getNetworkID());
//                        didDamageThisAttack = true;
//                    }
//                }
//                // No movement while attacking
//                moveX = 0;
//                moveY = 0;
//                break;
//        }
//
//        // --- Execute Movement & Update Animation ---
//        if (moveX != 0 || moveY != 0) {
//            move(moveX, moveY); // move() handles collision and updates lastDirection
//        }
//
//        // Set animation state based on behavior
//        animationManager.setVisualState(animState, this.lastDirection);
//        animationManager.update(elapsedSeconds);
//    }
//
//    private void pickNewWanderTarget() {
//        // Pick a random point within WANDER_DISTANCE
//        double angle = random.nextDouble() * 2 * Math.PI;
//        wanderTargetX = x + Math.cos(angle) * WANDER_DISTANCE;
//        wanderTargetY = y + Math.sin(angle) * WANDER_DISTANCE;
//        // Basic bounds check (optional)
//        // wanderTargetX = Math.max(0, Math.min(wanderTargetX, paneWidth));
//        // wanderTargetY = Math.max(0, Math.min(wanderTargetY, paneHeight));
//        stateTimer = 0; // Reset timer for new wander segment
//    }
//}
