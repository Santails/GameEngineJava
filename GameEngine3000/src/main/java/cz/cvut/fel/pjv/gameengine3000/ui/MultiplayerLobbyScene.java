package cz.cvut.fel.pjv.gameengine3000.ui; // Example UI package

import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneManager;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameState;
import cz.cvut.fel.pjv.gameengine3000.scenes.GameSceneInterface;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MultiplayerLobbyScene implements GameSceneInterface {

    private GameSceneManager gsm;
    private VBox layout;

    private TextField ipAddressField;
    private Label statusLabel;

    public MultiplayerLobbyScene(GameSceneManager gsm /*, AssetManager am */) {
        this.gsm = gsm;
        layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setStyle("-fx-background-color: #333;");

        Label title = new Label("Multiplayer Lobby");
        title.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        Button hostButton = new Button("Host Game");
        hostButton.setOnAction(e -> gsm.requestStartMultiplayerHost());

        ipAddressField = new TextField("localhost");
        ipAddressField.setPromptText("Enter Host IP");
        ipAddressField.setMaxWidth(200);

        Button joinButton = new Button("Join Game");
        joinButton.setOnAction(e -> {
            String ip = ipAddressField.getText().trim();
            if (!ip.isEmpty()) {
                gsm.requestStartMultiplayerClient(ip);
            } else {
                if(statusLabel != null) statusLabel.setText("IP Address cannot be empty.");
            }
        });

        Button singlePlayerButton = new Button("Single Player Mode");
        singlePlayerButton.setOnAction(e -> gsm.requestStartSinglePlayer());

        Button backToMainMenuButton = new Button("Back to Main Menu");
        backToMainMenuButton.setOnAction(e -> gsm.setState(GameState.MAIN_MENU));


        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #ff8888;");

        layout.getChildren().addAll(title, hostButton, ipAddressField, joinButton, singlePlayerButton, backToMainMenuButton, statusLabel);
    }

    @Override
    public void onEnter(GameSceneManager manager) {
        System.out.println("Entering Multiplayer Lobby Scene");
        if(statusLabel != null) statusLabel.setText(""); // Clear status
    }

    @Override
    public void onExit(GameSceneManager manager) {
        System.out.println("Exiting Multiplayer Lobby Scene");
    }

    @Override
    public void update(double elapsedSeconds) {
        // UI animations or logic specific to this lobby scene
    }

    @Override
    public void render(Pane rootPane) {
        // Not directly used if getUIRoot provides the full UI
    }

    @Override
    public Pane getUIRoot() {
        return layout;
    }

    public TextField getIpAddressField() {
        return ipAddressField;
    }

}
