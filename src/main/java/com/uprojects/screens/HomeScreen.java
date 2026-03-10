package com.uprojects.screens;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.uprojects.server.GameServer;
import com.uprojects.server.Red;
import com.uprojects.ui.LobbyPane;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Random;


public class HomeScreen extends ControladorPantalla {


    private Client cliente;
    private GameServer servidorLocal;

    public HomeScreen() {
    }

    @FXML
    public void switchScenes(ActionEvent e) {
        System.out.println(e.getTarget());
    }

    @FXML
    public void startLobby(ActionEvent e) {

        String hostIP = "";

        try {

            hostIP = InetAddress.getLocalHost().getHostAddress();

            // Creando servidor local
            servidorLocal = new GameServer();

            // Creando el cliente que enviara los datos al servidor
            cliente = new Client();
            Red.registrar(cliente.getKryo());
            cliente.start();

            // Como esta creando el lobby, es anfitrion
            cliente.connect(5000, "localhost", Red.TCP_PORT, Red.UDP_PORT);

            Red.PaqueteConexion conexion = new Red.PaqueteConexion();
            conexion.nombreJugador = "Jugador_" + new Random().nextInt(100);
            cliente.sendTCP(conexion);

            GamePane lobby = new GamePane(stageManager.scene, cliente, 0, true);
            configurarRedListeners(lobby);
            stageManager.setRoot(lobby, "Lobby de espera");

        } catch (UnknownHostException ex) {
            System.out.println("Advertencia: No se pudo obtener la IP local");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }


        //stageManager.setRoot(new GamePane(stageManager.scene), "Now Playing");
    }

    @FXML
    public void joinLobby(ActionEvent e) {
        // Quick dialog to ask for the IP Address
        TextInputDialog dialog = new TextInputDialog("127.0.0.1");
        dialog.setTitle("Unirse a partida");
        dialog.setHeaderText("Ingresa la IP del anfitrión");
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String ipHost = result.get();

            try {

                // START ONLY THE CLIENT
                cliente = new Client();
                Red.registrar(cliente.getKryo());
                cliente.start();


                GamePane lobby = new GamePane(stageManager.scene, cliente, 0, false);
                configurarRedListeners(lobby);
                // CONNECT TO THE PROVIDED IP
                cliente.connect(5000, ipHost, Red.TCP_PORT, Red.UDP_PORT);

                stageManager.setRoot(lobby, "Lobby");

            } catch (IOException ex) {
                System.err.println("No se pudo conectar a la IP: " + ipHost);
                // Idea: Show an error alert in the UI here
                // STOP AND SHOW ALERT
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Conexión");
                alert.setHeaderText("No se pudo conectar al Lobby");
                alert.setContentText("No se encontró ningún servidor en la IP: " + ipHost + "\n\nVerifica que la IP sea correcta y que el host ya haya creado la sala.");
                alert.showAndWait();

                // Optional: Stop the client thread if it started but failed to connect
                if (cliente != null) cliente.stop();
            }
        }
    }

    @FXML
    public void irAjustes(ActionEvent e) {

    }

    @FXML
    public void salirDelJuego(ActionEvent e) {

    }

    private void configurarRedListeners(GamePane paneActual) {
        cliente.addListener(new Listener() {

            public void connected(Connection conexion) {
                paneActual.setLocalID(conexion.getID());
            }

            public void received(Connection conexion, Object objeto) {

                if (objeto instanceof Red.PaqueteLobbyInfo paquete) {
                    boolean soyAnfitrion = conexion.getID() == 1;
                    paneActual.actualizarLobby(paquete);
                }

                if (objeto instanceof Red.PaqueteIniciarJuego paquete) {
                    Platform.runLater(() -> {
                        paneActual.cambiarAMapaPrincipal(paquete);
                    });
                }

                if (objeto instanceof Red.PaqueteConexion) {
                    System.out.println("RECIBIDA PAQUETE DE CONEXION");
                    paneActual.agregarJugadorRemoto((Red.PaqueteConexion) objeto);
                }

                if (objeto instanceof Red.PaqueteActualizarJugador jugadorExt) {
                    //System.out.println("Recibida la posicion de " + jugadorExt.nombre);
                    paneActual.actualizarPosicionRemoto(jugadorExt);
                }
            }
        });
    }

    private void prepararCliente(String ip, boolean esHost) throws IOException {
        cliente = new Client();
        Red.registrar(cliente.getKryo());

        // 1. START the client thread first
        cliente.start();

        // 2. CREATE the UI pane first (so the listener has something to update)
        // We pass 0 as a placeholder ID; Kryonet will give us the real one after connect
        GamePane gamePane = new GamePane(stageManager.scene, cliente, 0, esHost);

        // 3. ADD LISTENERS BEFORE CONNECTING
        // This ensures we catch the very first packet the server sends
        configurarRedListeners(gamePane);

        // 4. NOW CONNECT
        cliente.connect(5000, ip, Red.TCP_PORT, Red.UDP_PORT);

        // 5. Update the local player's ID now that we are connected
        gamePane.getLocalPlayer().setId(cliente.getID());

        stageManager.setRoot(gamePane, "Lobby");
    }

    private String obtenerIpLocal() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
