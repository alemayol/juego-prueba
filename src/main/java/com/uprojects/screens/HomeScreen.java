package com.uprojects.screens;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.uprojects.core.PerfilJugador;
import com.uprojects.server.GameServer;
import com.uprojects.server.Red;
import com.uprojects.ui.ComoJugarPane;
import com.uprojects.ui.ConfiguracionPane;
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
    private PerfilJugador perfilLocal;


    public HomeScreen(StageManager stageManager) {

        // Instanciamos el perfil del jugador
        perfilLocal = new PerfilJugador();
        this.stageManager = stageManager;
    }

    public HomeScreen() {

        // Instanciamos el perfil del jugador
        perfilLocal = new PerfilJugador();
    }

    @FXML
    public void switchScenes(ActionEvent e) {
        System.out.println(e.getTarget());
    }

    @FXML
    public void startLobby(ActionEvent e) {

        limpiarRecursos();
        String hostIP = "";

        try {

            hostIP = obtenerIpLocal();

            // Creando servidor local
            servidorLocal = new GameServer();

            // Creando el cliente que enviara los datos al servidor
            cliente = new Client();
            Red.registrar(cliente.getKryo());
            cliente.start();


            Red.PaqueteConexion conexion = new Red.PaqueteConexion();
            //conexion.nombreJugador = "Jugador_" + new Random().nextInt(100);
            conexion.nombreJugador = perfilLocal.getNombre().isEmpty() ? "Cargando..." : perfilLocal.getNombre();
            conexion.colorJugador = perfilLocal.getColor().isEmpty() ? "Amarillo" : perfilLocal.getColor();
            conexion.idJugador = cliente.getID();

            GamePane lobby = new GamePane(stageManager.scene, cliente, cliente.getID(), true, servidorLocal, conexion.nombreJugador, conexion.colorJugador, stageManager);

            configurarRedListeners(lobby);

            // Como esta creando el lobby, es anfitrion
            cliente.connect(5000, hostIP, Red.TCP_PORT, Red.UDP_PORT);
            cliente.sendTCP(conexion);

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


                GamePane lobby = new GamePane(stageManager.scene, cliente, 0, false, null, perfilLocal.getNombre(), perfilLocal.getColor(), stageManager);
                configurarRedListeners(lobby);
                // Nos conectamos a la IP que el usuario proporsiono
                cliente.connect(5000, ipHost, Red.TCP_PORT, Red.UDP_PORT);


                Red.PaqueteConexion conexion = new Red.PaqueteConexion();
                //conexion.nombreJugador = "Jugador_" + new Random().nextInt(100);
                conexion.nombreJugador = perfilLocal.getNombre().isEmpty() ? "Cargando..." : perfilLocal.getNombre();
                conexion.colorJugador = perfilLocal.getColor().isEmpty() ? "Amarillo" : perfilLocal.getColor();
                conexion.idJugador = cliente.getID();

                cliente.sendTCP(conexion);

                stageManager.setRoot(lobby, "Lobby");

            } catch (IOException ex) {
                // Enseñamos la alerta al usurario (se puede mejorar la UI... un dia de estos)
                showAlert("Error", "No se pudo conectar a la IP " + ipHost);


                // Detenemos el cliente y servidor porsia habia iniciado
                limpiarRecursos();
            }
        }
    }

    // Metodo para abrir la cuenta del jugador
    @FXML
    public void cuentaJugador(ActionEvent event) {

        ConfiguracionPane configPane = new ConfiguracionPane(perfilLocal, () -> {
            try {
                // Ruta corregida a /styles/homescreen.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/homescreen.fxml"));
                Parent root = loader.load();
                HomeScreen controller = loader.getController();
                controller.setStageManager(this.stageManager);
                stageManager.setRoot(root, "Among Us UNEG");
            } catch (Exception ex) {
                System.out.println("Error al volver al menu principal: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        stageManager.setRoot(configPane, "Configuración de Cuenta");
    }

    public void cerrarAplicacion() {

        System.out.println("Cerrando aplicación...");

        // De nuevo limpiamos los recursos que quizas quedaron abiertos
        if (cliente != null) {
            try {
                System.out.println("Deteniendo cliente de red...");
                cliente.stop();    // Esto deteniene el hilo
                cliente.close();   // Y esto cierra el socket
                System.out.println("Cliente detenido correctamente");
            } catch (Exception ex) {
                System.err.println("Error al detener cliente: " + ex.getMessage());
            }
            cliente = null;
        }

        if (servidorLocal != null) {
            try {
                System.out.println("Deteniendo servidor local...");
                servidorLocal.detenerServidor(); // Your existing method
                System.out.println("Servidor detenido correctamente");
            } catch (Exception ex) {
                System.err.println("Error al detener servidor: " + ex.getMessage());
            }
            servidorLocal = null;
        }

        // Cerramos JavaFx
        if (stageManager != null && stageManager.getStage() != null) {
            Stage stage = stageManager.getStage();

            // Platform.runLater ejecuta la funcion callback (Runnable aqui) en el hilo de JavaFx, asi evitamos un bloqueo por hilo ocupado
            Platform.runLater(() -> {
                try {
                    stage.close();
                    System.out.println("Ventana cerrada");
                } catch (Exception ex) {
                    System.err.println("Error al cerrar ventana: " + ex.getMessage());
                }
            });
        }

        // Esto es para cerrar la JVM
        // Platform.exit() llama a Application.stop() y asi cerramos
        Platform.exit();

        // Porsia el JVM no quiere cerrar. Por ahora no lo he necesitado
        // System.exit(0);

        System.out.println("Aplicación cerrada exitosamente");
    }

    @FXML
    public void salirDelJuego(ActionEvent e) {
        cerrarAplicacion();
    }

    private void configurarRedListeners(GamePane paneActual) {
        cliente.addListener(new Listener() {

            public void connected(Connection conexion) {
                System.out.println("Setting the localID to -> " + conexion.getID());
                paneActual.setLocalID(conexion.getID());
            }

            public void disconnected(Connection conexion) {

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

                if (objeto instanceof Red.PaqueteActualizarTareasRestantes paquete) {
                    paneActual.actualizarTareasRestantes(paquete);
                }

                if (objeto instanceof Red.PaqueteRespuestaKill electrocucion) {
                    paneActual.manejarElectrocucion(electrocucion);
                }

                if (objeto instanceof Red.PaqueteRemoverJugador paquete) {
                    paneActual.removerJugadorRemoto(paquete);
                }

                if (objeto instanceof Red.PaqueteConexion paquete) {
                    System.out.println("RECIBIDA PAQUETE DE CONEXION");

                    if (paquete.idJugador == cliente.getID()) {
                        Platform.runLater(() -> {
                            paneActual.getLocalPlayer().setColor(paquete.colorJugador); // You need to create this method in Player/Entidad
                            paneActual.getLocalPlayer().cargarSprites(paquete.colorJugador);
                            paneActual.getLocalPlayer().setOculto(false);
                        });
                    } else {

                        paneActual.agregarJugadorRemoto((Red.PaqueteConexion) objeto);
                    }

                }

                if (objeto instanceof Red.PaqueteActualizarJugador jugadorExt) {
                    //System.out.println("Recibida la posicion de " + jugadorExt.nombre);
                    paneActual.actualizarPosicionRemoto(jugadorExt);
                }

                if (objeto instanceof Red.PaqueteLlamarReunion) {
                    Platform.runLater(paneActual::abrirSalaVotacion);
                }

                if (objeto instanceof Red.PaqueteResultadoVotacion result) {
                    Platform.runLater(() -> paneActual.procesarVotacion(result));
                }

                if (objeto instanceof Red.PaqueteFinJuego finJuego) {
                    Platform.runLater(() -> {
                        paneActual.mostrarPantallaFinJuego(finJuego);
                    });
                }

            }
        });
    }

    // Limpiamos recursos del cliente y servidor para permitir crear otro lobby al finalizar una partida
    private void limpiarRecursos() {

        if (cliente != null) {
            try {
                cliente.stop();
                cliente.close();
            } catch (Exception e) {
                System.err.println("Error limpiando cliente: " + e.getMessage());
            }
            cliente = null;
        }

        // Cerrar servidor en caso de que fuera anfitrion
        if (servidorLocal != null) {
            try {
                servidorLocal.detenerServidor();
            } catch (Exception e) {
                System.err.println("Error limpiando server: " + e.getMessage());
            }
            servidorLocal = null;
        }

        // Demora para evitar una condicion de carrera entre que el jugador hacer click y la liberacion de los puertos
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void setStageManager(StageManager manager) {
        super.setStageManager(manager);
        this.stageManager.getStage().setOnCloseRequest(e -> {
            cerrarAplicacion();
        });
    }

    private String obtenerIpLocal() throws UnknownHostException {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void PantallaComoJugar(ActionEvent actionEvent) {

        ComoJugarPane guiaComoJugar = new ComoJugarPane(()->{
            try{
                //Nuevo FXM para el menu principal
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/homescreen.fxml"));
                Parent root = loader.load();
                HomeScreen controller = loader.getController();
                controller.setStageManager(this.stageManager);
                stageManager.setRoot(root, "Among Us UNEG");
            }catch (Exception ex){
                System.out.println("Error al volver al menu: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        stageManager.setRoot(guiaComoJugar, "Como Jugar");
    }
}
