package com.uprojects.server;

import com.esotericsoftware.kryo.Kryo;

public class Red {

    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;

    // Método para registrar todas las clases en el mismo orden exacto
    public static void registrar(Kryo kryo) {
        kryo.register(PaqueteConexion.class);
        kryo.register(PaqueteIniciarJuego.class);
        kryo.register(PaqueteActualizarJugador.class);
        kryo.register(PaqueteTareaCompletada.class);
        kryo.register(PaqueteFinJuego.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(PaqueteLobbyInfo.class);
        kryo.register(PaqueteSalirLobby.class);
        kryo.register(PaqueteRemoverJugador.class);
        kryo.register(PaquetePedirInicio.class);
    }

    // Clases que viajan por la red
    public static class PaqueteConexion {
        public int idJugador;
        public String nombreJugador;
        public String colorJugador;
    }

    public static class PaquetePedirInicio {
        public String mapa;
    }

    // Server -> All: Envia la lista de personas actualmente en el lobby
    public static class PaqueteLobbyInfo {
        //public java.util.ArrayList<String> nombres;
        public int conectados;
        public boolean puedeEmpezar; // True si hay >= 5 jugadores
        public String mapaActual;
    }

    public static class PaqueteSalirLobby {
    }

    public static class PaqueteIniciarJuego {
        public int inicioX;
        public int inicioY;
        public String mapa; // path -> mapa1.tmx
        public boolean esImpostor;
    }

    public static class PaqueteActualizarJugador {
        public int idJugador;
        public String nombre;
        public String color;
        public float x, y; // Coordenadas
        public String accion;
        public String facingTowards;
        public int spriteNumber;
        public boolean killed;
    }

    public static class PaqueteRemoverJugador {
        public int idJugador;
    }

    public static class PaqueteTareaCompletada {
        public int idJugador;
        // Puedes añadir el ID de la tarea si necesitas saber cuál hizo
    }

    public static class PaqueteFinJuego {
        public String mensajeGanador;
    }


}
