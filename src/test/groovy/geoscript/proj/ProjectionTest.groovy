package geoscript.proj

import org.geotools.referencing.CRS
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import static geoscript.AssertUtil.assertPointsAreEqual
import geoscript.geom.Point
import geoscript.geom.Bounds

/**
 * The Projection UnitTest
 */
class ProjectionTest {

    @Test void constructors() {

        Projection p1 = new Projection("EPSG:2927")
        assertEquals "EPSG:2927", p1.id

        Projection p2 = new Projection("""GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",SPHEROID["WGS_1984",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["Degree",0.017453292519943295]]""")
        assertEquals "EPSG:4326", p2.id

        Projection p3 = new Projection(org.geotools.referencing.CRS.decode("EPSG:2927"))
        assertEquals "EPSG:2927", p3.id

        Projection p4 = new Projection(p1)
        assertEquals "EPSG:2927", p4.id

        Projection p5 = new Projection(42001, new Point(-102.304688,39.250413))
        assertEquals "EPSG:32613", p5.id

    }

    @Test void createFromWellKnownName() {
        assertNotNull(new Projection("WGS84"))
        assertNotNull(new Projection("Mollweide"))
        assertNotNull(new Projection("WagnerIV"))
        assertNotNull(new Projection("WagnerV"))
        assertNotNull(new Projection("WorldVanderGrintenI"))
        assertNotNull(new Projection("EqualEarth"))
    }

    @Test void parseError() {
        assertThrows(Exception) {
            new Projection("BAD PROJECTION STRING")
        }
    }

    @Test void getId() {
        Projection p1 = new Projection("EPSG:2927")
        assertEquals "EPSG:2927", p1.id
    }

    @Test void getEpsg() {
        Projection p = new Projection("EPSG:2927")
        assertEquals 2927, p.epsg
        p = new Projection("EPSG:4326")
        assertEquals 4326, p.epsg
        p = new Projection("EPSG:3857")
        assertEquals 3857, p.epsg
    }

    @Test void getSrs() {
        Projection p = new Projection("EPSG:2927")
        assertEquals "EPSG:2927", p.srs
        p = new Projection("EPSG:4326")
        assertEquals "EPSG:4326", p.srs
        p = new Projection("EPSG:3857")
        assertEquals "EPSG:3857", p.srs
        p = new Projection("urn:ogc:def:crs:EPSG::4326")
        assertEquals "urn:ogc:def:crs:EPSG::4326", p.srs
        assertEquals "4326", p.getSrs(true)
        assertEquals 4326, p.epsg
        assertEquals "EPSG:4326", p.id
    }

    @Test void getWkt() {
        Projection p = new Projection("EPSG:4326")
        String expected = """GEOGCS["WGS 84",
  DATUM["World Geodetic System 1984", 
    SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]], 
    AUTHORITY["EPSG","6326"]], 
  PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], 
  UNIT["degree", 0.017453292519943295], 
  AXIS["Geodetic longitude", EAST], 
  AXIS["Geodetic latitude", NORTH], 
  AUTHORITY["EPSG","4326"]]""".replaceAll(" ","").replaceAll("\n","")
        String actual = p.wkt.replaceAll(" ","").replaceAll(System.getProperty("line.separator"),"")
        assertEquals expected, actual
    }

    @Test void getEsriWkt() {
        Projection p = new Projection("EPSG:4326")
        String expected = "GEOGCS[\"WGS 84\", DATUM[\"D_WGS_1984\", SPHEROID[\"D_WGS_1984\", 6378137.0, 298.257223563, " +
                "AUTHORITY[\"EPSG\",\"7030\"]], AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\", 0.0, " +
                "AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"Degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], " +
                "AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4326\"]]"
        String actual = p.getWkt("esri", 0)
        assertEquals expected, actual
    }

    @Test void getBounds() {
        Bounds bounds = new Projection("EPSG:2927").bounds
        assertNotNull(bounds)
        assertEquals "EPSG:2927", bounds.proj.id
        assertEquals(641400.91, bounds.minX, 0.1)
        assertEquals(75405.06, bounds.minY, 0.1)
        assertEquals(2560080.68, bounds.maxX, 0.1)
        assertEquals(854063.65, bounds.maxY, 0.1)
    }

    @Test void getGeoBounds() {
        Bounds bounds = new Projection("EPSG:2927").geoBounds
        assertNotNull(bounds)
        assertEquals "EPSG:4326", bounds.proj.id
        assertEquals(-124.5, bounds.minX, 0.1)
        assertEquals(45.55, bounds.minY, 0.1)
        assertEquals(-116.9, bounds.maxX, 0.1)
        assertEquals(47.6, bounds.maxY, 0.1)
    }

    @Test void transform() {
        Point point = new Point(1181199.82, 652100.72)
        Projection src = new Projection("EPSG:2927")
        Projection dest = new Projection("EPSG:4326")
        Point projectedPoint = src.transform(point, dest)
        assertPointsAreEqual(new Point(-122.34427530579649, 47.10678696848592), projectedPoint, 0.0000000001)
        projectedPoint = src.transform(point, "EPSG:4326")
        assertPointsAreEqual(new Point(-122.34427530579649, 47.10678696848592), projectedPoint, 0.0000000001)
    }

    @Test void testToString() {
        Projection p1 = new Projection("EPSG:2927")
        assertEquals "EPSG:2927", p1.toString()
    }

    @Test void testEquals() {
        assertTrue new Projection("EPSG:2927") == new Projection("EPSG:2927")
        assertFalse new Projection("EPSG:2927") == new Projection("EPSG:4326")
        assertFalse new Projection("EPSG:2927") == "String"
    }

    @Test void testHashcode() {
        assertTrue new Projection("EPSG:2927").hashCode() == new Projection("EPSG:2927").hashCode()
        assertFalse new Projection("EPSG:2927").hashCode() == new Projection("EPSG:4326").hashCode()
    }

    @Test void staticTransform() {
        Point point = new Point(1181199.82, 652100.72)
        Projection src = new Projection("EPSG:2927")
        Projection dest = new Projection("EPSG:4326")
        Point projectedPoint = Projection.transform(point, src, dest)
        assertPointsAreEqual(new Point(-122.34427530579649,47.10678696848592), projectedPoint, 0.000000001)
    }

    @Test void transform26916To4326() {
        Point point = new Point(776041.0, 3386618.0)
        Point projectedPoint = Projection.transform(point, "EPSG:26916", "EPSG:4326")
        assertPointsAreEqual(new Point(-84.121611219545, 30.580286157377163), projectedPoint, 0.000000001)
    }

    @Test void projections() {
        CRS.metaClass.static.getSupportedCodes = { String type ->
            println "Mocking CRS.getSupportedCodes..."
            [4326,2927]
        }
        List<Projection> projections = Projection.projections()
        assertEquals 2, projections.size()
        CRS.metaClass = null
    }

    @Test void hasEpsgExtension() {
        assertNotNull new Projection("EPSG:104905")
    }

    @Test void toMeters() {
        Projection p = new Projection("EPSG:2927")
        double meters = p.toMeters(5280)
        assertEquals(1609.3472, meters, 0.001)
    }

}

