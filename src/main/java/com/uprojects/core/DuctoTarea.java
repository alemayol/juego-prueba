package com.uprojects.core;

import com.uprojects.ui.DuctoPane;
import com.uprojects.ui.TareaPane;

public class DuctoTarea extends Tarea {

    private boolean impostorAqui;

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

    public boolean getImpostorAqui() {
        return this.impostorAqui;
    }

    public void setImpostorAqui(boolean dentro) {
        this.impostorAqui = dentro;
    }

}
