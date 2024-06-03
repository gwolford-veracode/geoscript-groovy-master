package geoscript.workspace

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import geoscript.layer.*
import geoscript.feature.*
import geoscript.geom.Point

/**
 *
 */
class MemoryTest {

    @Test void create() {
        Memory mem = new Memory()
        Layer l = mem.create('widgets',[new Field("geom", "Point"), new Field("name", "String")])
        assertNotNull(l)
        l.add([new Point(1,1), "one"])
        l.add([new Point(2,2), "two"])
        l.add([new Point(3,3), "three"])
        assertEquals 3, l.count()
    }

    @Test void add() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shp = new Shapefile(file)

        Memory mem = new Memory()
        Layer l = mem.add(shp, 'counties')
        assertEquals shp.count(), l.count()
    }

    @Test void format() {
        Memory m = new Memory()
        assertEquals "Memory", m.format
    }

    @Test void remove() {
        Workspace workspace = new Memory()
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
}

