package com.uprojects.helpers;

import com.uprojects.core.*;

public class TareaFactory {

    public static Tarea crearTarea(String type, int worldX, int worldY, int tareaWidth, int tareaHeight) {
        switch (type) {
            case "cables" -> {
                return new ArreglarCablesTarea(worldX, worldY, tareaWidth, tareaHeight);
            }
            case "codigo" -> {
                return new CodigoNumericoTarea(worldX, worldY, tareaWidth, tareaHeight);
            }
            case "escaneo" -> {
                return new EscaneoTarea(worldX, worldY, tareaWidth, tareaHeight);
            }
            case "motor" -> {
                return new GolpearBotonTarea(worldX, worldY, tareaWidth, tareaHeight);
            }
            case "votacion" -> {
                return new SalaVotacion(worldX, worldY, tareaWidth, tareaHeight);
            }
            case "ducto" -> {
                return new DuctoTarea(worldX, worldY, tareaWidth, tareaHeight);
            }
            default -> {
                return null;
            }
        }
    }
}
