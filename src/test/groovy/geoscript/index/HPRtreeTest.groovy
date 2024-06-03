package geoscript.index

import geoscript.geom.Bounds
import geoscript.geom.Point
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * The HPRtreeTest
 */
class HPRtreeTest {

    @Test void index() {

        def spatialIndex = new HPRtree()
        spatialIndex.insert(new Bounds(0,0,10,10), new Point(5,5))
        spatialIndex.insert(new Bounds(2,2,6,6), new Point(4,4))
        spatialIndex.insert(new Bounds(20,20,60,60), new Point(30,30))
        spatialIndex.insert(new Bounds(22,22,44,44), new Point(32,32))

        assertEquals 4, spatialIndex.size

        def results = spatialIndex.query(new Bounds(1,1,5,5))
        assertEquals 2, results.size()
        assertTrue(results[0].toString() == 'POINT (4 4)' || results[0].toString() == 'POINT (5 5)')
        assertTrue(results[1].toString() == 'POINT (4 4)' || results[1].toString() == 'POINT (5 5)')

        results = spatialIndex.query(new Bounds(25,25,50,55))
        assertEquals 2, results.size()
        assertTrue(results[0].toString() == 'POINT (30 30)' || results[0].toString() == 'POINT (32 32)')
        assertTrue(results[1].toString() == 'POINT (30 30)' || results[1].toString() == 'POINT (32 32)')
    }
}

