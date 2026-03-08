package com.uprojects.entities;

import com.uprojects.helpers.CollisionChecker;
import javafx.scene.canvas.GraphicsContext;

public class RemotePlayer extends Entidad {

    private double targetX, targetY; // Positions received from network

    public RemotePlayer(int tileSize, String nombre, String color) {
        super(tileSize, nombre, color);
    }

    public void updateFromNetwork(float netX, float netY, String netAcc) {
        this.targetX = netX;
        this.targetY = netY;
        this.accion = netAcc;
    }

    @Override
    public void updatePosition(CollisionChecker cc) {
        // Interpolation (lerp) to prevent stuttering
        worldX += (targetX - worldX) * 0.1;
        worldY += (targetY - worldY) * 0.1;

        if (Math.abs(targetX - worldX) > 1 || Math.abs(targetY - worldY) > 1) {
            actualizarSprite();
        }
    }

    public void draw(GraphicsContext gc, Player localPlayer) {
        // Remote players draw relative to the local player's world position
        double screenX = worldX - localPlayer.worldX + localPlayer.cameraX;
        double screenY = worldY - localPlayer.worldY + localPlayer.cameraY;

        gc.drawImage(getCurrentImage(), screenX, screenY);

        // Bonus: Draw name tag since we have the 'nombre' property
        gc.fillText(nombre, screenX, screenY - 10);
    }
}
