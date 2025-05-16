package cz.cvut.fel.pjv.gameengine3000.savegame;

public interface GameEngineAccess {
    double getTimeSinceLastSpawn();
    void setTimeSinceLastSpawn(double time);
    String getCurrentMapPath();
    double getScreenWidth(); // Needed by serializer to pass to ensureRemotePlayerVisualExists
}