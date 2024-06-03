package geoscript.style

import geoscript.filter.Function
import geoscript.layer.Layer

/**
 * The Symbolizers class is a collection of static methods that can be used
 * to create Symbolizers.
 * <p><blockquote><pre>
 * import static geoscript.style.Symbolizers.*
 *
 * Fill fill = fill("#003300", 0.65)
 * Stroke stroke = stroke("wheat", 1.2, [5,2], "square", "bevel",0.45)
 * </pre></blockquote></p>
 */
class Symbolizers {

    /**
     * Create a new Fill.
     * <p><blockquote><pre>
     * def f = fill('#ff0000', 0.5)
     * def f = fill('red', 0.5)
     * def f = fill([255,0,0], 0.5)
     * </pre></blockquote></p>
     * @param color The Color
     * @param opacity The opacity (1 opaque to 0 transparent)
     * @return A Fill
     */
    static Fill fill(def color, def opacity = 1.0) {
        new Fill(color, opacity)
    }

    /**
     * Create a new Fill.
     * <p><blockquote><pre>
     * def f = fill(color: '#ff0000', opacity: 0.25)
     * </pre></blockquote></p>
     * @param properties A Map of named parameters.
     * @return A Fill
     */
    static Fill fill(Map properties) {
        new Fill(properties)
    }

    /**
     * Create a new Stroke.
     * <p><blockquote><pre>
     * def stroke = stroke("#ff0000", 0.25, [5,2], "round", "bevel")
     * </pre></blockquote></p>
     * @param color The color
     * @param width The width
     * @param dash The dash pattern
     * @param cap The line cap (round, butt, square)
     * @param join The line join (mitre, round, bevel)
     * @return A Stroke
     */
    static Stroke stroke(def color = "#000000", def width = 1, List dash = null, def cap = null, def join = null, def opacity = 1.0) {
        new Stroke(color, width, dash, cap, join, opacity)
    }

    /**
     * Create a new Stroke with named parameters.
     * <p><blockquote><pre>
     * def stroke = stroke(width: 1.2, dash: [5,2], color: "#ff00ff", opacity: 0.75)
     * </pre></blockquote></p>
     * @param properties A Map of named parameters.
     * @return A Stroke
     */
    static Stroke stroke(Map properties) {
        new Stroke(properties)
    }

    /**
     * Create a new Font.
     * <p><blockquote><pre>
     * def f = font("normal", "bold", 12, "Arial")
     * </pre></blockquote></p>
     * @param style The Font style (normal, italic, oblique)
     * @param weight The Font weight (normal, bold)
     * @param size The Font size (8,10,12,24,ect...)
     * @param family The Font family (serif, Arial, Verdana)
     * @return A Font
     */
    static Font font(def style = "normal", def weight = "normal", def size = 10, def family = "serif") {
        new Font(style, weight, size, family)
    }

    /**
     * Create a new Font with named parameters.
     * <p><blockquote><pre>
     * def f = font(weight: "bold", size: 32)
     * </pre></blockquote></p>
     * @param properties A Map of named parameters.
     * @return A Font
     */
    static Font font(Map properties) {
        new Font(properties)
    }

    /**
     * Create a new Halo with a Fill and radius.
     * <p><blockquote><pre>
     * def h = halo(new Fill("navy"), 2.5)
     * </pre></blockquote></p>
     * @param fill The Fill
     * @param radius The radius
     * @return A Halo
     */
    static Halo halo(Fill fill, def radius) {
        new Halo(fill, radius)
    }

    /**
     * Create a new Halo with named parameters.
     * <p><blockquote><pre>
     * def h = halo(fill: new Fill("navy"), radius: 2.5)
     * </pre></blockquote></p>
     * @param properties A Map of named parameters.
     * @return A Halo
     */
    static Halo halo(Map properties) {
        new Halo(properties)
    }

    /**
     * Create a new Hatch.
     * <p><blockquote><pre>
     * def hatch = hatch("times", new Stroke("wheat", 1.2, [5,2], "square", "bevel"), 12.2)
     * </pre></blockquote></p>
     * @param name (vertline, horline, slash, backslash, plus, times)
     * @param stroke A Stroke
     * @param size The size
     * @return A Hatch
     */
    static Hatch hatch(def name, Stroke stroke = new Stroke(), def size = 8) {
        new Hatch(name, stroke, size)
    }

    /**
     * Create a new Hatch with named parameters.
     * <p><blockquote><pre>
     * def hatch = hatch(size: 10, stroke: new Stroke("wheat",1.0), name: "slash")
     * </pre></blockquote></p>
     * @param map A Map of named parameters.
     * @return A Hatch
     */
    static Hatch hatch(Map map) {
        new Hatch(map)
    }

