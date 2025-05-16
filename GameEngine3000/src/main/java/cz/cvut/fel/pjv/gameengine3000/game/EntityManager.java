package cz.cvut.fel.pjv.gameengine3000.game;

import cz.cvut.fel.pjv.gameengine3000.animation.AnimationManager;
import cz.cvut.fel.pjv.gameengine3000.entities.Enemies.Bear;
import cz.cvut.fel.pjv.gameengine3000.entities.Enemies.Enemy;
import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.entities.Projectile;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.multiplayer.*;
import cz.cvut.fel.pjv.gameengine3000.savegame.EnemyData;
import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityManager {

    private final Pane rootPane;
    private final AssetManager assetManager;
    private final CustomMapLoader mapLoader;

    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Enemy> networkedEnemies = new ConcurrentHashMap<>();
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();

    private Player localPlayer = null;
    private MultiplayerRole currentRole = MultiplayerRole.NONE;
    private int nextEnemyNetworkId = 1000;
    private Player globalEnemyTarget = null;

    private final String p1WalkSheet, p1IdleSheet, p1AttackSheet;
    private final String p2WalkSheet, p2IdleSheet, p2AttackSheet;
    private final String slimeWalkSheet, slimeIdleSheet;
    private final String bearWalkSheet, bearIdleSheet, bearAttackSheet;


    public EntityManager(Pane rootPane, AssetManager assetManager, CustomMapLoader mapLoader,
                         String p1Walk, String p1Idle, String p1Attack,
                         String p2Walk, String p2Idle, String p2Attack,
                         String slimeWalk, String slimeIdle,
                         String bearWalk, String bearIdle, String bearAttack) {
        this.rootPane = Objects.requireNonNull(rootPane);
        this.assetManager = Objects.requireNonNull(assetManager);
        this.mapLoader = mapLoader;

        this.p1WalkSheet = Objects.requireNonNull(p1Walk);
        this.p1IdleSheet = Objects.requireNonNull(p1Idle);
        this.p1AttackSheet = Objects.requireNonNull(p1Attack);
        this.p2WalkSheet = Objects.requireNonNull(p2Walk);
        this.p2IdleSheet = Objects.requireNonNull(p2Idle);
        this.p2AttackSheet = Objects.requireNonNull(p2Attack);

        this.slimeWalkSheet = Objects.requireNonNull(slimeWalk);
        this.slimeIdleSheet = Objects.requireNonNull(slimeIdle);
        this.bearWalkSheet = Objects.requireNonNull(bearWalk);
        this.bearIdleSheet = Objects.requireNonNull(bearIdle);
        this.bearAttackSheet = Objects.requireNonNull(bearAttack);
    }

    public void setCurrentRole(MultiplayerRole role) { this.currentRole = role; }
    private boolean isAuthoritative() { return currentRole == MultiplayerRole.HOST || currentRole == MultiplayerRole.NONE; }

    public void initializeLocalPlayer(int networkID, double startX, boolean isHostStyle) {
        if (players.containsKey(networkID)) return;
//        String walk = isHostStyle ? p1WalkSheet : p2WalkSheet;
//        String idle = isHostStyle ? p1IdleSheet : p2IdleSheet;
//        String attack = isHostStyle ? p1AttackSheet : p2AttackSheet;
        String walk = p1WalkSheet;
        String idle = p1IdleSheet;
        String attack = p1AttackSheet;
        double playerDisplayWidth = 48;
        double playerDisplayHeight = 48;
        double startY = findStartY(startX, playerDisplayHeight);

        Player player = new Player(networkID, true, startX, startY, assetManager, mapLoader,
                playerDisplayWidth, playerDisplayHeight, walk, idle, attack);
        this.localPlayer = player;
        addPlayer(player);
    }

    public void ensureRemotePlayerVisualExists(int networkID, double screenWidth, MultiplayerRole localRole) {
        if (players.containsKey(networkID)) return;
        boolean remoteIsHostStyle = (networkID == 0);
        String walk = remoteIsHostStyle ? p1WalkSheet : p1WalkSheet;
        String idle = remoteIsHostStyle ? p1IdleSheet : p1IdleSheet;
        String attack = remoteIsHostStyle ? p1AttackSheet : p1AttackSheet;
        double playerDisplayWidth = 48;
        double playerDisplayHeight = 48;
        double startX = (localRole == MultiplayerRole.HOST) ? screenWidth * 3.0 / 4.0 : screenWidth / 4.0;
        double startY = findStartY(startX, playerDisplayHeight);

        Player player = new Player(networkID, false, startX, startY, assetManager, mapLoader,
                playerDisplayWidth, playerDisplayHeight, walk, idle, attack);
        addPlayer(player);
    }

    private void addPlayer(Player player) {
        players.put(player.getNetworkID(), player);
        safeAddToPane(player.getPlayerImageView());
        System.out.println("EntityManager: Added player " + player.getNetworkID() + " to pane.");
    }

    public void removeRemotePlayer(int networkID) {
        Player removed = players.remove(networkID);
        if (removed != null) {
            safeRemoveFromPane(removed.getPlayerImageView());
            if (globalEnemyTarget == removed) {
                globalEnemyTarget = determineEnemyTarget();
            }
        }
    }

    private double findStartY(double startX, double entityHeight) {
        if (mapLoader != null) {
            double mapHeightPixels = mapLoader.getMapHeightTiles() * mapLoader.getTileHeight();
            double startY = mapHeightPixels / 2.0;
            if (mapLoader.isAreaPassable(startX - 16, startY - entityHeight/2.0 , 32, entityHeight)) return startY;

            for (double y = startY + mapLoader.getTileHeight(); y < mapHeightPixels - entityHeight; y += mapLoader.getTileHeight()) {
                if (mapLoader.isAreaPassable(startX - 16, y - entityHeight/2.0, 32, entityHeight)) return y;
            }
            for (double y = startY - mapLoader.getTileHeight(); y > entityHeight/2.0; y -= mapLoader.getTileHeight()) {
                if (mapLoader.isAreaPassable(startX - 16, y - entityHeight/2.0, 32, entityHeight)) return y;
            }
        }
        return rootPane.getPrefHeight() / 2.0;
    }

    public void spawnHostControlledEnemy(double screenWidth, double screenHeight, Random random) {
        if (!isAuthoritative()) return;

        double spawnX = 0, spawnY = 0; int attempts = 0; boolean found = false;
        double enemyDisplayWidth = 48, enemyDisplayHeight = 48;

        while(attempts++ < 20) {
            spawnX = random.nextDouble() * (screenWidth - enemyDisplayWidth) + enemyDisplayWidth/2.0;
            spawnY = random.nextDouble() * (screenHeight - enemyDisplayHeight) + enemyDisplayHeight/2.0;
            if(mapLoader == null || mapLoader.isAreaPassable(spawnX - enemyDisplayWidth/2.0, spawnY - enemyDisplayHeight/2.0, enemyDisplayWidth, enemyDisplayHeight)) {
                found = true; break;
            }
        }
        if(!found) { return; }

        int id = nextEnemyNetworkId++;
        Enemy enemy;

        enemy = new Bear(id, spawnX, spawnY, enemyDisplayWidth, enemyDisplayHeight, assetManager, mapLoader);
        addNetworkedEnemy(enemy);
    }

    private void addNetworkedEnemy(Enemy enemy) {
        if (enemy == null) return;
        networkedEnemies.put(enemy.getNetworkId(), enemy);
        safeAddToPane(enemy.getEnemyImageView());
    }

    public void updateAllEntities(double elapsedSeconds, double screenWidth, double screenHeight) {
        players.values().forEach(p -> p.update(elapsedSeconds, screenWidth, screenHeight));

        if (isAuthoritative()) {
            if (globalEnemyTarget == null || !globalEnemyTarget.isAlive()) {
                globalEnemyTarget = determineEnemyTarget();
            }

            for (Enemy enemy : networkedEnemies.values()) {
                if (enemy.isAlive()) {
                    enemy.update(elapsedSeconds, globalEnemyTarget);
                    Point2D movementIntent = enemy.calculateMovementIntent(elapsedSeconds, globalEnemyTarget);

                    double intendedDX = movementIntent.getX();
                    double intendedDY = movementIntent.getY();
                    double actualDX = 0;
                    double actualDY = 0;

                    if (intendedDX != 0) {
                        Rectangle2D futureEnemyBoundsX = new Rectangle2D(
                                (enemy.getX() + intendedDX) - enemy.getCollisionBounds().getWidth() / 2.0,
                                enemy.getY() - enemy.getCollisionBounds().getHeight() / 2.0,
                                enemy.getCollisionBounds().getWidth(), enemy.getCollisionBounds().getHeight());
                        boolean collisionWithPlayerX = false;
                        for (Player player : players.values()) {
                            if (player.isAlive() && futureEnemyBoundsX.intersects(player.getCollisionBounds().getMinX(), player.getCollisionBounds().getMinY(), player.getCollisionBounds().getWidth(), player.getCollisionBounds().getHeight() )) {
                                collisionWithPlayerX = true;
                                break;
                            }
                        }
                        if (!collisionWithPlayerX) {
                            actualDX = intendedDX;
                        }
                    }

                    double tempXPosForYCheck = enemy.getX() + actualDX;

                    if (intendedDY != 0) {
                        Rectangle2D futureEnemyBoundsY = new Rectangle2D(
                                tempXPosForYCheck - enemy.getCollisionBounds().getWidth() / 2.0,
                                (enemy.getY() + intendedDY) - enemy.getCollisionBounds().getHeight() / 2.0,
                                enemy.getCollisionBounds().getWidth(), enemy.getCollisionBounds().getHeight());
                        boolean collisionWithPlayerY = false;
                        for (Player player : players.values()) {
                            if (player.isAlive() && futureEnemyBoundsY.intersects(player.getCollisionBounds().getMinX(), player.getCollisionBounds().getMinY(), player.getCollisionBounds().getWidth(), player.getCollisionBounds().getHeight() )) {
                                collisionWithPlayerY = true;
                                break;
                            }
                        }
                        if (!collisionWithPlayerY) {
                            actualDY = intendedDY;
                        }
                    }
                    enemy.applyActualMovement(actualDX, actualDY, elapsedSeconds, screenWidth, screenHeight);
                } else {
                    enemy.update(elapsedSeconds, null);
                }
            }
        } else {
            networkedEnemies.values().forEach(enemy -> {
                enemy.update(elapsedSeconds, null);
            });
        }
        projectiles.forEach(p -> p.update(elapsedSeconds));
    }

    public void applyServerUpdate(ServerUpdateMessage msg, int localPlayerId) {
        Player p0 = players.get(0);
        if(p0 != null) p0.applyNetworkState(msg.p0x, msg.p0y, msg.p0direction, msg.p0attacking, msg.p0health, msg.p0isMoving);

        Player p1 = players.get(1);
        if(p1 != null) p1.applyNetworkState(msg.p1x, msg.p1y, msg.p1direction, msg.p1attacking, msg.p1health, msg.p1isMoving);

        Set<Integer> serverEnemyIds = new HashSet<>();
        if (msg.enemyStates != null) {
            for (EnemyState es : msg.enemyStates) {
                serverEnemyIds.add(es.id);
                updateOrCreateNetworkedEnemyFromNetState(es);
            }
        }
        removeStaleEnemies(serverEnemyIds);
    }

    public void applyClientUpdate(ClientUpdateMessage msg) {
        Player clientP = players.get(msg.playerId);
        if (clientP != null && !clientP.isLocalPlayer()) {
            clientP.goUp = false;
            clientP.goDown = false;
            clientP.goLeft = false;
            clientP.goRight = false;

            if (msg.isMoving) {
                if ("UP".equalsIgnoreCase(msg.direction)) {
                    clientP.goUp = true;
                } else if ("DOWN".equalsIgnoreCase(msg.direction)) {
                    clientP.goDown = true;
                } else if ("LEFT".equalsIgnoreCase(msg.direction)) {
                    clientP.goLeft = true;
                } else if ("RIGHT".equalsIgnoreCase(msg.direction)) {
                    clientP.goRight = true;
                }
            }

            if (msg.attacking) {
                clientP.requestAttack();
            }
        }
    }

    private void updateOrCreateNetworkedEnemyFromNetState(EnemyState es) {
        Enemy enemy = networkedEnemies.get(es.id);
        if (enemy != null) {
            enemy.applyNetworkState(es);
        } else if (es.alive) {
            Enemy newEnemy = createEnemyInstanceFromTypeString(es.type, es.id, es.x, es.y);
            if (newEnemy != null) {
                addNetworkedEnemy(newEnemy);
                newEnemy.applyNetworkState(es);
            }
        }
    }

    public Enemy createEnemyFromStateForLoad(EnemyData ed) {
        if (ed == null) return null;

        Enemy enemy = createEnemyInstanceFromTypeString(ed.enemyType, ed.networkId, ed.x, ed.y);

        if (enemy != null) {
            int healthDifference = enemy.getHealth() - ed.health;
            for(int i=0; i < healthDifference; i++) {
                if(enemy.isAlive()) enemy.takeDamage(1); else break;
            }
            if (ed.health > 0 && !enemy.isAlive()) { // Revive if needed
                enemy.health = ed.health; // Direct set after takeDamage made it 0
                enemy.alive = true;
                enemy.updateAliveVisualState(); // Ensure visual matches
            }

            enemy.lastDirection = AnimationManager.directionFromString(ed.lastDirection);
            addNetworkedEnemy(enemy);
        }
        return enemy;
    }

    private Enemy createEnemyInstanceFromTypeString(String type, int id, double x, double y) {
        double enemyDisplayWidth = 48;
        double enemyDisplayHeight = 48;
        if ("bear".equalsIgnoreCase(type)) {
            return new Bear(id, x, y, enemyDisplayWidth, enemyDisplayHeight, assetManager, mapLoader);
        }
        // else if ("slime".equalsIgnoreCase(type)) {
        //    return new Slime(id, x, y, ...);
        // }
        System.err.println("EntityManager: Unknown enemy type '" + type + "' for instantiation.");
        return null;
    }


    private void removeStaleEnemies(Set<Integer> serverEnemyIds) {
        if (isAuthoritative()) return;
        networkedEnemies.keySet().removeIf(localId -> {
            if (!serverEnemyIds.contains(localId)) {
                Enemy removed = networkedEnemies.get(localId);
                if (removed != null) {
                    safeRemoveFromPane(removed.getEnemyImageView());
                }
                return true;
            }
            return false;
        });
    }

    public void cleanupHostEnemies() {
        if (!isAuthoritative()) return;
        networkedEnemies.entrySet().removeIf(entry -> {
            Enemy enemy = entry.getValue();
            if (!enemy.isAlive()) {
                safeRemoveFromPane(enemy.getEnemyImageView());
                return true;
            }
            return false;
        });
    }

    public void addProjectile(Projectile projectile) {
        if (projectile == null) return;
        projectiles.add(projectile);
        safeAddToPane(projectile.getCircle());
    }
    public void cleanupProjectiles(double screenWidth, double screenHeight) {
        projectiles.removeIf(p -> {
            boolean remove = !p.doesExist() || p.isOutOfBounds(screenWidth, screenHeight);
            if (remove) {
                safeRemoveFromPane(p.getCircle());
            }
            return remove;
        });
    }

    public void clearAllEntities() {
        List<Node> nodesToRemove = new ArrayList<>();
        players.values().forEach(p -> nodesToRemove.add(p.getPlayerImageView()));
        networkedEnemies.values().forEach(e -> nodesToRemove.add(e.getEnemyImageView()));
        projectiles.forEach(p -> nodesToRemove.add(p.getCircle()));

        Platform.runLater(() -> rootPane.getChildren().removeAll(nodesToRemove));

        players.clear();
        localPlayer = null;
        networkedEnemies.clear();
        projectiles.clear();
    }

    public Player determineEnemyTarget() {
        if (localPlayer != null && localPlayer.isAlive()) {
            return localPlayer;
        }
        for (Player p : players.values()) {
            if (p != null && !p.isLocalPlayer() && p.isAlive()) {
                return p;
            }
        }
        return null;
    }
    public void setGlobalEnemyTarget(Player target) { this.globalEnemyTarget = target; }

    private void safeAddToPane(Node node) {
        if (node != null) {
            Platform.runLater(() -> {
                if (!rootPane.getChildren().contains(node)) {
                    rootPane.getChildren().add(node);
                }
            });
        }
    }
    private void safeRemoveFromPane(Node node) {
        if (node != null) {
            Platform.runLater(() -> rootPane.getChildren().remove(node));
        }
    }

    public Player getLocalPlayer() { return localPlayer; }
    public Player getPlayerById(int id) { return players.get(id); }
    public Collection<Player> getAllPlayers() { return players.values(); }
    public Map<Integer, Enemy> getNetworkedEnemies() { return networkedEnemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
}