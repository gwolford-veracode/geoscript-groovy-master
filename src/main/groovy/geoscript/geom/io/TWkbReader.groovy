package geoscript.geom.io

import geoscript.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.twkb.TWKBReader

/**
 * Read a {@link geoscript.geom.Geometry Geometry} from a TWKB hex String or byte array.
 * <p><blockquote><pre>
 * TWkbReader reader = new TWkbReader()
 * {@link geoscript.geom.Point Point} pt = reader.read("01000204")
 *
 * POINT (1 2)
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class TWkbReader implements Reader {

    /**
     * The GeoTools TWKBReader
     */
    private final static TWKBReader reader = new TWKBReader(new GeometryFactory())

    /**
     * Read a Geometry from a String
     * @param str The String
     * @return A Geometry
     */
    Geometry read(String str) {
        read(WKBReader.hexToBytes(str))
    }

    /**
     * Read a Geometry from a byte array
     * @param bytes The byte array
     * @return A Geometry
     */
    Geometry read(byte[] bytes) {
        Geometry.wrap(reader.read(bytes))
    }

}
