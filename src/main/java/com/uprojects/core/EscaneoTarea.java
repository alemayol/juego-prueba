package com.uprojects.core;

import com.uprojects.ui.EscaneoPane;
import com.uprojects.ui.TareaPane;

public class EscaneoTarea extends Tarea {

    public EscaneoTarea(int x, int y, int w, int h) {
        super(x, y, w, h, "Escaneo de MedBay");
    }

    // ¡Este era el método que faltaba!
    @Override
    public TareaPane crearUI() {
        return new EscaneoPane();
    }

    @Override
    public void actualizarTarea(double delta) {
        // En este caso no necesitamos lógica aquí porque el UI maneja el tiempo
        /*
        if (delta >= 1.0) {
            super.actualizarTarea(delta);
        }

         */

    }
}