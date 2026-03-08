package com.uprojects.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;

public class GameServer {

    private Server server;
    private final int MAX_JUGADORES = 10;
    private final int TCP_PORT = 54555;
    private final int UDP_PORT = 54777;


    private HashMap<Integer, ServerPlayer> jugadores = new HashMap<>();
    private boolean juegoIniciado = false;

    public GameServer() throws IOException {
        this.server = new Server();

        // Registramos las clases que se transmitiran por la red
        Red.registrar(server.getKryo());

        server.start();
        server.bind(TCP_PORT, UDP_PORT);


        System.out.println("Servidor iniciado. Esperando jugadores...");

        server.addListener(new Listener() {
            @Override
            public void connected(Connection conexion) {
                if (jugadores.size() >= MAX_JUGADORES || juegoIniciado) {
                    conexion.close(); // Rechaza si está lleno o ya empezó
                }
            }

            @Override
            public void disconnected(Connection conexion) {
                jugadores.remove(conexion.getID());
                System.out.println("Jugador desconectado. Total: " + jugadores.size());
                // Si alguien se va, verificamos si los que quedan ya terminaron
                if (juegoIniciado) verificarFinDeJuego();
            }

            @Override
            public void received(Connection conexion, Object paquete) {

                // 1. Un jugador entra a la sala
                if (paquete instanceof Red.PaqueteConexion) {
                    Red.PaqueteConexion pc = (Red.PaqueteConexion) paquete;
                    ServerPlayer nuevoJugador = new ServerPlayer(conexion.getID(), pc.nombreJugador);
                    jugadores.put(conexion.getID(), nuevoJugador);
                    System.out.println(pc.nombreJugador + " se ha unido.");

                    if (jugadores.size() == MAX_JUGADORES) {
                        iniciarJuego();
                    }
                }

                // 2. Un jugador se mueve (UDP)
                if (paquete instanceof Red.PaqueteMovimiento) {
                    Red.PaqueteMovimiento mov = (Red.PaqueteMovimiento) paquete;
                    ServerPlayer jug = jugadores.get(conexion.getID());
                    if (jug != null) {
                        jug.x = mov.x;
                        jug.y = mov.y;

                        // Reenviamos el movimiento a TODOS los demás por UDP (rápido)
                        mov.idJugador = conexion.getID();
                        server.sendToAllExceptUDP(conexion.getID(), mov);
                    }
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
            }
        });

    }

    private void iniciarJuego() {
        juegoIniciado = true;
        System.out.println("¡Sala llena! Iniciando partida...");
        server.sendToAllTCP(new Red.PaqueteIniciarJuego());
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


}
