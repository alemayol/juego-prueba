package com.uprojects.screens;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.uprojects.core.PerfilJugador;
import com.uprojects.server.GameServer;
import com.uprojects.server.Red;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;


public class HomeScreen extends ControladorPantalla {


    private Client cliente;
    private GameServer servidorLocal;
    private PerfilJugador perfilLocal;
    private String mapaSeleccionado = "mapa1.tmx"; // Un valor por defecto porsia

    @FXML
    private TextField nombreConfig;
    @FXML
    private ComboBox<String> cmbColorConfig;
    @FXML
    private Button villaAsiaSuperior;
    @FXML
    private Button villaAsiaInferior;


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
    public void initialize() {
        // Verificamos que los elementos existan (ya que el controlador se comparte con otras escenas)
        if (nombreConfig != null && cmbColorConfig != null && perfilLocal != null) {
            System.out.println("Perfil local. Nombre: " + perfilLocal.getNombre());
            nombreConfig.setText(perfilLocal.getNombre());
            cmbColorConfig.setValue(perfilLocal.getColor());
        }


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
            conexion.nombreJugador = perfilLocal.getNombre().isEmpty() ? "Cargando..." : perfilLocal.getNombre() + "_" + cliente.getID();
            conexion.colorJugador = perfilLocal.getColor().isEmpty() ? "Amarillo" : perfilLocal.getColor();
            conexion.idJugador = cliente.getID();

            GamePane lobby = new GamePane(stageManager.scene, cliente, cliente.getID(), true, servidorLocal, conexion.nombreJugador, conexion.colorJugador, stageManager, this.mapaSeleccionado);

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

                // Creando cliente
                cliente = new Client();
                Red.registrar(cliente.getKryo());
                cliente.start();


                // Le pasamos el mapa seleccionado, pero es un placeholder, el servidor le mandará el mapa real cuando comience
                GamePane lobby = new GamePane(stageManager.scene, cliente, 0, false, null, perfilLocal.getNombre(), perfilLocal.getColor(), stageManager, this.mapaSeleccionado);
                configurarRedListeners(lobby);
                // Nos conectamos a la IP que el usuario proporsiono
                cliente.connect(5000, ipHost, Red.TCP_PORT, Red.UDP_PORT);


                Red.PaqueteConexion conexion = new Red.PaqueteConexion();
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
                servidorLocal.detenerServidor();
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

    @FXML
    public void guardarConfiguracion() {
        if (perfilLocal != null) {
            perfilLocal.guardarPerfil(nombreConfig.getText(), cmbColorConfig.getValue());
        }
    }

    @FXML
    public void pantallaConfiguracion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/configuracion.fxml"));
            Parent root = loader.load();

            HomeScreen controller = loader.getController();
            controller.setStageManager(this.stageManager);
            controller.setPerfilLocal(this.perfilLocal);

            this.stageManager.setRoot(root, "Configuración de Cuenta");
        } catch (IOException e) {
            showAlert("Error", "No se pudo cargar la configuración: " + e.getMessage());
        }
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
                    paneActual.actualizarLobby(paquete);
                }

                if (objeto instanceof Red.PaqueteIniciarJuego paquete) {
                    Platform.runLater(() -> {
                        //transicionComienzoJuego(conexion, paquete, paneActual);
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


    //Pantalla Como Jugar
    @FXML
    public void pantallaComoJugar(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/comoJugar.fxml"));
            Parent root = loader.load();
            HomeScreen controller = loader.getController();
            controller.setStageManager(this.stageManager);
            this.stageManager.setRoot(root, "Como jugar");

        } catch (IOException e) {
            showAlert("Ups", "Error cargando la pantalla Como Jugar: " + e.getMessage());
        }
    }


    // Pantalla creditos
    public void pantallaCreditos(ActionEvent event) {
        try {

            // Cambiar el fxml al de creditos
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/creditos.fxml"));
            Parent root = loader.load();
            HomeScreen controller = loader.getController();
            controller.setStageManager(this.stageManager);
            this.stageManager.setRoot(root, "Creditos");


        } catch (IOException e) {
            showAlert("Ups", "Error cargando la pantalla de créditos: " + e.getMessage());
        }
    }

    public void volverAlMenu(ActionEvent event) {
        try {
            //Nuevo FXM para el menu principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/homescreen.fxml"));
            Parent root = loader.load();
            HomeScreen controller = loader.getController();
            controller.setStageManager(this.stageManager);
            stageManager.setRoot(root, "Among Us UNEG");

        } catch (IOException e) {
            showAlert("Ups", "Error regresando al menu: " + e.getMessage());
        }
    }

    public void pantallaSeleccionMapa(ActionEvent event) {
        try {

            // Cambiar el fxml al de creditos
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/seleccionMapa.fxml"));
            Parent root = loader.load();
            HomeScreen controller = loader.getController();
            controller.setStageManager(this.stageManager);
            this.stageManager.setRoot(root, "Seleccion de Mapa");


        } catch (IOException e) {
            showAlert("Ups", "Error cargando la pantalla de seleccion de mapas: " + e.getMessage());
        }
    }


    public void setMapa(ActionEvent event) {


        // No se han inicializado
        if (villaAsiaInferior == null || villaAsiaSuperior == null) {
            System.out.println("No se han inicializado los botones");

            return;
        }

        if (event.getSource() == villaAsiaSuperior) {
            System.out.println("Setteando mapa a mapa1");
            villaAsiaSuperior.requestFocus();
            this.mapaSeleccionado = "mapa1.tmx";
        } else if (event.getSource() == villaAsiaInferior) {
            System.out.println("Setteando mapa a mapa2");
            villaAsiaInferior.requestFocus();
            this.mapaSeleccionado = "mapa2.tmx";
        }
    }

    public void pantallaAcercaDe(ActionEvent event) {
        try {

            // Cambiar el fxml al de Acerca de
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/acercade.fxml"));
            Parent root = loader.load();
            HomeScreen controller = loader.getController();
            controller.setStageManager(this.stageManager);
            this.stageManager.setRoot(root, "Acerca de");

        } catch (IOException e) {
            showAlert("Ups", "Error cargando la pantalla Acerca De: " + e.getMessage());
        }
    }

    public PerfilJugador getPerfilLocal() {
        return this.perfilLocal;
    }

    public void setPerfilLocal(PerfilJugador nuevoPerfil) {
        this.perfilLocal = nuevoPerfil;
    }
}
