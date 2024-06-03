package geoscript.layer

import geoscript.geom.Bounds
import geoscript.proj.Projection
import org.apache.commons.dbcp.BasicDataSource
import org.geotools.image.test.ImageAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*
import static geoscript.AssertUtil.assertBoundsEquals

/**
 * The GeoPackage Unit Test
 * @author Jared Erickson
 */
class GeoPackageTest {

    @TempDir
    private File folder

    @Test
    void create() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-2.00363951478813E7, -2.00374712051371E7, 2.00363951478813E7, 2.00374712051371E7, "EPSG:3857")
        assertEquals "states", layer.name
        assertEquals new Projection("EPSG:3857"), layer.proj
        assertBoundsEquals b, layer.bounds, 0.0001
        Pyramid pyramid = layer.pyramid
        assertEquals "EPSG:3857", pyramid.proj.id
        assertBoundsEquals b, pyramid.bounds, 0.0001
        assertEquals 256, pyramid.tileWidth
        assertEquals 256, pyramid.tileHeight
        assertEquals Pyramid.Origin.TOP_LEFT, pyramid.origin
        assertEquals 20, pyramid.grids.size()
        pyramid.grids.eachWithIndex { Grid g, int z ->
            assertEquals z, g.z
            int n = Math.pow(2, z)
            assertEquals n, g.width
            assertEquals n, g.height
            assertEquals 156412.0 / n, g.xResolution, 0.01
            assertEquals 156412.0 / n, g.yResolution, 0.01
        }
        layer.close()
    }

    @Test
    void createFromDataSource() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        BasicDataSource ds = new BasicDataSource()
        ds.driverClassName = "org.sqlite.JDBC"
        ds.url = "jdbc:sqlite:" + file.absolutePath
        GeoPackage layer = new GeoPackage(ds, "states")
        Bounds b = new Bounds(-2.00363951478813E7, -2.00374712051371E7, 2.00363951478813E7, 2.00374712051371E7, "EPSG:3857")
        assertEquals "states", layer.name
        assertEquals new Projection("EPSG:3857"), layer.proj
        assertBoundsEquals b, layer.bounds, 0.0001
        Pyramid pyramid = layer.pyramid
        assertEquals "EPSG:3857", pyramid.proj.id
        assertBoundsEquals b, pyramid.bounds, 0.0001
        assertEquals 256, pyramid.tileWidth
        assertEquals 256, pyramid.tileHeight
        assertEquals Pyramid.Origin.TOP_LEFT, pyramid.origin
        assertEquals 20, pyramid.grids.size()
        pyramid.grids.eachWithIndex { Grid g, int z ->
            assertEquals z, g.z
            int n = Math.pow(2, z)
            assertEquals n, g.width
            assertEquals n, g.height
            assertEquals 156412.0 / n, g.xResolution, 0.01
            assertEquals 156412.0 / n, g.yResolution, 0.01
        }
        layer.close()
    }

    @Test
    void createNew() {
        File file = new File(folder, "states.gpkg")
        file.delete()
        GeoPackage layer = new GeoPackage(file, "states", Pyramid.createGlobalMercatorPyramid())
        Bounds b = new Bounds(-2.00363951478813E7, -2.00374712051371E7, 2.00363951478813E7, 2.00374712051371E7, "EPSG:3857")
        assertEquals "states", layer.name
        assertEquals new Projection("EPSG:3857"), layer.proj
        assertBoundsEquals b, layer.bounds, 0.0001
        Pyramid pyramid = layer.pyramid
        assertEquals "EPSG:3857", pyramid.proj.id
        assertBoundsEquals b, pyramid.bounds, 0.0001
        assertEquals 256, pyramid.tileWidth
        assertEquals 256, pyramid.tileHeight
        assertEquals Pyramid.Origin.TOP_LEFT, pyramid.origin
        assertEquals 20, pyramid.grids.size()
        pyramid.grids.eachWithIndex { Grid g, int z ->
            assertEquals z, g.z
            int n = Math.pow(2, z)
            assertEquals n, g.width
            assertEquals n, g.height
            assertEquals 156412.0 / n, g.xResolution, 0.01
            assertEquals 156412.0 / n, g.yResolution, 0.01
        }
        layer.close()
    }

    @Test
    void get() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Tile tile = layer.get(4, 2, 3)
        assertNotNull tile
        assertEquals 4, tile.z
        assertEquals 2, tile.x
        assertEquals 3, tile.y
        assertNotNull tile.data
        layer.close()
    }

    @Test
    void put() {
        // Since we are modifying the mbtiles file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        File newFile = new File(folder, "states_temp1.gpkg")
        newFile.withOutputStream { out ->
            file.withInputStream { inp ->
                out << inp
            }
        }
        GeoPackage layer = new GeoPackage(newFile, "states")

        // Make sure Tile doesn't exist in database
        Tile tile = layer.get(10, 0, 0)
        assertNotNull tile
        assertEquals 10, tile.z
        assertEquals 0, tile.x
        assertEquals 0, tile.y
        assertNull tile.data

        // Load a tile image
        File f = new File(getClass().getClassLoader().getResource("0.png").toURI())
        tile.data = f.bytes
        assertNotNull tile.data

        // Save Tile and make sure it saved correctly by getting it again
        layer.put(tile)
        tile = layer.get(10, 0, 0)
        assertNotNull tile
        assertEquals 10, tile.z
        assertEquals 0, tile.x
        assertEquals 0, tile.y
        assertNotNull tile.data
        layer.close()
    }

    @Test
    void delete() {
        // Since we are modifying the mbtiles file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        File newFile = new File(folder, "states_temp2.gpkg")
        newFile.withOutputStream { out ->
            file.withInputStream { inp ->
                out << inp
            }
        }
        GeoPackage layer = new GeoPackage(newFile, "states")
        Tile tile = layer.get(4, 2, 3)
        assertNotNull tile
        assertNotNull tile.data
        layer.delete(tile)
        tile = layer.get(4, 2, 3)
        assertNotNull tile
        assertNull tile.data
        layer.close()
    }

    @Test
    void deleteTiles() {
        // Since we are modifying the geopackage file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        File newFile = new File(folder, "states_temp2.gpkg")
        newFile.withOutputStream { out ->
            file.withInputStream { inp ->
                out << inp
            }
        }
        GeoPackage layer = new GeoPackage(newFile, "states")
        layer.tiles(4).each { Tile tile ->
            assertNotNull tile
            assertNotNull tile.data
        }
        layer.delete(layer.tiles(4))
        layer.tiles(4).each { Tile tile ->
            assertNotNull tile
            assertNull tile.data
        }
        layer.tiles(3).each { Tile tile ->
            assertNotNull tile
            assertNotNull tile.data
        }
        layer.close()
    }

    @Test
    void tilesByZoomLevel() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        TileCursor cursor = layer.tiles(1)
        assertEquals 1, cursor.z
        assertEquals 0, cursor.minX
        assertEquals 0, cursor.minY
        assertEquals 1, cursor.maxX
        assertEquals 1, cursor.maxY
        assertEquals 2, cursor.width
        assertEquals 2, cursor.height
        assertEquals 4, cursor.size
        int c = 0
        cursor.each { Tile t ->
            assertEquals 1, t.z
            assertNotNull t.data
            c++
        }
        assertEquals 4, c
        layer.close()
    }

    @Test
    void tilesByTileCoordinates() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        TileCursor cursor = layer.tiles(2, 1, 2, 3, 3)
        assertEquals 2, cursor.z
        assertEquals 1, cursor.minX
        assertEquals 2, cursor.minY
        assertEquals 3, cursor.maxX
        assertEquals 3, cursor.maxY
        assertEquals 3, cursor.width
        assertEquals 2, cursor.height
        assertEquals 6, cursor.size
        int c = 0
        cursor.each { Tile t ->
            assertEquals 2, t.z
            assertNotNull t.data
            c++
        }
        assertEquals 6, c
        layer.close()
    }

    @Test
    void tilesByBoundsAndZoomLevel() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-123.09, 46.66, -121.13, 47.48, "EPSG:4326").reproject("EPSG:3857")
        TileCursor cursor = layer.tiles(b, 3)
        assertEquals 3, cursor.z
        assertEquals 1, cursor.minX
        assertEquals 2, cursor.minY
        assertEquals 1, cursor.maxX
        assertEquals 2, cursor.maxY
        assertEquals 1, cursor.width
        assertEquals 1, cursor.height
        assertEquals 1, cursor.size
        int c = 0
        cursor.each { Tile t ->
            assertEquals 3, t.z
            assertNotNull t.data
            c++
        }
        assertEquals 1, c
        layer.close()
    }

    @Test
    void tilesByBoundsAndResolutions() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        TileCursor cursor = layer.tiles(b, b.width / 400, b.height / 300)
        assertEquals 4, cursor.z
        assertEquals 2, cursor.minX
        assertEquals 5, cursor.minY
        assertEquals 5, cursor.maxX
        assertEquals 6, cursor.maxY
        assertEquals 4, cursor.width
        assertEquals 2, cursor.height
        assertEquals 8, cursor.size
        int c = 0
        cursor.each { Tile t ->
            assertEquals 4, t.z
            assertNotNull t.data
            c++
        }
        assertEquals 8, c
        layer.close()
    }

    @Test
    void tilesByBoundsAndImageSize() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        TileCursor cursor = layer.tiles(b, 400, 300)
        assertEquals 4, cursor.z
        assertEquals 2, cursor.minX
        assertEquals 5, cursor.minY
        assertEquals 5, cursor.maxX
        assertEquals 6, cursor.maxY
        assertEquals 4, cursor.width
        assertEquals 2, cursor.height
        assertEquals 8, cursor.size
        int c = 0
        cursor.each { Tile t ->
            assertEquals 4, t.z
            assertNotNull t.data
            c++
        }
        assertEquals 8, c
        layer.close()
    }

    @Test
    void getTileCoordinates() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Map coords = layer.getTileCoordinates(b, layer.pyramid.grid(4))
        assertEquals 2, coords.minX
        assertEquals 5, coords.minY
        assertEquals 5, coords.maxX
        assertEquals 6, coords.maxY
        layer.close()
    }

    @Test
    void getRaster() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Raster raster = layer.getRaster(layer.tiles(b, 4))
        assertNotNull raster
        ImageAssert.assertEquals(getFile("geoscript/layer/gpkg_raster.png"), raster.image, 100)
        File out = new File(folder, "raster.png")
        WorldImage format = new WorldImage(out)
        format.write(raster)
        assertTrue out.exists()
        assertTrue out.length() > 0
        layer.close()
    }

    @Test
    void getRasterCropped() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Raster raster = layer.getRaster(b, 400, 300)
        assertNotNull raster
        ImageAssert.assertEquals(getFile("geoscript/layer/gpkg_raster_cropped.png"), raster.image, 100)
        File out = new File(folder, "raster.png")
        WorldImage format = new WorldImage(out)
        format.write(raster)
        assertTrue out.exists()
        assertTrue out.length() > 0
        layer.close()
    }

    @Test
    void tileCounts() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        List stats = layer.tileCounts
        stats.eachWithIndex { Map stat, int index ->
            assertEquals(index, stat.zoom)
            assertEquals(Math.pow(4, index), stat.tiles, 0.1)
            assertEquals(Math.pow(4, index), stat.total, 0.1)
            assertEquals(1.0, stat.percent, 0.1)
        }
    }

    @Test
    void minMaxZoom() {
        File file = new File(getClass().getClassLoader().getResource("states.gpkg").toURI())
        GeoPackage layer = new GeoPackage(file, "states")
        assertEquals 0, layer.minZoom
        assertEquals 4, layer.maxZoom
    }

    @Test
    void deleteLayer() {
        File file = new File(folder, "states.gpkg")
        file.delete()
        GeoPackage layer1 = new GeoPackage(file, "states", Pyramid.createGlobalMercatorPyramid())
        GeoPackage layer2 = new GeoPackage(file, "world", Pyramid.createGlobalGeodeticPyramid())
        assertEquals(["states","world"], GeoPackage.getNames(file))
        layer1.delete()
        assertEquals(["world"], GeoPackage.getNames(file))
        layer2.delete()
        assertEquals([], GeoPackage.getNames(file))
    }

    private File getFile(String resource) {
        new File(getClass().getClassLoader().getResource(resource).toURI())
    }

}
