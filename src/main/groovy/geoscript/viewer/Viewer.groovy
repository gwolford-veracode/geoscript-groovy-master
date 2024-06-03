package geoscript.viewer

import org.locationtech.jts.geom.Geometry as JtsGeometry

import org.locationtech.jts.awt.PointShapeFactory
import org.locationtech.jts.awt.PointTransformation
import org.locationtech.jts.awt.ShapeWriter
import org.locationtech.jts.geom.Coordinate
import geoscript.geom.Bounds
import geoscript.geom.Geometry
import geoscript.geom.GeometryCollection

import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.util.List
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import java.awt.*

/**
 * A Viewer can be used to visualize Geometry.
 * <p><blockquote><pre>
 * import geoscript.geom.Point
 * import geoscript.viewer.Viewer
 * Point p = new Point(10,10)
 * Viewer.draw(p.buffer(100))
 * </pre></blockquote></p>
 * Or you can plot a List of Geometries.
 * <p><blockquote><pre>
 * import geoscript.geom.Point
 * import geoscript.viewer.Viewer
 * Point p = new Point(10,10)
 * Viewer.plot([p, p.buffer(50), p.buffer(100)])
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class Viewer {

    /**
     * Draw a Geometry (or List of Geometries) onto a GUI
     * @param options A Map of options or named parameters
     * <ul>
     *  <li>size = The size of the image</li>
     *  <li>bounds = The Bounds</li>
     *  <li>strokeColor = The stroke color</li>
     *  <li>fillColor = The fill color</li>
     *  <li>markerShape = The marker shape (circle, square, cross, ect...)</li>
     *  <li>markerSize = The marker size</li>
     *  <li>opacity = The opacity</li>
     *  <li>strokeWidth = The stroke width</li>
     *  <li>drawCoords = Whether to draw coordinates or not (true | false)</li>
     * </ul>
     * @param geom The Geometry to List of Geometries to render
     */
    static void draw(java.util.Map options = [:], def geom) {
        if (!(geom instanceof java.util.List)) {
            geom = [geom]
        }
        Panel panel = new Panel(options, geom)
        java.util.List size = options.get("size", [500,500])
        double buf = options.get("expandBy", 0)
        Dimension dim = new Dimension((int) (size[0] + 2 * buf), (int) (size[1] + 2 * buf))
        panel.preferredSize = dim
        panel.minimumSize = dim
        JFrame frame = new JFrame("GeoScript Viewer")
        // If we are opening Windows from the GroovyConsole, we can't use EXIT_ON_CLOSE because the GroovyConsole
        // itself will exit
        if (java.awt.Frame.frames.find{it.title.contains("GroovyConsole")}) {
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        } else {
            // The Groovy Shell has a special SecurityManager that doesn't allow EXIT_ON_CLOSE
            try { frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE } catch (SecurityException ex) {frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE}
        }
        frame.layout = new BorderLayout()
        frame.add(panel, BorderLayout.CENTER)
        frame.pack()
        frame.visible = true
    }

    /**
     * Draw Geometry (or List of Geometries) to a BufferedImage
     * @param options A Map of options or named parameters
     * <ul>
     *  <li>size = The size of the image</li>
     *  <li>bounds = The Bounds</li>
     *  <li>strokeColor = The stroke color</li>
     *  <li>fillColor = The fill color</li>
     *  <li>markerShape = The marker shape (circle, square, cross, ect...)</li>
     *  <li>markerSize = The marker size</li>
     *  <li>opacity = The opacity</li>
     *  <li>strokeWidth = The stroke width</li>
     *  <li>drawCoords = Whether to draw coordinates or not (true | false)</li>
     * </ul>
     * @param geom A Geometry or a List of Geometries
     * @return A BufferedImage
     */
    static BufferedImage drawToImage(java.util.Map options = [:], def geom) {
        java.util.List size = options.get("size", [500,500])
        BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB)
        Graphics2D g2d = image.createGraphics()
        Bounds bounds = new GeometryCollection(geom).bounds
        bounds.expandBy(bounds.width * 0.10)
        geom = geom instanceof java.util.List ? geom : [geom]
        draw(options, g2d, geom)
        g2d.dispose()
        image
    }

    /**
     * Draw Geometry (or List of Geometries) to a Base64 Encoded String
     * @param options A Map of options or named parameters
     * <ul>
     *  <li>size = The size of the image</li>
     *  <li>bounds = The Bounds</li>
     *  <li>strokeColor = The stroke color</li>
     *  <li>fillColor = The fill color</li>
     *  <li>markerShape = The marker shape (circle, square, cross, ect...)</li>
     *  <li>markerSize = The marker size</li>
     *  <li>opacity = The opacity</li>
     *  <li>strokeWidth = The stroke width</li>
     *  <li>drawCoords = Whether to draw coordinates or not (true | false)</li>
     *  <li>includePrefix = Whether to include the base 64 prefix</li>
     *  <li>imageType = The image type (PNG or JPEG)</li>
     * </ul>
     * @param geom A Geometry or a List of Geometries
     * @return A Base64 Encoded String
     */
    static String drawToBase64EncodedString(java.util.Map options = [:], def geom) {
        BufferedImage image = drawToImage(options, geom)
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        String imageType = options.get("imageType", "png")
        ImageIO.write(image, imageType, out)
        byte[] bytes = org.apache.commons.codec.binary.Base64.encodeBase64(out.toByteArray())
        String str = new String(bytes, "UTF-8")
        if (options.get("includePrefix", true)) {
            str = "image/${imageType};base64,${str}"
        }
        str
    }

    /**
     * Save a drawing of the Geometry (or List of Geometries) to a File
     * @param options A Map of options or named parameters
     * @param geom A Geometry or a List of Geometries
     * @param file The File
     */
    static void drawToFile(java.util.Map options = [:], def geom, File file) {
        String fileName = file.absolutePath
        String imageFormat = fileName.substring(fileName.lastIndexOf(".")+1)
        FileOutputStream out = new FileOutputStream(file)
        ImageIO.write(drawToImage(options, geom), imageFormat, out)
        out.close()
    }

    /**
     * Draw a List of GeoScript Geometries to a Graphics2D.
     * @param options A Map of options or named parameters
     * <ul>
     *  <li>size = The size of the image</li>
     *  <li>bounds = The Bounds</li>
     *  <li>strokeColor = The stroke color</li>
     *  <li>fillColor = The fill color</li>
     *  <li>markerShape = The marker shape (circle, square, cross, ect...)</li>
     *  <li>markerSize = The marker size</li>
     *  <li>opacity = The opacity</li>
     *  <li>strokeWidth = The stroke width</li>
     *  <li>drawCoords = Whether to draw coordinates or not (true | false)</li>
     * </ul>
     * @param g2d The Graphics2D
     * @param geometries A List of GeoScript Geometries
     */
    static void draw(java.util.Map options = [:], Graphics2D g2d, List geometries) {

        java.util.List size = options.get("size", [500,500])
        Bounds bounds = (options.get("bounds",new GeometryCollection(geometries).bounds.scale(1.1)) as Bounds).ensureWidthAndHeight()
        java.awt.Color strokeColor = new geoscript.filter.Color(options.get("strokeColor", [99,99,99])).asColor()
        java.awt.Color fillColor = new geoscript.filter.Color(options.get("fillColor", [206,206,206])).asColor()
        java.awt.Color backgroundColor = options.get("backgroundColor", null) == null ? null : new geoscript.filter.Color(options.get("backgroundColor")).asColor()
        String markerShape = options.get("markerShape","square")
        double markerSize = options.get("markerSize", 8)
        float opacity = options.get("opacity", 0.75)
        float strokeWidth = options.get("strokeWidth", 1.0)
        boolean drawCoordinates = options.get("drawCoords", false)

        int imageWidth = size[0]
        int imageHeight = size[1]

        def shapeFactory
        if (markerShape.equalsIgnoreCase("circle")) {
            shapeFactory = new PointShapeFactory.Circle(markerSize)
        } else if (markerShape.equalsIgnoreCase("cross")) {
            shapeFactory = new PointShapeFactory.Cross(markerSize)
        } else if (markerShape.equalsIgnoreCase("star")) {
            shapeFactory = new PointShapeFactory.Star(markerSize)
        } else if (markerShape.equalsIgnoreCase("Triangle")) {
            shapeFactory = new PointShapeFactory.Triangle(markerSize)
        } else if (markerShape.equalsIgnoreCase("X")) {
            shapeFactory = new PointShapeFactory.X(markerSize)
        } else /* if (markerShape.equalsIgnoreCase("square")) */ {
            shapeFactory = new PointShapeFactory.Square(markerSize)
        }

        ShapeWriter shapeWriter = new ShapeWriter({Coordinate mapCoordinate, Point2D shape ->
            double imageX = (1 - (bounds.maxX - mapCoordinate.x) / bounds.width) * imageWidth
            double imageY = ((bounds.maxY - mapCoordinate.y) / bounds.height) * imageHeight
            shape.setLocation(imageX,imageY);
        } as PointTransformation, shapeFactory)

        if (backgroundColor != null) {
            g2d.color = backgroundColor
            g2d.fillRect(0,0,size[0],size[1])    
        }

        Composite strokeComposite = g2d.getComposite()
        Composite fillComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)

        geometries.each{geometry->
            Shape shp = shapeWriter.toShape(geometry.g)
            // Fill
            if (!(geometry.g instanceof org.locationtech.jts.geom.Lineal)) {
                g2d.setComposite(fillComposite)
                g2d.setColor(fillColor)
                g2d.fill(shp)
            }
            // Stroke
            g2d.setComposite(strokeComposite)
            g2d.setStroke(new BasicStroke(strokeWidth))
            g2d.setColor(strokeColor)
            g2d.draw(shp)
            if (drawCoordinates) {
                g2d.setStroke(new BasicStroke(strokeWidth))
                java.util.List coords = geometry.coordinates
                if (coords.size() > 1) {
                    coords.each{c ->
                        Shape coordinateShp = shapeWriter.toShape(geometry.g.getFactory().createPoint(c))
                        // Fill
                        g2d.setComposite(fillComposite)
                        g2d.setColor(fillColor)
                        g2d.fill(coordinateShp)
                        // Stroke
                        g2d.setComposite(strokeComposite)
                        g2d.setColor(strokeColor)
                        g2d.draw(coordinateShp)
                    }
                }
            }
        }
    }

    /**
     * The JPanel used to render Geometry
     */
    private static class Panel extends JPanel {

        /**
         * The List of Geometries to render
         */
        private java.util.List<Geometry> geoms

        /**
         * The Map of options
         */
        private Map options

        /**
         * Create a new Panel with the List of Geometries to render
         * @param geoms The List of Geometries to render
         */
        Panel(java.util.Map options = [:], java.util.List<Geometry> geoms) {
            super()
            // Geometries
            this.geoms = geoms
            // Options
            this.options = options
        }

        /**
         * Override the paintComponent method to render the Geometries
         * @param gr The Graphics context
         */
        void paintComponent(Graphics gr) {
            super.paintComponent(gr)
            Graphics2D g2d = (Graphics2D)gr
            Bounds bounds = new GeometryCollection(geoms).bounds
            bounds.expandBy(bounds.width * 0.10)
            Viewer.draw(options, g2d, geoms)
        }
    }
}