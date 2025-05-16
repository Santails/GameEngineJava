package cz.cvut.fel.pjv.gameengine3000; // Adjust to your actual package

import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class GameApp extends Application {

    private GameSceneManager gameSceneManager; // Make it a field to access in onCloseRequest

    @Override
    public void start(Stage primaryStage) {
        gameSceneManager = new GameSceneManager(primaryStage);
        gameSceneManager.initialize(); // This now sets up the scene and shows the stage

        // Input events are now set up INSIDE GameSceneManager.initialize()
        // so the setOnKeyPressed, setOnKeyReleased, setOnMouseClicked calls
        // from the previous version of GameApp.start() should be REMOVED if they are still there.

        // OnCloseRequest to handle graceful shutdown
        primaryStage.setOnCloseRequest(event -> { // Now WindowEvent should be recognized
            if (gameSceneManager != null) {
                gameSceneManager.shutdown();
            }
            // You might want to ensure the application truly exits if shutdown doesn't handle it.
            // However, usually, if all non-daemon threads finish (like the JavaFX thread after shutdown),
            // the app will close. Platform.exit() can be a more forceful way if needed.
            // Platform.exit();
        });

        // Note: primaryStage.show() is now called inside gameSceneManager.initialize()
    }

    public static void main(String[] args) {
        launch(args);
    }
}