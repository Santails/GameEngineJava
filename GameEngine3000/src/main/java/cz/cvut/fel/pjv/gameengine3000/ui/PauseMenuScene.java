package cz.cvut.fel.pjv.gameengine3000.ui;

import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneManager;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameState;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneInterface;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.effect.DropShadow;


public class PauseMenuScene implements GameSceneInterface {

    private GameSceneManager gsm;
    private VBox rootLayout;
    private Label titleLabel;


    public PauseMenuScene(GameSceneManager gsm ) {
        this.gsm = gsm;

        rootLayout = new VBox(15);
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(40));
        rootLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); " +
                "-fx-background-radius: 10;");
        rootLayout.setMaxSize(350, 400);

        titleLabel = new Label("Paused");
        titleLabel.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);
        DropShadow ds = new DropShadow();
        ds.setOffsetY(3.0f);
        ds.setColor(Color.color(0.1f, 0.1f, 0.1f));
        titleLabel.setEffect(ds);


        Button resumeButton = createPauseButton("Resume Game");
        resumeButton.setOnAction(e -> {
            gsm.requestTogglePause();
        });

        Button saveGameButton = createPauseButton("Save Game");
        saveGameButton.setOnAction(e -> {
            gsm.requestSaveGame();
        });

        Button loadGameButton = createPauseButton("Load Game");
        loadGameButton.setOnAction(e -> {
            gsm.requestLoadGame();
        });


        Button mainMenuButton = createPauseButton("Back to Main Menu");
        mainMenuButton.setOnAction(e -> {
            gsm.stopAndCleanupGameEngine();
            gsm.setState(GameState.MAIN_MENU);
        });

        rootLayout.getChildren().addAll(titleLabel, resumeButton, saveGameButton, loadGameButton, mainMenuButton);
    }

    private Button createPauseButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(220);
        button.setPrefHeight(45);
        button.setFont(Font.font("Arial", 16));
        button.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #388E3C; " +
                        "-fx-border-width: 1px; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #5cb85c; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #4cae4c; " +
                        "-fx-border-width: 1px; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #388E3C; " +
                        "-fx-border-width: 1px; " +
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