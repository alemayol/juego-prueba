package com.uprojects.server;

public class ServerPlayer {

    public int id;
    public String nombre;
    public float x, y;
    public int tareasCompletadas = 0;
    public int tareasTotales = 4; // Maximo de tareas del jugador
    public boolean killed;

    public ServerPlayer(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.killed = false;
    }

}
