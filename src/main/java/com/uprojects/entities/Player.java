package com.uprojects.entities;

import com.uprojects.core.Tarea;
import com.uprojects.helpers.CollisionChecker;
import com.uprojects.helpers.KeyHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.*;
import java.util.List;
import java.util.Objects;


public class Player extends Entidad {


    private final KeyHandler keyH;
    private CollisionChecker collisionChecker;
    public final int cameraX, cameraY;
    private List<Tarea> tareasAsignadas;


    // IDEA: El jugador recibe el KeyH y un SpawnPoint como argumentos solamente
    // IDEA 2: Crear una clase Camara que maneje todos esos calculos
    public Player(KeyHandler kh, int screenW, int screenH, int tileSize, String name, String color) {

        super(tileSize, name, color);

        this.keyH = kh;
        this.areaSolida = new Rectangle(8, 16, 16, 14);
        this.lastSprite = idleR != null ? idleR[0] : null;
        this.cameraX = screenW / 2 - (tileSize / 2);
        this.cameraY = screenH / 2 - (tileSize / 2);

        this.resetValues();
    }

    public void resetValues() {
        this.worldX = tileSize * 16;
        this.worldY = tileSize * 12;
        this.spd = 6;
        this.paused = false;
        this.killed = false;
        this.accion = "right";
        this.facingTowards = "right";
        this.colision = false;
    }

    public void asignarTareas(List<Tarea> tareas) {
        this.tareasAsignadas = tareas;
    }

    public void updatePosition(CollisionChecker collisionChecker) {


        if (paused || killed) {
            return;
        }

        if (keyH.isMoving()) {
            int dx = 0;
            int dy = 0;

            if (keyH.getUpPressed() && keyH.getRightPressed()) {
                this.accion = "up";
                this.facingTowards = "right";

                int normVector = this.spd / 2;
                dx += normVector;
                dy -= normVector;


            } else if (keyH.getUpPressed() && keyH.getLeftPressed()) {
                this.accion = "up";
                this.facingTowards = "left";

                int normVector = this.spd / 2;
                dx -= normVector;
                dy -= normVector;

            } else if (keyH.getDownPressed() && keyH.getRightPressed()) {
                this.accion = "down";
                this.facingTowards = "right";

                int normVector = this.spd / 2;
                dx += normVector;
                dy += normVector;

            } else if (keyH.getDownPressed() && keyH.getLeftPressed()) {
                this.accion = "down";
                this.facingTowards = "left";

                int normVector = this.spd / 2;
                dx -= normVector;
                dy += normVector;

            } else if (keyH.getUpPressed()) {
                this.accion = "up";
                dy -= spd;
            } else if (keyH.getDownPressed()) {
                this.accion = "down";
                dy += spd;
            } else if (keyH.getLeftPressed()) {
                this.accion = "left";
                this.facingTowards = "left";
                dx -= spd;
            } else if (keyH.getRightPressed()) {
                this.accion = "right";
                this.facingTowards = "right";
                dx += spd;
            }

            // Chequeando si hubo una colision antes de mover al personaje
            colision = false;
            //collisionChecker.checkTile(this);


            if (!collisionChecker.estaChocando(worldX + dx, worldY, this)) {
                this.worldX += dx;
            }

            if (!collisionChecker.estaChocando(worldX, worldY + dy, this)) {
                this.worldY += dy;
            }


            actualizarSprite();
        } else {
            accion = facingTowards.contains("right") ? "idle-r" : "idle-l";

        }


    }

    public void draw(GraphicsContext gc) {


        Image sprite = lastSprite;

        /*
        switch (direction) {
            case "up", "down" -> {
                if (facingTowards.contains("right")) {
                    sprite = this.right[spriteNumber];
                } else {

                    sprite = this.left[spriteNumber];
                }
            }

            case "left" -> {
                sprite = this.left[spriteNumber];
            }
            case "right" -> {
                sprite = this.right[spriteNumber];
            }
            case "idle-r" -> {
                sprite = this.idleR[spriteNumber];
            }
            case "idle-l" -> {
                sprite = this.idleL[spriteNumber];
            }
            case "paused" -> {
                if (facingTowards.contains("right")) {
                    sprite = this.idleR[spriteNumber];
                } else {

                    sprite = this.idleL[spriteNumber];
                }
            }
            case "killed" -> {
                sprite = this.electrocutado[spriteNumber];
            }
            default -> {
                break;
            }
        }

         */

        sprite = getCurrentImage();

        lastSprite = sprite;


        // Depending on the current direction we draw the respective sprite


        //gc.setFill(Color.RED);
        //gc.fillText("Presione E", worldX, worldY - 12, 32);
        gc.drawImage(sprite, worldX - (double) (tileSize / 2), worldY - (double) (tileSize / 2));
    }


    public String getAccion() {
        return this.accion;
    }

    public int getSpd() {
        return this.spd;
    }

    public void setColision(boolean colisiono) {
        this.colision = colisiono;
    }

    public boolean getColision() {
        return this.colision;
    }
}
