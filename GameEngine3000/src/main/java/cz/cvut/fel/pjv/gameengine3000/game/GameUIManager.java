package cz.cvut.fel.pjv.gameengine3000.game;

import cz.cvut.fel.pjv.gameengine3000.entities.Player;
import cz.cvut.fel.pjv.gameengine3000.map.CustomMapLoader;
import cz.cvut.fel.pjv.gameengine3000.game.GameStatus;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class GameUIManager {
    private final Pane rootPane;
    private final GameStatus gameStatus;

    private Text healthText, scoreText, gameOverText;

    public GameUIManager(Pane rootPane, GameStatus gameStatus, CustomMapLoader mapLoader, double initialWidth, double initialHeight) {
        this.rootPane = rootPane;
        this.gameStatus = gameStatus;
        setupGameUI(initialWidth, initialHeight);
        addGameUIToScene(mapLoader);
    }

    private void setupGameUI(double width, double height) {
        healthText = createText("Health: ---", 20, Color.LIGHTPINK, 10, 25);
        scoreText = createText("Score: 0", 20, Color.YELLOW, 10, 50);
        gameOverText = createText("GAME OVER", 60, Color.RED, 0, 0);
        gameOverText.setVisible(false);
    }

    public void addGameUIToScene(CustomMapLoader mapLoader) {
        if (mapLoader != null && mapLoader.getMapContainerPane() != null) {
            if (!rootPane.getChildren().contains(mapLoader.getMapContainerPane())) {
                rootPane.getChildren().add(0, mapLoader.getMapContainerPane());
            }
        }
        if (!rootPane.getChildren().contains(healthText)) {
            rootPane.getChildren().addAll(healthText, scoreText, gameOverText);
        }
    }

    private Text createText(String content, int size, Color fill, double x, double y) {
        Text t = new Text(content);
        t.setFont(Font.font("Verdana", size));
        t.setFill(fill);
        t.setLayoutX(x); t.setLayoutY(y);
        return t;
    }

    public void updateGameUI(double screenWidth, double screenHeight, Player localPlayer) {
        healthText.setText("Health: " + (localPlayer != null && localPlayer.isAlive() ? localPlayer.getHealth() : "---"));
        scoreText.setText("Score: " + gameStatus.getScore());

        if (gameStatus.isGameIsOver()) {
            if (!gameOverText.isVisible()) {
                showGameOverText(screenWidth, screenHeight);
            }
        } else {
            if (gameOverText.isVisible()) {
                gameOverText.setVisible(false);
            }
        }
    }

    public void showGameOverText(double screenWidth, double screenHeight) {
        if (!gameOverText.isVisible()) {
            centerText(gameOverText, screenWidth, screenHeight);
            gameOverText.setVisible(true);
        }
    }

    private void centerText(Text textNode, double containerWidth, double containerHeight) {
        Platform.runLater(() -> {
            double textWidth = textNode.getLayoutBounds().getWidth();
            textNode.setLayoutX((containerWidth - textWidth) / 2.0);
            textNode.setLayoutY(containerHeight / 2.0 - textNode.getBaselineOffset() / 2.0 - textNode.getLayoutBounds().getHeight()/2.0 + textNode.getBaselineOffset() );

        });
    }
}