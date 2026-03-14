package com.uprojects.ui;

import com.uprojects.core.ArreglarCablesTarea;
import com.uprojects.core.Tarea;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ArreglarCablesPane extends TareaPane implements Initializable {

    @FXML
    private VBox leftBox, rightBox;
    @FXML
    private Pane canvasWires;

    private Line cableActivo; // El cable que se esta trazando actualmente
    private final String[] colors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00"};

    @FXML
    private Label lblProgreso;

    @FXML
    private Pane panelCables;
    @FXML
    private Label lblCompletado;
    @FXML
    private Pane rootPane;

    private final Random random = new Random();

    public ArreglarCablesPane() {
        super();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/styles/arreglarCables.fxml"));
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

        setupWires();

        if (btnCerrar != null) {

            btnCerrar.setOnAction(e -> {
                handleCerrar();
            });
        }
    }


    private void setupWires() {
        leftBox.getChildren().clear();
        rightBox.getChildren().clear();

        // Creamos una lista a partir del arreglo de colores para utilizar la funcion shuffle y agregar un poco de azar al "barajarlos"
        List<String> shuffledColors = new ArrayList<>(List.of(colors));
        Collections.shuffle(shuffledColors);

        for (int i = 0; i < colors.length; i++) {
            createPin(leftBox, colors[i], true);
            createPin(rightBox, shuffledColors.get(i), false);
        }
    }

    private void createPin(VBox container, String color, boolean isSource) {
        Rectangle pin = new Rectangle(30, 15, Color.web(color));
        pin.setUserData(color);

        if (isSource) {
            pin.setOnMousePressed(e -> startWire(e, pin, color));
            pin.setOnMouseDragged(this::arrastrarCable);
            pin.setOnMouseReleased(e -> endWire(e, pin, color));
        }
        container.getChildren().add(pin);
    }

    // Dibujamos el cable y los añadimos al Pane
    private void startWire(MouseEvent e, Rectangle pin, String color) {
        if (tarea.fueCompletada()) return;

        double startX = leftBox.getLayoutX() + pin.getWidth();
        double startY = leftBox.getLayoutY() + pin.getLayoutY() + (pin.getHeight() / 2);

        cableActivo = new Line(startX, startY, startX, startY);
        cableActivo.setStroke(Color.web(color));
        cableActivo.setStrokeWidth(8);
        panelCables.getChildren().add(cableActivo);
    }

    // Arratramos el cable a la posicion del mouse
    private void arrastrarCable(MouseEvent e) {
        if (cableActivo == null) return;
        cableActivo.setEndX(e.getX() + leftBox.getLayoutX());
        cableActivo.setEndY(e.getY() + leftBox.getLayoutY() + ((Node) e.getSource()).getLayoutY());
    }

    private void endWire(MouseEvent e, Rectangle sourcePin, String color) {
        boolean success = false;
        for (Node terminal : rightBox.getChildren()) {
            if (terminal instanceof Rectangle targetPin && targetPin.getUserData().equals(color)) {
                // Revisamos si el cable se encuentra en el terminal del mismo color
                if (terminal.localToScene(terminal.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
                    calzarEnTerminal(targetPin);
                    tarea.actualizarTarea(1); // Incrementamos la cantidad de cables conectados
                    actualizarUI();
                    success = true;
                    break;
                }
            }
        }

        // Si el cable dibujado no cae en la terminal correspondiente, limpiamos el cable dibujado
        if (!success) panelCables.getChildren().remove(cableActivo);
        cableActivo = null;
    }

    // Setteamos la posicion del cable activo en el terminal correspondiente a su derecha
    private void calzarEnTerminal(Rectangle target) {

        double endX = rightBox.getLayoutX();
        double endY = rightBox.getLayoutY() + target.getLayoutY() + (target.getHeight() / 2);

        cableActivo.setEndX(endX);
        cableActivo.setEndY(endY);

        triggerSpark(endX, endY, (Color) target.getFill());
    }


    private void triggerSpark(double x, double y, Color color) {
        int particleCount = 6 + random.nextInt(4);

        for (int i = 0; i < particleCount; i++) {
            Circle spark = new Circle(x, y, 2 + random.nextDouble() * 3, color);
            spark.setMouseTransparent(true);
            panelCables.getChildren().add(spark);

            double angle = Math.toRadians(random.nextInt(360));
            double distance = 20 + random.nextDouble() * 30;
            double targetX = x + Math.cos(angle) * distance;
            double targetY = y + Math.sin(angle) * distance;

            javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(Duration.millis(400), spark);
            move.setToX(Math.cos(angle) * distance);
            move.setToY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.millis(400), spark);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(400), spark);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(0.1);
            scale.setToY(0.1);

            ParallelTransition pt = new ParallelTransition(move, fade, scale);
            pt.setOnFinished(e -> panelCables.getChildren().remove(spark));
            pt.play();
        }
    }

    @Override
    public void actualizarUI() {
        if (tarea instanceof ArreglarCablesTarea t) {
            lblProgreso.setText("Cables: " + t.getCablesConectados() + "/4");
            if (t.fueCompletada()) {
                lblCompletado.setText("Arreglado!");
                if (onTareaCompletada != null) onTareaCompletada.run();
            }
        }
    }

    @Override
    protected void uiCerrarTarea() {

        panelCables.getChildren().clear();
        lblCompletado.setText("");
        setupWires(); // Se resetea para la prox vez
    }

    @FXML
    private void handleCerrar() {
        ocultarTarea(); // Cerrar la UI
        if (onCerrarTarea != null) onCerrarTarea.run();
    }


    @Override
    public void setTarea(Tarea tarea) {
        super.setTarea(tarea);
        actualizarUI();
    }

    @Override
    protected void uiAbrirTarea() {
        actualizarUI();
    }


}
