package geoscript.geom

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import geoscript.proj.Projection
import org.geotools.geometry.jts.ReferencedEnvelope
import org.locationtech.jts.geom.Envelope

/**
 * The Bounds unit test
 */
class BoundsTest {
	
    @Test void constructors() {
        ReferencedEnvelope e = new ReferencedEnvelope(1,3,2,4,null)
        Bounds b1 = new Bounds(e)
        assertEquals "(1.0,2.0,3.0,4.0)", b1.toString()
		
        Bounds b2 = new Bounds(1,2,3,4, new Projection("EPSG:2927"))
        assertEquals "(1.0,2.0,3.0,4.0,EPSG:2927)", b2.toString()
		
        Bounds b3 = new Bounds(1,2,3,4)
        assertEquals "(1.0,2.0,3.0,4.0)", b3.toString()

        Bounds b4 = new Bounds(1,2,3,4, "EPSG:2927")
        assertEquals "(1.0,2.0,3.0,4.0,EPSG:2927)", b4.toString()

        Bounds b5 = new Bounds(new Envelope(1,2,3,4))
        assertEquals "(1.0,3.0,2.0,4.0)", b5.toString()
        assertNull b5.proj

        // Point as origin with width and height
        Bounds b6 = new Bounds(new Point(0,0), 10 , 10)
        assertEquals "(0.0,0.0,10.0,10.0)", b6.toString()
        assertNull b6.proj

        // Point as center with width and height
        Bounds b7 = new Bounds(new Point(5,5), 10 , 10, false)
        assertEquals "(0.0,0.0,10.0,10.0)", b7.toString()
        assertNull b7.proj
    }
	
    @Test void minX() {
        Bounds b = new Bounds(1,2,3,4)
        assertEquals 1.0, b.minX, 0.0
    }

    @Test void minY() {
        Bounds b = new Bounds(1,2,3,4)
        assertEquals 2.0, b.minY, 0.0
    }

    @Test void maxX() {
        Bounds b = new Bounds(1,2,3,4)
        assertEquals 3.0, b.maxX, 0.0
    }
	
    @Test void maxY() {
        Bounds b = new Bounds(1,2,3,4)
        assertEquals 4.0, b.maxY, 0.0
    }

    @Test void getWidth() {
        Bounds b = new Bounds(1,2,3,4)
        assertEquals 2.0, b.width, 0.0
    }

    @Test void getHeight() {
        Bounds b = new Bounds(1,2,3,5)
        assertEquals 3.0, b.height, 0.0
    }

    @Test void getArea() {
        Bounds b = new Bounds(1,2,3,5)
        assertEquals 6.0, b.area, 0.1
    }

    @Test void getAspect() {
        Bounds b = new Bounds(1,2,3,5)
        assertEquals 0.6666, b.aspect, 0.1
    }

    @Test void quadTree() {
        Bounds b = new Bounds(-180, -90, 180, 90, "EPSG:4326")
        List quads = [
            // Level 0
            [-180,-90,180,90],
            // Level 1
            [-180,-90,0,0],
            [-180,0,0,90],
            [0,-90,180,0],
            [0,0,180,90],
            // Level 2
            [-180,-90,-90,-45],
            [-180,-45,-90,0],
            [-180,0,-90,45],
            [-180,45,-90,90],
            [-90,-90,0,-45],
            [-90,-45,0,0],
            [-90,0,0,45],
            [-90,45,0,90],
            [0,-90,90,-45],
            [0,-45,90,0],
            [0,0,90,45],
            [0,45,90,90],
            [90,-90,180,-45],
            [90,-45,180,0],
            [90,0,180,45],
            [90,45,180,90]
        ]

        int c = 0;
        b.quadTree(0,2,{bounds ->
            assertEquals(bounds.minX, quads[c][0], 0.1)
            assertEquals(bounds.minY, quads[c][1], 0.1)
            assertEquals(bounds.maxX, quads[c][2], 0.1)
            assertEquals(bounds.maxY, quads[c][3], 0.1)
            c++
        })
    }

    @Test void expandBy() {
        Bounds b = new Bounds(1,2,3,4)
        Bounds b2 = b.expandBy(10)
        assertEquals b, b2
        assertEquals(-9, b.minX, 0.0)
        assertEquals(-8, b.minY, 0.0)
        assertEquals(13, b.maxX, 0.0)
        assertEquals(14, b.maxY, 0.0)
    }

