package cz.cvut.fel.pjv.gameengine3000.animation; // Adjust package as needed

import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;
// No need to import Image or Rectangle2D here if base class handles it

/**
 * Manages animations specifically for the Player entity.
 * Defines "IDLE", "WALK", and "ATTACK" states using the base AnimationManager.
 */
public class PlayerAnimationManager extends AnimationManager {

    // Player-specific state names (constants for consistency)
    public static final String IDLE_STATE = "IDLE";
    public static final String WALK_STATE = "WALK";
    public static final String ATTACK_STATE = "ATTACK";

    // Player state enum for type-safe state setting
    public enum PlayerState { IDLE, WALKING, ATTACKING }

    private final double attackVisualDuration; // Total duration visual attack should play

    /**
     * Constructor for PlayerAnimationManager.
     *
     * @param imageView             The player's ImageView to animate.
     * @param assetManager          Shared AssetManager for loading spritesheets.
     * @param walkSheetPath         Path to walking spritesheet.
     * @param idleSheetPath         Path to idle spritesheet.
     * @param attackSheetPath       Path to attack spritesheet.
     * @param nativeFrameWidth      Width of a single frame on the sheets.
     * @param nativeFrameHeight     Height of a single frame on the sheets.
     * @param walkFrameDuration     Duration of each walking frame.
     * @param attackAnimFrameDuration Duration of each *visual* attack frame.
     * @param attackLogicDuration   Duration of the attack's *logic* (used to match visual roughly).
     */
    public PlayerAnimationManager(ImageView imageView, AssetManager assetManager,
                                  String walkSheetPath, String idleSheetPath, String attackSheetPath,
                                  int nativeFrameWidth, int nativeFrameHeight,
                                  double walkFrameDuration, double attackAnimFrameDuration,
                                  double attackLogicDuration) {
        super(imageView, assetManager); // Call base constructor

        this.attackVisualDuration = attackLogicDuration; // Try to match visual to logic duration

        // --- Define Animations ---
        // These parameters (startFrameX, startFrameY, frameCount) depend heavily on your spritesheet layouts.
        // Example: Assuming spritesheets where:
        // - Row 0: Down animation frames
        // - Row 1: Up animation frames
        // - Row 2: Left animation frames
        // - Row 3: Right animation frames
        // And all start at column 0 on their respective rows.

        // --- IDLE Animations ---
        int idleFramesPerDirection = 1; // Example: 4 frames for idle in each direction
        double idleFrameDisplayDuration = walkFrameDuration * 1.5; // Idle often slower
        if (idleSheetPath != null && !idleSheetPath.isEmpty()) {
            defineAnimation(IDLE_STATE, Direction.DOWN,  idleSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 0, idleFramesPerDirection, idleFrameDisplayDuration, true);
            defineAnimation(IDLE_STATE, Direction.UP,    idleSheetPath, nativeFrameWidth, nativeFrameHeight, 1, 0, idleFramesPerDirection, idleFrameDisplayDuration, true);
            defineAnimation(IDLE_STATE, Direction.LEFT,  idleSheetPath, nativeFrameWidth, nativeFrameHeight, 2, 0, idleFramesPerDirection, idleFrameDisplayDuration, true);
            defineAnimation(IDLE_STATE, Direction.RIGHT, idleSheetPath, nativeFrameWidth, nativeFrameHeight, 3, 0, idleFramesPerDirection, idleFrameDisplayDuration, true);
        } else {
            System.err.println("PlayerAnimationManager: Idle sheet path is missing. IDLE animations not defined.");
        }

        // --- WALK Animations ---
        int walkFramesPerDirection = 4; // Example: 6 frames for walking
        if (walkSheetPath != null && !walkSheetPath.isEmpty()) {
            defineAnimation(WALK_STATE, Direction.DOWN,  walkSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 0, walkFramesPerDirection, walkFrameDuration, true);
            defineAnimation(WALK_STATE, Direction.UP,    walkSheetPath, nativeFrameWidth, nativeFrameHeight, 1, 0, walkFramesPerDirection, walkFrameDuration, true);
            defineAnimation(WALK_STATE, Direction.LEFT,  walkSheetPath, nativeFrameWidth, nativeFrameHeight, 2, 0, walkFramesPerDirection, walkFrameDuration, true);
            defineAnimation(WALK_STATE, Direction.RIGHT, walkSheetPath, nativeFrameWidth, nativeFrameHeight, 3, 0, walkFramesPerDirection, walkFrameDuration, true);
        } else {
            System.err.println("PlayerAnimationManager: Walk sheet path is missing. WALK animations not defined.");
        }

        // --- ATTACK Animations ---
        int attackFramesPerDirection = 1; // Example: 5 frames for attack
        // Calculate frame duration to make total animation time roughly match attackVisualDuration
        double effectiveAttackFrameDuration = (attackFramesPerDirection > 0) ? this.attackVisualDuration / attackFramesPerDirection : attackAnimFrameDuration;
        if (attackSheetPath != null && !attackSheetPath.isEmpty()) {
            defineAnimation(ATTACK_STATE, Direction.DOWN,  attackSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 0, attackFramesPerDirection, effectiveAttackFrameDuration, false); // Non-looping
            defineAnimation(ATTACK_STATE, Direction.UP,    attackSheetPath, nativeFrameWidth, nativeFrameHeight, 1, 0, attackFramesPerDirection, effectiveAttackFrameDuration, false);
            defineAnimation(ATTACK_STATE, Direction.LEFT,  attackSheetPath, nativeFrameWidth, nativeFrameHeight, 2, 0, attackFramesPerDirection, effectiveAttackFrameDuration, false);
            defineAnimation(ATTACK_STATE, Direction.RIGHT, attackSheetPath, nativeFrameWidth, nativeFrameHeight, 3, 0, attackFramesPerDirection, effectiveAttackFrameDuration, false);
        } else {
            System.err.println("PlayerAnimationManager: Attack sheet path is missing. ATTACK animations not defined.");
        }

        // Set initial state (important for the base class to pick up the first spritesheet)
        // The base class's defineAnimation already tries to set the state if currentSpriteSheet is null.
        // We can explicitly set it here to ensure a known default if paths were valid.
        if (animations.containsKey(IDLE_STATE)) {
            setVisualState(IDLE_STATE, Direction.DOWN);
        } else if (animations.containsKey(WALK_STATE)) {
            setVisualState(WALK_STATE, Direction.DOWN); // Fallback if IDLE not defined
        } else {
            System.err.println("PlayerAnimationManager: No default (IDLE/WALK) animations could be set. Player might be invisible.");
        }
    }

