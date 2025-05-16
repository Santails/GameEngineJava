package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.Serializable;

public class ClientUpdateMessage implements Serializable {
    private static final long serialVersionUID = 4L;

    public final int playerId;
    public final double x;
    public final double y;
    public final String direction;
    public final boolean attacking;
    public final boolean isMoving;

    public ClientUpdateMessage(int playerId, double x, double y, String direction, boolean attacking, boolean isMoving) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.attacking = attacking;
        this.isMoving = isMoving;
    }
}