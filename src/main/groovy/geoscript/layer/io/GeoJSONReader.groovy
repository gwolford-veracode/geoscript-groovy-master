package geoscript.layer.io

import geoscript.feature.Schema
import geoscript.layer.Cursor
import geoscript.layer.Layer
import geoscript.proj.Projection
import geoscript.workspace.Memory
import geoscript.workspace.Workspace
import org.apache.commons.io.IOUtils
import org.geotools.feature.FeatureCollection
import org.geotools.geojson.feature.FeatureJSON
import org.geotools.api.feature.simple.SimpleFeatureType

/**
 * Read a {@link geoscript.layer.Layer Layer} from a GeoJSON InputStream, File, or String.
 * <p><blockquote><pre>
 * String json = """{"type":"FeatureCollection","features":[
 * {"type":"Feature","geometry":{"type":"Point","coordinates":[111,-47]},
 * "properties":{"name":"House","price":12.5},"id":"fid-3eff7fce_131b538ad4c_-8000"},
 * {"type":"Feature","geometry":{"type":"Point","coordinates":[121,-45]},
 * "properties":{"name":"School","price":22.7},"id":"fid-3eff7fce_131b538ad4c_-7fff"}]}"""
 * GeoJSONReader reader = new GeoJSONReader()
 * Layer layer = reader.read(json)
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class GeoJSONReader implements Reader {

    /**
     * Read a GeoScript Layer from an InputStream
     * @param options The optional named parameters:
     * <ul>
     *     <li>workspace: The Workspace used to create the Layer (defaults to Memory)</li>
     *     <li>projection: The Projection assigned to the Layer (defaults to null)</li>
     *     <li>name: The name of the Layer (defaults to geojson)</li>
     *     <li>uniformSchema: Whether the Schema is uniform and should be read from the first feature only (defaults to false)</li>
     * </ul>
     * @param input An InputStream
     * @return A GeoScript Layer
     */
    Layer read(Map options = [:], InputStream input) {
        // Default parameters
        Workspace workspace = options.get("workspace", new Memory())
        Projection proj = options.get("projection")
        String name = options.get("name", "geojson")
        boolean uniformSchema = options.get("uniformSchema", false)
        // Parse GeoJSON
        final FeatureJSON featureJSON = new FeatureJSON()
        FeatureCollection featureCollection
        if (uniformSchema) {
            featureCollection = featureJSON.readFeatureCollection(input)
        } else {
            StringWriter writer = new StringWriter()
            IOUtils.copy(input, writer)
            String json = writer.toString()
            SimpleFeatureType featureType = featureJSON.readFeatureCollectionSchema(json, true)
            featureJSON.featureType = featureType
            featureCollection = featureJSON.readFeatureCollection(json)
        }
        // Create Schema and Layer
        Schema schema = new Schema(featureCollection.schema).reproject(proj, name)
        Layer layer = workspace.create(schema)
        layer.add(new Cursor(featureCollection))
        layer

    }

    /**
     * Read a GeoScript Layer from a File
     * @param options The optional named parameters:
     * <ul>
     *     <li>workspace: The Workspace used to create the Layer (defaults to Memory)</li>
     *     <li>projection: The Projection assigned to the Layer (defaults to null)</li>
     *     <li>name: The name of the Layer (defaults to geojson)</li>
     *     <li>uniformSchema: Whether the Schema is uniform and should be read from the first feature only (defaults to false)</li>
     * </ul>
     * @param file A File
     * @return A GeoScript Layer
     */
    Layer read(Map options = [:], File file) {
        read(options, new FileInputStream(file))
    }

    /**
     * Read a GeoScript Layer from a String
     * @param options The optional named parameters:
     * <ul>
     *     <li>workspace: The Workspace used to create the Layer (defaults to Memory)</li>
     *     <li>projection: The Projection assigned to the Layer (defaults to null)</li>
     *     <li>name: The name of the Layer (defaults to geojson)</li>
     *     <li>uniformSchema: Whether the Schema is uniform and should be read from the first feature only (defaults to false)</li>
     * </ul>
     * @param str A String
     * @return A GeoScript Layer
     */
    Layer read(Map options = [:], String str) {
        read(options, new ByteArrayInputStream(str.getBytes("UTF-8")))
    }

}
