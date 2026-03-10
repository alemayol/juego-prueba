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
    private Label lblContador;
    private Label lblLocalIP;
    private boolean isHost;

    public LobbyUI(int width, boolean isHost, Client cliente) throws UnknownHostException {
        super(width);
        this.isHost = isHost;

        this.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 20;");
        this.setTranslateX(20);
        this.setTranslateY(20);

        lblContador = new Label("Esperando jugadores (0/10)");
        lblContador.setTextFill(Color.WHITE);


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
            //irAMenuPrincipal(); // Logic to close the game window
        });

        this.getChildren().addAll(lblLocalIP, lblContador, btnSalir);

        // 3. Add Start button only for Host
        if (isHost) {
            btnEmpezar = new Button("INICIAR PARTIDA");
            btnEmpezar.setDisable(true); // Disabled until 5 players
            btnEmpezar.setOnAction(e -> cliente.sendTCP(new Red.PaquetePedirInicio()));
            this.getChildren().add(btnEmpezar);
        }
    }
}
