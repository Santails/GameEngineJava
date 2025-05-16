package cz.cvut.fel.pjv.gameengine3000.animation; // Or your animation package

import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image; // Ensure this is imported if using assetManager.getImage()
import javafx.scene.image.ImageView;

import java.util.HashMap;

/**
 * Manages animations for generic Enemy entities.
 * Defines common states like "IDLE", "WALK".
 * Can be extended further for enemies with more complex animations (e.g., "ATTACK", "DEATH").
 */
public class EnemyAnimationManager extends AnimationManager {

    // Common enemy state names (constants for consistency)
    public static final String IDLE = "IDLE";
    public static final String WALK = "WALK";
    public static final String ATTACK = "ATTACK"; // Example, if common attack anim
    public static final String DEATH = "DEATH";   // Example, if common death anim

    /**
     * Constructor for EnemyAnimationManager.
     *
     * @param imageView         The enemy's ImageView to animate.
     * @param assetManager      Shared AssetManager for loading spritesheets.
     * @param idleSheetPath     Path to the spritesheet for idle animations.
     * @param walkSheetPath     Path to the spritesheet for walking animations.
     * @param nativeFrameWidth  Width of a single frame on the sheets.
     * @param nativeFrameHeight Height of a single frame on the sheets.
     * @param idleFrameCount    Number of frames in the idle animation sequence for each direction.
     * @param walkFrameCount    Number of frames in the walk animation sequence for each direction.
     * @param idleFrameDuration Duration of each idle frame.
     * @param walkFrameDuration Duration of each walking frame.
     *                          (Add more parameters for attack/death if needed)
     */
    public EnemyAnimationManager(ImageView imageView, AssetManager assetManager,
                                 String idleSheetPath, String walkSheetPath,
                                 int nativeFrameWidth, int nativeFrameHeight,
                                 int idleFrameCount, int walkFrameCount,
                                 double idleFrameDuration, double walkFrameDuration) {
        super(imageView, assetManager);

        // --- Define IDLE Animations ---
        // Assuming 4 directions (Down, Up, Left, Right) correspond to rows 0, 1, 2, 3 on the sheet.
        // Adjust startFrameX and startFrameY based on your actual spritesheet layout.
        // If all directions for one action are on the same sheet:
        if (idleSheetPath != null && !idleSheetPath.isEmpty()) {
            defineAnimation(IDLE, Direction.DOWN, idleSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 1, idleFrameCount, idleFrameDuration, true);
            defineAnimation(IDLE, Direction.UP, idleSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 3, idleFrameCount, idleFrameDuration, true); // Assuming row 1 for UP
            defineAnimation(IDLE, Direction.LEFT, idleSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 4, idleFrameCount, idleFrameDuration, true); // Assuming row 2 for LEFT
            defineAnimation(IDLE, Direction.RIGHT, idleSheetPath, nativeFrameWidth, nativeFrameHeight, 0, 2, idleFrameCount, idleFrameDuration, true); // Assuming row 3 for RIGHT
        } else {
            System.err.println("EnemyAnimationManager: Idle sheet path is null or empty. IDLE animations not defined.");
        }


        // --- Define WALK Animations ---
        if (walkSheetPath != null && !walkSheetPath.isEmpty()) {
            defineAnimation(WALK, Direction.DOWN, walkSheetPath, nativeFrameWidth, nativeFrameHeight, 4, 1, walkFrameCount, walkFrameDuration, true);
            defineAnimation(WALK, Direction.UP, walkSheetPath, nativeFrameWidth, nativeFrameHeight, 4, 3, walkFrameCount, walkFrameDuration, true);
            defineAnimation(WALK, Direction.LEFT, walkSheetPath, nativeFrameWidth, nativeFrameHeight, 4, 4, walkFrameCount, walkFrameDuration, true);
            defineAnimation(WALK, Direction.RIGHT, walkSheetPath, nativeFrameWidth, nativeFrameHeight, 4, 2, walkFrameCount, walkFrameDuration, true);
        } else {
            System.err.println("EnemyAnimationManager: Walk sheet path is null or empty. WALK animations not defined.");
        }

        // Set initial state (e.g., IDLE, facing DOWN)
        // This will only work if at least IDLE animations were defined.
        if (animations.containsKey(IDLE)) {
            setVisualState(IDLE, Direction.DOWN);
        } else if (animations.containsKey(WALK)) { // Fallback if IDLE not defined but WALK is
            setVisualState(WALK, Direction.DOWN);
        } else {
            System.err.println("EnemyAnimationManager: No default animations (IDLE or WALK) could be set.");
            // ImageView might remain blank if no animations are defined and set.
        }
    }

    /**
     * Corrected defineAnimation method to use assetManager.getImage() for the full sheet.
     * This overrides the one in the abstract class if its AssetManager call was incorrect.
     * If the base class's defineAnimation is already correct (uses assetManager.getImage()),
     * then this override is not strictly necessary unless you need to change other parts of its logic.
     */
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
                    (startFrameX + i) * frameWidth, // Calculate pixel X
                    startFrameY * frameHeight,      // Calculate pixel Y (assuming each direction is a new row)
                    frameWidth, frameHeight);
        }

        AnimationData data = new AnimationData(sheet, frames, frameDuration, loop);
        animations.computeIfAbsent(stateName, k -> new HashMap<>()).put(direction, data);

        // Set the initial state only once, if no animations were set before.
        if (this.currentState == null || this.currentSpriteSheet == null) {
            if (animations.containsKey(IDLE) && animations.get(IDLE).containsKey(Direction.DOWN)) {
                setVisualState(IDLE, Direction.DOWN);
            } else if (!animations.isEmpty() && !animations.values().iterator().next().isEmpty()){
                // Fallback to the first defined animation if IDLE DOWN is not available
                String firstState = animations.keySet().iterator().next();
                Direction firstDir = animations.get(firstState).keySet().iterator().next();
                setVisualState(firstState, firstDir);
            }
        }
    }


    /**
     * Override if specific actions need to happen when an enemy animation finishes.
     * For example, after a "DEATH" animation, the enemy might be marked for removal.
     */
    @Override
    protected void onAnimationFinish(String finishedStateName) {
        super.onAnimationFinish(finishedStateName); // Call base class behavior if any

        if (DEATH.equals(finishedStateName)) {
            System.out.println("Enemy " + imageView.getId() + " death animation finished.");
            // Here you could set a flag on the enemy object itself, e.g., enemy.setVisuallyDead(true);
            // The EntityManager would then pick this up for removal.
            // For now, it just stays on the last frame of the death animation.
        } else if (ATTACK.equals(finishedStateName)) {
            // After attack animation, revert to IDLE or WALK based on enemy's AI state
            // This requires the enemy object to tell the animation manager its next logical state.
            // For simplicity, we might just revert to IDLE here.
            setVisualState(IDLE, this.currentDirection);
        }
    }

    // You can add more specific methods if needed, e.g.:
    // public void playAttackAnimation(Direction direction) {
    //     setVisualState(ATTACK, direction);
    // }
    // public void playDeathAnimation(Direction direction) {
    //     setVisualState(DEATH, direction);
    // }
}
