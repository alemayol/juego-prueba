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
        // Si el progreso que envia la UI es 1.0 (100%) o mayor, completamos la tarea
        if (delta >= 1.0) {
            this.completada = true;
        }


    }
}