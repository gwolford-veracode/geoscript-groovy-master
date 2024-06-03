package geoscript.style

import org.geotools.image.test.ImageAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import javax.imageio.ImageIO

import static org.junit.jupiter.api.Assertions.*
import geoscript.filter.Color

import geoscript.render.Map
import geoscript.layer.Shapefile

/**
 * The UniqueValues UnitTest
 * @author Jared Erickson
 */
class UniqueValuesTest {

    @TempDir
    File folder

    @Test void create() {

        // Get states shapefile
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shapefile = new Shapefile(file)

        // Create a Map
        Map map = new Map()
        map.addLayer(shapefile)

        // Default is random colors
        UniqueValues sym1 = new UniqueValues(shapefile, "STATE_ABBR")
        assertNotNull(sym1)
        assertEquals(49, sym1.parts.size())

        shapefile.style = sym1
        File imgFile = new File(folder,"uniquevalues_states1.png")
        map.render(imgFile)
        assertTrue imgFile.length() > 0

        // Color palette
        UniqueValues sym2 = new UniqueValues(shapefile, "STATE_ABBR", "Greens")
        assertNotNull(sym2)
        assertEquals(49, sym2.parts.size())

        shapefile.style = sym2
        imgFile = new File(folder,"uniquevalues_states2.png")
        map.render(imgFile)
        ImageAssert.assertEquals(getFile("geoscript/style/uniquevalues_states2.png"), ImageIO.read(imgFile), 200)

        // Color list
        UniqueValues sym3 = new UniqueValues(shapefile, "STATE_ABBR", ["teal","slateblue","tan","wheat","salmon"])
        assertNotNull(sym3)
        assertEquals(49, sym3.parts.size())

        shapefile.style = sym3
        imgFile = new File(folder,"uniquevalues_states3.png")
        map.render(imgFile)
        ImageAssert.assertEquals(getFile("geoscript/style/uniquevalues_states3.png"), ImageIO.read(imgFile), 300)

        // Color Closure
        UniqueValues sym4 = new UniqueValues(shapefile, "STATE_ABBR", {i,v -> Color.getRandom()})
        assertNotNull(sym4)
        assertEquals(49, sym4.parts.size())

        shapefile.style = sym4
        imgFile = new File(folder,"uniquevalues_states4.png")
        map.render(imgFile)
        assertTrue imgFile.length() > 0
    }

    private File getFile(String resource) {
        new File(getClass().getClassLoader().getResource(resource).toURI())
    }
}
