package com.uprojects.server;

import com.esotericsoftware.kryo.Kryo;

public class Red {

    // Método para registrar todas las clases en el mismo orden exacto
    public static void registrar(Kryo kryo) {
        kryo.register(PaqueteConexion.class);
        kryo.register(PaqueteIniciarJuego.class);
        kryo.register(PaqueteMovimiento.class);
        kryo.register(PaqueteTareaCompletada.class);
        kryo.register(PaqueteFinJuego.class);
    }

    // Clases que viajan por la red
    public static class PaqueteConexion {
        public String nombreJugador;
    }

    public static class PaqueteIniciarJuego {
    }

    public static class PaqueteMovimiento {
        public int idJugador;
        public float x, y; // Coordenadas
    }

    public static class PaqueteTareaCompletada {
        public int idJugador;
        // Puedes añadir el ID de la tarea si necesitas saber cuál hizo
    }

    public static class PaqueteFinJuego {
        public String mensajeGanador;
    }



}
