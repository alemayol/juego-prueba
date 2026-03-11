package com.uprojects.core;

import com.uprojects.entities.Player;
import com.uprojects.ui.TareaPane;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public abstract class Tarea {

    protected boolean completada = false;
    protected int worldX, worldY;
    protected int width, height;
    protected String nombre;
    protected boolean jugadorCerca;
    protected TareaPane uiPane; // Pantalla del minijuego

    public Tarea(int worldX, int worldY, int width, int height, String nombre) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.nombre = nombre;
        this.jugadorCerca = false;
    }

    public void update(Player jugador) {

        double centroX = worldX + (width / 2.0);
        double centroY = worldY + (height / 2.0);

        double distancia = Math.hypot(centroX - jugador.getWorldX(), centroY - jugador.getWorldY());

        this.jugadorCerca = (distancia < 16 + (Math.max(width, height) / 2.0));
    }

    public boolean getJugadorCerca() {
        return this.jugadorCerca;
    }

    public void drawInteractionBox(GraphicsContext gc) {

        if (this.jugadorCerca && !this.completada) {
            gc.setStroke(Color.YELLOW); // Green if we can interact
            gc.setLineWidth(3);
            gc.setFill(Color.WHITE);
            gc.fillText(
                    "Presione 'E'",
                    this.worldX,
                    this.worldY - 8
            );
        }

        // 2. Draw the bounding box
        // NOTE: Use absolute world coordinates
        gc.strokeRect(
                this.worldX,
                this.worldY,
                this.width,
                this.height
        );


    }

    public int getWorldX() {
        return this.worldX;
    }

    public int getWorldY() {
        return this.worldY;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setUiPane(TareaPane uiPane) {
        this.uiPane = uiPane;
        uiPane.setTarea(this);
    }

    public TareaPane getUiPane() {
        return this.uiPane;
    }

    public abstract TareaPane crearUI();

    ;

    public void comenzarTarea() {
        this.completada = false;
    }

    public void cerrarTarea() {
    }

    public boolean fueCompletada() {
        return this.completada;
    }

    public abstract void actualizarTarea(double progreso);

    public void reset() {
        this.completada = false;
    }
}
