package geoscript.render

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.feature.Schema
import geoscript.geom.Geometry
import geoscript.geom.LineString
import geoscript.layer.Layer
import geoscript.layer.Shapefile
import geoscript.workspace.Memory
import org.geotools.image.test.ImageAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static geoscript.render.Plot.*
import static org.junit.jupiter.api.Assertions.*

/**
 * The Plot UnitTest
 * @author Jared Erickson
 */
class PlotTest {

    @TempDir
    File folder

    private File getFile(String resource) {
        return new File(getClass().getClassLoader().getResource(resource).toURI())
    }

    @Test
    void plotGeometryToFile() {
        Geometry geom = Geometry.fromWKT("POINT (-111 45.7)").buffer(10)
        File file = new File(folder,"plot.png")
        plot(geom, size: [400, 400], out: file)
        assertTrue file.exists()
        assertTrue file.length() > 0
        ImageAssert.assertEquals(getFile("geoscript/render/plot_geom_to_file.png"), ImageIO.read(file), 10000)
    }

    @Test
    void plotGeometryToOutputStream() {
        Geometry geom = Geometry.fromWKT("POINT (-111 45.7)").buffer(10)
        File file = new File(folder,"plot.png")
        OutputStream out = new FileOutputStream(file)
        plot(geom, size: [400, 400], out: out, type: "png")
        assertTrue file.exists()
        assertTrue file.length() > 0
        ImageAssert.assertEquals(getFile("geoscript/render/plot_geom_to_out.png"), ImageIO.read(file), 10000)
    }

    @Test
    void plotGeometryToImage() {
        Geometry geom = Geometry.fromWKT("POINT (-111 45.7)").buffer(10)
        BufferedImage image = plotToImage(geom, size: [400, 400])
        File file = new File(folder,"plot.png")
        ImageIO.write(image, "png", file)
        assertTrue file.exists()
        assertTrue file.length() > 0
        ImageAssert.assertEquals(getFile("geoscript/render/plot_geom_to_img.png"), ImageIO.read(file), 10000)
    }

    @Test
    void plotFeatureToImage() {
        Schema schema = new Schema("shapes", [new Field("geom", "Polygon"), new Field("name", "String")])
        Feature feature = new Feature([new LineString([0, 0], [1, 1]).bounds.polygon, "square"], "0", schema)
        BufferedImage image = plotToImage(feature, size: [400, 400])
        File file = new File(folder,"plot_feature.png")
        ImageIO.write(image, "png", file)
        assertTrue file.exists()
        assertTrue file.length() > 0
        ImageAssert.assertEquals(getFile("geoscript/render/plot_feat_to_file.png"), ImageIO.read(file), 10000)
    }

    @Test
    void plotLayerToImage() {
        File shpFile = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shp = new Shapefile(shpFile)
        Memory mem = new Memory()
        Layer layer = mem.add(shp).filter("STATE_ABBR IN ('ND','SD','MT')", "nd_sd_mt")
        BufferedImage image = plotToImage(layer, size: [400, 400])
        File file = new File(folder,"plot_layer.png")
        ImageIO.write(image, "png", file)
        assertTrue file.exists()
        assertTrue file.length() > 0
        ImageAssert.assertEquals(getFile("geoscript/render/plot_layer_to_file.png"), ImageIO.read(file), 10000)
    }
}
