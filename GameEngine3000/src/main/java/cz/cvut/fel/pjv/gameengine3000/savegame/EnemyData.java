package cz.cvut.fel.pjv.gameengine3000.savegame;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EnemyData {
    public int networkId;
    public String enemyType;
    public double x, y;
    public int health;
    public String lastDirection;

    public EnemyData() {} // No-arg constructor

    @JsonCreator
    public EnemyData(
            @JsonProperty("networkId") int networkId,
            @JsonProperty("enemyType") String enemyType,
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("health") int health,
            @JsonProperty("lastDirection") String lastDirection) {
        this.networkId = networkId;
        this.enemyType = enemyType;
        this.x = x;
        this.y = y;
        this.health = health;
        this.lastDirection = lastDirection;
    }
}