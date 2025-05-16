package cz.cvut.fel.pjv.gameengine3000.scenes;

import javafx.scene.layout.Pane;

public interface GameSceneInterface {
    void onEnter(GameSceneManager manager);
    void onExit(GameSceneManager manager);

    void update(double elapsedSeconds);
    void render(Pane rootPane);

    Pane getUIRoot();
}