    @Test void expand() {
        Bounds b1 = new Bounds(1,1,4,4)
        Bounds b2 = new Bounds(8,8,20,20)
        Bounds b3 = b1.expand(b2)
        assertEquals b1, b3
        assertEquals(1, b3.minX, 0.0)
        assertEquals(1, b3.minY, 0.0)
        assertEquals(20, b3.maxX, 0.0)
        assertEquals(20, b3.maxY, 0.0)
    }

    @Test void scale() {
        Bounds b1 = new Bounds(5,5,10,10)
        Bounds b2 = b1.scale(2)
        assertEquals(2.5, b2.minX, 0.0)
        assertEquals(2.5, b2.minY, 0.0)
        assertEquals(12.5, b2.maxX, 0.0)
        assertEquals(12.5, b2.maxY, 0.0)

    }

    @Test void getGeometry() {
        Bounds b = new Bounds(1,2,3,4)
        Geometry g = b.geometry
        assertEquals "POLYGON ((1 2, 1 4, 3 4, 3 2, 1 2))", g.wkt
    }

    @Test void getPolygon() {
        Bounds b = new Bounds(1,2,3,4)
        Geometry g = b.polygon
        assertEquals "POLYGON ((1 2, 1 4, 3 4, 3 2, 1 2))", g.wkt
    }

    @Test void reproject() {
        Bounds b1 = new Bounds(-111, 44.7, -110, 44.9, "EPSG:4326")
        Bounds b2 = b1.reproject("EPSG:26912")
        assertEquals(500000, b2.minX as int)
        assertEquals(4949625, b2.minY as int)
        assertEquals(579225, b2.maxX as int)
        assertEquals(4972328, b2.maxY as int)
    }

    @Test void setProj() {
        // Reproject
        Bounds b1 = new Bounds(-111, 44.7, -110, 44.9, "EPSG:4326")
        b1.setProj("EPSG:26912")
        assertEquals("EPSG:26912", b1.proj.id)
        assertEquals(500000, b1.minX as int)
        assertEquals(4949625, b1.minY as int)
        assertEquals(579225, b1.maxX as int)
        assertEquals(4972328, b1.maxY as int)
        // Set
        Bounds b2 = new Bounds(-111, 44.7, -110, 44.9)
        b2.setProj("EPSG:4326")
        assertEquals("EPSG:4326", b2.proj.id)
        assertEquals(-111, b2.minX, 0.1)
        assertEquals(44.7, b2.minY, 0.1)
        assertEquals(-110, b2.maxX, 0.1)
        assertEquals(44.9, b2.maxY, 0.1)
        // Do nothing
        Bounds b3 = new Bounds(-111, 44.7, -110, 44.9, "EPSG:4326")
        b3.setProj("EPSG:4326")
        assertEquals("EPSG:4326", b3.proj.id)
        assertEquals(-111, b2.minX, 0.1)
        assertEquals(44.7, b2.minY, 0.1)
        assertEquals(-110, b2.maxX, 0.1)
        assertEquals(44.9, b2.maxY, 0.1)
    }

    @Test void string() {
        ReferencedEnvelope e = new ReferencedEnvelope(1,3,2,4,null)
        Bounds b1 = new Bounds(e)
        assertEquals "(1.0,2.0,3.0,4.0)", b1.toString()
		
        Bounds b2 = new Bounds(1,2,3,4, new Projection("EPSG:2927"))
        assertEquals "(1.0,2.0,3.0,4.0,EPSG:2927)", b2.toString()
		
        Bounds b3 = new Bounds(1,2,3,4)
        assertEquals "(1.0,2.0,3.0,4.0)", b3.toString()
    }

    @Test void getAt() {
        Bounds b = new Bounds(1,2,3,4)
        assertEquals(1, b[0], 0)
        assertEquals(2, b[1], 0)
        assertEquals(3, b[2], 0)
        assertEquals(4, b[3], 0)
        assertNull(b[4])
        def (w,s,e,n) = b
        assertEquals(1, w, 0)
        assertEquals(2, s, 0)
        assertEquals(3, e, 0)
        assertEquals(4, n, 0)
    }

    @Test void tile() {
        def b = new Bounds(0,0,100,100)
        def bounds = b.tile(0.50)
        assertEquals 4, bounds.size()
        assertEquals new Bounds(0.0,0.0,50.0,50.0).geometry.wkt, bounds[0].geometry.wkt
        assertEquals new Bounds(50.0,0.0,100.0,50.0).geometry.wkt, bounds[1].geometry.wkt
        assertEquals new Bounds(0.0,50.0,50.0,100.0).geometry.wkt, bounds[2].geometry.wkt
        assertEquals new Bounds(50.0,50.0,100.0,100.0).geometry.wkt, bounds[3].geometry.wkt
    }

