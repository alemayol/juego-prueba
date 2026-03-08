module com.uprojects.amongus {
    requires javafx.controls;
    requires javafx.swing;
    requires libtiled;
    requires java.desktop;
    requires java.xml.bind;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.esotericsoftware.kryo;
    requires kryonet;

    exports com.uprojects.screens;
    exports com.uprojects.stages;
    exports com.uprojects.entities;
    exports com.uprojects.helpers;
    exports com.uprojects.core;
    exports com.uprojects.ui;
    opens com.uprojects.launcher to javafx.graphics;
    opens com.uprojects.screens to javafx.fxml;
    opens com.uprojects.ui to javafx.fxml;
    opens maps to libtiled, javafx.graphics;

}