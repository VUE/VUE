/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.osid.OsidException;

import edu.tufts.vue.dsm.impl.VueDataSourceManager;

/**
 * The purpose of this class is to resolve authentication on images stored in
 * protected repositories.
 * 
 * The initial use case is Sakai.  We can authenticate access to images stored
 * in Sakai by getting a session id through the Sakai web service, and passing that session id
 * in the header of the http request.
 * 
 * Since getRequestProperties is called for every URL image resource, most of which are not stored 
 * in Sakai, the default behavior of this method should be as lightweight as possible.
 * 
 * A future goal is to generalize this class so that it is not Sakai specific.
 *  
 */
public class UrlAuthentication implements edu.tufts.vue.dsm.DataSourceListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(UrlAuthentication.class);
    
    private static final Map<String, Map<String,String>> HostMap = new java.util.concurrent.ConcurrentHashMap();
    private static final UrlAuthentication ua = new UrlAuthentication();
    
    /** Note: this must be called the first time before the VDSM has configured any DataSources */
    public static UrlAuthentication getInstance() {
        return ua;
    }

    public void changed(final edu.tufts.vue.dsm.DataSource[] dataSources,
                        final Object state,
                        final edu.tufts.vue.dsm.DataSource changed)
    {
        if (state == VueDataSourceManager.DS_CONFIGURED) {

            if (DEBUG.DR) Log.debug("CONFIGURED: " + changed);

            scanForCredentials(changed);

        } else if (state == VueDataSourceManager.DS_ALL_CONFIGURED) {

            if (VueDataSourceManager.BLOCKING_OSID_LOAD) {
                for (edu.tufts.vue.dsm.DataSource ds : dataSources)
                    scanForCredentials(ds);
            }
            
            if (true||DEBUG.Enabled)
                Log.info("Done scanning for authentication keys: " + HostMap);
            else
                Log.info("Done scanning for authentication keys.");
        }

    }

    private void scanForCredentials(edu.tufts.vue.dsm.DataSource ds) {
        if (DEBUG.Enabled) Log.debug("scanning " + ds);
        try {
            if (SakaiExport.isSakaiSource(ds)) {
                Log.info("asking sakai data source for any needed credentials: " + ds); 
                loadHostMap(ds.getConfiguration());
            }
        } catch (OsidException e) {
            Log.error(e);
        }
    }

    
    /** This must be instanced before the VDSM has configured any DataSources */
    private UrlAuthentication() 
    {
        try {
            edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance()
                .addDataSourceListener(this);
            Log.info("instanced; listening to VDSM");
        } catch (Throwable t) {
            // Even if we fail for any reason, make sure UrlAuthentication successfully
            // initializes, otherwise every single URL data fetch (e.g., for images at
            // open access on the web) could fail, as their data fetches all go through
            // this code to check for possible needed authorization.
            Log.error(t);
        }
    }
    
    
//     /**
//      * Currently stores only Sakai hosts
//      */
//     private UrlAuthentication() 
//     {
//         edu.tufts.vue.dsm.DataSourceManager dsm;
//         edu.tufts.vue.dsm.DataSource dataSources[] = null;
				
//         try {
//             // load new data sources
//             Log.info("Fetching VueDataSourceManager");
//             dsm = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
			
//             Log.info("Fetching Sakai data sources from VDSM");
            
//             // Sakai specific code begins
//             SakaiExport se = new SakaiExport(dsm);
//             dataSources = se.getSakaiDataSources();
			
//             for (int i = 0; i < dataSources.length; i++) {
//                 if (dataSources[i].hasConfiguration()) {
//                     Log.info("asking sakai data source for any needed credentials: " + dataSources[i]);
//                     Properties configuration = dataSources[i].getConfiguration();
//                     loadHostMap(configuration);
//                     //VUE.Log .info("Sakai session id = " + _sessionId);
//                 }
//             }

//             Log.info("Done loading authentication keys.");
            
