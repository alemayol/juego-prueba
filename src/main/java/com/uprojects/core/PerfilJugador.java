package com.uprojects.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class PerfilJugador {
    
    private String nombre = "Tripulante";
    private String color = "Rojo";
    private final String RUTA_ARCHIVO = "perfil_uneg.xml";

    public PerfilJugador() {
        cargarPerfil();
    }

    public void cargarPerfil() {
        try {
            File archivo = new File(RUTA_ARCHIVO);
            if (archivo.exists()) {
                Properties props = new Properties();
                FileInputStream fis = new FileInputStream(archivo);
                props.loadFromXML(fis);
                
                this.nombre = props.getProperty("nombre", "Tripulante");
                this.color = props.getProperty("color", "Rojo");
                
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void guardarPerfil(String nuevoNombre, String nuevoColor) {
        this.nombre = nuevoNombre;
        this.color = nuevoColor;
        
        try {
            Properties props = new Properties();
            props.setProperty("nombre", this.nombre);
            props.setProperty("color", this.color);
            
            FileOutputStream fos = new FileOutputStream(RUTA_ARCHIVO);
            props.storeToXML(fos, "Perfil del Jugador UNEG");
            
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNombre() { 
        return nombre; 
    }
    
    public String getColor() { 
        return color; 
    }
}