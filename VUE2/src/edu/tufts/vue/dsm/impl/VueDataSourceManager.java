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

import tufts.Util;

import org.osid.repository.Repository;
import edu.tufts.vue.dsm.DataSource;
import edu.tufts.vue.dsm.DataSourceListener;

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

    /**
     * This set will only maintain uniqueness based on the hashCode, which is currently
     * defaulting to the System.identityHashCode for known implemented data sources
     * (VueDataSource's).  This is fine for our purposes now, tho a more complete impl
     * would require DataSource.hashCode() to return the getId().getIdString() (and then
     * we could also use a HashMap for referencing by ID string).
     *
     * We need a set because upon creation, VueDataSources always attempt to add
     * themselves to the global data source list (in setDone, called by castor during
     * unmarshalling).  Don't know if we need that behavior, but this impl allows that
     * to happen w/out putting duplicates in the global list.
     *
     * -- SMF 10/2007
     */
    
    private static final Set<DataSource> DataSources = new HashSet();

    /** This is only used marshalling and unmarshalling */
    private static final Vector<DataSource> dataSourceVector = new Vector();
    
    private static edu.tufts.vue.dsm.DataSourceManager dataSourceManager = new VueDataSourceManager();

    private static final File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
    private static final String xmlFilename  = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
    private static final List<edu.tufts.vue.dsm.DataSourceListener> dataSourceListeners = new ArrayList<edu.tufts.vue.dsm.DataSourceListener>();
    
    private static volatile boolean marshalling = false;
    
    public static edu.tufts.vue.dsm.DataSourceManager getInstance() {
        return dataSourceManager;
    }
    
    /** this is public for castor persistance support only */
    public VueDataSourceManager() {}
    
    public void save() {
        notifyDataSourceListeners();
        marshall(new File(this.xmlFilename), this);
    }
    
    public static void load() {
        try {
            File f = new File(xmlFilename);
            if (f.exists()) {

                // Note: this class has no member variables, so assigning the new
                // dataSourceManager here has no effect -- it might as well be a final
                // constant for now (e.g., getInstance() isn't needed at the moment).
                // However, should member variables be added in the future, we'll need
                // the existing semantics.
                
                dataSourceManager = unMarshall(f); 
                dataSourceManager.notifyDataSourceListeners();
            } else {
                debug("Installed datasources not found");
            }
        }  catch (Throwable t) {
            tufts.vue.VueUtil.alert("Error instantiating Provider support","Error");
            Log.warn("In load via Castor", t);
        }
    }
    
    public edu.tufts.vue.dsm.DataSource[] getDataSources() {
        synchronized (DataSources) {
            return DataSources.toArray(new DataSource[DataSources.size()]);
        }
    }
    
    /**
     * Add the given data source to the global list if it isn't already there.
     * Will immediately save if it was added.
     */
    public void add(edu.tufts.vue.dsm.DataSource dataSource) {

        synchronized (DataSources) {
            if (DataSources.add(dataSource)) {
                Log.info("add data src: " + dataSource);
                //Log.info("added data source: " + dataSource);
                if (!marshalling)
                    save();
            }
        }
    }
    
    /**
     * Add the given data source to the global list if it's there (any matching the id, tho there should be only one).
     * Will immediately save if anything was removed.
     */

    public void remove(org.osid.shared.Id id) {

        if (id == null) {
            Util.printStackTrace(this + ".remove: null id");
            return;
        }

        synchronized (DataSources) {
            boolean removed = false;
            try {
                for (DataSource ds : DataSources) {
                    if (id.isEqual(ds.getId())) {
                        DataSources.remove(ds);
                        if (removed)
                            Log.warn("removed again: " + ds);
                        else
                            Log.info("removed: " + ds);
                        removed = true;
                        // should be able to break, but just in case different instances have same ID..
                    }
                }
            } catch (Throwable t) {
                Log.error("remove " + Util.tags(id), t);
            }

            if (removed && !marshalling)
                save();
        }
    }
    
    /**
     */
    public edu.tufts.vue.dsm.DataSource getDataSource(org.osid.shared.Id dataSourceId) {

        try {
            synchronized (DataSources) {
                for (DataSource ds : DataSources)
                    if (dataSourceId.isEqual(ds.getId()))
                        return ds;
            }
        } catch (Throwable t) {
            Log.warn("found no DataSource with id " + Util.tags(dataSourceId));
        }
        return null;
    }
    
    /**
     */
    public org.osid.repository.Repository[] getIncludedRepositories() {

        final List<Repository> included = new ArrayList();

        for (DataSource ds : DataSources)
            if (ds.isIncludedInSearch())
                included.add(ds.getRepository());

        return included.toArray(new Repository[included.size()]);
        
//         removeDuplicatesFromVector();
//         java.util.Vector results = new java.util.Vector();
//         int size = dataSourceVector.size();
//         for (int i=0; i < size; i++) {
//             edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
//             if (ds.isIncludedInSearch()) {
//                 try {
//                     debug("Getting included data sourceA " + tufts.Util.tag(ds));
//                     debug("Getting included data sourceB " + tufts.Util.tags(ds.getId()));
//                     debug("Getting included data source0 " + ds.getId().getIdString());
//                     debug("Getting included data source1 " + ds.getRepository());
//                     debug("Getting included data source2 " + ds.getRepository().getDisplayName());
//                     debug("Getting included data source3 " + ds.getRepository().getId().getIdString());
//                 } catch (Throwable t) {
//                 }
//                 results.addElement(ds.getRepository());
//             }
//         }
//         size = results.size();
//         org.osid.repository.Repository repositories[] = new org.osid.repository.Repository[size];
//         for (int i=0; i < size; i++) {
//             repositories[i] = (org.osid.repository.Repository)results.elementAt(i);
//         }
//         return repositories;
        
    }

    private static void debug(String s) {
        Log.info(s);
    }
    
    public edu.tufts.vue.dsm.DataSource[] getIncludedDataSources() {

        final List<DataSource> included = new ArrayList();

        synchronized (DataSources) {
            for (DataSource ds : DataSources)
                if (ds.isIncludedInSearch())
                    included.add(ds);
        }

        return included.toArray(new DataSource[included.size()]);
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
    
    /** for castor persistance only -- should not be used for fetching the data source list
     * This will normally return null except during marshalling */
    public Vector getDataSourceVector() {
        return marshalling ? dataSourceVector : null;
    }
    
//     public void setDataSourceVector(Vector dsv) {
//         dataSourceVector = dsv;
//     }
    
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
    
    public static synchronized void marshall(File file,VueDataSourceManager dsm)
    {
        //System.out.println("Marshalling: file -"+ file.getAbsolutePath());

        dataSourceVector.clear();
        synchronized (DataSources) {
            dataSourceVector.addAll(DataSources);
        }

        marshalling = true;
        try {
            Mapping mapping = tufts.vue.action.ActionUtil.getDefaultMapping();
            FileWriter writer = new FileWriter(file);
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            Log.debug("Marshalling to " + file + "...");
            marshaller.marshal(dsm);
            writer.flush();
            writer.close();
            Log.info(" Marshalled to " + file);
        } catch (Throwable t) {
            Log.error("marshall to " + file, t);
        } finally {
            marshalling = false;
        }
    }
    
    public static synchronized VueDataSourceManager unMarshall(File file)
        throws java.io.IOException, org.exolab.castor.xml.MarshalException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.ValidationException
    {
        //System.out.println("UnMarshalling: file -"+ file.getAbsolutePath());
        
        Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller(file.toString());
        
        VueDataSourceManager dsm = null;
        marshalling = true;
        try {
            
            final FileReader reader = new FileReader(file);
            dataSourceVector.clear();
            dsm = (VueDataSourceManager) unmarshaller.unmarshal(new InputSource(reader));
            reader.close();

            // Note that we do NOT clear DataSources here -- thus multiple unmarshalls
            // may load DataSources with multiple instances (as we're currently
            // only unique via identity hash code) -- this was the old behavior.
            // Si if we were ever to unmarshall more than once during a runtime, the impl
            // will need to handle full uniqueness based on ID, or again we'll
            // get repeats, even in the DataSources set.
            
            synchronized (DataSources) {
                for (DataSource ds : dataSourceVector) {
                    try {
                        debug("Unmarshalled: " + ds + "; RepositoryID=" + ds.getRepository().getId().getIdString());
                    } catch (Throwable t) {
                        Log.warn("Unmarshalling", t);
                    }
                    DataSources.add(ds);
                }
            }
        } finally {
            marshalling = false;
        }
        
//         int size = dataSourceVector.size();
//         for (int i=0; i < size; i++) {
//             edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
//             try {
//                 debug("Unmarshalled data sourceA #" + i + " " + tufts.Util.tag(ds));
//                 debug("Unmarshalled data sourceB " + tufts.Util.tags(ds.getId()));
//                 debug("Unmarshalled data source0 " + ds.getId().getIdString());
//                 debug("Unmarshalled data source1 " + ds.getRepository());
//                 debug("Unmarshalled data source2 " + ds.getRepository().getDisplayName());
//                 debug("Unmarshalled data source3 " + ds.getRepository().getId().getIdString());
//             } catch (Throwable t) {
//             }
//         }
	
        return dsm;
    }

}
