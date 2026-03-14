package com.uprojects.screens;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.uprojects.core.ArreglarCablesTarea;
import com.uprojects.core.DuctoTarea;
import com.uprojects.core.SalaVotacion;
import com.uprojects.core.Tarea;
import com.uprojects.entities.RemotePlayer;
import com.uprojects.helpers.CollisionChecker;
import com.uprojects.helpers.KeyHandler;
import com.uprojects.entities.Player;
import com.uprojects.server.GameServer;
import com.uprojects.server.Red;
import com.uprojects.stages.MapHandler;
import com.uprojects.ui.*;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

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
    private boolean juegoIniciado;

    // Config Pantalla
    private final int originalTileSize = 32; // Eje: 16x16 tiles
    private final int scale = 1;

    // ProgressBar
    //private int totalTareasGlobales;
    //private javafx.scene.control.ProgressBar barraProgresoTareas;
    private TareaProgressBar barraProgreso;


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
    private Tarea ductoEnUso;

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
    //private double conoVision;

    // Map handler (more like map manager but u get it)
    private MapHandler mapHandler;
    private String mapaActual;
    private String mapaSeleccionado;

    // Colision Checker
    private CollisionChecker collisionChecker;

    // Fog of War (Campo de vision del jugador)
    private FogOfWar fog;

    // Lobby
    private LobbyUI lobbyUI;

    // HUD para electrocutar
    private KillHUD killHUD;


    public GamePane(Scene scene, Client cliente, int localID, boolean isHost, GameServer servidor, String nombreJugadorLocal, String colorJugadorLocal, StageManager stageManager, String mapa) {

        this.canvas = new Canvas();
        this.stageManager = stageManager;
        this.esHost = isHost;
        this.localID = localID;
        this.cliente = cliente;
        this.juegoIniciado = false;
        this.localPlayerName = nombreJugadorLocal;
        this.localPlayerColor = colorJugadorLocal;
        this.mapaSeleccionado = mapa;


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

        // No podemos instanciar el KeyHandler hasta que tengamos una Scene valida
        this.sceneProperty().addListener((obs, oldS, nuevaScene) -> {
            if (nuevaScene != null) {
                this.keyH = new KeyHandler(scene);
            }
        });


        // Inicializando lista de jugadores
        this.jugadoresRemotos = new HashMap<>();
        this.tareaActual = null;
        this.tareasPorHacer = new ArrayList<>();
        this.ductoEnUso = null;


        // UI
        try {
            this.lobbyUI = new LobbyUI(isHost, cliente, mapaSeleccionado, this::irAMenuPrincipal);
        } catch (UnknownHostException e) {
            System.out.println("No se pudo crear el lobby");
            irAMenuPrincipal();
        }

        this.barraProgreso = new TareaProgressBar();
        this.killHUD = new KillHUD(15000);
        this.killHUD.layoutXProperty().bind(this.widthProperty().subtract(120));
        this.killHUD.layoutYProperty().bind(this.heightProperty().subtract(140));

        this.getChildren().addAll(group, tareaOverlay, lobbyUI, barraProgreso, killHUD);

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

        // Comenzamos en el lobby, despues cambiamos al mapa elegido
        this.localPlayer = new Player(keyH, (int) canvas.getWidth(), (int) canvas.getHeight(), tileSize, localPlayerName, localPlayerColor);
        this.mapHandler = new MapHandler();
        this.mapHandler.loadMapFile("lobby.tmx");
        this.mapaActual = "lobby.tmx";
        this.collisionChecker = new CollisionChecker(mapHandler);
        this.tareasPorHacer = mapHandler.calcularPosicionTareas(false);
        this.fog = new FogOfWar(220.0);

        inicializarTareaUIs();

        /*
        if (this.impostor) {
            tiempoDeUltimaPeticionKill = System.currentTimeMillis();
        }

         */

        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (lastNanoTime == 0) {
                    lastNanoTime = now;
                    fpsTimer = now;
                    return;
                }

                // FPS
                frameCount++;
                // Paso 1 segundo (1,000,000,000 nanosegundos)
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
                    System.err.println("=== EXCEPCION en game loop ===");
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


        localPlayer.actualizarPosicion(collisionChecker);
        enviarStatusLocalPlayer();

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

            solicitarKill();

            /*
            long tiempoActual = System.currentTimeMillis();

            if (tiempoActual - tiempoDeUltimaPeticionKill >= enfriamientoKill) {
                tiempoDeUltimaPeticionKill = tiempoActual;
                solicitarKill();
            }

             */
        }


        for (RemotePlayer jugadorExterno : jugadoresRemotos.values()) {
            jugadorExterno.actualizarPosicion(null);
        }

    }

    private void enviarStatusLocalPlayer() {

        Red.PaqueteActualizarJugador posicionEnviar = new Red.PaqueteActualizarJugador();
        posicionEnviar.idJugador = localID;
        posicionEnviar.x = localPlayer.getWorldX();
        posicionEnviar.y = localPlayer.getWorldY();
        posicionEnviar.accion = localPlayer.getAccion();
        posicionEnviar.facingTowards = localPlayer.getFacingTowards();
        posicionEnviar.killed = localPlayer.wasKilled();
        posicionEnviar.oculto = localPlayer.isOculto();
        cliente.sendUDP(posicionEnviar);
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

            // Distancia euclidiana entre el jugador local y los demas jugadores. Lo usamos para ver si se encuentran fuera del cono de vision del jugador
            double distancia = Math.hypot(localPlayer.getWorldX() - jugadorExterno.getWorldX(), localPlayer.getWorldY() - jugadorExterno.getWorldY());

            if (!this.juegoIniciado) {
                jugadorExterno.draw(gc);
            } else if (distancia <= fog.getCampoVision()) {
                jugadorExterno.draw(gc);
            }

        }

        for (Tarea tarea : this.tareasPorHacer) {
            tarea.drawInteractionBox(gc);
        }


        //mapHandler.draw(this.gc, zoom);
        //localPlayer.draw(this.gc);
        // Liberamos los recursos que este usando el GraphicalContext
        gc.restore();

        //fogOfWar(gc);
        if (juegoIniciado)
            fog.render(gc, canvas.getWidth(), canvas.getHeight());

        // Aqui dibujamos componentes de UI (menus, botones, etc) en una posicion fija
        //dibujarImpostorEnfriamiento(gc);
        killHUD.actualizarUI();
        // Contador de FPS
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText(fpsDisplay, 120, 60); // Draws at top-left

    }

    private void dibujarImpostorEnfriamiento(GraphicsContext gc) {
        if (this.impostor && !localPlayer.wasKilled()) {

            /*
            // No ha hecho nada o falló una kill
            if (this.tiempoDeUltimaPeticionKill == 0)
                return;

             */

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

            this.lobbyUI.actualizarUI(datos.totalJugadoresConectados, datos.puedeEmpezar);

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
        if (datos.idJugador == this.localID) {
            return;
        }


        System.out.println("Creando jugador remoto");
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

                if (ui instanceof DuctoPane ducto) {
                    ducto.configurar(this::viajarAlSiguienteDucto, () -> {
                        localPlayer.setOculto(false);
                        ductoEnUso = null;
                    });
                }

                ui.setOnTareaCompletada(() -> {


                    tarea.setCompletada(true);

                    // Bloqueamos la intefaz para evitar spameo the tareas hechas
                    //ui.setDisable(true);

                    System.out.println("Tarea completada!");
                    Red.PaqueteTareaCompletada tareaCompletada = new Red.PaqueteTareaCompletada();
                    tareaCompletada.idJugador = localID;
                    tareaCompletada.tipoTarea = tarea.getNombre();
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
                } else if (tarea.getNombre().equals("Ducto")) {
                    localPlayer.setOculto(true);
                    ductoEnUso = tarea;
                    enviarStatusLocalPlayer();
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


    private void viajarAlSiguienteDucto() {

        ArrayList<Tarea> listDeDuctos = new ArrayList<>();

        for (Tarea tarea : tareasPorHacer) {
            if (tarea instanceof DuctoTarea) {
                listDeDuctos.add(tarea);
            }
        }


        if (listDeDuctos.size() > 1 && ductoEnUso != null) {

            int indiceActual = listDeDuctos.indexOf(ductoEnUso);
            int siguienteIndice = (indiceActual + 1) % listDeDuctos.size();
            Tarea siguienteDucto = listDeDuctos.get(siguienteIndice);

            int centroDuctoX = siguienteDucto.getWorldX() + (tileSize / 2);
            int centroDuctoY = siguienteDucto.getWorldY() + (tileSize / 2);

            // Teletransportar al jugador al centro del siguiente ducto
            localPlayer.setWorldPosition(centroDuctoX, centroDuctoY);

            // Cerramos la pantalla del ducto
            this.ductoEnUso = siguienteDucto;
            //cerrarTareaActual();
        }
    }


    public void cambiarAMapaPrincipal(Red.PaqueteIniciarJuego datos) {

        this.impostor = localID == datos.idImpostor1 || localID == datos.idImpostor2;

        this.killHUD.setImpostor(this.impostor);

        if (this.impostor) {
            System.out.println("-------------- ERES EL IMPOSTOR ------------------");
            //this.killHUD.comenzarEnfriamiento();
            this.fog.setCampoVision(400.0);
        } else {
            this.fog.setCampoVision(220.0);
        }

        // Ocultamos la ventana del lobby
        this.ocultarLobbyUI();

        this.juegoIniciado = true;

        // Cargamos el nuevo mapa
        this.mapaActual = datos.mapa;
        this.mapHandler.loadMapFile(datos.mapa);
        // Si es impostor, no tiene que hacer tareas
        this.tareasPorHacer = mapHandler.calcularPosicionTareas(this.impostor);
        this.tareasRestantes = datos.tareasRestantes;
        barraProgreso.setTareasTotales(datos.tareasRestantes);
        barraProgreso.setVisible(true);
        //this.totalTareasGlobales = datos.tareasRestantes; // Inicialmente son iguales pero solo vamos a restar a tareas restantes

        inicializarTareaUIs();

        //this.barraProgresoTareas.setVisible(true);

        // Enviamos a todos los jugadores a la biblioteca (debemos calcular esto mejor, por ahora se queda asi)
        this.localPlayer.setWorldPosition(datos.inicioX, datos.inicioY); // Primero al jugador local

        this.localPlayer.setPaused(true);

        for (RemotePlayer rp : jugadoresRemotos.values()) {
            rp.setTargets(datos.inicioX, datos.inicioY);
        }

        AnuncioRolJugador pantallaInicio = new AnuncioRolJugador(this.impostor, this.getScene().getWidth(), this.getScene().getHeight(), () -> {
            this.localPlayer.setPaused(false);
        });

        this.getChildren().add(pantallaInicio);
        pantallaInicio.toFront();

        //transicionComienzoJuego(datos, this, this.impostor);
    }


    private void transicionComienzoJuego(Red.PaqueteIniciarJuego paquete, GamePane pane, boolean impostor) {
        // Creamos la pantalla oscura que cubrirá todo el stage
        VBox pantallaComienzo = new VBox(20);
        pantallaComienzo.setAlignment(Pos.CENTER);
        pantallaComienzo.setStyle("-fx-background-color: rgba(0, 0, 0, 1.00);"); // Fondo negro casi opaco
        pantallaComienzo.setPrefSize(pane.getScene().getWidth(), pane.getScene().getHeight());

        // Tomamos el mensaje de victoria del servidor y lo mostramos
        Label lblRolJugador = new Label();
        lblRolJugador.setFont(new javafx.scene.text.Font("Arial", 60));
        lblRolJugador.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 10, 0, 0, 0);");

        // Cambiamos el color dependiendo de quién ganó
        if (impostor) {
            lblRolJugador.setTextFill(Color.RED);
            lblRolJugador.setText("ERES UN IMPOSTOR");
        } else {
            lblRolJugador.setTextFill(Color.CYAN); // Azul claro para los tripulantes
            lblRolJugador.setText("ERES UN TRIPULANTE");
        }

        pantallaComienzo.getChildren().add(lblRolJugador);

        // Lo agregamos a la pantalla y forzamos que esté por encima de todo
        pane.getChildren().add(pantallaComienzo);
        pantallaComienzo.toFront();

        // Iniciamos un temporizador de 5 segundos antes de salir al menú para que no sean tan abrupto el cambio
        PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
        delay.setOnFinished(e -> {
            pane.getChildren().remove(pantallaComienzo);
            pane.getLocalPlayer().setPaused(false);
        });
        delay.play();
    }

    public void actualizarLobby(Red.PaqueteLobbyInfo status) {
        Platform.runLater(() -> {

            this.lobbyUI.actualizarUI(status.conectados, status.puedeEmpezar);

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
            remotePlayer.setOculto(status.oculto);
        }


    }

    // Actualizamos la variable e UI de progreso para que el jugador sepa cuanto falta (Progress Bar)
    public void actualizarTareasRestantes(Red.PaqueteActualizarTareasRestantes datos) {
        this.tareasRestantes = datos.tareasRestantes;

        Platform.runLater(() -> {
            barraProgreso.actualizarUI(datos.tareasRestantes);
        });
    }

    public void manejarElectrocucion(Red.PaqueteRespuestaKill paquete) {

        // No se electrocuto a nadie
        if (paquete.idJugadorElectrocutado == -1) {
            System.out.println("No se electrocuto a nadie");
            //this.tiempoDeUltimaPeticionKill = 0;
            this.killHUD.resetEnfriamiento();
            return;
        }

        // Empezamos el conteo para la siguiente kill
        if (this.impostor) {
            this.killHUD.comenzarEnfriamiento();
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

                if (this.impostor) {
                    localPlayer.setAccion("attacking");
                    localPlayer.setPaused(true);
                    keyH.resetPressedKeys();


                    PauseTransition pauseAtaque = new PauseTransition(Duration.seconds(1.5));

                    pauseAtaque.setOnFinished((e) -> {
                        localPlayer.setPaused(false);
                        localPlayer.setAccion("idle");
                    });

                    pauseAtaque.play();


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
        pantallaFin.setAlignment(Pos.CENTER);
        pantallaFin.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);"); // Fondo negro casi opaco
        pantallaFin.setPrefSize(this.getScene().getWidth(), this.getScene().getHeight());

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

    /*
    private void fogOfWar(GraphicsContext gc) {


        // Calculamos el centro del jugador, como cuando lo dibujamos
        double playerScreenX = canvas.getWidth() / 2.0;
        double playerScreenY = canvas.getHeight() / 2.0;

        // Gradiente radial (transparente en el centro y negro en los bordes)
        RadialGradient fog = new RadialGradient(
                0, 0, // angulo, distancia
                playerScreenX, playerScreenY, // centro del círculo
                conoVision, // radio del círculo de vision
                false, // si es proporcional (false para usar pixeles exactos)
                CycleMethod.NO_CYCLE, // fuera del radio del gradiente no se afecta
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.6, Color.rgb(0, 0, 0, 0.4)), // Comienza a desvanecerse
                new Stop(1.0, Color.rgb(0, 0, 0, 0.98)) // oscuro (98% opaco)
        );

        // Dibujamos un rectángulo sobre todo el gamePane la gradiente
        gc.setFill(fog);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

     */

}