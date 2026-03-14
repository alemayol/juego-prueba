package com.uprojects.launcher;

import com.uprojects.screens.HomeScreen;
import com.uprojects.screens.StageManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainLoop extends Application {


    @Override
    public void start(Stage stage) {

        System.setProperty("prism.dirtyopts", "false");
        System.setProperty("prism.vsync", "true");
        StageManager controladorPantallas = new StageManager("Among Us", stage);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/homescreen.fxml"));
            Parent root = loader.load();

            HomeScreen home = loader.getController();
            home.setStageManager(controladorPantallas);

            stage.setScene(new Scene(root));
            stage.show();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        launch(args);

    }


}
