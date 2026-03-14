package com.uprojects.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;

public class KillHUD extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final long duracionEnfriamiento;
    private long tiempoUltimaKill;
    private boolean esImpostor;

    public KillHUD(long enfriamientoMs) {
        this.duracionEnfriamiento = enfriamientoMs;
        // Este canvas va por encima del GamePane, por eso no necesitamos tanto espacio
        this.canvas = new Canvas(100, 120);
        this.gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        this.setMouseTransparent(true);
        this.setVisible(false);
    }

    public void setImpostor(boolean impostor) {
        this.esImpostor = impostor;
        this.setVisible(impostor);
    }

    public void comenzarEnfriamiento() {
        this.tiempoUltimaKill = System.currentTimeMillis();
    }

    public void resetEnfriamiento() {
        this.tiempoUltimaKill = 0;
    }

    public void actualizarUI() {
        if (!esImpostor) return;

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        long now = System.currentTimeMillis();
        long transcurrido = now - tiempoUltimaKill;

        double x = 20;
        double y = 20;
        double size = 60;

        if (transcurrido < duracionEnfriamiento && tiempoUltimaKill != 0) {
            double progreso = (double) transcurrido / duracionEnfriamiento;
            double angulo = 360 * progreso;

            // Circulo de fondo
            gc.setFill(Color.rgb(0, 0, 0, 0.6));
            gc.fillOval(x, y, size, size);

            // Progreso en rojo
            gc.setFill(Color.rgb(255, 0, 0, 0.5));
            gc.fillArc(x, y, size, size, 90, angulo, ArcType.ROUND);

            // Segundos restantes para poder electrocutar de nuevo
            long restante = (duracionEnfriamiento - transcurrido) / 1000 + 1;
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 20));
            gc.fillText(String.valueOf(restante), x + 22, y + 38);

            gc.setFont(new Font("Arial", 12));
            gc.fillText("TASE", x + 18, y + size + 15);
        } else {
            // De lo contrario, esta listo para electrocutar
            gc.setFill(Color.RED);
            gc.fillOval(x, y, size, size);
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 14));
            gc.fillText("LISTO", x + 10, y + 38);
        }
    }
}
