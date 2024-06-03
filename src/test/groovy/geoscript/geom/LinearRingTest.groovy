package geoscript.geom

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*

/**
 * The LinearRing unit test
 */
class LinearRingTest {

    @Test void constructors() {
        
        def l1 = new LinearRing([[111.0, -47],[123.0, -48],[110.0, -47], [111.0, -47]])
        assertEquals "LINEARRING (111 -47, 123 -48, 110 -47, 111 -47)", l1.wkt

        def l2 = new LinearRing([new Point(111.0, -47), new Point(123.0, -48), new Point(110.0, -47), new Point(111.0, -47)])
        assertEquals "LINEARRING (111 -47, 123 -48, 110 -47, 111 -47)", l2.wkt

        def l3 = new LinearRing([111.0, -47],[123.0, -48],[110.0, -47], [111.0, -47])
        assertEquals "LINEARRING (111 -47, 123 -48, 110 -47, 111 -47)", l3.wkt

        def l4 = new LinearRing(new Point(111.0, -47), new Point(123.0, -48), new Point(110.0, -47), new Point(111.0, -47))
        assertEquals "LINEARRING (111 -47, 123 -48, 110 -47, 111 -47)", l4.wkt
    }

    @Test void isClosed() {
        def ring = new LinearRing([[111.0, -47],[123.0, -48],[110.0, -47], [111.0, -47]])
        assertTrue(ring.isClosed())
    }

    @Test void isRing() {
        def ring = new LinearRing([[111.0, -47],[123.0, -48],[110.0, -47], [111.0, -47]])
        assertTrue(ring.isRing())
    }
}