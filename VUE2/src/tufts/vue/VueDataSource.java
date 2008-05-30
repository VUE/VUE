/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

/*
 * VueDataSource.java
 *
 * Created on October 15, 2003, 5:28 PM
 */

package tufts.vue;

/**
 * Abstract class for all "browse" based VUE data sources, sometimes called
 * "old style", before VUE had OSID integration for accessing data-sources via search.
 *
 * This class is for data-sources where all the content want's to be seen, based
 * on the configuration.  E.g., a local directory, a list of user favorites, a remote FTP
 * site, an RSS feed, etc.
 * 
 * @version $Revision: 1.13 $ / $Date: 2008-05-30 22:46:25 $ / $Author: sfraize $
 * @author  rsaigal
 * @author  sfraize
 */

import tufts.vue.DEBUG;

import javax.swing.JComponent;

public abstract class VueDataSource implements DataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDataSource.class);

    private String displayName;
    private String address;
    private String Id;
    private boolean isAutoConnect;
    private boolean isIncludedInSearch;
    private int publishMode;

    private volatile JComponent _viewer; // volatile should be overkill, but just in case
    private boolean isAvailable;
    private String hostName;
    
    public VueDataSource() {}
    
    public VueDataSource(String name) {
        setDisplayName(name);
    }

    // todo: to persist extra properties (e.g., authentication keys) add a getPropertyList for
    // castor that returns PropertyEntry's to persist extra key/values.  Could use PropertyMap and
    // add a convert to list (URLResource just does this manually when requested for persistance by
    // castor) or add a util function. (or, could just hack it into RSS data source)

    /**
     * This handles the default properties "name" and "address" -- implementors should override
     * to add additional properties of their own.  This is used by EditLibraryPanel to
     * pass the result of user property edits back into the VueDataSource.
     */
    public void setConfiguration(java.util.Properties p) {
        String val = null;
        
        try {
            if ((val = p.getProperty("name")) != null)
                setDisplayName(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty("address")) != null)
                setAddress(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
    }

    public String getTypeName() {
        return getClass().getSimpleName();
    }
    
    public final void setAddress(String newAddress) {
        out("setAddress[" + newAddress + "]");
        if (newAddress != null && !newAddress.equals(address)) {
            this.address = newAddress;
            // any time we change the address, rebuild the viewer
            unloadViewer();

            java.net.URI uri;
            try {
                uri = new java.net.URI(newAddress);
                hostName = uri.getHost();
            } catch (Throwable t) {
                hostName = null;
            }
            
        }
    }

    public final String getAddress() {
        return this.address;
    }

    /** @return a host name if one can be found in the address, otherwise returns the address */
    public String getAddressName() {
        if (hostName != null)
            return hostName;
        else
            return getAddress();
    }

    public String getHostName() {
        return hostName;
    }

    public String getDisplayName() {
        return this.displayName;   
    }
   
    public void setDisplayName(String name) {
        this.displayName = name;
    }
    
    /**
     * @return the JComponent that is current set to displays the content for this data source
     * Will return null until set.
     */
    public final JComponent getResourceViewer() {
        return _viewer;
    }
    
    /** set the viewer that's been loaded */
    // call from AWT only
    void setViewer(JComponent v) {
        if (DEBUG.Enabled && _viewer != v) out("setViewer " + tufts.vue.gui.GUI.name(v));
        _viewer = v;
    }

    // call from AWT only
    protected void unloadViewer() {
        if (DEBUG.DR) out("unloadViewer");
        if (mLoadThread != null)
            setLoadThread(null);
        if (_viewer != null)
            setViewer(null);
    }


    private Thread mLoadThread;
    
    // call from AWT only
    void setLoadThread(Thread t) {
        if (DEBUG.Enabled) out("setLoadThread: " + t);
        if (mLoadThread != null && mLoadThread.isAlive()) {
            if (DEBUG.Enabled) Log.debug(this + "; setLoadThread: INTERRUPT " + mLoadThread);
            //if (DEBUG.Enabled) Log.warn(this + "; setLoadThread: FALLBACK-INTERRUPT " + mLoadThread);
            mLoadThread.interrupt();
        }
        mLoadThread = t;
    }
    // call from AWT only
    Thread getLoadThread() {
        return mLoadThread;
    }
    
    // call from AWT only
    boolean isLoading() {
        return mLoadThread != null;
    }

    boolean isAvailable() {
        return isAvailable;
    }

    void setAvailable(boolean t) {
        isAvailable = t;
    }
    
    
    
    /**
     * @return build a JComponent that displays the content for this data source
     * This will most likely NOT be called on the AWT thread, so it should
     * only build the component, and not add anything into any live on-screen
     * AWT component hierarchies.
     */
    protected abstract JComponent buildResourceViewer();

    public void setisAutoConnect() {
        this.isAutoConnect = false;
    }
     
    public String getId() {
        return this.Id; 
    }
   
    public void setId(String Id) {
        this.Id = Id;
    }
    
    public int getPublishMode() {
        return this.publishMode;   
    }
   
    public boolean isAutoConnect() {
        return this.isAutoConnect;   
    }
    
    public void setAutoConnect(boolean b) {
        this.isAutoConnect = b;
    }
    
    public boolean isIncludedInSearch() {
        return this.isIncludedInSearch;
    }
	
    public void setIncludedInSearch(boolean included) {
        this.isIncludedInSearch = included;
    }
    
    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + getDisplayName() + "; " + getAddress() + "]";
    }
    
    private void out(String s) {
        Log.debug(getClass().getSimpleName() + "[" + getDisplayName() + "] " + s);
    }


    //private JPanel addDataSourcePanel;
    //private JPanel editDataSourcePanel;
//    public void setAddDataSourcePanel() {
//        this.addDataSourcePanel = new JPanel();
//    }
//    public  JComponent getAddDataSourcePanel(){
//        return this.addDataSourcePanel;   
//    }
       
//    public void setEditDataSourcePanel(){
//        this.editDataSourcePanel = new JPanel();
//    }
//    public JComponent getEditDataSourcePanel(){
//        return this.editDataSourcePanel;  
//    }
   
    
    
        
    
}

