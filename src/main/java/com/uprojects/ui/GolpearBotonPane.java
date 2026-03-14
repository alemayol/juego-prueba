package com.uprojects.ui;

import com.uprojects.core.GolpearBotonTarea;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;

public class GolpearBotonPane extends TareaPane implements Initializable {

    @FXML
    private ProgressBar progressMotor;
    @FXML
    private Button btnMash;

    public GolpearBotonPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/golpearBoton.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        btnMash.setOnAction(e -> {
            if (tarea != null) {
                tarea.actualizarTarea(1);
                // Efecto de presionado
                btnMash.setScaleX(1.1);
                btnMash.setScaleY(1.1);
                new PauseTransition(javafx.util.Duration.millis(50))
                        .setOnFinished(ev -> {
                            btnMash.setScaleX(1.0);
                            btnMash.setScaleY(1.0);
                        });
            }
        });
    }

    @Override
    public void actualizarUI() {
        if (tarea instanceof GolpearBotonTarea t) {
            double progreso = (double) t.getClicsActuales() / t.getClicsRequeridos();
            progressMotor.setProgress(progreso);

            if (t.fueCompletada()) {
                btnMash.setDisable(true);
                if (onTareaCompletada != null) onTareaCompletada.run();
            }
        }
    }


    @Override
    protected void uiAbrirTarea() {
        actualizarUI();
    }

    @Override
    protected void uiCerrarTarea() {

        System.out.println("Deberia cerrarse...");
        System.out.println("onCerrarTarea callback is null: " + (onCerrarTarea == null));

        this.setVisible(false);
        this.setDisable(true);
    }

    @FXML
    private void handleCerrar() {
        if (onCerrarTarea != null) onCerrarTarea.run();
    }
}
