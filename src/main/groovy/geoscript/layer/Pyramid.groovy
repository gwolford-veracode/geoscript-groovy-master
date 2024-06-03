package geoscript.layer

import geoscript.geom.Bounds
import geoscript.geom.Point
import geoscript.layer.io.PyramidReader
import geoscript.layer.io.PyramidReaders
import geoscript.proj.Projection
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * The Tile Pyramid
 * @author Jared Erickson
 */
@EqualsAndHashCode
@ToString(includeNames = true, excludes = ["grids", "minGrid", "maxGrid"])
class Pyramid {

    /**
     * The Projection
     */
    Projection proj = new Projection("EPSG:4326")

    /**
     * The Bounds
     */
    Bounds bounds = new Bounds(-179.99, -90, 179.99, 90, "EPSG:4326")

    /**
     * The Origin
     */
    Origin origin = Origin.BOTTOM_LEFT

    /**
     * The tile width
     */
    int tileWidth = 256

    /**
     * The tile height
     */
    int tileHeight = 256

    /**
     * The List of Grids
     */
    List<Grid> grids = []

    /**
     * The Origin enumeration
     */
    static enum Origin {
        BOTTOM_LEFT,
        TOP_LEFT,
        BOTTOM_RIGHT,
        TOP_RIGHT
    }

    /**
     * Find the Grid for the given zoom level
     * @param z The zoom level
     * @return The Grid or null
     */
    Grid grid(long z) {
        grids.find { Grid m ->
            m.z == z
        }
    }

    /**
     * Find the best Grid for the given Bounds and XY resolutions
     * @param b The Bounds
     * @param resX The x resolution
     * @param resY The y resolution
     * @return A best Grid or null
     */
    Grid grid(Bounds b, double resX, double resY) {
        Grid best = null
        double score = Double.MAX_VALUE
        grids.each { Grid m ->
            double res = Math.abs(resX - m.xResolution) + Math.abs(resY - m.yResolution)
            if (res < score) {
                score = res
                best = m
            }
        }
        best
    }

    /**
     * Find the best Grid for the given Bounds and image width and height
     * @param b The Bounds
     * @param w The image width
     * @param h The image height
     * @return The best Grid or null
     */
    Grid grid(Bounds b, int w, int h) {
        double resX = b.width / (w as double)
        double resY = b.height / (h as double)
        grid(b, resX, resY)
    }

    /**
     * Get the max Grid by zoom level
     * @return THe max Grid by zoom level
     */
    Grid getMaxGrid() {
        grids.max { Grid g -> g.z }
    }

    /**
     * Get the min Grid by zoom level
     * @return The min Grid by zoom level
     */
    Grid getMinGrid() {
        grids.min { Grid g -> g.z }
    }

    /**
     * Find the Bounds around a Point at a given zoom level for a canvas of a given width and height
     * @param p The Point (in the Pyramid's projection)
     * @param z The zoom level
     * @param width The canvas width
     * @param height The canvas height
     * @return a Bounds
     */
    Bounds bounds(Point p, long z, int width, int height) {
        Grid g = grid(z)
        double boundsWidth = g.xResolution * width
        double boundsHeight = g.yResolution * height
        new Bounds(p.x - boundsWidth / 2, p.y - boundsHeight / 2, p.x + boundsWidth / 2, p.y + boundsHeight / 2, this.proj)
    }

    /**
     * Calculate the Bounds for the given Tile
     * @param t The Tile
     * @return A Bounds
     */
    Bounds bounds(Tile t) {
        Grid m = grid(t.z)
        if (m == null) {
            throw new IllegalArgumentException("No grid for zoom level ${t.z}")
        }
        int w = m.width
        int h = m.height
        double dx = bounds.width / (w as double)
        double dy = bounds.height / (h as double)

        double x
        if (origin == Origin.BOTTOM_LEFT || origin == Origin.TOP_LEFT) {
            x = bounds.minX + dx * t.x
        } else {
            x = bounds.minX + (dx * (w - t.x)) - dx
        }

        double y
        if (origin == Origin.BOTTOM_LEFT || origin == Origin.BOTTOM_RIGHT) {
            y = bounds.minY + dy * t.y
        } else {
            y = bounds.minY + (dy * (h - t.y)) - dy
        }

        new Bounds(x, y, x + dx, y + dy, this.proj)
    }

    /**
     * Get Tile coordinates (minX, minY, maxX, maxY) for the given Bounds and zoom level
     * @param b The Bounds
     * @param z The zoom level
     * @return A Map with tile coordinates (minX, minY, maxX, maxY)
     */
    Map getTileCoordinates(Bounds b, long z) {
        getTileCoordinates(b, grid(z))
    }

