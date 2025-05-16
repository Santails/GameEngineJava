package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import cz.cvut.fel.pjv.gameengine3000.entities.Enemies.Enemy;
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.game.GameEngine;
import cz.cvut.fel.pjv.gameengine3000.game.EntityManager;
import cz.cvut.fel.pjv.gameengine3000.game.MultiplayerRole;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneManager;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameState;
import cz.cvut.fel.pjv.gameengine3000.game.GameStatus;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.util.Map;

public class MultiplayerCoordinator {

    private final GameEngine gameEngine;
    private final EntityManager entityManager;
    private final GameSceneManager gameSceneManager;
    private final GameStatus gameStatus;

    private NetworkHandler networkHandler;
    private boolean waitingForIdFromServer = false;

    private static final long NETWORK_UPDATE_INTERVAL_MS = 50;
    private long lastNetworkSendTime = 0;

    public MultiplayerCoordinator(GameEngine gameEngine, EntityManager entityManager, GameSceneManager gsm, GameStatus gameStatus) {
        this.gameEngine = gameEngine;
        this.entityManager = entityManager;
        this.gameSceneManager = gsm;
        this.gameStatus = gameStatus;
    }

    public MultiplayerRole getCurrentRole() { return gameStatus.getCurrentRole(); }
    public int getLocalPlayerId() { return gameStatus.getLocalPlayerId(); }
    public boolean isAuthoritative() {
        MultiplayerRole role = gameStatus.getCurrentRole();
        return role == MultiplayerRole.HOST || role == MultiplayerRole.NONE;
    }
    public boolean isConnectionPending() { return waitingForIdFromServer; }

    public void setupSinglePlayerMode() {
        cleanupNetworkState();
        gameStatus.setCurrentRole(MultiplayerRole.NONE);
        gameStatus.setLocalPlayerId(0);
        this.waitingForIdFromServer = false;
        gameEngine.onMultiplayerRoleAndIdConfirmed(gameStatus.getCurrentRole(), gameStatus.getLocalPlayerId());
    }

    public void startHostSession(int port) {
        cleanupNetworkState();
        gameStatus.setCurrentRole(MultiplayerRole.HOST);
        gameStatus.setLocalPlayerId(0);
        this.waitingForIdFromServer = false;
        try {
            networkHandler = new ServerNetworkHandler(this, port);
            networkHandler.start();
            gameEngine.onMultiplayerRoleAndIdConfirmed(gameStatus.getCurrentRole(), gameStatus.getLocalPlayerId());
        } catch (IOException e) {
            handleNetworkError("Could not start server: " + e.getMessage());
        }
    }

    public void startClientSession(String ipAddress, int port) {
        cleanupNetworkState();
        gameStatus.setCurrentRole(MultiplayerRole.CLIENT);
        gameStatus.setLocalPlayerId(-1);
        this.waitingForIdFromServer = true;
        gameEngine.prepareSceneForMultiplayer();
        try {
            networkHandler = new ClientNetworkHandler(this, ipAddress, port);
            networkHandler.start();
        } catch (IOException e) {
            handleNetworkError("Could not connect: " + e.getMessage());
        }
    }

    public void stopAndCleanup() {
        if (networkHandler != null) {
            networkHandler.stop();
        }
        cleanupNetworkState();
    }

    private void cleanupNetworkState() {
        networkHandler = null;
        if(gameStatus != null) {
            gameStatus.setCurrentRole(MultiplayerRole.NONE);
            gameStatus.setLocalPlayerId(-1);
        }
        waitingForIdFromServer = false;
        lastNetworkSendTime = 0;
    }

