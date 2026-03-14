package com.uprojects.ui;

import com.uprojects.core.CodigoNumericoTarea;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CodigoNumericoPane extends TareaPane implements Initializable {

    @FXML
    private Label lblCodigoObjetivo;
    @FXML
    private Label lblCodigoIngresado;
    @FXML
    private Label lblEstado;

    private CodigoNumericoTarea tareaCodigo;

    public CodigoNumericoPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/codigoNumerico.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnCerrar.setOnAction(e -> uiCerrarTarea());
    }

    @Override
    public void actualizarUI() {
        // Requerido por TareaPane, pero lo dejamos vacío

        String ingresado = tareaCodigo.getCodigoIngresado();
        StringBuilder display = new StringBuilder(ingresado);
        while (display.length() < 5) {
            display.append("_");
        }
        lblCodigoIngresado.setText(display.toString());
    }

    @Override
    public void uiAbrirTarea() {
        // Vinculamos la tarea y actualizamos los textos al abrir el panel
        if (tarea instanceof CodigoNumericoTarea) {
            tareaCodigo = (CodigoNumericoTarea) tarea;
            lblCodigoObjetivo.setText("CÓDIGO: " + tareaCodigo.getCodigoObjetivo());
            tareaCodigo.limpiarIngreso();
            actualizarUI();
            lblEstado.setText("");
        }
    }

    @Override
    public void uiCerrarTarea() {
        ocultarTarea(); // Cierra el panel visualmente
    }

    @FXML
    private void handleBotonNumero(ActionEvent event) {
        if (tareaCodigo == null || tareaCodigo.fueCompletada()) return;

        Button btn = (Button) event.getSource();
        String numeroPulsado = btn.getText();

        tareaCodigo.agregarDigito(numeroPulsado);
        actualizarUI();

        // Validar si ya ingresó los 5 números
        if (tareaCodigo.getCodigoIngresado().length() == 5) {
            if (tareaCodigo.verificarCodigo()) {
                lblEstado.setText("CORRECTO");
                lblEstado.setTextFill(Color.GREEN);
                tareaCodigo.actualizarTarea(1.0); // Marca como completada

                if (onTareaCompletada != null) onTareaCompletada.run();

                // Retraso de casi 1 seg para que el jugador vea que ganó antes de cerrarse
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                javafx.application.Platform.runLater(() -> uiCerrarTarea());
                            }
                        }, 800
                );
            } else {
                lblEstado.setText("ERROR");
                lblEstado.setTextFill(Color.RED);
                actualizarUI();
            }
        }
    }


}
