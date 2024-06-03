/*
 *  The MIT License
 * 
 *  Copyright 2010 Jared Erickson.
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package geoscript.geom.io

import groovy.xml.StreamingMarkupBuilder
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import geoscript.geom.*

/**
 * The GeoRSSWriter UnitTest
 * @author Jared Erickson
 */
class GeoRSSWriterTest {

    @Test void writePoint() {
        GeoRSSWriter writer = new GeoRSSWriter()
        Point p = new Point(-71.92, 45.256)
        assertEquals "<georss:point>45.256 -71.92</georss:point>", writer.write(p)
        writer = new GeoRSSWriter(type: "simple")
        assertEquals "<georss:point>45.256 -71.92</georss:point>", writer.write(p)
        writer = new GeoRSSWriter(type: "gml")
        assertEquals "<georss:where><gml:Point><gml:pos>45.256 -71.92</gml:pos></gml:Point></georss:where>", writer.write(p)
        writer = new GeoRSSWriter(type: "w3c")
        assertEquals "<geo:Point><geo:lat>45.256</geo:lat><geo:long>-71.92</geo:long></geo:Point>", writer.write(p)
    }

    @Test void writeLineString() {
        GeoRSSWriter writer = new GeoRSSWriter()
        LineString l = new LineString([-110.45,45.256], [-109.48,46.46], [-109.86,43.84])
        assertEquals "<georss:line>45.256 -110.45 46.46 -109.48 43.84 -109.86</georss:line>", writer.write(l)
        writer = new GeoRSSWriter(type: "simple")
        assertEquals "<georss:line>45.256 -110.45 46.46 -109.48 43.84 -109.86</georss:line>", writer.write(l)
        writer = new GeoRSSWriter(type: "gml")
        assertEquals "<georss:where><gml:LineString><gml:posList>45.256 -110.45 46.46 -109.48 43.84 -109.86</gml:posList></gml:LineString></georss:where>", writer.write(l)
        writer = new GeoRSSWriter(type: "w3c")
        assertNull writer.write(l)
    }

    @Test void writePolygon() {
        GeoRSSWriter writer = new GeoRSSWriter()
        Polygon p = new Polygon([-110.45,45.256], [-109.48,46.46], [-109.86,43.84], [-110.45,45.256])
        assertEquals "<georss:polygon>45.256 -110.45 46.46 -109.48 43.84 -109.86 45.256 -110.45</georss:polygon>", writer.write(p)
        writer = new GeoRSSWriter(type: "simple")
        assertEquals "<georss:polygon>45.256 -110.45 46.46 -109.48 43.84 -109.86 45.256 -110.45</georss:polygon>", writer.write(p)
        writer = new GeoRSSWriter(type: "gml")
        assertEquals "<georss:where><gml:Polygon><gml:exterior><gml:LinearRing><gml:posList>45.256 -110.45 46.46 -109.48 43.84 -109.86 45.256 -110.45</gml:posList></gml:LinearRing></gml:exterior></gml:Polygon></georss:where>", writer.write(p)
        writer = new GeoRSSWriter(type: "w3c")
        assertNull writer.write(p)
    }

    @Test void writeUsingMarkupBuilder() {
        StreamingMarkupBuilder builder = new StreamingMarkupBuilder()
        // Simple
        GeoRSSWriter writer = new GeoRSSWriter(type: "simple")
        // Point
        def actual = builder.bind { b ->
            mkp.declareNamespace([georss: "http://www.georss.org/georss"])
            writer.write b, new Point(-71.92, 45.256)
        } as String
        String expected = "<georss:point xmlns:georss='http://www.georss.org/georss'>45.256 -71.92</georss:point>"
        assertEquals expected, actual
        // LineString
        actual = builder.bind { b ->
            mkp.declareNamespace([georss: "http://www.georss.org/georss"])
            writer.write b, new LineString([-110.45,45.256], [-109.48,46.46], [-109.86,43.84])
        } as String
        expected = "<georss:line xmlns:georss='http://www.georss.org/georss'>45.256 -110.45 46.46 -109.48 43.84 -109.86</georss:line>"
        assertEquals expected, actual
        // Polygon
        actual = builder.bind { b ->
            mkp.declareNamespace([georss: "http://www.georss.org/georss"])
            writer.write b, new Polygon([-110.45,45.256], [-109.48,46.46], [-109.86,43.84], [-110.45,45.256])
        } as String
        expected = "<georss:polygon xmlns:georss='http://www.georss.org/georss'>45.256 -110.45 46.46 -109.48 43.84 -109.86 45.256 -110.45</georss:polygon>"
        assertEquals expected, actual

        // GML
        writer = new GeoRSSWriter(type: "gml")
        // Point
        actual = builder.bind { b ->
            mkp.declareNamespace([georss: "http://www.georss.org/georss", gml: "http://www.opengis.net/gml"])
            writer.write b, new Point(-71.92, 45.256)
        } as String
        expected = "<georss:where xmlns:georss='http://www.georss.org/georss' xmlns:gml='http://www.opengis.net/gml'><gml:Point><gml:pos>45.256 -71.92</gml:pos></gml:Point></georss:where>"
        assertEquals expected, actual
        // LineString
        actual = builder.bind { b ->
            mkp.declareNamespace([georss: "http://www.georss.org/georss", gml: "http://www.opengis.net/gml"])
            writer.write b, new LineString([-110.45,45.256], [-109.48,46.46], [-109.86,43.84])
        } as String
        expected = "<georss:where xmlns:georss='http://www.georss.org/georss' xmlns:gml='http://www.opengis.net/gml'><gml:LineString><gml:posList>45.256 -110.45 46.46 -109.48 43.84 -109.86</gml:posList></gml:LineString></georss:where>"
        assertEquals expected, actual
        // Polygon
        actual = builder.bind { b ->
            mkp.declareNamespace([georss: "http://www.georss.org/georss", gml: "http://www.opengis.net/gml"])
            writer.write b, new Polygon([-110.45,45.256], [-109.48,46.46], [-109.86,43.84], [-110.45,45.256])
        } as String
        expected = "<georss:where xmlns:georss='http://www.georss.org/georss' xmlns:gml='http://www.opengis.net/gml'><gml:Polygon><gml:LinearRing><gml:posList>45.256 -110.45 46.46 -109.48 43.84 -109.86 45.256 -110.45</gml:posList></gml:LinearRing></gml:Polygon></georss:where>"
        assertEquals expected, actual

        // W3C
        writer = new GeoRSSWriter(type: "w3c")
        // Point
        actual = builder.bind { b ->
            mkp.declareNamespace(geo: "http://www.w3.org/2003/01/geo/wgs84_pos#")
            writer.write b, new Point(-71.92, 45.256)
        } as String
        expected = "<geo:Point xmlns:geo='http://www.w3.org/2003/01/geo/wgs84_pos#'><geo:lat>45.256</geo:lat><geo:long>-71.92</geo:long></geo:Point>"
        assertEquals expected, actual
    }
}

