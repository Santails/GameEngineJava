package cz.cvut.fel.pjv.gameengine3000.savegame;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cvut.fel.pjv.gameengine3000.game.MultiplayerRole;

public class GameStatusData {
    public MultiplayerRole currentRole;
    public int localPlayerId;
    public int score;

    public GameStatusData() {} // No-arg constructor

    @JsonCreator
    public GameStatusData(
            @JsonProperty("currentRole") MultiplayerRole currentRole,
            @JsonProperty("localPlayerId") int localPlayerId,
            @JsonProperty("score") int score) {
        this.currentRole = currentRole;
        this.localPlayerId = localPlayerId;
        this.score = score;
    }
}