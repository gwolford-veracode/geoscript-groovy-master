package geoscript.render

import geoscript.geom.Bounds
import geoscript.layer.Shapefile
import geoscript.proj.Projection
import geoscript.layer.GeoTIFF
import geoscript.layer.Raster
import geoscript.style.Fill
import geoscript.style.Stroke
import org.geotools.image.test.ImageAssert
import geoscript.layer.MBTiles
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import javax.imageio.ImageIO

import static org.junit.jupiter.api.Assertions.*

/**
 * The Map UnitTest
 * @author Jared Erickson
 */
class MapTest {

    @TempDir
    File folder

    private File getFile(String resource) {
        return new File(getClass().getClassLoader().getResource(resource).toURI())
    }

    @Test void proj() {
        Map map = new Map();
        map.proj = new Projection("EPSG:2927")
        assertEquals("EPSG:2927", map.proj.id)
        map.proj = "EPSG:4326"
        assertEquals("EPSG:4326", map.proj.id)
    }

    @Test void layer() {
        Map map = new Map()
        assertEquals(0, map.layers.size())
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)
        map.addLayer(shp)
        assertEquals(1, map.layers.size())
        map.layers = [shp]
        assertEquals(1, map.layers.size())
        map.close()
    }

    @Test void renderToImage() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)

        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.proj = new Projection("EPSG:2927")
        map.addLayer(shp)
        map.bounds = shp.bounds
        def image = map.renderToImage()
        assertNotNull(image)

        File out = new File(folder,"map.png")
        javax.imageio.ImageIO.write(image, "png", out);
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_image.png"), ImageIO.read(out), 200)
    }

    @Test void renderRasterToImage() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)

        GeoTIFF geoTIFF = new GeoTIFF(file)
        Raster raster = geoTIFF.read()

        Map map = new Map()
        map.proj = new Projection("EPSG:2927")
        map.addRaster(raster)
        map.bounds = raster.bounds
        def image = map.renderToImage()
        assertNotNull(image)

        File out = new File(folder,"raster.png")
        javax.imageio.ImageIO.write(image, "png", out);
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_raster.png"), ImageIO.read(out), 100)
    }

    @Test void renderDemRaster() {
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        assertNotNull(file)

        GeoTIFF geoTIFF = new GeoTIFF(file)
        Raster raster = geoTIFF.read()
        raster.style = new  geoscript.style.ColorMap([[color: "#008000", quantity:70], [color:"#663333", quantity:256]])

        Map map = new Map()
        map.addRaster(raster)
        def image = map.renderToImage()
        assertNotNull(image)

        File out = new File(folder,"raster.png")
        javax.imageio.ImageIO.write(image, "png", out);
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_dem.png"), ImageIO.read(out), 100)
    }

    @Test void renderTileLayer() {
        MBTiles layer = new MBTiles(new File(getClass().getClassLoader().getResource("states.mbtiles").toURI()))
        Shapefile shp = new Shapefile(new File(getClass().getClassLoader().getResource("states.shp").toURI()))
        shp.style = new Stroke("#ff0000", 2.0)

        Map map = new Map()
        map.addTileLayer(layer)
        map.addLayer(shp)
        def image = map.renderToImage()
        assertNotNull(image)

        File out = new File(folder,"raster.png")
        javax.imageio.ImageIO.write(image, "png", out);
        assertTrue(out.exists())
        map.close()
    }

    @Test void renderToImageWithMapNoProjection() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)

        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.addLayer(shp)
        map.bounds = shp.bounds
        def image = map.renderToImage()
        assertNotNull(image)

        File out = new File(folder,"map.png")
        javax.imageio.ImageIO.write(image, "png", out);
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_image_noproj.png"), ImageIO.read(out), 200)
    }

    @Test void renderToImageWithMapBoundsNoProjection() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)

        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.addLayer(shp)
        map.bounds = new Bounds(-126, 45.315, -116, 50.356)
        def image = map.renderToImage()
        assertNotNull(image)

        File out = new File(folder,"map.png")
        javax.imageio.ImageIO.write(image, "png", out);
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_image_noproj_nobounds.png"), ImageIO.read(out), 200)
    }

    @Test void renderToFile() {
        File out = new File(folder,"map.png")
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.proj = new Projection("EPSG:2927")
        map.addLayer(shp)
        map.bounds = shp.bounds
        map.render(out)
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_file.png"), ImageIO.read(out), 200)
    }

    @Test void renderToContinuousFile() {
        File out = new File(folder,"map.png")
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.width = 400
        map.height = 100
        map.proj = new Projection("EPSG:4326")
        map.addLayer(shp)
        map.bounds = new Bounds(-180, -90, 180, 90, "EPSG:4326")
        map.render(out)
        assertTrue(out.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_continuous.png"), ImageIO.read(out), 100)
    }

    @Test void renderToOutputStream() {
        File f = new File(folder,"map.png")
        FileOutputStream out = new FileOutputStream(f)

        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.proj = new Projection("EPSG:2927")
        map.addLayer(shp)
        map.bounds = shp.bounds
        map.render(out)
        out.close()
        assertTrue(f.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map_to_out.png"), ImageIO.read(f), 200)
    }

    @Test void renderToPdf() {
        File f = new File(folder,"map.pdf")

        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)
        shp.style = new Fill("white") + new Stroke("#CCCCCC", 0.1)

        Map map = new Map(type:"pdf", layers:[shp])
        map.addLayer(shp)
        map.render(f)
        assertTrue(f.exists())
        map.close()
    }

    @Test void renderToSvg() {
        File f = new File(folder,"map.svg")

        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)
        shp.style = new Fill("white") + new Stroke("#CCCCCC", 0.1)

        Map map = new Map(type:"svg", layers:[shp])
        map.render(f)
        assertTrue(f.exists())
        map.close()
    }

    @Test void renderToJpeg() {
        File f = new File(folder,"map.jpeg")

        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)
        shp.style = new Fill("white") + new Stroke("#CCCCCC", 0.1)

        Map map = new Map(type:"jpeg", layers: [shp])
        map.render(f)
        assertTrue(f.exists())
        map.close()

        ImageAssert.assertEquals(getFile("geoscript/render/map.jpeg"), ImageIO.read(f), 100)
    }

    @Test void renderToGif() {
        File f = new File(folder,"map.gif")

        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)
        shp.style = new Fill("white") + new Stroke("#CCCCCC", 0.1)

        Map map = new Map(type:"gif", layers: [shp])
        map.render(f)
        assertTrue(f.exists())
        map.close()

        // File f2 = new File(folder,"map2.gif")
        // ImageIO.write(ImageIO.read(f), 'gif', f2)
        // ImageAssert.assertEquals(getFile("geoscript/render/map.gif"), ImageIO.read(f2), 100)
    }

    @Test void renderToBase64() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)
        shp.style = new Fill("white") + new Stroke("#CCCCCC", 0.1)

        Map map = new Map(type:"base64", layers: [shp])
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        map.render(out)
        map.close()
        byte[] bytes = out.toByteArray()
        assertTrue(bytes.length > 0)
    }

    @Test void getScaleDenominator() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.proj = new Projection("EPSG:2927")
        map.addLayer(shp)
        map.bounds = shp.bounds
        assertEquals(38273743.41534821, map.scaleDenominator, 0.01)
    }

    @Test void getBounds() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        assertNotNull(file)
        Shapefile shp = new Shapefile(file)
        assertNotNull(shp)

        Map map = new Map()
        map.proj = new Projection("EPSG:2927")
        map.addLayer(shp)
        map.bounds = shp.bounds
        assertEquals(shp.bounds.minX, map.bounds.minX, 0.01)
        assertEquals(shp.bounds.maxX, map.bounds.maxX, 0.01)
        assertEquals(shp.bounds.minY, map.bounds.minY, 0.01)
        assertEquals(shp.bounds.maxY, map.bounds.maxY, 0.01)
        assertEquals(shp.bounds.proj.id, map.bounds.proj.id)
    }
}
