package cz.cvut.fel.pjv.gameengine3000.game;

import cz.cvut.fel.pjv.gameengine3000.savegame.GameEngineAccess;
import cz.cvut.fel.pjv.gameengine3000.savegame.GameStateSerializer; // Import
import cz.cvut.fel.pjv.gameengine3000.savegame.SaveGameData;      // Import
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
import cz.cvut.fel.pjv.gameengine3000.multiplayer.*;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneManager;


import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.util.Random;

public class GameEngine implements GameEngineAccess { // Implement interface

    private final Pane rootPane;
    private final AssetManager assetManager;
    private final CustomMapLoader mapLoader;
    private final GameSceneManager gameSceneManager;
    private final EntityManager entityManager;
    private final MultiplayerCoordinator multiplayerCoordinator;

    private final GameStatus gameStatus;
    private final GameUIManager uiManager;
    private final InputHandler inputHandler;
    private final CollisionHandler collisionHandler;
    private final GameStateSerializer gameStateSerializer;

    private double timeSinceLastSpawn = 0.0;
    private final Random random = new Random();

    public GameEngine(double width, double height, GameSceneManager gsm, GameStatus gameStatus) {
        this.gameSceneManager = gsm;
        this.gameStatus = gameStatus;
        this.rootPane = new Pane();
        this.rootPane.setPrefSize(width, height);
        this.rootPane.setStyle("-fx-background-color: #1a1a1a;");
        this.assetManager = new AssetManager();

        CustomMapLoader loadedMap = null;
        try {
            loadedMap = new CustomMapLoader(GameConfig.DEFAULT_MAP_PATH, this.assetManager);
        } catch (Exception e) {
            Platform.runLater(()-> new Alert(Alert.AlertType.ERROR, "Map Load Failed: " + e.getMessage()).show());
        }
        this.mapLoader = loadedMap;

        this.entityManager = new EntityManager(this.rootPane, this.assetManager, this.mapLoader,
                GameConfig.KNIGHT_WALK_SHEET, GameConfig.KNIGHT_IDLE_SHEET, GameConfig.KNIGHT_ATTACK_SHEET,
                GameConfig.MAGE_WALK_SHEET, GameConfig.MAGE_IDLE_SHEET, GameConfig.MAGE_ATTACK_SHEET,
                GameConfig.DEFAULT_ENEMY_WALK_SHEET, GameConfig.DEFAULT_ENEMY_WALK_SHEET,
                GameConfig.BEAR_ENEMY_SHEET, GameConfig.BEAR_ENEMY_SHEET, GameConfig.BEAR_ENEMY_SHEET
        );

        this.multiplayerCoordinator = new MultiplayerCoordinator(this, this.entityManager, this.gameSceneManager, this.gameStatus);

        this.uiManager = new GameUIManager(this.rootPane, this.gameStatus, this.mapLoader, width, height);
        this.inputHandler = new InputHandler(this.entityManager, this.gameStatus, this.multiplayerCoordinator);
        this.collisionHandler = new CollisionHandler(this.entityManager, this.gameStatus, this.rootPane, this::handlePlayerDeathEvent);
        this.gameStateSerializer = new GameStateSerializer(this.entityManager, this.gameStatus, this, this.mapLoader);
    }

    public SaveGameData captureGameStateForSave() {
        return gameStateSerializer.gatherCurrentGameState();
    }

    public boolean restoreGameStateFromLoad(SaveGameData data) {
        stopGameAndCleanUp();

        boolean success = gameStateSerializer.applyGameState(data,
                () -> {
                    if (uiManager != null) {
                        uiManager.updateGameUI(rootPane.getWidth(), rootPane.getHeight(), entityManager.getLocalPlayer());
                    }
                },
                () -> {
                    gameSceneManager.requestReturnToMainMenu();
                }
        );
        return success;
    }


    @Override
    public double getTimeSinceLastSpawn() {
        return this.timeSinceLastSpawn;
    }

    @Override
    public void setTimeSinceLastSpawn(double time) {
        this.timeSinceLastSpawn = time;
    }

    @Override
    public String getCurrentMapPath() {
        return GameConfig.DEFAULT_MAP_PATH;
    }

    @Override
    public double getScreenWidth() {
        return rootPane.getWidth();
    }


    public void setupSinglePlayer() {
        gameStatus.setCurrentRole(MultiplayerRole.NONE);
        multiplayerCoordinator.setupSinglePlayerMode();
    }

    public void setupMultiplayerHost(int port) {
        gameStatus.setCurrentRole(MultiplayerRole.HOST);
        multiplayerCoordinator.startHostSession(port);
    }

    public void setupMultiplayerClient(String ip, int port) {
        gameStatus.setCurrentRole(MultiplayerRole.CLIENT);
        multiplayerCoordinator.startClientSession(ip, port);
    }

    public void onMultiplayerRoleAndIdConfirmed(MultiplayerRole role, int assignedId) {
        gameStatus.setCurrentRole(role);
        gameStatus.setLocalPlayerId(assignedId);
        entityManager.setCurrentRole(role);

        Platform.runLater(() -> {
            if (entityManager.getLocalPlayer() == null) {
                boolean isHostStyle = (assignedId == 0);
                double startX = (role == MultiplayerRole.HOST || role == MultiplayerRole.NONE) ? rootPane.getWidth() / 4.0 : rootPane.getWidth() * 3.0 / 4.0;
                entityManager.initializeLocalPlayer(assignedId, startX, isHostStyle);
            }
        });
    }

