package com.uprojects.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.uprojects.entities.RemotePlayer;

import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer extends Listener {

    private Server server;
    private boolean enReunion;
    private HashMap<Integer, Integer> votos;
    private int votosRecibidos;
    private final int MAX_JUGADORES = 10;
    private final int TCP_PORT = 54555;
    private final int UDP_PORT = 54777;


    private HashMap<Integer, ServerPlayer> jugadores = new HashMap<>();
    private final ConcurrentHashMap<Integer, Red.PaqueteActualizarJugador> playersInfo = new ConcurrentHashMap<>();
    private boolean juegoIniciado = false;

    public GameServer() throws IOException {
        this.server = new Server();
        this.enReunion = false;
        this.votos = new HashMap<>();
        this.votosRecibidos = 0;

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
        }

        ServerPlayer nuevoJugador = new ServerPlayer(conexion.getID(), "Jugador_" + conexion.getID(), "Amarillo");
        jugadores.put(conexion.getID(), nuevoJugador);

        System.out.println("Agregado jugador " + nuevoJugador.nombre);


        // Le enviamos la posicion de todos los jugadores al jugador nuevo
        for (ServerPlayer player : jugadores.values()) {
            Red.PaqueteConexion pc = new Red.PaqueteConexion();
            pc.idJugador = player.id;
            pc.nombreJugador = player.nombre;
            pc.colorJugador = player.color;
            conexion.sendTCP(pc); // Solo al que se acaba de conectar recibe esto
        }

        // Ahora si avisamos a los demas
        Red.PaqueteConexion avisoNuevo = new Red.PaqueteConexion();
        avisoNuevo.idJugador = nuevoJugador.id;
        avisoNuevo.nombreJugador = nuevoJugador.nombre;
        avisoNuevo.colorJugador = nuevoJugador.color;

        // Lo enviamos a todos MENOS al que acaba de entrar
        server.sendToAllExceptTCP(conexion.getID(), avisoNuevo);

        broadcastLobbyStatus();
    }


    // Se ejecuta cuando algun cliente (jugador) se desconecta
    @Override
    public void disconnected(Connection conexion) {

        System.out.println("Jugador desconectado. Total: " + jugadores.size());

        jugadores.remove(conexion.getID());
        playersInfo.remove(conexion.getID());

        Red.PaqueteRemoverJugador removerJugador = new Red.PaqueteRemoverJugador();
        removerJugador.idJugador = conexion.getID();

        server.sendToAllTCP(removerJugador);

        // Si alguien se va, verificamos si los que quedan ya terminaron
        if (juegoIniciado) verificarFinDeJuego();
    }

    // Se ejecuta cuando recibimos un paquete a traves del socket
    @Override
    public void received(Connection conexion, Object paquete) {

        if (paquete instanceof Red.PaquetePedirInicio) {
            if (conexion.getID() == 1 && jugadores.size() >= 2) {
                iniciarJuego(conexion);
            }
        }

        // 1. Un jugador entra a la sala
        if (paquete instanceof Red.PaqueteConexion pc) {
            pc.idJugador = conexion.getID();
            ServerPlayer nuevoJugador = new ServerPlayer(pc.idJugador, pc.nombreJugador, pc.colorJugador);
            jugadores.put(pc.idJugador, nuevoJugador);
            System.out.println(pc.nombreJugador + " se ha unido.");

            server.sendToAllExceptTCP(conexion.getID(), pc);

            for (ServerPlayer serverPlayer : jugadores.values()) {
                if (serverPlayer.id != pc.idJugador) {
                    Red.PaqueteConexion infoExistente = new Red.PaqueteConexion();
                    infoExistente.idJugador = serverPlayer.id;
                    infoExistente.nombreJugador = serverPlayer.nombre;
                    infoExistente.colorJugador = "Amarillo"; // Despues veo el color

                    conexion.sendTCP(infoExistente);
                }
            }

            broadcastLobbyStatus();

        }

        // 2. Un jugador se mueve (UDP)
        if (paquete instanceof Red.PaqueteActualizarJugador jugador) {

            // Establecemos el ID del jugador en base al ID de la conexion. Esto nos facilita garantizar la unicidad del ID
            jugador.idJugador = conexion.getID();
            playersInfo.put(jugador.idJugador, jugador);

            server.sendToAllExceptUDP(conexion.getID(), jugador);
        }

        // 3. Un jugador completa una tarea (TCP)
        if (paquete instanceof Red.PaqueteTareaCompletada) {
            ServerPlayer jugador = jugadores.get(conexion.getID());
            if (jugador != null) {
                jugador.tareasCompletadas++;
                System.out.println(jugador.nombre + " completó una tarea (" + jugador.tareasCompletadas + "/" + jugador.tareasTotales + ")");
                verificarFinDeJuego();
            }
        }

        if (paquete instanceof Red.PaqueteSalirLobby) {
            System.out.println("Jugador " + conexion.getID() + " salió del lobby");
            jugadores.remove(conexion.getID()); //

            // Enviamos la señal para que los demas jugadores dejen de renderizarlo
            Red.PaqueteRemoverJugador remove = new Red.PaqueteRemoverJugador();
            remove.idJugador = conexion.getID();
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
            if (expulsado != null) expulsado.killed = true;
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

        Collections.shuffle(idJugadores);
        int idImpostor = idJugadores.get(0); // Como ya los sorteamos, simplemente escogemos el primero

        // Se lo mandamos a cada conexion, es decir, a cada jugador conectado al servidor
        //for (Connection conexion : server.getConnections()) {
        Red.PaqueteIniciarJuego iniciarJuego = new Red.PaqueteIniciarJuego();
        iniciarJuego.mapa = "mapa.tmx";
        // Por ahora lo mandamos a la biblioteca, tenemos que calcular esto mejor al tener dos mapas
        iniciarJuego.inicioX = 512;
        iniciarJuego.inicioY = 384;

        iniciarJuego.esImpostor = conexion.getID() == idImpostor;
        server.sendToAllTCP(iniciarJuego);
        //}

        System.out.println("¡Sala llena! Iniciando partida...");
        //server.sendToAllTCP(new Red.PaqueteIniciarJuego());
    }

    private void verificarFinDeJuego() {
        if (jugadores.isEmpty()) return;

        boolean todosTerminaron = true;

        // Calculamos la cantidad de jugadores electrocutados para ver si el impostor gano
        int jugadoresVivos = jugadores.size();
        boolean jugadoresInsuficientes = false;

        // Revisamos si algún jugador aún no termina sus tareas
        for (ServerPlayer jugador : jugadores.values()) {
            if (jugador.tareasCompletadas < jugador.tareasTotales) {
                todosTerminaron = false;
                break;
            }

            if (jugador.killed) {
                jugadoresVivos--;
            }
        }

        // Si el bucle termina y todosTerminaron sigue siendo true, ¡ganaron!
        if (todosTerminaron) {
            System.out.println("¡Todas las tareas completadas! Fin del juego.");
            Red.PaqueteFinJuego fin = new Red.PaqueteFinJuego();
            fin.mensajeGanador = "¡VICTORIA DE LOS TRIPULANTES!";
            server.sendToAllTCP(fin);

            // Aquí podrías reiniciar las variables para una nueva partida
            juegoIniciado = false;
        } else if (jugadoresVivos < 3) {
            System.out.println("El Impostor ha ganado");
            Red.PaqueteFinJuego fin = new Red.PaqueteFinJuego();


            fin.mensajeGanador = "¡VICTORIA DE LOS IMPOSTORES!";
            server.sendToAllTCP(fin);

            // Aquí podrías reiniciar las variables para una nueva partida
            juegoIniciado = false;
        }
    }

    private void broadcastLobbyStatus() {
        Red.PaqueteLobbyInfo status = new Red.PaqueteLobbyInfo();
        status.conectados = jugadores.size();
        status.puedeEmpezar = jugadores.size() >= 2;
        status.mapaActual = "lobby.tmx";
        server.sendToAllTCP(status);
    }

}


