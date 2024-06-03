package geoscript.filter

import geoscript.geom.GeometryCollection
import geoscript.layer.Layer
import geoscript.layer.Shapefile
import geoscript.process.Process
import geoscript.style.Fill
import geoscript.style.Stroke
import geoscript.style.Transform
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * The ProcessFunction Unit Test.
 * @author Jared Erickson
 */
class ProcessFunctionTest {

    @TempDir
    private File folder

    @Test void function() {
        Process p = new Process("convexhull",
                "Create a convexhull around the features",
                [features: geoscript.layer.Cursor],
                [result: geoscript.layer.Cursor],
                { inputs ->
                    def geoms = new GeometryCollection(inputs.features.collect{f -> f.geom})
                    def output = new Layer()
                    output.add([geoms.convexHull])
                    [result: output]
                }
        )
        Function f = new Function(p, new Function("parameter", new Expression("features")))

        File imgFile = new File(folder, "states_function.png")
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        def statesShp = new Shapefile(file)

        def sym = (new Stroke("red",0.4) + new Transform(f, Transform.RENDERING)).zindex(1) + (new Fill("#E6E6E6") + new Stroke("#4C4C4C",0.5)).zindex(2)
        assertTrue sym.sld.contains("<ogc:Function name=\"geoscript:convexhull\">")
        statesShp.style = sym

        def map = new geoscript.render.Map(width: 600, height: 400, fixAspectRatio: true)
        map.proj = "EPSG:4326"
        map.addLayer(statesShp)
        map.bounds = statesShp.bounds
        map.render(imgFile)
        assertTrue imgFile.length() > 0
    }

    @Test void processFunction() {
        Process p = new Process("bounds",
                "Create bounds around features",
                [features: geoscript.layer.Cursor],
                [result: geoscript.layer.Cursor],
                { inputs ->
                    def geoms = new GeometryCollection(inputs.features.collect{f -> f.geom})
                    def output = new Layer()
                    output.add([geoms.bounds.geometry])
                    [result: output]
                }
        )
        ProcessFunction processFunction = new ProcessFunction(p, new Function("parameter", new Expression("features")))

        File imgFile = new File(folder, "states_bounds_function.png")
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        def statesShp = new Shapefile(file)

        def sym = (new Stroke("red",0.4) + new Transform(processFunction, Transform.RENDERING)).zindex(1) + (new Fill("#E6E6E6") + new Stroke("#4C4C4C",0.5)).zindex(2)
        assertTrue sym.sld.contains("<ogc:Function name=\"geoscript:bounds\">")
        statesShp.style = sym

        def map = new geoscript.render.Map(width: 600, height: 400, fixAspectRatio: true)
        map.proj = "EPSG:4326"
        map.addLayer(statesShp)
        map.bounds = statesShp.bounds
        map.render(imgFile)
        assertTrue imgFile.length() > 0
    }
}
