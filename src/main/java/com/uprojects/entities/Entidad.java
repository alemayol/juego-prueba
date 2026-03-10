package com.uprojects.entities;

import com.uprojects.helpers.CollisionChecker;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.awt.*;
import java.util.Objects;

public abstract class Entidad {


    //Estado de la entidad
    protected String nombre;
    protected int ID;
    protected boolean paused, killed;
    protected boolean colision;
    protected int tareasTotales = 4;
    protected int tareasCompletadas = 0;

    // Posicion
    protected int worldX, worldY;
    protected int tileSize;
    protected int spd;
    protected Rectangle areaSolida;

    // Sprites
    protected Image[] left, right, idleR, idleL, attack, electrocutadoR, electrocutadoL;
    protected Image lastSprite;
    protected String accion, facingTowards;
    protected int spriteCounter = 0;
    protected int spriteNumber = 0;

    public Entidad(int tileSize, String nombre, String color) {
        this.tileSize = tileSize;
        this.nombre = nombre;

        this.areaSolida = new Rectangle(8, 16, 16, 14); // Your fixed hitbox

        // Initialize arrays
        this.left = new Image[2];
        this.right = new Image[2];
        this.idleL = new Image[2];
        this.idleR = new Image[2];
        this.electrocutadoL = new Image[2];
        this.electrocutadoR = new Image[2];

        this.accion = "up";
        this.facingTowards = "right";

        cargarSprites(color);
    }


    protected void cargarSprites(String color) {
        try {

            for (int i = 0; i < 2; i++) {

                if (this.left != null)
                    this.left[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Izquierda/Walk/" + (i + 1) + ".png")));


                if (this.right != null)
                    this.right[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Derecha/Walk/" + (i + 1) + ".png")));


                if (this.idleL != null)
                    this.idleL[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Izquierda/Idle/" + (i + 1) + ".png")));


                if (this.idleR != null)
                    this.idleR[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Derecha/Idle/" + (i + 1) + ".png")));


                if (this.electrocutadoL != null)
                    this.electrocutadoL[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Izquierda/Killed/" + (i + 1) + ".png")));


                if (this.electrocutadoR != null)
                    this.electrocutadoR[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Derecha/Killed/" + (i + 1) + ".png")));

            }


        } catch (IllegalArgumentException e) {
            System.out.println("Bad reading of sprite");
            e.printStackTrace();
        }

    }

    protected void actualizarSprite() {
        spriteCounter++;

        if (spriteCounter > 8) {

            spriteNumber = (spriteNumber + 1) % 2;
            spriteCounter = 0;
        }
    }

    protected Image getCurrentImage() {

        if (accion == null) accion = "idle-r";
        if (facingTowards == null) facingTowards = "right";
        return switch (accion) {
            case "left" -> left[spriteNumber];
            case "right" -> right[spriteNumber];
            case "idle-l" -> idleL[spriteNumber];
            case "idle-r" -> idleR[spriteNumber];
            case "killed" ->
                    facingTowards.equals("right") ? electrocutadoR[spriteNumber] : electrocutadoL[spriteNumber];
            case "up", "down" -> facingTowards.equals("right") ? right[spriteNumber] : left[spriteNumber];
            default -> idleR[spriteNumber];
        };
    }


    public abstract void updatePosition(CollisionChecker collisionChecker);

    public int getWorldX() {
        return this.worldX;
    }

    public int getWorldY() {
        return this.worldY;
    }

    public Rectangle getAreaSolida() {
        return this.areaSolida;
    }

    public String getFacingTowards() {
        return this.facingTowards;
    }

    public void setFacingTowards(String facingTowards) {
        this.facingTowards = facingTowards;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void aumentarTareasCompletadas() {
        this.tareasCompletadas++;
    }

    public int getTareasCompletadas() {
        return this.tareasCompletadas;
    }

    public int getTareasTotales() {
        return this.tareasTotales;
    }

    public boolean isKilled() {
        return this.killed;
    }

    public void setId(int id) {
        this.ID = id;
    }

    public int getID() {
        return this.ID;
    }
}
