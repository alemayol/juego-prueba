package com.uprojects.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DuctoPane extends TareaPane {

    private VBox contenedor;
    private Runnable onViajar;

    public DuctoPane() {
        super();
        contenedor = new VBox(20);
        contenedor.setAlignment(Pos.CENTER);
        contenedor.setStyle("-fx-background-color: #2c3e50; -fx-padding: 30; -fx-border-color: #95a5a6; -fx-border-width: 5;");

        Label titulo = new Label("SISTEMA DE VENTILACIÓN");
        titulo.setTextFill(Color.WHITE);
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button btnViajar = new Button("Moverse al siguiente ducto");
        btnViajar.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 16px;");
        btnViajar.setOnAction(e -> {
            if (onViajar != null) onViajar.run();
        });

        Button btnSalir = new Button("Salir del ducto");
        btnSalir.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand;");
        btnSalir.setOnAction(e -> ocultarTarea());

        contenedor.getChildren().addAll(titulo, btnViajar, btnSalir);
        this.getChildren().add(contenedor);
    }

    public void configurar(Runnable onViajar) {
        this.onViajar = onViajar;
    }

    @Override
    public void uiAbrirTarea() {
    }

    @Override
    public void uiCerrarTarea() {
    }

    @Override
    public void actualizarUI() {
    }

}
