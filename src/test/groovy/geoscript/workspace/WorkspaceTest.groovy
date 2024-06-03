package geoscript.workspace

import geoscript.FileUtil
import geoscript.feature.Schema
import geoscript.geom.Point
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*
import geoscript.layer.Layer

/**
 * The Workspace Unit Test
 * @author Jared Erickson
 */
class WorkspaceTest {

    @TempDir
    File folder

    @Test void constructorWithMapOfParams() {
        // H2
        Workspace h2 = new Workspace(["dbtype": "h2", "database": new File(folder,"roads.db").absolutePath])
        assertNotNull(h2.ds)
        assertEquals("org.geotools.jdbc.JDBCDataStore", h2.format)
        h2.close()

        // Shapefile
        URL url = getClass().getClassLoader().getResource("states.shp")
        Workspace shp = new Workspace(["url": url])
        assertNotNull(shp.ds)
        assertEquals("org.geotools.data.shapefile.ShapefileDataStore", shp.format)
    }

    @Test void constructorWithParamString() {
        // H2
        Workspace h2 = new Workspace("dbtype=h2 database=" + new File(folder,"roads.db").absolutePath)
        assertNotNull(h2.ds)
        assertEquals("org.geotools.jdbc.JDBCDataStore", h2.format)
        h2.close()

        // Shapefile
        URL url = getClass().getClassLoader().getResource("states.shp")
        Workspace shp = new Workspace("url='${url}' 'create spatial index'=true")
        assertNotNull(shp.ds)
        assertEquals("org.geotools.data.shapefile.ShapefileDataStore", shp.format)

        Workspace shp2 = new Workspace("${url}")
        assertNotNull(shp2.ds)
        assertEquals("org.geotools.data.directory.DirectoryDataStore", shp2.format)

        Workspace shp3 = new Workspace("${url.file}")
        assertNotNull(shp3.ds)
        assertEquals("org.geotools.data.directory.DirectoryDataStore", shp3.format)

        Workspace dir = new Workspace("${new File(url.file).getAbsoluteFile().getParent()}")
        assertNotNull(dir.ds)
        assertEquals("org.geotools.data.directory.DirectoryDataStore", dir.format)

        // Properties
        File propFile = new File(getClass().getClassLoader().getResource("points.properties").file)
        Workspace prop = new Workspace("directory='${propFile}'")
        assertNotNull(prop.ds)
        assertEquals("org.geotools.data.property.PropertyDataStore", prop.format)

        Workspace prop2 = new Workspace(propFile.absolutePath)
        assertNotNull(prop2.ds)
        assertEquals("org.geotools.data.property.PropertyDataStore", prop2.format)

        Workspace prop3 = new Workspace(new File(propFile.parentFile, "asdfasdfas.properties").absolutePath)
        assertNotNull(prop3.ds)
        assertEquals("org.geotools.data.property.PropertyDataStore", prop3.format)
    }

    @Test void getParametersFromString() {
        // PostGIS
        Map params = Workspace.getParametersFromString("dbtype=postgis database=postgres host=localhost port=5432");
        assertEquals(params['dbtype'], "postgis")
        assertEquals(params['database'], "postgres")
        assertEquals(params['host'], "localhost")
        assertEquals(params['port'], "5432")

        // Neo4j
        params = Workspace.getParametersFromString("'The directory path of the neo4j database'=/opt/neo4j/data/graph.db")
        assertEquals(params['The directory path of the neo4j database'], "/opt/neo4j/data/graph.db")

        // H2
        params = Workspace.getParametersFromString("dbtype=h2 database='C:\\My Data\\my.db'");
        assertEquals(params['dbtype'], "h2")
        assertEquals(params['database'], "C:\\My Data\\my.db")

        params = Workspace.getParametersFromString("C:\\My Data\\my.db");
        assertEquals(params['dbtype'], "h2")
        assertTrue params['database'].toString().endsWith("my.db")

        // Shapefile
        params = Workspace.getParametersFromString("/my/states.shp")
        assertEquals("shapefile", params.type)
        assertTrue(params.containsKey("file"))
        assertTrue(params["file"] instanceof File)
        assertTrue(params["file"].toString().endsWith("my"))

        params = Workspace.getParametersFromString("url='/my/states.shp' 'create spatial index'=true")
        assertTrue(params.containsKey("url"))
        assertTrue(params["url"] instanceof URL)
        assertTrue(params["url"].toString().endsWith("my/states.shp"))
        assertEquals(params['create spatial index'], "true")

        // Property
        params = Workspace.getParametersFromString("directory=/my/states.properties")
        assertTrue(params.containsKey("directory"))
        assertTrue(params['directory'].toString().endsWith("my/states.properties"))

        params = Workspace.getParametersFromString("directory=/my/propertyfiles")
        assertTrue(params.containsKey("directory"))
        assertTrue(params['directory'].toString().endsWith("my/propertyfiles"))

        params = Workspace.getParametersFromString("/my/states.properties")
        assertTrue(params.containsKey("directory"))
        assertTrue(params['directory'].toString().endsWith("my"))

        // GeoPackage
        params = Workspace.getParametersFromString("dbtype=geopkg database=layers.gpkg")
        assertEquals(params['dbtype'], "geopkg")
        assertEquals(params['database'], "layers.gpkg")

        params = Workspace.getParametersFromString("layers.gpkg")
        assertEquals(params['dbtype'], "geopkg")
        assertTrue(params['database'].toString().endsWith("layers.gpkg"))

        // Spatialite
        params = Workspace.getParametersFromString("dbtype=spatialite database=layers.sqlite")
        assertEquals(params['dbtype'], "spatialite")
        assertEquals(params['database'], "layers.sqlite")

        params = Workspace.getParametersFromString("layers.sqlite")
        assertEquals(params['dbtype'], "sqlite")
        assertTrue(params['database'].toString().endsWith("layers.sqlite"))

        // WFS
        String wfsUrl = "http://localhost:8080/geoserver/ows?service=wfs&version=1.1.0&request=GetCapabilities"
        params = Workspace.getParametersFromString(wfsUrl)
        assertEquals params["WFSDataStoreFactory:GET_CAPABILITIES_URL"], wfsUrl

        // Memory
        params = Workspace.getParametersFromString("memory")
        assertTrue params.containsKey("type")
        assertEquals "memory", params.type
    }

