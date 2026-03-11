package com.uprojects.core;

import com.uprojects.ui.DuctoPane;
import com.uprojects.ui.TareaPane;

public class DuctoTarea extends Tarea {

    public DuctoTarea(int x, int y, int w, int h) {
        super(x, y, w, h, "Ducto");
    }

    @Override
    public TareaPane crearUI() {
        return new DuctoPane();
    }

    @Override
    public void actualizarTarea(double delta) {
        // Los ductos no se completan, así que no sumamos progreso
    }

}
