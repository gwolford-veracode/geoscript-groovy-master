package geoscript.layer

import geoscript.proj.Projection
import org.apache.commons.io.input.ReaderInputStream
import org.geotools.coverage.grid.io.AbstractGridFormat
import org.geotools.util.factory.GeoTools
import org.geotools.util.factory.Hints
import org.geotools.gce.arcgrid.ArcGridFormat

import java.nio.charset.Charset

/**
 * A Format that can read and write ArcGrids
 * @author Jared Erickson
 */
class ArcGrid extends Format {

    /**
     * Create a new ArcGrid
     * @param stream The file
     */
    ArcGrid(def stream) {
        super(new org.geotools.gce.arcgrid.ArcGridFormat(), stream)
    }

    /**
     * Read a Raster from the stream (usually a File)
     * @param options Optional named parameters that are turned into an array
     * of GeoTools GeneralParameterValues
     * @param stream The stream (usually a File)
     * @return A Raster
     */
    @Override
    Raster read(Map options = [:]) {
        if (stream instanceof String) {
            String charset = "UTF-8"
            if (options.containsKey("charset")) {
                charset = options.get("charset")
                options.remove(charset)
            }
            stream = new ReaderInputStream(new StringReader(stream), Charset.forName(charset))
        }
        super.read(options, GeoTools.getDefaultHints())
    }

    /**
     * Read a Raster from the stream (usually a File)
     * @param options Optional named parameters that are turned into an array
     * of GeoTools GeneralParameterValues
     * @param proj The Projection
     * @return A Raster
     */
    @Override
    Raster read(Map options = [:], Projection proj) {
        if (stream instanceof String) {
            String charset = "UTF-8"
            if (options.containsKey("charset")) {
                charset = options.get("charset")
                options.remove(charset)
            }
            stream = new ReaderInputStream(new StringReader(stream), Charset.forName(charset))
        }
        super.read(options, proj)
    }

    /**
     * Read a Raster from the stream (usually a File)
     * @param options Optional named parameters that are turned into an array
     * of GeoTools GeneralParameterValues
     * @param hints GeoTools Hints
     * @return A Raster
     */
    @Override
    Raster read(Map options = [:], Hints hints) {
        if (stream instanceof String) {
            String charset = "UTF-8"
            if (options.containsKey("charset")) {
                charset = options.get("charset")
                options.remove(charset)
            }
            stream = new ReaderInputStream(new StringReader(stream), Charset.forName(charset))
        }
        super.read(options, hints)
    }

    /**
     * Write an ArcGrid Raster to a String
     * @param raster The Raster
     * @param format The string format can either be "arc" which is the default, or grass
     * @return A String
     */
    String writeToString(Raster raster, String format = "arc") {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArcGrid arcGrid = new ArcGrid(out)
        arcGrid.write(raster, GRASS: format.equalsIgnoreCase("grass") ? true : false)
        out.close()
        out.toString()
    }

    /**
     * The ArcGrid FormatFactory
     */
    static class Factory extends FormatFactory<ArcGrid> {

        @Override
        protected List<String> getFileExtensions() {
            ["asc"]
        }

        @Override
        protected Format createFromFormat(AbstractGridFormat gridFormat, Object source) {
            if (gridFormat instanceof ArcGridFormat) {
                new ArcGrid(source)
            }
        }

        @Override
        protected Format createFromFile(File file) {
            new ArcGrid(file)
        }

    }
}
