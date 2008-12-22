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
 * @version $Revision: 1.50 $ / $Date: 2008-12-22 21:39:23 $ / $Author: sfraize $  
 */
public class VueDataSourceManager
    implements edu.tufts.vue.dsm.DataSourceManager
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDataSourceManager.class);
    
    /** If true, all data sources will block until their repositories are found and
     * configured, which can result in a hang if there is a problem with any repository.
     * This is the original implementation, and remains as a fallback impl in case of
     * threading issues at startup, but VUE's relevant impl's should now fully handle
     * the multi-threaded dynamic event delivery required by the non-blocking case.
     */
    public static final boolean BLOCKING_OSID_LOAD = false;
    
    /* states for DataSourceListener callbacks */
    public static final String DS_UNMARSHALLED = "DS_UNMARSHALLED";
    public static final String DS_CONFIGURED = "DS_CONFIGURED";
    public static final String DS_ADDED = "DS_ADDED";
    public static final String DS_ERROR = "DS_ERROR";
    public static final String DS_ALL_CONFIGURED = "DS_ALL_CONFIGURED";
    public static final String DS_SAVING = "DS_SAVING";
    
    protected static final String PASS = "pass"; // a key that cotains this string is encrypted.
    
    private static final File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
    private static final String xmlFilename  = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
    
    private static final VueDataSourceManager singleton = new VueDataSourceManager(true);

    /**
     * This set will only maintain uniqueness based on the hashCode, which is currently
     * defaulting to the System.identityHashCode for known implemented data sources
     * (VueDataSource's).  This is fine for our purposes now, tho a more complete impl
     * would require DataSource.hashCode() to return the getId().getIdString() (and then
     * we could also use a HashMap for referencing by ID string).
     *
     * [ Old as of 12/22/08: We need a set because upon creation, VueDataSources always
     * attempt to add themselves to the global data source list (in setDone, called by
     * castor during unmarshalling).  Don't know if we need that behavior, but this impl
     * allows that to happen w/out putting duplicates in the global list. ]
     *
     * -- SMF 10/2007
     */
    private final Set<DataSource> DataSources;

    /** This is only used marshalling and unmarshalling */
    private final Vector<DataSource> marshallingVector = new Vector();
    
    private final List<edu.tufts.vue.dsm.DataSourceListener> dataSourceListeners
        = new java.util.concurrent.CopyOnWriteArrayList<edu.tufts.vue.dsm.DataSourceListener>();
    
    private volatile boolean isMarshalling;
    
    private boolean isLoaded;
    
    // Todo: this class doesn't always appear to be threadsafe.
    
    // Do we we really want getInstance to return an empty default DSM
    // instance, that changes after load is called? Currently this
    // doesn't matter, VDSM has no member variables -- all are static,
    // but it isn't a very clean way to do it.

    public static VueDataSourceManager getInstance() {
        return singleton;
    }
    
    /** this is public for castor persistance support only */
    public VueDataSourceManager() {
        isMarshalling = true;
        DataSources = null; // so will deliberately NPE if use is attempted
    }
    
    private VueDataSourceManager(boolean isSingleton) {
        DataSources = new LinkedHashSet();        
    }
    
    public synchronized void save() {
        // why do we notify on save? keeping this only in case something is
        // currently depending on this...
        notifyDataSourceListeners(DS_SAVING); 
        marshall(new File(this.xmlFilename));
    }

    
    public synchronized void reload() {
        isLoaded = false;
        load();
    }
    
    public synchronized void load() {

        if (isLoaded)
            return;

        try {

            // if (true) throw new Error("oh my"); // exception test
            
            final File file = new File(xmlFilename);
            if (file.exists()) {

                DataSources.clear();
                
                final VueDataSourceManager unmarshalled = unMarshall(file);

                // currently, we really only need the list of DataSources from
                // the unmarshalled VDSM instance -- we'd could just marshall
                // a list of items.  We throw away the newly marshalled
                // VDSM and copy the result into our singleton, so that
                // it can be a final constant initialized at class load.

                DataSources.addAll(unmarshalled.marshallingVector);

                isLoaded = true;
                
                if (BLOCKING_OSID_LOAD) {

                    for (edu.tufts.vue.dsm.DataSource ds : DataSources) {
                        try {
                            if (ds instanceof VueDataSource)
                                ((VueDataSource)ds).assignRepositoryConfiguration();
                        } catch (Throwable t) {
                            Log.error(t);
                        }
                    }
                    
                    // they're already fully configured at this point
                    notifyDataSourceListeners(DS_ALL_CONFIGURED);
                    
                } else {
                    
                    notifyDataSourceListeners(DS_UNMARSHALLED);
                    // DS_ALL_CONFIGURED will come later
                }
                    
            } else {
                debug("Installed DataSources not found; missing " + file);
            }
        } catch (Throwable t) {
            Log.warn("unmarshalling;", t);
            throw Util.wrapException("unmarshalling", t);
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

    public synchronized void startRepositoryConfiguration(final java.awt.Component clientUI) {
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
            
            Log.debug("configuring " + ds);
            
            new Thread(String.format("%s@%07x %8.8s",
                                     ds.getClass().getSimpleName(),
                                     System.identityHashCode(ds),
                                     ds.getRepositoryDisplayName()))
            {
                @Override public void run() {
                    try {
                        
                        try {
                        
                            //if (ds.getRepositoryDisplayName().startsWith("(Conn"))
                            //try { Thread.sleep(5000); } catch (Throwable t) {}
  
                            ((VueDataSource)ds).assignRepositoryConfiguration();
                            
                            Log.info(String.format("configured %-30s %s",
                                                   '"' + ds.getRepositoryDisplayName() + '"',
                                                   ds.getRepository()));
                            //getRepositoryID(ds);

                            if (ds.getRepository() == null)
                                notifyDataSourceListeners(DS_ERROR, ds);
                            else
                                notifyDataSourceListeners(DS_CONFIGURED, ds);

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
                            // as they come in.

                            setName(getName().toUpperCase()); // tweak the thread name for debug

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
    public synchronized edu.tufts.vue.dsm.DataSource[] getDataSources() {
        if (!isLoaded)
            load();
        return Util.toArray(DataSources, DataSource.class);
    }
    
    /**
     * Add the given data source to the global list if it isn't already there.
     * Will immediately save if it was added.
     */
    public synchronized void add(edu.tufts.vue.dsm.DataSource ds) {
        if (DataSources.add(ds)) {
            Log.info("add data src: " + ds);
            notifyDataSourceListeners(DS_CONFIGURED, ds);  // *should* be configured...
            if (!isMarshalling)
                save();
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

        synchronized (this) {
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

            if (removed && !isMarshalling)
                save();
        }
    }
    
    /**
     */
    public edu.tufts.vue.dsm.DataSource getDataSource(org.osid.shared.Id dataSourceId) {

        try {
            synchronized (this) {
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

        synchronized (this) {
            for (DataSource ds : DataSources)
                if (ds.isIncludedInSearch()) {
                    Repository r = ds.getRepository();
                    if (r == null) {
                        if (tufts.vue.DEBUG.DR) Log.debug("has no repository; not included: " + ds);
                    } else
                        included.add(r);
                }
        }

        return Util.toArray(included, Repository.class);
        
//         removeDuplicatesFromVector();
//         java.util.Vector results = new java.util.Vector();
//         int size = marshallingVector.size();
//         for (int i=0; i < size; i++) {
//             edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)marshallingVector.elementAt(i);
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

        synchronized (this) {
            for (DataSource ds : DataSources)
                if (ds.isIncludedInSearch())
                    included.add(ds);
        }

        return Util.toArray(included, DataSource.class);
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
        return isMarshalling ? marshallingVector : null;
    }
    
//     public void setDataSourceVector(Vector dsv) {
//         marshallingVector = dsv;
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
        for (edu.tufts.vue.dsm.DataSourceListener listener : dataSourceListeners) {
            try {
                listener.changed(dataSources, state, changed);
            } catch (Throwable t) {
                Log.error("DataSourceListener failure: " + Util.tags(listener), t);
            }
        }
    }
    
    private synchronized void marshall(File file)
    {
        //System.out.println("Marshalling: file -"+ file.getAbsolutePath());

        marshallingVector.clear();
        marshallingVector.addAll(DataSources);

        for (DataSource ds : marshallingVector) {
            try {
                Log.info("marshalling:  " + ds + "; RepositoryID=" + getRepositoryID(ds));
            } catch (Throwable t) {
                Log.warn("Marshalling:", t);
            }
        }

        isMarshalling = true;
        try {
            Mapping mapping = tufts.vue.action.ActionUtil.getDefaultMapping();
            FileWriter writer = new FileWriter(file);
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            marshaller.setMarshalListener(new PropertyEntryMarshalListener());
            Log.debug("Marshalling to " + file + "...");
            marshaller.marshal(this);
            writer.flush();
            writer.close();
            Log.info(" Marshalled to " + file);
        } catch (Throwable t) {
            Log.error("marshall to " + file, t);
        } finally {
            isMarshalling = false;
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
        
    
    private static synchronized VueDataSourceManager unMarshall(File file)
        throws java.io.IOException, org.exolab.castor.xml.MarshalException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.ValidationException
    {
        Log.info("Unmarshalling: " + file);
        
        Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller(file.toString());
        unmarshaller.setUnmarshalListener(new PropertyEntryUnMarshalListener());
            
        final FileReader reader = new FileReader(file);
        final VueDataSourceManager vdsm =
            (VueDataSourceManager) unmarshaller.unmarshal(new InputSource(reader));
        reader.close();

        for (DataSource ds : vdsm.marshallingVector) {
            try {
                Log.info("unmarshalled: " + ds);
            } catch (Throwable t) {
                Log.warn(t);
            }
        }
        
        Log.debug("unmarshall: done.");
        
//         int size = marshallingVector.size();
//         for (int i=0; i < size; i++) {
//             edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)marshallingVector.elementAt(i);
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
	
        return vdsm;
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
  

  