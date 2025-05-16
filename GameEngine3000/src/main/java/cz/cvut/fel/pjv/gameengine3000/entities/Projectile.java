package cz.cvut.fel.pjv.gameengine3000.entities;

import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Projectile {
    private ImageView projectileImageView;
    private Circle circle;
    private double x, y;
    private double speed  = 350;
    private double velocityX, velocityY;
    private boolean existance = true;

    private final double startX;
    private final double startY;
    private final double maxTravelDistance = 300.0;

    public Projectile(double startX, double startY, double targetX, double targetY) {
        this.x = startX;
        this.y = startY;
        this.startX = startX;
        this.startY = startY;

        double dx = targetX - startX;
        double dy = targetY - startY;

        double magnitude = Math.sqrt(dx * dx + dy * dy);

        if (magnitude > 0) {
            this.velocityX = (dx / magnitude) * speed;
            this.velocityY = (dy / magnitude) * speed;
        } else {
            this.velocityX = 0;
            this.velocityY = -speed;
        }
        circle = new Circle(5, Color.ORANGERED);
        circle.setCenterX(x);
        circle.setCenterY(y);
    }

    public void update(double elapsedSeconds) {
        if(!existance) return;
        x += velocityX * elapsedSeconds;
        y += velocityY * elapsedSeconds;

        circle.setCenterX(x);
        circle.setCenterY(y);

        double distanceTraveled = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        if (distanceTraveled > maxTravelDistance) {
            this.existance = false;
        }
    }

    public Circle getCircle() {
        return circle;
    }

    public boolean doesExist() {
        return existance;
    }
    public void setExistance(boolean existance) {
        this.existance = existance;
    }

    public boolean isOutOfBounds(double screenWidth, double screenHeight) {
        return x < -circle.getRadius() || x > screenWidth + circle.getRadius() || y < -circle.getRadius() || y > screenHeight + circle.getRadius();
    }

    public Bounds getBounds() {
        return circle.getBoundsInParent();
    }

    public ImageView getView() {
        return projectileImageView;
    }
    public double getX() { return x; }
    public double getY() { return y; }
}