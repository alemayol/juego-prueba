package com.uprojects.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class FogOfWar {
    private double campoVision;

    public FogOfWar(double initialVisionRadius) {
        this.campoVision = initialVisionRadius;
    }

    public void setCampoVision(double visionRadius) {
        this.campoVision = visionRadius;
    }

    public double getCampoVision() {
        return this.campoVision;
    }


    public void render(GraphicsContext gc, double width, double height) {

        // Calculamos el centro del jugador, como cuando lo dibujamos
        double jugadorX = width / 2.0;
        double jugadorY = height / 2.0;


        // Gradiente radial (transparente en el centro y negro en los bordes)
        RadialGradient niebla = new RadialGradient(
                0, 0, // angulo, distancia
                jugadorX, jugadorY,
                campoVision,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.6, Color.rgb(0, 0, 0, 0.4)), // Start fading
                new Stop(1.0, Color.rgb(0, 0, 0, 0.98)) // Almost fully opaque
        );

        gc.setFill(niebla);
        gc.fillRect(0, 0, width, height);
    }
}
