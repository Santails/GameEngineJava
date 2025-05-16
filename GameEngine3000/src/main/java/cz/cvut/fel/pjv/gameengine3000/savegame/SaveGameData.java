package cz.cvut.fel.pjv.gameengine3000.savegame;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

public class SaveGameData {
    public String currentMapPath;
    public GameStatusData gameStatus;
    public List<PlayerData> players;
    public List<EnemyData> enemies;
    public double timeSinceLastSpawn;

    public SaveGameData() { // Keep the no-arg constructor
        this.players = new ArrayList<>();
        this.enemies = new ArrayList<>();
    }

    // Explicit constructor for Jackson (optional but can help)
    @JsonCreator
    public SaveGameData(
            @JsonProperty("currentMapPath") String currentMapPath,
            @JsonProperty("gameStatus") GameStatusData gameStatus,
            @JsonProperty("players") List<PlayerData> players,
            @JsonProperty("enemies") List<EnemyData> enemies,
            @JsonProperty("timeSinceLastSpawn") double timeSinceLastSpawn) {
        this.currentMapPath = currentMapPath;
        this.gameStatus = gameStatus;
        this.players = players != null ? players : new ArrayList<>();
        this.enemies = enemies != null ? enemies : new ArrayList<>();
        this.timeSinceLastSpawn = timeSinceLastSpawn;
    }
}