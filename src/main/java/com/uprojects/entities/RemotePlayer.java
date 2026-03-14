package com.uprojects.entities;

import com.uprojects.helpers.CollisionChecker;
import javafx.scene.canvas.GraphicsContext;

public class RemotePlayer extends Entidad {

    private double targetX, targetY;

    public RemotePlayer(int tileSize, String nombre, String color) {
        super(tileSize, nombre, color);
    }


    @Override
    public void actualizarPosicion(CollisionChecker cc) {
        // Interpolacion (lerp) para evitar que sus movimientos sean poco fluidos
        worldX += (int) ((targetX - worldX) * 0.1);
        worldY += (int) ((targetY - worldY) * 0.1);

        if (Math.abs(targetX - worldX) > 1 || Math.abs(targetY - worldY) > 1) {
            actualizarSprite();
        }
    }

    public void draw(GraphicsContext gc) {

        if (this.oculto)
            return;


        // Sprite y nombre del jugador
        gc.drawImage(getCurrentImage(), worldX, worldY);
        gc.fillText(nombre, worldX, worldY - 10);
    }

    public void setTargets(double x, double y) {
        this.targetX = x;
        this.targetY = y;
    }


}