    @Override
    public void defineAnimation(String stateName, Direction direction, String sheetPath,
                                int frameWidth, int frameHeight, int startFrameX, int startFrameY,
                                int frameCount, double frameDuration, boolean loop) {

        Image sheet = assetManager.loadSpritesheet(sheetPath); // <<< CORRECTED: Load the whole sheet
        if (sheet == null) {
            System.err.println("EnemyAnimationManager: Failed to load spritesheet using assetManager.getImage(): " + sheetPath);
            return;
        }

        Rectangle2D[] frames = new Rectangle2D[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new Rectangle2D(
                    startFrameX * frameWidth, // Calculate pixel X
                    (startFrameY + i) * frameHeight,      // Calculate pixel Y (assuming each direction is a new row)
                    frameWidth, frameHeight);
        }

        AnimationData data = new AnimationData(sheet, frames, frameDuration, loop);
        animations.computeIfAbsent(stateName, k -> new HashMap<>()).put(direction, data);

        // Set the initial state only once, if no animations were set before.
        if (this.currentState == null || this.currentSpriteSheet == null) {
            if (animations.containsKey(IDLE_STATE) && animations.get(IDLE_STATE).containsKey(Direction.DOWN)) {
                setVisualState(IDLE_STATE, Direction.DOWN);
            } else if (!animations.isEmpty() && !animations.values().iterator().next().isEmpty()){
                // Fallback to the first defined animation if IDLE DOWN is not available
                String firstState = animations.keySet().iterator().next();
                Direction firstDir = animations.get(firstState).keySet().iterator().next();
                setVisualState(firstState, firstDir);
            }
        }
    }

    /**
     * Convenience method to set the visual state using the PlayerState enum.
     * @param state The PlayerState enum value (IDLE, WALKING, ATTACKING).
     * @param direction The facing direction.
     */
    public void setVisualState(PlayerState state, Direction direction) {
        String stateName;
        switch (state) {
            case WALKING:   stateName = WALK_STATE; break;
            case ATTACKING: stateName = ATTACK_STATE; break;
            case IDLE:
            default:        stateName = IDLE_STATE; break;
        }
        super.setVisualState(stateName, direction); // Call the base class method
    }

    /**
     * Overridden to handle the ATTACK animation finishing.
     * Switches back to IDLE state by default when attack visual finishes.
     */
    @Override
    protected void onAnimationFinish(String finishedStateName) {
        super.onAnimationFinish(finishedStateName); // Call base class behavior if any

        if (ATTACK_STATE.equals(finishedStateName)) {
            // When attack *visual* finishes, switch visual back to IDLE.
            // The Player class's logical attack timer might still be running or finished.
            // This ensures the visual doesn't get stuck on the last attack frame.
            // The Player.updateLocalAnimationState will correct this further if moving.
            setVisualState(PlayerState.IDLE, this.currentDirection);
        }
    }

    /** Checks if the current animation state is ATTACK. */
    public boolean isVisuallyAttacking() {
        return ATTACK_STATE.equals(currentState);
    }

    /** Gets the current state as a PlayerState enum for easier use. */
    public PlayerState getCurrentPlayerState() {
        if (WALK_STATE.equals(currentState)) return PlayerState.WALKING;
        if (ATTACK_STATE.equals(currentState)) return PlayerState.ATTACKING;
        return PlayerState.IDLE; // Default
    }
}
