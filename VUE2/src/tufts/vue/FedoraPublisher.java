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
    public static final String COMMENT = "Object published through VUE";
    public static final boolean VERSIONABLE  = true;
    
    public static final String RESOURCE_DS = "RESOURCE";
    public static final String DC_DS = "DC";
    public static final String VUE_DS = "map.vue";
    
    public static final String PID_KEY = "%PID";
    public static final String CM_KEY = "%CONTENT_MODEL%";
    public static final String CREATE_DATE_KEY = "%CREATE_DATE%";
    public static final String DC_KEY ="%DC%";
    public static final String UPLOAD_KEY ="%UPLOAD%";
    public static final String OWNER_KEY  = "%OWNER%";
    public static final String TITLE_KEY = "%TITLE%";
    public static final String MIME_TYPE = "%MIME_TYPE%";
    public static final String DC_MIME_TYPE_KEY = "%DC_MIME_TYPE%";
    public static final String DC_LABEL_KEY = "%DC_LABEL%";
    public static final String DC_ID_KEY = "%DC_ID%";
    public static final String RELS_EXT_KEY  = "%RELS_EXT%";
    public static final String FORMAT = "foxml1.0";
    public static final String VUE_MIME_TYPE ="application/vue";
    public static final String XML_MIME_TYPE ="text/xml";
    public static final String DC_LABEL = "Dublin Core Metadata";
    public static final String RELS_LABEL ="Relationships to other objects";
    public static final String VUE_FORMAT_URL = "http://vue.tufts.edu/docs/vueformat/";
    public static final String MAP_DS = "map.vue";
    public static final String RELS_DS = "RELS-EXT";
    
    public static final String VUE_CM =  "tufts/vue/map/generic";
    public static final String OTHER_CM = "tufts/vue/other";
    public static final String REMOTE_CM = "tufts/vue/remote";
    public static final String RESULT_FIELDS[] = {"pid"};
    public static final String FILE_PREFIX = "file://";
    public static final String ONT_TYPE_METADATA = "http://vue.tufts.edu/ontology/vue.rdfs#ontoType";
    public static final String FEDORA_ONTOLOGY = "http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs#";
    public static final String DC_URL = VueResources.getString("metadata.dublincore.url");
    public static final String RELS_URL ="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static Map<String,String> foxmls = new HashMap<String,String>();
    static String foxml;
    /** Creates a new instance of FedoraExporter */
    public FedoraPublisher() {
    }
    
    public static void uploadMap(edu.tufts.vue.dsm.DataSource ds, LWMap map) throws Exception {
        addObjectToRepository(ds,VUE_CM,map.getFile(),map,map);
    }
    
    public static void uploadMapAll(edu.tufts.vue.dsm.DataSource ds, LWMap map) throws Exception{
        Properties properties = ds.getConfiguration();
        LWMap cloneMap = (LWMap)map.clone();
        cloneMap.setLabel(map.getLabel());
        Iterator i = cloneMap.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource() && (component instanceof LWNode || component instanceof LWLink) && (component.getResource() instanceof URLResource)){
                URLResource resource = (URLResource) component.getResource();
                if(resource.isLocalFile()) {
                    String pid = getFedoraPid(component);
                    File file = new File(resource.getSpec().replace(FILE_PREFIX,""));
                    addObjectToRepository(ds,OTHER_CM, file,  component, map);
                    String ingestUrl = HTTP+"://"+properties.getProperty("fedora22Address")+":"+properties.getProperty("fedora22Port")+FEDORA_URL_PATH+"get/"+pid+"/"+RESOURCE_DS;
                }
            }
        }
        uploadMap(ds,map);
    }
    
    
    public static void uploadObjectToRepository(String protocol, String host, int port, String userName, String password,File file, File contentModel,String cm,String mimeType,String pid,String label,String dsName,LWComponent component,LWMap map) throws Exception {
        System.setProperty("javax.net.ssl.trustStore", VueUtil.getDefaultUserFolder()+File.separator+"truststore");
        System.setProperty("javax.net.ssl.trustStorePassword","tomcat");
        System.out.println("Trustore path(UOR):"+System.getProperty("javax.net.ssl.trustStore"));
        System.out.println("Trustore password(UOR):"+System.getProperty("javax.net.ssl.trustStorePassword"));
        
        FedoraClient fc = new FedoraClient(protocol+"://"+host+":"+port+"/fedora/", userName, password);
        AutoFinder af = new AutoFinder(fc.getAPIA());
        FieldSearchQuery query =  new FieldSearchQuery();
        Condition conds[] = new Condition[1];
        conds[0] = new  Condition(); //"pid",ComparisonOperator.eq,mapPid);
        conds[0].setProperty("pid");
        conds[0].setOperator(ComparisonOperator.eq);
        conds[0].setValue(pid);
        query.setConditions(conds);
        String dcXML= getDC(component,label,pid);
        FieldSearchResult result = af.findObjects(RESULT_FIELDS,1,query);
        Uploader uploader = new Uploader(protocol, host, port, userName, password);
        String uploadId = uploader.upload(file);
        if(result.getResultList().length  >0 ) {
            // updating the content
            fc.getAPIM().modifyDatastreamByReference(pid, dsName,  null,label, mimeType, VUE_FORMAT_URL, uploadId, null,null,  COMMENT,true);
            //updating the DC metadata
            fc.getAPIM().modifyDatastreamByValue(pid,DC_DS,null,DC_LABEL,XML_MIME_TYPE,DC_URL,dcXML.getBytes(),null,null,COMMENT,true);
            //update the rels stream
            if(!cm.equals(VUE_CM)) {
                System.out.println("RDF: "+getRDFDescriptionForLWComponent(component,map));
                fc.getAPIM().modifyDatastreamByValue(pid,RELS_DS,null,RELS_LABEL,XML_MIME_TYPE,RELS_URL, getRDFDescriptionForLWComponent(component,map).getBytes(),null,null,COMMENT,true);
            }
        } else {
            // reading the foxml if it doesn't exist
            if(!foxmls.containsKey(contentModel)) {
                BufferedReader r = new BufferedReader(new FileReader(contentModel));
                String line = new String();
                foxml = "";
                while((line = r.readLine())!=null) {
                    foxml += line+"\n";
                }
                foxmls.put(contentModel.toString(),foxml);
            } else {
                foxml = foxmls.get(contentModel.toString());
            }
            
            SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
            Date currentTime = new Date();
            String dateString = formatter.format(currentTime);
            String ingestFoxml = foxml;
            String relsExt = "";// getRels(cm,component,map);
            ingestFoxml = ingestFoxml.replace(RELS_EXT_KEY,relsExt);
            ingestFoxml = ingestFoxml.replace("%OWNER%",userName);
            ingestFoxml = ingestFoxml.replaceAll("%CREATE_DATE%",dateString);
            ingestFoxml = ingestFoxml.replaceAll("%PID%",pid);
            ingestFoxml = ingestFoxml.replace("%TITLE%",label);
            ingestFoxml = ingestFoxml.replace("%CONTENT_MODEL%",cm);
            ingestFoxml = ingestFoxml.replace("%UPLOAD%",uploadId);
            ingestFoxml = ingestFoxml.replace("%DC%",dcXML);
            ingestFoxml = ingestFoxml.replace("%DC_ID%",DC_DS);
            ingestFoxml = ingestFoxml.replace("%DC_LABEL%",DC_LABEL);
            ingestFoxml = ingestFoxml.replace("%DC_MIME_TYPE%",XML_MIME_TYPE);
            ingestFoxml = ingestFoxml.replace("%MIME_TYPE%",mimeType);
            System.out.println(ingestFoxml);
            //BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\temp\\IngestTest.xml"));
            //writer.write(ingestFoxml);
            //writer.close();
            StringBufferInputStream s = new StringBufferInputStream(ingestFoxml);
            AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), s,FORMAT, COMMENT);
            
        }
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
        //BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\temp\\IngestTest.xml"));
        //writer.write(ingestFoxml);
        //writer.close();
        //System.out.println("INGEST XML:\n"+ingestFoxml);
        StringBufferInputStream s = new StringBufferInputStream(ingestFoxml);
        AutoIngestor.ingestAndCommit(fc.getAPIA(), fc.getAPIM(), s,FORMAT, COMMENT);
    }
    private static void modifyObject(FedoraClient fc,Properties p, String cModel,File file,LWComponent comp,LWMap map) throws Exception{
        String pid = getFedoraPid(comp);
        String dsName = RESOURCE_DS;
        String mimeType = (new MimetypesFileTypeMap().getContentType(file));
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
        }else {
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
        SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
        Date currentTime = new Date();
        String dateString = formatter.format(currentTime);
        String uploadId = new String();
        String pid = getFedoraPid(comp);
        String dsName = RESOURCE_DS;
        String mimeType = (new MimetypesFileTypeMap().getContentType(file));
        String controlGroup = "M";
        String contentLocationType = "INTERNAL_ID";
        if(cModel.equals(VUE_CM)) {
            dsName = VUE_DS;
            mimeType =VUE_MIME_TYPE;
        }
        if(!cModel.equals(REMOTE_CM)){
            Uploader uploader = new Uploader(HTTPS, p.getProperty("fedora22Address"),Integer.parseInt(p.getProperty("fedora22SecurePort")),p.getProperty("fedora22UserName"), p.getProperty("fedora22Password"));
            uploadId = uploader.upload(file);
        } else {
            controlGroup = "E";
            contentLocationType = "URL";
        }
        
        StringBuffer xml = new StringBuffer();
        xml.append("<foxml:datastream CONTROL_GROUP=\""+controlGroup+"\" ID=\""+dsName+"\" STATE=\"A\" VERSIONABLE=\"true\">\n");
        xml.append("<foxml:datastreamVersion CREATED=\""+dateString+"\" FORMAT_URI=\"http://vue.tufts.edu/docs/vueformat/\"");
        xml.append(" ID=\""+dsName+".0\" LABEL=\""+comp.getLabel()+"\" MIMETYPE=\""+mimeType+"\">\n");
        xml.append("<foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>\n");
        xml.append("<foxml:contentLocation  REF=\""+uploadId+"\" TYPE=\""+contentLocationType+"\"/>\n");
        xml.append("</foxml:datastreamVersion></foxml:datastream>\n");
        return xml.toString();
    }
    
    private static String getDCXML(LWComponent comp) {
        SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
        Date currentTime = new Date();
        String dateString = formatter.format(currentTime);
        StringBuffer xml = new StringBuffer();
        xml.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"DC\" STATE=\"A\" VERSIONABLE=\"true\">\n");
        xml.append("<foxml:datastreamVersion CREATED=\""+dateString+"\" ID=\"DC1.0\" LABEL=\"Dublin Core Metadata\" MIMETYPE=\"text/xml\">\n");
        xml.append("<foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>\n");
        xml.append(" <foxml:xmlContent>\n");
        xml.append(getDC(comp,comp.getLabel(),getFedoraPid(comp)));
        xml.append("</foxml:xmlContent></foxml:datastreamVersion></foxml:datastream>\n");
        return xml.toString();
    }
    public static String getDC(LWComponent c,String title,String identifier) {
        String dc = new String();
        dc +="<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">";
        dc += "<dc:title>"+title+"</dc:title>";
        dc += "<dc:identifier>"+identifier+"</dc:identifier>";
        for(VueMetadataElement element: c.getMetadataList().getMetadata()) {
            if(element.getKey().contains(DC_URL)) {
                String key = "dc:"+element.getKey().substring(DC_URL.length()+1);
                dc += "<"+key+">"+element.getValue()+"</"+key+">";
            }
        }
        dc += "</oai_dc:dc>";
        return dc;
    }
    
    public static String getRelsXML(LWComponent comp,LWMap map) throws Exception  {
        SimpleDateFormat  formatter =  new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
        Date currentTime = new Date();
        String dateString = formatter.format(currentTime);
        StringBuffer xml = new StringBuffer();
        xml.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\" STATE=\"A\" VERSIONABLE=\"true\">\n");
        xml.append("<foxml:datastreamVersion CREATED=\""+dateString+"\" ID=\"RELS-EXT.0\" LABEL=\"Relationships to other objects\" MIMETYPE=\"text/xml\">\n");
        xml.append("<foxml:xmlContent>\n");
        xml.append(getRDFDescriptionForLWComponent(comp,map));
        xml.append("</foxml:xmlContent></foxml:datastreamVersion></foxml:datastream>");
        return xml.toString();
        
    }
    
    
    public static String getRDFDescriptionForLWComponent(LWComponent comp,LWMap map) {
        String rdfDescription = new String();
        rdfDescription = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">";
        rdfDescription += "<rdf:Description rdf:about=\"info:fedora/"+getFedoraPid(comp)+"\">";
        for(LWComponent c: map.getAllDescendents(LWComponent.ChildKind.PROPER)) {
            for(VueMetadataElement element: c.getMetadataList().getMetadata()) {
                if(element.getKey().equals(ONT_TYPE_METADATA) && element.getValue().startsWith(FEDORA_ONTOLOGY) && c instanceof LWLink){
                    LWLink link = (LWLink)c;
                    LWComponent head = link.getHead();
                    if(head == comp) {
                        LWComponent tail = link.getTail();
                        rdfDescription +=  "<rel:"+getFedoraOntologyTerm(element.getValue())+" rdf:resource=\"info:fedora/"+getFedoraPid(tail)+"\" />";
                    }
                }
            }
        }
        rdfDescription +=  "<rel:IsPartOf rdf:resource=\"info:fedora/"+getFedoraPid(map)+"\" />";
        rdfDescription += "</rdf:Description>";
        rdfDescription +=      "</rdf:RDF>";
        return rdfDescription;
    }
    
    public static String getFedoraPid(LWComponent component) {
        return "vue:"+component.getURIString().substring(component.getURIString().lastIndexOf("/")+1);
    }
    
    public static String getFedoraOntologyTerm(String value) {
        String term = new String();
        term = value.substring(FEDORA_ONTOLOGY.length());
        term = term.replaceAll(" ","");
        return term;
    }
}
