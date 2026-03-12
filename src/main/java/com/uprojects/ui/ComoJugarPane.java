package com.uprojects.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ComoJugarPane extends VBox {

    public ComoJugarPane (Runnable onVolver) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setStyle("-fx-background-color: #111b24; -fx-padding: 40;");

        Label titulo = new Label("INSTRUCCIONES");
        titulo.setStyle ("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Consolas';");

        VBox instrucciones = new VBox (15);
        instrucciones.setAlignment(Pos.CENTER_LEFT);
        instrucciones.setMaxWidth(500);

        //Instruccion para el movimiento.
        HBox fila1 = new HBox (15);
        Label regla1 = new Label("Movimiento: ");
        regla1.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label descripcion1 = new Label ("Para moverte por el mapa usa W, A, S, D.");
        descripcion1.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        fila1.getChildren().addAll(regla1, descripcion1);

        //Instruccion para interactuar con las tareas
        HBox fila2 = new HBox (15);
        Label regla2 = new Label("Tareas: ");
        regla2.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label descripcion2 = new Label ("Para interactuar con los objetos y tareas del mapa, acercate a ellos y presiona E.");
        descripcion2.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        fila1.getChildren().addAll(regla2, descripcion2);

        //Instruccion para reportar
        HBox fila3 = new HBox (15);
        Label regla3 = new Label("Reportar: ");
        regla3.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label descripcion3 = new Label ("Si encuentras un cuerpo, acercate al boton rojo mas cercano y presionalo");
        descripcion3.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        fila1.getChildren().addAll(regla3, descripcion3);

        //Instruccion para sabotear.
        HBox fila4 = new HBox (15);
        Label regla4 = new Label("Impostor: ");
        regla4.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label descripcion4 = new Label ("Como impostor podras dejar inmoviles al resto de jugadores presionando F," +
                "tambien podras moverte por los ductos presionando E. Tu objetivo sera inmovilizar al resto de jugadores.");
        descripcion4.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        fila1.getChildren().addAll(regla4, descripcion4);

        instrucciones.getChildren().addAll(fila1, regla1, descripcion1, fila2, regla2, descripcion2, fila3, regla3, descripcion3, fila4, regla4, descripcion4);

        HBox botones = new HBox(20);
        botones.setAlignment(Pos.CENTER);

        Button btnVolver = new Button("Volver al Menú");
        btnVolver.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        btnVolver.setOnAction(e -> onVolver.run());

        botones.getChildren().addAll(btnVolver);
        this.getChildren().addAll(titulo, instrucciones, btnVolver);
    }
}
