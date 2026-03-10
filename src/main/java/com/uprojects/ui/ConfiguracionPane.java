package com.uprojects.ui;

import com.uprojects.core.PerfilJugador;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ConfiguracionPane extends VBox {

    public ConfiguracionPane(PerfilJugador perfil, Runnable onVolver) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(25);
        this.setStyle("-fx-background-color: #111b24; -fx-padding: 40;");

        Label titulo = new Label("CUENTA UNEG");
        titulo.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Consolas';");

        HBox filaNombre = new HBox(15);
        filaNombre.setAlignment(Pos.CENTER);
        Label lblNombre = new Label("Nombre de Tripulante:");
        lblNombre.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        TextField txtNombre = new TextField(perfil.getNombre());
        txtNombre.setStyle("-fx-font-size: 16px;");
        filaNombre.getChildren().addAll(lblNombre, txtNombre);

        HBox filaColor = new HBox(15);
        filaColor.setAlignment(Pos.CENTER);
        Label lblColor = new Label("Color de Traje:");
        lblColor.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        ComboBox<String> cmbColor = new ComboBox<>();
        cmbColor.getItems().addAll("Amarillo", "Azul", "AzulClaro", "Gris", "Morado", "Naranja", "Rojo", "Rosado", "Verde", "VerdeOscuro");
        cmbColor.setValue(perfil.getColor());
        cmbColor.setStyle("-fx-font-size: 16px;");
        filaColor.getChildren().addAll(lblColor, cmbColor);

        HBox botones = new HBox(20);
        botones.setAlignment(Pos.CENTER);

        Button btnVolver = new Button("Volver al Menú");
        btnVolver.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        btnVolver.setOnAction(e -> onVolver.run());

        Button btnGuardar = new Button("Guardar Cambios");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        btnGuardar.setOnAction(e -> {
            perfil.guardarPerfil(txtNombre.getText(), cmbColor.getValue());
            onVolver.run();
        });

        botones.getChildren().addAll(btnVolver, btnGuardar);

        this.getChildren().addAll(titulo, filaNombre, filaColor, botones);
    }
}