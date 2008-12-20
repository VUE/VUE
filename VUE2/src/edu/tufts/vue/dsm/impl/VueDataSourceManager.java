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
package edu.tufts.vue.dsm.impl;

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

/**
 *
 * This class loads and saves Data Source content from an XML file, provides
 * for multi-threaded hang-proof initialization (repository configuration),
 * and event delivery to track the progress of loading.
 *
 * @version $Revision: 1.47 $ / $Date: 2008-12-20 20:05:21 $ / $Author: sfraize $  
 */
public class VueDataSourceManager
    implements edu.tufts.vue.dsm.DataSourceManager
{
    /** if true, all data sources will block until their repositories are found and configured,
     * which can result in a hang if there is a problem with any repository.  We're keeping this
     * flag until we're sure our more complex impl's (VUE init, UI updates, etc) for handling the
     * more complex non-blocking case are fully tested. SMF 2008-12-20 
     */
    public static final boolean BLOCKING_OSID_LOAD = false;
    
    /* states for DataSourceListener callbacks */
    public static final String DS_UNMARSHALLED = "DS_UNMARSHALLED";
    public static final String DS_CONFIGURED = "DS_CONFIGURED";
    public static final String DS_ERROR = "DS_ERROR";
    public static final String DS_ALL_CONFIGURED = "DS_ALL_CONFIGURED";
    public static final String DS_SAVING = "DS_SAVING";
    
    public static final String PASS = "pass"; // a key that cotains this string is encrypted.
    
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
    private static final Set<DataSource> DataSources = new LinkedHashSet();

    /** This is only used marshalling and unmarshalling */
    private static final Vector<DataSource> dataSourceVector = new Vector();
    
    private static VueDataSourceManager dataSourceManager = new VueDataSourceManager();

    private static final File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
    private static final String xmlFilename  = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
    private static final List<edu.tufts.vue.dsm.DataSourceListener> dataSourceListeners
        = new java.util.concurrent.CopyOnWriteArrayList<edu.tufts.vue.dsm.DataSourceListener>();
    
    private static volatile boolean marshalling = false;
    
    // Todo: this class doesn't always appear to be threadsafe.
    // And do we we really want getInstance to return an empty default DSM instance,
    // that changes after load is called?
    public static VueDataSourceManager getInstance() {
        return dataSourceManager;
    }
    
    /** this is public for castor persistance support only */
    public VueDataSourceManager() {}
    
    public void save() {
        // why do we notify on save? keeping this only in case something is
        // currently depending on this...
        notifyDataSourceListeners(DS_SAVING); 
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
                if (BLOCKING_OSID_LOAD) {
                    // they're already fully configured at this point
                    getInstance().notifyDataSourceListeners(DS_ALL_CONFIGURED);
                } else {
                    getInstance().notifyDataSourceListeners(DS_UNMARSHALLED);
                    // DS_ALL_CONFIGURED will come later
                }
                    
            } else {
                debug("Installed datasources not found");
            }
        }  catch (Throwable t) {
            tufts.vue.VueUtil.alert("Error instantiating Provider support","Error");
            Log.warn("In load via Castor", t);
        }
    }

    /**
     * Assign repository configurations to loaded DataSources.  Currently only handles
     * VueDataSource impls.  This will spawn a thread for each assignment, as the
     * RepositoryManager implementation for each source can possibly hang while
     * assigning configuration, and there's no need to hang the whole application during
     * startup for a single hung DataSource / Repository failing to respond.
     *
     * @param clientUI -- if non-null, repaint() will be called on this each time a
     * DataSource completes configuration as a very simple callback, in addition to
     * notifications to any DataSourceListeners
     */

    public void startRepositoryConfiguration(final java.awt.Component clientUI) {
        Log.info("configuring data sources; n=" + DataSources.size());
        
        final edu.tufts.vue.dsm.DataSource dataSources[] = getDataSources();

        final java.util.concurrent.atomic.AtomicInteger RunCount =
            new java.util.concurrent.atomic.AtomicInteger(dataSources.length);

        for (final edu.tufts.vue.dsm.DataSource ds : dataSources) {
            if (ds instanceof VueDataSource == false) {
                Log.warn("unhandled DataSource impl, cannot configure: " + Util.tags(ds));
                RunCount.decrementAndGet();
            }
        }
            
        for (final edu.tufts.vue.dsm.DataSource ds : dataSources) {

            if (ds instanceof VueDataSource == false)
                continue;
            
            Log.debug("configure: " + ds);
            
            new Thread(String.format("%s@%07x %8.8s",
                                     ds.getClass().getSimpleName(),
                                     System.identityHashCode(ds),
                                     ds.getRepositoryDisplayName()))
            {
                @Override public void run() {

                    try {
                        
                        try {
                        
                            ((VueDataSource)ds).assignRepositoryConfiguration();
                            
                            Log.info("configured " + ds);

                            if (ds.getRepository() == null)
                                notifyDataSourceListeners(DS_ERROR, ds);
                            else
                                notifyDataSourceListeners(DS_CONFIGURED, ds);

                            if (ds.getRepositoryDisplayName().startsWith("Conn")) // test
                            try { Thread.sleep(5000); } catch (Throwable t) {}

                        } catch (Throwable t) {
                            Log.error("configuration error: " + Util.tags(ds), t);
                            notifyDataSourceListeners(DS_ERROR, ds);
                        }
                    

                        // TODO: VueDataSource accessors accessed during client painting
                        // (in AWT thread) or are not fully thread-safe against the
                        // above assignRepositoryConfiguration in this config thread.
                        
                        if (clientUI != null)
                            clientUI.repaint();
                        //else Log.debug("config complete, no UI to update");
                    
                    } catch (Throwable t) {
                        
                        Log.error("configuring", t);
                        
                    } finally {
                        
                        if (RunCount.decrementAndGet() <= 0) {

                            // Whichever thread finishes last will deliver the DS_ALL_CONFIGURED
                            // event. If any thread should hang, this will never be delivered, so
                            // listeners should ideally be designed to provide at least some
                            // functionality based only on the delivery of the DS_CONFIGURED events
                            // as the come in.
                            
                            notifyDataSourceListeners(DS_ALL_CONFIGURED);
                        }
                        
                    }
                }
            }.start();
            
        }
    }
    
    
    /**
     * Return the list of found DataSources.  Will trigger a blocking file I/O load if none are
     * present.  Some or all of the returned DataSources may be in an "unconfigured" state (without
     * a repository), requiring a later call to startRepositoryConfiguration before they can be
     * used.
     */
    public edu.tufts.vue.dsm.DataSource[] getDataSources() {
        synchronized (DataSources) {
            if (DataSources.isEmpty())
                load();
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
                Iterator<DataSource> i = DataSources.iterator();
                while (i.hasNext()) {
                    DataSource ds = i.next();
                    if (id.isEqual(ds.getId())) {
                        i.remove();
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

        synchronized (DataSources) {
            for (DataSource ds : DataSources)
                if (ds.isIncludedInSearch()) {
                    Repository r = ds.getRepository();
                    if (r == null)
                        Log.debug("has no repository; not included: " + ds);
                    else
                        included.add(r);
                }
        }

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
        // threadsafe: is CopyOnWriteArrayList
        dataSourceListeners.add(listener);
    }
    
    public void removeDataSourceListener(edu.tufts.vue.dsm.DataSourceListener listener) {
        // threadsafe: is CopyOnWriteArrayList
        dataSourceListeners.remove(listener);
    }
    
    public void notifyDataSourceListeners(Object state) {
        notifyDataSourceListeners(state, null);
    }
    
    public synchronized void notifyDataSourceListeners(Object state, DataSource changed) {
        // synchronized so notifications from different threads are at least atomic
        // to the state of all DataSources for each notification batch
        final edu.tufts.vue.dsm.DataSource dataSources[] = getDataSources();
        for (edu.tufts.vue.dsm.DataSourceListener listener: dataSourceListeners) {
            try {
                listener.changed(dataSources, state, changed);
            } catch (Throwable t) {
                Log.error("DataSourceListener failure: " + Util.tags(listener), t);
            }
        }
    }
    
    public static synchronized void marshall(File file,VueDataSourceManager dsm)
    {
        //System.out.println("Marshalling: file -"+ file.getAbsolutePath());

        dataSourceVector.clear();
        synchronized (DataSources) {
            dataSourceVector.addAll(DataSources);
        }

        for (DataSource ds : dataSourceVector) {
            try {
                debug("Marshalling:  " + ds + "; RepositoryID=" + getRepositoryID(ds));
            } catch (Throwable t) {
                Log.warn("Marshalling:", t);
            }
        }
        

        marshalling = true;
        try {
            Mapping mapping = tufts.vue.action.ActionUtil.getDefaultMapping();
            FileWriter writer = new FileWriter(file);
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            marshaller.setMarshalListener(new PropertyEntryMarshalListener());
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

    public static String getRepositoryID(DataSource ds) {
        final Repository r = ds.getRepository();
        String id = "<null-repository>";
        if (r != null) {
            try {
                id = r.getId().getIdString();
            } catch (Throwable t) {
                id = "<repositoryID:" + t + ">";
            }
        }
        return id;
    }
        
    
    public static synchronized VueDataSourceManager unMarshall(File file)
        throws java.io.IOException, org.exolab.castor.xml.MarshalException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.ValidationException
    {
        Log.info("Unmarshalling: " + file);
        
        Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller(file.toString());
        unmarshaller.setUnmarshalListener(new PropertyEntryUnMarshalListener());
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
                    if (tufts.vue.DEBUG.DR) {
                        try {
                            Log.info("Unmarshalled: " + ds + "; RepositoryID=" + getRepositoryID(ds));
                        } catch (Throwable t) {
                            Log.warn("Unmarshalling:", t);
                        }
                    }
                    DataSources.add(ds);
                }
            }
        } finally {
            marshalling = false;
        }
        Log.debug("unmarshall: done.");
        
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
  class PropertyEntryMarshalListener implements org.exolab.castor.xml.MarshalListener {
       public  boolean preMarshal(java.lang.Object object) {
            if(object instanceof tufts.vue.PropertyEntry) {
                tufts.vue.PropertyEntry pe = (tufts.vue.PropertyEntry) object;
                if(pe.getEntryKey().toLowerCase().contains(VueDataSourceManager.PASS)) {
                     pe.setEntryValue(edu.tufts.vue.util.Encryption.encrypt(pe.getEntryValue().toString()));
                    
                }
            }
            return true;
        }
        
        public void postMarshal(java.lang.Object object)  {
             if(object instanceof tufts.vue.PropertyEntry) {
                tufts.vue.PropertyEntry pe = (tufts.vue.PropertyEntry) object;
                if(pe.getEntryKey().toLowerCase().contains(VueDataSourceManager.PASS)) {
                     pe.setEntryValue(edu.tufts.vue.util.Encryption.decrypt(pe.getEntryValue().toString()));
                   
                }
            }
        }
    }

  class PropertyEntryUnMarshalListener implements org.exolab.castor.xml.UnmarshalListener {
       public void unmarshalled(java.lang.Object object) {
            if(object instanceof tufts.vue.PropertyEntry) {
                tufts.vue.PropertyEntry pe = (tufts.vue.PropertyEntry) object;
                if(pe.getEntryKey().toLowerCase().contains(VueDataSourceManager.PASS)) {
                     pe.setEntryValue(edu.tufts.vue.util.Encryption.decrypt(pe.getEntryValue().toString())); 
                }
            }
        }
      public void  attributesProcessed(java.lang.Object object)  {
      
      }
       public void fieldAdded(java.lang.String fieldName, java.lang.Object parent, java.lang.Object child)  {
           
       }
       
       public void initialized(java.lang.Object object)  {
           
       }
    }
  

  