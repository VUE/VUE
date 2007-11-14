/*
 * SakaiPublisher.java
 *
 * Created on October 15, 2007, 5:03 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 * @author  akumar03
 * @version $Revision: 1.5 $ / $Date: 2007-11-14 17:20:15 $ / $Author: peter $
 */

package tufts.vue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import edu.tufts.vue.dsm.DataSource;


public class SakaiPublisher {
	
    /**
     * All references to _local resources have URLs with a "file" prefix
     */
	public static final String FILE_PREFIX = "file://";
    //private static final Map<String, Map<String,String>> HostMap = new HashMap();

	private static final org.apache.log4j.Logger Log = 
		org.apache.log4j.Logger.getLogger(SakaiPublisher.class);
    
    /** uploadMap
     *  
     * @param dataSource DataSource object for Sakai LMS
     * @param collectionId Workspace in Sakai to store map
     * @param map VUE map to store in Sakai
     */
    public static void uploadMap(
    		edu.tufts.vue.dsm.DataSource dataSource,  // target datasource
    		Object collectionId,  // String representing collection within Sakai worksite           
    		LWMap map)            // usually the current map 
    	throws Exception
    {
       	Properties dsConfig = dataSource.getConfiguration();
       	String sessionId = getSessionId(dsConfig);
       	LWMap cloneMap = (LWMap)map.clone();
        cloneMap.setLabel(map.getLabel());
        File savedMap = saveMapToFile( cloneMap );
        
        if( !savedMap.exists() )
        {
        	// there is no saved file to write to Sakai.
        }
        uploadObjectToRepository( 
        		dsConfig.getProperty("sakaiHost"),
        		dsConfig.getProperty("sakaiPort"),
        		sessionId, 
        		savedMap.getName(), 
        		collectionId.toString(), 
        		savedMap, "VUE map", 
        		cloneMap, 
        		dataSource);
    }
    
    /** uploadMapAll
     *  
     * @param dataSource DataSource object for Sakai LMS
     * @param collectionId Workspace in Sakai to store map
     * @param map VUE map to store in Sakai
     */
    public static void uploadMapAll(edu.tufts.vue.dsm.DataSource dataSource, Object collectionId, LWMap map) 
    {        
    	Properties dsConfig = dataSource.getConfiguration();
    	String sessionId = getSessionId(dsConfig);
    	
    }
    
