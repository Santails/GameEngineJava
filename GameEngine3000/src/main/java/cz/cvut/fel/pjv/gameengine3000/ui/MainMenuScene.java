package cz.cvut.fel.pjv.gameengine3000.ui;

import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneManager;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameState;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneInterface;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class MainMenuScene implements GameSceneInterface {

    private GameSceneManager gsm;
    private VBox rootLayout;

    public MainMenuScene(GameSceneManager gsm ) {
        this.gsm = gsm;

        rootLayout = new VBox(25);
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(50));
        rootLayout.setStyle("-fx-background-color: #2B2B2B;");

        Label titleLabel = new Label("GameEngine3000 Adventure");
        titleLabel.setFont(Font.font("Arial", 36));
        titleLabel.setTextFill(Color.LIGHTGOLDENRODYELLOW);

        Button singlePlayerButton = createMenuButton("New Single Player Game");
        singlePlayerButton.setOnAction(e -> {
            gsm.requestNewGame();
        });

        Button multiplayerButton = createMenuButton("Multiplayer");
        multiplayerButton.setOnAction(e -> {
            gsm.setState(GameState.MULTIPLAYER_LOBBY);
        });

        Button loadGameButton = createMenuButton("Load Game");
        loadGameButton.setOnAction(e -> {
            gsm.requestLoadGame();
        });

        Button exitButton = createMenuButton("Exit Game");
        exitButton.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        rootLayout.getChildren().addAll(titleLabel, singlePlayerButton, multiplayerButton, loadGameButton, exitButton);
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(280);
        button.setPrefHeight(50);
        button.setFont(Font.font("Arial", 18));
        button.setStyle(
                "-fx-background-color: #555555; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #777777; " +
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #6a6a6a; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #8a8a8a; " +
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #555555; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #777777; " +
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5;"
        ));
        return button;
    }

    @Override
    public void onEnter(GameSceneManager manager) {
    }

    @Override
    public void onExit(GameSceneManager manager) {
    }

    @Override
    public void update(double elapsedSeconds) {
    }

    @Override
    public void render(Pane rootPane) {
    }

    @Override
    public Pane getUIRoot() {
        return rootLayout;
    }
}