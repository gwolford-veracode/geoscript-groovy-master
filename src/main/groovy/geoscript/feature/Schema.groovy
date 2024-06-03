package geoscript.feature

import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.api.feature.simple.SimpleFeatureType
import org.geotools.api.feature.type.AttributeDescriptor
import org.geotools.api.feature.type.GeometryDescriptor
import org.geotools.feature.NameImpl
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.data.DataUtilities
import geoscript.proj.Projection

/**
 * A Schema describes the structure of a Feature.  It is contains a name, and
 * a set of Fields.
 * <p> You can create a Schema from a name and a List of {@link Field}s</p>
 * <code>
 * Schema s1 = new Schema("widgets", [new {@link Field}("geom","Point"), new {@link Field}("name","string"), new {@link Field}("price","float")])
 * </code>
 * <p> You can create a Schema from a name and a List of Lists which contain a name and type</p>
 * <code>
 * Schema s2 = new Schema("widgets", [["geom","Point"], ["name","string"], ["price","float"]])
 * </code>
 * <p> You can create a Schema from a name and a List of Maps containing name, type, and optionally proj keys</p>
 * <code>
 * Schema s3 = new Schema("widgets", [[name: "geom",type: "Point"], [name: "name", type: "string"], [name: "price", type: "float"]])
 * </code>
 * <p> You can create a Schema from a name and a type spec:</p>
 * <code>
 * Schema s4 = new Schema("widgets","geom:Point:srid=4326,name:String,price:float")
 * </code>
 * @author Jared Erickson
 */
class Schema {

    /**
     * The wrapped GeoTools SimpleFeatureType
     */
    SimpleFeatureType featureType

    /**
     * Create a new Schema wrapping a GeoTools SimpleFeatureType
     * @param The wrapped GeoTools SimpleFeatureType
     */
    Schema(SimpleFeatureType featureType) {
        this.featureType = featureType
    }

    /**
     * Create a new Schema with a name and a String containing a comma delimited
     * list of fields.
     * <p><code>
     * Schema s = new Schema("widgets","geom:Point:srid=4326,name:String,price:float")
     * </code></p>
     * @param name The Schema name
     * @param typeSpec The Schema String.
     * @param uri The namespace uri
     */
    Schema(String name, String typeSpec, String uri = "http://geoscript.org/feature") {
        this(DataUtilities.createType(uri, name, typeSpec))
    }

    /**
     * Create a new Schema with a name and a List of Fields.
     * <p><code>
     * Schema s1 = new Schema("widgets", [new Field("geom","Point"), new Field("name","string"), new Field("price","float")])
     * </code></p>
     * <p><code>
     * Schema s2 = new Schema("widgets", [["geom","Point"], ["name","string"], ["price","float"]])
     * </code></p>
     * <p><code>
     * Schema s3 = new Schema("widgets", [[name: "geom",type: "Point"], [name: "name", type: "string"], [name: "price", type: "float"]])
     * </code></p>
     * @param name The Schema's name
     * @param fields A List of Fields, a List of Lists, or a List of Maps
     * @param uri The namespace uri
     */
    Schema(String name, def fields, String uri = "http://geoscript.org/feature") {
        this(buildFeatureType(name, fields, uri))
    }

    /**
     * Get the Schema's name
     * @return The of the Schema
     */
    String getName() {
        featureType.name.localPart
    }

    /**
     * Get the Schema's geometry Field or null if none exists
     * @return The Schema's Geometry Field
     */
    Field getGeom() {
        Field f = null
        if (featureType.geometryDescriptor) {
            f = field(featureType.geometryDescriptor.localName)
        }
        f
    }

    /**
     * Get the Schema's Projection or null if one doesn't exist
     * @return A Projection or null
     */
    Projection getProj() {
        def p = null
        if (featureType.coordinateReferenceSystem) {
            p = new Projection(featureType.coordinateReferenceSystem)
        }
        p
    }

    /**
     * Get the namespace uri of this Schema
     * @return The namespace uri
     */
    String getUri() {
        featureType.name.namespaceURI
    }

