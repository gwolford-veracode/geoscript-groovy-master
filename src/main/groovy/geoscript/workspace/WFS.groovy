package geoscript.workspace

import org.geotools.api.data.DataStore
import org.geotools.data.wfs.WFSDataStore
import org.geotools.data.wfs.WFSDataStoreFactory

/**
 * A WFS Workspace.
 * <p><blockquote><pre>
 * WFS wfs = new WFS("http://localhost:8080/geoserver/ows?service=wfs&version=1.1.0&request=GetCapabilities")
 * </pre></blockquote></p>
 * @author Jared Erickson
 */
class WFS extends Workspace {

    /**
     * Create a new WFS Workspace with a WFS get capabilities URL.
     * @param options The optional named parameters can include:
     * <ul>
     *      <li>protocol = The HTTP protocol (auto, post, get)</li>
     *      <li>username = The user name if the server requires HTTP authentication</li>
     *      <li>password = The password if the server requires HTTP authentication</li>
     *      <li>encoding = The character encoding (defaults to UTF-8)</li>
     *      <li>maxFeatures = The maximum number of features to get (defaults to 0 or no limit)</li>
     *      <li>timeout = The HTTP connection timeout in milleseconds (defaults to 3000)</li>
     *      <li>bufferSize =  The number of features to buffer at once (defaults to 10)</li>
     *      <li>tryGzip = Tries to use GZip if the server supports it (defaults to true)</li>
     *      <li>lenient = Continue parsing features even if there is an error (defaults to true)</li>
     * </ul>
     * @param url The get capabilities URL
     */
    WFS(Map options = [:], String url) {
        super(new WFSDataStoreFactory().createDataStore(createParams(url, options)))
    }

    /**
     * Create a new WFS Workspace from a GeoTools WFSDataStore
     * @param ds The GeoTools WFSDataStore
     */
    WFS(WFSDataStore ds) {
        super(ds)
    }

    /**
     * Get the format
     * @return The workspace format name
     */
    String getFormat() {
        return "WFS"
    }

    /**
     * The String representation
     * @return A String representation
     */
    String toString() {
        return "WFS (" + (ds as WFSDataStore).capabilitiesURL + ")"
    }

    /**
     * Create a Map of parameters for the WFSDataStoreFactory.
     * @param url The get capabilities url
     * @param options A Map of options
     * @return A Map with WFSDataStoreFactory keys and values
     */
    private static Map createParams(String url, Map options) {
        Map params = [:]
        params.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", url)
        if (options.containsKey("protocol")){
            params.put("WFSDataStoreFactory:PROTOCOL", options.get("protocol"))
        }
        if (options.containsKey("username")) {
            params.put("WFSDataStoreFactory:USERNAME", options.get("username"))
        }
        if (options.containsKey("password")) {
            params.put("WFSDataStoreFactory:PASSWORD", options.get("password"))
        }
        if (options.containsKey("encoding")) {
            params.put("WFSDataStoreFactory:ENCODING", options.get("encoding"))
        }
        if (options.containsKey("maxFeatures")) {
            params.put("WFSDataStoreFactory:MAXFEATURES", options.get("maxFeatures"))
        }
        params.put("WFSDataStoreFactory:TIMEOUT", options.get("timeout", 3000))
        params.put("WFSDataStoreFactory:BUFFER_SIZE", options.get("bufferSize",10))
        params.put("WFSDataStoreFactory:TRY_GZIP", options.get("tryGzip",true))
        params.put("WFSDataStoreFactory:LENIENT", options.get("lenient",true))
        return params
    }

    /**
     * The WFS WorkspaceFactory
     */
    static class Factory extends WorkspaceFactory<WFS> {

        @Override
        Map getParametersFromString(String str) {
            Map params = [:]
            if (str.toLowerCase().startsWith("http") && str.toLowerCase().contains("service=wfs") &&
                str.toLowerCase().contains("request=getcapabilities")) {
                params.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", str)
            } else {
                params = super.getParametersFromString(str)
            }
            params
        }

        @Override
        WFS create(String type, Map params) {
            if (type.equalsIgnoreCase('wfs')) {
                Map newParams = WFS.createParams(params.get('url'), params)
                super.create(newParams)
            } else {
                null
            }
        }

        @Override
        WFS create(DataStore dataStore) {
            WFS wfs = null
            if (dataStore instanceof org.geotools.data.wfs.WFSDataStore) {
                wfs = new WFS(dataStore)
            }
            wfs
        }
    }
}