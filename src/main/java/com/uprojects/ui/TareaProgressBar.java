package com.uprojects.ui;

import javafx.scene.control.ProgressBar;

public class TareaProgressBar extends ProgressBar {

    private int tareasTotales;

    // La barra de progreso debe comienzar oculta en el lobby
    public TareaProgressBar() {
        super(0); // Comienza en 0, o sea no hay progreso

        this.tareasTotales = 0;

        this.setPrefWidth(300);
        this.setPrefHeight(25);
        this.setTranslateX(20);
        this.setTranslateY(20);
        this.setStyle("-fx-accent: #2ecc71; -fx-control-inner-background: #34495e; -fx-background-color: transparent; -fx-border-color: #2ecc71; -fx-border-width: 2;");
        this.setVisible(false);
    }

    public void setTareasTotales(int tareas) {
        this.tareasTotales = tareas;
    }

    public void actualizarUI(int tareasRestantes) {
        if (tareasTotales > 0) {
            double progreso = (double) (tareasTotales - tareasRestantes) / tareasTotales;
            this.setProgress(progreso);
        }
    }
}
