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
 * $Revision: 1.140 $ / $Date: 2007/09/07 19:18:46 $ / $Author: dan $ 
 * $Header: /home/vue/cvsroot/VUE2/src/build.xml,v 1.140 2007/09/07 19:18:46 dan Exp $
 */

package tufts.vue;


import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import org.apache.axis.encoding.Base64;

import edu.tufts.vue.dsm.DataSource;

public class UrlAuthentication {
	URI _uri;
    edu.tufts.vue.dsm.impl.VueDataSourceManager dataSourceManager = null;

    
    public UrlAuthentication(String urlString) {
		try{
			_uri = new URI(urlString);
		}
		catch(URISyntaxException e) {
			e.printStackTrace();
		}
	}
 
    public UrlAuthentication(URL url) {
		try{
			_uri = url.toURI();
		}
		catch(URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public UrlAuthentication(URI uri) {
		_uri = uri ;
	}
	
	public String getSessionId() {
		edu.tufts.vue.dsm.DataSourceManager dsm;
		edu.tufts.vue.dsm.DataSource dataSources[] = null;
		DataSourceList dataSourceList;
		String _sessionId = "none";
		
		try {
			// load new data sources
			dsm = edu.tufts.vue.dsm.impl.VueDataSourceManager
					.getInstance();
			VUE.Log
					.info("DataSourceViewer; loading Installed data sources via Data Source Manager");
			((edu.tufts.vue.dsm.impl.VueDataSourceManager)dsm).load();
			SakaiExport se = new SakaiExport(dsm);
			dataSources = se.getSakaiDataSources();
			
			for (int i = 0; i < dataSources.length; i++) {
				VUE.Log
						.info("DataSourceViewer; adding data source to sakai data source list: "
								+ dataSources[i]);
				if (dataSources[i].hasConfiguration()) {
					Properties configuration = dataSources[i]
							.getConfiguration();
					_sessionId = getSessionId(configuration);
					VUE.Log .info("Sakai session id = " + _sessionId);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			// VueUtil.alert("Error loading Resource", "Error");
		}
		return _sessionId;
	}
	
	private String getSessionId(Properties configuration)
	{
		String username = configuration.getProperty("sakaiUsername");
		String password = configuration.getProperty("sakaiPassword");
		String host     = configuration.getProperty("sakaiHost");
		String port     = configuration.getProperty("sakaiPort");

		String sessionId = "none";
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
			// System.out.println("Session id " + this.sessionId);
		}
		catch( MalformedURLException e ) {
			
		}
		catch( RemoteException e ) {
			
		}
		catch( ServiceException e ) {
			
		}
		return sessionId;
	}
}
