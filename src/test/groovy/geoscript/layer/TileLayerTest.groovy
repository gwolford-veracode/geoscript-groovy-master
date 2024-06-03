package geoscript.layer

import geoscript.FileUtil
import geoscript.feature.Feature
import geoscript.geom.Bounds
import geoscript.geom.Point
import geoscript.proj.Projection
import org.geotools.image.test.ImageAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*

/**
 * The TileLayer Unit Test
 * @author Jared Erickson
 */
class TileLayerTest {

    @TempDir
    File folder

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
    void put() {
        // Since we are modifying the mbtiles file copy it to a temp file
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        File newFile = new File(folder, "states_temp.mbtiles")
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
    void boundsAroundPoint() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        TileCursor cursor = layer.tiles(Projection.transform(new Point(-100.81,46.81),"EPSG:4326","EPSG:3857"), 8, 400, 300)
        assertEquals(8, cursor.z)
        assertEquals(55, cursor.minX)
        assertEquals(165, cursor.minY)
        assertEquals(57, cursor.maxX)
        assertEquals(166, cursor.maxY)
        assertEquals(3, cursor.width)
        assertEquals(2, cursor.height)
        assertEquals(6, cursor.size)
        layer.close()
    }

    @Test
    void getRaster() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Bounds b = new Bounds(-124.73142200000001, 24.955967, -66.969849, 49.371735, "EPSG:4326").reproject("EPSG:3857")
        Raster raster = layer.getRaster(layer.tiles(b, 4))
        assertNotNull raster
        ImageAssert.assertEquals(new File(getClass().getClassLoader().getResource("geoscript/layer/tilelayer_raster.png").toURI()), raster.image, 100)
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
        ImageAssert.assertEquals(new File(getClass().getClassLoader().getResource("geoscript/layer/tilelayer_raster_cropped.png").toURI()), raster.image, 100)
        File out = new File(folder, "raster.png")
        WorldImage format = new WorldImage(out)
        format.write(raster)
        assertTrue out.exists()
        assertTrue out.length() > 0
        layer.close()
    }

    @Test
    void getRasterAroundPoint() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Point point = Projection.transform(new Point(-100.81,46.81),"EPSG:4326","EPSG:3857")
        Raster raster = layer.getRaster(point, 4, 300, 200)
        assertNotNull raster
        ImageAssert.assertEquals(new File(getClass().getClassLoader().getResource("geoscript/layer/tilelayer_raster_point.png").toURI()), raster.image, 100)
        File out = new File(folder, "raster.png")
        WorldImage format = new WorldImage(out)
        format.write(raster)
        assertTrue out.exists()
        assertTrue out.length() > 0
        layer.close()
    }

    @Test
    void getLayer() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        MBTiles layer = new MBTiles(file)
        Layer vlayer = layer.getLayer(layer.tiles(1))
        assertNotNull vlayer
        assertTrue vlayer.schema.has("the_geom")
        assertTrue vlayer.schema.has("id")
        assertTrue vlayer.schema.has("z")
        assertTrue vlayer.schema.has("x")
        assertTrue vlayer.schema.has("y")
        assertEquals 4, vlayer.count
        vlayer.eachFeature { Feature f ->
            assertTrue f['id'] in [0, 1, 2, 3]
            assertTrue f['z'] == 1
            assertTrue f['x'] in [0, 1]
            assertTrue f['y'] in [0, 1]
            assertNotNull f.geom
        }
    }

    @Test
    void withTileLayer() {
        File file = new File(getClass().getClassLoader().getResource("states.mbtiles").toURI())
        TileLayer.withTileLayer(new MBTiles(file)) { TileLayer layer ->
            Tile tile = layer.get(4, 2, 3)
            assertNotNull tile
            assertEquals 4, tile.z
            assertEquals 2, tile.x
            assertEquals 3, tile.y
            assertNotNull tile.data
        }
    }

    @Test
    void getTileLayerFromString() {
        // MBTiles params
        File file = new File(folder, 'test48.mbtiles')
        file.delete()
        TileLayer tileLayer = TileLayer.getTileLayer("type=mbtiles file=${file.absolutePath}")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof MBTiles)
        // MBTiles file
        file = new File(folder, 'test10.mbtiles')
        file.delete()
        tileLayer = TileLayer.getTileLayer(file.absolutePath)
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof MBTiles)
        // GeoPackage params
        file = new File(folder, 'test69.gpkg')
        file.delete()
        tileLayer = TileLayer.getTileLayer("type=geopackage file=${file.absolutePath}")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof GeoPackage)
        // GeoPackage file
        file = new File(folder, 'test19.gpkg')
        file.delete()
        tileLayer = TileLayer.getTileLayer(file.absolutePath)
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof GeoPackage)
        // TMS
        file = FileUtil.createDir(folder,"tms")
        tileLayer = TileLayer.getTileLayer("type=tms file=${file.absolutePath} format=jpeg")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof TMS)
        assertEquals(file.absolutePath, (tileLayer as TMS).dir.absolutePath)
        assertEquals("jpeg", (tileLayer as TMS).imageType)
        // OSM with URL
        tileLayer = TileLayer.getTileLayer("type=osm url=http://a.tile.openstreetmap.org")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof OSM)
        assertTrue((tileLayer as OSM).baseUrls.contains("http://a.tile.openstreetmap.org"))
        assertFalse((tileLayer as OSM).baseUrls.contains("http://b.tile.openstreetmap.org"))
        assertFalse((tileLayer as OSM).baseUrls.contains("http://c.tile.openstreetmap.org"))
        // OSM with URLs
        tileLayer = TileLayer.getTileLayer("type=osm urls=http://a.tile.openstreetmap.org,http://c.tile.openstreetmap.org")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof OSM)
        assertTrue((tileLayer as OSM).baseUrls.contains("http://a.tile.openstreetmap.org"))
        assertFalse((tileLayer as OSM).baseUrls.contains("http://b.tile.openstreetmap.org"))
        assertTrue((tileLayer as OSM).baseUrls.contains("http://c.tile.openstreetmap.org"))
        // OSM with no URLs
        tileLayer = TileLayer.getTileLayer("type=osm")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof OSM)
        assertTrue((tileLayer as OSM).baseUrls.contains("http://a.tile.openstreetmap.org"))
        assertTrue((tileLayer as OSM).baseUrls.contains("http://b.tile.openstreetmap.org"))
        assertTrue((tileLayer as OSM).baseUrls.contains("http://c.tile.openstreetmap.org"))
        // OSM
        tileLayer = TileLayer.getTileLayer("osm")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof OSM)
        assertTrue((tileLayer as OSM).baseUrls.contains("http://a.tile.openstreetmap.org"))
        assertTrue((tileLayer as OSM).baseUrls.contains("http://b.tile.openstreetmap.org"))
        assertTrue((tileLayer as OSM).baseUrls.contains("http://c.tile.openstreetmap.org"))
        // UTFGrid
        file = FileUtil.createDir(folder,"utfgrid")
        tileLayer = TileLayer.getTileLayer("type=utfgrid file=${file.absolutePath}")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof UTFGrid)
        // VectorTiles Directory
        file = FileUtil.createDir(folder,"vectortiles")
        tileLayer = TileLayer.getTileLayer("type=vectortiles file=${file.absolutePath} format=mvt pyramid=GlobalMercator")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof VectorTiles)
        assertEquals("mvt", (tileLayer as VectorTiles).type)
        assertEquals(file.absolutePath, (tileLayer as VectorTiles).dir.absolutePath)
        assertEquals(Pyramid.createGlobalMercatorPyramid(), (tileLayer as VectorTiles).pyramid)
        // VectorTiles URL
        tileLayer = TileLayer.getTileLayer("type=vectortiles url=http://vectortiles.org format=pbf pyramid=GlobalGeodetic")
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof VectorTiles)
        assertEquals("pbf", (tileLayer as VectorTiles).type)
        assertEquals("http://vectortiles.org", (tileLayer as VectorTiles).url.toString())
        assertEquals(Pyramid.createGlobalGeodeticPyramid(), (tileLayer as VectorTiles).pyramid)
        // Null
        tileLayer = TileLayer.getTileLayer("type=asdfasd")
        assertNull(tileLayer)
    }

    @Test
    void getTileLayerFromParams() {
        // MBTiles (file doesn't exist)
        File file = new File(folder, 'test45.mbtiles')
        file.delete()
        TileLayer tileLayer = TileLayer.getTileLayer([type: 'mbtiles', file: file])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof MBTiles)
        // MBTiles (empty file)
        file = new File(folder, 'test23.mbtiles')
        tileLayer = TileLayer.getTileLayer([type: 'mbtiles', file: file])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof MBTiles)
        // GeoPackage (file doesn't exist)
        file = new File(folder, 'test67.gpkg')
        file.delete()
        tileLayer = TileLayer.getTileLayer([type: 'geopackage', file: file])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof GeoPackage)
        // GeoPackage (empty file)
        file = new File(folder, 'test34.gpkg')
        file.delete()
        tileLayer = TileLayer.getTileLayer([type: 'geopackage', file: file])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof GeoPackage)
        // TMS
        file = FileUtil.createDir(folder,"tms")
        tileLayer = TileLayer.getTileLayer([type: 'tms', file: file, format: 'jpeg'])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof TMS)
        assertEquals(file.absolutePath, (tileLayer as TMS).dir.absolutePath)
        assertEquals("jpeg", (tileLayer as TMS).imageType)
        // OSM
        tileLayer = TileLayer.getTileLayer([type: 'osm', url: 'http://a.tile.openstreetmap.org'])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof OSM)
        assertTrue((tileLayer as OSM).baseUrls.contains("http://a.tile.openstreetmap.org"))
        // UTFGrid
        file = FileUtil.createDir(folder,"utfgrid")
        tileLayer = TileLayer.getTileLayer([type: 'utfgrid', file: file])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof UTFGrid)
        // VectorTiles Directory
        file = FileUtil.createDir(folder,"vectortiles")
        tileLayer = TileLayer.getTileLayer([type: 'vectortiles', file: file, format: 'mvt', pyramid: 'GlobalMercator'])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof VectorTiles)
        assertEquals("mvt", (tileLayer as VectorTiles).type)
        assertEquals(file.absolutePath, (tileLayer as VectorTiles).dir.absolutePath)
        assertEquals(Pyramid.createGlobalMercatorPyramid(), (tileLayer as VectorTiles).pyramid)
        // VectorTiles URL
        tileLayer = TileLayer.getTileLayer([type:'vectortiles', url: 'http://vectortiles.org', format: 'pbf', pyramid: Pyramid.createGlobalGeodeticPyramid()])
        assertNotNull(tileLayer)
        assertTrue(tileLayer instanceof VectorTiles)
        assertEquals("pbf", (tileLayer as VectorTiles).type)
        assertEquals("http://vectortiles.org", (tileLayer as VectorTiles).url.toString())
        assertEquals(Pyramid.createGlobalGeodeticPyramid(), (tileLayer as VectorTiles).pyramid)
    }

    @Test void getTileRenderer() {
        TileRenderer tileRenderer
        TileLayer tileLayer
        File file
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("states.mbtiles").toURI()))
        // MBTiles
        file = new File(folder, 'test1.mbtiles')
        file.delete()
        tileLayer = TileLayer.getTileLayer("type=mbtiles file=${file.absolutePath}")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof ImageTileRenderer)
        // GeoPackage params
        file = new File(folder, 'test1.gpkg')
        file.delete()
        tileLayer = TileLayer.getTileLayer("type=geopackage file=${file.absolutePath}")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof ImageTileRenderer)
        // GeoPackage file
        file = new File(folder, 'test2.gpkg')
        file.delete()
        tileLayer = TileLayer.getTileLayer(file.absolutePath)
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof ImageTileRenderer)
        // TMS
        file = FileUtil.createDir(folder,"tms")
        tileLayer = TileLayer.getTileLayer("type=tms file=${file.absolutePath} format=jpeg")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof ImageTileRenderer)
        // OSM
        tileLayer = TileLayer.getTileLayer("type=osm url=http://a.tile.openstreetmap.org")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNull(tileRenderer)
        // UTFGrid
        file = FileUtil.createDir(folder,"utfgrid")
        tileLayer = TileLayer.getTileLayer("type=utfgrid file=${file.absolutePath}")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof UTFGridTileRenderer)
        // VectorTiles Directory
        file = FileUtil.createDir(folder,"vectortiles")
        tileLayer = TileLayer.getTileLayer("type=vectortiles file=${file.absolutePath} format=mvt pyramid=GlobalMercator")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof VectorTileRenderer)
        // VectorTiles URL
        tileLayer = TileLayer.getTileLayer("type=vectortiles url=http://vectortiles.org format=pbf pyramid=GlobalGeodetic")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNotNull(tileRenderer)
        assertTrue(tileRenderer instanceof PbfVectorTileRenderer)
        // Null
        tileLayer = TileLayer.getTileLayer("type=asdfasd")
        tileRenderer = TileLayer.getTileRenderer(tileLayer, layer)
        assertNull(tileRenderer)
    }

}
