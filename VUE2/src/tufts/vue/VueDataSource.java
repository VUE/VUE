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
 *
 * @author  rsaigal
 */
import javax.swing.JComponent;
import javax.swing.JPanel;



public abstract class VueDataSource implements DataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDataSource.class);

    public static final String RESOURCEVIEWER_ERROR = "No ResourceViewer Available";
    
    private String displayName;
    private String address;
    private String Id;
    private boolean isAutoConnect;
    private boolean isIncludedInSearch;
    private int publishMode;
    private JPanel addDataSourcePanel;
    private JPanel editDataSourcePanel;

    private JComponent _viewer; 
    
    public VueDataSource() {}
    
    //public VueDataSource(String DisplayName) throws DataSourceException{
    public VueDataSource(String DisplayName) {
        this.displayName = DisplayName;   
        //this.setResourceViewer();
    }

//     public boolean isLoaded() {
//         return resourceViewer != null;
//     }

//     public void loadViewer() {
//         getResourceViewer();
//     }
    
//     /** @deprecated */
//     public final void setResourceViewer() {
//         new Throwable("DEPRECATED").printStackTrace();
//     }

    public final String getAddress() {
        return this.address;
    }
    
    public final void setAddress(String address) {
        out("setAddress[" + address + "]");
        this.address = address;
        // any time we change the address, rebuild the viewer
        _viewer = null;
    }

    /**
     * @return the JComponent that displays the content for this data source
     */
    public final synchronized JComponent getResourceViewer() {
        if (_viewer == null)
            _viewer = buildResourceViewer();
        return _viewer;
    }

//     public synchronized JComponent getResourceViewer() {
//         Log.debug("getResourceViewer; current=" + tufts.vue.gui.GUI.name(mViewer));

//         if (mViewer == null)
//             loadContentAndBuildViewer();
        
//         Log.debug("getResourceViewer;  return " + tufts.vue.gui.GUI.name(mViewer));
//         return this.mViewer;
//     }
    
    /**
     * @return build a JComponent that displays the content for this data source
     */
    protected abstract JComponent buildResourceViewer();
    

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
    
    public String getDisplayName() {
        return this.displayName;   
    }
   
    public void setDisplayName(String DisplayName) throws DataSourceException {
        this.displayName = DisplayName;  
    }
    
    public void setisAutoConnect() {
        this.isAutoConnect = false;
    }
     
    public String getId() {
        return this.Id; 
    }
   
    public void setId(String Id)  throws DataSourceException{
        this.Id = Id;
    }
    
    public int getPublishMode() {
        return this.publishMode;   
    }
   
    public boolean isAutoConnect() {
        return this.isAutoConnect;   
    }
    
    public void setAutoConnect(boolean b)  throws DataSourceException
    {
        this.isAutoConnect = b;
    }
    
   public void setAddDataSourcePanel(){
       this.addDataSourcePanel = new JPanel();
   }
   
 
       
   
   public void setEditDataSourcePanel(){
       this.editDataSourcePanel = new JPanel();
   }
   
     /**
     *Returns a JComponent that is the panel to add the datasource
     *
     */
   public  JComponent getAddDataSourcePanel(){
       return this.addDataSourcePanel;   
   }
   
   
    
   public JComponent getEditDataSourcePanel(){
       
         return this.editDataSourcePanel;  
       
       
       
   }
   
    public boolean isIncludedInSearch()
    {
        return this.isIncludedInSearch;
    }
	
    public void setIncludedInSearch(boolean included)
    {
        this.isIncludedInSearch = included;
    }
    
    public String toString() {
        return getDisplayName();
    }
    
    private void out(String s) {
        Log.debug(getClass().getSimpleName() + "[" + getDisplayName() + "] " + s);
    }
    
        
    
}

