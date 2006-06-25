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
    private static final File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
    private static String  xmlFilename  = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
	
	// cache of already instantiated data sources
	private static java.util.Map cacheMap = new java.util.HashMap();
	
    public static edu.tufts.vue.dsm.DataSourceManager getInstance() 
	{
        load();
        return dataSourceManager;
    }

    public VueDataSourceManager() 
	{
    }
    
	public void addDataSourceToCache(org.osid.shared.Id providerId,
									 edu.tufts.vue.dsm.DataSource dataSource)
	{
		try {
			String s = providerId.getIdString();	
			cacheMap.put(s,dataSource);
		} catch (Throwable t) {
			
		}
	}
	
	public edu.tufts.vue.dsm.DataSource getDataSourceFromCache(org.osid.shared.Id providerId)
	{
		try {
			String s = providerId.getIdString();	
			if (s != null) {
				return (edu.tufts.vue.dsm.DataSource)cacheMap.get(providerId.getIdString());
			}
		} catch (Throwable t) {
		}
		return null;
	}
	    
    public void save() {
        marshall(new File(this.xmlFilename), this);
    }
    
    public static  void load() {
        try {
            File f = new File(xmlFilename);
            if (f.exists()) {
                dataSourceManager = unMarshall(f);
            } else {
                System.out.println("Installed datasources not found");
            }
        }  catch (Throwable t) {
            System.out.println("VueDataSourceManager.load: "+t) ;
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
            org.osid.shared.Id dataSourceId = dataSource.getId();
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                if (dataSourceId.isEqual(ds.getId())) {
                    // duplicate, no error
                    edu.tufts.vue.util.Logger.log("cannot add a data source with an id already in use");
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
    public void remove(org.osid.shared.Id dataSourceId) {
        try {
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                if (dataSourceId.isEqual(ds.getId())) {
					this.dataSourceVector.removeElementAt(i);
                    save();
                }
            }
        } catch (Throwable t) {
            
        }
    }
    
    /**
     */
    public edu.tufts.vue.dsm.DataSource getDataSource(org.osid.shared.Id dataSourceId) {
        try {
            for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
                if (dataSourceId.isEqual(ds.getId())) {
                    return ds;
                }
            }
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log("no datasource with id found");
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