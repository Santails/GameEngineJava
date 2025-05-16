module cz.cvut.fel.pjv.gameengine {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;
    requires annotations;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;

    exports cz.cvut.fel.pjv.gameengine3000.savegame to com.fasterxml.jackson.databind;
    exports cz.cvut.fel.pjv.gameengine3000.game to com.fasterxml.jackson.databind;

    opens cz.cvut.fel.pjv.gameengine3000 to javafx.fxml;
    exports cz.cvut.fel.pjv.gameengine3000;
    exports cz.cvut.fel.pjv.gameengine3000.animation;
    opens cz.cvut.fel.pjv.gameengine3000.animation to javafx.fxml;
}