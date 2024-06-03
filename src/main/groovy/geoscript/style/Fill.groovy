package geoscript.style

import geoscript.filter.Expression
import org.geotools.api.style.Fill as GtFill
import org.geotools.api.style.Rule
import org.geotools.api.style.PolygonSymbolizer
import org.geotools.api.style.Symbolizer as GtSymbolizer
import geoscript.filter.Color

/**
 * A Symbolizer for area/polygonal geometries.  It consists of a color and an opacity.
 * You can create a Fill from a color and opacity:
 * <p><blockquote><pre>
 * def fill = new Fill('#ff0000', 0.5)
 * </pre></blockquote></p>
 * Or from named parameters:
 * <p><blockquote><pre>
 * def fill = new Fill(color: '#ff0000', opacity: 0.25)
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class Fill extends Symbolizer {

    /**
     * The Color (#ff0000, red, [0,0.,255])
     */
    Expression color

    /**
     * The opacity (1.0 = opaque to 0.0 = transparent)
     */
    Expression opacity

    /**
     * The Icon
     */
    Icon icon

    /**
     * The Hatch
     */
    Hatch hatch

    /**
     * Create a new Fill.
     * <p><blockquote><pre>
     * def f = new Fill(color: '#ff0000', opacity: 0.25)
     * </pre></blockquote></p>
     * @param map A Map of named parameters.
     */
    Fill(Map map) {
        super()
        map.each{k,v->
            if(this.hasProperty(k as String)){
                this."$k" = v
            }
        }
    }

    /**
     * Create a new Fill.
     * <p><blockquote><pre>
     * def f = new Fill('#ff0000', 0.5)
     * def f = new Fill('red', 0.5)
     * def f = new Fill([255,0,0], 0.5)
     * </pre></blockquote></p>
     * @param color The Color
     * @param opacity The opacity (1 opaque to 0 transparent)
     */
    Fill(def color, def opacity = 1.0) {
        super()
        this.color = color instanceof Expression ? color : new Color(color)
        this.opacity = new Expression(opacity)
        this.icon = null
        this.hatch = null
    }

    /**
     * Compose this Fill as an Icon.
     * <p><blockquote><pre>
     * def f = new Fill().icon('icon.png','image/png')
     * </pre></blockquote></p>
     * @param url The URL or File of the image
     * @param format The mime type of the image.
     * @return This Fill
     */
    Fill icon(def url, String format) {
        this.icon = new Icon(url, format)
        this
    }

    /**
     * Compose this Fill with a Hatch pattern.
     * <p><blockquote><pre>
     * def f = new Fill().hatch('slash')
     * </pre></blockquote></p>
     * @param name The name of the hatch pattern
     * @param stroke The Stroke
     * @param size The size
     * @return This Fill
     */
    Fill hatch(String name, Stroke stroke = new Stroke(), def size = 8) {
        this.hatch = new Hatch(name, stroke, size)
        this
    }

    /**
     * Compose this Fill with a Hatch pattern that has a Fill and Stroke.
     * <p><blockquote><pre>
     * def f = new Fill().hatch('slash', new Fill("white"), new Stroke("blue",0.1), 8)
     * </pre></blockquote></p>
     * @param name The name of the hatch pattern
     * @param fill The Fill
     * @param stroke The Stroke
     * @param size The size
     * @return This Fill
     */
    Fill hatch(String name, Fill fill, Stroke stroke, def size = 8) {
        this.hatch = new Hatch(name, fill, stroke, size)
        this
    }

    /**
     * Compose this Fill with a Hatch pattern that has a Fill but no Stroke
     * <p><blockquote><pre>
     * def f = new Fill().hatch('slash', new Fill("wheat"), 8)
     * </pre></blockquote></p>
     * @param name The name of the hatch pattern
     * @param fill The Fill
     * @param size The size
     * @return This Fill
     */
    Fill hatch(String name, Fill fill, def size = 8) {
        this.hatch = new Hatch(name, fill, size)
        this
    }

    /**
     * Set the color
     * @param color The color (#ffffff, red)
     */
    void setColor(def color) {
        this.color = color instanceof Expression ? color : new Color(color)
    }

    /**
     * Set the opacity
     * @param opacity The opacity
     */
    void setOpacity(def opacity) {
        this.opacity = new Expression(opacity)
    }

    /**
     * Add randomized fill. See http://osgeo-org.1560.x6.nabble.com/Randomized-fill-landed-on-trunk-td5081889.html
     * for more details.
     * @param params Named parameters may include:
     * <ul>
     *     <li>random: none, free, grid</li>
     *     <li>seed: The seed generator value (defaults to 0)</li>
     *     <li>symbolCount: The number of symbols (defaults to 16)</li>
     *     <li>tileSize: The size of the square area (defaults to 256)</li>
     *     <li>rotation: Whether to rotate symbols (none or free) </li>
     * </ul>
     * @return This Fill
     */
    Fill random(Map params = [:]) {
        options["random"] = params.get("random", "free") as String
        options["random-seed"] = params.get("seed", 0)  as String
        options["random-symbol-count"] = params.get("symbolCount", 16) as String
        options["random-tile-size"] = params.get("tileSize", 256) as String
        options["random-rotation"] = params.get("rotation", "none") as String
        this
    }

    /**
     * Create the GeoTools Fill object
     * @return A GeoTools Fill
     */
    protected GtFill createFill() {
        GtFill fill = styleBuilder.createFill()
        if (color != null && color.value != null) {
            fill.color = color.expr
        } else {
            fill.color = null
        }
        if (hatch) {
            fill.graphicFill = hatch.createHatch()
        }
        fill.opacity = opacity.expr
        fill
    }

    /**
     * Prepare the GeoTools Rule by applying this Symbolizer
     * @param rule The GeoTools Rule
     */
    @Override
    protected void prepare(Rule rule) {
        super.prepare(rule)
        getGeoToolsSymbolizers(rule, PolygonSymbolizer).each{s ->
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
        sym.fill = createFill()
        if (icon) {
            icon.apply(sym)
        }
    }

    /**
     * The string representation
     * @return The string representation
     */
    String toString() {
        buildString("Fill", ['color': color, 'opacity': opacity])
    }
}

