package com.uprojects.ui;

import com.uprojects.core.EscaneoTarea;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EscaneoPane extends TareaPane implements Initializable {

    @FXML private ProgressBar progressEscaner;
    @FXML private Label lblDatosEscaneo;
    
    private Timeline timeline;
    private double progresoActual = 0.0;

    public EscaneoPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/escaneo.fxml"));
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
        
        // Configuramos la animación de 5 segundos (actualiza cada 100ms)
        timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            progresoActual += 0.02; // 2% cada 100ms = 5 segundos total
            progressEscaner.setProgress(progresoActual);
            
            // Simular datos cambiando
            if (progresoActual < 0.3) lblDatosEscaneo.setText("Analizando ID...");
            else if (progresoActual < 0.6) lblDatosEscaneo.setText("Comprobando tipo de sangre...");
            else if (progresoActual < 0.9) lblDatosEscaneo.setText("Verificando peso...");
            else lblDatosEscaneo.setText("Escaneo completado.");

            if (progresoActual >= 1.0) {
                timeline.stop();
                completarEscaneo();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }
    @Override
    public void actualizarUI() {
        // Tu TareaPane exige este método, pero lo dejamos vacío 
        // porque nuestro Timeline ya se encarga de animar la barra.
    }

    @Override
    public void uiAbrirTarea() {
        // Quitamos el super.uiAbrirTarea();
        progresoActual = 0.0;
        progressEscaner.setProgress(0);
        lblDatosEscaneo.setText("Iniciando escáner...");
        timeline.play(); // Iniciar escaneo al abrir
    }

    @Override
    public void uiCerrarTarea() {
        // Quitamos el super.uiCerrarTarea();
        if (timeline != null) timeline.stop(); // Pausar si el jugador se va
        ocultarTarea(); // Usamos el método de tu clase base para esconder el panel
    }

    private void completarEscaneo() {
        if (tarea != null) {
            tarea.actualizarTarea(1.0); // Marca progreso al máximo
            // El atributo "completada" está en la clase Tarea
            
            if (tarea.fueCompletada()) {
                if (onTareaCompletada != null) {
                    onTareaCompletada.run();
                }
                System.out.println("Escaneo finalizado!");
                
                // Darle un segundo al jugador para ver el 100% antes de cerrar
                Timeline delayCierre = new Timeline(new KeyFrame(Duration.seconds(1), ev -> uiCerrarTarea()));
                delayCierre.play();
            }
        }
    }
}
