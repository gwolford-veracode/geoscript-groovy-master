package geoscript.geom

import org.geotools.geometry.jts.ReferencedEnvelope
import org.locationtech.jts.geom.Envelope
import geoscript.proj.Projection
import org.locationtech.jts.util.GeometricShapeFactory
import org.locationtech.jts.geom.util.SineStarFactory

/**
 * A Bounds is an Envelope with a {@link geoscript.proj.Projection Projection}.
 * <p><blockquote><pre>
 * Bounds b = new Bounds(1,2,3,4, new {@link geoscript.proj.Projection Projection}("EPSG:2927"))
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class Bounds {
	
    /**
     * The GeoTools' wrapped ReferencedEnvelope
     */
    ReferencedEnvelope env
	
    /**
     * Create a new Bounds wrapping a ReferencedEnvelope.
     * <p><code>ReferencedEnvelope e = new ReferencedEnvelope(1,3,2,4,null)</code></p>
     * <p><code>Bounds b = new Bounds(e)</code></p>
     * @param env The ReferencedEnvelope
     */
    Bounds(ReferencedEnvelope env) {
        this.env = env
    }

    /**
     * Create a new Bounds wrapping an Envelope.
     * <p><code>Envelope e = new Envelope(1,3,2,4)</code></p>
     * <p><code>Bounds b = new Bounds(e)</code></p>
     * @param env The ReferencedEnvelope
     */
    Bounds(Envelope env) {
        this(new ReferencedEnvelope(env, null))
    }

    /**
     * Create a new Bounds with minX, minY, maxX, and maxY coordinates.
     * <p><code>Bounds b = new Bounds(1,2,3,4)</code></p>
     * @param minX The left/west most coordinate (minX)
     * @param minY the bottom/south most coordinate (minY)
     * @param maxX The right/east most coordinate (maxX)
     * @param maxY The top/north most coordinate (maxY)
     */
    Bounds(double minX, double minY, double maxX, double maxY) {
        this(minX, minY, maxX, maxY, null)
    }
	
    /**
     * Create a new Bounds with minX, minY, maxX, and maxY coordinates
     * and a Projection.
     * <p><code>Bounds b = new Bounds(1,2,3,4, new Projection("EPSG:2927"))</code></p>
     * <p><code>Bounds b = new Bounds(1,2,3,4, "EPSG:2927")</code></p>
     * @param minX The left/minX most coordinate (minX)
     * @param minY the bottom/minY most coordinate (minY)
     * @param maxX The right/maxX most coordinate (maxX)
     * @param maxY The top/maxY most coordinate (maxY)
     * @param proj The Projection can either be a Projection or a String
     */
    Bounds(double minX, double minY, double maxX, double maxY, def proj) {
        this(new ReferencedEnvelope(minX, maxX, minY, maxY, new Projection(proj).crs))
    }

    /**
     * Create a new Bounds from a Point (which can either be the origin/lower left or center) and a width and height
     * @param point The Point origin or center
     * @param width The width
     * @param height The height
     * @param isOrigin Whether the Point is the origin (true) or the center (false)
     */
    Bounds(Point point, double width, double height, boolean isOrigin = true) {
        this(createBounds(point, width, height, isOrigin).env)
    }

    /**
     * Create a new Bounds from a Point (which can either be the origin/lower left or center) and a width and height
     * @param point The Point origin or center
     * @param width The width
     * @param height The height
     * @param isOrigin Whether the Point is the origin (true) or the center (false)
     */
    private static Bounds createBounds(Point point, double width, double height, boolean isOrigin = true) {
        // Lower left
        if (isOrigin) {
            return new Bounds(point.x, point.y, point.x + width, point.y + height)
        }
        // Center
        else {
            return new Bounds(point.x - width / 2, point.y - height / 2, point.x + width / 2, point.y + height / 2)
        }
    }

    /**
     * Create a Bounds from a String or return null
     * @param str The string
     * @return A Bounds or null
     */
    static Bounds fromString(String str) {
        Bounds bounds = null
        if (str && str.trim()) {
            def parts = str.split(",")
            if (parts.length >= 4) {
                bounds = new Bounds(parts[0] as double, parts[1] as double, parts[2] as double, parts[3] as double,
                        parts.length == 5 ? new Projection(parts[4]) : null)
            }
            if (!bounds) {
                parts = str.split(" ")
                if (parts.length >= 4) {
                    bounds = new Bounds(parts[0] as double, parts[1] as double, parts[2] as double, parts[3] as double,
                            parts.length == 5 ? new Projection(parts[4]) : null)
                }
            }
        }
        bounds
    }

    /**
     * Get the left/west most coordinate (minX)
     * @return The left/west most coordinate (minX)
     */
    double getMinX() {
        env.minX
    }

    /**
     * Get the right/east most coordinate (maxX)
     * @return The right/east most coordinate (maxX)
     */
    double getMaxX() {
        env.maxX
    }

    /**
     * Get the bottom/south most coordinate (minY)
     * @return The bottom/south most coordinate (minY)
     */
    double getMinY() {
        env.minY
    }

    /**
     * Get the top/north most coordinate (maxY)
     * @return The top/north most coordinate (maxY)
     */
    double getMaxY() {
        env.maxY
    }

    /**
     * Get the area of this Bounds
     * @return The area
     */
    double getArea() {
        env.area
    }

    /**
     * Get the width
     * @return The width
     */
    double getWidth() {
        env.width
    }

    /**
     * Get the height
     * @return The height
     */
    double getHeight() {
        env.height
    }

    /**
     * Get the ratio of width to height for this bounds.
     * @return The aspect ratio
     */
    double getAspect() {
        width / height
    }

    /**
     * Expand the Bounds by the given distance in all directions
     * @param distance The distance
     * @return The modified Bounds
     */
    Bounds expandBy(double distance) {
        env.expandBy(distance)
        this
    }

    /**
     * Expand this Bounds to include another Bounds
     * @param other Another Bounds
     * @return The modified Bounds
     */
    Bounds expand(Bounds other) {
        ReferencedEnvelope otherEnv
        if (this.proj.equals(other.proj)) {
            otherEnv = other.env
        } else {
            otherEnv = other.reproject(this.proj).env
        }
        if (otherEnv.contains(this.env)) {
            this.env = otherEnv
        } else if (!this.env.contains(otherEnv)) {
            env.expandToInclude(otherEnv)
        }
        this
    }

    /**
     * Scales the current Bounds by a specific factor.
     * @param factor The scale factor
     * @return The new scaled Bounds
     */
    Bounds scale(double factor) {
        double w = width * (factor - 1) / 2
        double h = height * (factor - 1) / 2
        new Bounds(minX - w, minY - h, maxX + w, maxY + h)
    }


    /**
     * Get the Projection (if any) or null
     * @return The Projection (if any) or null
     */
    Projection getProj() {
        if (env.coordinateReferenceSystem)
            return new Projection(env.coordinateReferenceSystem)
        else
            return null
    }

    /**
     * Set a Projection if the current Projection is not set
     * or reproject if needed
     * @param projection The new Projection
     */
    void setProj(def projection) {
        def p = new Projection(projection)
        if (!getProj()) {
            this.env = new Bounds(minX, minY, maxX, maxY, p).env
        } else if (!p.equals(getProj())) {
            this.env = reproject(p).env
        }
    }

    /**
     * Reprojects the Bounds
     * @param proj A Projection or String
     * @return The reprojected Bounds
     */
    Bounds reproject(def proj) {
        proj = new Projection(proj)
        new Bounds(env.transform(proj.crs, true))
    }

    /**
     * Convert this Bounds into a Geometry object
     * @return A Geometry
     */
    Geometry getGeometry() {
        Geometry.wrap(Geometry.factory.toGeometry(env))
    }

    /**
     * Convert this Bounds into a Polygon
     * @return A Polygon
     */
    Polygon getPolygon() {
        new Polygon([minX, minY], [minX, maxY], [maxX, maxY], [maxX, minY], [minX, minY])
    }

    /**
     * Get the corners of the Bounds as a List of Points.  The ordering is:
     * [minX,minY],[minX, maxY],[maxX,maxY],[maxX,minY]
     * @return A List of Points
     */
    List getCorners() {
        [
                new Point(minX, minY),
                new Point(minX, maxY),
                new Point(maxX, maxY),
                new Point(maxX, minY)
        ]
    }

    /**
     * Partitions the bounding box into a set of smaller bounding boxes.
     * @param res The resolution to tile at and should be in range 0-1.
     * @return A List of smaller bounding boxes
     */
    List<Bounds> tile(double res) {
        double dx = width * res
        double dy = height * res
        List bounds = []
        double y = minY
        while (y < maxY) {
            double x = minX
            while (x < maxX) {
                bounds.add(new Bounds(x,y,Math.min(x + dx, maxX), Math.min(y+dy, maxY), proj))
                x += dx
            }
            y += dy
        }
        bounds
    }

    /**
     * Calculate a quad tree for this Bounds between the start and stop levels. The Closure
     * is called for each new Bounds generated.
     * @param start The start level
     * @param stop The stop level
     * @param closure The Closure called for each new Bounds
     */
    void quadTree(int start, int stop, Closure closure) {
        Projection p = getProj()
        for(int level = start; level < stop; level++) {
            double factor = Math.pow(2, level)
            double dx = (this.maxX - this.minX) / factor
            double dy = (this.maxY - this.minY) / factor
            double minx = this.minX
            for(int x = 0; x < factor; ++x) {
                double miny = this.minY
                for(int y = 0; y < factor; ++y) {
                    closure(new Bounds(minx, miny, minx + dx, miny + dy, p))
                    miny += dy
                }
                minx += dx
            }
        }
    }

    /**
     * Get whether the Bounds is empty (width and height are zero)
     * @return Whether the Bounds is empty
     */
    boolean isEmpty() {
       env.empty || area == 0.0
    }
    
    /**
     * Determine whether this Bounds equals another Bounds
     * @param other The other Bounds
     * @return Whether this Bounds and the other Bounds are equal
     */
    @Override
    boolean equals(Object other) {
        other instanceof Bounds && env.equals(other.env)
    }

    /**
     * Get the hash code for this Bounds
     * @return The hash code for this Bounds
     */
    @Override
    int hashCode() {
        env.hashCode()
    }

    /**
     * Determine whether this Bounds contains the other Bounds
     * @param other The other Bounds
     * @return Whether this Bounds contains the other Bounds
     */
    boolean contains(Bounds other) {
        env.contains(other.env)
    }

    /**
     * Determine whether this Bounds contains the Point
     * @param point The Point
     * @return Whether this Bounds contains the Point
     */
    boolean contains(Point point) {
        env.contains(point.g.coordinate)
    }
    
    /**
     * Determine whether this Bounds intersects with the other Bounds
     * @param other The other Bounds
     * @return Whether this Bounds intersects with the other Bounds
     */
    boolean intersects(Bounds other) {
        env.intersects(other.env)
    }
    
    /**
     * Calculate the intersection between this Bounds and the other Bounds
     * @param other The other Bounds
     * @return The intersection Bounds between this and the other Bounds
     */
    Bounds intersection(Bounds other) {
        new Bounds(new ReferencedEnvelope(env.intersection(other.env), this.proj?.crs))
    }

    /**
     * Ensure that the Bounds has a width and height.  Handle vertical and horizontal lines and points.
     * @return A new Bounds with a width and height
     */
    Bounds ensureWidthAndHeight() {
        Bounds b = new Bounds(env)
        if (b.width == 0 || b.height == 0) {
            if (b.height > 0) {
                double h = b.height / 2.0
                b = new Bounds(b.minX - h, b.minY, b.maxX + h, b.maxY, b.proj)
            } else if (b.width > 0) {
                double w = b.width / 2.0
                b = new Bounds(b.minX, b.minY - w, b.maxX, b.maxY + w, b.proj)
            } else {
                def e = new Point(b.minX, b.minY).buffer(0.1).envelopeInternal
                b = new Bounds(e.minX, e.minY, e.maxX, e.maxY, proj)
            }
        }
        return b
    }

    /**
     * Fix the aspect ration of this Bounds based on the given width and height.
     * @param w The image width
     * @param h The image height
     * @return A new Bounds
     */
    Bounds fixAspectRatio(int w, int h) {
        double mapWidth = this.width
        double mapHeight = this.height
        double scaleX = w / mapWidth
        double scaleY = h / mapHeight
        double scale
        if (scaleX < scaleY) {
            scale = scaleX
        } else {
            scale = scaleY
        }
        double deltaX = w / scale - mapWidth
        double deltaY = h / scale - mapHeight
        double minX = this.minX - deltaX / 2D
        double maxX = this.maxX + deltaX / 2D
        double minY = this.minY - deltaY / 2D
        double maxY = this.maxY + deltaY / 2D
        new Bounds(minX, minY, maxX, maxY, this.proj)
    }


    /**
     * Get a value from this Bounds at the given index (minX = 0, minY = 1,
     * maxX = 2, maxY = 3).
     * <p><code>Bounds b = new Bounds(1,2,3,4)</code></p>
     * <p><code>def w = b[0]</code></p>
     * <p><code>def (w,s,e,n) = b</code></p>
     * @return A value from this Bounds or null if the index is greater than 3
     */
    Object getAt(int index) {
        if (index == 0) {
            minX
        } else if (index == 1) {
            minY
        } else if (index == 2) {
            maxX
        } else if (index == 3) {
            maxY
        } else {
            null
        }
    }

    /**
     * Get a generated grid with the given number of the rows and column
     * @param columns The number of columns
     * @param rows The number of rows
     * @param type The cell type (polygon, point, circle/ellipse, hexagon, hexagon-inv, triangle)
     * @return The generated grid as a Geometry
     */
    Geometry getGrid(int columns, int rows, String type = "polygon") {
        List geoms = []
        this.generateGrid(rows, columns, type, {cell, c, r ->
            geoms.add(cell)
        })
        new GeometryCollection(geoms)
    }

    /**
     * Generate a grid with the given number or rows and columns
     * @param columns The number of columns
     * @param rows The number of rows
     * @param type The cell type (polygon, point, circle/ellipse, hexagon, hexagon-inv, triangle)
     * @param c A Closure that is called with each Geometry cell with the geometry, the column, and the row
     */
    void generateGrid(int columns, int rows, String type, Closure c) {
        double cellWidth = this.width / columns
        double cellHeight = this.height / rows
        double x = this.minX
        double y = this.minY
        (1..columns).each {column ->
            (1..rows).each {row ->
                Bounds b = new Bounds(x,y,x+cellWidth,y+cellHeight)
                Geometry g = b.geometry
                if (type.equalsIgnoreCase("point")) {
                    g = g.centroid
                } else if (type.equalsIgnoreCase("ellipse") || type.equalsIgnoreCase("circle")) {
                    g = b.createEllipse(100)
                } else if (type.equalsIgnoreCase("hexagon")) {
                    Bounds newBounds = new Bounds(
                            b.minX - b.width / 6,
                            column % 2 == 0 ? b.minY : b.minY - b.height / 2,
                            b.maxX + b.width / 6,
                            column % 2 == 0 ? b.maxY : b.maxY - b.height / 2
                    )
                    g = newBounds.createHexagon()
                } else if (type.equalsIgnoreCase("hexagon-inv")) {
                    Bounds newBounds = new Bounds(
                            row % 2 == 0 ? b.minX : b.minX - b.width / 2,
                            b.minY - b.height / 6,
                            row % 2 == 0 ? b.maxX : b.maxX - b.width / 2,
                            b.maxY + b.height / 6,
                    )
                    g = newBounds.createHexagon(true)
                } else if (type.equalsIgnoreCase("triangle")) {
                    g = b.createTriangles()
                }
                c.call(g, column, row)
                y += cellHeight
            }
            x += cellWidth
            y = this.minY
        }
    }

    /**
     * Get a generated grid with the given cell width and height
     * @param cellWidth The cell width
     * @param cellHeight The cell height
     * @param type The cell type (polygon, point, circle/ellipse, hexagon, hexagon-inv, triangle)
     * @return The generated grid as a Geometry
     */
    Geometry getGrid(double cellWidth, double cellHeight, String type = "polygon") {
        List geoms = []
        this.generateGrid(cellWidth, cellHeight, type, {cell, c, r ->
            geoms.add(cell)
        })
        new GeometryCollection(geoms)
    }

    /**
     * Generate a grid with the given cell width and height
     * @param cellWidth The cell width
     * @param cellHeight The cell height
     * @param type The cell type (polygon, point, circle/ellipse, hexagon, hexagon-inv, triangle)
     * @param c A Closure that is called with each Geometry cell with the geometry, the column, and the row
     */
    void generateGrid(double cellWidth, double cellHeight, String type, Closure c) {
        int columns = (height / cellWidth) as int
        if (height % cellWidth != 0) {
            columns++
        }
        int rows = (width / cellHeight) as int
        if (width % cellHeight != 0) {
            rows++
        }
        generateGrid(columns, rows, type, c)
    }

    /**
     * Create a hexagon based on this Bound's extent
     * @param inverted Whether the hexagon is inverted.  If true then
     * the point is north/south, else point is east/west
     * @return A Polygon
     */
    Polygon createHexagon(boolean inverted = false) {
        double w = this.width
        double h = this.height
        if (inverted) {
            new Polygon([[
                    [this.minX + w / 2, this.minY],
                    [this.maxX, this.minY + h * 1/4],
                    [this.maxX, this.minY + h * 3/4],
                    [this.minX + w / 2, this.maxY],
                    [this.minX, this.minY + h * 3/4],
                    [this.minX, this.minY + h * 1/4],
                    [this.minX + w / 2, this.minY]
            ]])
        } else {
            new Polygon([[
                    [this.minX + w * 1/4, this.minY],
                    [this.minX + w * 3/4, this.minY],
                    [this.maxX, this.minY + h / 2],
                    [this.minX + w * 3/4, this.maxY],
                    [this.minX + w * 1/4, this.maxY],
                    [this.minX, this.minY + h/2],
                    [this.minX + w * 1/4, this.minY]
            ]])
        }
    }

    /**
     * Create triangles to fill the Bounds
     * @return A MultiPolygon of 8 triangles
     */
    MultiPolygon createTriangles() {
        double midX = minX + width / 2
        double midY = minY + height / 2
        new MultiPolygon([
            new Polygon(new LinearRing(new Point(minX, midY), new Point(minX, maxY), new Point(midX, maxY), new Point(minX, midY))),
            new Polygon(new LinearRing(new Point(minX, midY), new Point(midX, maxY), new Point(midX, midY), new Point(minX, midY))),
            new Polygon(new LinearRing(new Point(midX, midY), new Point(midX, maxY), new Point(maxX, midY), new Point(midX, midY))),
            new Polygon(new LinearRing(new Point(midX, maxY), new Point(maxX, maxY), new Point(maxX, midY), new Point(midX, maxY))),
            new Polygon(new LinearRing(new Point(minX, minY), new Point(minX, midY), new Point(midX, minY), new Point(minX, minY))),
            new Polygon(new LinearRing(new Point(minX, midY), new Point(midX, minY), new Point(midX, midY), new Point(minX, midY))),
            new Polygon(new LinearRing(new Point(midX, minY), new Point(midX, midY), new Point(maxX, midY), new Point(midX, minY))),
            new Polygon(new LinearRing(new Point(midX, minY), new Point(maxX, minY), new Point(maxX, midY), new Point(midX, minY))),
        ])
    }

    /**
     * Create a rectangle or square based on this Bound's extent with the given number of points and rotation.
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The rectangular Geometry
     */
    Polygon createRectangle(int numPoints = 20, double rotation = 0) {
        Geometry.wrap(createGeometricShapeFactory(numPoints, rotation).createRectangle()) as Polygon
    }

    /**
     * Create an ellipse or circle based on this Bound's extent with the given number of points and rotation.
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The elliptical or circular Geometry
     */
    Polygon createEllipse(int numPoints = 20, double rotation = 0) {
        Geometry.wrap(createGeometricShapeFactory(numPoints, rotation).createEllipse()) as Polygon
    }

    /**
     * Create a squircle based on this Bound's extent with the given number of points and rotation.
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The squircular Geometry
     */
    Polygon createSquircle(int numPoints = 20, double rotation = 0) {
        Geometry.wrap(createGeometricShapeFactory(numPoints, rotation).createSquircle()) as Polygon
    }

    /**
     * Create a super circle based on this Bound's extent with the given number of points and rotation.
     * @param power The positive power
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The super circular Geometry
     */
    Polygon createSuperCircle(double power, int numPoints = 20, double rotation = 0) {
        Geometry.wrap(createGeometricShapeFactory(numPoints, rotation).createSupercircle(power)) as Polygon
    }

    /**
     * Create a LineString arc based on this Bound's extent from the start angle (in radians) for the given angle extent
     * (also in radians) with the given number of points and rotation.
     * @param startAngle The start angle (in radians)
     * @param angleExtent The extent of the angle (in radians)
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The LineString arc
     */
    LineString createArc(double startAngle, double angleExtent, int numPoints = 20, double rotation = 0) {
        Geometry.wrap(createGeometricShapeFactory(numPoints, rotation).createArc(startAngle, angleExtent)) as LineString
    }

    /**
     * Create a Polygon arc based on this Bound's extent from the start angle (in radians) for the given angle extent
     * (also in radians) with the given number of points and rotation.
     * @param startAngle The start angle (in radians)
     * @param angleExtent The extent of the angle (in radians)
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The Polygon arc
     */
    Polygon createArcPolygon(double startAngle, double angleExtent, int numPoints = 20, double rotation = 0) {
        Geometry.wrap(createGeometricShapeFactory(numPoints, rotation).createArcPolygon(startAngle, angleExtent)) as Polygon
    }

    /**
     * Create a sine star based on this Bound's extent with the given number of arms and arm length ratio with the
     * given number of points and rotation.
     * @param numberOfArms The number of arms
     * @param armLengthRatio The arm length ratio
     * @param numPoints The number of points
     * @param rotation The rotation angle
     * @return The sine star Polygon
     */
    Polygon createSineStar(int numberOfArms, double armLengthRatio, int numPoints = 20, double rotation = 0) {
        def shapeFactory = createGeometricShapeFactory(numPoints, rotation, new SineStarFactory())  as SineStarFactory
        shapeFactory.setArmLengthRatio(armLengthRatio)
        shapeFactory.setNumArms(numberOfArms)
        Geometry.wrap(shapeFactory.createSineStar()) as Polygon
    }

    /**
     * Create a GeometricShapeFactory and initialize it with number of points and a rotation.
     * @param numPoints The number of points
     * @param rotation The rotation
     * @param shapeFactory The GeometricShapeFactory
     * @return The initialized GeometricShapeFactory
     */
    private GeometricShapeFactory createGeometricShapeFactory(int numPoints = 100, double rotation = 0.0, GeometricShapeFactory shapeFactory = new GeometricShapeFactory()) {
        shapeFactory.numPoints = numPoints
        shapeFactory.rotation = rotation
        shapeFactory.envelope = env
        shapeFactory
    }

    /**
     * Override the asType method to convert Bounds to Geometry
     * if the Class is Geometry
     * @param type The Class
     * @return The converted Object
     */
    Object asType(Class type) {
        if (type == Geometry) {
            return this.geometry
        } else {
            return super.asType(type)
        }
    }

    /**
     * The string representation
     * @return The string representation
     */
    String toString() {
        "(${minX},${minY},${maxX},${maxY}${if (proj != null){',' + proj.id } else {''}})"
    }

}