    @Test void isEmpty() {
        def b1 = new Bounds(-10, -20, 10, -10)
        def b2 = new Bounds(-10, 0, 10, 20)
        assertTrue b1.intersection(b2).empty
        assertTrue new Bounds(0,10,10,10).empty
        assertFalse new Bounds(0,10,10,20).empty
    }

    @Test void equals() {
        def b1 = new Bounds(-10, -20, 10, -10)
        def b2 = new Bounds(-10, 0, 10, 20)
        def b3 = new Bounds(-10, -20, 10, -10)
        assertFalse b1.equals(b2)
        assertFalse b2.equals(b3)
        assertTrue b1.equals(b3)
    }

    @Test void contains() {
        def b1 = new Bounds(0,0,10,10)
        def b2 = new Bounds(3,3,6,6)
        def b3 = new Bounds(5,5,15,15)
        assertTrue b1.contains(b2)
        assertFalse b2.contains(b1)
        assertFalse b2.contains(b3)
        assertFalse b1.contains(b3)
    }

    @Test void intersects() {
        def b1 = new Bounds(0,0,10,10)
        def b2 = new Bounds(3,3,6,6)
        def b3 = new Bounds(5,5,15,15)
        def b4 = new Bounds(20,25,25,30)
        assertTrue b1.intersects(b2)
        assertTrue b2.intersects(b1)
        assertTrue b2.intersects(b3)
        assertTrue b1.intersects(b3)
        assertFalse b1.intersects(b4)
    }

    @Test void intersection() {
        def b1 = new Bounds(0,0,10,10,"EPSG:4326")
        def b2 = new Bounds(3,3,6,6,"EPSG:4326")
        def b3 = new Bounds(20,25,25,30,"EPSG:4326")
        assertEquals new Bounds(3,3,6,6,"EPSG:4326"), b1.intersection(b2)
        assertTrue b1.intersection(b3).empty
    }

    @Test void ensureWidthAndHeight() {

        // Horizontal Line
        def b1 = new LineString([0,0], [0,10]).bounds
        assertEquals new Bounds(0,0,0,10), b1
        assertEquals new Bounds(-5,0,5,10), b1.ensureWidthAndHeight()

        // Vertical Line
        def b2 = new LineString([0,0],[10,0]).bounds
        assertEquals new Bounds(0.0,0.0,10.0,0.0), b2
        assertEquals new Bounds(0.0,-5.0,10.0,5.0), b2.ensureWidthAndHeight()

        // Point
        def b3 = new Point(10,20).bounds
        assertEquals new Bounds(10.0,20.0,10.0,20.0), b3
        assertEquals new Bounds(9.9,19.9,10.1,20.1), b3.ensureWidthAndHeight()
    }

    @Test void createTriangles() {
        def b = new Bounds(0,0,10,10)
        def g = b.createTriangles()
        assertEquals "MULTIPOLYGON (" +
                "((0 5, 0 10, 5 10, 0 5)), " +
                "((0 5, 5 10, 5 5, 0 5)), " +
                "((5 5, 5 10, 10 5, 5 5)), " +
                "((5 10, 10 10, 10 5, 5 10)), " +
                "((0 0, 0 5, 5 0, 0 0)), " +
                "((0 5, 5 0, 5 5, 0 5)), " +
                "((5 0, 5 5, 10 5, 5 0)), " +
                "((5 0, 10 0, 10 5, 5 0)))", g.wkt
    }

    @Test void createRectangle() {
        def b = new Bounds(0,0,10,10)
        def g = b.createRectangle()
        assertEquals "POLYGON ((0 0, 2 0, 4 0, 6 0, 8 0, 10 0, 10 2, 10 4, 10 6, " +
            "10 8, 10 10, 8 10, 6 10, 4 10, 2 10, 0 10, 0 8, 0 6, 0 4, 0 2, 0 0))", g.wkt
    }

    @Test void createEllipse() {
        def b = new Bounds(0,0,10,10)
        def g = b.createEllipse()
        assertEquals("Polygon", g.geometryType)
        assertTrue(g.contains(new Point(5,5)))
    }

