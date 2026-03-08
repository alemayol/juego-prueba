package com.uprojects.core;

import com.uprojects.ui.CodigoNumericoPane;
import com.uprojects.ui.TareaPane;
import java.util.Random;

public class CodigoNumericoTarea extends Tarea {
    
    private String codigoObjetivo;
    private String codigoIngresado = "";

    public CodigoNumericoTarea(int x, int y, int w, int h) {
        super(x, y, w, h, "Ingresar Código ID");
        generarNuevoCodigo();
    }

    @Override
    public TareaPane crearUI() {
        return new CodigoNumericoPane();
    }

    @Override
    public void actualizarTarea(double delta) {
        // No necesitamos lógica constante aquí, los botones manejan la acción
    }

    private void generarNuevoCodigo() {
        Random rand = new Random();
        // Genera un código de 5 dígitos (ej. 04821)
        this.codigoObjetivo = String.format("%05d", rand.nextInt(100000));
    }

    public String getCodigoObjetivo() { return codigoObjetivo; }
    public String getCodigoIngresado() { return codigoIngresado; }

    public void agregarDigito(String digito) {
        if (codigoIngresado.length() < 5 && !completada) {
            codigoIngresado += digito;
        }
    }

    public void limpiarIngreso() { codigoIngresado = ""; }

    public boolean verificarCodigo() {
        if (codigoIngresado.equals(codigoObjetivo)) {
            return true; 
        } else {
            limpiarIngreso(); // Borra lo escrito si se equivoca
            return false;
        }
    }
}