package cz.cvut.fel.pjv.gameengine3000.savegame;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerData {
    public int networkID;
    public double x, y;
    public int health;
    public boolean isLocalPlayer;
    public String lastDirection;

    public PlayerData() {} // No-arg constructor

    @JsonCreator
    public PlayerData(
            @JsonProperty("networkID") int networkID,
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("health") int health,
            @JsonProperty("isLocalPlayer") boolean isLocalPlayer,
            @JsonProperty("lastDirection") String lastDirection) {
        this.networkID = networkID;
        this.x = x;
        this.y = y;
        this.health = health;
        this.isLocalPlayer = isLocalPlayer;
        this.lastDirection = lastDirection;
    }
}