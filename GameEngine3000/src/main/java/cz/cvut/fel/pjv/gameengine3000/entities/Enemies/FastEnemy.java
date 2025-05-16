//package cz.cvut.fel.pjv.gameengine3000.entities.Enemies;
//import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
//
//public class FastEnemy extends Enemy {
//
//    public FastEnemy(double startX, double startY, AssetManager assetManager, String enemyWalkSheetPath, String enemyIdleSheetPath) {
//        // Call the parent constructor with specific values for FastEnemy
//        super(startX, startY, 50, 150, assetManager, enemyWalkSheetPath, enemyIdleSheetPath,32, 32, false); // Lower health (50), higher speed (150)
//
//        // Optional: Load different sprites/animations if FastEnemy looks different
//        // String sheetPath = "/enemies/fast_enemy_sheet.png";
//        // SpriteSheet fastSheet = new SpriteSheet(assetManager, sheetPath, 32, 32);
//        // Image[] fastMoveFrames = { fastSheet.getSprite(0,0), ... };
//        // this.moveAnimation = new Animation(fastMoveFrames, 0.1); // Faster animation?
//        // this.currentAnimation = this.moveAnimation;
//        // if(this.currentAnimation != null) this.imageView.setImage(this.currentAnimation.getCurrentFrame());
//
//        // Ensure display size matches potential new sprite size
//        // this.displayWidth = /* fast sprite width */;
//        // this.displayHeight = /* fast sprite height */;
//        // this.imageView.setFitWidth(this.displayWidth);
//        // this.imageView.setFitHeight(this.displayHeight);
//    }
//
//    // Optional: Override update if movement AI is different
//    // @Override
//    // public void update(double elapsedSeconds, Player player) {
//    //     super.update(elapsedSeconds, player); // Call base movement
//    //     // Add specific FastEnemy logic here? E.g., dodging?
//    // }
//}