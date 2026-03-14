package com.uprojects.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class AnuncioRolJugador extends VBox {

    public AnuncioRolJugador(boolean esImpostor, double width, double height, Runnable onFinalizado) {
        // Setup container
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 1.0);");
        this.setPrefSize(width, height);

        // Role Label
        Label lblRol = new Label();
        lblRol.setFont(new Font("Arial", 60));
        lblRol.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 10, 0, 0, 0);");

        if (esImpostor) {
            lblRol.setText("ERES UN IMPOSTOR");
            lblRol.setTextFill(Color.RED);
        } else {
            lblRol.setText("ERES UN TRIPULANTE");
            lblRol.setTextFill(Color.CYAN);
        }

        this.getChildren().add(lblRol);

        // Transicion de 5 segundos que luego resumira el juego
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> {
            if (this.getParent() != null) {
                ((javafx.scene.layout.Pane) this.getParent()).getChildren().remove(this);
            }
            onFinalizado.run();
        });
        delay.play();
    }
}
