println """<html>
    <head>
        <title>GeoScript WMS Example</title>
        <link rel="stylesheet" type="text/css" href="http://openlayers.org/api/theme/default/style.css">
        <link rel="shortcut icon" href="/static/favicon.ico">
        <script src="http://openlayers.org/api/OpenLayers.js"></script>
        <style>
            #map {
                width: 512px;
                height: 256px;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = new OpenLayers.Map({
                theme: null,
                div: "map",
                layers: [
                    new OpenLayers.Layer.WMS(
                        "Global Imagery",
                        "http://maps.opengeo.org/geowebcache/service/wms", 
                        {layers: "bluemarble"} 
                    ),
                    new OpenLayers.Layer.WMS(
                        "GeoScript WMS",
                        "http://localhost:8080/geoscript/tile.groovy",
                        {layers: "states"},
                        {isBaseLayer: false, buffer: 0, opacity: 0.7}
                    )
                ],
                center: new OpenLayers.LonLat(-98.28, 37.85),
                zoom: 3
            });
            map.addControl(new OpenLayers.Control.LayerSwitcher());
        </script>
    </body>
</html>"""