    @Test void createSquircle() {
        def b = new Bounds(0,0,10,10)
        def g = b.createSquircle()
        assertEquals "POLYGON ((5 10, 7.102241038134286 9.960471208089702, 9.204482076268572 9.204482076268572, " +
            "9.960471208089702 7.102241038134286, 10 5, 9.960471208089702 2.897758961865714, 9.204482076268572 " +
            "0.7955179237314267, 7.102241038134286 0.039528791910298, 5 0, 2.897758961865714 0.039528791910298, " +
            "0.7955179237314267 0.7955179237314276, 0.039528791910298 2.897758961865714, 0 5, 0.039528791910298 " +
            "7.102241038134286, 0.7955179237314276 9.204482076268572, 2.897758961865714 9.960471208089702, 5 10))",
            g.wkt
    }

    @Test void createSuperCircle() {
        def b = new Bounds(0,0,10,10)
        def g = b.createSuperCircle(3)
        assertEquals "POLYGON ((5 10, 6.984251314960249 9.893584551461078, 8.968502629920499 8.968502629920499, " +
            "9.893584551461078 6.984251314960249, 10 5, 9.893584551461078 3.0157486850397506, 8.968502629920499 " +
            "1.0314973700795012, 6.984251314960249 0.1064154485389208, 5 0, 3.0157486850397506 0.1064154485389208, " +
            "1.0314973700795012 1.0314973700795016, 0.1064154485389208 3.0157486850397506, 0 5, 0.1064154485389208 " +
            "6.984251314960249, 1.0314973700795016 8.968502629920499, 3.0157486850397506 9.893584551461078, 5 10))",
            g.wkt
    }

    @Test void createArc() {
        def b = new Bounds(0,0,10,10)
        def g = b.createArc(Math.toRadians(45), Math.toRadians(90))
        assertEquals "LINESTRING (8.535533905932738 8.535533905932738, 8.231496189304703 8.815420340999033, " +
            "7.905384077009692 9.069243586350975, 7.559425245448005 9.295269771849426, 7.195982944236851 " +
            "9.491954909459894, 6.817539852819149 9.657955440256394, 6.426681121245526 9.792137412291265, " +
            "6.026076710978172 9.893584226636772, 5.6184631563467375 9.961602898685225, 5.206624871244067 " +
            "9.995728791936505, 4.793375128755934 9.995728791936505, 4.3815368436532625 9.961602898685225, " +
            "3.9739232890218292 9.893584226636772, 3.573318878754474 9.792137412291265, 3.1824601471808514 " +
            "9.657955440256394, 2.8040170557631487 9.491954909459896, 2.4405747545519945 9.295269771849426, " +
            "2.0946159229903087 9.069243586350975, 1.7685038106952966 8.815420340999033, 1.4644660940672627 " +
            "8.535533905932738)",
            g.wkt
    }

    @Test void createArcPolygon() {
        def b = new Bounds(0,0,10,10)
        def g = b.createArcPolygon(Math.toRadians(45), Math.toRadians(90))
        assertEquals "POLYGON ((5 5, 8.535533905932738 8.535533905932738, 8.231496189304703 8.815420340999033, " +
            "7.905384077009692 9.069243586350975, 7.559425245448005 9.295269771849426, 7.195982944236851 " +
            "9.491954909459894, 6.817539852819149 9.657955440256394, 6.426681121245526 9.792137412291265, " +
            "6.026076710978172 9.893584226636772, 5.6184631563467375 9.961602898685225, 5.206624871244067 " +
            "9.995728791936505, 4.793375128755934 9.995728791936505, 4.3815368436532625 9.961602898685225, " +
            "3.9739232890218292 9.893584226636772, 3.573318878754474 9.792137412291265, 3.1824601471808514 " +
            "9.657955440256394, 2.8040170557631487 9.491954909459896, 2.4405747545519945 9.295269771849426, " +
            "2.0946159229903087 9.069243586350975, 1.7685038106952966 8.815420340999033, 1.4644660940672627 " +
            "8.535533905932738, 5 5))",
            g.wkt
    }

    @Test void createSineStar() {
        def b = new Bounds(0,0,10,10)
        def g = b.createSineStar(5, 2.3)
        assertEquals("Polygon", g.geometryType)
        assertFalse(g.isEmpty())
    }

    @Test void createHexagon() {
        def b = new Bounds(0,0,10,10)
        def g = b.createHexagon()
        assertEquals "POLYGON ((2.5 0, 7.5 0, 10 5, 7.5 10, 2.5 10, 0 5, 2.5 0))", g.wkt
        g = b.createHexagon(true)
        assertEquals "POLYGON ((5 0, 10 2.5, 10 7.5, 5 10, 0 7.5, 0 2.5, 5 0))", g.wkt
    }

    @Test void boundsAsGeometry() {
        def b = new Bounds(0,0,10,10)
        def g = b as Geometry
        assertEquals "POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))", g.wkt
        assertEquals "(0.0,0.0,10.0,10.0)", b as String
    }

