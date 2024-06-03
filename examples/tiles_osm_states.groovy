import geoscript.feature.Feature
import geoscript.geom.Bounds
import geoscript.layer.Raster
import geoscript.layer.Shapefile
import geoscript.layer.OSM

import javax.imageio.ImageIO

File dir = new File("osm")
dir.mkdir()

OSM osm = new OSM("OSM", [
        "http://a.tile.openstreetmap.org",
        "http://b.tile.openstreetmap.org",
        "http://c.tile.openstreetmap.org"
])

Shapefile shp = new Shapefile("states.shp")
["North Dakota", "Oregon", "Washington"].each { String name ->
    shp.getFeatures("STATE_NAME = '${name}'").each { Feature f ->
        Bounds b = f.geom.bounds.expandBy(0.5)
        b.proj = "EPSG:4326"
        Raster raster = osm.getRaster(b.reproject("EPSG:3857"), 400, 400)
        ImageIO.write(raster.image, "png", new File(dir, "${name}.png"))
    }
}