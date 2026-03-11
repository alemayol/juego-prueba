package com.uprojects.helpers;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.util.HashSet;
import java.util.Set;

public class KeyHandler {

    protected boolean upPressed, downPressed, leftPressed, rightPressed;
    protected Set<KeyCode> pressedKeys;
    private Runnable interactionListener;

    public KeyHandler(Scene scene) {

        pressedKeys = new HashSet<KeyCode>();


        /*
        scene.setOnKeyPressed((KeyEvent e) -> {
            this.pressedKeys.add(e.getCode());
        });
        scene.setOnKeyReleased((KeyEvent e) -> {
            this.pressedKeys.remove(e.getCode());
        });

         */
    }

    public void setInteractionListener(Runnable interaction) {
        this.interactionListener = interaction;
    }

    public void anclarScene(Scene scene) {

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    this.pressedKeys.add(e.getCode());

                    /*
                    if (e.getCode() == KeyCode.E) {
                        if (interactionListener != null) {
                            this.interactionListener.run();
                        }
                    }

                     */
                }
        );
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> this.pressedKeys.remove(e.getCode()));
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
