package cz.cvut.fel.pjv.gameengine3000.animation;

import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for managing sprite animations for entities.
 * Handles loading spritesheets, defining animation states, updating frames,
 * and applying the correct viewport to an ImageView.
 */
public abstract class AnimationManager {

    // --- Constants & Enum ---
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    // --- Components ---
    protected final ImageView imageView; // The ImageView to update
    protected final AssetManager assetManager;

    // --- Animation Data ---
    // Map<StateName, Map<Direction, AnimationData>>
    protected final Map<String, Map<Direction, AnimationData>> animations = new HashMap<>();
    protected Image currentSpriteSheet = null; // The sheet currently being used

    // --- Current State ---
    protected String currentState = "IDLE"; // Default state name
    protected Direction currentDirection = Direction.DOWN;
    protected int currentFrameIndex = 0;
    protected double frameTimer = 0.0;

    /**
     * Constructor for the base AnimationManager.
     * @param imageView The ImageView to animate.
     * @param assetManager For loading images.
     */
    protected AnimationManager(ImageView imageView, AssetManager assetManager) {
        if (imageView == null || assetManager == null) {
            throw new IllegalArgumentException("ImageView and AssetManager cannot be null.");
        }
        this.imageView = imageView;
        this.assetManager = assetManager;
    }

    /**
     * Defines an animation sequence for a specific state and direction.
     * @param stateName Name of the state (e.g., "IDLE", "WALK").
     * @param direction The direction this animation applies to.
     * @param sheetPath Path to the spritesheet image.
     * @param frameWidth Width of a single frame on the sheet.
     * @param frameHeight Height of a single frame on the sheet.
     * @param startFrameX X-coordinate (in frames, 0-indexed) of the first frame on the sheet.
     * @param startFrameY Y-coordinate (in frames, 0-indexed) of the first frame on the sheet.
     * @param frameCount Number of frames in this sequence.
     * @param frameDuration Duration each frame should be displayed (in seconds).
     * @param loop Should the animation loop?
     */
    public void defineAnimation(String stateName, Direction direction, String sheetPath,
                                   int frameWidth, int frameHeight, int startFrameX, int startFrameY,
                                   int frameCount, double frameDuration, boolean loop) {

        Image sheet = assetManager.getSpriteImage(sheetPath, 0, 0, 16, 16);
        if (sheet == null) {
            System.err.println("AnimationManager: Failed to load spritesheet: " + sheetPath);
            return;
        }

        Rectangle2D[] frames = new Rectangle2D[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new Rectangle2D(
                    (startFrameX + i) * frameWidth, // Calculate pixel X
                    startFrameY * frameHeight,      // Calculate pixel Y
                    frameWidth, frameHeight);
        }

        AnimationData data = new AnimationData(sheet, frames, frameDuration, loop);

        // Store the animation data
        animations.computeIfAbsent(stateName, k -> new HashMap<>()).put(direction, data);

        // Set the initial state if not already set
        if (currentSpriteSheet == null) {
            setVisualState(stateName, direction);
        }
    }

    /**
     * Updates the animation timer and changes the displayed frame if necessary.
     * @param elapsedSeconds Time elapsed since the last update call.
     */
    public void update(double elapsedSeconds) {
        AnimationData currentAnim = getCurrentAnimationData();
        if (currentAnim == null) return; // No animation defined for current state/direction

        frameTimer += elapsedSeconds;

        // Check if it's time to advance the frame
        if (frameTimer >= currentAnim.frameDuration) {
            frameTimer -= currentAnim.frameDuration; // Consume time for one frame

            currentFrameIndex++;
            if (currentFrameIndex >= currentAnim.frames.length) {
                // Reached end of animation
                if (currentAnim.loop) {
                    currentFrameIndex = 0; // Loop back to start
                } else {
                    currentFrameIndex = currentAnim.frames.length - 1; // Stay on last frame
                    // Optionally, notify that non-looping animation finished
                    onAnimationFinish(currentState);
                }
            }
            applyCurrentFrame(); // Update the ImageView
        }
    }

    /**
     * Changes the current animation state and direction.
     * Resets the frame index and timer.
     * @param newStateName The name of the state to switch to.
     * @param newDirection The direction for the new state.
     */
    public void setVisualState(String newStateName, Direction newDirection) {
        // Check if state change is necessary
        if (newStateName.equals(currentState) && newDirection == currentDirection) {
            return;
        }

        AnimationData newAnimData = animations.getOrDefault(newStateName, Map.of()).get(newDirection);
        if (newAnimData == null) {
            System.err.println("AnimationManager: No animation defined for state='" + newStateName + "', direction=" + newDirection);
            // Optionally fall back to a default state/direction
            newAnimData = animations.getOrDefault("IDLE", Map.of()).get(Direction.DOWN);
            if(newAnimData == null) return; // Still no default available
            newStateName = "IDLE";
            newDirection = Direction.DOWN;
        }

        // Apply the change
        this.currentState = newStateName;
        this.currentDirection = newDirection;
        this.currentFrameIndex = 0;
        this.frameTimer = 0.0;
        this.currentSpriteSheet = newAnimData.spriteSheet; // Update the current sheet reference

        applyCurrentFrame(); // Immediately apply the first frame of the new animation
    }

    /** Applies the current frame's viewport to the ImageView. */
    protected void applyCurrentFrame() {
        AnimationData currentAnim = getCurrentAnimationData();
        if (currentAnim == null || currentFrameIndex < 0 || currentFrameIndex >= currentAnim.frames.length) {
            // Ensure imageView shows something reasonable if state is invalid
            imageView.setImage(null); // Or a default placeholder image
            return;
        }

        if (imageView.getImage() != currentAnim.spriteSheet) {
            imageView.setImage(currentAnim.spriteSheet);
        }
        imageView.setViewport(currentAnim.frames[currentFrameIndex]);
    }

    /** Returns the AnimationData for the current state and direction. */
    protected AnimationData getCurrentAnimationData() {
        return animations.getOrDefault(currentState, Map.of()).get(currentDirection);
    }

    /** Returns the current animation state name (e.g., "IDLE"). */
    public String getCurrentStateName() {
        return currentState;
    }

    /** Returns the current facing direction. */
    public Direction getLastFacingDirection() {
        return currentDirection;
    }

    /** Sets the current facing direction without changing the animation state. */
    public void setLastFacingDirection(Direction direction) {
        if (this.currentDirection != direction) {
            this.currentDirection = direction;
            // Re-apply frame in case the new direction uses a different sheet or viewport data
            applyCurrentFrame();
        }
    }

    /** Helper method called when a non-looping animation finishes. Can be overridden. */
    protected void onAnimationFinish(String finishedStateName) {
        // Default implementation does nothing.
        // Subclasses can override this to trigger events or state changes.
        // For example, switch back to IDLE after an ATTACK animation.
    }

    /** Converts a string representation to a Direction enum. */
    public static Direction directionFromString(String dirStr) {
        if (dirStr == null) return Direction.DOWN;
        try {
            return Direction.valueOf(dirStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("AnimationManager: Invalid direction string: " + dirStr + ". Defaulting to DOWN.");
            return Direction.DOWN;
        }
    }

    // --- Inner Class for Animation Data ---
    protected static class AnimationData {
        final Image spriteSheet;
        final Rectangle2D[] frames;
        final double frameDuration;
        final boolean loop;

        AnimationData(Image sheet, Rectangle2D[] frames, double duration, boolean loop) {
            this.spriteSheet = sheet;
            this.frames = frames;
            this.frameDuration = duration;
            this.loop = loop;
        }
    }
}