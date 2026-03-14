package com.uprojects.screens;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class StageManager {

    private final Stage stage;
    protected Group gRoot;
    protected Scene scene;
    protected String title;
    protected boolean resizable, visibility;
    protected int sizeX, sizeY;

    public StageManager(String t, boolean resize, boolean visible, int sizeX, int sizeY, Stage primaryStage) throws IllegalArgumentException {

        if (sizeX <= 0 || sizeY <= 0)
            throw new IllegalArgumentException("The frame can't have size 0 or less");


        this.stage = primaryStage;
        this.title = t;

        this.resizable = resize;
        this.visibility = visible;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    public StageManager(String titulo, Stage primaryStage) {


        this.stage = primaryStage;
        this.title = titulo;

        this.resizable = true;
        this.gRoot = new Group();
        this.scene = new Scene(gRoot, Color.BLACK);
        this.stage.setResizable(resizable);
        this.stage.setWidth(500);
        this.stage.setHeight(500);

        this.stage.setFullScreenExitHint("Pantalla completa activada. Presiona p para salir");
        this.stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("p"));

        this.stage.setScene(this.scene);


    }

    public StageManager(String t, boolean resize, Stage primaryStage, GamePane game) {


        this.stage = primaryStage;
        this.title = t;
        this.resizable = resize;
        this.gRoot = new Group();

        this.gRoot.getChildren().add(game);

        this.scene = new Scene(gRoot, Color.LIGHTBLUE);
        this.stage.setResizable(resizable);
        this.stage.setWidth(500);
        this.stage.setHeight(500);

        this.stage.setFullScreenExitHint("Pantalla completa activada. Presiona p para salir");
        this.stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("p"));

        this.stage.setTitle("Among Us");
        this.stage.setScene(this.scene);


    }


    public void showWindow() {
        this.stage.show();
    }

    public void switchScene(String fxmlArchivo, String titulo) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlArchivo));
            loader.setController(this);
            loader.getController();
            Parent root = loader.load();


            Object pantalla = loader.getController();

            if (pantalla instanceof ControladorPantalla) {
                ((ControladorPantalla) pantalla).setStageManager(this);
            }


            setRoot(root, titulo);


        } catch (IOException e) {
            System.out.println("Imposible cargar archivo FXML -> " + fxmlArchivo);
            e.printStackTrace();

        }
    }

    public void setRoot(Parent root, String titulo) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root));

        } else {
            stage.getScene().setRoot(root);
        }

        stage.setTitle(titulo);
        stage.centerOnScreen();
        stage.show();
    }

    public Stage getStage() {
        return this.stage;
    }
}
