package com.uprojects.helpers;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.security.Key;
import java.util.HashSet;
import java.util.Set;

public class KeyHandler {

    protected boolean upPressed, downPressed, leftPressed, rightPressed;
    protected Set<KeyCode> pressedKeys;
    protected Set<KeyCode> consumedKeys;
    private Runnable interactionListener;

    public KeyHandler(Scene scene) {

        pressedKeys = new HashSet<KeyCode>();
        consumedKeys = new HashSet<KeyCode>();


    }

    public void setInteractionListener(Runnable interaction) {
        this.interactionListener = interaction;
    }

    public void anclarScene(Scene scene) {

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    this.pressedKeys.add(e.getCode());


                }
        );
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
                    this.pressedKeys.remove(e.getCode());
                    this.consumedKeys.remove(e.getCode());
                }
        );
    }

    private boolean apenasPresionada(KeyCode keyCode) {
        // Si esta presionada pero no la hemos procesado, aceptamos la tecla. Sino significa que ya la utilizó antes
        // Esto es para que no spamee teclas al atacar y activar tareas
        if (pressedKeys.contains(keyCode) && !consumedKeys.contains(keyCode)) {
            consumedKeys.add(keyCode);
            return true;
        }

        return false;
    }

    public boolean isMoving() {
        return pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.S) || pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.D);
    }

    public boolean accionarTarea() {
        return pressedKeys.contains(KeyCode.E);
    }

    public boolean electrocutar() {
        return pressedKeys.contains(KeyCode.F);
    }

    public void resetPressedKeys() {
        this.pressedKeys.clear();
    }

    public boolean getUpPressed() {
        return this.pressedKeys.contains(KeyCode.W);
    }

    public boolean getDownPressed() {
        return this.pressedKeys.contains(KeyCode.S);
    }

    public boolean getLeftPressed() {
        return this.pressedKeys.contains(KeyCode.A);
    }

    public boolean getRightPressed() {
        return this.pressedKeys.contains(KeyCode.D);
    }
}
