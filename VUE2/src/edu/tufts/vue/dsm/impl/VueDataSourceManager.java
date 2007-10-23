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
import edu.tufts.vue.dsm.DataSourceListener;
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
        implements edu.tufts.vue.dsm.DataSourceManager
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDataSourceManager.class);

    private static edu.tufts.vue.dsm.DataSourceManager dataSourceManager = new VueDataSourceManager();
    private static java.util.Vector dataSourceVector = new java.util.Vector();
    //private final static String XML_MAPPING_CURRENT_VERSION_ID = tufts.vue.VueResources.getString("mapping.lw.current_version");
    //private final static URL XML_MAPPING_DEFAULT = tufts.vue.VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    private static final File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
    private static String  xmlFilename  = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
    private static boolean marshalling = false;
    private static List<edu.tufts.vue.dsm.DataSourceListener> dataSourceListeners = new ArrayList<edu.tufts.vue.dsm.DataSourceListener>();
    public static edu.tufts.vue.dsm.DataSourceManager getInstance() {
        return dataSourceManager;
    }
    
    public VueDataSourceManager() {
    }
    
   public  void save() {
        notifyDataSourceListeners();
        marshall(new File(this.xmlFilename), this);
    }
    
    public static void load() {
        try {
			dataSourceVector = new java.util.Vector();
            File f = new File(xmlFilename);
            if (f.exists()) {
                dataSourceManager = unMarshall(f); 
                dataSourceManager. notifyDataSourceListeners();
            } else {
                debug("Installed datasources not found");
            }
        }  catch (Throwable t) {
//			t.printStackTrace();
//            tufts.vue.VueUtil.alert("Error instantiating Provider support","Error");
            Log.warn("In load via Castor " + t.getMessage());
        }
    }
    
	private void removeDuplicatesFromVector()
	{
		try {
			java.util.Vector v = new java.util.Vector();
			java.util.Vector idStringVector = new java.util.Vector();
			for (int i = 0; i < dataSourceVector.size(); i++) 
			{
				edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
				String idString = ds.getId().getIdString();
				if (!idStringVector.contains(idString)) {
					v.addElement(ds);
					idStringVector.addElement(idString);
				}
			}
			dataSourceVector = v;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
    public edu.tufts.vue.dsm.DataSource[] getDataSources() {
		// There appears to be a bug that causes duplicates in the vector
		// no idea why.  Calling a method to clear those out.
		removeDuplicatesFromVector();
        int size = dataSourceVector.size();
        edu.tufts.vue.dsm.DataSource dataSources[] = new edu.tufts.vue.dsm.DataSource[size];
		try {
			for (int i=0; i < size; i++) 
			{
				dataSources[i] = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
        return dataSources;
    }
    
    /**
     */
    public void add(edu.tufts.vue.dsm.DataSource dataSource) {
        try {
            // we have to worry about duplicates
            org.osid.shared.Id dataSourceId = dataSource.getId();
            for (int i=0, size = dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
                if (dataSourceId.isEqual(ds.getId())) {
                    // duplicate, no error
                    edu.tufts.vue.util.Logger.log("cannot add a data source with an id already in use");
                    return;
                }
            }
			try {				
				Log.info("Adding data source " + dataSource.getId().getIdString() + " " + dataSource.getRepository().getDisplayName());
			} catch (Throwable t) {
			}
            dataSourceVector.addElement(dataSource);
            if(!marshalling) save();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     */
    public void remove(org.osid.shared.Id dataSourceId) {
        try {
            for (int i=0, size = dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
                if (dataSourceId.isEqual(ds.getId())) {
                    dataSourceVector.removeElementAt(i);
                    if(!marshalling) save();
                }
            }
        } catch (Throwable t) {
            
        }
    }
    
    /**
     */
    public edu.tufts.vue.dsm.DataSource getDataSource(org.osid.shared.Id dataSourceId) {
        try {
            for (int i=0, size = dataSourceVector.size(); i < size; i++) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
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
		removeDuplicatesFromVector();
        java.util.Vector results = new java.util.Vector();
        int size = dataSourceVector.size();
        for (int i=0; i < size; i++) {
            edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
            if (ds.isIncludedInSearch()) {
				try {
					debug("Getting included data source0 " + ds.getId().getIdString());
					debug("Getting included data source1 " + ds.getRepository());
					debug("Getting included data source2 " + ds.getRepository().getDisplayName());
					debug("Getting included data source3 " + ds.getRepository().getId().getIdString());
				} catch (Throwable t) {
				}
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

    private static void debug(String s) {
        Log.info(s);
    }
    
	public edu.tufts.vue.dsm.DataSource[] getIncludedDataSources() {
		removeDuplicatesFromVector();
        java.util.Vector results = new java.util.Vector();
        int size = dataSourceVector.size();
        for (int i=0; i < size; i++) {
            edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
			if (ds.isIncludedInSearch()) {
                results.addElement(ds);
            }
        }
        size = results.size();
        edu.tufts.vue.dsm.DataSource dataSources[] = new edu.tufts.vue.dsm.DataSource[size];
        for (int i=0; i < size; i++) {
            dataSources[i] = (edu.tufts.vue.dsm.DataSource)results.elementAt(i);
        }
        return dataSources;
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
        return dataSourceVector;
    }
    
    public void setDataSourceVector(Vector dsv) {
        dataSourceVector = dsv;
    }
    
    public void addDataSourceListener(edu.tufts.vue.dsm.DataSourceListener listener) {
        dataSourceListeners.add(listener);
    }
    
    public void removeDataSourceListener(edu.tufts.vue.dsm.DataSourceListener listener) {
        if(dataSourceListeners.contains(listener))
            dataSourceListeners.remove(listener);
    }
    
    public  void notifyDataSourceListeners() {
        for(edu.tufts.vue.dsm.DataSourceListener listener: dataSourceListeners) {
            listener.changed(getDataSources());
        }
    }
    public  static void marshall(File file,VueDataSourceManager dsm) {
    //    System.out.println("Marshalling: file -"+ file.getAbsolutePath());
        Marshaller marshaller = null;
        //Mapping mapping = new Mapping();
        Mapping mapping = tufts.vue.action.ActionUtil.getDefaultMapping();
        marshalling = true;
        try {
            FileWriter writer = new FileWriter(file);
            marshaller = new Marshaller(writer);
            //mapping.loadMapping(XML_MAPPING_DEFAULT);
            marshaller.setMapping(mapping);
            marshaller.marshal(dsm);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("VueDataSourceManager.marshall " + t.getMessage());
        }
        marshalling = false;
    }
    
    public static  VueDataSourceManager unMarshall(File file) throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException {
     // System.out.println("UnMarshalling: file -"+ file.getAbsolutePath());
        
        Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller(file.toString());
        /*
        Unmarshaller unmarshaller = null;
        Mapping mapping = new Mapping();
        unmarshaller = new Unmarshaller();
        unmarshaller.setIgnoreExtraElements(true);
        mapping.loadMapping(XML_MAPPING_DEFAULT);
        unmarshaller.setMapping(mapping);
        */
        marshalling = true;
        FileReader reader = new FileReader(file);
        VueDataSourceManager dsm = (VueDataSourceManager) unmarshaller.unmarshal(new InputSource(reader));
				
		int size = dataSourceVector.size();
        for (int i=0; i < size; i++) {
            edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
			try {
				debug("Unmarshalled data source0 " + ds.getId().getIdString());
				debug("Unmarshalled data source1 " + ds.getRepository());
				debug("Unmarshalled data source2 " + ds.getRepository().getDisplayName());
				debug("Unmarshalled data source3 " + ds.getRepository().getId().getIdString());
			} catch (Throwable t) {
			}
		}
		
        reader.close();
        marshalling = false;
        return dsm;
    }
    
}
