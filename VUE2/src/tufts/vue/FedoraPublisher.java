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
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.client.Uploader;
import fedora.server.types.gen.Condition;
import fedora.server.types.gen.ComparisonOperator;

public class FedoraPublisher {
    public static final String PID_KEY = "%PID";
    public static final String CM_KEY = "%CONTENT_MODEL%";
    public static final String CREATE_DATE_KEY = "%CREATE_DATE%";
    public static final String DC_KEY ="%DC%";
    public static final String UPLOAD_KEY ="%UPLOAD%";
    public static final String OWNER_KEY  = "%OWNER%";
    public static final String TITLE_KEY = "%TITLE%";
    public static final String FORMAT = "foxml1.0";
    public static final String COMMENT = "Automatic Publish From VUE";
    public static final String RESULT_FIELDS[] = {"pid"};
    /** Creates a new instance of FedoraExporter */
    public FedoraPublisher() {
    }
    
    public static void uploadMap(String protocol, String host, int port, String userName, String password,LWMap map) {
        try {
            String mapPid = "vue:"+map.getURIString().substring(map.getURIString().lastIndexOf("/")+1);
            System.setProperty("javax.net.ssl.trustStore", VueUtil.getDefaultUserFolder()+"\\truststore");
            System.setProperty("javax.net.ssl.trustStorePassword","tomcat");
            FedoraClient fc = new FedoraClient(protocol+"://"+host+":"+port+"/fedora/", userName, password);
            AutoFinder af = new AutoFinder(fc.getAPIA());
            FieldSearchQuery query =  new FieldSearchQuery();
            Condition conds[] = new Condition[1];
             conds[0] = new  Condition(); //"pid",ComparisonOperator.eq,mapPid);
            conds[0].setProperty("pid");
            conds[0].setOperator(ComparisonOperator.eq);
            conds[0].setValue(mapPid);
            query.setConditions(conds);
            FieldSearchResult result = af.findObjects(RESULT_FIELDS,1,query);
             Uploader uploader = new Uploader(protocol, host, port, userName, password);
             String uploadId = uploader.upload(map.getFile());
               
            
            if(result.getResultList().length  >0 ) {
                //String pids[] = AutoFinder.getPIDs("http", host, 8080, mapPid);
                
                System.out.println("PID already exists in repository. Will update datastream: "+uploadId);
                // Datastream datastreams[] = fc.getAPIM().getDatastreams(pids[0],null,null);
                // for(int i=0;i<datastreams.length;i++) {
                //     System.out.println("Datastream: "+datastreams[i].getID()+" label: "+datastreams[i].getLabel());
                // }
                
                
                
                fc.getAPIM().modifyDatastreamByReference(mapPid, "map.vue",  null,"VUE Map", "application/vue", "http://vue.tufts.edu/docs/vueformat/", uploadId, null,null,  COMMENT,true);
                
            } else {
                BufferedReader r = new BufferedReader(new FileReader(VueResources.getFile("fedora.cm.vue")));
                String line = new String();
                String foxml = new String();
                while((line = r.readLine())!=null) {
                    foxml += line+"\n";
                }
                foxml = foxml.replace("%OWNER%","fedoraAdmin");
                foxml = foxml.replaceAll("%CREATE_DATE%","2007-08-30T18:38:56.638Z");
                foxml = foxml.replace("%PID%",mapPid);
                foxml = foxml.replace("%TITLE%",map.getLabel());
                foxml = foxml.replace("%CONTENT_MODEL%","tufts/vue/map/generic");
                foxml = foxml.replace("%UPLOAD%",uploadId);
                System.out.println("FOXML:"+foxml+"UPLOAD ID:"+uploadId);
                StringBufferInputStream s = new StringBufferInputStream(foxml);
                AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), s,FORMAT, COMMENT);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        
    }
}
