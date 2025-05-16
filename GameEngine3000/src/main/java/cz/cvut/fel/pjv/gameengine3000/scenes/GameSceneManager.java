package cz.cvut.fel.pjv.gameengine3000.scenes;

import cz.cvut.fel.pjv.gameengine3000.game.GameEngine;
import cz.cvut.fel.pjv.gameengine3000.game.GameStatus;
import cz.cvut.fel.pjv.gameengine3000.game.MultiplayerRole;
import cz.cvut.fel.pjv.gameengine3000.savegame.SaveLoadManager; // Import
import cz.cvut.fel.pjv.gameengine3000.savegame.SaveGameData;   // Import
import cz.cvut.fel.pjv.gameengine3000.ui.MainMenuScene;
import cz.cvut.fel.pjv.gameengine3000.ui.MultiplayerLobbyScene;
import cz.cvut.fel.pjv.gameengine3000.ui.PauseMenuScene;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ChoiceDialog;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GameSceneManager {

    private final Stage primaryStage;
    private final StackPane rootStackPane;
    private final Scene mainJavaFxScene;

    private GameState currentState;
    private GameSceneInterface currentUiScene;
    private final Map<GameState, GameSceneInterface> sceneCache;

    private GameEngine gameEngine;
    private GameStatus gameStatus;
    private AnimationTimer mainLoop;
    private GameState stateBeforePause = null;

    private String connectIp = "localhost";
    private int connectPort = 12345;
    private SaveLoadManager saveLoadManager;


    public GameSceneManager(Stage primaryStage) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "Primary Stage cannot be null.");
        this.rootStackPane = new StackPane();
        this.mainJavaFxScene = new Scene(rootStackPane, 1280, 720);
        this.sceneCache = new HashMap<>();
        this.gameStatus = new GameStatus();
        this.saveLoadManager = new SaveLoadManager();


        sceneCache.put(GameState.MAIN_MENU, new MainMenuScene(this));
        sceneCache.put(GameState.MULTIPLAYER_LOBBY, new MultiplayerLobbyScene(this));
    }

    public void initialize() {
        primaryStage.setScene(mainJavaFxScene);
        primaryStage.setTitle("GameEngine3000 PJV");
        primaryStage.setOnCloseRequest(event -> shutdown());

        mainJavaFxScene.setOnKeyPressed(this::handleKeyPress);
        mainJavaFxScene.setOnKeyReleased(this::handleKeyRelease);
        rootStackPane.setOnMouseClicked(this::handleBackgroundMouseClick);

        mainLoop = new AnimationTimer() {
            private long lastUpdateNanos = 0;
            @Override
            public void handle(long nowNanos) {
                if (lastUpdateNanos == 0) { lastUpdateNanos = nowNanos; return; }
                double elapsedSeconds = (nowNanos - lastUpdateNanos) / 1_000_000_000.0;
                lastUpdateNanos = nowNanos;
                updateGame(Math.min(elapsedSeconds, 0.1));
            }
        };
        mainLoop.start();

        setState(GameState.MAIN_MENU);
        primaryStage.show();
    }

    private void updateGame(double elapsedSeconds) {
        if (gameEngine != null && isGameplayState(currentState) && !gameStatus.isPaused()) {
            gameEngine.tick(elapsedSeconds, rootStackPane.getWidth(), rootStackPane.getHeight());
        }
        if (currentUiScene != null) {
            currentUiScene.update(elapsedSeconds);
        }
    }

    public synchronized void setState(GameState newState) {
        if (currentState == newState && newState != GameState.MAIN_MENU && newState != GameState.MULTIPLAYER_LOBBY) {
            if (newState == GameState.PLAYING_SP || newState == GameState.PLAYING_MP_HOST || newState == GameState.PLAYING_MP_CLIENT) {
                if (gameEngine == null) {  }
                else return;
            } else {
                return;
            }
        }

        GameState previousState = currentState;

        if (currentUiScene != null) {
            currentUiScene.onExit(this);
        }

        currentState = newState;
        currentUiScene = getSceneView(newState);

        if (currentUiScene != null) {
            currentUiScene.onEnter(this);
        }
        updateDisplayedViews();
    }

    private void updateDisplayedViews() {
        Platform.runLater(() -> {
            Pane gamePane = (gameEngine != null) ? gameEngine.getRootPane() : null;
            Pane uiPane = (currentUiScene != null) ? currentUiScene.getUIRoot() : null;

            boolean shouldShowGame = gameEngine != null && (isGameplayState(currentState) || currentState == GameState.PAUSED);

            if (gamePane != null) {
                if (shouldShowGame && !rootStackPane.getChildren().contains(gamePane)) {
                    rootStackPane.getChildren().add(0, gamePane);
                } else if (!shouldShowGame && rootStackPane.getChildren().contains(gamePane)) {
                    rootStackPane.getChildren().remove(gamePane);
                }
                if (shouldShowGame) {
                    gamePane.toBack();
                }
            }

            rootStackPane.getChildren().removeIf(node -> node != gamePane && node != null);

            if (uiPane != null) {
                if(!rootStackPane.getChildren().contains(uiPane)) {
                    rootStackPane.getChildren().add(uiPane);
                }
                uiPane.toFront();
            }
        });
    }

    private boolean ensureGameEngineReady(GameState targetState) {
        MultiplayerRole requiredRole = GameState.getRequiredRole(targetState);
        if (requiredRole == null) {
            stopAndCleanupGameEngine(); return false;
        }

        if (gameEngine == null || gameStatus.getCurrentRole() != requiredRole) {
            stopAndCleanupGameEngine();
            this.gameStatus = new GameStatus();
            this.gameStatus.setCurrentRole(requiredRole);

            try {
                double width = rootStackPane.getWidth() > 0 ? rootStackPane.getWidth() : 1280;
                double height = rootStackPane.getHeight() > 0 ? rootStackPane.getHeight() : 720;
                gameEngine = new GameEngine(width, height, this, this.gameStatus);
            } catch (Exception e) {
                gameEngine = null;
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Failed to initialize game engine: " + e.getMessage()).showAndWait());
                return false;
            }
        }
        return true;
    }

    public void stopAndCleanupGameEngine() {
        if (gameEngine != null) {
            final Pane gamePane = gameEngine.getRootPane();
            gameEngine.stopGameAndCleanUp();
            if (gamePane != null) {
                Platform.runLater(() -> rootStackPane.getChildren().remove(gamePane));
            }
            gameEngine = null;
        }
    }

    private GameSceneInterface getSceneView(GameState state) {
        if (isGameplayState(state) || state == GameState.GAME_OVER) return null;

        return sceneCache.computeIfAbsent(state, s -> {
            switch (s) {
                case MAIN_MENU:         return new MainMenuScene(this);
                case MULTIPLAYER_LOBBY: return new MultiplayerLobbyScene(this);
                case PAUSED:            return new PauseMenuScene(this);
                default:
                    return null;
            }
        });
    }

    public void requestTogglePause() {
        if (isGameplayState(currentState)) {
            stateBeforePause = currentState;
            if (gameEngine != null) gameEngine.pauseGame();
            setState(GameState.PAUSED);
        } else if (currentState == GameState.PAUSED) {
            GameState returnState = (stateBeforePause != null) ? stateBeforePause : GameState.MAIN_MENU;
            setState(returnState);
            if (gameEngine != null) gameEngine.resumeGame();
            stateBeforePause = null;
        }
    }

    public void requestNewGame() {
        stopAndCleanupGameEngine();
        requestStartSinglePlayer();
    }

    public void requestSaveGame() {
        boolean canSave = gameEngine != null &&
                ( (isGameplayState(currentState) && !gameStatus.isPaused()) ||
                        currentState == GameState.PAUSED );

        if (canSave) {
            boolean wasActivelyPlayingAndNotPaused = isGameplayState(currentState) && !gameStatus.isPaused();
            if (wasActivelyPlayingAndNotPaused && gameEngine != null) {
                gameEngine.pauseGame();
            }

            TextInputDialog dialog = new TextInputDialog("saveSlot1");
            dialog.setTitle("Save Game");
            dialog.setHeaderText("Enter a name for your save file:");
            dialog.setContentText("Slot Name:");
            Optional<String> result = dialog.showAndWait();

            result.ifPresent(slotName -> {
                if (!slotName.trim().isEmpty()) {
                    SaveGameData saveData = gameEngine.captureGameStateForSave();
                    if (saveData != null) {
                        if (saveLoadManager.saveGame(saveData, slotName)) {
                            new Alert(Alert.AlertType.INFORMATION, "Game saved successfully as '" + slotName + "'.").showAndWait();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Failed to save game.").showAndWait();
                        }
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Could not gather data to save.").showAndWait();
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING, "Save name cannot be empty.").showAndWait();
                }
            });

            if (wasActivelyPlayingAndNotPaused && gameEngine != null) {
                gameEngine.resumeGame();
            }
        } else {
            new Alert(Alert.AlertType.INFORMATION, "Cannot save game from the current context (e.g., main menu, or no game active).").showAndWait();
        }
    }

    public void requestLoadGame() {
        String[] slots = saveLoadManager.getAvailableSaveSlots();
        if (slots.length == 0) {
            new Alert(Alert.AlertType.INFORMATION, "No save games found.").showAndWait();
            return;
        }

        List<String> choices = Arrays.asList(slots);
        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Load Game");
        dialog.setHeaderText("Select a save slot to load:");
        dialog.setContentText("Available Saves:");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(slotName -> {
            SaveGameData loadedData = saveLoadManager.loadGame(slotName);
            if (loadedData != null) {

                GameState targetState = GameState.PLAYING_SP;
                if(loadedData.gameStatus != null && loadedData.gameStatus.currentRole == MultiplayerRole.HOST) targetState = GameState.PLAYING_MP_HOST;
                else if(loadedData.gameStatus != null && loadedData.gameStatus.currentRole == MultiplayerRole.CLIENT) targetState = GameState.PLAYING_MP_CLIENT;

                if (ensureGameEngineReady(targetState)) {
                    if (gameEngine.restoreGameStateFromLoad(loadedData)) {
                        setState(targetState);
                        new Alert(Alert.AlertType.INFORMATION, "Game '" + slotName + "' loaded.").showAndWait();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Failed to apply loaded game data.").showAndWait();
                        requestReturnToMainMenu();
                    }
                } else {
                    new Alert(Alert.AlertType.ERROR, "Could not prepare game engine for loaded game.").showAndWait();
                    requestReturnToMainMenu();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to load game from slot '" + slotName + "'.").showAndWait();
            }
        });
    }


    public void requestStartSinglePlayer() {
        if (ensureGameEngineReady(GameState.PLAYING_SP)) {
            gameEngine.setupSinglePlayer();
            gameEngine.startGameLoop();
            setState(GameState.PLAYING_SP);
        } else { setState(GameState.MAIN_MENU);  }
    }

    public void requestStartMultiplayerHost() {
        if (ensureGameEngineReady(GameState.PLAYING_MP_HOST)) {
            gameEngine.setupMultiplayerHost(connectPort);
            gameEngine.startGameLoop();
            setState(GameState.PLAYING_MP_HOST);
        } else { setState(GameState.MULTIPLAYER_LOBBY);  }
    }

    public void requestStartMultiplayerClient(String ip) {
        this.connectIp = ip;
        if (ensureGameEngineReady(GameState.PLAYING_MP_CLIENT)) {
            gameEngine.setupMultiplayerClient(connectIp, connectPort);
            gameEngine.startGameLoop();
            setState(GameState.PLAYING_MP_CLIENT);
        } else { setState(GameState.MULTIPLAYER_LOBBY);  }
    }

    public void requestReturnToMainMenu() {
        stopAndCleanupGameEngine();
        setState(GameState.MAIN_MENU);
    }

    public void requestGoToLobby() {
        stopAndCleanupGameEngine();
        setState(GameState.MULTIPLAYER_LOBBY);
    }


    private void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.ESCAPE && (isGameplayState(currentState) || currentState == GameState.PAUSED)) {
            requestTogglePause();
            event.consume();
            return;
        }

        if (isGameplayState(currentState) && gameEngine != null && !gameStatus.isPaused()) {
            gameEngine.handleKeyPress(code);
            event.consume();
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        KeyCode code = event.getCode();
        if ((isGameplayState(currentState) || currentState == GameState.PAUSED) && gameEngine != null) {
            gameEngine.handleKeyRelease(code);
            event.consume();
        }
    }

    private void handleBackgroundMouseClick(MouseEvent event) {
        if (event.getTarget() == rootStackPane && isGameplayState(currentState) && gameEngine != null && !gameStatus.isPaused()) {
            gameEngine.handleMouseClick(event.getSceneX(), event.getSceneY());
            event.consume();
        }
    }

    public void shutdown() {
        if (mainLoop != null) mainLoop.stop();
        stopAndCleanupGameEngine();
    }

    private boolean isGameplayState(GameState state) {
        return state == GameState.PLAYING_SP || state == GameState.PLAYING_MP_HOST || state == GameState.PLAYING_MP_CLIENT;
    }
    public Stage getPrimaryStage() { return primaryStage; }

    public GameStatus getGameStatus() { return gameStatus; }

    public String getConnectIp() { return connectIp; }
    public void setConnectIp(String connectIp) { this.connectIp = connectIp; }
    public int getConnectPort() { return connectPort; }
    public void setConnectPort(int connectPort) { this.connectPort = connectPort; }
}