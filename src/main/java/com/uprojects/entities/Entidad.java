package com.uprojects.entities;

import com.uprojects.helpers.CollisionChecker;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.awt.*;
import java.util.Objects;

public abstract class Entidad {


    //Estado de la entidad
    protected String nombre, color;
    protected int ID;
    protected boolean paused, killed, oculto;
    protected boolean colision;
    protected int tareasTotales = 4;
    protected int tareasCompletadas = 0;

    // Posicion
    protected int worldX, worldY;
    protected int tileSize;
    protected int spd;
    protected Rectangle areaSolida;

    // Sprites
    private static final String[] colores = {"Amarillo", "Azul", "AzulClaro", "Gris", "Morado", "Naranja", "Rojo", "Rosado", "Verde", "VerdeOscuro"};
    protected Image[] left, right, idleR, idleL, attackR, attackL, electrocutadoR, electrocutadoL;
    protected Image lastSprite;
    protected String accion, facingTowards;
    protected int spriteCounter = 0;
    protected int spriteNumber = 0;

    public Entidad(int tileSize, String nombre, String color) {
        this.tileSize = tileSize;
        this.nombre = nombre;

        this.areaSolida = new Rectangle(8, 16, 16, 14);

        this.left = new Image[2];
        this.right = new Image[2];
        this.idleL = new Image[2];
        this.idleR = new Image[2];
        this.electrocutadoL = new Image[2];
        this.electrocutadoR = new Image[2];
        this.attackL = new Image[2];
        this.attackR = new Image[2];

        this.accion = "up";
        this.facingTowards = "right";
        this.oculto = false;
        this.paused = false;
        this.killed = false;

        cargarSprites(color);
    }


    public void cargarSprites(String color) {
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


                if (this.attackL != null)
                    this.attackL[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Izquierda/Attack/" + (i + 1) + ".png")));


                if (this.attackR != null)
                    this.attackR[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/units/" + color + "/Derecha/Attack/" + (i + 1) + ".png")));

            }


        } catch (IllegalArgumentException e) {
            System.out.println("Bad reading of sprite");
            e.printStackTrace();
        }

    }

    protected void actualizarSprite() {
        spriteCounter++;

        if (spriteCounter > 12) {

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
            case "idle" -> facingTowards.equals("right") ? idleR[spriteNumber] : idleL[spriteNumber];
            case "killed" ->
                    facingTowards.equals("right") ? electrocutadoR[spriteNumber] : electrocutadoL[spriteNumber];
            case "up", "down" -> facingTowards.equals("right") ? right[spriteNumber] : left[spriteNumber];
            case "attacking" -> facingTowards.equals("right") ? attackR[spriteNumber] : attackL[spriteNumber];
            default -> idleR[spriteNumber];
        };
    }


    public abstract void actualizarPosicion(CollisionChecker collisionChecker);

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

    public String getColor() {
        return this.color;
    }

    public void setColor(String nuevoColor) {

        for (String color : colores) {
            if (color.equals(nuevoColor)) {
                this.color = nuevoColor;
                return;
            }
        }
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

    public void setKilled(boolean electrocutado) {
        this.killed = electrocutado;
    }

    public boolean wasKilled() {
        return this.killed;
    }

    public void setId(int id) {
        this.ID = id;
    }

    public int getID() {
        return this.ID;
    }

    public void setPaused(boolean pausado) {
        this.paused = pausado;
    }

    public boolean isOculto() {
        return oculto;
    }

    public void setOculto(boolean oculto) {
        this.oculto = oculto;
    }
}
