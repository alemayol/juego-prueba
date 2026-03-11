package com.uprojects.screens;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.uprojects.core.ArreglarCablesTarea;
import com.uprojects.core.SalaVotacion;
import com.uprojects.core.Tarea;
import com.uprojects.entities.RemotePlayer;
import com.uprojects.helpers.CollisionChecker;
import com.uprojects.helpers.KeyHandler;
import com.uprojects.entities.Player;
import com.uprojects.server.GameServer;
import com.uprojects.server.Red;
import com.uprojects.stages.MapHandler;
import com.uprojects.ui.ArreglarCablesPane;
import com.uprojects.ui.TareaPane;
import com.uprojects.ui.VotacionPane;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
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
    private GameServer servidorLocal;
    private Client cliente;
    private HashMap<Integer, RemotePlayer> jugadoresRemotos;
    private int tareasRestantes;
    private boolean esHost;
    private StageManager stageManager;

    // Config Pantalla
    private final int originalTileSize = 32; // Eje: 16x16 tiles
    private final int scale = 1;

    // ProgressBar
    private int totalTareasGlobales;
    private javafx.scene.control.ProgressBar barraProgresoTareas;


    // World settings
    // Tamano de cuadros
    public final int tileSize = originalTileSize * scale; // 32 * 2 = 64x64 tiles


    // Variables del jugador
    private KeyHandler keyH;
    private List<Tarea> tareasPorHacer;
    private Tarea tareaActual;
    private boolean impostor = false;
    private long tiempoDeUltimaPeticionKill = 0;
    private final long enfriamientoKill = 15000; // 15 segundos == 15000 ms

    // Canvas JavaFx
    public final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer gameLoop;


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

    public GamePane(Scene scene, Client cliente, int localID, boolean isHost, GameServer servidor, String nombreJugadorLocal, String colorJugadorLocal, StageManager stageManager) {

        this.canvas = new Canvas();
        this.stageManager = stageManager;
        this.esHost = isHost;
        this.localID = localID;
        this.cliente = cliente;
        this.localPlayerName = nombreJugadorLocal;
        this.localPlayerColor = colorJugadorLocal;

        if (servidor != null) {
            this.servidorLocal = servidor;
        } else {
            this.servidorLocal = null;
        }

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

        //Progress Bar
        barraProgresoTareas = new javafx.scene.control.ProgressBar(0);
        barraProgresoTareas.setPrefWidth(300);
        barraProgresoTareas.setPrefHeight(25);
        barraProgresoTareas.setTranslateX(20);
        barraProgresoTareas.setTranslateY(20);
        barraProgresoTareas.setStyle("-fx-accent: #2ecc71; -fx-control-inner-background: #34495e; -fx-background-color: transparent; -fx-border-color: #2ecc71; -fx-border-width: 2;");
        barraProgresoTareas.setVisible(false);


        lobbyUI.getChildren().addAll(lblLocalIP, lblContador, btnSalir);

        // 3. Add Start button only for Host
        if (isHost) {
            btnEmpezar = new Button("INICIAR PARTIDA");
            btnEmpezar.setDisable(true); // Disabled until 5 players
            btnEmpezar.setOnAction(e -> cliente.sendTCP(new Red.PaquetePedirInicio()));
            lobbyUI.getChildren().add(btnEmpezar);
        }


        this.getChildren().addAll(group, tareaOverlay, lobbyUI, barraProgresoTareas);

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
        this.mapHandler = new MapHandler();
        this.mapHandler.loadMapFile("lobby.tmx");
        this.mapaActual = "lobby.tmx";
        this.collisionChecker = new CollisionChecker(mapHandler);
        this.tareasPorHacer = mapHandler.calcularPosicionTareas(false);


        inicializarTareaUIs();

        this.gameLoop = new AnimationTimer() {
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
        };

        gameLoop.start();
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

        if (keyH.electrocutar() && this.impostor) {

            long tiempoActual = System.currentTimeMillis();

            if (tiempoActual - tiempoDeUltimaPeticionKill >= enfriamientoKill) {
                tiempoDeUltimaPeticionKill = tiempoActual;
                solicitarKill();
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


        // Aqui dibujamos componentes de UI (menus, botones, etc) en una posicion fija

        dibujarImpostorEnfriamiento(gc);

        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText(fpsDisplay, 120, 120); // Draws at top-left

    }

    private void dibujarImpostorEnfriamiento(GraphicsContext gc) {
        if (this.impostor && !localPlayer.wasKilled()) {

            // No ha hecho nada o falló una kill
            if (this.tiempoDeUltimaPeticionKill == 0)
                return;

            long tiempoActual = System.currentTimeMillis();
            long transcurrido = tiempoActual - tiempoDeUltimaPeticionKill;

            // Solo dibujamos si esta en enfriamiento
            if (transcurrido < enfriamientoKill) {
                double porcentajeRestante = (double) transcurrido / enfriamientoKill;
                double angulo = 360 * porcentajeRestante;

                // Lo posicionamos abajo a la derecha (como Capriles en el 2014)
                double x = getWidth() - 100;
                double y = getHeight() - 100;
                double size = 60;

                // Circulo (dark/disabled)
                gc.setFill(Color.rgb(0, 0, 0, 0.6));
                gc.fillOval(x, y, size, size);

                // Rellenado
                gc.setFill(Color.rgb(255, 0, 0, 0.5)); // Red "Kill" color
                gc.fillArc(x, y, size, size, 90, angulo, javafx.scene.shape.ArcType.ROUND);

                // Dibujamos tambien los segundos restantes
                long secondsLeft = (enfriamientoKill - transcurrido) / 1000 + 1;
                gc.setFill(Color.WHITE);
                gc.setFont(new javafx.scene.text.Font("Arial", 20));
                gc.fillText(String.valueOf(secondsLeft), x + 22, y + 38);

                gc.setFont(new javafx.scene.text.Font("Arial", 12));
                gc.fillText("TASE", x + 18, y + size + 15);
            } else {
                // De lo contrario simplemente dibujamos que puede electrocutar
                double x = getWidth() - 100;
                double y = getHeight() - 100;
                gc.setFill(Color.RED);
                gc.fillOval(x, y, 60, 60);
                gc.setFill(Color.WHITE);
                gc.fillText("LISTO", x + 10, y + 35);
            }
        }
    }

    public void removerJugadorRemoto(Red.PaqueteRemoverJugador datos) {

        jugadoresRemotos.remove(datos.idJugadorDesconectado);

        Platform.runLater(() -> {
            lblContador.setText("Jugadores: " + datos.totalJugadoresConectados + "/10 (Min. 5)");
            if (btnEmpezar != null) {
                btnEmpezar.setDisable(!datos.puedeEmpezar);
            }

        });

    }

    public void solicitarKill() {
        Red.PaqueteSolicitarKill solicitarKill = new Red.PaqueteSolicitarKill();
        solicitarKill.idImpostor = localID;

        System.out.println("Solicitando kill");
        cliente.sendTCP(solicitarKill);
    }

    public void agregarJugadorRemoto(Red.PaqueteConexion datos) {
        // Si el ID soy yo mismo, no me agrego a la lista de "remotos"
        System.out.println("Creando jugador remoto");
        if (datos.idJugador == this.localID) {
            return;
        }


        System.out.println("Viendo si es duplicado");
        // Evitar duplicados
        if (jugadoresRemotos.containsKey(datos.idJugador)) return;


        Platform.runLater(() -> {
            System.out.println("Creando avatar para: " + datos.nombreJugador);

            // Creamos la entidad visual
            RemotePlayer nuevoRemoto = new RemotePlayer(
                    this.tileSize,
                    datos.nombreJugador,
                    datos.colorJugador
            );

            // Lo guardamos en el mapa para que el update() y draw() lo procesen
            nuevoRemoto.setId(datos.idJugador);
            nuevoRemoto.setAccion("up");
            nuevoRemoto.setFacingTowards("right");
            jugadoresRemotos.put(datos.idJugador, nuevoRemoto);
        });
    }


    private void inicializarTareaUIs() {
        for (Tarea tarea : tareasPorHacer) {
            try {

                TareaPane ui = tarea.crearUI();

                ui.setOnCerrarTarea(() -> {

                    System.out.println("GamePane: Cerrar tarea callback!");
                    System.out.println("Cerrar tarea callback triggered!");
                    cerrarTareaActual();
                });

                ui.setOnTareaCompletada(() -> {

                    System.out.println("Tarea completada!");
                    Red.PaqueteTareaCompletada tareaCompletada = new Red.PaqueteTareaCompletada();
                    tareaCompletada.idJugador = localID;
                    cliente.sendTCP(tareaCompletada);
                    // Autocerrado despues de un segundo luego de completar la tarea
                    javafx.animation.PauseTransition delay =
                            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                    delay.setOnFinished(e -> cerrarTareaActual());
                    delay.play();
                });


                tarea.setUiPane(ui);


            } catch (Exception e) {
                System.err.println("ERROR loading task UI:");
                e.printStackTrace();
            }

        }
    }

    private void abrirTareaActual() {

        for (Tarea tarea : tareasPorHacer) {
            if (!tarea.fueCompletada() && tarea.getJugadorCerca()) {

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
        SalaVotacion salaVotacion = null;

        for (Tarea tarea : tareasPorHacer) {
            if (tarea.getNombre().equals("Sala-de-Votacion")) {
                salaVotacion = (SalaVotacion) tarea;
                break;
            }
        }

        if (salaVotacion != null && salaVotacion.getUiPane() instanceof VotacionPane votacionPane) {
            HashMap<Integer, String> jugadoresVivos = new HashMap<>();

            if (!localPlayer.wasKilled()) {
                jugadoresVivos.put(localID, localPlayer.getNombre());
            }


            for (java.util.Map.Entry<Integer, RemotePlayer> entry : jugadoresRemotos.entrySet()) {
                RemotePlayer remotePlayer = entry.getValue();

                if (!remotePlayer.wasKilled()) {
                    jugadoresVivos.put(entry.getKey(), remotePlayer.getNombre());
                }

            }


            votacionPane.configurar(jugadoresVivos, localID, this::jugadorSaltaVoto, this::jugadorEjerceVoto);
        }

        if (salaVotacion != null) {
            abrirTarea(salaVotacion);
        }

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

        this.impostor = localID == datos.idImpostor1 || localID == datos.idImpostor2;

        if (this.impostor) {
            System.out.println("-------------- ERES EL IMPOSTOR ------------------");
        }

        // Ocultamos la ventana del lobby
        this.ocultarLobbyUI();

        // Cargamos el nuevo mapa
        this.mapHandler.loadMapFile(datos.mapa);
        // Si es impostor, no tiene que hacer tareas
        this.tareasPorHacer = mapHandler.calcularPosicionTareas(this.impostor);
        this.tareasRestantes = datos.tareasRestantes;
        this.totalTareasGlobales = datos.tareasRestantes; // Inicialmente son iguales pero solo vamos a restar a tareas restantes

        inicializarTareaUIs();

        this.barraProgresoTareas.setVisible(true);

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


    }

    // Actualizamos la variable e UI de progreso para que el jugador sepa cuanto falta (Progress Bar)
    public void actualizarTareasRestantes(Red.PaqueteActualizarTareasRestantes datos) {
        this.tareasRestantes = datos.tareasRestantes;

        Platform.runLater(() -> {
            if (totalTareasGlobales > 0) {
                double progreso = (double) (totalTareasGlobales - tareasRestantes) / totalTareasGlobales;
                barraProgresoTareas.setProgress(progreso);
            }
        });
    }

    public void manejarElectrocucion(Red.PaqueteRespuestaKill paquete) {

        // No se electrocuto a nadie
        if (paquete.idJugadorElectrocutado == -1) {
            System.out.println("No se electrocuto a nadie");
            this.tiempoDeUltimaPeticionKill = 0;
            return;
        }

        if (paquete.idJugadorElectrocutado == localID) {
            localPlayer.setKilled(true);
            localPlayer.setAccion("killed");
            keyH.resetPressedKeys(); // inmobilizamos
            this.tareasRestantes = paquete.tareasRestantes;
            return;
        }

        for (RemotePlayer jugadorRemoto : jugadoresRemotos.values()) {
            if (jugadorRemoto.getID() == paquete.idJugadorElectrocutado) {
                jugadorRemoto.setKilled(true);
                jugadorRemoto.setAccion("killed");
                this.tareasRestantes = paquete.tareasRestantes;

                if (impostor) {
                    localPlayer.setAccion("attacking");
                    localPlayer.setPaused(true);
                    keyH.resetPressedKeys();


                    long tiempoActual = System.currentTimeMillis();
                    long tiempoCongelado = tiempoActual + 2000; // 2 segundos

                    while (tiempoActual < tiempoCongelado) {
                        tiempoActual = System.currentTimeMillis();
                    }

                    localPlayer.setPaused(false);
                    localPlayer.setAccion("idle");


                }

                break;
            }
        }
    }

    public void ocultarLobbyUI() {
        Platform.runLater(() -> lobbyUI.setVisible(false));
    }

    private void irAMenuPrincipal() {
        if (this.gameLoop != null) {
            this.gameLoop.stop();
            this.gameLoop = null;
        }

        if (this.cliente != null) {

            try {
                this.cliente.stop();
                this.cliente.close();
            } catch (Exception e) {
                System.out.println("Error cerrando el cliente " + e.getMessage());
                e.printStackTrace();
            }

            this.cliente = null;
        }


        if (this.servidorLocal != null) {
            System.out.println("Tratando de cerrar servidor");
            this.servidorLocal.detenerServidor();
            this.servidorLocal = null;
        }

        Platform.runLater(() -> {
            try {
                // Cargamos el menu principal y cambiamos la scene de JavaFx
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/homescreen.fxml"));
                Parent root = loader.load();
                HomeScreen controller = loader.getController();
                controller.setStageManager(this.stageManager);

                stageManager.setRoot(root, "Among Us Uneg");
            } catch (IOException e) {
                System.err.println("Error al regresar al menu principal: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Player getLocalPlayer() {
        return this.localPlayer;
    }

    public void setLocalID(int id) {
        this.localID = id;
    }

    public void mostrarPantallaFinJuego(Red.PaqueteFinJuego paquete) {
        // Detenemos las entradas del teclado para que nadie se mueva
        if (this.keyH != null) {
            this.keyH.resetPressedKeys();
        }

        // Cerramos cualquier tarea que el jugador tenga abierta
        cerrarTareaActual();

        // Creamos la pantalla oscura que cubrirá todo el canvas
        VBox pantallaFin = new VBox(20);
        pantallaFin.setAlignment(javafx.geometry.Pos.CENTER);
        pantallaFin.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);"); // Fondo negro casi opaco
        pantallaFin.setPrefSize(canvas.getWidth(), canvas.getHeight());

        // Tomamos el mensaje de victoria del servidor y lo mostramos
        Label lblGanador = new Label(paquete.mensajeGanador);
        lblGanador.setFont(new javafx.scene.text.Font("Arial", 60));
        lblGanador.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 10, 0, 0, 0);");

        // Cambiamos el color dependiendo de quién ganó
        if (paquete.mensajeGanador.contains("IMPOSTORES")) {
            lblGanador.setTextFill(Color.RED);
        } else {
            lblGanador.setTextFill(Color.CYAN); // Azul claro para los tripulantes
        }

        pantallaFin.getChildren().add(lblGanador);

        // Lo agregamos a la pantalla y forzamos que esté por encima de todo
        this.getChildren().add(pantallaFin);
        pantallaFin.toFront();

        // Iniciamos un temporizador de 5 segundos antes de salir al menú para que no sean tan abrupto el cambio
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
        delay.setOnFinished(e -> irAMenuPrincipal());
        delay.play();
    }

}
