package cz.cvut.fel.pjv.gameengine3000.savegame;

import cz.cvut.fel.pjv.gameengine3000.animation.AnimationManager;
import cz.cvut.fel.pjv.gameengine3000.entities.Enemies.Enemy;
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.game.EntityManager;
import cz.cvut.fel.pjv.gameengine3000.game.GameConfig;
import cz.cvut.fel.pjv.gameengine3000.savegame.GameEngineAccess;
import cz.cvut.fel.pjv.gameengine3000.game.MultiplayerRole;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.game.GameStatus;

public class GameStateSerializer {

    private final EntityManager entityManager;
    private final GameStatus gameStatus;
    private final GameEngineAccess gameEngineAccess;
    private final CustomMapLoader mapLoader;


    public GameStateSerializer(EntityManager entityManager, GameStatus gameStatus, GameEngineAccess gameEngineAccess, CustomMapLoader mapLoader) {
        this.entityManager = entityManager;
        this.gameStatus = gameStatus;
        this.gameEngineAccess = gameEngineAccess;
        this.mapLoader = mapLoader;
    }

    public SaveGameData gatherCurrentGameState() {
        if (gameStatus == null || entityManager == null || gameEngineAccess == null) {
            return null;
        }

        SaveGameData saveData = new SaveGameData();

        saveData.gameStatus = new GameStatusData();
        saveData.gameStatus.currentRole = gameStatus.getCurrentRole();
        saveData.gameStatus.localPlayerId = gameStatus.getLocalPlayerId();
        saveData.gameStatus.score = gameStatus.getScore();

        saveData.currentMapPath = gameEngineAccess.getCurrentMapPath();
        saveData.timeSinceLastSpawn = gameEngineAccess.getTimeSinceLastSpawn();

        for (Player p : entityManager.getAllPlayers()) {
            PlayerData pd = new PlayerData();
            pd.networkID = p.getNetworkID();
            pd.x = p.getX();
            pd.y = p.getY();
            pd.health = p.getHealth();
            pd.isLocalPlayer = p.isLocalPlayer();
            pd.lastDirection = p.getLastDirectionString();
            saveData.players.add(pd);
        }

        for (Enemy e : entityManager.getNetworkedEnemies().values()) {
            if (e.isAlive()) {
                EnemyData ed = new EnemyData();
                ed.networkId = e.getNetworkId();
                ed.enemyType = e.getEnemyType();
                ed.x = e.getX();
                ed.y = e.getY();
                ed.health = e.getHealth();
                ed.lastDirection = e.lastDirection.toString();
                saveData.enemies.add(ed);
            }
        }
        return saveData;
    }

    public boolean applyGameState(SaveGameData data, Runnable afterLoadUICallback, Runnable onFailCallback) {
        if (data == null || data.gameStatus == null) {
            if(onFailCallback != null) onFailCallback.run();
            return false;
        }

        gameStatus.setCurrentRole(data.gameStatus.currentRole);
        gameStatus.setLocalPlayerId(data.gameStatus.localPlayerId);
        gameStatus.setScore(data.gameStatus.score);
        gameStatus.setGameIsOver(false);
        gameStatus.setPaused(false);

        gameEngineAccess.setTimeSinceLastSpawn(data.timeSinceLastSpawn);

        entityManager.clearAllEntities();

        for (PlayerData pd : data.players) {
            boolean isHostStyle = (pd.networkID == 0 && (data.gameStatus.currentRole == MultiplayerRole.HOST || data.gameStatus.currentRole == MultiplayerRole.NONE)) ||
                    (pd.networkID != 0 && data.gameStatus.currentRole == MultiplayerRole.CLIENT && pd.networkID == data.gameStatus.localPlayerId);

            entityManager.initializeLocalPlayer(pd.networkID, pd.x, isHostStyle);
            Player p = entityManager.getPlayerById(pd.networkID);
            if (p != null) {
                p.setPosition(pd.x, pd.y);
                p.setHealth(pd.health);
                if (p.isLocalPlayer() && mapLoader != null) {
                    p.setMapLoader(mapLoader);
                }
            }
        }

        if (gameStatus.getCurrentRole() == MultiplayerRole.CLIENT && gameStatus.getLocalPlayerId() != 0) {
            if (entityManager.getPlayerById(0) == null) {
                PlayerData hostData = data.players.stream().filter(playerData -> playerData.networkID == 0).findFirst().orElse(null);
                if(hostData != null) {
                    entityManager.ensureRemotePlayerVisualExists(0, gameEngineAccess.getScreenWidth(), gameStatus.getCurrentRole());
                    Player hostP = entityManager.getPlayerById(0);
                    if(hostP != null) {
                        hostP.setPosition(hostData.x, hostData.y);
                        hostP.setHealth(hostData.health);
                    }
                }
            }
        }

        for (EnemyData ed : data.enemies) {
            Enemy enemy = entityManager.createEnemyFromStateForLoad(ed);
        }

        if(afterLoadUICallback != null) {
            afterLoadUICallback.run();
        }
        return true;
    }
}