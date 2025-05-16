package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.Serializable;

public class EnemyState implements Serializable {
    private static final long serialVersionUID = 3L;

    public final int id; // Network ID
    public final double x;
    public final double y;
    public final boolean alive;
    public final String type;       // e.g., "slime", "goblin"
    public final String direction; // e.g., "UP", "DOWN", "LEFT", "RIGHT"
    public final boolean chasing;   // Is the enemy currently in a chasing state?

    public EnemyState(int id, double x, double y, boolean alive, String type, String direction, boolean chasing) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.alive = alive;
        this.type = type;
        this.direction = direction;
        this.chasing = chasing;
    }
}
