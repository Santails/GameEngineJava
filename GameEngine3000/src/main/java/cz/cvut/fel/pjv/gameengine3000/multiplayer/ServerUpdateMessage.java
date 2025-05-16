package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServerUpdateMessage implements Serializable {
    private static final long serialVersionUID = 2L;

    public double p0x;
    public double p0y;
    public String p0direction;
    public boolean p0attacking;
    public int p0health;
    public boolean p0isMoving;

    public double p1x;
    public double p1y;
    public String p1direction;
    public boolean p1attacking;
    public int p1health;
    public boolean p1isMoving;

    public List<EnemyState> enemyStates;

    public ServerUpdateMessage() {
        this.enemyStates = new ArrayList<>();
        this.p0direction = "DOWN";
        this.p1direction = "DOWN";
    }
}