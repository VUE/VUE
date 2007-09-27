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

import java.text.SimpleDateFormat;

import javax.activation.MimetypesFileTypeMap;

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
    public static final String MIME_TYPE = "%MIME_TYPE%";
    public static final String FORMAT = "foxml1.0";
    public static final String COMMENT = "Automatic Publish From VUE";
    public static final String VUE_MIME_TYPE ="application/vue";
    public static final String VUE_FORMAT_URL = "http://vue.tufts.edu/docs/vueformat/";
    public static final String VUE_DATASOURCE = "map.vue";
    public static final String RESOURCE_DATASOURCE = "RESOURCE";
    public static final String VUE_CM =  "tufts/vue/map/generic";
    public static final String OTHER_CM = "tufts/vue/other";
    public static final String RESULT_FIELDS[] = {"pid"};
    public static final String FILE_PREFIX = "file://";
    static String foxml;
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
            System.out.println("MapId:"+mapPid+" Result Size:"+result.getResultList().length+" Result:"+result);
            if(result.getResultList().length  >0 ) {
                fc.getAPIM().modifyDatastreamByReference(mapPid, VUE_DATASOURCE,  null,map.getLabel(), VUE_MIME_TYPE, VUE_FORMAT_URL, uploadId, null,null,  COMMENT,true);
            } else {
                // reading the foxml if it doesn't exist
                //if(foxml == null) {
                    BufferedReader r = new BufferedReader(new FileReader(VueResources.getFile("fedora.cm.vue")));
                    String line = new String();
                    foxml = "";
                    while((line = r.readLine())!=null) {
                        foxml += line+"\n";
                    }
               // }
                SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
                Date currentTime = new Date();
                String dateString = formatter.format(currentTime);
                System.out.println("Date String: "+dateString);
                foxml = foxml.replace("%OWNER%",userName);
                foxml = foxml.replaceAll("%CREATE_DATE%",dateString);
                foxml = foxml.replace("%PID%",mapPid);
                foxml = foxml.replace("%TITLE%",map.getLabel());
                foxml = foxml.replace("%CONTENT_MODEL%",VUE_CM);
                foxml = foxml.replace("%UPLOAD%",uploadId);
                foxml = foxml.replace("%DC%",getBasicDC(map.getLabel(),mapPid));
                System.out.println(foxml);
                StringBufferInputStream s = new StringBufferInputStream(foxml);
                AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), s,FORMAT, COMMENT);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    public static void uploadMapAll(String protocol, String host, int port, String userName, String password,LWMap map) {
        try {
            Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            while(i.hasNext()) {
                LWComponent component = (LWComponent) i.next();
                // System.out.println("Component:"+component+" has resource:"+component.hasResource());
                if(component.hasResource() && (component.getResource() instanceof URLResource)){
                    URLResource resource = (URLResource) component.getResource();
                    //   System.out.println("Component:"+component+"file:" +resource.getSpec()+" has file:"+resource.getSpec().startsWith(FILE_PREFIX));
                    if(resource.isLocalFile()) {
                        String pid = "vue:"+component.getURIString().substring(map.getURIString().lastIndexOf("/")+1);  
                        File file = new File(resource.getSpec());
                        System.out.println("LWComponent:"+component.getLabel() + "Resource: "+resource.getSpec()+"File:"+file+" exists:"+file.exists()+" MimeType"+new MimetypesFileTypeMap().getContentType(file));
                        uploadObjectToRepository(protocol,host,port, userName,password,file, VueResources.getFile("fedora.cm.other"),OTHER_CM,(new MimetypesFileTypeMap().getContentType(file)),pid,file.getName());
                        //Replace the link for resouce in the map
                        String ingestUrl =  "http://"+host+":8080/fedora/get/"+pid+"/RESOURCE";
                        resource.setSpec(ingestUrl);
                    }
                }
            }
            //upload the map
            uploadMap( protocol,   host, port,   userName,   password,  map);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void uploadObjectToRepository(String protocol, String host, int port, String userName, String password,File file, File contentModel,String cm,String mimeType,String pid,String label) {
        try{
            System.setProperty("javax.net.ssl.trustStore", VueUtil.getDefaultUserFolder()+"\\truststore");
            System.setProperty("javax.net.ssl.trustStorePassword","tomcat");
            FedoraClient fc = new FedoraClient(protocol+"://"+host+":"+port+"/fedora/", userName, password);
            AutoFinder af = new AutoFinder(fc.getAPIA());
            FieldSearchQuery query =  new FieldSearchQuery();
            Condition conds[] = new Condition[1];
            conds[0] = new  Condition(); //"pid",ComparisonOperator.eq,mapPid);
            conds[0].setProperty("pid");
            conds[0].setOperator(ComparisonOperator.eq);
            conds[0].setValue(pid);
            query.setConditions(conds);
            FieldSearchResult result = af.findObjects(RESULT_FIELDS,1,query);
            Uploader uploader = new Uploader(protocol, host, port, userName, password);
            String uploadId = uploader.upload(file);
            if(result.getResultList().length  >0 ) {
                fc.getAPIM().modifyDatastreamByReference(pid, RESOURCE_DATASOURCE,  null,label, mimeType, VUE_FORMAT_URL, uploadId, null,null,  COMMENT,true);
            } else {
                // reading the foxml if it doesn't exist
             //   if(foxml == null) {
                    BufferedReader r = new BufferedReader(new FileReader(contentModel));
                    String line = new String();
                    foxml = "";
                    while((line = r.readLine())!=null) {
                        foxml += line+"\n";
                    }
               // }
                SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
                Date currentTime = new Date();
                String dateString = formatter.format(currentTime);
                foxml = foxml.replace("%OWNER%",userName);
                foxml = foxml.replaceAll("%CREATE_DATE%",dateString);
                foxml = foxml.replace("%PID%",pid);
                foxml = foxml.replace("%TITLE%",label);
                foxml = foxml.replace("%CONTENT_MODEL%",cm);
                foxml = foxml.replace("%UPLOAD%",uploadId);
                foxml = foxml.replace("%DC%",getBasicDC(label,pid));
                foxml = foxml.replace("%MIME_TYPE%",mimeType);
                System.out.println(foxml);
                BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\temp\\IngestTest.xml"));
                writer.write(foxml);
                writer.close();
                StringBufferInputStream s = new StringBufferInputStream(foxml);
                AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), s,FORMAT, COMMENT);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    public static String getBasicDC(String title,String identifier) {
        String dc = new String();
        dc = "<dc:title>"+title+"</dc:title>";
        dc += "<dc:identifier>"+identifier+"</dc:identifier>";
        return dc;
    }
    
}
