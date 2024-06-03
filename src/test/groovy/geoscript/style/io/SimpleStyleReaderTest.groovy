package geoscript.style.io

import geoscript.style.Composite
import geoscript.style.Icon
import geoscript.style.Label
import geoscript.style.Style
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*

/**
 * The SimpleStyleReader Unit Test
 * @author Jared Erickson
 */
class SimpleStyleReaderTest {

    @TempDir
    File folder

    @Test void readFromString() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        // Fill and Stroke
        Style style = styleReader.read("fill=#555555 fill-opacity=0.6 stroke=#555555 stroke-width=0.5")
        assertEquals style.toString(), "Composite (Fill(color = #555555, opacity = 0.6), Stroke(color = #555555, width = 0.5))"
        // Shape with Fill and Stroke
        style = styleReader.read("fill=navy stroke=yellow shape-type=circle")
        assertEquals style.toString(), "Composite (Shape(color = #000080, size = 6, type = circle, stroke = Stroke(color = #ffff00, width = 0.5)))"
        // Shape with Fill and Stroke with Label
        style = styleReader.read("fill=#554466 stroke=255,255,0 shape-type=triangle label=NAME label-size=12")
        assertEquals style.toString(), "Composite (Shape(color = #554466, size = 6, type = triangle, stroke = Stroke(color = #ffff00, width = 0.5)), Label(property = NAME))"
        // Just fill
        style = styleReader.read("fill=#554466")
        assertEquals style.toString(), "Composite (Fill(color = #554466, opacity = 0.6))"
        // Just stroke
        style = styleReader.read("stroke=#554466")
        assertEquals style.toString(), "Composite (Stroke(color = #554466, width = 0.5))"
        // Just shape
        style = styleReader.read("shape=#554466")
        assertEquals style.toString(), "Composite (Shape(color = #554466, size = 6, type = circle))"
        // Icon
        style = styleReader.read("icon=place.png")
        Composite composite = style as Composite
        Icon icon = composite.parts[0] as Icon
        assertTrue(icon.url.toString().endsWith("place.png"))
        assertEquals(-1, icon.size.value, 0.1)
        assertEquals("image/png", icon.format)
        // Icon with Size
        style = styleReader.read("icon=place.jpeg icon-size=8")
        composite = style as Composite
        icon = composite.parts[0] as Icon
        assertTrue(icon.url.toString().endsWith("place.jpeg"))
        assertEquals(8, icon.size.value, 0.1)
        assertEquals("image/jpeg", icon.format)
    }

    @Test void pointWithLabelWithHalo() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("shape-type=triangle label=NAME label-size=12 label-halo-color=white label-halo-radius=7")
        assertEquals style.toString(), "Composite (Shape(color = #7e7e7e, size = 6, type = triangle), Label(property = NAME))"
    }

    @Test void pointWithLabelWithPointPlacement() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("shape-type=triangle label=NAME label-placement=point")
        assertEquals style.toString(), "Composite (Shape(color = #7e7e7e, size = 6, type = triangle), Label(property = NAME))"
    }

    @Test void pointWithLabelWithComplexPointPlacement() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("shape-type=triangle label=NAME label-placement=point label-point-anchor=5,6 label-point-displace=10,10 label-point-rotate=45")
        assertEquals style.toString(), "Composite (Shape(color = #7e7e7e, size = 6, type = triangle), Label(property = NAME))"
    }

    @Test void pointWithLabelWithLinePlacement() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("shape-type=triangle label=NAME label-placement=line")
        assertEquals style.toString(), "Composite (Shape(color = #7e7e7e, size = 6, type = triangle), Label(property = NAME))"
    }

    @Test void pointWithLabelWithComplexLinePlacement() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("shape-type=triangle label=NAME label-placement=line label-line-offset=4 label-line-gap=2 label-line-igap=3 label-line-align=true label-line-follow=true label-line-group=true label-line-displacement=12 label-line-repeat=true")
        assertEquals style.toString(), "Composite (Shape(color = #7e7e7e, size = 6, type = triangle), Label(property = NAME))"
    }

    @Test void polygonWithAlign() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("fill=#555555 fill-opacity=0.6 stroke=#555555 stroke-width=0.5 label=name label-polygonalign=mbr")
        assertEquals style.toString(), "Composite (Fill(color = #555555, opacity = 0.6), Stroke(color = #555555, width = 0.5), Label(property = name))"
        Composite composite = style as Composite
        assertEquals("mbr", composite.parts[2].options["polygonAlign"])
    }

    @Test void labelMaxDisplacementAndMaxAngleDelta() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read("fill=#555555 fill-opacity=0.6 stroke=#555555 stroke-width=0.5 label=name label-maxdisplacement=10 label-maxangledelta=45")
        assertEquals style.toString(), "Composite (Fill(color = #555555, opacity = 0.6), Stroke(color = #555555, width = 0.5), Label(property = name))"
        Composite composite = style as Composite
        assertEquals("45.0", composite.parts[2].options["maxAngleDelta"])
        assertEquals("10.0", composite.parts[2].options["maxDisplacement"])
    }

    @Test void readFromMap() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        Style style = styleReader.read([fill: 'wheat', 'stroke-width': 1.2])
        assertEquals style.toString(), "Composite (Fill(color = #f5deb3, opacity = 0.6), Stroke(color = #555555, width = 1.2))"
    }

    @Test void readFromFile() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        File file = new File(folder,"stroke.txt")
        file.text = "stroke=black stroke-width=0.1 stroke-opacity=0.55 no-fill=true"
        Style style = styleReader.read(file)
        assertEquals style.toString(), "Composite (Stroke(color = #000000, width = 0.1))"
    }

    @Test void readFromInputStream() {
        SimpleStyleReader styleReader = new SimpleStyleReader()
        File file = new File(folder,"stroke.txt")
        file.text = "fill=blue fill-opacity=0.55 no-stroke=true"
        file.withInputStream { InputStream inputStream ->
            Style style = styleReader.read(inputStream)
            assertEquals style.toString(), "Composite (Fill(color = #0000ff, opacity = 0.55))"
        }
    }

    @Test void readers() {
        Reader reader = Readers.find("simple")
        assertNotNull reader
        assertTrue reader instanceof SimpleStyleReader
    }

}
