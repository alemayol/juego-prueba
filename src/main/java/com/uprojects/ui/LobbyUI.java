package com.uprojects.ui;

import com.esotericsoftware.kryonet.Client;
import com.uprojects.server.Red;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class LobbyUI extends VBox {


    private Button btnSalir;
    private Button btnEmpezar;
    private final Label lblContador;
    private Label lblLocalIP;

    public LobbyUI(boolean isHost, Client cliente, String mapaSeleccionado, Runnable onSalir) throws UnknownHostException {
        super(20);

        this.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 20;");
        this.setTranslateX(20);
        this.setTranslateY(20);

        lblContador = new Label("Esperando jugadores (0/10)");
        lblContador.setTextFill(Color.WHITE);


        // Tratamos de obtener IP
        try {
            String hostIP = isHost ? InetAddress.getLocalHost().getHostAddress() : "";

            lblLocalIP = new Label("Server IP: " + hostIP);
            lblLocalIP.setTextFill(Color.WHITE);
        } catch (UnknownHostException ex) {
            System.out.println("Advertencia: No se pudo obtener la IP local");
        }


        btnSalir = new Button("Salir del Lobby");
        btnSalir.setOnAction(e -> {
            cliente.sendTCP(new Red.PaqueteSalirLobby());
            onSalir.run();
        });

        this.getChildren().addAll(lblLocalIP, lblContador, btnSalir);

        // El boton para comenzar es solo para el anfitrion
        if (isHost) {
            btnEmpezar = new Button("INICIAR PARTIDA");
            btnEmpezar.setDisable(true); // Desactiva hasta que haya 5 jugadores
            btnEmpezar.setOnAction(e -> {
                Red.PaquetePedirInicio nuevoJuego = new Red.PaquetePedirInicio();
                nuevoJuego.mapa = mapaSeleccionado;
                cliente.sendTCP(nuevoJuego);
            });
            this.getChildren().add(btnEmpezar);
        }
    }

    public void actualizarUI(int jugConectados, boolean puedeEmpezar) {
        lblContador.setText("Jugadores: " + jugConectados + "/10 (Min. 5)");

        if (btnEmpezar != null) {
            btnEmpezar.setDisable(!puedeEmpezar);
        }
    }
}
