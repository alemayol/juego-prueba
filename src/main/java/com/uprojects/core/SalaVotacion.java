package com.uprojects.core;

import com.uprojects.ui.GolpearBotonPane;
import com.uprojects.ui.TareaPane;
import com.uprojects.ui.VotacionPane;

public class SalaVotacion extends Tarea {


    public SalaVotacion(int x, int y, int w, int h) {
        super(x, y, w, h, "Sala-de-Votacion");
    }

    @Override
    public TareaPane crearUI() {
        return new VotacionPane();
    }

    @Override
    public void actualizarTarea(double valor) {


    }


}
