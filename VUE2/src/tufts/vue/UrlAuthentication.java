/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 * 
 * $Revision: 1.2 $ / $Date: 2007/09/12 21:28:10 $ / $Author: peter $ 
 * $Header: /home/vue/cvsroot/VUE2/src/tufts/vue/UrlAuthentication.java,v 1.2 2007/09/12 21:28:10 peter Exp $
 */

package tufts.vue;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.osid.OsidException;

/**
 * The purpose of this class is to resolve authentication on images stored in
 * protected repositories.
 * 
 * The initial use case is Sakai.  We can authenticate access to images stored
 * in Sakai by getting a session id through the Sakai web service, and passing that session id
 * in the header of the http request.
 * 
 * Since getSessionId() is called for every(?) image resource, most of which are not stored 
 * in Sakai, the default behavior of this method should be as lightweight as possible.
 * 
 *  A goal is to generalize this class so that it is not Sakai specific.
 *  
 */
public class UrlAuthentication 
{
    private static UrlAuthentication ua = new UrlAuthentication();
    private static Map<String, String> hostMap = new HashMap<String, String>();
    public static UrlAuthentication getInstance() 
    {
        return ua;
    }
    
	URL _url;
    edu.tufts.vue.dsm.impl.VueDataSourceManager dataSourceManager = null;
    
    /**
     * Currently stores only Sakai hosts
     */
	private UrlAuthentication() 
	{
		edu.tufts.vue.dsm.DataSourceManager dsm;
		edu.tufts.vue.dsm.DataSource dataSources[] = null;
				
		try {
			// load new data sources
			VUE.Log
					.info("DataSourceViewer; loading Installed data sources via Data Source Manager");
			dsm = edu.tufts.vue.dsm.impl.VueDataSourceManager
					.getInstance();
			
			// Sakai specific code begins
			SakaiExport se = new SakaiExport(dsm);
			dataSources = se.getSakaiDataSources();
			
			for (int i = 0; i < dataSources.length; i++) 
			{
				VUE.Log
						.info("DataSourceViewer; adding data source to sakai data source list: "
								+ dataSources[i]);
				if (dataSources[i].hasConfiguration()) 
				{
					Properties configuration = dataSources[i]
							.getConfiguration();
					loadHostMap(configuration);
					//VUE.Log .info("Sakai session id = " + _sessionId);
				}
			}
		} catch (OsidException e) {
			e.printStackTrace();
			// VueUtil.alert("Error loading Resource", "Error");
		}
	}
	
	/** 
	 * 
	 * @param url The URL of map resource 
	 * @return the session id of the host, as a string, or null, if the host is unknown.
	 */
	public String getSessionId( URL url ) 
	{
		return hostMap.get(url);
	}

	/** 
	 * Extract credentials from configuration of installed datasources
	 * and use those credentials to generate a session id.  Note that 
	 * though the configuration information supports the OSID search, 
	 * this code doesn't use OSIDs to generate a session id.
	 * @param configuration
	 * @return 
	 */
	private void loadHostMap(Properties configuration)
	{
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
		
		// add http if it is not present
		if (!host.startsWith("http://")) {
			host = "http://" + host;
		}	
		try {
			String endpoint = host + ":" + port + "/sakai-axis/SakaiLogin.jws";
			Service  service = new Service();
			Call call = (Call) service.createCall();
			
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(host + port + "/", "login"));
			
			sessionId = (String) call.invoke( new Object[] { username, password } );
			hostMap.put(host, sessionId);
			// System.out.println("Session id " + this.sessionId);
		}
		catch( MalformedURLException e ) {
			
		}
		catch( RemoteException e ) {
			
		}
		catch( ServiceException e ) {
			
		}
	}
}
