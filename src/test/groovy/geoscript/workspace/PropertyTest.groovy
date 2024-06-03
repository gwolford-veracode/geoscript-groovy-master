package geoscript.workspace

import geoscript.FileUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*
import geoscript.layer.Layer
import geoscript.feature.Field
import geoscript.feature.Schema

/**
 * The Property Workspace UnitTest
 * @author Jared Erickson
 */
class PropertyTest {

    @TempDir
    File folder

    @Test void read() {

        File dir = new File(getClass().getClassLoader().getResource("points.properties").toURI()).parentFile
        assertNotNull(dir)

        Property property = new Property(dir)
        assertNotNull(property)
        assertEquals "Property", property.format
        assertEquals "Property[${dir}]".toString(), property.toString()

        assertEquals 2, property.layers.size()
        assertTrue property.has("points")
        assertTrue property.has("earthquakes")

        Layer layer = property.get("points")
        assertNotNull(layer)
        assertEquals 4, layer.count

        def features = layer.features

        assertEquals "point 1", features[0].get("name")
        assertEquals "point 2", features[1].get("name")
        assertEquals "point 3", features[2].get("name")
        assertEquals "point 4", features[3].get("name")

        assertEquals "POINT (0 0)", features[0].geom.wkt
        assertEquals "POINT (10 10)", features[1].geom.wkt
        assertEquals "POINT (20 20)", features[2].geom.wkt
        assertEquals "POINT (30 30)", features[3].geom.wkt

        layer = property.get("points.properties")
        assertNotNull(layer)
        assertEquals 4, layer.count
    }

    @Test void create() {
        File dir = FileUtil.createDir(folder,"points")
        Property property = new Property(dir)
        Layer layer = property.create("points", [new Field("geom","Point","EPSG:4326")])
        assertNotNull(layer)
        assertTrue(new File(dir,"points.properties").exists())

        Layer layer2 = property.create(new Schema("lines", [new Field("geom","Point","EPSG:4326")]))
        assertNotNull(layer2)
        assertTrue(new File(dir,"lines.properties").exists())
    }

    @Test void add() {
        File shpDir = new File(getClass().getClassLoader().getResource("states.shp").toURI()).parentFile
        Directory directory = new Directory(shpDir)
        Layer statesLayer = directory.get("states")

        File tempDir = FileUtil.createDir(folder,"states")
        Property property = new Property(tempDir)
        Layer propLayer = property.add(statesLayer, "states")
        assertTrue(new File(tempDir,"states.properties").exists())
        assertEquals statesLayer.count, propLayer.count
    }

    @Test void remove() {
        File directory = FileUtil.createDir(folder,"layers")
        Workspace workspace = new Property(directory)
        workspace.create("points",[new Field("geom","Point","EPSG:4326")])
        workspace.create("lines",[new Field("geom","LineString","EPSG:4326")])
        workspace.create("polygons",[new Field("geom","Polygon","EPSG:4326")])
        assertTrue workspace.has("points")
        assertTrue workspace.has("lines")
        assertTrue workspace.has("polygons")
        workspace.remove("points")
        workspace.remove(workspace.get("lines"))
        workspace.remove(workspace.get("polygons"))
        assertFalse workspace.has("points")
        assertFalse workspace.has("lines")
        assertFalse workspace.has("polygons")
    }

    @Test void getWorkspaceFromString() {
        File file = new File(getClass().getClassLoader().getResource("points.properties").toURI())
        Property prop = Workspace.getWorkspace("type=property file=${file}")
        assertNotNull prop
        assertTrue prop.names.contains("points")
        prop = Workspace.getWorkspace("type=property file=${file.parentFile}")
        assertNotNull prop
        println prop.names
        assertTrue prop.names.contains("points")
    }

    @Test void getWorkspaceFromMap() {
        File file = new File(getClass().getClassLoader().getResource("points.properties").toURI())
        Property prop = Workspace.getWorkspace([type: 'property', file:file])
        assertNotNull prop
        assertTrue prop.names.contains("points")
        prop = Workspace.getWorkspace([type: 'property', file:file.parentFile])
        assertNotNull prop
        assertTrue prop.names.contains("points")
    }
}
