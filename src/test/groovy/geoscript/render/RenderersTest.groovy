package geoscript.render

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*

/**
 * The Renderers Unit Test
 * @author Jared Erickson
 */
class RenderersTest {

    @Test void list() {
        List<Renderer> renderers = Renderers.list()
        assertNotNull renderers
        assertTrue renderers.size() > 0
    }

    @Test void find() {
        Renderer renderer = Renderers.find("png")
        assertNotNull renderer
        renderer = Renderers.find("jpeg")
        assertNotNull renderer

        renderer = Renderers.find("asdf")
        assertNull renderer
    }
}