    public void sendUpdatesIfNeeded() {
        MultiplayerRole currentActualRole = gameStatus.getCurrentRole();
        if (currentActualRole == MultiplayerRole.NONE || networkHandler == null || !networkHandler.isConnected()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNetworkSendTime <= NETWORK_UPDATE_INTERVAL_MS) {
            return;
        }
        lastNetworkSendTime = currentTime;

        Player localP = entityManager.getLocalPlayer();
        if (localP == null || !localP.isAlive()) return;

        if (currentActualRole == MultiplayerRole.CLIENT && networkHandler instanceof ClientNetworkHandler) {
            ClientUpdateMessage cum = new ClientUpdateMessage(
                    gameStatus.getLocalPlayerId(),
                    localP.getX(), localP.getY(),
                    localP.getLastDirectionString(),
                    localP.isCurrentlyLogicallyAttacking(),
                    localP.isMoving()
            );
            networkHandler.sendClientUpdate(cum);
        }
        else if (currentActualRole == MultiplayerRole.HOST && networkHandler instanceof ServerNetworkHandler) {
            ServerUpdateMessage sum = new ServerUpdateMessage();

            sum.p0x = localP.getX(); sum.p0y = localP.getY();
            sum.p0direction = localP.getLastDirectionString();
            sum.p0attacking = localP.isCurrentlyLogicallyAttacking();
            sum.p0health = localP.getHealth();
            sum.p0isMoving = localP.isMoving();

            Player remoteP = entityManager.getPlayerById(1);
            if (remoteP != null && remoteP.isAlive()) {
                sum.p1x = remoteP.getX(); sum.p1y = remoteP.getY();
                sum.p1direction = remoteP.getLastDirectionString();
                sum.p1attacking = remoteP.isCurrentlyLogicallyAttacking();
                sum.p1health = remoteP.getHealth();
                sum.p1isMoving = remoteP.isMoving();
            } else {
                sum.p1x = -1000; sum.p1y = -1000; sum.p1direction = "DOWN";
                sum.p1attacking = false; sum.p1health = 0; sum.p1isMoving = false;
            }

            sum.enemyStates.clear();
            for (Map.Entry<Integer, Enemy> entry : entityManager.getNetworkedEnemies().entrySet()) {
                Enemy e = entry.getValue();
                if (e.isAlive()) {
                    boolean isChasing = false;
                    if(e.getEnemyType().equalsIgnoreCase("bear")){
                        // This is a simplification. Ideally Enemy would have a common isChasing()
                        // or current AI state getter. Here we might need a cast or a more generic way.
                        // For now, assuming you can get this info if needed, otherwise pass false.
                        // isChasing = ((cz.cvut.fel.pjv.gameengine3000.entities.Enemies.Bear)e).isCurrentlyChasing();
                    }

                    sum.enemyStates.add(new EnemyState(
                            e.getNetworkId(), e.getX(), e.getY(), e.isAlive(),
                            e.getEnemyType(), e.lastDirection.toString(), isChasing
                    ));
                }
            }
            networkHandler.sendServerUpdate(sum);
        }
    }

    public void onAssignPlayerIdReceived(AssignPlayerIdMessage msg) {
        if (gameStatus.getCurrentRole() == MultiplayerRole.CLIENT && waitingForIdFromServer) {
            gameStatus.setLocalPlayerId(msg.assignedPlayerId);
            this.waitingForIdFromServer = false;
            gameEngine.onMultiplayerRoleAndIdConfirmed(gameStatus.getCurrentRole(), gameStatus.getLocalPlayerId());
            if (gameStatus.getLocalPlayerId() != 0) {
                gameEngine.ensureRemotePlayerVisualExists(1);
            }
        }
    }

    public void onClientConnected(int newClientId) {
        if (gameStatus.getCurrentRole() == MultiplayerRole.HOST && networkHandler instanceof ServerNetworkHandler) {
            ((ServerNetworkHandler) networkHandler).sendPlayerIdAssignment(newClientId, newClientId);
            gameEngine.ensureRemotePlayerVisualExists(newClientId);
        }
    }

    public void onClientDisconnected(int clientId) {
        if (gameStatus.getCurrentRole() == MultiplayerRole.HOST) {
            entityManager.removeRemotePlayer(clientId);
            gameEngine.showInfoMessage("Player " + clientId + " disconnected.");
        }
    }

    public void onServerUpdateReceived(ServerUpdateMessage msg) {
        if (gameStatus.getCurrentRole() == MultiplayerRole.CLIENT) {
            gameEngine.applyServerUpdate(msg);
        }
    }

    public void onClientUpdateReceived(ClientUpdateMessage msg) {
        if (gameStatus.getCurrentRole() == MultiplayerRole.HOST) {
            gameEngine.applyClientUpdate(msg);
        }
    }

    public void onNetworkDisconnected() {
        gameEngine.handleDisconnection();
        cleanupNetworkState();
    }

    private void handleNetworkError(String errorMessage) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, errorMessage).showAndWait());
        cleanupNetworkState();
        if (gameSceneManager != null) {
            MultiplayerRole currentActualRole = gameStatus.getCurrentRole();
            if(currentActualRole == MultiplayerRole.HOST || currentActualRole == MultiplayerRole.CLIENT) {
                gameSceneManager.setState(GameState.MULTIPLAYER_LOBBY);
            } else {
                gameSceneManager.setState(GameState.MAIN_MENU);
            }
        }
    }
}