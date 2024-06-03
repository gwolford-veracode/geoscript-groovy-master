package geoscript.style.io

import geoscript.style.Style
import org.geotools.api.style.Style as GtStyle
import org.geotools.api.style.StyleFactory
import org.geotools.styling.StyleFactoryImpl
import org.geotools.xml.styling.SLDParser

/**
 * Read a Geoscript Style from a SLD File, InputStream or String
 * <p><blockquote><pre>
 * Reader reader = new SLDReader()
 * Style style = reader.read("states.sld")
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class SLDReader implements Reader {

    /**
     * The GeoTools StyleFactory
     */
    private static StyleFactory styleFactory = new StyleFactoryImpl()

    /**
     * Read a GeoScript Style from a SLD InputStream
     * @param input A SLD InputStream
     * @return A GeoScript Style
     */
    Style read(InputStream input) {
        SLDParser parser = new SLDParser(styleFactory, input)
        GtStyle[] styles = parser.readXML()
        new SLDStyle(styles[0])
    }

    /**
     * Read a GeoScript Style from a SLD File
     * @param file A SLD File
     * @return A GeoScript Style
     */
    Style read(File file) {
        read(new FileInputStream(file))
    }

    /**
     * Read a GeoScript Style from a SLD String
     * @param str A SLD String
     * @return A GeoScript Style
     */
    Style read(String str) {
        if (str == null || str.isEmpty()) {
            null
        } else {
            read(new ByteArrayInputStream(str.getBytes("UTF-8")))
        }
    }

    /**
     * A simple GeoScript Style that wraps a GeoTools Style
     */
    private static class SLDStyle implements Style {
        private final GtStyle style
        SLDStyle(GtStyle style) {
            this.style = style
        }
        GtStyle getGtStyle() {
            style
        }
    }
}
