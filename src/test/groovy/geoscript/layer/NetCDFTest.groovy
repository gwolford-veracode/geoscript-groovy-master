package geoscript.layer

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*

/**
 * The NetCDF Test Case
 */
class NetCDFTest {

    @Test void read() {
        File file = new File(getClass().getClassLoader().getResource("O3-NO2.nc").toURI())
        NetCDF netcdf = new NetCDF(file)
        assertNotNull(netcdf)
        assertEquals "NetCDF", netcdf.name
        assertNotNull(netcdf.read("O3"))
        assertNotNull(netcdf.read("NO2"))
    }

    @Test void getNames() {
        File file = new File(getClass().getClassLoader().getResource("O3-NO2.nc").toURI())
        NetCDF netcdf = new NetCDF(file)
        assertNotNull(netcdf)
        List names = netcdf.names
        assertEquals(2, names.size())
        assertTrue(names.contains("O3"))
        assertTrue(names.contains("NO2"))
        netcdf.names.each{ String name ->
            Raster raster = netcdf.read(name)
            assertNotNull raster
            assertNotNull raster.proj
            assertNotNull raster.bounds
            raster.dispose()
        }
    }
}