    @Test void generateGridRowsAndColumns() {
        def b = new Bounds(0,0,100,100)
        List geoms = []
        b.generateGrid(20, 20, "polygon", {cell, c, r ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 5, cell.numPoints
            geoms.add(cell)
        })
        assertEquals 400, geoms.size()
    }

    @Test void generateGridCellWidthAndHeight() {
        def b = new Bounds(0,0,100,100)
        List geoms = []
        b.generateGrid(20.0, 20.0, "polygon", {cell, c, r ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 5, cell.numPoints
            geoms.add(cell)
        })
        assertEquals 25, geoms.size()
    }

    @Test void getGridRowsAndColumnsWithTriangles() {
        def b = new Bounds(0,0,100,100)
        def g = b.getGrid(20,20,"triangle")
        g.geometries.each {cell ->
            assertEquals "MultiPolygon", cell.geometryType
            assertEquals 8, cell.numGeometries
        }
        assertEquals 400, g.numGeometries
    }

    @Test void getGridRowsAndColumns() {
        def b = new Bounds(0,0,100,100)
        def g = b.getGrid(20,20)
        g.geometries.each {cell ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 5, cell.numPoints
        }
        assertEquals 400, g.numGeometries

        b = new Bounds(80, 87, 120, 110)
        g = b.getGrid(2 as int,2 as int)
    }

    @Test void getGridCellWidthAndHeight() {
        def b = new Bounds(0,0,100,100)
        def g = b.getGrid(20.0,20.0)
        g.geometries.each {cell ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 5, cell.numPoints
        }
        assertEquals 25, g.numGeometries

        g = b.getGrid(20.0,20.0, "point")
        g.geometries.each {cell ->
            assertEquals "Point", cell.geometryType
            assertEquals 1, cell.numPoints
        }
        assertEquals 25, g.numGeometries

        g = b.getGrid(20.0,20.0, "circle")
        g.geometries.each {cell ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 101, cell.numPoints
        }
        assertEquals 25, g.numGeometries

        g = b.getGrid(20.0,20.0, "hexagon")
        g.geometries.each {cell ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 7, cell.numPoints
        }
        assertEquals 25, g.numGeometries

        g = b.getGrid(20.0,20.0, "hexagon-inv")
        g.geometries.each {cell ->
            assertEquals "Polygon", cell.geometryType
            assertEquals 7, cell.numPoints
        }
        assertEquals 25, g.numGeometries
    }

    @Test void containsPoint() {
        Bounds b = new Bounds(10,10,20,20)
        assertTrue b.contains(new Point(10,10))
        assertTrue b.contains(new Point(11,11))
        assertTrue b.contains(new Point(20,20))
        assertFalse b.contains(new Point(21,21))
        assertFalse b.contains(new Point(1,1))
    }

    @Test void fixAspectRatio() {
        Bounds b1 = new Bounds(10,10,20,20)
        Bounds b2 = b1.fixAspectRatio(100,300)
        assertEquals new Bounds(10.0,0.0,20.0,30.0), b2
    }

    @Test void fromString() {
        Bounds b1 = Bounds.fromString("0,0,10,10")
        assertEquals(b1, new Bounds(0,0,10,10))
        Bounds b2 = Bounds.fromString("0 0 10 10")
        assertEquals(b2, new Bounds(0,0,10,10))
        Bounds b3 = Bounds.fromString("0,0,10,10,EPSG:4326")
        assertEquals(b3, new Bounds(0,0,10,10,"EPSG:4326"))
        Bounds b4 = Bounds.fromString("0 0 10 10 EPSG:4326")
        assertEquals(b4, new Bounds(0,0,10,10,"EPSG:4326"))
        assertNull(Bounds.fromString(""))
        assertNull(Bounds.fromString("   "))
        assertNull(Bounds.fromString(null))
        assertNull(Bounds.fromString("1,2,3"))
        assertNull(Bounds.fromString("1 2 3"))
    }

    @Test void getCorners() {
        Bounds b = new Bounds(1,2,7,8)
        List corners = b.corners
        assertEquals 1, corners[0].x, 0.1
        assertEquals 2, corners[0].y, 0.1

        assertEquals 1, corners[1].x, 0.1
        assertEquals 8, corners[1].y, 0.1

        assertEquals 7, corners[2].x, 0.1
        assertEquals 8, corners[2].y, 0.1

        assertEquals 7, corners[3].x, 0.1
        assertEquals 2, corners[3].y, 0.1
    }
}
