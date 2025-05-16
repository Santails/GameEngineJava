package cz.cvut.fel.pjv.gameengine3000.ui;

import cz.cvut.fel.pjv.gameengine3000.utils.AssetManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Rectangle2D;

public class SpriteButton extends StackPane {

    private ImageView imageView;
    private Text buttonText;
    private Image buttonSheet;

    private Rectangle2D normalViewport;
    private Rectangle2D hoverViewport;
    private Rectangle2D pressedViewport;

    private Runnable onAction;

    public SpriteButton(AssetManager assetManager, String sheetPath,
                        int normalCol, int normalRow,
                        int hoverCol, int hoverRow,
                        int pressedCol, int pressedRow,
                        int frameWidth, int frameHeight, String text) {

        this.buttonSheet = assetManager.loadSpritesheet(sheetPath);

        if (this.buttonSheet == null) {
            System.err.println("SpriteButton: Failed to load sheet: " + sheetPath + ". Button will be invisible.");
            this.imageView = new ImageView(); // Create empty to avoid NPE
        } else {
            this.imageView = new ImageView(this.buttonSheet);
        }

        this.normalViewport = new Rectangle2D(normalCol * frameWidth, normalRow * frameHeight, frameWidth, frameHeight);
        this.hoverViewport = new Rectangle2D(hoverCol * frameWidth, hoverRow * frameHeight, frameWidth, frameHeight);
        this.pressedViewport = new Rectangle2D(pressedCol * frameWidth, pressedRow * frameHeight, frameWidth, frameHeight);

        this.imageView.setViewport(normalViewport);
        this.setPrefSize(frameWidth, frameHeight); // Set StackPane preferred size
        this.imageView.setFitWidth(frameWidth);
        this.imageView.setFitHeight(frameHeight);

        this.buttonText = new Text(text);
        this.buttonText.setFont(Font.font("Arial", 16));
        this.buttonText.setFill(Color.WHITE);

        getChildren().addAll(imageView, buttonText);

        setupMouseEvents();
    }

    private void setupMouseEvents() {
        setOnMouseEntered(event -> {
            if(buttonSheet != null) imageView.setViewport(hoverViewport);
        });
        setOnMouseExited(event -> {
            if(buttonSheet != null) imageView.setViewport(normalViewport);
        });
        setOnMousePressed(event -> {
            if(buttonSheet != null) imageView.setViewport(pressedViewport);
        });
        setOnMouseReleased(event -> {
            if(buttonSheet != null) {
                if (isHover()) {
                    imageView.setViewport(hoverViewport);
                } else {
                    imageView.setViewport(normalViewport);
                }
            }
        });
        setOnMouseClicked(event -> {
            if (onAction != null) {
                onAction.run();
            }
        });
    }

    public void setOnAction(Runnable action) {
        this.onAction = action;
    }

    public void setTextFill(Color color) {
        if (buttonText != null) {
            buttonText.setFill(color);
        }
    }

    public void setFont(Font font) {
        if (buttonText != null) {
            buttonText.setFont(font);
        }
    }
}