//         } catch (OsidException e) {
//             Log.error(e);
//             // VueUtil.alert("Error loading Resource", "Error");
//         } catch (Throwable t) {
//             // Even if we fail to load any needed authorization keys for any web hosts,
//             // make sure UrlAuthentication successfully initializes, otherwise every
//             // single URL data fetch (e.g., for images at open access on the web)
//             // could fail, as their data fetches all go through this code to check
//             // for possible needed authorization.  SMF 2008-02-28
//             Log.error(t);
//         }
//     }
	
    /** 
     * @param url The URL of map resource 
     * @return a Map of key/value pairs to delivered to a remote HTTP server with
     * a content request.  The set of key/value pairs should ensure that
     * the remote server will accept the incoming  URLConnection when
     * used with URLConnection.addRequestProperty.
     * E.g., key "Cookie", value "JSESSIONID=someAuthenticatedSessionID"
     */
    public static Map<String,String> getRequestProperties( URL url ) 
    {
        if (! "http".equals(url.getProtocol()))
            return null;

        final String key;

        if (url.getPort() > 0)
            key = url.getHost() + ":" + url.getPort();
        else
            key = url.getHost();

        //System.out.println("Checking for host/port key [" + key + "] in " + HostMap);
            
        return HostMap.get(key);
    }

    /**
     * This will return a URLConnection, authenticated if need be, with it's
     * connection already open.  Calling getInputStream() on the returned
     * connection will returned the cached open input stream, positioned
     * at the top of the content, reading for reading.
     */
    public static java.net.URLConnection getAuthenticatedConnection(URL url)
        throws java.io.IOException
    {
        //-----------------------------------------------------------------------------
        // We don't need authorization for any local file access

        if ("file".equals(url.getProtocol())) {
            //if (DEBUG.IO) Log.debug("Skipping auth checks for local access: " + url);
            Log.warn("Skipping auth checks for local access: " + url);

            java.net.URLConnection conn = null;

            // SMF: As of 2008-04-07, WinXP appears to hang for on the order of 30 seconds,
            // before sometimes return an FTP url connection!  (e.g., url was "file://Z:/0/test-pak.vue" )
            // Vista is returning same, only faster, but then the connection times out with a java.net.ConnectException.
            
            //conn = url.openConnection();            
            
            //if (DEBUG.IO) Log.debug("Returning local URLConnection: " + conn);
            Log.warn("Returning local URLConnection: " + conn);

            return conn;
        }
        
        //-----------------------------------------------------------------------------

        
        final String asText = url.toString();
        URL cleanURL = url;

        if (asText.indexOf(' ') > 0) {
            // Added 2007-09-20 SMF -- Sakai HTTP server is rejecting spaces in the URL path.
            try {
                cleanURL = new URL(asText.replaceAll(" ", "%20"));
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, asText);
                return null;
            }
        }

        final boolean debug = DEBUG.IMAGE || DEBUG.IO || DEBUG.DR;

        final Map<String,String> sessionKeys = UrlAuthentication.getRequestProperties(url);

        if (debug) Log.debug("opening URLConnection... (sessionKeys=" + sessionKeys + ") " + cleanURL);
        final java.net.URLConnection conn = cleanURL.openConnection();

        if (sessionKeys != null) {
            for (Map.Entry<String,String> e : sessionKeys.entrySet()) {
                if (debug) System.out.println("\tHTTP request[" + e.getKey() + ": " + e.getValue() + "]");
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        if (debug) {
            Log.debug("got URLConnection: " + conn);
            // Note: getting the request properties will throw an exception if called
            // after the connection is open (getInputStream is called)
            final Map<String,List<String>> rp = conn.getRequestProperties();
            for (Map.Entry<String,List<String>> e : rp.entrySet()) {
                System.out.println("\toutbound HTTP header[" +e.getKey() + ": " + e.getValue() + "]");
            }
        }
                
        if (debug) Log.debug("opening URL stream...");
        final java.io.InputStream urlStream = conn.getInputStream();
        if (debug) Log.debug("got URL stream");

        if (debug) {
            Log.debug("Connected; Headers from [" + conn + "];");
            // Note: asking for the header fields will force the connection open (getInputStream will be called)
            final Map<String,List<String>> headers = conn.getHeaderFields();
            List<String> response = headers.get(null);
            if (response != null)
                System.out.format("%20s: %s\n", "HTTP-RESPONSE", response);
            for (Map.Entry<String,List<String>> e : headers.entrySet()) {
                if (e.getKey() != null)
                    System.out.format("%20s: %s\n", e.getKey(), e.getValue());
            }
        }

        return conn;
    }

    public static java.io.InputStream getAuthenticatedStream(URL url)
        throws java.io.IOException
    {
        java.net.URLConnection conn = null;
        try {
            conn = getAuthenticatedConnection(url);
        } catch (java.io.IOException ioe) {
            Log.warn(url + ": " + ioe);
            throw ioe;
        }
        return conn.getInputStream();
    }

    /** This method will return the final redirected url. This method is important inorder to know that actual file name
     **/
    public static java.net.URL getRedirectedUrl(URL url, int n) {
        if(n==0) {
            return url;
        }
        try {
            if ("file".equals(url.getProtocol())) {
                if (DEBUG.IO) Log.debug("Skipping auth checks for local access: " + url);
                return url;
            }
            final String asText = url.toString();
            URL cleanURL = url;
            if (asText.indexOf(' ') > 0) {
                // Added 2007-09-20 SMF -- Sakai HTTP server is rejecting spaces in the URL path.
                try {
                    cleanURL = new URL(asText.replaceAll(" ", "%20"));
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, asText);
                    return null;
                }
            }
            final  HttpURLConnection conn = (HttpURLConnection) cleanURL.openConnection();
            conn.setInstanceFollowRedirects(false);
            if(conn.getHeaderField("location") == null) {
                return url;
            } else {
                return getRedirectedUrl(new URL(conn.getHeaderField("location")),n-1);
            }
            
        } catch (java.io.IOException ioe) {
            Log.warn(url + ": " + ioe);
        }
        return url;
    }


    /** 
     * Extract credentials from configuration of installed datasources and use those
     * credentials to generate a session id.  Note that though the configuration
     * information supports the OSID search, this code doesn't use OSIDs to generate a
     * session id.
     * @param configuration
     * @return 
     */
    private void loadHostMap(Properties configuration)
    {
        if (DEBUG.DR) Log.debug("loading " + configuration);
            
        String username = configuration.getProperty("sakaiUsername");
        String password = configuration.getProperty("sakaiPassword");
        String host     = configuration.getProperty("sakaiHost");
        String port     = configuration.getProperty("sakaiPort");

        String sessionId;
        boolean debug = false;

        // show web services errors?
        String debugString = configuration.getProperty("sakaiAuthenticationDebug");
        if (debugString != null) {
            debug = (debugString.trim().toLowerCase().equals("true"));
        }
		
        // System.out.println("username " + this.username);
        // System.out.println("password " + this.password);
        // System.out.println("host " + this.host);
        // System.out.println("port " + this.port);

        final String hostname;

        if (host.startsWith("http://")) {
            hostname = host.substring(7);
        } else {
            hostname = host;
            // add http if it is not present
            host = "http://" + host;
        }
                
        try {
            String endpoint = host + ":" + port + "/sakai-axis/SakaiLogin.jws";
            Service  service = new Service();
            Call call = (Call) service.createCall();
                    
            call.setTargetEndpointAddress (new java.net.URL(endpoint) );
            call.setOperationName(new QName(host + port + "/", "login"));
                    
            sessionId = (String) call.invoke( new Object[] { username, password } );

            // todo: the ".vue-sakai" should presumably come from the web service,
            // or at least from some internal config.
            sessionId = "JSESSIONID=" + sessionId + ".vue-sakai";

            final String hostPortKey;

            if ("80".equals(port)) {
                // 80 is the default port -- not encoded
                hostPortKey = hostname;
            } else {
                hostPortKey = hostname + ":" + port;
            }
                    

            Map<String,String> httpRequestProperties;

            if ("vue-dl.tccs.tufts.edu".equals(hostname) && "8180".equals(port)) {
                httpRequestProperties = new HashMap();
                httpRequestProperties.put("Cookie", sessionId);
                // Special case for tufts Sakai server? Do all Sakai servers
                // need this?
                httpRequestProperties.put("Host", "vue-dl.tccs.tufts.edu:8180");
                httpRequestProperties = Collections.unmodifiableMap(httpRequestProperties);
            } else {
                httpRequestProperties = Collections.singletonMap("Cookie", sessionId);
            }
            HostMap.put(hostPortKey, httpRequestProperties);
            Log.info("cached auth keys for [" + hostPortKey + "]; " + httpRequestProperties);
//             if (DEBUG.Enabled)
//                 System.out.println("URLAuthentication: cached auth keys for [" + hostPortKey + "]; "
//                                    + httpRequestProperties);
        }
        catch( MalformedURLException e ) {
			
        }
        catch( RemoteException e ) {
			
        }
        catch( ServiceException e ) {
			
        }
    }
}
