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

/**
 *
 * @author akumar03
 */
package tufts.vue;


import java.security.interfaces.DSAKey;
import java.util.*;
import java.net.*;
import java.io.*;

import java.text.SimpleDateFormat;

import javax.activation.MimetypesFileTypeMap;


import edu.tufts.vue.metadata.VueMetadataElement;

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
    public static final String HTTPS = "https";
    public static final String HTTP = "http";
    public static final String FEDORA_URL_PATH = "/fedora/";
    public static final String ENCODING = "UTF-8";
    public static final String COMMENT = "Object published through VUE";
    public static final boolean VERSIONABLE  = true;
    
    public static final String RESOURCE_DS = "RESOURCE";
    public static final String DC_DS = "DC";
    public static final String VUE_DS = "map.vue";
    
    
    public static final String FORMAT = "foxml1.0";
    public static final String VUE_FORMAT_URL = "http://vue.tufts.edu/docs/vueformat/";
    public static final String ONT_TYPE_METADATA = "http://vue.tufts.edu/ontology/vue.rdfs#ontoType";
    public static final String FEDORA_ONTOLOGY = "http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs#";
    public static final String DC_URL = VueResources.getString("metadata.dublincore.url");
    public static final String RELS_URL ="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    
    
    public static final String VUE_MIME_TYPE ="application/vue";
    public static final String XML_MIME_TYPE ="text/xml";
    
    public static final String DC_LABEL = "Dublin Core Metadata";
    public static final String RELS_LABEL ="Relationships to other objects";
    
    
    public static final String MAP_DS = "map.vue";
    public static final String RELS_DS = "RELS-EXT";
    
    public static final String VUE_CM =  "tufts/vue/map/generic";
    public static final String OTHER_CM = "tufts/vue/other";
    public static final String REMOTE_CM = "tufts/vue/remote";
    
    public static final String RESULT_FIELDS[] = {"pid"};
    
    public static final String FILE_PREFIX = "file://";
    
    static SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
    
    /** Creates a new instance of FedoraExporter */
    public FedoraPublisher() {
    }
    
    public static void uploadMap(edu.tufts.vue.dsm.DataSource ds, LWMap map) throws Exception {    
        addObjectToRepository(ds,VUE_CM,map.getFile(),map,map); 
    }
    
    public static void uploadArchive(edu.tufts.vue.dsm.DataSource ds, LWMap map) throws Exception {
        System.out.println("Saving archive to repository: "+map.getFile());
        //check if the file ends with vpk extension
        String  mapName = map.getFile().getName();
        if(mapName.endsWith(VueUtil.VueArchiveExtension)){
            addObjectToRepository(ds,VUE_CM,map.getFile(),map,map);
        } else {
            String archiveFilePath = map.getFile().getAbsolutePath();
            archiveFilePath = archiveFilePath.substring(0,archiveFilePath.length()-4)+VueUtil.VueArchiveExtension;
            File archiveFile = new File(archiveFilePath);
            tufts.vue.action.Archive.writeArchive(map,archiveFile);
            System.out.println("Writing Archival Object:"+archiveFile);
            addObjectToRepository(ds,VUE_CM,archiveFile,map,map);
        }
    }
    public static void uploadMapAll(edu.tufts.vue.dsm.DataSource ds, LWMap map) throws Exception{
        Properties properties = ds.getConfiguration();
        String mapLabel = map.getLabel();
        File origFile = map.getFile();
        
        File tempFile  = new File(VueUtil.getDefaultUserFolder()+File.separator+origFile.getName());
        tempFile.deleteOnExit();
        tufts.vue.action.ActionUtil.marshallMap(tempFile,map);
        
        LWMap cloneMap =   tufts.vue.action.OpenAction.loadMap(tempFile.getAbsolutePath());
        
        Iterator i = cloneMap.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource() && (component instanceof LWNode || component instanceof LWLink) && (component.getResource() instanceof URLResource)){
                URLResource resource = (URLResource) component.getResource();
                String pid = getFedoraPid(component);
                if(resource.isLocalFile()) {
                    File localFile = new File(resource.getSpec().replace(FILE_PREFIX,""));
                    addObjectToRepository(ds,OTHER_CM, localFile,  component, cloneMap);
                    String ingestUrl = HTTP+"://"+properties.getProperty("fedora22Address")+":"+properties.getProperty("fedora22Port")+FEDORA_URL_PATH+"get/"+pid+"/"+RESOURCE_DS;
                    component.setResource(URLResource.create(ingestUrl));
                } else if(!(resource instanceof Osid2AssetResource)) {
                    addObjectToRepository(ds,REMOTE_CM,null,component,cloneMap);
                    String ingestUrl = HTTP+"://"+properties.getProperty("fedora22Address")+":"+properties.getProperty("fedora22Port")+FEDORA_URL_PATH+"get/"+pid+"/"+RESOURCE_DS;
                    component.setResource(URLResource.create(ingestUrl));
                }
                //        System.out.println("Replacing resource: "+resource+ " with "+ingestUrl+" resource is "+resource.getClass());
                
                
            }
        }
        tufts.vue.action.ActionUtil.marshallMap(tempFile,cloneMap);
        uploadMap(ds,cloneMap);
        tufts.vue.action.ActionUtil.marshallMap(origFile, map);
        
        
    }
    
    private static void  addObjectToRepository(edu.tufts.vue.dsm.DataSource ds,String cModel,File file,LWComponent comp,LWMap map) throws Exception{
        Properties properties = ds.getConfiguration();
        System.setProperty("javax.net.ssl.trustStore", properties.getProperty("fedora22TrustStore"));
        System.setProperty("javax.net.ssl.trustStorePassword",properties.getProperty("fedora22TrustStorePassword"));
        FedoraClient fc = new FedoraClient(HTTPS+"://"+properties.getProperty("fedora22Address")+":"+properties.getProperty("fedora22SecurePort")+FEDORA_URL_PATH, properties.getProperty("fedora22UserName"), properties.getProperty("fedora22Password"));
        String pid = getFedoraPid(comp);
        AutoFinder af = new AutoFinder(fc.getAPIA());
        FieldSearchQuery query =  new FieldSearchQuery();
        Condition conds[] = new Condition[1];
        conds[0] = new  Condition(); //"pid",ComparisonOperator.eq,mapPid);
        conds[0].setProperty("pid");
        conds[0].setOperator(ComparisonOperator.eq);
        conds[0].setValue(pid);
        query.setConditions(conds);
        FieldSearchResult result = af.findObjects(RESULT_FIELDS,1,query);
        if(result.getResultList().length  >0 ) {
            modifyObject(fc,properties,cModel,file,comp,map);
        } else {
            addObject(fc,properties,cModel,file,comp,map);
        }
    }
    
    private static void addObject(FedoraClient fc,Properties p, String cModel,File file,LWComponent comp,LWMap map) throws Exception{
        String ingestFoxml =  getDigitalObjectXML(p,comp,map,cModel,file);
       BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\temp\\IngestTest.xml"));
        writer.write(ingestFoxml);
        writer.close();
        System.out.println("INGEST XML:\n"+ingestFoxml);
        StringBufferInputStream s = new StringBufferInputStream(ingestFoxml);
        AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), s,FORMAT, COMMENT);
    }
    private static void modifyObject(FedoraClient fc,Properties p, String cModel,File file,LWComponent comp,LWMap map) throws Exception{
        String pid = getFedoraPid(comp);
        String dsName = RESOURCE_DS;
        String mimeType =getMimeType(file,comp);
        if(cModel.equals(VUE_CM)) {
            dsName = VUE_DS;
            mimeType =VUE_MIME_TYPE;
        }
        String dcXML= getDC(comp,comp.getLabel(),pid);
        fc.getAPIM().modifyDatastreamByValue(pid,DC_DS,null,DC_LABEL,XML_MIME_TYPE,DC_URL,dcXML.getBytes(),null,null,COMMENT,true);
        // modifying rels-ext for non VUE_CM
        if(!cModel.equals(VUE_CM)){
            fc.getAPIM().modifyDatastreamByValue(pid,RELS_DS,null,RELS_LABEL,XML_MIME_TYPE,RELS_URL, getRDFDescriptionForLWComponent(comp,map).getBytes(),null,null,COMMENT,true);
        }
        if(cModel.equals(REMOTE_CM)){
            fc.getAPIM().modifyDatastreamByReference(pid, dsName,  null,comp.getLabel(), mimeType, VUE_FORMAT_URL, comp.getResource().getSpec(), null,null,  COMMENT,true);
        }else {
            dsName = file.getName();
            Uploader uploader = new Uploader(HTTPS, p.getProperty("fedora22Address"),Integer.parseInt(p.getProperty("fedora22SecurePort")),p.getProperty("fedora22UserName"), p.getProperty("fedora22Password"));
            String uploadId = uploader.upload(file);
            fc.getAPIM().modifyDatastreamByReference(pid, dsName,  null,comp.getLabel(), mimeType, VUE_FORMAT_URL, uploadId, null,null,  COMMENT,true);
            
        }
    }
    private static String getDigitalObjectXML(Properties p,LWComponent comp,LWMap map,String cModel,File file) throws Exception{
        String pid = getFedoraPid(comp);
        String label = comp.getLabel();
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<foxml:digitalObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("           xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        xml.append("           xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-0.xsd\"");
        xml.append("\n           PID=\""+ pid + "\">\n");
        xml.append("  <foxml:objectProperties>\n");
        xml.append("    <foxml:property NAME=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" VALUE=\"FedoraObject\"/>\n");
        xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""
                + label + "\"/>\n");
        xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#contentModel\" VALUE=\""
                + cModel + "\"/>\n");
        xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\""+p.getProperty("fedora22UserName")+"\"/>");
        xml.append("  </foxml:objectProperties>\n");
        xml.append(getDCXML(comp));
        if(!cModel.equals(VUE_CM)) {
            xml.append( getRelsXML(comp,map));
        }
        xml.append(getOjectDSXML(p,comp,cModel,file));
        xml.append("</foxml:digitalObject>");
        String objXML = xml.toString();
        return objXML;
    }
    
    private static String getOjectDSXML(Properties p,LWComponent comp,String cModel, File file) throws Exception {
        System.out.println("Getting object XML for "+comp.getLabel()+" file: "+file);
        String r = "";
        String dateString = getDateString();
        String uploadId = new String();
        String pid = getFedoraPid(comp);
        String dsName = RESOURCE_DS;
        String mimeType = getMimeType(file,comp);
        String controlGroup = "M";
        String contentLocationType = "INTERNAL_ID";
        if(cModel.equals(VUE_CM)) {
            dsName = file.getName();
            mimeType =VUE_MIME_TYPE;
            r += getDSXML("R",MAP_DS,dateString,comp.getLabel(),"http://local.fedora.server/fedora/get/"+pid+"/"+dsName,"URL",mimeType);
        }
        if(!cModel.equals(REMOTE_CM)){
            //dsName = URLEncoder.encode(file.getName(), ENCODING);
            dsName = file.getName();
            //Uploader uploader = new Uploader(HTTPS, p.getProperty("fedora22Address"),Integer.parseInt(p.getProperty("fedora22SecurePort")),p.getProperty("fedora22UserName"), edu.tufts.vue.util.Encryption.decrypt(p.getProperty("fedora22Password")));
            Uploader uploader = new Uploader(HTTPS, p.getProperty("fedora22Address"),Integer.parseInt(p.getProperty("fedora22SecurePort")),p.getProperty("fedora22UserName"), p.getProperty("fedora22Password"));
            uploadId = uploader.upload(file);
            r+= getDSXML("R",RESOURCE_DS,dateString,comp.getLabel(), "http://local.fedora.server/fedora/get/"+pid+"/"+dsName,"URL",mimeType);
        } else {
            controlGroup = "E";
            contentLocationType = "URL";
            uploadId = comp.getResource().getSpec();
            
        }
        
        return r+ getDSXML(controlGroup,dsName,dateString,comp.getLabel(), uploadId,contentLocationType,mimeType);
    }
    
    public static String getDSXML(String controlGroup,String dsName,String dateString,String label, String uploadId,String contentLocationType, String mimeType) {
        StringBuffer xml = new StringBuffer();
        xml.append("<foxml:datastream CONTROL_GROUP=\""+controlGroup+"\" ID=\""+dsName+"\" STATE=\"A\" VERSIONABLE=\"true\">\n");
        xml.append("<foxml:datastreamVersion CREATED=\""+dateString+"\" FORMAT_URI=\"http://vue.tufts.edu/docs/vueformat/\"");
        xml.append(" ID=\""+dsName+".0\" LABEL=\""+label+"\" MIMETYPE=\""+mimeType+"\">\n");
        xml.append("<foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>\n");
        xml.append("<foxml:contentLocation  REF=\""+uploadId+"\" TYPE=\""+contentLocationType+"\"/>\n");
        xml.append("</foxml:datastreamVersion></foxml:datastream>\n");
        return xml.toString();
    }
    
    
    
    private static String getDCXML(LWComponent comp) {
        String dateString = getDateString();
        StringBuffer xml = new StringBuffer();
        xml.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"DC\" STATE=\"A\" VERSIONABLE=\"true\">\n");
        xml.append("<foxml:datastreamVersion CREATED=\""+dateString+"\" ID=\"DC1.0\" LABEL=\"Dublin Core Metadata\" MIMETYPE=\"text/xml\">\n");
        xml.append("<foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>\n");
        xml.append(" <foxml:xmlContent>\n");
        xml.append(getDC(comp,comp.getLabel(),getFedoraPid(comp)));
        xml.append("</foxml:xmlContent></foxml:datastreamVersion></foxml:datastream>\n");
        return xml.toString();
    }
    
    private static String getDC(LWComponent c,String title,String identifier) {
        String dc = new String();
//      System.out.println("getDC: LWComponent: "+c+ " metadata elements: "+  c.getMetadataList().getMetadata().size());
        dc +="<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">";
        dc += "<dc:title>"+title+"</dc:title>";
        dc += "<dc:identifier>"+identifier+"</dc:identifier>";
        if(c.getNotes() != null) {
            dc += "<dc:description>"+c.getNotes()+"</dc:description>";
        }
        for(VueMetadataElement element: c.getMetadataList().getMetadata()) {
            //          System.out.println("Publishing Metadata: key:"+element.getKey()+ " value: "+ element.getValue()+"  for "+c.getLabel());
            if(element.getKey().contains(DC_URL)) {
                String key = "dc:"+element.getKey().substring(DC_URL.length()+1).toLowerCase();
                dc += "<"+key+">"+element.getValue()+"</"+key+">";
            }
        }
        dc += "</oai_dc:dc>";
        return dc;
    }
    
    private static String getRelsXML(LWComponent comp,LWMap map) throws Exception  {
        String dateString = getDateString();
        StringBuffer xml = new StringBuffer();
        xml.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\" STATE=\"A\" VERSIONABLE=\"true\">\n");
        xml.append("<foxml:datastreamVersion CREATED=\""+dateString+"\" ID=\"RELS-EXT.0\" LABEL=\"Relationships to other objects\" MIMETYPE=\"text/xml\">\n");
        xml.append("<foxml:xmlContent>\n");
        xml.append(getRDFDescriptionForLWComponent(comp,map));
        xml.append("</foxml:xmlContent></foxml:datastreamVersion></foxml:datastream>");
        return xml.toString();
        
    }
    
    
    private static String getRDFDescriptionForLWComponent(LWComponent comp,LWMap map) {
        String rdfDescription = new String();
        rdfDescription = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">";
        rdfDescription += "<rdf:Description rdf:about=\"info:fedora/"+getFedoraPid(comp)+"\">";
        for(LWComponent c: map.getAllDescendents(LWComponent.ChildKind.PROPER)) {
            for(VueMetadataElement element: c.getMetadataList().getMetadata()) {
                if(DEBUG.RDF) System.out.println("METADATA: "+element.getValue()+" key:  "+element.getKey()+" LWComponent: "+comp.getLabel() );
                if(element.getKey().equals(ONT_TYPE_METADATA) && element.getValue().startsWith(FEDORA_ONTOLOGY) && c instanceof LWLink){
                    LWLink link = (LWLink)c;
                    LWComponent head = link.getHead();
                    if(getFedoraPid(head).equals(getFedoraPid(comp))) {
                        if(DEBUG.RDF) System.out.println("METADATA MATCH COMPONENT: "+element.getValue()+" key:  "+element.getKey()+" LWComponent: "+comp.getLabel() +" Link:"+link);
                        LWComponent tail = link.getTail();
                        rdfDescription +=  "<rel:"+getFedoraOntologyTerm(element.getValue())+" rdf:resource=\"info:fedora/"+getFedoraPid(tail)+"\" />\n";
                    }
                }
            }
        }
        rdfDescription +=  "<rel:IsPartOf rdf:resource=\"info:fedora/"+getFedoraPid(map)+"\" />";
        rdfDescription += "</rdf:Description>";
        rdfDescription +=      "</rdf:RDF>";
        return rdfDescription;
    }
    
    private static String getFedoraPid(LWComponent component) {
        return "vue:"+component.getURIString().substring(component.getURIString().lastIndexOf("/")+1);
    }
    
    private static String getFedoraOntologyTerm(String value) {
        String term = new String();
        term = value.substring(FEDORA_ONTOLOGY.length());
        term = term.replaceAll(" ","");
        return term;
    }
    
    private static String getDateString() {
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }
    
    private static String getMimeType(File file, LWComponent comp) {
        String mimeType = "text/html";
        if(file!= null) {
            mimeType =  new MimetypesFileTypeMap().getContentType(file) ;
        } else{
            mimeType =  new MimetypesFileTypeMap().getContentType(comp.getResource().getSpec());
        }
        return mimeType;
    }
    public static void replaceResource(LWMap map,Resource r1,Resource r2) {
        Iterator i = map.getAllDescendentsIterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource()){
                Resource resource = component.getResource();
                if(resource.getSpec().equals(r1.getSpec()))
                    component.setResource(r2);
            }
        }
    }
}