    /**
     * Create a new Icon with named parameters.
     * <p><blockquote><pre>
     * def i = icon(format: "image/png", url: "images/star.png")
     * </pre></blockquote></p>
     * @param map A Map of named parameters.
     * @return An Icon
     */
    static Icon icon(Map map) {
        new Icon(map)
    }

    /**
     * Create a new Icon.
     * <p><blockquote><pre>
     * def i = icon("images/star.png", "image/png")
     * </pre></blockquote></p>
     * @param url The file or url of the icon
     * @param format The image format (image/png)
     * @param size The size of the Icon (default to -1 which means auto-size)
     * @return An Icon
     */
    static Icon icon(def url, String format, def size = -1) {
        new Icon(url, format, size)
    }

    /**
     * Create a new Label with a property which is a field or attribute with which
     * to generate labels form.
     * <p><blockquote><pre>
     * def l = label("STATE_ABBR")
     * </pre></blockquote></p>
     * @param property The field or attribute
     * @return A Label
     */
    static Label label(def property) {
        new Label(property)
    }

    /**
     * Create a new Label with named parameters.
     * <p><blockquote><pre>
     * def l = label(property: "name", font: new Font(weight: "bold")))
     * </pre></blockquote></p>
     * @param map A Map of named parameters.
     * @return A Label
     */
    static Label label(Map map) {
        new Label(map)
    }

    /**
     * Create a new Shape.
     * @return A Shape
     */
    static Shape shape() {
        new Shape()
    }

    /**
     * Create a new Shape with named parameters.
     * <p><blockquote><pre>
     * def s = shape(type: "star", size: 4, color: "#ff00ff")
     * </pre></blockquote></p>
     * @param map A Map of named parameters.
     * @return A Shape
     */
    static Shape shape(Map map) {
        new Shape(map)
    }

    /**
     * Create a new Shape.
     * <p><blockquote><pre>
     * def s = shape("#ff0000", 8, "circle", 0.55, 0)
     * </pre></blockquote></p>
     * @param color The color
     * @param size The size
     * @param type The type
     * @param opacity The opacity (0-1)
     * @param angle The angle or rotation (0-360)
     * @return A Shape
     */
    static Shape shape(def color, def size = 6, def type = "circle", def opacity = 1.0, def angle = 0) {
        new Shape(color, size, type, opacity, angle)
    }

    /**
     * Create a new Transform from a Function.
     * <p><blockquote><pre>
     * Transform t = transform(new Function("myCentroid", {g -> g.centroid}))
     * </pre></blockquote></p>
     * @param function The geoscript.filter.Function
     * @return A Transform
     */
    static Transform transform(Function function) {
        new Transform(function)
    }

    /**
     * Create a new Transform from a CQL filter function.
     * <p><blockquote><pre>
     * Transform t = transform("centroid(the_geom)")
     * </pre></blockquote></p>
     * @param cql A CQL string
     * @return A Transform
     */
    static Transform transform(String cql) {
        new Transform(cql)
    }

    /**
     * Create a new UniqueValues Composite.
     * <p><blockquote><pre>
     * UniqueValues values = uniqueValues(shp, "STATE_NAME")
     * </pre></blockquote></p>
     * @param layer The Layer
     * @param field The Field or the Field's name
     * @param colors A Closure (which takes index based on 0 and a value), a Palette name, or a List of Colors
     */
    static UniqueValues uniqueValues(Layer layer, def field, def colors = {index, value -> Color.getRandomPastel()}) {
        new UniqueValues(layer, field, colors)
    }

    /**
     * Create a new Gradient by interpolating between a List of values and styles.
     * <p><blockquote><pre>
     * Gradient g = gradient("PERSONS / LAND_KM",[0,200],[new Fill("#000066"), new Fill("red")],10,"exponential")
     * </pre></blockquote></p>
     * @param expression An Expression or a String expression.
     * @param values A List of values
     * @param styles A List of Styles
     * @param classes The number of classes
     * @param method The interpolation method (linear, exponential, logarithmic)
     * @param inclusive Whether to include the last value of not
     */
    static Gradient gradient(def expression, List values, List styles, int classes = 5, String method="linear", boolean inclusive = true) {
        new Gradient(expression, values, styles, classes, method, inclusive)
    }

    /**
     * Create a new Gradient where the interpolation is based on a classification method based on values from the Layer's
     * Field.
     * <p><blockquote><pre>
     * Gradient g = gradient(shapefile, "WORKERS", "Quantile", 5, "Greens")
     * </pre></blockquote></p>
     * @param layer The Layer
     * @param field The Field or Field's name
     * @param method The classification method (Quantile or EqualInterval)
     * @param number The number of categories
     * @param colors A Color Brewer palette name, or a List of Colors
     * @param elseMode The else mode (ignore, min, max)
     */
    static Gradient gradient(Layer layer, def field, String method, int number, def colors, String elseMode = "ignore") {
        new Gradient(layer, field, method, number, colors, elseMode)
    }
}