package geoscript.layer

import geoscript.AssertUtil
import geoscript.FileUtil
import geoscript.filter.Expression
import geoscript.style.Stroke
import geoscript.workspace.Directory
import groovy.json.JsonSlurper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*
import geoscript.feature.Schema
import geoscript.feature.Field
import geoscript.feature.Feature
import geoscript.proj.Projection
import geoscript.filter.Filter
import geoscript.workspace.Memory
import geoscript.workspace.Property
import geoscript.geom.*
import geoscript.workspace.Workspace
import geoscript.workspace.H2

/**
 * The Layer UnitTest
 */
class LayerTest {

    @TempDir
    private File folder

    @Test void createLayer() {
        Layer layer1 = new Layer()
        assertTrue layer1.name.startsWith("layer_")
        Layer layer2 = new Layer()
        assertTrue layer2.name.startsWith("layer_")
        Layer layer3 = new Layer("cities")
        assertEquals "cities", layer3.name
        Layer layer4 = new Layer("stations")
        assertEquals "stations", layer4.name
    }

    @Test void fromGeometry() {
        Layer layer = Layer.fromGeometry("world", new Bounds(-180,-90,180,90).geometry)
        assertEquals("world", layer.name)
        assertEquals(1, layer.count)
        assertEquals("EPSG:4326", layer.proj.id)

        layer = Layer.fromGeometry("world", new Bounds(-180,-85,180,85, "EPSG:4326").reproject("EPSG:3857").geometry,
                proj: new Projection("EPSG:3857"),
                style: new Stroke("black", 0.50)
        )
        assertEquals("world", layer.name)
        assertEquals(1, layer.count)
        assertEquals("EPSG:3857", layer.proj.id)
    }

    @Test void fromGeometries() {
        Layer layer = Layer.fromGeometries("testPits", Geometry.createRandomPoints(new Bounds(-180,-90,180,90).geometry, 10).geometries)
        assertEquals("testPits", layer.name)
        assertEquals("Point", layer.schema.geom.typ)
        assertEquals(10, layer.count)
        assertEquals("EPSG:4326", layer.proj.id)

        layer = Layer.fromGeometries("geometries", [new Point(1,2), new LineString([1,2],[3,4])],
            proj: new Projection("EPSG:4326"),
            style: new Stroke("blue", 1)
        )
        assertEquals("geometries", layer.name)
        assertEquals("GeometryCollection", layer.schema.geom.typ)
        assertEquals(2, layer.count)
        assertEquals("EPSG:4326", layer.proj.id)
    }

