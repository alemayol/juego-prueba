package com.uprojects.stages;


import com.esotericsoftware.kryo.io.Input;
import com.uprojects.core.ArreglarCablesTarea;
import com.uprojects.core.Tarea;
import com.uprojects.entities.Player;
import com.uprojects.helpers.TareaFactory;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.mapeditor.core.*;
import org.mapeditor.io.TMXMapReader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;

public class MapHandler {

    private Map map;
    private Player player;
    private HashMap<Tile, Image> tileImages;


    public MapHandler(Player player1) {

        this.loadMapFile();
        this.player = player1;
        this.tileImages = new HashMap<>();
        this.buffToFxImage();

    }


    public void loadMapFile() {

        String cwd = System.getProperty("user.dir");

        try {


            TMXMapReader mapReader = new TMXMapReader();


            this.map = mapReader.readMap("src/main/resources/maps/mapa.tmx");

            //this.map = mapReader.readMap("src/main/resources/maps/test3.tmx");

            if (this.map == null) {
                System.out.println("MAP NOT LOADED");
            } else {
                System.out.println("Map Loaded Successfully!");
                System.out.println("Dimensions: " + map.getWidth() + "x" + map.getHeight());
                System.out.println("Layers found: " + map.getLayerCount());
                System.out.println("Tilesets found: " + map.getTileSets().size());
            }

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void buffToFxImage() {

        if (this.map == null) {
            System.out.println("[ADVERTENCIA] El mapa no ha sido cargado. No puedo extraer los tiles");
            return;
        }

        try {


            int tileW = map.getTileWidth();
            int tileH = map.getTileHeight();

            String dir = new File("src/main/resources/maps/mapa.tmx").getParent();
            int tsIndex = 0;
            for (TileSet ts : this.map.getTileSets()) {


                //InputStream imgSource = ts.getImageData().getSource().getClass().getResourceAsStream("/maps/");
                //InputStream imgSource = getClass().getResourceAsStream("/maps/tilesets/tileset.png");

                System.out.println("Processing TileSet: " + ts.getName());

                // Try getTilebmpFile() first
                String imagePath = ts.getTilebmpFile();

                if (imagePath == null) {
                    imagePath = "/maps/tilesets/" + ts.getName() + ".png";
                }

                /*
                // Fallback: manually parse TMX
                if (imagePath == null || imagePath.isEmpty()) {
                    System.out.println("  getTilebmpFile() returned null, parsing TMX manually...");
                    imagePath = extractTilesetImagePath(dir, tsIndex);
                }

                if (imagePath == null || imagePath.isEmpty()) {
                    System.out.println("  [SKIP] TileSet sin imagen: " + ts.getName());
                    tsIndex++;
                    continue;
                }

                System.out.println("  Image path: " + imagePath);
*/

                InputStream imgSrc = getClass().getResourceAsStream(imagePath);

                if (imgSrc == null) {
                    System.out.println("  [ERROR] No se encontró: " + imagePath);
                    System.out.println("  Available resources: Check src/main/resources/" + imagePath);
                    tsIndex++;
                    continue;
                }

                BufferedImage imgSet = ImageIO.read(imgSrc);


                // Calculate how many columns are in the tileset image
                int columns = imgSet.getWidth() / tileW;


                for (Tile tile : ts) {
                    if (tile == null) continue;

                    int id = tile.getId();


                    int tileXPos = (id % columns) * tileW;
                    int tileYPos = (id / columns) * tileH;

                    BufferedImage singleTileImg = imgSet.getSubimage(tileXPos, tileYPos, tileW, tileH);
                    Image fxImg = SwingFXUtils.toFXImage(singleTileImg, null);
                    tileImages.put(tile, fxImg);


                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Image getFxImage(Tile tile) {

        if (tile == null) {
            return null;
        }

        /*
        int id = tile.getId();

        if (!tileImages.containsKey(id)) {
            BufferedImage awtImg = (BufferedImage) tile.getImage();
            Image fxImg = SwingFXUtils.toFXImage(awtImg, null);
            this.tileImages.put(tile, fxImg);
        }
*/
        return this.tileImages.get(tile);
    }


    public void draw(GraphicsContext gc, int zoom) {

        try {

            // Dimensiones de los tiles individuales (64x64 o 32x32 por ejemplo)
            int tileWidth = map.getTileWidth();
            int tileHeight = map.getTileHeight();

            // Pixeles visibles despues de aplicar zoom
            //int xVisible = (player.cameraX * 2) / zoom;
            //int yVisible = (player.cameraY * 2) / zoom;

            int xVisible = (int) (gc.getCanvas().getWidth() / zoom) / 2;
            int yVisible = (int) (gc.getCanvas().getHeight() / zoom) / 2;

            // Calculamos Tiles visibles para el jugador (campo de vision) para no renderizar mas de la cuenta
            int colInicial = Math.max(0, (player.getWorldX() - xVisible) / tileWidth);
            int colFinal = Math.min(map.getWidth(), (player.getWorldX() + xVisible) / tileWidth) + 2;
            int filaInicial = Math.max(0, (player.getWorldY() - yVisible) / tileHeight);
            int filaFinal = Math.min(map.getHeight(), (player.getWorldX() + yVisible) / tileHeight) + 2;


            for (MapLayer layer : this.map.getLayers()) {

                if (layer instanceof TileLayer) {


                    TileLayer tLayer = (TileLayer) layer;

                    // Dimensiones de una capa del mapa
                    int anchoCapa = layer.getBounds().width;
                    int altoCapa = layer.getBounds().height;


                    //int offsetX = (player.cameraX / zoom) - player.worldX;
                    //int offsetY = (player.cameraY / zoom) - player.worldY;

                    for (int y = filaInicial; y < filaFinal; y++) {
                        for (int x = colInicial; x < colFinal; x++) {
                            Tile singleTile = tLayer.getTileAt(x, y);
                            //Image img = getFxImage(singleTile);


                            if (singleTile == null) continue;


                            Image img = this.getFxImage(singleTile);


                            // Posicion absoluta del mundo (pixeles)
                            int posX = x * tileWidth;
                            int posY = y * tileHeight;

                            // Posicion relativa a la camara del jugador
                            //int screenX = posX - this.player.worldX + this.player.cameraX;
                            //int screenY = posY - this.player.worldY + this.player.cameraY;

                            // Posicion considerando zoom
                            //int screenX = posX + offsetX;
                            //int screenY = posY + offsetY;
                            // POR HACER: IMPLEMENTAR LOGICA DE COLISIONES Y PROPIEDADES PERSONALIZADAS AQUI
                            boolean isVisible = true;


                            if (isVisible) {
                                //gc.drawImage(this.getFxImage(singleTile), screenX, screenY);

                                gc.drawImage(this.getFxImage(singleTile), posX, posY);
                            }

                        }
                    }

                }

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String objetoInteractuable(int columna, int fila) {
        if (columna < 0 || columna >= map.getWidth() || fila < 0 || fila >= map.getHeight()) {
            return null;
        }

        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TileLayer tileLayer) {
                Tile tile = tileLayer.getTileAt(columna, fila);

                if (tile != null && tile.getProperties() != null) {
                    String interactuable = tile.getProperties().getProperty("interactuable");

                    if ("true".equals(interactuable)) {
                        String tarea = tile.getProperties().getProperty("tarea");

                        return tarea;
                    }
                }
            }
        }

        return null;
    }

    public String getTipoTarea(int columna, int fila) {
        if (columna < 0 || columna >= map.getWidth() || fila < 0 || fila >= map.getHeight()) {
            return null;
        }

        return "";
    }

    public List<Tarea> calcularPosicionTareas() {

        List<Tarea> estaciones = new ArrayList<>();
        int mapW = map.getWidth();
        int mapH = map.getHeight();
        boolean[][] tileVisitados = new boolean[mapW][mapH];

        for (MapLayer layer : map.getLayers()) {

            if (layer instanceof TileLayer tileLayer) {

                for (int fila = 0; fila < mapH; fila++) {
                    for (int col = 0; col < mapW; col++) {

                        Tile tile = tileLayer.getTileAt(col, fila);
                        if (tile != null && !tileVisitados[col][fila]) {
                            String tarea = tile.getProperties().getProperty("tarea");

                            if (tarea != null) {
                                Tarea estacion = procesarEstacionTarea(tileLayer, col, fila, tarea, tileVisitados);

                                if (estacion != null) {

                                    estaciones.add(estacion);
                                }

                            }
                        }
                    }
                }
            }
        }

        return estaciones;
    }

    // Buscamos los tiles adyacentes de un tile marcado como tarea para mapear las coordenadas del mundo de dicha tarea (estacion en Among Us)
    private Tarea procesarEstacionTarea(TileLayer tLayer, int comienzoX, int comienzoY, String nombreTarea, boolean[][] visitados) {
        int minX = comienzoX;
        int topX = comienzoX;
        int minY = comienzoY;
        int topY = comienzoY;

        int layerW = tLayer.getBounds().width;
        int layerH = tLayer.getBounds().height;

        // Buscando hacia la derecha
        while (topX + 1 < layerW) {
            Tile nextTile = tLayer.getTileAt(topX + 1, comienzoY);

            if (nextTile != null && nextTile.getProperties().getProperty(nombreTarea) != null) {
                topX++;
            } else {
                break;
            }
        }

        // Buscando hacia abajo
        while (topY + 1 < layerH) {
            Tile nextTile = tLayer.getTileAt(comienzoX, topY + 1);

            if (nextTile != null && nextTile.getProperties().getProperty(nombreTarea) != null) {
                topY++;
            } else {
                break;
            }
        }


        // Marcamos como visitados los "nodos" (tiles en este caso)
        for (int tileX = minX; tileX <= topX; tileX++) {
            for (int tileY = minY; tileY <= topY; tileY++) {
                visitados[tileX][tileY] = true;
            }
        }

        // Mapeamos a coordenadas para poder crear el objeto
        int mapTileW = map.getTileWidth();
        int mapTileH = map.getTileHeight();
        int worldX = minX * mapTileW;
        int worldY = minY * mapTileH;
        int tareaWidth = (topX - minX + 1) * mapTileW;
        int tareaHeight = (topY - minY + 1) * mapTileH;

        // Tendra que ser a traves de if, ir detectando cual tarea es. Lo bueno es que son 4 nada mas
        return TareaFactory.crearTarea(nombreTarea, worldX, worldY, tareaWidth, tareaHeight);

    }

    public boolean tileSolido(int columna, int fila) {


        if (columna < 0 || columna >= map.getWidth() || fila < 0 || fila >= map.getHeight()) {
            return true;
        }


        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TileLayer tileLayer) {
                Tile tile = tileLayer.getTileAt(columna, fila);

                if (tile != null) {
                    String colision = tile.getProperties().getProperty("Solido");

                    if ("true".equals(colision)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public int getTileSize() {
        return map.getTileWidth(); // Altura y anchura son iguales 32x32
    }

    private String extractTilesetImagePath(String tmxPath, int tilesetIndex) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.FileInputStream(tmxPath));

            org.w3c.dom.NodeList tilesets = doc.getElementsByTagName("tileset");
            if (tilesetIndex < tilesets.getLength()) {
                org.w3c.dom.Element tileset = (org.w3c.dom.Element) tilesets.item(tilesetIndex);
                org.w3c.dom.NodeList images = tileset.getElementsByTagName("image");
                if (images.getLength() > 0) {
                    return images.item(0).getAttributes()
                            .getNamedItem("source").getNodeValue();
                }
            }
        } catch (Exception e) {
            System.out.println("  [ERROR] Failed to parse TMX for image path: " + e.getMessage());
        }
        return null;
    }
}
