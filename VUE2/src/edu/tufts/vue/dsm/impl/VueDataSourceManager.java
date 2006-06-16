package edu.tufts.vue.dsm.impl;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 * This class loads and saves Data Source content from an XML file
 */
import java.io.*;
import java.util.*;
import java.net.*;

//classes to support marshalling and unmarshalling
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;

public class VueDataSourceManager
        implements edu.tufts.vue.dsm.DataSourceManager {
    private static edu.tufts.vue.dsm.DataSourceManager dataSourceManager = new VueDataSourceManager();
    private java.util.Vector dataSourceVector = new java.util.Vector();
    private final static String XML_MAPPING_CURRENT_VERSION_ID = tufts.vue.VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT = tufts.vue.VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    private static final String FILE_NOT_FOUND_MESSAGE = "Cannot find or open ";
    private static final String OKI_NAMESPACE = "xmlns:oki";
    private static final String OKI_NAMESPACE_URL = "http://www.okiproject.org/registry/elements/1.0/" ;
    private static final String REGISTRY_TAG = "registry";
    private static final String RECORDS_TAG = "records";
    private static final String PROVIDER_RECORD_TAG = "record";
    private static final String PROVIDER_ID_TAG = "oki:providerid";
    private static final String OSID_SERVICE_TAG = "oki:osidservice";
    private static final String OSID_MAJOR_VERSION_TAG = "oki:osidmajorversion";
    private static final String OSID_MINOR_VERSION_TAG = "oki:osidminorversion";
    private static final String OSID_LOAD_KEY_TAG = "oki:osidloadkey";
    private static final String DISPLAY_NAME_TAG = "oki:displayname";
    private static final String DESCRIPTION_TAG = "oki:description";
    private static final String CREATOR_TAG = "oki:creator";
    private static final String PUBLISHER_TAG = "oki:publisher";
    private static final String PUBLISHER_URL_TAG = "oki:publisherurl";
    private static final String PROVIDER_MAJOR_VERSION_TAG = "oki:providermajorversion";
    private static final String PROVIDER_MINOR_VERSION_TAG = "oki:providerminonrversion";
    private static final String RELEASE_DATE_TAG = "oki:releasedate";
    private static final String RIGHTS_TAG = "oki:rights";
    private static final String RIGHT_TAG = "oki:rightentry";
    private static final String RIGHT_TYPE_TAG = "oki:righttype";
    private static final String README_TAG = "oki:readme";
    private static final String REPOSITORY_ID_TAG = "oki:repositoryid";
    private static final String REPOSITORY_IMAGE_TAG = "oki:repositoryimage";
    private static final String REGISTRATION_DATE_TAG = "oki:registrationdate";
    private static final String HIDDEN_TAG = "oki:hidden";
    private static final String CONFIGURATIONS_TAG = "oki:configuration";
    private static final String CONFIGURATION_KEY_TAG = "oki:configurationkey";
    
    private static final String INCLUDED_IN_SEARCH_TAG = "oki:includedinsearch";
    private static final File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
    private static String  xmlFilename  = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
	
	// cache of already instantiated data sources
	private static java.util.Map cacheMap = new java.util.HashMap();
	
    public static edu.tufts.vue.dsm.DataSourceManager getInstance() {
        load();
        return dataSourceManager;
    }
    public VueDataSourceManager() {
    }
    
	public void addDataSourceToCache(org.osid.shared.Id providerId,
								DataSource dataSource)
	{
		cacheMap.put(providerId.getIdString(),dataSource);
	}
	
	public DataSourcegetDataSourceFromCache(org.osid.shared.Id providerId)
	{
		return cacheMap.get(providerId.getIdString());
	}
	
    public void refresh() {
        try {
            java.io.InputStream istream = new java.io.FileInputStream(this.xmlFilename);
            if (istream == null) {
                // assume there are no data sources saved
                System.out.println("no file " + xmlFilename);
                return;
            }
            
            javax.xml.parsers.DocumentBuilderFactory dbf = null;
            javax.xml.parsers.DocumentBuilder db = null;
            org.w3c.dom.Document document = null;
            
            dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            document = db.parse(istream);
            
            org.w3c.dom.NodeList records = document.getElementsByTagName(PROVIDER_RECORD_TAG);
            int numRecords = records.getLength();
            for (int i=0; i < numRecords; i++) {
                org.osid.shared.Id providerId = null;
                boolean isIncludedInSearch = false;
                
                org.w3c.dom.Element record = (org.w3c.dom.Element)records.item(i);
                org.w3c.dom.NodeList nodeList = record.getElementsByTagName(PROVIDER_ID_TAG);
                int numNodes = nodeList.getLength();
                for (int k=0; k < numNodes; k++) {
                    org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
                    if (e.hasChildNodes()) {
                        String providerIdString = e.getFirstChild().getNodeValue();
                        providerId = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(providerIdString);
                    }
                }
                nodeList = record.getElementsByTagName(INCLUDED_IN_SEARCH_TAG);
                numNodes = nodeList.getLength();
                for (int k=0; k < numNodes; k++) {
                    org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
                    if (e.hasChildNodes()) {
                        String includedString = e.getFirstChild().getNodeValue();
                        isIncludedInSearch = (new Boolean(includedString)).booleanValue();
                    }
                }
                
                // if we already have this data source, update it in place
                boolean found = false;
                for (int x=0, sizex = this.dataSourceVector.size(); x < sizex; x++) {
                    edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(x);
                    if (ds.getProviderId().isEqual(providerId)) {
                        found = true;
                        ds.setProviderId(providerId);
                        ds.setIncludedInSearch(isIncludedInSearch);
                        
                    }
                }
                if (!found) {
                    System.out.println("data source not found, adding to vector");
                    edu.tufts.vue.dsm.impl.VueDataSource vds = (new edu.tufts.vue.dsm.impl.VueDataSource(providerId,
                            isIncludedInSearch));
                    // simple check that all is working
                    if (vds.getRepositoryDisplayName() != null) {
                        this.dataSourceVector.addElement(vds);
                    } else {
                        System.out.println("Some problem loading data source");
                    }
                }
            }
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
        }
        
    }
    
    public void save() {
        marshall(new File(this.xmlFilename), this);
    }
    
    public static  void load() {
        try {
            File f = new File(xmlFilename);
            if (f.exists()) {
                //  If there is no InstalledDataSources file or no Extensions file, create them
                dataSourceManager = unMarshall(f);
            } else {
                System.out.println("Installed datasources not found");
            }
        }  catch (Throwable t) {
            System.out.println("VueDataSourceManager.load: "+t) ;
        }
    }
    
    public void saveOld() {
        try {
            java.io.File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
            this.xmlFilename = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
            
            javax.xml.parsers.DocumentBuilderFactory dbf = null;
            javax.xml.parsers.DocumentBuilder db = null;
            org.w3c.dom.Document document = null;
            
            dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            document = db.newDocument(); // always rewrite complete file
            
            org.w3c.dom.Element top = document.createElement(REGISTRY_TAG);
            org.w3c.dom.Element records = document.createElement(RECORDS_TAG);
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource dataSource = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                
                org.w3c.dom.Element record = document.createElement(PROVIDER_RECORD_TAG);
                record.setAttribute(OKI_NAMESPACE,OKI_NAMESPACE_URL);
                
                String nextValue = dataSource.getProviderId().getIdString();
                org.w3c.dom.Element e;
                if (nextValue != null) {
                    e = document.createElement(PROVIDER_ID_TAG);
                    e.appendChild(document.createTextNode(nextValue));
                    record.appendChild(e);
                }
                nextValue = (dataSource.isIncludedInSearch()) ? "true" : "false";
                if (nextValue != null) {
                    e = document.createElement(INCLUDED_IN_SEARCH_TAG);
                    e.appendChild(document.createTextNode(nextValue));
                    record.appendChild(e);
                }
                
                records.appendChild(record);
            }
            
            top.appendChild(records);
            document.appendChild(records);
            
            javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            java.util.Properties properties = new java.util.Properties();
            properties.put("indent","yes");
            transformer.setOutputProperties(properties);
            javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
            javax.xml.transform.stream.StreamResult result =
                    new javax.xml.transform.stream.StreamResult(this.xmlFilename);
            transformer.transform(domSource,result);
            
            refresh();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"saving data sources to XML");
        }
    }
    
    public edu.tufts.vue.dsm.DataSource[] getDataSources() {
        int size = this.dataSourceVector.size();
        edu.tufts.vue.dsm.DataSource dataSources[] = new edu.tufts.vue.dsm.DataSource[size];
        for (int i=0; i < size; i++) dataSources[i] = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
        return dataSources;
    }
    
    /**
     */
    public void add(edu.tufts.vue.dsm.DataSource dataSource) {
        try {
            // we have to worry about duplicates
            org.osid.shared.Id providerId = dataSource.getProviderId();
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                if (providerId.isEqual(ds.getProviderId())) {
                    // duplicate, no error
                    edu.tufts.vue.util.Logger.log("cannot add a data source with a provider id already in use");
                    return;
                }
            }
            this.dataSourceVector.addElement(dataSource);
            save();
        } catch (Throwable t) {
            
        }
    }
    
    /**
     */
    public void remove(edu.tufts.vue.dsm.DataSource dataSource) {
        try {
            org.osid.shared.Id providerId = dataSource.getProviderId();
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                if (providerId.isEqual(ds.getProviderId())) {
                    this.dataSourceVector.removeElementAt(i);
                    save();
                }
            }
        } catch (Throwable t) {
            
        }
    }
    
    /**
     */
    public edu.tufts.vue.dsm.DataSource getDataSource(org.osid.shared.Id repositoryId) {
        try {
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                if (repositoryId.isEqual(ds.getRepositoryId())) {
                    return ds;
                }
            }
        } catch (Throwable t) {
            
        }
        return null;
    }
    
    /**
     */
    public org.osid.repository.Repository[] getIncludedRepositories() {
        java.util.Vector results = new java.util.Vector();
        int size = this.dataSourceVector.size();
        for (int i=0; i < size; i++) {
            edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
            if (ds.isIncludedInSearch()) {
                results.addElement(ds.getRepository());
            }
        }
        size = results.size();
        org.osid.repository.Repository repositories[] = new org.osid.repository.Repository[size];
        for (int i=0; i < size; i++) {
            repositories[i] = (org.osid.repository.Repository)results.elementAt(i);
        }
        return repositories;
    }
    
    /**
     */
    public java.awt.Image getImageForRepositoryType(org.osid.shared.Type repositoryType) {
        return null;
    }
    
    /**
     */
    public java.awt.Image getImageForSearchType(org.osid.shared.Type searchType) {
        return null;
    }
    
    /**
     */
    public java.awt.Image getImageForAssetType(org.osid.shared.Type assetType) {
        return null;
    }
    
    public Vector getDataSourceVector() {
        return this.dataSourceVector;
    }
    
    public void setDataSourceVector(Vector dataSourceVector) {
        this.dataSourceVector = dataSourceVector;
    }
    
    public  static void marshall(File file,VueDataSourceManager dsm) {
        Marshaller marshaller = null;
        Mapping mapping = new Mapping();
        
        try {
            FileWriter writer = new FileWriter(file);
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING_DEFAULT);
            marshaller.setMapping(mapping);
            marshaller.marshal(dsm);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("VueDataSourceManager.marshall " + t.getMessage());
        }
    }
    
    
    public static  VueDataSourceManager unMarshall(File file) throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException{
        Unmarshaller unmarshaller = null;
        VueDataSourceManager dsm = null;
        Mapping mapping = new Mapping();
        unmarshaller = new Unmarshaller();
        unmarshaller.setIgnoreExtraElements(true);
        mapping.loadMapping(XML_MAPPING_DEFAULT);
        unmarshaller.setMapping(mapping);
        FileReader reader = new FileReader(file);
        dsm = (VueDataSourceManager) unmarshaller.unmarshal(new InputSource(reader));
        reader.close();
        return dsm;
    }
    
}