    /**
     * Get a Field by name
     * @param name The name of the Field
     * @return The Field
     */
    Field field(String name) {
        AttributeDescriptor ad = featureType.getDescriptor(name)
        if (ad != null) {
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) ad
                Projection p = null
                if (gd.coordinateReferenceSystem != null) {
                    p = new Projection(gd.coordinateReferenceSystem)
                }
                else if (featureType.coordinateReferenceSystem != null) {
                    p = new Projection(featureType.coordinateReferenceSystem)
                }
                Field fld = new Field(gd.localName, Schema.lookUpAlias(gd.type.binding.name), p)
                return fld
            }
            else {
                return new Field(ad.localName, Schema.lookUpAlias(ad.type.binding.name))
            }
        }
        throw new IllegalArgumentException("No such field ${name}".toString())
    }

    /**
     * Get a Field by name
     * @param The name of the Field
     * @return The Field
     */
    Field get(String name) {
        field(name)
    }

    /**
     * Whether this Schema contains a Field by the given name or not
     * @param field The Field or Field name
     * @return Whether this Schema contains a Field by the given name or not
     */
    boolean has(def field) {
        featureType.getDescriptor(field instanceof Field ? field.name : field as String) != null
    }

    /**
     * Get the List of Fields
     * @return The List of Fields
     */
    List<Field> getFields() {
        List<Field> fields = []
        Iterator<AttributeDescriptor> it = featureType.attributeDescriptors.iterator()
        while(it.hasNext()) {
            AttributeDescriptor ad = it.next()
            fields.add(field(ad.localName))
        }
        return fields
    }

    /**
     * Create a Feature from this Schema with default values
     * @return A new Feature
     */
    Feature feature() {
        new Feature(DataUtilities.template(this.featureType))
    }

    /**
     * Create a Feature from an existing Feature
     * @param f The exiting Feature
     * @return A new Feature
     */
    Feature feature(Feature f) {
        feature(f.attributes, f.id)
    }

    /**
     * Create a Feature from this Schema with a List of values and the id.
     * @param The List of values
     * @param id The Feature ID (defaults to feature)
     * @return A new Feature
     */
    Feature feature(List values, String id = SimpleFeatureBuilder.createDefaultFeatureId()) {
        new Feature(values, id, this)
    }

    /**
     * Create a Feature from this Schema with a Map of values and the id.
     * @param The Map of key value pairs
     * @param id The Feature ID (defaults to feature)
     * @return A new Feature
     */
    Feature feature(Map values, String id = SimpleFeatureBuilder.createDefaultFeatureId()) {
        new Feature(values, id, this)
    }

    /**
     * Reproject the Schema.
     * @param prj A Projection or a String
     * @return The reprojected Schema
     */
    Schema reproject(def prj) {
        reproject(prj, name)
    }

    /**
     * Reproject the schema with a new name
     * @param prj A Projection or a String
     * @param name The new name of the reprojected Schema
     * @return The reprojected Schema
     */
    Schema reproject(def prj, String name) {
        Projection proj = prj != null ? new Projection(prj) : null
        Field geom = geom
        List<Field> flds = fields.collect{
            fld -> (fld.name == geom.name) ? new Field(fld.name, fld.typ, proj) : new Field(fld.name, fld.typ)
        }
        new Schema(name, flds)
    }

    /**
     * Create a new Schema with the same fields as this Schema but with a different
     * Geometry type
     * @param geometryType The new type of Geometry
     * @param name The new Schema name
     * @return A new Schema
     */
    Schema changeGeometryType(String geometryType, String name) {
        changeField(this.geom, new Field(this.geom.name, geometryType, this.geom.proj), name)
    }

    /**
     * Create a new Schema by changing an existing Field's definition.
     * @param oldField The old existing Field
     * @param newField The new Field definition
     * @param name The new Schema name
     * @return A new Schema
     */
    Schema changeField(Field oldField, Field newField, String name) {
        Map fieldsToChange = [:]
        fieldsToChange.put(oldField, newField)
        changeFields(fieldsToChange, name)
    }

    /**
     * Create a new Schema by changing one or more Field's definition.
     * @param fieldsToChange A Map of old existing Fields as keys and new Fields as values
     * @param name The new Schema name
     * @return A new Schema
     */
    Schema changeFields(Map<Field, Field> fieldsToChange, String name) {
        List flds = []
        fields.each{fld ->
            if (fieldsToChange.containsKey(fld)) {
                flds.add(fieldsToChange[fld])
            } else {
                flds.add(new Field(fld))
            }
        }
        new Schema(name, flds)
    }

    /**
     * Create a new Schema with a new name by adding a Field to the current Schema
     * @param field The Field to add
     * @param name The name of the new Schema
     * @return The new Schema with the added Field
     */
    Schema addField(Field field, String name) {
        addFields([field], name)
    }

    /**
     * Create a new Schema with a new name by adding a List of Fields to the current Schema
     * @param fields The List of Fields to add
     * @param name The name of the new Schema
     * @return The new Schema with the added Fields
     */
    Schema addFields(List<Field> newFields, String name) {
        List flds = []
        fields.each{fld ->
            flds.add(new Field(fld))
        }
        newFields.each {fld ->
            flds.add(fld)
        }
        new Schema(name, flds)
    }

    /**
     * Create a new Schema with a new name by removing a Field from the current Schema
     * @param field The Field to remove
     * @param name The name of the new Schema
     * @return The new Schema with the removed Field
     */
    Schema removeField(Field field, String name) {
        removeFields([field], name)
    }

    /**
     * Create a new Schema with a new name by removing a List of Fields from the current Schema
     * @param fieldsToRemove The List of Fields to remove
     * @param name The name of the new Schema
     * @return The new Schema with the removed Fields
     */
    Schema removeFields(List<Field> fieldsToRemove, String name) {
        List fieldNamesToRemove = fieldsToRemove.collect{fld -> fld.name.toLowerCase()}
        List flds = []
        fields.each{fld ->
            if (!fieldNamesToRemove.contains(fld.name.toLowerCase())) {
                flds.add(new Field(fld))
            }
        }
        new Schema(name, flds)
    }

    /**
     * Create a new Schema with only a subset of Fields
     * @param fieldsToInclude A List of Fields or Field names to include
     * @param name The new name
     * @return The new Schema
     */
    Schema includeFields(List fieldsToInclude, String name) {
        List<String> fieldNames = fieldsToInclude.collect{ fld ->
            if (fld instanceof Field) {
                fld.name
            } else {
                fld
            }
        }
        new Schema(name, fields.findAll{fld ->
            fieldNames.contains(fld.name)
        })
    }

    /**
     * Create a new Schema by adding another Schema to this current Schema. The Geometry Field is taken from the
     * current Schema.
     * @param otherSchema The other Schema
     * @param newName The new Schema's name
     * @param options A Map of optional parameters:
     * <ul>
     * <li>postfixAll: Whether to postfix all field names (true) or not (false). If true, all Fields from the
     * this current Schema will have '1' at the end of their name while the other Schema's Fields will have '2'.
     * Defaults to false.</li>
     * <li>includeDuplicates: Whether or not to include duplicate fields names. Defaults to false. If a duplicate is found
     * a '2' will be added.</li>
     * <li>maxFieldNameLength: The maximum new Field name length (mostly to support shapefiles where Field names can't be longer
     * than 10 characters</li>
     * <li>firstPostfix: The postfix string (default is '1') for Fields from the current Schema. Only applicable when postfixAll or includeDuplicates is true.</li>
     * <li>secondPostfix: The postfix string (default is '2') for Fields from the other Schema. Only applicable when postfixAll or includeDuplicates is true.</li>
     * </ul>
     * @return A Map with the new Schema as schema and two Maps containing the old and new Field names as fields.
     */
    Map addSchema(Map options = [:], Schema otherSchema, String newName) {
        boolean postfixAll = options.get("postfixAll", false)
        boolean includeDuplicates = options.get("includeDuplicates", false)
        int maxFieldNameLength = options.get("maxFieldNameLength", -1)
        String firstPostfix = options.get("firstPostfix","1")
        String secondPostfix = options.get("secondPostfix","2")
        List flds = []
        List fieldNames = []
        Map fieldMap1 = [:]
        this.fields.each {fld ->
            Field newField
            String fieldName = fld.name
            if (maxFieldNameLength > -1 && fieldName.length() + 1 > maxFieldNameLength) {
                fieldName = fieldName.substring(0, maxFieldNameLength)
            }
            if (fld.isGeometry()) {
                newField = new Field(fld)
            } else {
                newField = new Field ((postfixAll) ? "${fieldName}${firstPostfix}" : fieldName, fld.typ)
            }
            fieldMap1[fld.name] = newField.name
            flds.add(newField)
            fieldNames.add(newField.name)
        }
        Map fieldMap2 = [:]
        otherSchema.fields.each {fld ->
            if (!fld.isGeometry()) {
                String fieldName = fld.name
                if (maxFieldNameLength > -1 && fieldName.length() + 1 > maxFieldNameLength) {
                    fieldName = fieldName.substring(0, maxFieldNameLength)
                }
                boolean isDuplicate = fieldNames.contains(fld.name)
                Field newField = new Field ((postfixAll || isDuplicate) ? "${fieldName}${secondPostfix}" : fieldName, fld.typ)
                if (includeDuplicates || (!includeDuplicates && !isDuplicate)) {
                    fieldMap2[fld.name] = newField.name
                    flds.add(newField)
                    fieldNames.add(newField.name)
                }
            }
        }
        List fieldMaps = [fieldMap1, fieldMap2]
        [schema: new Schema(newName, flds), fields: fieldMaps]
    }

    /**
     * Get the spec string
     * @return The spec string
     */
    String getSpec() {
        DataUtilities.encodeType(this.featureType)
    }

    /**
     * The string representation
     * @return The string representation
     */
    @Override
    String toString() {
        "${name} ${fields.join(', ')}"
    }

    /**
     * Look up the alias of a GeoTools binding.
     * For example: "org.locationtech.jts.geom.LinearRing" => "LinearRing"
     * @param The Class name
     * @return The alias
     */
    static String lookUpAlias(String binding) {
        Map map = [
            ("org.locationtech.jts.geom.LinearRing".toLowerCase()) : "LinearRing",
            ("org.locationtech.jts.geom.LineString".toLowerCase()) : "LineString",
            ("org.locationtech.jts.geom.MultiLineString".toLowerCase()) : "MultiLineString",
            ("org.locationtech.jts.geom.MultiPoint".toLowerCase()) : "MultiPoint",
            ("org.locationtech.jts.geom.MultiPolygon".toLowerCase()) : "MultiPolygon",
            ("org.locationtech.jts.geom.Point".toLowerCase()) : "Point",
            ("org.locationtech.jts.geom.Polygon".toLowerCase()) : "Polygon",
            ("org.locationtech.jts.geom.Geometry".toLowerCase()) : "Geometry",
            ("org.locationtech.jts.geom.GeometryCollection".toLowerCase()) : "GeometryCollection",
            ("org.geotools.geometry.jts.CircularRing".toLowerCase()) : "CircularRing",
            ("org.geotools.geometry.jts.CircularString".toLowerCase()) : "CircularString",
            ("org.geotools.geometry.jts.CompoundCurve".toLowerCase()) : "CompoundCurve",
            ("org.geotools.geometry.jts.CompoundRing".toLowerCase()) : "CompoundRing",
            ("java.lang.String".toLowerCase()) : "String",
            ("java.lang.Float".toLowerCase()) : "Float",
            ("java.lang.Integer".toLowerCase()) : "Integer",
            ("java.lang.Long".toLowerCase()) : "Long",
            ("java.lang.Double".toLowerCase()) : "Double",
            ("java.util.Date".toLowerCase()) : "Date"
        ]
        map.get(binding.toLowerCase(), binding)
    }

    /**
     * Look up a GeoTools binding for the alias.
     * For example: "geoscript.geom.LinearRing" => "org.locationtech.jts.geom.LinearRing"
     * @param alias The alias
     * @return The GeoTools binding class name
     */
    static String lookUpBinding(String alias) {
        Map map = [
            ("geoscript.geom.LinearRing".toLowerCase()) : "org.locationtech.jts.geom.LinearRing",
            ("LinearRing".toLowerCase()) : "org.locationtech.jts.geom.LinearRing",
            ("geoscript.geom.LineString".toLowerCase()) : "org.locationtech.jts.geom.LineString",
            ("LineString".toLowerCase()) : "org.locationtech.jts.geom.LineString",
            ("geoscript.geom.MultiLineString".toLowerCase()) : "org.locationtech.jts.geom.MultiLineString",
            ("MultiLineString".toLowerCase()) : "org.locationtech.jts.geom.MultiLineString",
            ("geoscript.geom.MultiPoint".toLowerCase()) : "org.locationtech.jts.geom.MultiPoint",
            ("MultiPoint".toLowerCase()) : "org.locationtech.jts.geom.MultiPoint",
            ("geoscript.geom.MultiPolygon".toLowerCase()) : "org.locationtech.jts.geom.MultiPolygon",
            ("MultiPolygon".toLowerCase()) : "org.locationtech.jts.geom.MultiPolygon",
            ("geoscript.geom.Point".toLowerCase()) : "org.locationtech.jts.geom.Point",
            ("Point".toLowerCase()) : "org.locationtech.jts.geom.Point",
            ("geoscript.geom.Polygon".toLowerCase()) : "org.locationtech.jts.geom.Polygon",
            ("Polygon".toLowerCase()) : "org.locationtech.jts.geom.Polygon",
            ("geoscript.geom.Geometry".toLowerCase()) : "org.locationtech.jts.geom.Geometry",
            ("Geometry".toLowerCase()) : "org.locationtech.jts.geom.Geometry",
            ("geoscript.geom.CircularRing".toLowerCase()) : "org.geotools.geometry.jts.CircularRing",
            ("CircularRing".toLowerCase()) : "org.geotools.geometry.jts.CircularRing",
            ("geoscript.geom.CircularString".toLowerCase()) : "org.geotools.geometry.jts.CircularString",
            ("CircularString".toLowerCase()) : "org.geotools.geometry.jts.CircularString",
            ("geoscript.geom.CompoundCurve".toLowerCase()) : "org.geotools.geometry.jts.CompoundCurve",
            ("CompoundCurve".toLowerCase()) : "org.geotools.geometry.jts.CompoundCurve",
            ("geoscript.geom.CompoundRing".toLowerCase()) : "org.geotools.geometry.jts.CompoundRing",
            ("CompoundRing".toLowerCase()) : "org.geotools.geometry.jts.CompoundRing",
            ("String".toLowerCase()) : "java.lang.String",
            ("Str".toLowerCase()) : "java.lang.String",
            ("Float".toLowerCase()) : "java.lang.Float",
            ("Int".toLowerCase()) : "java.lang.Integer",
            ("Integer".toLowerCase()) : "java.lang.Integer",
            ("Short".toLowerCase()) : "java.lang.Integer",
            ("Long".toLowerCase()) : "java.lang.Long",
            ("Double".toLowerCase()) : "java.lang.Double",
            ("BigDecimal".toLowerCase()) : "java.lang.Double",
            ("Date".toLowerCase()) : "java.util.Date"    
        ]
        map.get(alias.toLowerCase(), alias)
    }

    static boolean isValidFieldType(String type) {
        boolean isValid = true
        try {
            Class.forName(Schema.lookUpBinding(type))
        } catch(ClassNotFoundException ex) {
            isValid = false
        }
        isValid
    }

    /**
     * Build a SimpleFeatureType from the name and a List of Fields.
     * @param name The name
     * @param fields A List of Fields or Strings
     * @param uri The namespace uri
     * @return a GeoTools SimpleFeatureType
     */
    private static SimpleFeatureType buildFeatureType(String name, def fields, String uri = "http://geoscript.org/feature") {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder()
        builder.setName(new NameImpl(name))
        if (uri != null) {
            builder.namespaceURI = uri
        }
        fields.each{field ->
            if (!(field instanceof Field)) {
                field = new Field(field)
            }
            Class c = Class.forName(lookUpBinding(field.typ))
            if (field.proj != null && field.proj.crs != null) {
                builder.setCRS(field.proj.crs)
                builder.add(field.name, c, field.proj.crs)
            }
            else {
                builder.add(field.name, c)
            }
        }
        builder.buildFeatureType()
    }

    @Override
    boolean equals(Object obj) {
        if (obj instanceof Schema) {
            this.featureType.equals(obj.featureType)
        } else {
            false
        }
    }

    @Override
    int hashCode() {
        this.featureType.hashCode()
    }
}