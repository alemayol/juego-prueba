package com.uprojects.helpers;

import com.uprojects.entities.Player;
import com.uprojects.stages.MapHandler;


public class CollisionChecker {

    private MapHandler mapH;

    public CollisionChecker(MapHandler mh) {
        this.mapH = mh;
    }


    // Determinamos si en una posible proxima posicion podemos salirnos del choque con un objeto (el jugador se clipea y piensa que no puede moverse)
    public boolean estaChocando(int nextX, int nextY, Player jugador) {


        int tilesize = mapH.getTileSize();

        // Coordenadas en una posible proxima posicion
        int leftX = nextX - (tilesize / 2) + jugador.getAreaSolida().x;
        int rightX = leftX + jugador.getAreaSolida().width - 1;
        int topY = nextY - (tilesize / 2) + jugador.getAreaSolida().y;
        int bottomY = topY + jugador.getAreaSolida().height - 1;


        int leftColumna = leftX / tilesize;
        int rightColumna = rightX / tilesize;
        int topFila = topY / tilesize;
        int bottomFila = bottomY / tilesize;

        return mapH.tileSolido(leftColumna, topFila) || mapH.tileSolido(leftColumna, bottomFila) || mapH.tileSolido(rightColumna, topFila) || mapH.tileSolido(rightColumna, bottomFila);
    }


}
