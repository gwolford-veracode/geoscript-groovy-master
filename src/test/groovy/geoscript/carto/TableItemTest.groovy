package geoscript.carto

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*

class TableItemTest extends AbstractCartoTest {

    @Test
    void create() {

        TableItem item = new TableItem(10,20,300,400)
            .columns(["ID"]).column("Name")
            .row([[ID: 1, Name: "One"]])
            .row([[ID: 2, Name: "Two"]])
            .row([[ID: 3, Name: "Three"]])

        assertEquals(10, item.x)
        assertEquals(20, item.y)
        assertEquals(300, item.width)
        assertEquals(400, item.height)
        assertEquals(2, item.columns.size())
        assertEquals(3, item.rows.size())
        assertTrue(item.toString().startsWith("TableItem(x = 10, y = 20, width = 300, height = 400, columns = [ID, Name], rows = [[ID:1, Name:One], [ID:2, Name:Two], [ID:3, Name:Three]], "))
    }

    @Test
    void draw() {
        draw("table", 300, 100, { CartoBuilder cartoBuilder ->
            cartoBuilder.table(new TableItem(10,10,280,80)
                    .columns(["ID"]).column("Name")
                    .row([[ID: 1, Name: "One"]])
                    .row([[ID: 2, Name: "Two"]])
                    .row([[ID: 3, Name: "Three"]])
            )
        })
    }

}
