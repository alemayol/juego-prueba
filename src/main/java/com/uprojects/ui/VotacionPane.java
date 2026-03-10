package com.uprojects.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.HashMap;

public class VotacionPane extends TareaPane {

    /*
    private Runnable onSaltarVoto;
    private java.util.function.Consumer<Integer> onVotar;

     */

    private VBox contenedor;
    private java.util.function.Consumer<Integer> onVotar;
    private Runnable onSkip;

    public VotacionPane() {
        super();
        contenedor = new VBox(15);
        contenedor.setAlignment(Pos.CENTER);
        contenedor.setStyle("-fx-background-color: #2c3e50; -fx-padding: 30; -fx-border-color: #e74c3c; -fx-border-width: 5;");
        this.getChildren().add(contenedor);
    }

    public void configurar(HashMap<Integer, String> jugadores, int localID, Runnable onSkip, java.util.function.Consumer<Integer> onVotar) {
        this.onSkip = onSkip;
        this.onVotar = onVotar;
        contenedor.getChildren().clear();

        Label titulo = new Label("SALA DE VOTACIÓN");
        titulo.setTextFill(Color.WHITE);
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        contenedor.getChildren().add(titulo);

        System.out.println("Jugadores que vamos a considerar");

        for (String nombre : jugadores.values()) {
            System.out.println("Jugador -> " + nombre);
        }

        for (Integer id : jugadores.keySet()) {
            HBox fila = new HBox(15);
            fila.setAlignment(Pos.CENTER);

            System.out.println("ID -> " + id);
            System.out.println("Local Player ID -> " + localID);

            String nombre = jugadores.get(id);
            //if (id == localID) nombre += " (Tú)";
            if (id == localID) continue;

            Label lblNombre = new Label(nombre);
            lblNombre.setTextFill(Color.WHITE);
            lblNombre.setPrefWidth(200);

            Button btnVotar = new Button("Votar");
            btnVotar.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand;");
            btnVotar.setOnAction(e -> emitirVoto(id));

            fila.getChildren().addAll(lblNombre, btnVotar);
            contenedor.getChildren().add(fila);
        }

        Button btnSkip = new Button("Skip Vote");
        btnSkip.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-cursor: hand;");
        btnSkip.setOnAction(e -> {
            this.setDisable(true);
            onSkip.run();
        });

        contenedor.getChildren().add(btnSkip);
    }

    private void emitirVoto(int idJugador) {
        this.setDisable(true);
        onVotar.accept(idJugador);
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
    /*
    public void showSala(HashMap<Integer, String> jugadores, int localID, Runnable onSkip, java.util.function.Consumer<Integer> onVotar) {

        this.onSaltarVoto = onSkip;
        this.onVotar = onVotar;

        this.setStyle("-fx-background-color: #2c3e50; -fx-padding: 20; -fx-border-color: #e74c3c; -fx-border-width: 5;");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(15);
        this.setPrefSize(400, 500);

        Label titulo = new Label("SALA DE VOTACIÓN");
        titulo.setTextFill(Color.WHITE);
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        this.getChildren().add(titulo);

        for (Integer id : jugadores.keySet()) {
            HBox fila = new HBox(10);
            fila.setAlignment(Pos.CENTER);

            String nombre = jugadores.get(id);
            if (id == localID) nombre += " (Tú)";

            Label lblNombre = new Label(nombre);
            lblNombre.setTextFill(Color.WHITE);
            lblNombre.setPrefWidth(200);

            Button btnVotar = new Button("Votar");
            btnVotar.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
            btnVotar.setOnAction(e -> emitirVoto(id));

            fila.getChildren().addAll(lblNombre, btnVotar);
            this.getChildren().add(fila);
        }

        Button btnSaltarVoto = new Button("Saltar Voto");
        btnSaltarVoto.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 16px;");
        btnSaltarVoto.setOnAction(e -> {
            this.setDisable(true);
            onSaltarVoto.run();
        });

        this.getChildren().add(btnSaltarVoto);
    }

    private void emitirVoto(int idJugador) {
        this.setDisable(true);
        onVotar.accept(idJugador);
    }

     */

}