    /**
     * Iterate over map, storing local resources in repository, then rewriting the 
     * references in the map so they point to the remote location.
     * 
     * Based on method of the same name in tufts.vue.FedoraPublisher.java.
     * 
     * @param host URL of repository 
     * @param port that repository listens on
     * @param userName of authorized user
     * @param password of authorized user
     * @param map the current map
     * @param ds TODO
     * @throws Exception
     */
     private static void uploadMapAll( String host, int port, String userName, String password, LWMap map, DataSource ds) throws Exception 
     {
    	LWMap cloneMap = (LWMap)map.clone();
        cloneMap.setLabel(map.getLabel());
        Iterator<LWComponent> i = cloneMap.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(i.hasNext()) 
        {
            LWComponent component = (LWComponent) i.next();
            Log.debug("Component:" + component +" has resource:" + component.hasResource());
            if(component.hasResource()  
            		&& (component instanceof LWNode || component instanceof LWLink) 
            		&& (component.getResource() instanceof URLResource))
            {
                URLResource resource = (URLResource) component.getResource();
                Log.debug("Component:" + component 
                		+ "file:" + resource.getSpec() 
                		+ " has file:"+resource.getSpec().startsWith(FILE_PREFIX));
                if(resource.isLocalFile()) 
                {                    
                    File file = new File(resource.getSpec().replace(FILE_PREFIX,""));
                    Log.debug("LWComponent:" + component.getLabel() 
                    		+ " Resource: "+resource.getSpec() 
                    		+ " File:" + file + " exists:" + file.exists() 
                    		+ " MimeType" + new MimetypesFileTypeMap().getContentType(file));
                    // Maps created on another computer could contain a reference to a local file
                    // doesn't exist on this user's computer.  Don't process these.
                    if( !file.exists() ) 
                    {
                    	continue;
                    }
//                    uploadObjectToRepository( host, port, userName, file, VueResources.getFile("fedora.cm.other"), (new MimetypesFileTypeMap().getContentType(file)), component, cloneMap, null);

                    //Replace the link for resource in the map
                    String ingestUrl =  "http://"+host+":8080/fedora/get/"+"/RESOURCE";
                    resource.setSpec(ingestUrl);
                }
            }
        }
        //upload the map
        //uploadMap( host, port, userName, password, cloneMap);
    }

 	
	/**
	 * @param sessionId a valid sessionid
	 * @param resourceName a name of the resource to be added
	 * @param collectionId  collectionId of the collection it is to be added to
	 * @param file
	 * @param file2
	 * @param description of the resource to be added
	 * @param component
	 * @param cloneMap
	 * @param ds
	 */
	private static void uploadObjectToRepository( String host, String port, String sessionId,
			String resourceName, String collectionId, File file, String description,
			LWMap cloneMap, DataSource ds) 
	{
		/**
		 *	Add a resource to a given collection.  The resource is passed either as text or encoded using Base64 flagged
		 *	using the binary parameter.
		 */
		 //String sessionId = null; 		// a valid sessionid
		 //String resourceName = null;	// a name of the resource to be added
		 //String collectionId = null; 	// collectionId of the collection it is to be added to
		 String contentMime = null;   	// contentMime content string 
		 //String description = null; 	// description of the resource to be added
		 boolean isBinary = true; 		// binary if true, content is encoded using Base64, if false content is assumed to be text.
		 /*
		public String createContentItem(String sessionid, String name, String collectionId, String contentMime, 
			String description, String type, boolean binary) {
		 	@returns 'Success' or 'Failure'
		*/
		
		try {
			String endpoint = host + ":" + port + "/sakai-axis/ContentHosting.jws";
			Service service = new Service();
			Call call = (Call) service.createCall();

			call.setTargetEndpointAddress(new java.net.URL(endpoint));
			call.setOperationName(new QName(sessionId + resourceName + "/", "createContentItem"));

			String retVal = (String) call.invoke( 
					new Object[] { sessionId, resourceName, collectionId, 
							org.apache.axis.encoding.Base64.encode(getByteArrayFromFile(file)), 
							description, isBinary });
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static byte[] getByteArrayFromFile(File file)
	{
		int bufferSize = 1024;
		ByteBuffer buff = ByteBuffer.allocate( bufferSize ); 
		try {
			FileChannel fc = new FileInputStream(file).getChannel();

			fc.read( buff );
			buff.flip();
			while( buff.hasRemaining() )
			{
				buff.get();
			}
		} catch( FileNotFoundException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		}
		return buff.array();
	}

   
	public static String getSessionId( Properties configuration ) 
	{
		String username = configuration.getProperty("sakaiUsername");
		String password = configuration.getProperty("sakaiPassword");
		String host = configuration.getProperty("sakaiHost");
		String port = configuration.getProperty("sakaiPort");

		String sessionId = null;

		if (!host.startsWith("http://")) {
			// add http if it is not present
			host = "http://" + host;
		}

		try {
			String endpoint = host + ":" + port + "/sakai-axis/SakaiLogin.jws";
			Service service = new Service();
			Call call = (Call) service.createCall();

			call.setTargetEndpointAddress(new java.net.URL(endpoint));
			call.setOperationName(new QName(host + port + "/", "login"));

			sessionId = (String) call
					.invoke(new Object[] { username, password });

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		return sessionId;
	}

	public static String getServerId( Properties configuration, String sessionId )
	{
		String host = configuration.getProperty("sakaiHost");
		String port = configuration.getProperty("sakaiPort");

		String serverId = null;

		if (!host.startsWith("http://")) {
			// add http if it is not present
			host = "http://" + host;
		}

		try {
			String endpoint = host + ":" + port + "/sakai-axis/SakaiServerUtil.jws";
			Service service = new Service();
			Call call = (Call) service.createCall();

			call.setTargetEndpointAddress(new java.net.URL(endpoint));
			call.setOperationName(new QName(host + port + "/", "getSakaiServerId"));

			serverId = (String) call
					.invoke(new Object[] { sessionId });
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		
		return serverId;
	}
	
	public static String getCookieString( DataSource ds )
	{
	   	Properties dsConfig = ds.getConfiguration();
    	String sessionId = getSessionId(dsConfig);
    	String serverId = getServerId( dsConfig, sessionId);
 
		return "JSESSION=" + sessionId + "." + serverId;
	}
	
	/**
	 * @param map
	 * @return File object that contains marshalled content of map parameter
	 */
	private static File saveMapToFile( LWMap map )
	{
		File tmpFile = map.getFile();
		
		// map exists on disk, but hasn't been changed in memory
		if( (!map.isModified()) && (null != tmpFile) )
		{
			tmpFile = map.getFile();
		}
		
		// map has changed in memory. (may exist on disk, but may not.)
		if( map.isModified() )
		{
			tmpFile = tufts.vue.action.ActionUtil.selectFile("Save Map", "vue");
			tufts.vue.action.ActionUtil.marshallMap( tmpFile );
		}
		
		return tmpFile;
	}
}
