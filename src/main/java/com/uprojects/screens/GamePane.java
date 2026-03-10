package com.uprojects.screens;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.uprojects.core.ArreglarCablesTarea;
import com.uprojects.core.Tarea;
import com.uprojects.entities.RemotePlayer;
import com.uprojects.helpers.CollisionChecker;
import com.uprojects.helpers.KeyHandler;
import com.uprojects.entities.Player;
import com.uprojects.server.Red;
import com.uprojects.stages.MapHandler;
import com.uprojects.ui.ArreglarCablesPane;
import com.uprojects.ui.TareaPane;
import com.uprojects.ui.VotacionPane;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GamePane extends Pane {


    // Configuracion de cliente para servidor
    private Client cliente;
    private HashMap<Integer, RemotePlayer> jugadoresRemotos;

    // Config Pantalla
    private final int originalTileSize = 32; // Eje: 16x16 tiles
    private final int scale = 1;


    // World settings
    // Tamano de cuadros
    public final int tileSize = originalTileSize * scale; // 32 * 2 = 64x64 tiles


    // Variables del jugador
    private KeyHandler keyH;
    private List<Tarea> tareasPorHacer;
    private Tarea tareaActual;
    private boolean impostor = false;

    // Canvas JavaFx
    public final Canvas canvas;
    private final GraphicsContext gc;


    // Contador de FPS
    private long lastNanoTime = 0;
    private String fpsDisplay = "FPS: 0";
    private int frameCount = 0;
    private long fpsTimer = 0;

    // UI de la tarea actualmente mostrada
    private final Pane tareaOverlay;

    // Player creation
    private Player localPlayer;
    private int localID;
    private String localPlayerName;
    private String localPlayerColor;

    // Map handler (more like map manager but u get it)
    private MapHandler mapHandler;
    private String mapaActual;

    // Colision Checker
    private CollisionChecker collisionChecker;

    // Lobby
    private VBox lobbyUI;
    private Button btnSalir;
    private Button btnEmpezar;
    private Label lblContador;
    private Label lblLocalIP;

    public GamePane(Scene scene, Client cliente, int localID, boolean isHost, String nombreJugadorLocal, String colorJugadorLocal) {

        this.canvas = new Canvas();
        this.localID = localID;
        this.cliente = cliente;
        this.localPlayerName = nombreJugadorLocal;
        this.localPlayerColor = colorJugadorLocal;

        Group group = new Group(canvas);

        // Permitimos que el canvas reciba inputs del teclado
        this.canvas.setFocusTraversable(true);
        this.canvas.setOnMouseClicked(e -> this.canvas.requestFocus());

        this.gc = this.canvas.getGraphicsContext2D();

        // Anclamos el canvas al tamaño del Pane que a su vez se ajusta a todo el tamaño disponible en el Stage (Frame principal)
        this.canvas.widthProperty().bind(this.widthProperty());
        this.canvas.heightProperty().bind(this.heightProperty());
        this.gc.setImageSmoothing(false); // Para evitar vista pixelada por zoom


        // Tarea Overlay, la creamos de una vez pero la hacemos invisible
        this.tareaOverlay = new Pane();
        this.tareaOverlay.setMouseTransparent(false);
        this.tareaOverlay.setVisible(false);

        this.sceneProperty().addListener((obs, oldS, nuevaScene) -> {
            if (nuevaScene != null) {
                this.keyH = new KeyHandler(scene);

                /*
                nuevaScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (keyH.accionarTarea()) {
                        System.out.println("E Detectada!");
                        if (tareaActual != null && tareaActual.getUiPane() != null) {
                            cerrarTareaActual();
                        } else {
                            abrirTareaActual();
                        }
                    }
                });
                 */
                System.out.println("KeyHandler attached to active scene: " + nuevaScene);
            }
        });


        // Inicializando lista de jugadores
        this.jugadoresRemotos = new HashMap<>();
        this.tareaActual = null;
        this.tareasPorHacer = new ArrayList<>();
        //this.taresPorHacer.add(new ArreglarCablesTarea());


        // 2. Create the Overlay UI
        lobbyUI = new VBox(10);
        lobbyUI.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 20;");
        lobbyUI.setTranslateX(20);
        lobbyUI.setTranslateY(20);

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
            irAMenuPrincipal(); // Logic to close the game window
        });

        lobbyUI.getChildren().addAll(lblLocalIP, lblContador, btnSalir);

        // 3. Add Start button only for Host
        if (isHost) {
            btnEmpezar = new Button("INICIAR PARTIDA");
            btnEmpezar.setDisable(true); // Disabled until 5 players
            btnEmpezar.setOnAction(e -> cliente.sendTCP(new Red.PaquetePedirInicio()));
            lobbyUI.getChildren().add(btnEmpezar);
        }


        this.getChildren().addAll(group, tareaOverlay, lobbyUI);

        // Nos aseguramos de que sea visible el pane antes de iniciar el juego, de lo contrario el ancho y alto calculado seria de 0,0
        this.layoutBoundsProperty().addListener((obs, oldV, newV) -> {
            if (this.localPlayer == null && newV.getWidth() > 0) {
                startGameThread();
            }
        });

    }

    public void startGameThread() {

        Scene currScene = this.getScene();

        if (currScene != null) {
            this.keyH.anclarScene(currScene);
        } else {
            System.out.println("[ADVERTENCIA]: GamePane no tiene scene al comenzar");
        }

        this.localPlayer = new Player(keyH, (int) canvas.getWidth(), (int) canvas.getHeight(), tileSize, localPlayerName, localPlayerColor);
        //this.jugadoresRemotos.put();
        this.mapHandler = new MapHandler();
        this.mapHandler.loadMapFile("lobby.tmx");
        this.mapaActual = "lobby.tmx";
        this.collisionChecker = new CollisionChecker(mapHandler);
        this.tareasPorHacer = mapHandler.calcularPosicionTareas();

        System.out.println("=== DEBUG: Tasks Created ===");
        System.out.println("Total tasks: " + tareasPorHacer.size());
        for (Tarea t : tareasPorHacer) {
            System.out.println("Task: " + t.getNombre() + " at (" + t.getWorldX() + ", " + t.getWorldY() + ")");
        }
        System.out.println("================================");

        inicializarTareaUIs();

        new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (lastNanoTime == 0) {
                    lastNanoTime = now;
                    fpsTimer = now;
                    return;
                }

                // Calculate FPS
                frameCount++;
                // If 1 second has passed (1,000,000,000 nanoseconds)
                if (now - fpsTimer >= 1_000_000_000L) {
                    fpsDisplay = "FPS: " + frameCount;
                    frameCount = 0;
                    fpsTimer = now;
                }

                lastNanoTime = now;

                try {
                    update();
                    renderPane();
                } catch (Exception e) {
                    System.err.println("=== EXCEPTION in game loop ===");
                    e.printStackTrace();
                    System.err.println("================================");
                }
            }
        }.start();
    }


    public void update() {

        // Si la ventana no esta enfocada o esta realizando una tarea, se paraliza al jugador
        if (!canvas.isFocused() || tareaActual != null) {
            keyH.resetPressedKeys();
            //canvas.requestFocus();
        }


        localPlayer.updatePosition(collisionChecker);
        Red.PaqueteActualizarJugador posicionEnviar = new Red.PaqueteActualizarJugador();
        posicionEnviar.idJugador = localID;
        posicionEnviar.x = localPlayer.getWorldX();
        posicionEnviar.y = localPlayer.getWorldY();
        posicionEnviar.accion = localPlayer.getAccion();
        posicionEnviar.facingTowards = localPlayer.getFacingTowards();
        cliente.sendUDP(posicionEnviar);

        for (Tarea tarea : this.tareasPorHacer) {
            tarea.update(localPlayer);
        }

        if (keyH.accionarTarea()) {
            if (tareaActual != null) {
                cerrarTareaActual();
            } else {
                abrirTareaActual();
            }
        }
        for (RemotePlayer jugadorExterno : jugadoresRemotos.values()) {
            jugadorExterno.updatePosition(null);
        }

    }

    public void renderPane() {
        // Limpiamos pantalla
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();


        // Tamaño del canvas (para ajustar el campo visible)
        //int anchoCampo = (int) canvas.getWidth();
        //int altoCampo = (int) canvas.getHeight();

        // Creando zoom para que el personaje no vea todo el mapa
        int zoom = 3;


        gc.translate((canvas.getWidth() / 2), (canvas.getHeight() / 2));
        gc.scale(zoom, zoom);
        gc.translate(-localPlayer.getWorldX(), -localPlayer.getWorldY());


        mapHandler.draw(this.gc, zoom, localPlayer);
        localPlayer.draw(this.gc);

        for (RemotePlayer jugadorExterno : jugadoresRemotos.values()) {
            jugadorExterno.draw(gc, localPlayer);
        }

        for (Tarea tarea : this.tareasPorHacer) {
            tarea.drawInteractionBox(gc);
        }

        // We pass the graphics object to be able to set graphics per player
        //mapHandler.draw(this.gc, zoom);
        //localPlayer.draw(this.gc);
        // Release any system resources use by this graphics context
        gc.restore();


        // Aqui dibujamos componentes de UI (menus, botones, etc)

        // Draw UI Elements in Screen Space (Fixed position)
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText(fpsDisplay, 120, 120); // Draws at top-left

    }

    public void removerJugadorRemoto(Red.PaqueteRemoverJugador datos) {

    }

    public void agregarJugadorRemoto(Red.PaqueteConexion datos) {
        // Si el ID soy yo mismo, no me agrego a la lista de "remotos"
        System.out.println("Creando jugador remoto");
        if (datos.idJugador == this.localID) return;


        System.out.println("Viendo si es duplicado");
        // Evitar duplicados
        if (jugadoresRemotos.containsKey(datos.idJugador)) return;


        Platform.runLater(() -> {
            System.out.println("Creando avatar para: " + datos.nombreJugador);

            // Creamos la entidad visual
            RemotePlayer nuevoRemoto = new RemotePlayer(
                    this.tileSize,
                    datos.nombreJugador,
                    "Amarillo"
            );

            // Lo guardamos en el mapa para que el update() y draw() lo procesen
            nuevoRemoto.setAccion("up");
            nuevoRemoto.setFacingTowards("right");
            jugadoresRemotos.put(datos.idJugador, nuevoRemoto);
        });
    }

    public void conectarCliente(String ip) throws IOException {
        cliente = new Client();
        Red.registrar(cliente.getKryo());
        cliente.start();
        // 5 segundos de timeout
        cliente.connect(5000, ip, Red.TCP_PORT, Red.UDP_PORT);

        cliente.addListener(new Listener() {
            @Override
            public void received(Connection conexion, Object object) {
                if (object instanceof Red.PaqueteActualizarJugador paquete) {
                    if (paquete.idJugador == localID)
                        return;

                    Platform.runLater(() -> {
                        RemotePlayer jugadorExterno = jugadoresRemotos.get(paquete.idJugador);

                        // Si no existe localmente, creamos al nuevo jugador externo
                        if (jugadorExterno == null) {
                            jugadorExterno = new RemotePlayer(tileSize, paquete.nombre, paquete.color);
                            jugadoresRemotos.put(paquete.idJugador, jugadorExterno);
                        }

                        // Actualizamos su posicion para que todos los demas jugadores puedan renderizarlo
                        jugadorExterno.updateFromNetwork(paquete.x, paquete.y, paquete.accion);

                    });
                }

                if (object instanceof Red.PaqueteIniciarJuego paquete) {
                    mapHandler.loadMapFile(paquete.mapa);

                    // Enviamos a todos los jugadores al mismo lugar
                    localPlayer.setWorldPosition(paquete.inicioX, paquete.inicioY);

                    for (RemotePlayer jugadorExt : jugadoresRemotos.values()) {
                        jugadorExt.setTargets(paquete.inicioX, paquete.inicioY);
                    }

                }
            }
        });
    }


    private void inicializarTareaUIs() {
        System.out.println("=== Initializing Task UIs ===");
        System.out.println("Tasks to process: " + tareasPorHacer.size());
        for (Tarea tarea : tareasPorHacer) {
            System.out.println("Processing task: " + tarea.getNombre());
            System.out.println("  -> Task is ArreglarCablesTarea, loading FXML...");
            try {
                System.out.println("  -> Creating FXMLLoader...");

                TareaPane ui = tarea.crearUI();

                ui.setOnCerrarTarea(() -> {

                    System.out.println("GamePane: Cerrar tarea callback!");
                    System.out.println("Cerrar tarea callback triggered!");
                    cerrarTareaActual();
                });

                ui.setOnTareaCompletada(() -> {

                    System.out.println("Tarea completada!");
                    // Autocerrado despues de un segundo luego de completar la tarea
                    javafx.animation.PauseTransition delay =
                            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                    delay.setOnFinished(e -> cerrarTareaActual());
                    delay.play();
                });


                tarea.setUiPane(ui);

                System.out.println("Task UI initialized successfully for: " + tarea.getNombre());


                System.out.println("Total tasks: " + tareasPorHacer.size());
                System.out.println("Task UI loaded: " + (tarea.getUiPane() != null));

            } catch (Exception e) {
                System.err.println("ERROR loading task UI:");
                e.printStackTrace();
            }

        }
    }

    private void abrirTareaActual() {

        for (Tarea tarea : tareasPorHacer) {
            if (!tarea.fueCompletada() && tarea.getJugadorCerca()) {
                System.out.println("Abriendo tarea -> " + tarea.getNombre());

                if (tarea.getNombre().equals("Sala-de-Votacion")) {
                    Red.PaqueteLlamarReunion reunion = new Red.PaqueteLlamarReunion();
                    reunion.idJugadorSolicitante = localID;
                    cliente.sendTCP(reunion);
                    return;
                }

                abrirTarea(tarea);
                break;
            }
        }
    }

    private void abrirTarea(Tarea tarea) {

        System.out.println("=== Opening Task ===");
        System.out.println("Task: " + tarea.getNombre());
        System.out.println("tarea.getUiPane() is null: " + (tarea.getUiPane() == null));

        this.tareaActual = tarea;

        if (tarea.getUiPane() != null) {
            // Centramos el pane de la tarea en la pantalla
            TareaPane uiPane = tarea.getUiPane();
            uiPane.setPrefSize(tareaOverlay.getWidth(), tareaOverlay.getHeight());
            tareaOverlay.getChildren().clear();
            tareaOverlay.getChildren().add(uiPane);
            tareaOverlay.setTranslateX((canvas.getWidth() - 400) / 2);
            tareaOverlay.setTranslateY((canvas.getHeight() - 500) / 2);
            tareaOverlay.setVisible(true);
            tareaOverlay.setDisable(false);

            uiPane.mostrarTarea();
        }
    }


    private void cerrarTareaActual() {
        if (tareaActual != null) {
            if (tareaActual.getUiPane() != null) {
                tareaActual.getUiPane().ocultarTarea();
            }
            tareaOverlay.getChildren().clear();
            tareaOverlay.setVisible(false);
            tareaOverlay.setDisable(true);
            tareaActual = null;
        }
    }

    public void abrirSalaVotacion() {
        Tarea salaVotacion = null;

        for (Tarea tarea : tareasPorHacer) {
            if (tarea.getNombre().equals("Sala-de-Votacion")) {
                salaVotacion = tarea;
                break;
            }
        }

        if (salaVotacion != null && salaVotacion.getUiPane() instanceof VotacionPane votacionPane) {
            HashMap<Integer, String> jugadoresVivos = new HashMap<>();

            /*
            if (!localPlayer.wasKilled())
                jugadoresVivos.put(localID, localPlayer.getNombre());
             */

            for (RemotePlayer remotePlayer : jugadoresRemotos.values()) {
                System.out.println("Considering " + remotePlayer.getNombre() + " for pool of voting");
                if (!remotePlayer.wasKilled()) {
                    System.out.println("Adding " + remotePlayer.getNombre() + " to pool of voting");
                    jugadoresVivos.put(remotePlayer.getID(), remotePlayer.getNombre());
                }
            }

            votacionPane.configurar(jugadoresVivos, localID, this::jugadorSaltaVoto, this::jugadorEjerceVoto);
        }

        abrirTarea(salaVotacion);

    }

    private void jugadorSaltaVoto() {
        Red.PaqueteVoto voto = new Red.PaqueteVoto();
        voto.idVotante = localID;
        voto.idVotado = -1;
        cliente.sendTCP(voto);
    }

    private void jugadorEjerceVoto(int idVotado) {

        Red.PaqueteVoto voto = new Red.PaqueteVoto();
        voto.idVotante = localID;
        voto.idVotado = idVotado;
        cliente.sendTCP(voto);
    }

    public void procesarVotacion(Red.PaqueteResultadoVotacion resultado) {
        cerrarTareaActual();

        if (!resultado.empate && resultado.idExpulsado != -1) {
            if (resultado.idExpulsado == localID) {
                localPlayer.setKilled(true);
                localPlayer.setAccion("killed");
            } else {
                RemotePlayer jugadorRemotoExpulsado = jugadoresRemotos.get(resultado.idExpulsado);

                if (jugadorRemotoExpulsado != null) {
                    jugadorRemotoExpulsado.setKilled(true);
                }
            }
        }

        // Tenemos que calcular esto mejor
        if (!localPlayer.wasKilled()) {
            localPlayer.setWorldPosition(512, 384);
        }

        for (RemotePlayer jugadorRemoto : jugadoresRemotos.values()) {
            jugadorRemoto.setTargets(512, 384);
        }
    }

    public int getTileSize() {
        return this.tileSize;
    }

    public void cambiarAMapaPrincipal(Red.PaqueteIniciarJuego datos) {

        this.impostor = datos.esImpostor;

        if (this.impostor) {
            System.out.println("ERES EL IMPOSTOR");
        }

        // Ocultamos la ventana del lobby
        this.ocultarLobbyUI();

        // Cargamos el nuevo mapa
        this.mapHandler.loadMapFile(datos.mapa);
        this.tareasPorHacer = mapHandler.calcularPosicionTareas();


        inicializarTareaUIs();

        System.out.println("=== DEBUG: Tasks Created ===");
        System.out.println("Total tasks: " + tareasPorHacer.size());
        for (Tarea t : tareasPorHacer) {
            System.out.println("Task: " + t.getNombre() + " at (" + t.getWorldX() + ", " + t.getWorldY() + ")");
        }
        System.out.println("================================");


        // Enviamos a todos los jugadores a la biblioteca (debemos calcular esto mejor, por ahora se queda asi)
        this.localPlayer.setWorldPosition(datos.inicioX, datos.inicioY); // Primero al jugador local

        for (RemotePlayer rp : jugadoresRemotos.values()) {
            rp.setTargets(datos.inicioX, datos.inicioY);
        }
    }

    public void actualizarLobby(Red.PaqueteLobbyInfo status) {
        Platform.runLater(() -> {
            lblContador.setText("Jugadores: " + status.conectados + "/10 (Min. 5)");
            if (btnEmpezar != null) {
                btnEmpezar.setDisable(!status.puedeEmpezar);
            }

            if (!this.mapaActual.equals(status.mapaActual)) {
                this.mapHandler.loadMapFile(status.mapaActual);
                this.mapaActual = status.mapaActual;
            }
        });
    }

    public void actualizarPosicionRemoto(Red.PaqueteActualizarJugador status) {


        RemotePlayer remotePlayer = jugadoresRemotos.get(status.idJugador);


        if (remotePlayer != null) {
            remotePlayer.setTargets(status.x, status.y);
            remotePlayer.setAccion(status.accion);
            remotePlayer.setFacingTowards(status.facingTowards);
        }

        /*
        for (RemotePlayer remotePlayer : jugadoresRemotos.values()) {

            if (status.idJugador == remotePlayer.getID()) {
                // Actualizamos todas sus propiedades
                remotePlayer.setTargets(status.x, status.y);
                remotePlayer.setAccion(status.accion);
                remotePlayer.setFacingTowards(status.facingTowards);

            }
        }

         */
    }

    public void ocultarLobbyUI() {
        Platform.runLater(() -> lobbyUI.setVisible(false));
    }

    private void irAMenuPrincipal() {

    }

    public Player getLocalPlayer() {
        return this.localPlayer;
    }

    public void setLocalID(int id) {
        this.localID = id;
    }
}