    @Test void getWorkspaceNames() {
        List names = Workspace.workspaceNames
        assertTrue names.size() > 0
        assertTrue names.contains("Shapefile")
    }

    @Test void getWorkspaceParameters() {
        // H2
        List params = Workspace.getWorkspaceParameters("H2")
        assertTrue params.size() > 0
        assertNotNull params.find {p ->
            p.key.equals("dbtype") ? p : null
        }
        assertNotNull params.find {p ->
            p.key.equals("database") ? p : null
        }

        // Shapefile
        params = Workspace.getWorkspaceParameters("Shapefile")
        assertTrue params.size() > 0
        assertNotNull params.find {p ->
            p.key.equals("url") ? p : null
        }
        assertNotNull params.find {p ->
            p.key.equals("create spatial index") ? p : null
        }
    }

    @Test void add() {
        Layer layer1 = new Memory().create(new Schema("points", [["the_geom", "Point", "EPSG:4326"],["name","String"]]))
        layer1.add([new Point(1,1),"point1"])
        layer1.add([new Point(2,2),"point2"])
        layer1.add([new Point(3,3),"point3"])

        File file = FileUtil.createDir(folder,"points")
        Directory dir = new Directory(file)
        Layer layer2 = dir.add(layer1)
        assertTrue(new File(file,"points.shp").exists())
        assertEquals 3, layer2.count
    }

    @Test void has() {
        Workspace workspace = new Memory()
        assertFalse workspace.has("points")
        workspace.create("points", [["the_geom", "Point", "EPSG:4326"]])
        assertTrue workspace.has("points")
    }

    @Test void getWorkspaceWithParams() {
        // H2
        Workspace h2 = Workspace.getWorkspace(["dbtype": "h2", "database": new File(folder,"roads.db").absolutePath])
        assertNotNull(h2.ds)
        assertTrue(h2 instanceof H2)
        h2.close()

        // Shapefile
        URL url = getClass().getClassLoader().getResource("states.shp")
        Workspace shp = Workspace.getWorkspace(["url": url])
        assertNotNull(shp.ds)
        assertTrue(shp instanceof Directory)
    }

    @Test void getWorkspaceWithParamString() {
        // H2
        Workspace h2 = Workspace.getWorkspace("dbtype=h2 database=" + new File(folder,"roads.db").absolutePath)
        assertNotNull(h2.ds)
        assertTrue(h2 instanceof H2)
        h2.close()

        // Shapefile
        URL url = getClass().getClassLoader().getResource("states.shp")
        Workspace shp = Workspace.getWorkspace("url='${url}' 'create spatial index'=true")
        assertNotNull(shp.ds)
        assertTrue(shp instanceof Directory)

        Workspace shp2 = Workspace.getWorkspace("${url}")
        assertNotNull(shp2.ds)
        assertTrue(shp instanceof Directory)

        Workspace shp3 = Workspace.getWorkspace("${url.file}")
        assertNotNull(shp3.ds)
        assertTrue(shp instanceof Directory)

        Workspace dir = Workspace.getWorkspace("${new File(url.file).getAbsoluteFile().getParent()}")
        assertNotNull(dir.ds)
        assertTrue(shp instanceof Directory)

        // Properties
        File propFile = new File(getClass().getClassLoader().getResource("points.properties").file)
        Workspace prop = Workspace.getWorkspace("directory='${propFile}'")
        assertNotNull(prop.ds)
        assertTrue(prop instanceof Property)

        Workspace prop2 = Workspace.getWorkspace(propFile.absolutePath)
        assertNotNull(prop2.ds)
        assertTrue(prop instanceof Property)

        Workspace prop3 = Workspace.getWorkspace(new File(propFile.parentFile, "asdfasdfas.properties").absolutePath)
        assertNotNull(prop3.ds)
        assertTrue(prop instanceof Property)

        // Memory
        Workspace mem = Workspace.getWorkspace("memory")
        assertNotNull(mem.ds)
        assertTrue(mem instanceof Memory)
        mem = Workspace.getWorkspace([type: "memory"])
        assertNotNull(mem.ds)
        assertTrue(mem instanceof Memory)
    }

    @Test void badWorkspaceString() {
        assertThrows(IllegalArgumentException) {
            Workspace w = Workspace.getWorkspace("BAD_INPUT")
        }
    }

    @Test void withWorkspace() {
        Workspace.withWorkspace(["dbtype": "h2", "database": new File(folder,"roads_1.db").absolutePath]) { Workspace w ->
            assertNotNull w
            assertEquals "H2", w.format
        }
        Workspace.withWorkspace("dbtype=h2 database=" + new File(folder,"roads_2.db").absolutePath) { Workspace w ->
            assertNotNull w
            assertEquals "H2", w.format
        }
        Workspace.withWorkspace(new H2(new File(folder,"roads_3.db").absolutePath)) { Workspace w ->
            assertNotNull w
            assertEquals "H2", w.format
        }
    }
}
