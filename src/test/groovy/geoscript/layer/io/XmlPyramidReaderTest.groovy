package geoscript.layer.io

import geoscript.layer.Pyramid
import org.junit.jupiter.api.Test

import static geoscript.AssertUtil.assertPyramidsAreEquals
import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * The XmlPyramidReader Unit Test
 * @author Jared Erickson
 */
class XmlPyramidReaderTest {

    @Test void read() {
        String xml = """<pyramid>
  <proj>EPSG:3857</proj>
  <bounds>
    <minX>-2.0036395147881314E7</minX>
    <minY>-2.0037471205137067E7</minY>
    <maxX>2.0036395147881314E7</maxX>
    <maxY>2.003747120513706E7</maxY>
  </bounds>
  <origin>BOTTOM_LEFT</origin>
  <tileSize>
    <width>256</width>
    <height>256</height>
  </tileSize>
  <grids>
    <grid>
      <z>0</z>
      <width>1</width>
      <height>1</height>
      <xres>156412.0</xres>
      <yres>156412.0</yres>
    </grid>
    <grid>
      <z>1</z>
      <width>2</width>
      <height>2</height>
      <xres>78206.0</xres>
      <yres>78206.0</yres>
    </grid>
    <grid>
      <z>2</z>
      <width>4</width>
      <height>4</height>
      <xres>39103.0</xres>
      <yres>39103.0</yres>
    </grid>
    <grid>
      <z>3</z>
      <width>8</width>
      <height>8</height>
      <xres>19551.5</xres>
      <yres>19551.5</yres>
    </grid>
    <grid>
      <z>4</z>
      <width>16</width>
      <height>16</height>
      <xres>9775.75</xres>
      <yres>9775.75</yres>
    </grid>
    <grid>
      <z>5</z>
      <width>32</width>
      <height>32</height>
      <xres>4887.875</xres>
      <yres>4887.875</yres>
    </grid>
    <grid>
      <z>6</z>
      <width>64</width>
      <height>64</height>
      <xres>2443.9375</xres>
      <yres>2443.9375</yres>
    </grid>
    <grid>
      <z>7</z>
      <width>128</width>
      <height>128</height>
      <xres>1221.96875</xres>
      <yres>1221.96875</yres>
    </grid>
    <grid>
      <z>8</z>
      <width>256</width>
      <height>256</height>
      <xres>610.984375</xres>
      <yres>610.984375</yres>
    </grid>
    <grid>
      <z>9</z>
      <width>512</width>
      <height>512</height>
      <xres>305.4921875</xres>
      <yres>305.4921875</yres>
    </grid>
    <grid>
      <z>10</z>
      <width>1024</width>
      <height>1024</height>
      <xres>152.74609375</xres>
      <yres>152.74609375</yres>
    </grid>
    <grid>
      <z>11</z>
      <width>2048</width>
      <height>2048</height>
      <xres>76.373046875</xres>
      <yres>76.373046875</yres>
    </grid>
    <grid>
      <z>12</z>
      <width>4096</width>
      <height>4096</height>
      <xres>38.1865234375</xres>
      <yres>38.1865234375</yres>
    </grid>
    <grid>
      <z>13</z>
      <width>8192</width>
      <height>8192</height>
      <xres>19.09326171875</xres>
      <yres>19.09326171875</yres>
    </grid>
    <grid>
      <z>14</z>
      <width>16384</width>
      <height>16384</height>
      <xres>9.546630859375</xres>
      <yres>9.546630859375</yres>
    </grid>
    <grid>
      <z>15</z>
      <width>32768</width>
      <height>32768</height>
      <xres>4.7733154296875</xres>
      <yres>4.7733154296875</yres>
    </grid>
    <grid>
      <z>16</z>
      <width>65536</width>
      <height>65536</height>
      <xres>2.38665771484375</xres>
      <yres>2.38665771484375</yres>
    </grid>
    <grid>
      <z>17</z>
      <width>131072</width>
      <height>131072</height>
      <xres>1.193328857421875</xres>
      <yres>1.193328857421875</yres>
    </grid>
    <grid>
      <z>18</z>
      <width>262144</width>
      <height>262144</height>
      <xres>0.5966644287109375</xres>
      <yres>0.5966644287109375</yres>
    </grid>
    <grid>
      <z>19</z>
      <width>524288</width>
      <height>524288</height>
      <xres>0.29833221435546875</xres>
      <yres>0.29833221435546875</yres>
    </grid>
  </grids>
</pyramid>
"""
        Pyramid actual = new XmlPyramidReader().read(xml)
        Pyramid expected = Pyramid.createGlobalMercatorPyramid()
        assertPyramidsAreEquals(expected, actual, 0.0000001)
    }
}
