
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

package  tufts.vue;

/**
 *
 * @author  akumar03
 */

import java.util.Vector;
import java.util.Iterator;
import java.io.*;
import java.net.*;
import org.apache.commons.net.ftp.*;
import java.util.*;
import java.util.zip.*;

import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;
//import fedora.client.ingest.AutoIngestor;

import tufts.vue.action.*;

public class PublishUtil implements  tufts.vue.DublinCoreConstants  {
    public static final  String VUE_MIME_TYPE = VueResources.getString("vue.type");
    public static final  String BINARY_MIME_TYPE = "application/binary";
    public static final  String ZIP_MIME_TYPE = "application/zip";
    public static final int BUFFER_SIZE = 10240;// size for transferring files
    public static final String IMSCP_MANIFEST_ORGANIZATION = "%organization%";
    public static final String IMSCP_MANIFEST_METADATA = "%metadata%";
    public static final String IMSCP_MANIFEST_RESOURCES = "%resources%";
    public static final String RESOURCE_FOLDER = "resource:resources"+File.separator;
    public static File activeMapFile;
    public static String IMSManifest; // the string is written to manifest file;
  
    /** Creates a new instance of PublishUtil */
    public PublishUtil() {
    }
    
    public static File createZip(LWMap map, Vector resourceVector) throws IOException, URISyntaxException, CloneNotSupportedException, ZipException {
      
        System.out.println("Creating a zip file");
        IMSCP imscp = new IMSCP();
          try {
              if(map.getFile() == null) {
                  VueUtil.alert(VueResources.getString("dialog.exporterror.message"), VueResources.getString("dialog.exporterror.title"));
                  return null;
              }
        String saveEntry = VueUtil.getDefaultUserFolder()+File.separator+map.getFile().getName();
        File saveMapFile = new File(saveEntry);
        
        ActionUtil.marshallMap(saveMapFile,map);
        LWMap saveMap = tufts.vue.action.OpenAction.loadMap(saveEntry);
        //TODO: Need to add resources in zip file
        /**
        Iterator i = resourceVector.iterator();
        while(i.hasNext()) {
            Vector vector = (Vector)i.next();
            Boolean b = (Boolean)(vector.elementAt(0));
            if(b.booleanValue()) {
                Resource r = (Resource)(vector.elementAt(1));
                String rFileName = r.getSpec();
                File rFile = new File(rFileName);
                String entry = RESOURCE_FOLDER+rFile.getName();
                imscp.putEntry(entry,rFile);  
                replaceResource(saveMap,r,new MapResource(RESOURCE_FOLDER+rFile.getName()));
            }
        }
        
        ActionUtil.marshallMap(saveMapFile,saveMap);
         */
        imscp.putEntry(saveMap.getFile().getName(),saveMapFile);
        saveMapFile.deleteOnExit();
        imscp.closeZOS();
       } catch(Exception ex) {
            ex.printStackTrace();
        }
        return imscp.getFile();
         
    }
   public static File createIMSCP(Vector resourceVector) throws IOException,URISyntaxException,CloneNotSupportedException {
        String IMSCPMetadata = "";
        String IMSCPOrganization ="";
        String IMSCPResources = "";
        int resourceCount =2; //resourceIdentifier 1 is used for map
        Properties props = tufts.vue.VUE.getActiveMap().getMetadata().asProperties();
        IMSCPMetadata += getMetadataString(props);
        IMSCPResources += getResourceTag(props, IMSCP.MAP_FILE,1);
        IMSCPOrganization += "<organization identifier=\"TOC1\" structure=\"hierarchical\">";
        IMSCPOrganization += "<title>IMS Content Package of VUE Map</title> ";
        IMSCPOrganization += "<item identifier=\"ITEM1\" identifierref=\"RESOURCE1\">";
        IMSCPOrganization += "<title> VUE Cocept Map</title>";            
        LWMap saveMap = (LWMap) tufts.vue.VUE.getActiveMap().clone();
        IMSCP imscp = new IMSCP();
        Iterator i = resourceVector.iterator();
        while(i.hasNext()) {
            Vector vector = (Vector)i.next();
            Resource r = (Resource)(vector.elementAt(1));
            Boolean b = (Boolean)(vector.elementAt(0));
            File file = new File(new URL(r.getSpec()).getFile());
            //File file = new File((String)vector.elementAt(1));
            if(b.booleanValue()) {
                System.out.println("FileName = "+file.getName()+" index ="+resourceVector.indexOf(vector));
                //resourceTable.setValueAt("Processing",resourceVector.indexOf(vector),STATUS_COL);
                String entry = IMSCP.RESOURCE_FILES+File.separator+file.getName();
                imscp.putEntry(entry,file);
                IMSCPResources += getResourceTag(r.getProperties().asProperties(), entry,resourceCount);
                IMSCPOrganization += getItemTag("ITEM"+resourceCount, "RESOURCE"+resourceCount,"Resource "+resourceCount+" in Concept Map");
                //resourceTable.setValueAt("Done",resourceVector.indexOf(vector),STATUS_COL);
                replaceResource(saveMap,r,Resource.getFactory().get(IMSCP.RESOURCE_FILES+File.separatorChar+file.getName()));
                //replaceResource(saveMap,r,new MapResource(IMSCP.RESOURCE_FILES+File.separatorChar+file.getName()));
                resourceCount++;
            }
        }
        saveMap(saveMap);
        imscp.putEntry(IMSCP.MAP_FILE,activeMapFile);
        IMSCPOrganization +="</item>";  
        IMSCPOrganization +="</organization>";  
        IMSManifest = readRawManifest();
        IMSManifest = IMSManifest.replaceAll(IMSCP_MANIFEST_METADATA, IMSCPMetadata).trim();
        IMSManifest = IMSManifest.replaceAll(IMSCP_MANIFEST_ORGANIZATION, IMSCPOrganization);
        IMSManifest = IMSManifest.replaceAll(IMSCP_MANIFEST_RESOURCES, IMSCPResources);
          
        File IMSManifestFile = File.createTempFile("imsmanifest",".xml");
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(IMSManifestFile));
        fos.write(IMSManifest.getBytes());
        fos.close();
        imscp.putEntry(IMSCP.MANIFEST_FILE,IMSManifestFile);
        System.out.println("Writing Active Map : "+activeMapFile.getName());
        imscp.closeZOS();
        return imscp.getFile();
    }
   
    public static String getMetadataString(Properties dcFields) {
        String metadata = "";
        Enumeration e = dcFields.keys();
        while(e.hasMoreElements()) {
            String field = (String)e.nextElement();
            if(isSupportedMetadataField(field))
                metadata += "<"+DC_NAMESPACE+field+">"+dcFields.getProperty(field)+"</"+DC_NAMESPACE+field+">";
        }
        return metadata;
    }  
    
  public static String getResourceTag(Properties dcFields,String entry,int resourceCount) {
        String resourceTag = "";
        String identifier = "RESOURCE"+resourceCount;
        resourceTag = "<resource identifier=\""+identifier+"\" type=\"webcontent\" href=\""+entry+"\">\n";
        resourceTag += "<file  href=\""+entry+"\"/>\n";
        resourceTag += "<metadata>\n";
        resourceTag += "<schema>Dublin Core</schema> \n";
        resourceTag += "<schemaversion>1.1</schemaversion> \n";
        resourceTag += getMetadataString(dcFields);
        resourceTag += "</metadata>\n";
        resourceTag += "</resource>\n";
        return resourceTag;
    }
    
    public static String getItemTag(String item,String resource, String title) {
        String itemTag = "";
        itemTag += "<item identifier=\""+item+"\" identifierref=\""+resource+"\">";
        itemTag += "<title>"+title+"</title>";
        itemTag += "</item>";
        return itemTag;
    }
    public static String readRawManifest() {
        String s = "";
        try {
            BufferedInputStream fis = new BufferedInputStream(VueResources.getURL("imsmanifest").openStream());
            byte[] buf = new byte[BUFFER_SIZE];
            int ch;
            int len;
            while((len =fis.read(buf)) > 0) {
                s = s+ new String(buf);
            }
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }
    
   public static void replaceResource(LWMap map,Resource r1,Resource r2) {
       tufts.vue.action.Archive.replaceResource(map, r1, r2);
    }
    
    public static void saveActiveMap() throws IOException, CloneNotSupportedException {
        LWMap map = (LWMap) tufts.vue.VUE.getActiveMap().clone();
        
        activeMapFile = map.getFile();
        if(activeMapFile == null) {
            String prefix = "concept_map";
            String suffix = ".vue";
            activeMapFile  = File.createTempFile(prefix,suffix);
        }
        ActionUtil.marshallMap(activeMapFile, map);
    }
    
    public static  File saveMap(LWMap map) throws IOException {
        activeMapFile = map.getFile();
        if(activeMapFile == null) {
            String prefix = "concept_map";
            String suffix = ".vue";
            activeMapFile  = File.createTempFile(prefix,suffix);
        }
        ActionUtil.marshallMap(activeMapFile, map);
        return activeMapFile;
    }
    
    public static boolean isSupportedMetadataField(String field){
        for(int i=0;i<DC_FIELDS.length;i++) {
            if(DC_FIELDS[i].equalsIgnoreCase(field))
                return true;
        }
        return false;
    }
    
    
}
