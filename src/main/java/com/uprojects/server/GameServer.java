package com.uprojects.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.uprojects.core.Tarea;
import com.uprojects.entities.RemotePlayer;

import java.io.IOException;
import java.rmi.Remote;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer extends Listener {

    private Server server;
    private boolean enReunion;
    private int tareasRestantes;
    private HashMap<Integer, Integer> votos;
    private HashMap<String, Integer> coloresDisponibles;
    private static final String[] colores = {"Amarillo", "Azul", "AzulClaro", "Gris", "Morado", "Naranja", "Rojo", "Rosado", "Verde", "VerdeOscuro"};
    private final int MIN_JUGADORES = 2;
    private final int MAX_JUGADORES = 10;
    private int votosRecibidos;
    private int cantImpostores;
    private final int TCP_PORT = 54555;
    private final int UDP_PORT = 54777;


    private HashMap<Integer, ServerPlayer> jugadores = new HashMap<>();
    private boolean juegoIniciado = false;

    public GameServer() throws IOException {
        this.server = new Server();
        this.enReunion = false;
        this.votos = new HashMap<>();
        this.votosRecibidos = 0;
        this.coloresDisponibles = new HashMap<>(10);

        for (String color : colores) {
            coloresDisponibles.put(color, -1);
        }


        // Registramos las clases que se transmitiran por la red
        Red.registrar(server.getKryo());

        // Anclamos a los puertos TCP y UDP
        server.bind(TCP_PORT, UDP_PORT);


        server.start();


        System.out.println("Servidor iniciado. Esperando jugadores...");

        server.addListener(this);
    }

    // Se ejecuta cuando se recibe una conexion (un jugador se conecta al servidor)
    @Override
    public void connected(Connection conexion) {

        System.out.println("Conexion recibida de " + conexion.getID());

        if (jugadores.size() >= MAX_JUGADORES || juegoIniciado) {
            System.out.println("Servidor lleno. Rechazando a " + conexion.getRemoteAddressTCP());
            conexion.close(); // Rechaza si está lleno o ya empezó
            return;
        }

        ServerPlayer tempPlayer = new ServerPlayer(conexion.getID(), "Cargando...", "Amarillo");
        jugadores.put(conexion.getID(), tempPlayer);


        broadcastLobbyStatus();
    }


    // Se ejecuta cuando algun cliente (jugador) se desconecta
    @Override
    public void disconnected(Connection conexion) {

        System.out.println("Jugador desconectado. Total: " + jugadores.size());

        ServerPlayer jugador = jugadores.get(conexion.getID());

        if (jugador == null)
            return;

        // Liberamos el color del jugador
        if (jugador.color != null) {
            coloresDisponibles.put(jugador.color, -1);
        }

        jugadores.remove(conexion.getID());

        Red.PaqueteRemoverJugador jugadorDesconectado = new Red.PaqueteRemoverJugador();
        jugadorDesconectado.idJugadorDesconectado = conexion.getID();
        jugadorDesconectado.totalJugadoresConectados = jugadores.size();
        jugadorDesconectado.puedeEmpezar = jugadores.size() >= MIN_JUGADORES;

        if (jugador.impostor) {
            cantImpostores--;
        }


        server.sendToAllTCP(jugadorDesconectado);

        // Si alguien se va, verificamos si los que quedan ya terminaron
        if (juegoIniciado) verificarFinDeJuego();
    }

    // Se ejecuta cuando recibimos un paquete a traves del socket
    @Override
    public void received(Connection conexion, Object paquete) {

        if (paquete instanceof Red.PaquetePedirInicio) {
            if (conexion.getID() == 1 && jugadores.size() >= MIN_JUGADORES) {
                if (jugadores.size() >= 7) {
                    this.cantImpostores = 2;
                } else {
                    this.cantImpostores = 1;
                }
                iniciarJuego(conexion);
            }
        }

        // Un jugador entra a la sala
        if (paquete instanceof Red.PaqueteConexion pc) {
            pc.idJugador = conexion.getID();

            Integer idOcupante = coloresDisponibles.get(pc.colorJugador);
            boolean colorEnUso = false;

            if (idOcupante != null && idOcupante != -1 && idOcupante != pc.idJugador) {
                colorEnUso = true;
            }

            if (colorEnUso) {
                for (Map.Entry<String, Integer> entry : coloresDisponibles.entrySet()) {
                    if (entry.getValue() == -1) {
                        pc.colorJugador = entry.getKey();
                        break;
                    }
                }
            }

            // Marcamos el color como reservado
            coloresDisponibles.put(pc.colorJugador, pc.idJugador);

            // Actualizamos al jugador temporal que creamos cuando se conecto con sus valores calculados
            ServerPlayer nuevoJugador = jugadores.get(pc.idJugador);

            if (nuevoJugador != null) {
                nuevoJugador.nombre = pc.nombreJugador + "_" + conexion.getID();
                nuevoJugador.color = pc.colorJugador;
                nuevoJugador.oculto = false;
            }

            // Le avisamos al jugador para que se cambie el color localmente
            conexion.sendTCP(pc);


            // Ahora a los demas
            server.sendToAllExceptTCP(conexion.getID(), pc);


            // Le enviamos la posicion de todos los jugadores al jugador nuevo
            for (ServerPlayer serverPlayer : jugadores.values()) {
                if (serverPlayer.id != pc.idJugador) {
                    Red.PaqueteConexion infoExistente = new Red.PaqueteConexion();
                    infoExistente.idJugador = serverPlayer.id;
                    infoExistente.nombreJugador = serverPlayer.nombre;
                    infoExistente.colorJugador = serverPlayer.color;

                    conexion.sendTCP(infoExistente); // Solo al que se acaba de conectar recibe esto
                }
            }

            broadcastLobbyStatus();

        }

        // Un jugador se mueve (UDP)
        if (paquete instanceof Red.PaqueteActualizarJugador jugador) {

            // Establecemos el ID del jugador en base al ID de la conexion. Esto nos facilita garantizar la unicidad del ID
            jugador.idJugador = conexion.getID();

            ServerPlayer serverPlayer = jugadores.get(jugador.idJugador);

            if (serverPlayer != null) {
                serverPlayer.x = jugador.x;
                serverPlayer.y = jugador.y;
                serverPlayer.oculto = jugador.oculto;
                serverPlayer.killed = jugador.killed;
            }


            server.sendToAllExceptUDP(conexion.getID(), jugador);
        }

        // Un jugador completa una tarea (TCP)
        if (paquete instanceof Red.PaqueteTareaCompletada tareaCompletada) {
            ServerPlayer jugador = jugadores.get(tareaCompletada.idJugador);
            if (jugador != null) {


                boolean tareaEstabaCompletada = false;


                for (String tarea : jugador.tareasFinalizadas) {
                    if (tarea.equals(tareaCompletada.tipoTarea)) {
                        tareaEstabaCompletada = true;
                        break;
                    }
                }

                // Ignoramos si ya habia sido completada
                if (tareaEstabaCompletada)
                    return;

                jugador.tareasCompletadas++;
                this.tareasRestantes--;
                jugador.tareasFinalizadas.add(tareaCompletada.tipoTarea);

                //System.out.println(jugador.nombre + " completó una tarea (" + jugador.tareasCompletadas + "/" + jugador.tareasTotales + ")");


                verificarFinDeJuego();

                System.out.println("TAREA COMPLETADA -> TAREAS RESTANTES: " + this.tareasRestantes);

                Red.PaqueteActualizarTareasRestantes tareas = new Red.PaqueteActualizarTareasRestantes();
                tareas.tareasRestantes = this.tareasRestantes;

                server.sendToAllTCP(tareas);

            }
        }

        if (paquete instanceof Red.PaqueteSalirLobby) {
            System.out.println("Jugador " + conexion.getID() + " salió del lobby");

            // Liberamos el color del jugador

            ServerPlayer serverPlayer = jugadores.get(conexion.getID());

            if (serverPlayer != null && serverPlayer.color != null) {
                coloresDisponibles.put(serverPlayer.color, -1);
            }

            jugadores.remove(conexion.getID()); //

            // Enviamos la señal para que los demas jugadores dejen de renderizarlo
            Red.PaqueteRemoverJugador remove = new Red.PaqueteRemoverJugador();
            remove.idJugadorDesconectado = conexion.getID();
            remove.totalJugadoresConectados = jugadores.size();
            server.sendToAllExceptTCP(conexion.getID(), remove);

            conexion.close();
            broadcastLobbyStatus();
        }

        if (paquete instanceof Red.PaqueteLlamarReunion reunion) {
            if (!enReunion) {
                enReunion = true;
                votos.clear();
                votosRecibidos = 0;
                server.sendToAllTCP(reunion);
            }
        }

        if (paquete instanceof Red.PaqueteVoto voto) {
            if (enReunion) {
                votos.put(voto.idVotante, voto.idVotado);
                votosRecibidos++;

                int vivos = (int) jugadores.values().stream().filter(p -> !p.killed).count();

                if (votosRecibidos >= vivos) {
                    procesarVotacion();
                }
            }
        }

        if (paquete instanceof Red.PaqueteSolicitarKill kill) {
            ServerPlayer serverPlayer = jugadores.get(kill.idImpostor);

            System.out.println("Llegó solicitud de kill por parte de -> " + serverPlayer.nombre);

            if (serverPlayer != null && serverPlayer.impostor) {

                long tiempoActual = System.currentTimeMillis();
                long enfriamientoMS = 10000; // 15 segundos == 15000 milisegundos


                if (tiempoActual - serverPlayer.tiempoUltimaKill < enfriamientoMS) {
                    System.out.println("Kill denegada!");
                    return;
                }

                System.out.println("El impostor solicitó electrocución!");

                boolean huboEliminacion = false;

                for (ServerPlayer jugador : jugadores.values()) {


                    if (!jugador.impostor && jugador.id != serverPlayer.id && !jugador.killed) {
                        // Calculamos el centro del sprite. 32 porque los personajes son de 32x32 pixeles. Quizas al comienzo del juego podemos pasar el tilesize al servidor y manejarlo asi
                        double victimaX = jugador.x + (32 / 2.0);
                        double victimaY = jugador.y + (32 / 2.0);

                        double impostorX = serverPlayer.x + (32 / 2.0);
                        double impostorY = serverPlayer.y + (32 / 2.0);

                        // Distancia euclidiana, o algo asi
                        double distancia = Math.hypot(victimaX - impostorX, victimaY - impostorY);
                        double rango = 45.0;


                        if (distancia <= rango) {
                            System.out.println("Kill confirmada. " + serverPlayer.nombre + " ha electrocutado a " + jugador.nombre);
                            jugador.killed = true;
                            serverPlayer.tiempoUltimaKill = tiempoActual; // Actualizamos su tiempo para que no pueda spamear kills
                            int tareasPendienteDelElectrocutado = jugador.tareasTotales - jugador.tareasCompletadas;
                            this.tareasRestantes -= tareasPendienteDelElectrocutado;


                            Red.PaqueteRespuestaKill respuestaKill = new Red.PaqueteRespuestaKill();
                            respuestaKill.idJugadorElectrocutado = jugador.id;
                            respuestaKill.tareasRestantes = this.tareasRestantes;

                            server.sendToAllTCP(respuestaKill);

                            huboEliminacion = true;
                            break; // Terminamos el bucle para que no mate a dos que estaban cerca

                        }
                    }
                }

                if (huboEliminacion) {

                    verificarFinDeJuego();
                } else {

                    Red.PaqueteRespuestaKill respuestaKill = new Red.PaqueteRespuestaKill();
                    respuestaKill.idJugadorElectrocutado = -1;
                    respuestaKill.tareasRestantes = this.tareasRestantes;


                    // Se lo enviamos solo al impostor
                    conexion.sendTCP(respuestaKill);

                }
            }

        }

    }

    private void procesarVotacion() {
        enReunion = false;
        HashMap<Integer, Integer> conteo = new HashMap<>();

        for (Integer voto : votos.values()) {
            conteo.put(voto, conteo.getOrDefault(voto, 0) + 1);
        }

        int idMax = -1;
        int maxVotos = 0;
        boolean empate = false;

        for (java.util.Map.Entry<Integer, Integer> entry : conteo.entrySet()) {
            if (entry.getValue() > maxVotos) {
                maxVotos = entry.getValue();
                idMax = entry.getKey();
                empate = false;
            } else if (entry.getValue() == maxVotos) {
                empate = true;
            }
        }

        Red.PaqueteResultadoVotacion resultado = new Red.PaqueteResultadoVotacion();

        if (empate || idMax == -1) {
            resultado.empate = true;
            resultado.idExpulsado = -1;
        } else {
            resultado.empate = false;
            resultado.idExpulsado = idMax;
            ServerPlayer expulsado = jugadores.get(idMax);
            if (expulsado != null) {
                expulsado.killed = true;
                if (expulsado.impostor) {
                    cantImpostores--;
                }
            }
        }

        server.sendToAllTCP(resultado);
        verificarFinDeJuego();
    }


    private void iniciarJuego(Connection conexion) {

        // Por si acaso revisamos si el juego ya habia comenzado
        if (juegoIniciado)
            return;

        juegoIniciado = true;

        List<Integer> idJugadores = new ArrayList<>(jugadores.keySet());

        // 4 tareas por jugador, sin contar a los impostores
        this.tareasRestantes = (jugadores.size() - cantImpostores) * 4;

        System.out.println("INICIO DE JUEGO -> TAREAS RESTANTES: " + this.tareasRestantes);

        Collections.shuffle(idJugadores);
        // Como ya los barajamos, simplemente escogemos los que queramos
        int idImpostor1 = idJugadores.get(0);
        int idImpostor2 = idJugadores.get(1);


        // Se lo mandamos a cada conexion, es decir, a cada jugador conectado al servidor
        //for (Connection conexion : server.getConnections()) {
        Red.PaqueteIniciarJuego iniciarJuego = new Red.PaqueteIniciarJuego();
        iniciarJuego.mapa = "mapa1.tmx";
        // Por ahora lo mandamos a la biblioteca, tenemos que calcular esto mejor al tener dos mapas
        iniciarJuego.inicioX = 512;
        iniciarJuego.inicioY = 384;
        iniciarJuego.tareasRestantes = this.tareasRestantes;
        iniciarJuego.colorJugadorLocal = jugadores.get(conexion.getID()).color;

        if (cantImpostores == 2) {

            iniciarJuego.idImpostor1 = idImpostor1;
            iniciarJuego.idImpostor2 = idImpostor2;
            jugadores.get(idImpostor1).impostor = true;
            jugadores.get(idImpostor2).impostor = true;
        } else {

            iniciarJuego.idImpostor1 = idImpostor1;
            jugadores.get(idImpostor1).impostor = true;
            iniciarJuego.idImpostor2 = -1; // Para que ninguno coincida
        }

        server.sendToAllTCP(iniciarJuego);
        //}

        System.out.println("¡Sala llena! Iniciando partida...");
        //server.sendToAllTCP(new Red.PaqueteIniciarJuego());
    }

    private void verificarFinDeJuego() {
        if (jugadores.isEmpty() || !juegoIniciado) return;

        boolean todosTerminaron = true;

        // Calculamos la cantidad de jugadores electrocutados para ver si el impostor gano
        int tripulantesVivos = 0;
        boolean debeFinalizarJuego = false;

        // Revisamos si algún jugador aún no termina sus tareas
        for (ServerPlayer jugador : jugadores.values()) {

            if (!jugador.impostor && !jugador.killed) {
                tripulantesVivos++;
            }

            if (!jugador.impostor && jugador.tareasCompletadas < jugador.tareasTotales) {
                todosTerminaron = false;
            }


        }

        // Si el bucle termina y todosTerminaron sigue siendo true, ¡ganaron!
        if (todosTerminaron) {
            System.out.println("¡Todas las tareas completadas! Fin del juego.");
            Red.PaqueteFinJuego fin = new Red.PaqueteFinJuego();
            fin.mensajeGanador = "¡VICTORIA DE LOS TRIPULANTES!";
            server.sendToAllTCP(fin);

            juegoIniciado = false;

            return;

        } else if (cantImpostores == 0) {

            System.out.println("¡Han expulsado a todos los impostores! Fin del juego.");
            Red.PaqueteFinJuego fin = new Red.PaqueteFinJuego();
            fin.mensajeGanador = "¡VICTORIA DE LOS TRIPULANTES!";
            server.sendToAllTCP(fin);

            juegoIniciado = false;

            return;
        }

        if (tripulantesVivos <= cantImpostores) {
            System.out.println("El Impostor ha ganado");
            Red.PaqueteFinJuego fin = new Red.PaqueteFinJuego();


            fin.mensajeGanador = "¡VICTORIA DE LOS IMPOSTORES!";
            server.sendToAllTCP(fin);

            // Aquí podemos reiniciar las variables para una nueva partida
            juegoIniciado = false;
            return;
        }
    }

    private void broadcastLobbyStatus() {
        Red.PaqueteLobbyInfo status = new Red.PaqueteLobbyInfo();
        status.conectados = jugadores.size();
        status.puedeEmpezar = jugadores.size() >= MIN_JUGADORES;
        status.mapaActual = "lobby.tmx";
        server.sendToAllTCP(status);
    }

    public void detenerServidor() {
        System.out.println("Cerrando servidor...");


        if (this.server != null) {

            try {

                this.server.stop();

                this.server.close();
            } catch (Exception e) {
                System.out.println("Error cerrando el servidor " + e.getMessage());
                e.printStackTrace();
            }

            this.server = null;
        }

        if (!jugadores.isEmpty()) {
            jugadores.clear();
        }

        System.out.println("Servidor finalizado. Ya deberian estar libres los puertos");
    }

}