    /**
     * Get Tile coordinates (minX, minY, maxX, maxY) for the given Bounds and Grid
     * @param b The Bounds
     * @param g The Grid
     * @return A Map with tile coordinates (minX, minY, maxX, maxY)
     */
    Map getTileCoordinates(Bounds b, Grid g) {
        int minX = Math.floor((((b.minX - bounds.minX) / bounds.width) * g.width))
        int maxX = Math.ceil(((b.maxX - bounds.minX) / bounds.width) * g.width) - 1
        if (this.origin == Pyramid.Origin.TOP_RIGHT || this.origin == Pyramid.Origin.BOTTOM_RIGHT) {
            int invertedMinX = g.width - maxX
            int invertedMaxX = g.width - minX
            minX = invertedMinX - 1
            maxX = invertedMaxX - 1
        }
        int minY = Math.floor(((b.minY - bounds.minY) / bounds.height) * g.height)
        int maxY = Math.ceil(((b.maxY - bounds.minY) / bounds.height) * g.height) - 1
        if (this.origin == Pyramid.Origin.TOP_LEFT || this.origin == Pyramid.Origin.TOP_RIGHT) {
            int invertedMinY = g.height - maxY
            int invertedMaxY = g.height - minY
            minY = invertedMinY - 1
            maxY = invertedMaxY - 1
        }
        [minX: minX, minY: minY, maxX: maxX, maxY: maxY]
    }

    /**
     * Create a Pyramid from a String.  The String can be a well known name (GlobalMercator or GlobalMercatorBottomLeft),
     * a JSON String or File, an XML String or File, or a CSV String or File
     * @param str A Pyramid String or File
     * @return A Pyramid or null
     */
    static Pyramid fromString(String str) {
        // Well known names
        if (str.equalsIgnoreCase("GlobalMercator") || str.equalsIgnoreCase("mercator")) {
            Pyramid.createGlobalMercatorPyramid()
        } else if (str.equalsIgnoreCase("GlobalMercatorBottomLeft")) {
            Pyramid.createGlobalMercatorPyramid(origin: Pyramid.Origin.BOTTOM_LEFT)
        } else if (str.equalsIgnoreCase("GlobalMercatorTopLeft")) {
            Pyramid.createGlobalMercatorPyramid(origin: Pyramid.Origin.TOP_LEFT)
        } else if (str.equalsIgnoreCase("GlobalGeodetic") || str.equalsIgnoreCase("geodetic")) {
            Pyramid.createGlobalGeodeticPyramid()
        } else {
            File file = new File(str)
            if (file.exists()){
                str = file.text
            }
            Pyramid pyramid = null
            for (PyramidReader reader : PyramidReaders.list()) {
                try {
                    pyramid = reader.read(str)
                } catch(Exception ex){
                    // Just try the next reader
                }
            }
            pyramid
        }
    }

    /**
     * Create a Pyramid with Grids for common global web mercator tile sets.
     * http://wiki.openstreetmap.org/wiki/Zoom_levels
     * @param options The optional named parameters:
     * <ul>
     *     <li>origin = The Pyramid Origin (defaults to bottom left)</li>
     *     <li>maxZoom = The max zoom level (defaults to 19)</li>
     * </ul>
     * @return A Pyramid
     */
    static Pyramid createGlobalMercatorPyramid(Map options = [:]) {
        Projection latLonProj = new Projection("EPSG:4326")
        Projection mercatorProj = new Projection("EPSG:3857")
        Bounds latLonBounds = new Bounds(-179.99, -85.0511, 179.99, 85.0511, latLonProj)
        Bounds mercatorBounds = latLonBounds.reproject(mercatorProj)
        Pyramid p = new Pyramid(
            proj: mercatorProj,
            bounds: mercatorBounds,
            origin: options.get("origin", Pyramid.Origin.BOTTOM_LEFT),
            tileWidth: 256,
            tileHeight: 256
        )
        int maxZoom = options.get("maxZoom", 19)
        p.grids = Grid.createGlobalMercatorGrids(maxZoom)
        p
    }

    /**
     * Create a Pyramid with Grids for common global geodetic tile sets.
     * http://wiki.osgeo.org/wiki/Tile_Map_Service_Specification#global-geodetic
     * @param options The optional named parameters:
     * <ul>
     *     <li>maxZoom = The max zoom level (defaults to 19)</li>
     * </ul>
     * @return A Pyramid
     */
    static Pyramid createGlobalGeodeticPyramid(Map options = [:]) {
        Projection latLonProj = new Projection("EPSG:4326")
        Bounds latLonBounds = new Bounds(-179.99, -89.99, 179.99, 89.99, latLonProj)
        Pyramid p = new Pyramid(
                proj: latLonProj,
                bounds: latLonBounds,
                origin: Pyramid.Origin.BOTTOM_LEFT,
                tileWidth: 256,
                tileHeight: 256
        )
        int maxZoom = options.get("maxZoom", 19)
        p.grids = Grid.createGlobalGeodeticGrids(maxZoom)
        p
    }
}