    @Test void eachFeature() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("states.shp").toURI()))
        int count = 0
        layer.eachFeature({f ->
            assertTrue f instanceof Feature
            count++
        })
        assertEquals 49, count
        
        String name
        layer.eachFeature("STATE_NAME = 'Maryland'", {f ->
            name = f.get("STATE_NAME")
        })
        assertEquals "Maryland", name

        layer.eachFeature(filter: "STATE_NAME = 'Oregon'") {f ->
            name = f.get("STATE_NAME")
        }
        assertEquals "Oregon", name
    }

    @Test void collectFromFeature() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("states.shp").toURI()))
        List results = layer.collectFromFeature({f ->
            f.get("STATE_NAME")
        })
        assertEquals 49, results.size()
        assertTrue results.contains("Utah")

        results = layer.collectFromFeature("STATE_NAME = 'Utah'", {f ->
            f.get("STATE_NAME")
        })
        assertEquals 1, results.size()
        assertEquals "Utah", results[0]

        results = layer.collectFromFeature(filter: "STATE_NAME = 'Washington'") {f ->
            f.get("STATE_NAME")
        }
        assertEquals 1, results.size()
        assertEquals "Washington", results[0]
    }
    
    @Test void getProjection() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        assertEquals "EPSG:2927", layer1.proj.toString()
    }

    @Test void setProjection() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.proj = "EPSG:2927"
        assertEquals "EPSG:2927", layer1.proj.toString()
    }

    @Test void count() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        assertEquals 0, layer1.count()
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        assertEquals 1, layer1.count()

        Layer layer2 = new Shapefile(new File(getClass().getClassLoader().getResource("states.shp").toURI()))
        assertEquals 49, layer2.count()
        assertEquals 1, layer2.count(new Filter("STATE_NAME='Washington'"))
        assertEquals 1, layer2.count("STATE_NAME='Washington'")
        assertEquals 0, layer2.count(new Filter("STATE_NAME='BAD_STATE_NAME'"))
        assertEquals 1, layer2.count(filter: "STATE_NAME='Oregon'")
    }

    @Test void add() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        assertEquals 0, layer1.count()
        // Add a Feature
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        assertEquals 1, layer1.count()
        // Add a Feature by passing a List of values
        layer1.add([new Point(110,-46), "House 2", 14.1])
        assertEquals 2, layer1.count()
        // Add a List of Features
        layer1.add([
            new Feature([new Point(109,-45), "House 3", 15.5], "house2", s1),
            new Feature([new Point(108,-44), "House 4", 16.5], "house3", s1),
            new Feature([new Point(107,-43), "House 5", 17.5], "house4", s1)
        ])
        assertEquals 5, layer1.count()
        // Add a List of Maps
        layer1.add([
            [geom: new Point(100,-45), name: "Point 1", price: 1.0],
            [geom: new Point(101,-46), name: "Point 2", price: 10.0],
            [geom: new Point(102,-47), name: "Point 3", price: 100.0],
        ])
        assertEquals 8, layer1.count()
    }

    @Test void addFeaturesFromOneLayerToAnother() {
        Schema s1 = new Schema("facilities1", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities1", s1)

        Schema s2 = new Schema("facilities2", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer2 = new Layer("facilities2", s2)

        layer1.add([
            new Feature([new Point(109,-45), "House 3", 15.5], "house2", s1),
            new Feature([new Point(108,-44), "House 4", 16.5], "house3", s1),
            new Feature([new Point(107,-43), "House 5", 17.5], "house4", s1)
        ])
        assertEquals 3, layer1.count()

        layer1.eachFeature {f ->
            layer2.add(f)
        }
        assertEquals 3, layer2.count()

        layer2.add(layer1.features)
        assertEquals 6, layer2.count()
    }

    @Test void addFeaturesToLayerWithDifferentGeomName() {
        Schema schema1 = new Schema("points", [
            new Field("geom","Point","EPSG:4326"),
            new Field("id","int")
        ])
        Layer layer1 = new Memory().create(schema1)
        layer1.add([
            new Feature([geom: new Point(0,0), id: 1], "feature1"),
            new Feature([geom: new Point(1,1), id: 2], "feature2"),
            new Feature([geom: new Point(2,2), id: 3], "feature3")
        ])
        assertEquals 3, layer1.count

        File file = new File(folder,"points.shp")
        String name = file.name.substring(0, file.name.lastIndexOf(".shp"))
        Schema schema2 = new Schema(name, [
            new Field("the_geom","Point","EPSG:4326"),
            new Field("id","int")
        ])
        Layer layer2 = new Directory(file.parentFile).create(schema2)
        layer1.eachFeature{ f ->
            layer2.add(f)
        }
        assertEquals 3, layer2.count
        layer2.eachFeature{ f->
            assertNotNull f.geom
            assertNotNull f["id"]
        }
    }

    @Test void plus() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        assertEquals 0, layer1.count()
        layer1 + new Feature([new Point(111,-47), "House", 12.5], "house1", s1)
        assertEquals 1, layer1.count()
    }

    @Test void features() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        List<Feature> features = layer1.features
        assertEquals 1, features.size()
        features = layer1.getFeatures(filter: "name = 'House'")
        assertEquals 1, features.size()
    }

    @Test void bounds() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        Bounds bounds = layer1.bounds()
        assertNotNull(bounds);
        assertEquals(111.0, bounds.minX, 0.1)
        assertEquals(-47.0, bounds.minY, 0.1)
        assertEquals(111.0, bounds.maxX, 0.1)
        assertEquals(-47.0, bounds.maxY, 0.1)
        layer1.add(new Feature([new Point(108,-44), "House 2", 16.5], "house2", s1))
        bounds = layer1.bounds("name = 'House 2'")
        assertEquals new Bounds(108.0,-44.0,108.0,-44.0,"EPSG:2927"), bounds
        bounds = layer1.bounds(filter: "name = 'House 2'")
        assertEquals new Bounds(108.0,-44.0,108.0,-44.0,"EPSG:2927"), bounds
    }

    @Test void cursor() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(111,-47), "House 1", 12.5], "house1", s1))
        layer1.add(new Feature([new Point(111,-47), "House 2", 9.7], "house2", s1))
        layer1.add(new Feature([new Point(111,-47), "House 3", 3.4], "house3", s1))
        // All Features
        int i = 0
        Cursor c = layer1.getCursor()
        while(c.hasNext()) {
            Feature f = c.next()
            assertNotNull f
            assertNotNull f.geom
            i++
        }
        c.close()
        assertEquals layer1.count, i
        // Filtered with named parameter
        i = 0
        c = layer1.getCursor(filter: "price > 5")
        while(c.hasNext()) {
            Feature f = c.next()
            assertNotNull f
            assertNotNull f.geom
            i++
        }
        assertEquals layer1.count(filter: "price > 5"), i
    }

    @Test void toGML() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        def out = new java.io.ByteArrayOutputStream()
        layer1.toGML(out)
        String gml = out.toString()
        assertNotNull gml
    }

    @Test void toJSON() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        // OutputStream
        def out = new java.io.ByteArrayOutputStream()
        layer1.toJSON(out)
        String json = out.toString()
        assertNotNull json
        assertTrue json.startsWith("{\"type\":\"FeatureCollection\"")
        // File
        File file = new File(folder,"layer.json")
        layer1.toJSONFile(file)
        json = file.text
        assertNotNull json
        assertTrue json.startsWith("{\"type\":\"FeatureCollection\"")
        // String
        json = layer1.toJSONString()
        assertNotNull json
        assertTrue json.startsWith("{\"type\":\"FeatureCollection\"")
    }

    @Test void toJSONWithOptions() {
        Schema schema = new Schema("facilities", [new Field("geom","Point", "EPSG:4326"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", schema)
        layer.add(new Feature([new Point(111.123456,-47.123456), "House", 12.5], "house1", schema))
        layer.add(new Feature([new Point(121.123456,-45.123456), "School", 22.7], "house2", schema))
        // OutputStream
        def out = new java.io.ByteArrayOutputStream()
        layer.toJSON(out, decimals: 6, encodeFeatureBounds: true, encodeFeatureCRS: true,
                encodeFeatureCollectionBounds: true, encodeFeatureCollectionCRS: true)
        String json = out.toString()
        assertNotNull json
        checkJson(json)
        // File
        File file = new File(folder,"layer.json")
        layer.toJSONFile(file, decimals: 6, encodeFeatureBounds: true, encodeFeatureCRS: true,
                encodeFeatureCollectionBounds: true, encodeFeatureCollectionCRS: true)
        json = file.text
        checkJson(json)
        // String
        json = layer.toJSONString(decimals: 6, encodeFeatureBounds: true, encodeFeatureCRS: true,
                encodeFeatureCollectionBounds: true, encodeFeatureCollectionCRS: true)
        checkJson(json)
    }

    private void checkJson(String geojson) {
        JsonSlurper slurper = new JsonSlurper()
        Map json = slurper.parseText(geojson)
        assertEquals("FeatureCollection", json.type)
        assertTrue json.containsKey("bbox")
        assertTrue json.containsKey("crs")
        assertTrue json.containsKey("features")
        List features = json.features
        assertEquals(2, features.size())
        features.each { Map feature ->
            assertEquals("Feature", feature.type)
            assertTrue feature.containsKey("bbox")
            assertTrue feature.containsKey("crs")
            assertTrue feature.containsKey("geometry")
            assertTrue feature.containsKey("properties")
        }
    }

    @Test void toGeobuf() {
        Schema schema = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", schema)
        layer.add(new Feature([new Point(111,-47), "House", 12.5], "house1", schema))

        // OutputStream
        def out = new java.io.ByteArrayOutputStream()
        layer.toGeobuf(out)
        assertTrue(out.toByteArray().length > 0)
        // Bytes
        assertTrue(layer.toGeobufBytes().length > 0)
        // File
        File file = new File(folder,"test.pbf")
        layer.toGeobufFile(file)
        file.withInputStream {InputStream inputStream ->
            assertTrue(inputStream.bytes.length > 0)
        }
        // String
        String hex = layer.toGeobufString()
        assertNotNull hex
    }

    @Test void toKML() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(-122.444,47.2528), "House", 12.5], "house1", s1))
        def out = new java.io.ByteArrayOutputStream()
        layer1.toKML(out, {f->f.get("name")}, {f-> "${f.get('name')} ${f.get('price')}"})
        String kml = out.toString()
        String expected = """<?xml version="1.0" encoding="UTF-8"?><kml:kml xmlns:kml="http://www.opengis.net/kml/2.2">
  <kml:Document>
    <kml:Folder>
      <kml:name>facilities</kml:name>
      <kml:Schema kml:name="facilities" kml:id="facilities">
        <kml:SimpleField kml:name="name" kml:type="String"/>
        <kml:SimpleField kml:name="price" kml:type="Float"/>
      </kml:Schema>
      <kml:Placemark>
        <kml:name>House</kml:name>
        <kml:description>House 12.5</kml:description>
        <kml:Style>
          <kml:IconStyle>
            <kml:color>ff0000ff</kml:color>
          </kml:IconStyle>
        </kml:Style>
        <kml:ExtendedData>
          <kml:SchemaData kml:schemaUrl="#facilities">
            <kml:SimpleData kml:name="name">House</kml:SimpleData>
            <kml:SimpleData kml:name="price">12.5</kml:SimpleData>
          </kml:SchemaData>
        </kml:ExtendedData>
        <kml:Point>
          <kml:coordinates>-122.444,47.2528</kml:coordinates>
        </kml:Point>
      </kml:Placemark>
    </kml:Folder>
  </kml:Document>
</kml:kml>
"""
        AssertUtil.assertStringsEqual(expected.trim(), kml.trim(), trim: true)
    }

    @Test void toYaml() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(-122.444,47.2528), "House", 12.5], "house1", s1))

        String expected = """---
type: FeatureCollection
features:
- properties:
    name: House
    price: 12.5
  geometry:
    type: Point
    coordinates:
    - -122.444
    - 47.2528
"""
        // OutputStream
        def out = new java.io.ByteArrayOutputStream()
        layer1.toYaml(out)
        String yaml = out.toString()
        assertEquals(expected, yaml)

        // String
        yaml = layer1.toYamlString()
        assertEquals(expected, yaml)

        // File
        File file = new File(folder,"test.yaml")
        layer1.toYamlFile(file)
        assertEquals(expected, file.text)
    }

    @Test void reproject() {
        // With Layer Projection
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:4326"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(-122.494165, 47.198096), "House", 12.5], "house1", s1))
        Layer layer2 = layer1.reproject(new Projection("EPSG:2927"))
        assertEquals 1, layer2.count()
        assertEquals 1144727.44, layer2.features[0].geom.x, 0.01
        assertEquals 686301.31, layer2.features[0].geom.y, 0.01

        // Without Layer Projection
        s1 = new Schema("facilities", [new Field("geom","Point"), new Field("name","string"), new Field("price","float")])
        layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(-122.494165, 47.198096), "House", 12.5], "house1", s1))
        layer2 = layer1.reproject(new Projection("EPSG:2927"), "projected_facilties", 1000, new Projection("EPSG:4326"))
        assertEquals 1, layer2.count()
        assertEquals 1144727.44, layer2.features[0].geom.x, 0.01
        assertEquals 686301.31, layer2.features[0].geom.y, 0.01
    }

    @Test void reprojectToWorkspace() {
        // Create Layer in Memory
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:4326"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(-122.494165, 47.198096), "House", 12.5], "house1", s1))

        // Reproject to a Property Workspace
        File file = new File(folder,"reproject.properties")
        Property property = new Property(file.parentFile)
        Layer layer2 = layer1.reproject(new Projection("EPSG:2927"), property, "facilities_reprojected")
        assertEquals 1, layer2.count()
        assertEquals 1144727.44, layer2.features[0].geom.x, 0.01
        assertEquals 686301.31, layer2.features[0].geom.y, 0.01
    }

    @Test void reprojectToLayer() {
        // Create Layer in Memory
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:4326"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(-122.494165, 47.198096), "House", 12.5], "house1", s1))

        // Reproject to a Property Workspace Layer
        File file = new File(folder,"reproject.properties")
        Property property = new Property(file.parentFile)
        Layer propertyLayer = property.create(layer1.schema.reproject("EPSG:2927","facilities_epsg_2927"))
        Layer layer2 = layer1.reproject(propertyLayer)
        assertEquals 1, layer2.count()
        assertEquals 1144727.44, layer2.features[0].geom.x, 0.01
        assertEquals 686301.31, layer2.features[0].geom.y, 0.01
    }

    @Test void delete() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        assertEquals 0, layer1.count()
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        assertEquals 1, layer1.count()
        layer1.delete()
        assertEquals 0, layer1.count()
    }

    @Test void filter() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        layer1.add(new Feature([new Point(111,-47), "House", 12.5], "house1", s1))
        layer1.add(new Feature([new Point(112,-48), "Work", 67.2], "house2", s1))
        Layer layer2 = layer1.filter()
        assertEquals 2, layer2.count()
    }


    @Test void constructors() {
        Schema s1 = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer1 = new Layer("facilities", s1)
        assertNotNull(layer1)
        assertEquals "Memory", layer1.format
        assertEquals "facilities", layer1.name
        assertTrue(layer1.style instanceof geoscript.style.Shape)

        Layer layer2 = new Layer()
        assertEquals 0, layer2.count()
        layer2.add([new Point(1,2)])
        layer2.add([new Point(3,4)])
        assertEquals 2, layer2.count()

        Layer layer3 = new Layer("points")
        layer3.add([new Point(0,0)])
        layer3.add([new Point(1,1)])
        layer3.add([new Point(2,2)])
        layer3.add([new Point(3,3)])
        layer3.add([new Point(4,4)])

        assertEquals 5, layer3.count
        assertEquals "(0.0,0.0,4.0,4.0)", layer3.bounds.toString()

        // Make sure that the namepsace uri is passed through when creating Layers from FeatureCollections
        URL url = getClass().getClassLoader().getResource("states.shp")
        Workspace workspace = new Workspace(["url": url, namespace: 'http://omar.ossim.org'])
        Layer layer4 = workspace["states"]
        assertEquals layer4.schema.uri, 'http://omar.ossim.org'
        Layer layer5 = new Layer(layer4.fs.features)
        assertEquals layer5.schema.uri, 'http://omar.ossim.org'

        // Make sure Layers without geometry don't through an Exception
        Schema s2 = new Schema("facilities", [new Field("name","string"), new Field("price","float")])
        Layer layer6 = new Layer("facilities", s2)

    }

    @Test void updateFeatures() {

        // Create a Layer in memory
        Memory mem = new Memory()
        Layer l = mem.create('coffee_stands',[new Field("geom", "Point"), new Field("name", "String")])
        assertNotNull(l)

        // Add some Features
        l.add([new Point(1,1), "Hot Java"])
        l.add([new Point(2,2), "Cup Of Joe"])
        l.add([new Point(3,3), "Hot Wire"])

        // Make sure they are there and the attributes are equal
        assertEquals 3, l.count()
        List<Feature> features = l.features
        assertEquals features[0].get("geom").wkt, "POINT (1 1)"
        assertEquals features[1].get("geom").wkt, "POINT (2 2)"
        assertEquals features[2].get("geom").wkt, "POINT (3 3)"
        assertEquals features[0].get("name"), "Hot Java"
        assertEquals features[1].get("name"), "Cup Of Joe"
        assertEquals features[2].get("name"), "Hot Wire"

        // Now do some updating
        features[0].set("geom", new Point(5,5))
        features[1].set("name", "Coffee")
        features[2].set("geom", new Point(6,6))
        features[2].set("name", "Hot Coffee")
        l.update()

        // Ok, now do some more checking
        features = l.features
        assertEquals features[0].get("geom").wkt, "POINT (5 5)"
        assertEquals features[1].get("geom").wkt, "POINT (2 2)"
        assertEquals features[2].get("geom").wkt, "POINT (6 6)"
        assertEquals features[0].get("name"), "Hot Java"
        assertEquals features[1].get("name"), "Coffee"
        assertEquals features[2].get("name"), "Hot Coffee"
    }

    @Test void update() {
        // Create a Layer
        Schema s = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", s)
        layer.add(new Feature([new Point(111,-47), "House 1", 12.5], "house1", s))
        layer.add(new Feature([new Point(112,-46), "House 2", 13.5], "house2", s))
        layer.add(new Feature([new Point(113,-45), "House 3", 14.5], "house3", s))
        assertEquals 3, layer.count

        // Test original values
        def features = layer.features
        assertEquals "House 1", features[0].get('name')
        assertEquals "House 2", features[1].get('name')
        assertEquals "House 3", features[2].get('name')

        // Update with static value
        layer.update(s.get('name'), 'Building')
        features = layer.features
        assertEquals "Building", features[0].get('name')
        assertEquals "Building", features[1].get('name')
        assertEquals "Building", features[2].get('name')

        // Update static value with Filter
        layer.update(s.get('name'), 'Building 1', new Filter('price = 12.5'))
        layer.update(s.get('name'), 'Building 2', new Filter('price = 13.5'))
        layer.update(s.get('name'), 'Building 3', new Filter('price = 14.5'))
        features = layer.features
        assertEquals "Building 1", features[0].get('name')
        assertEquals "Building 2", features[1].get('name')
        assertEquals "Building 3", features[2].get('name')

        // Update with closure
        layer.update(s.get('price'), {f ->
            f.get('price') * 2
        })
        features = layer.features
        assertEquals 12.5 * 2, features[0].get('price'), 0.01
        assertEquals 13.5 * 2, features[1].get('price'), 0.01
        assertEquals 14.5 * 2, features[2].get('price'), 0.01
        assertEquals 3, layer.count

        // Update with script
        layer.update(s.get('name'), "return c + '). ' + f.get('name')", Filter.PASS, true)
        features = layer.features
        assertEquals "0). Building 1", features[0].get('name')
        assertEquals "1). Building 2", features[1].get('name')
        assertEquals "2). Building 3", features[2].get('name')
        assertEquals 3, layer.count

        // Update with an Expression
        layer.update(s.get("price"), Expression.fromCQL("price * 2"))
        features = layer.features
        // We already multiplied the price * 2 with a Closure
        assertEquals 12.5 * 4, features[0].get('price'), 0.01
        assertEquals 13.5 * 4, features[1].get('price'), 0.01
        assertEquals 14.5 * 4, features[2].get('price'), 0.01
        assertEquals 3, layer.count
    }

    @Test void minmax() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shp = new Shapefile(file)

        // No high/low
        def minMax = shp.minmax("SAMP_POP")
        assertEquals 72696.0, minMax.min, 0.1
        assertEquals 3792553.0, minMax.max, 0.1

        // low
        minMax = shp.minmax("SAMP_POP", 80000)
        assertEquals 83202.0, minMax.min, 0.1
        assertEquals 3792553.0, minMax.max, 0.1

        // high
        minMax = shp.minmax("SAMP_POP", null, 3000000)
        assertEquals 72696.0, minMax.min, 0.1
        assertEquals 2564485.0, minMax.max, 0.1

        // high and low
        minMax = shp.minmax("SAMP_POP", 80000, 3000000)
        assertEquals 83202.0, minMax.min, 0.1
        assertEquals 2564485.0, minMax.max, 0.1
    }

    @Test void histogram() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shp = new Shapefile(file)
        def h = shp.histogram("SAMP_POP")
        assertEquals 10, h.size()
        assertEquals 72696.0, h[0][0], 0.1
        assertEquals 3792553.0, h[h.size() - 1][1], 0.1
    }

    @Test void interpolate() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shp = new Shapefile(file)

        // Default: classes = 10 and method = linear
        def values = shp.interpolate("SAMP_POP")
        assertEquals 11, values.size()
        assertEquals 72696.0, values[0], 0.1
        assertEquals 1932624.5, values[5], 0.1
        assertEquals 3792553.0, values[values.size() - 1], 0.1

        // exp
        values = shp.interpolate("SAMP_POP", 8, "exp")
        assertEquals 9, values.size()
        assertEquals 72696.0, values[0], 0.1
        assertEquals 74623.69, values[4], 0.1
        assertEquals 3792553.0, values[values.size() - 1], 0.1

        // log
        values = shp.interpolate("SAMP_POP", 12, "log")
        assertEquals 13, values.size()
        assertEquals 72696.0, values[0], 0.1
        assertEquals 2248672.85, values[6], 0.1
        assertEquals 3792553.0, values[values.size() - 1], 0.1
    }

    @Test void cursorSorting() {
        File f = new File("target/h2").absoluteFile
        if (f.exists()) {
            boolean deleted = f.deleteDir()
        }
        H2 h2 = new H2("facilities", "target/h2")
        Layer layer = h2.create('facilities',[new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        layer.add(new Feature(["geom": new Point(111,-47), "name": "A", "price": 10], "house1"))
        layer.add(new Feature(["geom": new Point(112,-46), "name": "B", "price": 12], "house2"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "C", "price": 13], "house3"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "D", "price": 14], "house4"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "E", "price": 15], "house5"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "F", "price": 16], "house6"))

        Cursor c = layer.getCursor(Filter.PASS, [["name","ASC"]])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertEquals "E", c.next()["name"]
        assertEquals "F", c.next()["name"]
        c.close()

        c = layer.getCursor(Filter.PASS, ["name"])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertEquals "E", c.next()["name"]
        assertEquals "F", c.next()["name"]
        c.close()

        c = layer.getCursor(Filter.PASS, [["name","DESC"]])
        assertEquals "F", c.next()["name"]
        assertEquals "E", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "A", c.next()["name"]
        c.close()

        c = layer.getCursor(Filter.PASS, ["name ASC"])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertEquals "E", c.next()["name"]
        assertEquals "F", c.next()["name"]
        c.close()

        // Named Parameters
        c = layer.getCursor(filter: "price >= 14.0", sort: [["price", "DESC"]])
        assertTrue c.hasNext()
        assertEquals "F", c.next()["name"]
        assertEquals "E", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        h2.close()
    }

    @Test void cursorSortingAndPagingWithUnsupportedLayer() {
        Schema s = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", s)
        layer.add(new Feature([new Point(111,-47), "A", 10], "house1", s))
        layer.add(new Feature([new Point(112,-46), "B", 12], "house2", s))
        layer.add(new Feature([new Point(113,-45), "C", 11], "house3", s))
        layer.add(new Feature([new Point(113,-44), "D", 15], "house4", s))

        // Sort ascending explicitly
        Cursor c = layer.getCursor(Filter.PASS, [["name","ASC"]])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        // Sort ascending implicitly
        c = layer.getCursor(Filter.PASS, ["name"])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        // Sort descending
        c = layer.getCursor(Filter.PASS, [["name","DESC"]])
        assertEquals "D", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "A", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        // Page
        c = layer.getCursor(start:0, max:2, sort: [["name", "ASC"]])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertFalse c.hasNext()
        c.close()
        c = layer.getCursor(start:2, max:2, sort: [["name", "ASC"]])
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertFalse c.hasNext()
        c.close()
        c = layer.getCursor("price > 10", [["price", "DESC"]], 2, 1, [])
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertFalse c.hasNext()
        c.close()
    }

    @Test void cursorPaging() {
        File f = new File("target/h2").absoluteFile
        if (f.exists()) {
            boolean deleted = f.deleteDir()
        }
        H2 h2 = new H2("facilities", "target/h2")
        Layer layer = h2.create('facilities',[new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        layer.add(new Feature(["geom": new Point(111,-47), "name": "A", "price": 10], "house1"))
        layer.add(new Feature(["geom": new Point(112,-46), "name": "B", "price": 12], "house2"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "C", "price": 13], "house3"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "D", "price": 14], "house4"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "E", "price": 15], "house5"))
        layer.add(new Feature(["geom": new Point(113,-45), "name": "F", "price": 16], "house6"))

        Cursor c = layer.getCursor(Filter.PASS, [["name","ASC"]], 2, 0, [])
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        c = layer.getCursor(Filter.PASS, [["name","ASC"]], 2, 2, [])
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        c = layer.getCursor(Filter.PASS, [["name","ASC"]], 2, 4, [])
        assertEquals "E", c.next()["name"]
        assertEquals "F", c.next()["name"]
        assertFalse c.hasNext()
        c.close()

        // Named parameters
        c = layer.getCursor(start: 0, max: 4)
        assertEquals "A", c.next()["name"]
        assertEquals "B", c.next()["name"]
        assertEquals "C", c.next()["name"]
        assertEquals "D", c.next()["name"]
        c.close()

        h2.close()
    }

    @Test void cursorProjecting() {
        // With source Projection from Layer
        Schema s = new Schema("points", [new Field("geom","Point", "EPSG:4326")])
        Layer layer = new Layer("points", s)
        layer.add(new Feature([new Point(-122.316261, 47.084539)], "p1", s))
        layer.add(new Feature([new Point(-122.253802, 46.997483)], "p2", s))

        Cursor c = layer.getCursor(destProj: "EPSG:2927")
        Point pt = c.next().geom as Point
        assertEquals 1187987.19, pt.x, 0.01
        assertEquals 643826.70, pt.y, 0.01
        pt = c.next().geom as Point
        assertEquals 1202837.17, pt.x, 0.01
        assertEquals 611731.73, pt.y, 0.01
        assertFalse c.hasNext()
        c.close()

        // Without source projection from layer
        s = new Schema("points", [new Field("geom","Point")])
        layer = new Layer("points", s)
        layer.add(new Feature([new Point(-122.316261, 47.084539)], "p1", s))
        layer.add(new Feature([new Point(-122.253802, 46.997483)], "p2", s))

        c = layer.getCursor(sourceProj: "EPSG:4326", destProj: "EPSG:2927")
        pt = c.next().geom as Point
        assertEquals 1187987.19, pt.x, 0.01
        assertEquals 643826.70, pt.y, 0.01
        pt = c.next().geom as Point
        assertEquals 1202837.17, pt.x, 0.01
        assertEquals 611731.73, pt.y, 0.01
        assertFalse c.hasNext()
        c.close()
    }

    @Test void getRaster() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shp = new Shapefile(file)
        Raster raster = shp.getRaster("SAMP_POP", [800,600], shp.bounds, "SAMP_POP")
        assertNotNull raster
        File rasterFile = new File(folder,"states_pop.tif")
        GeoTIFF geotiff = new GeoTIFF(rasterFile)
        geotiff.write(raster)
    }

    @Test void cursorSubFields() {
        Schema s = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", s)
        layer.add(new Feature([new Point(111,-47), "House 1", 12.5], "house1", s))
        layer.add(new Feature([new Point(112,-46), "House 2", 13.5], "house2", s))
        layer.add(new Feature([new Point(113,-45), "House 3", 14.5], "house3", s))

        layer.getCursor([fields: ["name"]]).each { f ->
            assertNotNull f["name"]
            assertNull f["price"]
        }
    }

    @Test void first() {
        Schema s = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", s)
        layer.add(new Feature([new Point(111,-47), "House 1", 12.5], "house1", s))
        layer.add(new Feature([new Point(112,-46), "House 2", 13.5], "house2", s))
        layer.add(new Feature([new Point(113,-45), "House 3", 14.5], "house3", s))

        assertEquals "House 3", layer.first(sort: "price DESC").get("name")
        assertEquals "House 1", layer.first(sort: "price ASC").get("name")
        assertEquals "House 2", layer.first(filter: "price > 13 AND price < 14").get("name")
        assertEquals "House 3", layer.first(filter: "price > 13 AND price < 15", sort: "price DESC").get("name")
        assertNull layer.first(filter: "price < 10")
    }

    @Test void transform() {
        Schema s = new Schema("facilities", [new Field("geom","Point", "EPSG:2927"), new Field("name","string"), new Field("price","float")])
        Layer layer = new Layer("facilities", s)
        layer.add(new Feature([new Point(111,-47), "House 1", 12.5], "house1", s))
        layer.add(new Feature([new Point(112,-46), "House 2", 13.5], "house2", s))
        layer.add(new Feature([new Point(113,-45), "House 3", 14.5], "house3", s))

        Layer layer2 = layer.transform("buffered_facilities", [
            geom: "buffer(geom, 10)",
            name: "strToUpperCase(name)",
            price: "price * 10"
        ])

        List features1 = layer.features
        List features2 = layer2.features
        // 1
        assertTrue(features2[0].geom instanceof Polygon)
        assertEquals(features1[0].get("name").toString().toUpperCase(),  features2[0].get("name"))
        assertEquals(features1[0].get("price") * 10,  features2[0].get("price"), 0.1)
        // 2
        assertTrue(features2[1].geom instanceof Polygon)
        assertEquals(features1[1].get("name").toString().toUpperCase(),  features2[1].get("name"))
        assertEquals(features1[1].get("price") * 10,  features2[1].get("price"), 0.1)
        // 3
        assertTrue(features2[2].geom instanceof Polygon)
        assertEquals(features1[2].get("name").toString().toUpperCase(),  features2[2].get("name"))
        assertEquals(features1[2].get("price") * 10,  features2[2].get("price"), 0.1)
    }

    @Test void getWriter() {
        int numPoints = 100
        Geometry geom = new Bounds(0,0,50,50).geometry
        List pts = geom.createRandomPoints(geom, numPoints).points
        Workspace w = new Directory(FileUtil.createDir(folder, "points"))
        Schema s = new Schema("points", [new Field("the_geom","Point", "EPSG:4326"), new Field("id","int")])
        Layer layer = w.create(s)
        Writer writer = layer.getWriter(autoCommit: false, batch: 75)
        try {
            pts.eachWithIndex{Point pt, int i ->
                writer.add(s.feature([the_geom: pt, id: i], "point${i}"))
            }
        } finally {
            writer.close()
        }
        assertEquals numPoints, layer.count
    }

    @Test void withWriter() {
        int numPoints = 100
        Geometry geom = new Bounds(0,0,50,50).geometry
        List pts = geom.createRandomPoints(geom, numPoints).points
        Workspace w = new Directory(FileUtil.createDir(folder, "points"))
        Schema s = new Schema("points", [new Field("the_geom","Point", "EPSG:4326"), new Field("id","int")])
        Layer layer = w.create(s)
        layer.withWriter(batch: 45) {Writer writer ->
            pts.eachWithIndex{Point pt, int i ->
                writer.add(s.feature([the_geom: pt, id: i], "point${i}"))
            }
        }
        assertEquals numPoints, layer.count
    }

    /**
     * Create two Layers for Layer Algebra testing based on the GDAL spec:
     * http://trac.osgeo.org/gdal/wiki/LayerAlgebra
     * @return A List of two Layers
     */
    private List createGdalLayerAlgebraTestLayers() {
        def dir = new Memory()
        Layer layer1 = dir.create(new Schema("a",[new Field("the_geom", "Polygon"), new Field("A","int")]))
        Bounds b1 = new Bounds(90, 100, 100, 110)
        Bounds b2 = new Bounds(120, 100, 130, 110)
        layer1.add([the_geom: b1.geometry, A: 1])
        layer1.add([the_geom: b2.geometry, A: 2])

        Layer layer2 = dir.create(new Schema("b",[new Field("the_geom", "Polygon"), new Field("B","int")]))
        Bounds b3 = new Bounds(85, 95, 95, 105)
        Bounds b4 = new Bounds(97, 95, 125, 105)
        layer2.add([the_geom: b3.geometry, B: 3])
        layer2.add([the_geom: b4.geometry, B: 4])

        [layer1,layer2]
    }

    @Test void intersectionGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].intersection(layers[1])
        // Check schema
        assertEquals "a_b_intersection", layer.name
        assertTrue layer.schema.has("A")
        assertTrue layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 3, layer.count
        assertEquals 1, layer.count("A = 1 AND B = 3")
        assertEquals 1, layer.count("A = 1 AND B = 4")
        assertEquals 1, layer.count("A = 2 AND B = 4")
        assertEquals "POLYGON ((90 100, 90 105, 95 105, 95 100, 90 100))", layer.getFeatures("A = 1 AND B = 3")[0].geom.wkt
        assertEquals "POLYGON ((100 105, 100 100, 97 100, 97 105, 100 105))", layer.getFeatures("A = 1 AND B = 4")[0].geom.wkt
        assertEquals "POLYGON ((120 100, 120 105, 125 105, 125 100, 120 100))", layer.getFeatures("A = 2 AND B = 4")[0].geom.wkt
    }

    @Test void unionGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].union(layers[1])
        // Check schema
        assertEquals "a_b_union", layer.name
        assertTrue layer.schema.has("A")
        assertTrue layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 7, layer.count
        assertEquals 1, layer.count("A = 1 AND B = 3")
        assertEquals 1, layer.count("A = 1 AND B = 4")
        assertEquals 1, layer.count("A = 1 AND B IS NULL")
        assertEquals 1, layer.count("A = 2 AND B = 4")
        assertEquals 1, layer.count("A = 2 AND B IS NULL")
        assertEquals 1, layer.count("A IS NULL AND B = 3")
        assertEquals 1, layer.count("A IS NULL AND B = 4")
        assertEquals "POLYGON ((90 100, 90 105, 95 105, 95 100, 90 100))", layer.getFeatures("A = 1 AND B = 3")[0].geom.wkt
        assertEquals "POLYGON ((100 105, 100 100, 97 100, 97 105, 100 105))", layer.getFeatures("A = 1 AND B = 4")[0].geom.wkt
        assertEquals "POLYGON ((90 105, 90 110, 100 110, 100 105, 97 105, 97 100, 95 100, 95 105, 90 105))", layer.getFeatures("A = 1 AND B IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((120 100, 120 105, 125 105, 125 100, 120 100))", layer.getFeatures("A = 2 AND B = 4")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 120 110, 130 110, 130 100, 125 100, 125 105, 120 105))", layer.getFeatures("A = 2 AND B IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((85 95, 85 105, 90 105, 90 100, 95 100, 95 95, 85 95))", layer.getFeatures("A IS NULL AND B = 3")[0].geom.wkt
        assertEquals "POLYGON ((97 95, 97 100, 100 100, 100 105, 120 105, 120 100, 125 100, 125 95, 97 95))", layer.getFeatures("A IS NULL AND B = 4")[0].geom.wkt
    }

    @Test void symDifferenceGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].symDifference(layers[1])
        // Check schema
        assertEquals "a_b_symdifference", layer.name
        assertTrue layer.schema.has("A")
        assertTrue layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 4, layer.count
        assertEquals 1, layer.count("A = 1 AND B IS NULL")
        assertEquals 1, layer.count("A = 2 AND B IS NULL")
        assertEquals 1, layer.count("A IS NULL AND B = 3")
        assertEquals 1, layer.count("A IS NULL AND B = 4")
        assertEquals "POLYGON ((90 105, 90 110, 100 110, 100 105, 97 105, 97 100, 95 100, 95 105, 90 105))",layer.getFeatures("A = 1 AND B IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 120 110, 130 110, 130 100, 125 100, 125 105, 120 105))", layer.getFeatures("A = 2 AND B IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((85 95, 85 105, 90 105, 90 100, 95 100, 95 95, 85 95))", layer.getFeatures("A IS NULL AND B = 3")[0].geom.wkt
        assertEquals "POLYGON ((97 95, 97 100, 100 100, 100 105, 120 105, 120 100, 125 100, 125 95, 97 95))", layer.getFeatures("A IS NULL AND B = 4")[0].geom.wkt
    }

    @Test void identityGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].identity(layers[1])
        // Check schema
        assertEquals "a_b_identity", layer.name
        assertTrue layer.schema.has("A")
        assertTrue layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 5, layer.count
        assertEquals 1, layer.count("A = 1 AND B = 3")
        assertEquals 1, layer.count("A = 1 AND B = 4")
        assertEquals 1, layer.count("A = 1 AND B IS NULL")
        assertEquals 1, layer.count("A = 2 AND B = 4")
        assertEquals 1, layer.count("A = 2 AND B IS NULL")
        assertEquals "POLYGON ((90 100, 90 105, 95 105, 95 100, 90 100))", layer.getFeatures("A = 1 AND B = 3")[0].geom.wkt
        assertEquals "POLYGON ((100 105, 100 100, 97 100, 97 105, 100 105))", layer.getFeatures("A = 1 AND B = 4")[0].geom.wkt
        assertEquals "POLYGON ((90 105, 90 110, 100 110, 100 105, 97 105, 97 100, 95 100, 95 105, 90 105))", layer.getFeatures("A = 1 AND B IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((120 100, 120 105, 125 105, 125 100, 120 100))", layer.getFeatures("A = 2 AND B = 4")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 120 110, 130 110, 130 100, 125 100, 125 105, 120 105))", layer.getFeatures("A = 2 AND B IS NULL")[0].geom.wkt
    }

    @Test void identityInverseGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[1].identity(layers[0])
        // Check schema
        assertEquals "b_a_identity", layer.name
        assertTrue layer.schema.has("A")
        assertTrue layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 5, layer.count
        assertEquals 1, layer.count("B = 3 AND A IS NULL")
        assertEquals 1, layer.count("B = 4 AND A IS NULL")
        assertEquals 1, layer.count("B = 3 AND A = 1")
        assertEquals 1, layer.count("B = 4 AND A = 1")
        assertEquals 1, layer.count("B = 4 AND A = 2")
        assertEquals "POLYGON ((85 95, 85 105, 90 105, 90 100, 95 100, 95 95, 85 95))", layer.getFeatures("B = 3 AND A IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((97 95, 97 100, 100 100, 100 105, 120 105, 120 100, 125 100, 125 95, 97 95))", layer.getFeatures("B = 4 AND A IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((90 105, 95 105, 95 100, 90 100, 90 105))", layer.getFeatures("B = 3 AND A = 1")[0].geom.wkt
        assertEquals "POLYGON ((97 100, 97 105, 100 105, 100 100, 97 100))", layer.getFeatures("B = 4 AND A = 1")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 125 105, 125 100, 120 100, 120 105))", layer.getFeatures("B = 4 AND A = 2")[0].geom.wkt
    }

    @Test void updateGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].update(layers[1])
        // Check schema
        assertEquals "a_b_update", layer.name
        assertTrue layer.schema.has("A")
        assertFalse layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 4, layer.count
        assertEquals 1, layer.count("A = 1")
        assertEquals 1, layer.count("A = 2")
        assertEquals 2, layer.count("A IS NULL")
        assertEquals "POLYGON ((90 105, 90 110, 100 110, 100 105, 97 105, 97 100, 95 100, 95 105, 90 105))", layer.getFeatures("A = 1")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 120 110, 130 110, 130 100, 125 100, 125 105, 120 105))", layer.getFeatures("A = 2")[0].geom.wkt
        assertEquals "POLYGON ((85 95, 85 105, 95 105, 95 95, 85 95))", layer.getFeatures("A IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((97 95, 97 105, 125 105, 125 95, 97 95))", layer.getFeatures("A IS NULL")[1].geom.wkt
    }

    @Test void updateInverseGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[1].update(layers[0])
        // Check schema
        assertEquals "b_a_update", layer.name
        assertTrue layer.schema.has("B")
        assertFalse layer.schema.has("A")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 4, layer.count
        assertEquals 1, layer.count("B = 3")
        assertEquals 1, layer.count("B = 4")
        assertEquals 2, layer.count("B IS NULL")
        assertEquals "POLYGON ((85 95, 85 105, 90 105, 90 100, 95 100, 95 95, 85 95))", layer.getFeatures("B = 3")[0].geom.wkt
        assertEquals "POLYGON ((97 95, 97 100, 100 100, 100 105, 120 105, 120 100, 125 100, 125 95, 97 95))", layer.getFeatures("B = 4")[0].geom.wkt
        assertEquals "POLYGON ((120 100, 120 110, 130 110, 130 100, 120 100))", layer.getFeatures("B IS NULL")[0].geom.wkt
        assertEquals "POLYGON ((90 100, 90 110, 100 110, 100 100, 90 100))", layer.getFeatures("B IS NULL")[1].geom.wkt
    }

    @Test void clipGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].clip(layers[1])
        // Check schema
        assertEquals "a_b_clipped", layer.name
        assertTrue layer.schema.has("A")
        assertFalse layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 3, layer.count
        assertEquals 2, layer.count("A = 1")
        assertEquals 1, layer.count("A = 2")
        assertEquals "POLYGON ((90 100, 90 105, 95 105, 95 100, 90 100))", layer.getFeatures("A = 1")[0].geom.wkt
        assertEquals "POLYGON ((100 105, 100 100, 97 100, 97 105, 100 105))", layer.getFeatures("A = 1")[1].geom.wkt
        assertEquals "POLYGON ((120 100, 120 105, 125 105, 125 100, 120 100))", layer.getFeatures("A = 2")[0].geom.wkt
    }

    @Test void clipInverseGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[1].clip(layers[0])
        // Check schema
        assertEquals "b_a_clipped", layer.name
        assertTrue layer.schema.has("B")
        assertFalse layer.schema.has("A")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 3, layer.count
        assertEquals 2, layer.count("B = 4")
        assertEquals 1, layer.count("B = 3")
        assertEquals "POLYGON ((97 100, 97 105, 100 105, 100 100, 97 100))", layer.getFeatures("B = 4")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 125 105, 125 100, 120 100, 120 105))", layer.getFeatures("B = 4")[1].geom.wkt
        assertEquals "POLYGON ((90 105, 95 105, 95 100, 90 100, 90 105))", layer.getFeatures("B = 3")[0].geom.wkt
    }

    @Test void eraseGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[0].erase(layers[1])
        // Check schema
        assertEquals "a_b_erase", layer.name
        assertTrue layer.schema.has("A")
        assertFalse layer.schema.has("B")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 2, layer.count
        assertEquals 1, layer.count("A = 1")
        assertEquals 1, layer.count("A = 2")
        assertEquals "POLYGON ((90 105, 90 110, 100 110, 100 105, 97 105, 97 100, 95 100, 95 105, 90 105))", layer.getFeatures("A = 1")[0].geom.wkt
        assertEquals "POLYGON ((120 105, 120 110, 130 110, 130 100, 125 100, 125 105, 120 105))", layer.getFeatures("A = 2")[0].geom.wkt
    }

    @Test void eraseInverseGdal() {
        List layers = createGdalLayerAlgebraTestLayers()
        Layer layer = layers[1].erase(layers[0])
        // Check schema
        assertEquals "b_a_erase", layer.name
        assertTrue layer.schema.has("B")
        assertFalse layer.schema.has("A")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 2, layer.count
        assertEquals 1, layer.count("B = 3")
        assertEquals 1, layer.count("B = 4")
        assertEquals "POLYGON ((85 95, 85 105, 90 105, 90 100, 95 100, 95 95, 85 95))", layer.getFeatures("B = 3")[0].geom.wkt
        assertEquals "POLYGON ((97 95, 97 100, 100 100, 100 105, 120 105, 120 100, 125 100, 125 95, 97 95))", layer.getFeatures("B = 4")[0].geom.wkt
    }

    /**
     * Create two Layers for Layer Algebra testing based on the UW examples:
     * http://courses.washington.edu/gis250/lessons/Model_Builder/
     * @return A List of two Layers
     */
    private List createUWLayerAlgebraTestLayers() {
        def dir = new Memory()
        Layer rings = dir.create(new Schema("rings",[new Field("the_geom", "Polygon"), new Field("name","int")]))
        Point pt = new Point(100,100)
        Polygon poly1 = pt.buffer(12)
        Polygon poly2 = pt.buffer(15).difference(poly1)
        rings.add([the_geom: poly1, name: 1])
        rings.add([the_geom: poly2, name: 2])

        Layer boxes = dir.create(new Schema("boxes",[new Field("the_geom", "Polygon"), new Field("name","String")]))
        Bounds bounds = new Bounds(80.0, 90.0, 120.0, 110.0)
        Geometry grid = bounds.getGrid(2,2)
        boxes.add([the_geom: grid[1], name: "A"])
        boxes.add([the_geom: grid[3], name: "B"])
        boxes.add([the_geom: grid[0], name: "C"])
        boxes.add([the_geom: grid[2], name: "D"])

        [rings,boxes]
    }

    @Test void intersectionUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].intersection(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_intersection", layer.name
        assertTrue layer.schema.has("name1")
        assertTrue layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check features
        assertEquals 21, layer.count
        assertEquals 1, layer.count("name1 = 1 AND name2 = 'A'")
        assertEquals 4, layer.count("name1 = 1 AND name2 = 'C'")
        assertEquals 2, layer.count("name1 = 2 AND name2 = 'C'")
        assertEquals 8, layer.count("name1 = 1 AND name2 = 'D'")
        assertEquals 2, layer.count("name1 = 2 AND name2 = 'D'")
        assertEquals 1, layer.count("name1 = 2 AND name2 = 'A'")
        assertEquals 1, layer.count("name1 = 2 AND name2 = 'B'")
        assertEquals 2, layer.count("name1 = 1 AND name2 = 'B'")
    }

    @Test void unionUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].union(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_union", layer.name
        assertTrue layer.schema.has("name1")
        assertTrue layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 23, layer.count
        assertEquals 2, layer.count("name1 = 2 AND name2 = 'C'")
        assertEquals 2, layer.count("name1 IS NULL AND name2 = 'C'")
        assertEquals 4, layer.count("name1 = 1 AND name2 IS NULL")
        assertEquals 4, layer.count("name1 = 1 AND name2 = 'D'")
        assertEquals 2, layer.count("name1 = 2 AND name2 = 'D'")
        assertEquals 3, layer.count("name1 = 2 AND name2 IS NULL")
        assertEquals 2, layer.count("name1 IS NULL AND name2 = 'D'")
        assertEquals 2, layer.count("name1 IS NULL AND name2 IS NULL")
        assertEquals 2, layer.count("name1 = 1 AND name2 = 'B'")
    }

    @Test void symDifferenceUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].symDifference(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_symdifference", layer.name
        assertTrue layer.schema.has("name1")
        assertTrue layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 6, layer.count
        assertEquals 1, layer.count("name1 IS NULL AND name2 = 'C'")
        assertEquals 1, layer.count("name1 = 1 AND name2 IS NULL")
        assertEquals 1, layer.count("name1 = 2 AND name2 IS NULL")
        assertEquals 1, layer.count("name1 IS NULL AND name2 = 'D'")
        assertEquals 2, layer.count("name1 IS NULL AND name2 IS NULL")
    }

    @Test void identityUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].identity(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_identity", layer.name
        assertTrue layer.schema.has("name1")
        assertTrue layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 23, layer.count
        assertEquals 8, layer.count("name1 = 1 AND name2 IS NULL")
        assertEquals 2, layer.count("name1 = 2 AND name2 = 'C'")
        assertEquals 8, layer.count("name1 = 1 AND name2 = 'D'")
        assertEquals 2, layer.count("name1 = 2 AND name2 = 'D'")
        assertEquals 3, layer.count("name1 = 2 AND name2 IS NULL")
    }

    @Test void identityInverseUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[1].identity(layers[0], postfixAll: true)
        // Check schema
        assertEquals "boxes_rings_identity", layer.name
        assertTrue layer.schema.has("name1")
        assertTrue layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 16, layer.count
        assertEquals 2, layer.count("name1 = 'A' AND name2 IS NULL")
        assertEquals 2, layer.count("name1 = 'C' AND name2 = 2")
        assertEquals 2, layer.count("name1 = 'C' AND name2 IS NULL")
        assertEquals 2, layer.count("name1 = 'D' AND name2 = 2")
        assertEquals 2, layer.count("name1 = 'D' AND name2 IS NULL")
        assertEquals 2, layer.count("name1 = 'A' AND name2 = 2")
        assertEquals 2, layer.count("name1 = 'B' AND name2 = 2")
        assertEquals 2, layer.count("name1 = 'B' AND name2 IS NULL")
    }

    @Test void updateUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].update(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_update", layer.name
        assertTrue layer.schema.has("name")
        assertFalse layer.schema.has("name1")
        assertFalse layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 6, layer.count
        assertEquals 4, layer.count("name IS NULL")
        assertEquals 1, layer.count("name = 1")
        assertEquals 1, layer.count("name = 2")
    }

    @Test void updateInverseUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[1].update(layers[0], postfixAll: true)
        // Check schema
        assertEquals "boxes_rings_update", layer.name
        assertTrue layer.schema.has("name")
        assertFalse layer.schema.has("name1")
        assertFalse layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 6, layer.count
        assertEquals 2, layer.count("name IS NULL")
        assertEquals 1, layer.count("name = 'A'")
        assertEquals 1, layer.count("name = 'B'")
        assertEquals 1, layer.count("name = 'C'")
        assertEquals 1, layer.count("name = 'D'")
    }

    @Test void clipUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].clip(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_clipped", layer.name
        assertTrue layer.schema.has("name")
        assertFalse layer.schema.has("name1")
        assertFalse layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 8, layer.count
        assertEquals 4, layer.count("name = 1")
        assertEquals 4, layer.count("name = 2")
    }

    @Test void clipInverseUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[1].clip(layers[0], postfixAll: true)
        // Check schema
        assertEquals "boxes_rings_clipped", layer.name
        assertTrue layer.schema.has("name")
        assertFalse layer.schema.has("name1")
        assertFalse layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 8, layer.count
        assertEquals 2, layer.count("name = 'A'")
        assertEquals 2, layer.count("name = 'B'")
        assertEquals 2, layer.count("name = 'C'")
        assertEquals 2, layer.count("name = 'D'")
    }

    @Test void eraseUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[0].erase(layers[1], postfixAll: true)
        // Check schema
        assertEquals "rings_boxes_erase", layer.name
        assertTrue layer.schema.has("name")
        assertFalse layer.schema.has("name1")
        assertFalse layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 2, layer.count
        assertEquals 1, layer.count("name = 1")
        assertEquals 1, layer.count("name = 2")
    }

    @Test void eraseInverseUW() {
        List layers = createUWLayerAlgebraTestLayers()
        Layer layer = layers[1].erase(layers[0], postfixAll: true)
        // Check schema
        assertEquals "boxes_rings_erase", layer.name
        assertTrue layer.schema.has("name")
        assertFalse layer.schema.has("name1")
        assertFalse layer.schema.has("name2")
        assertEquals "Polygon", layer.schema.geom.typ
        // Check Features
        assertEquals 4, layer.count
        assertEquals 1, layer.count("name = 'A'")
        assertEquals 1, layer.count("name = 'B'")
        assertEquals 1, layer.count("name = 'C'")
        assertEquals 1, layer.count("name = 'D'")
    }

    @Test void dissolveByField() {
        Schema schema = new Schema("grid",[
            new Field("geom","Polygon","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int")
        ])
        Layer layer = new Memory().create(schema)
        Bounds bounds = new Bounds(0,0,10,10)
        bounds.generateGrid(2, 2, "polygon", {cell, col, row ->
            layer.add([
                "geom": cell,
                "col": col,
                "row": row
            ])
        })

        Layer rowLayer = layer.dissolve(layer.schema.get("row"))
        assertEquals 2, rowLayer.count
        assertEquals "POLYGON ((0 0, 0 5, 5 5, 10 5, 10 0, 5 0, 0 0))", rowLayer.first(filter: "row = 1").geom.wkt
        assertEquals "POLYGON ((0 5, 0 10, 5 10, 10 10, 10 5, 5 5, 0 5))", rowLayer.first(filter: "row = 2").geom.wkt

        Layer colLayer = layer.dissolve(layer.schema.get("col"))
        assertEquals 2, colLayer.count
        assertEquals "POLYGON ((0 0, 0 5, 0 10, 5 10, 5 5, 5 0, 0 0))", colLayer.first(filter: "col = 1").geom.wkt
        assertEquals "POLYGON ((5 0, 5 5, 5 10, 10 10, 10 5, 10 0, 5 0))", colLayer.first(filter: "col = 2").geom.wkt
    }

    @Test void dissolveIntersecting() {
        Schema schema = new Schema("grid",[
            new Field("geom","Polygon","EPSG:4326")
        ])
        Layer layer = new Memory().create(schema)

        Bounds b = new Bounds(0,0,10,10)
        b.generateGrid(3, 3, "point", {cell, col, row ->
            layer.add([geom: cell.buffer(col * 1)])
        })

        Layer dissolved = layer.dissolve()
        assertEquals 4, dissolved.count
        assertEquals 1, dissolved.count("count = 6")
        assertEquals 3, dissolved.count("count = 1")
    }

    @Test void merge() {

        // Create the first Layer
        Schema schema1 = new Schema("grid1",[
            new Field("geom","Polygon","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int")
        ])
        Layer layer1 = new Memory().create(schema1)
        new Bounds(0,0,10,10).generateGrid(3, 3, "polygon", {cell, col, row ->
            layer1.add([
                "geom": cell,
                "col": col,
                "row": row
            ])
        })

        // Create the second Layer
        Schema schema2 = new Schema("grid2",[
            new Field("geom","Polygon","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int")
        ])
        Layer layer2 = new Memory().create(schema2)
        new Bounds(20,20,40,40).generateGrid(4, 4, "polygon", {cell, col, row ->
            layer2.add([
                "geom": cell,
                "col": col,
                "row": row
            ])
        })

        // Merge (include duplicate fields)
        Layer merged = layer1.merge(layer2)
        assertEquals 25, merged.count
        assertTrue merged.schema.has("geom")
        assertTrue merged.schema.has("col")
        assertTrue merged.schema.has("row")
        assertTrue merged.schema.has("col2")
        assertTrue merged.schema.has("row2")
        assertEquals 9, merged.count("col IS NOT NULL and row IS NOT NULL")
        assertEquals 9, merged.count("col2 IS NULL and row2 IS NULL")
        assertEquals 16, merged.count("col2 IS NOT NULL and row2 IS NOT NULL")
        assertEquals 16, merged.count("col IS NULL and row IS NULL")
        merged.eachFeature{f ->
            assertNotNull f.geom
            assertTrue f.geom instanceof Geometry
        }

        // Merge (don't include duplicate fields but set values if present)
        merged = layer1.merge(layer2, includeDuplicates: false)
        assertEquals 25, merged.count
        assertTrue merged.schema.has("geom")
        assertTrue merged.schema.has("col")
        assertTrue merged.schema.has("row")
        assertFalse merged.schema.has("col2")
        assertFalse merged.schema.has("row2")
        assertEquals 25, merged.count("col IS NOT NULL and row IS NOT NULL")
        assertEquals 0, merged.count("col IS NULL or row IS NULL")
        merged.eachFeature{f ->
            assertNotNull f.geom
            assertTrue f.geom instanceof Geometry
        }
    }

    @Test void splitByField() {
        Schema schema = new Schema("grid",[
            new Field("geom","Polygon","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int")
        ])
        Layer layer = new Memory().create(schema)
        Bounds bounds = new Bounds(0,0,10,10)
        bounds.generateGrid(2, 2, "polygon", {cell, col, row ->
            layer.add([
                "geom": cell,
                "col": col,
                "row": row
            ])
        })

        Memory workspace = new Memory()
        layer.split(layer.schema.get("col"), workspace)
        Layer grid1 = workspace.get("grid_1")
        Layer grid2 = workspace.get("grid_2")
        assertNotNull grid1
        assertNotNull grid2
        assertEquals 2, grid1.count("col = 1")
        assertEquals 0, grid1.count("col = 2")
        assertEquals 2, grid2.count("col = 2")
        assertEquals 0, grid2.count("col = 1")
    }

    @Test void splitByLayer() {

        // Create the Layer
        Schema schema = new Schema("grid",[
            new Field("geom","Polygon","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int")
        ])
        Layer layer = new Memory().create(schema)
        new Bounds(0,0,10,10).generateGrid(4, 4, "polygon", {cell, col, row ->
            layer.add([
                "geom": cell,
                "col": col,
                "row": row
            ])
        })

        // Create the split Layer
        Schema splitSchema = new Schema("grid",[
            new Field("geom","Polygon","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int"),
            new Field("row_col","String")
        ])
        Layer splitLayer = new Memory().create(splitSchema)
        new Bounds(0,0,10,10).generateGrid(2, 1, "polygon", {cell, col, row ->
            splitLayer.add([
                "geom": cell,
                "col": col,
                "row": row,
                "row_col": "${row} ${col}"
            ])
        })

        Memory workspace = new Memory()
        layer.split(splitLayer,splitLayer.schema.get("row_col"),workspace)
        Layer grid11 = workspace.get("grid_1_1")
        Layer grid12 = workspace.get("grid_1_2")
        assertNotNull grid11
        assertNotNull grid12
        assertEquals splitLayer.bounds("col = 1"), grid11.bounds
        assertEquals splitLayer.bounds("col = 2"), grid12.bounds
    }

    @Test void buffer() {
        Schema schema = new Schema("points",[
            new Field("geom","Point","EPSG:4326"),
            new Field("col","int"),
            new Field("row","int")
        ])
        Layer layer = new Memory().create(schema)
        new Bounds(0,0,10,10).generateGrid(2, 2, "point", {cell, col, row ->
            layer.add([
                "geom": cell,
                "col": col,
                "row": row
            ])
        })

        // Buffer by distance
        Layer buffer = layer.buffer(2)
        assertEquals 4, buffer.count
        buffer.eachFeature{f ->
            assertTrue f.geom instanceof Polygon
            assertEquals 12.48, f.geom.area, 0.01
        }

        // Buffer by Field
        buffer = layer.buffer(new geoscript.filter.Property("col"))
        assertEquals 4, buffer.count
        buffer.eachFeature("col = 1", {f ->
            assertTrue f.geom instanceof Polygon
            assertEquals 3.12, f.geom.area, 0.01
        })
        buffer.eachFeature("col = 2", {f ->
            assertTrue f.geom instanceof Polygon
            assertEquals 12.48, f.geom.area, 0.01
        })

        // Buffer by Expression
        buffer = layer.buffer(geoscript.filter.Expression.fromCQL("col * 2"))
        assertEquals 4, buffer.count
        buffer.eachFeature("col = 1", {f ->
            assertTrue f.geom instanceof Polygon
            assertEquals 12.48, f.geom.area, 0.01
        })
        buffer.eachFeature("col = 2", {f ->
            assertTrue f.geom instanceof Polygon
            assertEquals 49.94, f.geom.area, 0.01
        })

        // Buffer by Function
        buffer = layer.buffer(new geoscript.filter.Function("calc_buffer(row,col)", {row, col -> row + col}))
        assertEquals 4, buffer.count
        assertEquals 12.48, buffer.first(filter: "col = 1 and row = 1").geom.area, 0.01
        assertEquals 28.09, buffer.first(filter: "(col = 1 and row = 2) or (col = 2 and row = 1)").geom.area, 0.01
        assertEquals 49.94, buffer.first(filter: "col = 2 and row = 2").geom.area, 0.01
    }
}

