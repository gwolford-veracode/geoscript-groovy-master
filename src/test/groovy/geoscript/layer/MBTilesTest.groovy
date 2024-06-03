package geoscript.layer

import geoscript.geom.Bounds
import geoscript.proj.Projection
import org.geotools.image.test.ImageAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*

/**
 * The MBTiles Unit Test
 * @author Jared Erickson
 */
class MBTilesTest {

    @TempDir
    private File folder

    @Test
    void create() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-179.99, -85.0511, 179.99, 85.0511, "EPSG:4326").reproject("EPSG:3857")
        assertEquals "states", layer.name
        assertEquals new Projection("EPSG:3857"), layer.proj
        assertEquals b, layer.bounds
        Pyramid pyramid = layer.pyramid
        assertEquals "EPSG:3857", pyramid.proj.id
        assertEquals b, pyramid.bounds
        assertEquals 256, pyramid.tileWidth
        assertEquals 256, pyramid.tileHeight
        assertEquals Pyramid.Origin.BOTTOM_LEFT, pyramid.origin
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
        File file = new File(folder, "states.mbtiles")
        file.delete()
        MBTiles layer = new MBTiles(file, "states", "The united states")
        Bounds b = new Bounds(-179.99, -85.0511, 179.99, 85.0511, "EPSG:4326").reproject("EPSG:3857")
        assertEquals "states", layer.name
        assertEquals new Projection("EPSG:3857"), layer.proj
        assertEquals b, layer.bounds
        Pyramid pyramid = layer.pyramid
        assertEquals "EPSG:3857", pyramid.proj.id
        assertEquals b, pyramid.bounds
        assertEquals 256, pyramid.tileWidth
        assertEquals 256, pyramid.tileHeight
        assertEquals Pyramid.Origin.BOTTOM_LEFT, pyramid.origin
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Tile tile = layer.get(4, 2, 3)
        assertNotNull tile
        assertEquals 4, tile.z
        assertEquals 2, tile.x
        assertEquals 3, tile.y
        assertNotNull tile.data
        layer.close()
    }

    @Test
    void delete() {
        // Since we are modifying the mbtiles file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        File newFile = new File(folder, "states_temp2.mbtiles")
        newFile.withOutputStream { out ->
            file.withInputStream { inp ->
                out << inp
            }
        }
        MBTiles layer = new MBTiles(newFile)
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
        // Since we are modifying the mbtiles file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        File newFile = new File(folder, "states_temp2.mbtiles")
        newFile.withOutputStream { out ->
            file.withInputStream { inp ->
                out << inp
            }
        }
        MBTiles layer = new MBTiles(newFile)
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
    void put() {
        // Since we are modifying the mbtiles file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        File newFile = new File(folder, "states_temp1.mbtiles")
        newFile.withOutputStream { out ->
            file.withInputStream { inp ->
                out << inp
            }
        }
        MBTiles layer = new MBTiles(newFile)

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
    void tilesByZoomLevel() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-123.09, 46.66, -121.13, 47.48, "EPSG:4326").reproject("EPSG:3857")
        TileCursor cursor = layer.tiles(b, 3)
        assertEquals 3, cursor.z
        assertEquals 1, cursor.minX
        assertEquals 5, cursor.minY
        assertEquals 1, cursor.maxX
        assertEquals 5, cursor.maxY
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        TileCursor cursor = layer.tiles(b, b.width / 400, b.height / 300)
        assertEquals 4, cursor.z
        assertEquals 2, cursor.minX
        assertEquals 9, cursor.minY
        assertEquals 5, cursor.maxX
        assertEquals 10, cursor.maxY
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        TileCursor cursor = layer.tiles(b, 400, 300)
        assertEquals 4, cursor.z
        assertEquals 2, cursor.minX
        assertEquals 9, cursor.minY
        assertEquals 5, cursor.maxX
        assertEquals 10, cursor.maxY
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Map coords = layer.getTileCoordinates(b, layer.pyramid.grid(4))
        assertEquals 2, coords.minX
        assertEquals 9, coords.minY
        assertEquals 5, coords.maxX
        assertEquals 10, coords.maxY
        layer.close()
    }

    @Test
    void getRaster() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Raster raster = layer.getRaster(layer.tiles(b, 4))
        assertNotNull raster
        ImageAssert.assertEquals(new File(getClass().getClassLoader().getResource("geoscript/layer/mbtiles_raster.png").toURI()), raster.image, 100)
        File out = new File(folder, "raster.png")
        WorldImage format = new WorldImage(out)
        format.write(raster)
        assertTrue out.exists()
        assertTrue out.length() > 0
        layer.close()
    }

    @Test
    void getRasterCropped() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Raster raster = layer.getRaster(b, 400, 300)
        assertNotNull raster
        ImageAssert.assertEquals(new File(getClass().getClassLoader().getResource("geoscript/layer/mbtiles_raster_cropped.png").toURI()), raster.image, 100)
        File out = new File(folder, "raster.png")
        WorldImage format = new WorldImage(out)
        format.write(raster)
        assertTrue out.exists()
        assertTrue out.length() > 0
        layer.close()
    }

    @Test
    void metadata() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Map metadata = layer.metadata
        assertEquals "base_layer", metadata.type
        assertEquals "states", metadata.name
        assertEquals "A map of the united states", metadata.description
        assertEquals "png", metadata.format
        assertEquals "1.0", metadata.version
        assertEquals "Created with GeoScript", metadata.attribution
        assertEquals "-180.0,-85.0511,180.0,85.0511", metadata.bounds
    }

    @Test
    void setMetadata() {
        // Create a new MBTiles
        File file = new File(folder, "world.mbtiles")
        MBTiles layer = new MBTiles(file, "World", "The world in tiles")
        Map metadata = layer.metadata
        assertEquals "base_layer", metadata.type
        assertEquals "World", metadata.name
        assertEquals "The world in tiles", metadata.description
        assertEquals "png", metadata.format
        assertEquals "1.0", metadata.version
        assertEquals "Created with GeoScript", metadata.attribution
        assertEquals "-179.99,-85.0511,179.99,85.0511", metadata.bounds
        assertEquals 0, metadata.minZoom
        assertEquals 19, metadata.maxZoom

        // Opening the same file shouldn't change the metadata
        layer = new MBTiles(file)
        metadata = layer.metadata
        assertEquals "base_layer", metadata.type
        assertEquals "World", metadata.name
        assertEquals "The world in tiles", metadata.description
        assertEquals "png", metadata.format
        assertEquals "1.0", metadata.version
        assertEquals "Created with GeoScript", metadata.attribution
        assertEquals "-179.99,-85.0511,179.99,85.0511", metadata.bounds
        assertEquals 0, metadata.minZoom
        assertEquals 19, metadata.maxZoom

        // Change the metadata
        layer.setMetadata([
                name: "world",
                version: "2.0",
                attribution: "GeoScript",
                minZoom: 1,
                maxZoom: 6
        ])
        metadata = layer.metadata
        assertEquals "base_layer", metadata.type
        assertEquals "world", metadata.name
        assertEquals "The world in tiles", metadata.description
        assertEquals "png", metadata.format
        assertEquals "2.0", metadata.version
        assertEquals "GeoScript", metadata.attribution
        assertEquals "-179.99,-85.0511,179.99,85.0511", metadata.bounds
        assertEquals 1, metadata.minZoom
        assertEquals 6, metadata.maxZoom
    }

    @Test
    void createWithMetadata() {
        File file = new File(folder, "test.mbtiles")
        MBTiles layer = new MBTiles.Factory().create([
                type: "mbtiles",
                file: file,
                name: "states",
                mbtilesType: "overlay",
                description: "USA States",
                attribution: "Groovy",
                version: "3.0",
                format: "JPEG",
                bounds: "-135.648049,20.93138,-56.898049,52.355946"
        ])
        Map metadata = layer.metadata
        assertEquals "overlay", metadata.type
        assertEquals "states", metadata.name
        assertEquals "USA States", metadata.description
        assertEquals "jpeg", metadata.format
        assertEquals "3.0", metadata.version
        assertEquals "Groovy", metadata.attribution
        assertEquals "-135.648049,20.93138,-56.898049,52.355946", metadata.bounds
    }

    @Test
    void tileCounts() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
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
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        assertEquals 0, layer.minZoom
        assertEquals 4, layer.maxZoom
    }
}
