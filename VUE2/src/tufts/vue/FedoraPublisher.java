/*
 * FedoraExporter.java
 *
 * Created on September 19, 2007, 3:44 PM
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
 *
 * @author akumar03
 */
package tufts.vue;


import java.util.*;
import java.net.*;
import java.io.*;

//required for publishing to Fedora 
import fedora.client.FedoraClient;
import fedora.client.utility.ingest.AutoIngestor;
import fedora.client.utility.AutoFinder;
import fedora.server.types.gen.Datastream;
import fedora.client.Uploader;

public class FedoraPublisher {
    public static final String PID_KEY = "%PID";
    public static final String CM_KEY = "%CONTENT_MODEL%";
    public static final String CREATE_DATE_KEY = "%CREATE_DATE%";
    public static final String DC_KEY ="%DC%";
    public static final String UPLOAD_KEY ="%UPLOAD%";
    public static final String OWNER_KEY  = "%OWNER%";
    public static final String TITLE_KEY = "%TITLE%";
    public static final String MAP_CM = "edu/tufts/osidimpl/repository/fedora_2_2/VUEMapContentModel.xml";
    /** Creates a new instance of FedoraExporter */
    public FedoraPublisher() {
    }
    
    public static void uploadMap(String protocol, String host, int port, String userName, String password,LWMap map) {
        try {
            System.setProperty("javax.net.ssl.trustStore", VueUtil.getDefaultUserFolder()+"\\truststore");
            System.setProperty("javax.net.ssl.trustStorePassword","tomcat");
            FedoraClient fc = new FedoraClient(protocol+"://"+host+":"+port+"/fedora/", userName, password);
            String pids[] = AutoFinder.getPIDs("http", host, 8080, map.getURIString());
            if(pids.length >0 ) {
                Uploader uploader = new Uploader(protocol, host, port, userName, password);
                String uploadId = uploader.upload(new File("C:\\anoop\\vue\\maps\\ab.vue"));
                System.out.println("PID already exists in repository. Will update datastream: "+uploadId);
                // Datastream datastreams[] = fc.getAPIM().getDatastreams(pids[0],null,null);
                // for(int i=0;i<datastreams.length;i++) {
                //     System.out.println("Datastream: "+datastreams[i].getID()+" label: "+datastreams[i].getLabel());
                // }
                
                
                
                fc.getAPIM().modifyDatastreamByReference(pids[0], "MyMap.vue",  null,"VUE Map", "text/xml", "http://vue.tufts.edu/docs/vueformat/", uploadId, null,null,  "Testing from VUE",true);
                
            } else {
                BufferedReader r = new BufferedReader(new FileReader(VueResources.getFile(MAP_CM)));
                String line = new String();
                String foxml = new String();
                while((line = r.readLine())!=null) {
                    foxml += line+"\n";
                }
                foxml.replace("%OWNER%","fedoraAdmin");
                foxml.replaceAll("%CREATE_DATE%","2007-08-30T18:38:56.638Z");
                foxml.replace("%PID",map.getURIString());
                foxml.replace("%TITLE",map.getLabel());
                foxml.replace("%UPLOAD%","uploaded://1");
                foxml.replace("%CONTENT_MODEL","tufts/vue/map/generic");
                AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), new FileInputStream(VueResources.getFile(MAP_CM)),"foxml1.0", "Testing Ingest");
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
     
    }
}
