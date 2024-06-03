package geoscript.style

import org.geotools.api.style.Rule
import org.geotools.api.style.PointSymbolizer
import org.geotools.api.style.PolygonSymbolizer
import org.geotools.api.style.Symbolizer as GtSymbolizer
import geoscript.filter.Expression

/**
 * A Symbolizer for an external image or glyph.
 * <p>You can create an Icon from a File/URL/URI and a mime type:</p>
 * <p><blockquote><pre>
 * def icon = new Icon("images/star.png", "image/png")
 * </pre></blockquote></p>
 * Or with named parameters:
 * <p><blockquote><pre>
 * def icon = new Icon(format: "image/png", url: "images/star.png")
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class Icon extends Symbolizer {

    /**
     * The location of the source image
     */
    URL url

    /**
     * The mime type of the image (image/png)
     */
    String format

    /**
     * The size of the Icon (default to -1 which means auto-size)
     */
    Expression size = new Expression(-1)

    /**
     * Create a new Icon with named parameters.
     * <p><blockquote><pre>
     * def icon = new Icon(format: "image/png", url: "images/star.png")
     * </pre></blockquote></p>
     * @param map A Map of named parameters.
     */
    Icon(Map map) {
        super()
        map.each{k,v->
            if(this.hasProperty(k as String)){
                this."$k" = k.equals("url") ? toURL(v) : v
            }
        }
        if (format == null) {
            format = guessMimeType(url)
        }
    }


    /**
     * Create a new Icon.
     * <p><blockquote><pre>
     * def icon = new Icon("images/star.png", "image/png")
     * </pre></blockquote></p>
     * @param url The file or url of the icon
     * @param format The image format (image/png)
     * @param size The size of the Icon (default to -1 which means auto-size)
     */
    Icon(def url, String format = null, def size = -1) {
        super()
        this.url = toURL(url)
        setFormat(format ? format : guessMimeType(this.url))
        this.size = new Expression(size)
    }

    /**
     * Try to guess the mime type for the URL
     * @param url The URL
     * @return A mime type or null
     */
    private static String guessMimeType(URL url) {
        String path = url.path
        int i = path.lastIndexOf(".")
        if (i > -1) {
            return "image/${path.substring(i + 1)}"
        } else  {
            return null
        }
    }

    /**
     * Set the size of the icon
     * @param size The size
     */
    void setSize(def size) {
        this.size = new Expression(size)
    }

    /**
     * Set the URL
     * @param url A URL, URI, or File
     */
    void setUrl(def url) {
        this.url = toURL(url)
    }

    /**
     * Set the format
     * @param fmt The format
     */
    void setFormat(String fmt) {
        if (!fmt.startsWith("image/")) {
            fmt = "image/${fmt}"
        }
        this.format = fmt
    }

    /**
     * Convert an Object to a URL.  Handles URLs, URIs, Files.
     * @param o An Object
     * @return A URL
     */
    private URL toURL(def o) {
        if (o instanceof java.net.URL) {
            return o as java.net.URL
        } else if (o instanceof java.net.URI) {
            return o.toURL()
        } else if (o instanceof java.io.File) {
            return o.toURL()
        } else {
            try {
                return new java.net.URL(o)
            }
            catch(java.net.MalformedURLException e) {
                return new File(o).toURL()
            }
        }
    }

    /**
     * Prepare the GeoTools Rule by applying this Symbolizer
     * @param rule The GeoTools Rule
     */
    @Override
    protected void prepare(Rule rule) {
        super.prepare(rule)
        getGeoToolsSymbolizers(rule, PointSymbolizer).each{s ->
            apply(s)
        }
    }

    /**
     * Apply this Symbolizer to the GeoTools Symbolizer
     * @param sym The GeoTools Symbolizer
     */
    @Override
    protected void apply(GtSymbolizer sym) {
        super.apply(sym)
        def externalGraphic = styleFactory.createExternalGraphic(url, format)
        if (sym instanceof PointSymbolizer) {
            if (!sym.graphic) {
                sym.graphic = styleBuilder.createGraphic()
            }
            if (size.value > -1) {
                sym.graphic.size = size.expr
            }
            sym.graphic.graphicalSymbols().add(externalGraphic)
        } else if (sym instanceof PolygonSymbolizer) {
            if (!sym.fill) {
                sym.fill = styleBuilder.createFill()
            }
            if (!sym.fill.graphicFill) {
                sym.fill.graphicFill = styleBuilder.createGraphic()
                sym.fill.graphicFill.graphicalSymbols().clear()
            }
            sym.fill.graphicFill.graphicalSymbols().add(externalGraphic)
        }
    }

    /**
     * The string representation
     * @return The string representation
     */
    String toString() {
        buildString("Icon", ['url': url, 'format': format])
    }
}

