package com.uprojects.helpers;

import com.uprojects.core.Tarea;
import com.uprojects.entities.Player;
import com.uprojects.stages.MapHandler;

import java.util.List;

public class CollisionChecker {

    private MapHandler mapH;

    public CollisionChecker(MapHandler mh) {
        this.mapH = mh;
    }

    public void checkTile(Player jugador) {

        int playerLeftX = jugador.getWorldX() + jugador.getAreaSolida().x;
        int playerRightX = jugador.getWorldX() + jugador.getAreaSolida().x + jugador.getAreaSolida().width;
        int playerTopY = jugador.getWorldY() + jugador.getAreaSolida().y;
        int playerBottomY = jugador.getWorldY() + jugador.getAreaSolida().y + jugador.getAreaSolida().height;

        int playerLeftColumna = playerLeftX / mapH.getTileSize();
        int playerRightColumna = playerRightX / mapH.getTileSize();
        int playerTopFila = playerTopY / mapH.getTileSize();
        int playerBottomFila = playerBottomY / mapH.getTileSize();


        int tileNum1, tileNum2;
        int tilesize = mapH.getTileSize();

        switch (jugador.getAccion()) {
            case "up" -> {
                int topFilafutura = (playerTopY - jugador.getSpd()) / tilesize;

                if (mapH.tileSolido(playerLeftColumna, topFilafutura) || mapH.tileSolido(playerRightColumna, topFilafutura)) {
                    jugador.setColision(true);
                }
            }
            case "down" -> {
                int bottomFilaFutura = (playerBottomY + jugador.getSpd()) / tilesize;

                if (mapH.tileSolido(playerLeftColumna, bottomFilaFutura) || mapH.tileSolido(playerRightColumna, bottomFilaFutura)) {
                    jugador.setColision(true);
                }
            }
            case "left" -> {
                int leftColumnaFutura = (playerLeftX - jugador.getSpd()) / tilesize;

                if (mapH.tileSolido(leftColumnaFutura, playerTopFila) || mapH.tileSolido(leftColumnaFutura, playerBottomFila)) {
                    jugador.setColision(true);
                }
            }
            case "right" -> {
                int rightColumnaFutura = (playerRightX + jugador.getSpd()) / tilesize;

                if (mapH.tileSolido(rightColumnaFutura, playerTopFila) || mapH.tileSolido(rightColumnaFutura, playerBottomFila)) {
                    jugador.setColision(true);
                }
            }
        }
    }

    // Determinamos si en una posible proxima posicion podemos salirnos del choque con un objeto (el jugador se clipea y piensa que no puede moverse)
    public boolean estaChocando(int nextX, int nextY, Player jugador) {


        int tilesize = mapH.getTileSize();

        // Coordenadas en una posible proxima posicion
        int leftX = nextX - (tilesize / 2) + jugador.getAreaSolida().x;
        //int rightX = nextX - (tilesize / 2) + jugador.areaSolida.x + jugador.areaSolida.width;
        int rightX = leftX + jugador.getAreaSolida().width - 1;
        int topY = nextY - (tilesize / 2) + jugador.getAreaSolida().y;
        //int bottomY = nextY - (tilesize / 2) + jugador.areaSolida.y + jugador.areaSolida.height;
        int bottomY = topY + jugador.getAreaSolida().height - 1;


        int leftColumna = leftX / tilesize;
        int rightColumna = rightX / tilesize;
        int topFila = topY / tilesize;
        int bottomFila = bottomY / tilesize;

        return mapH.tileSolido(leftColumna, topFila) || mapH.tileSolido(leftColumna, bottomFila) || mapH.tileSolido(rightColumna, topFila) || mapH.tileSolido(rightColumna, bottomFila);
    }


}