    public void ensureRemotePlayerVisualExists(int remotePId) {
        entityManager.ensureRemotePlayerVisualExists(remotePId, rootPane.getWidth(), gameStatus.getCurrentRole());
    }

    public void applyServerUpdate(ServerUpdateMessage msg) {
        if (gameStatus.getCurrentRole() != MultiplayerRole.CLIENT || gameStatus.isGameIsOver()) return;
        entityManager.applyServerUpdate(msg, gameStatus.getLocalPlayerId());
        Player localP = entityManager.getLocalPlayer();
        if(localP != null && !localP.isAlive()) {
            handlePlayerDeathEvent(localP);
        }
    }

    public void applyClientUpdate(ClientUpdateMessage msg) {
        if (gameStatus.getCurrentRole() != MultiplayerRole.HOST || gameStatus.isGameIsOver()) return;
        entityManager.applyClientUpdate(msg);
        Player clientP = entityManager.getPlayerById(msg.playerId);
        if (clientP != null && clientP.isCurrentlyLogicallyAttacking()) {
            collisionHandler.checkPlayerAttackCollisions(clientP);
        }
    }

    public void handleDisconnection() {
        if (!gameStatus.isGameIsOver()) {
            Platform.runLater(()-> new Alert(Alert.AlertType.WARNING, "Disconnected from session.").show());
        }
        triggerGameOver();
    }

    public void prepareSceneForMultiplayer() {

    }

    public void showInfoMessage(String message) {
        Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, message).show());
    }

    public void startGameLoop() {
        gameStatus.reset();
        gameStatus.setPaused(false);
        if(entityManager.getLocalPlayer() != null && mapLoader != null){
            entityManager.getLocalPlayer().setMapLoader(mapLoader);
        }
        uiManager.updateGameUI(rootPane.getWidth(), rootPane.getHeight(), entityManager.getLocalPlayer());
    }

    public void pauseGame() {
        if (!gameStatus.isGameIsOver()) {
            gameStatus.setPaused(true);
            inputHandler.clearLocalPlayerMovementFlags();
        }
    }
    public void resumeGame() {
        if (!gameStatus.isGameIsOver()) {
            gameStatus.setPaused(false);
        }
    }

    public boolean isPaused() {
        return gameStatus.isPaused();
    }

    public void tick(double elapsedSeconds, double screenWidth, double screenHeight) {
        if (gameStatus.isPaused() || gameStatus.isGameIsOver()) return;

        if ((gameStatus.getCurrentRole() == MultiplayerRole.CLIENT || gameStatus.getCurrentRole() == MultiplayerRole.HOST)
                && entityManager.getLocalPlayer() == null) {
            return;
        }
        if (gameStatus.getCurrentRole() == MultiplayerRole.NONE && entityManager.getLocalPlayer() == null && gameStatus.getLocalPlayerId() != -1) {
            return;
        }


        entityManager.updateAllEntities(elapsedSeconds, screenWidth, screenHeight);
        if (multiplayerCoordinator != null) multiplayerCoordinator.sendUpdatesIfNeeded();

        if (multiplayerCoordinator == null || multiplayerCoordinator.isAuthoritative()) {
            handleAuthoritativeLogic(elapsedSeconds, screenWidth, screenHeight);
        }

        entityManager.cleanupProjectiles(screenWidth, screenHeight);
        uiManager.updateGameUI(screenWidth, screenHeight, entityManager.getLocalPlayer());
    }

    private void handleAuthoritativeLogic(double elapsedSeconds, double screenWidth, double screenHeight) {
        timeSinceLastSpawn += elapsedSeconds;
        if (timeSinceLastSpawn >= GameConfig.ENEMY_SPAWN_INTERVAL && entityManager.getNetworkedEnemies().size() < GameConfig.MAX_ENEMIES) {
            entityManager.spawnHostControlledEnemy(screenWidth, screenHeight, random);
            timeSinceLastSpawn = 0.0;
        }

        Player target = entityManager.determineEnemyTarget();
        entityManager.setGlobalEnemyTarget(target);

        collisionHandler.checkAuthoritativeCollisions(screenWidth, screenHeight);
        entityManager.cleanupHostEnemies();
    }

    private void handlePlayerDeathEvent(Player deceasedPlayer) {
        if (gameStatus.isGameIsOver()) return;
        triggerGameOver();
    }

    private void triggerGameOver() {
        if (gameStatus.isGameIsOver()) return;
        gameStatus.setGameIsOver(true);
        gameStatus.setPaused(true);
        inputHandler.clearLocalPlayerMovementFlags();
        uiManager.showGameOverText(rootPane.getWidth(), rootPane.getHeight());
    }

    public void stopGameAndCleanUp() {
        gameStatus.setPaused(true);
        gameStatus.setGameIsOver(true);
        if (multiplayerCoordinator != null) {
            multiplayerCoordinator.stopAndCleanup();
        }
//        if (entityManager != null) {
        entityManager.clearAllEntities();
//    }
    }

    public void handleKeyPress(KeyCode code) { inputHandler.handleKeyPress(code); }
    public void handleKeyRelease(KeyCode code) { inputHandler.handleKeyRelease(code); }
    public void handleMouseClick(double sceneX, double sceneY) { inputHandler.handleMouseClick(sceneX, sceneY); }

    public Pane getRootPane() { return rootPane; }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}