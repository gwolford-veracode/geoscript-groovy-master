package geoscript.layer

import geoscript.geom.Bounds
import geoscript.geom.Point
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*

/**
 * The RasterAlgebra UnitTest
 * @author Jared Erickson
 */
class MapAlgebraTest {

    @TempDir
    private File folder

    @Test void greatThan() {
        File rasterFile = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        GeoTIFF geotiff = new GeoTIFF(rasterFile)
        Raster raster = geotiff.read()
        MapAlgebra algebra = new MapAlgebra()
        Raster output = algebra.calculate("dest = src > 200;", [src: raster], size: [600, 400])
        assertNotNull output
        File file = new File(folder, "greaterthan.tif")
        GeoTIFF outTiff = new GeoTIFF(file)
        outTiff.write(output)
        assertTrue file.size() > 0
    }

    @Test void waves() {
        MapAlgebra algebra = new MapAlgebra()
        Raster output = algebra.calculate("""init {
              // image centre coordinates
              xc = width() / 2;
              yc = height() / 2;

              // constant term
              C = M_PI * 8;
            }

            dx = (x() - xc) / xc;
            dy = (y() - yc) / yc;
            d = sqrt(dx*dx + dy*dy);

            destImg = sin(C * d);""", null, outputName: "destImg")
        assertNotNull output
        File file = new File(folder, "waves.tif")
        GeoTIFF geotiff = new GeoTIFF(file)
        geotiff.write(output)
        assertTrue file.size() > 0
    }

    @Test void minus() {

        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [5,5,5,5,5,5,5],
                [5,6,6,6,6,6,5],
                [5,6,7,6,7,6,5],
                [5,6,6,6,6,6,5],
                [5,5,5,5,5,5,5]
        ]
        Raster raster1 = new Raster(data1, bounds)

        List data2 = [
                [1,1,1,1,1,1,1],
                [1,2,2,2,2,2,1],
                [1,2,3,4,3,2,1],
                [1,2,2,2,2,2,1],
                [1,1,1,1,1,1,1]
        ]
        Raster raster2 = new Raster(data2, bounds)

        MapAlgebra mapAlgebra = new MapAlgebra()
        Raster raster3 = mapAlgebra.calculate("dest = src1 - src2;", [src1: raster1, src2: raster2])

        assertEquals(4, raster3.eval(new Point(0.5,0.5))[0], 0.1)
        assertEquals(4, raster3.eval(new Point(1.5,1.5))[0], 0.1)
        assertEquals(4, raster3.eval(new Point(2.5,2.5))[0], 0.1)
        assertEquals(2, raster3.eval(new Point(3.5,2.5))[0], 0.1)
    }
}