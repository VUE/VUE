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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */
/*
 * DataSource.java
 *
 * Created on January 23, 2004, 3:45
 */

package tufts.vue;

/**
 *  The DataSource interface defines a set of methods which all vue datasource objects must
 *  implement. The implementation of the interface enables ease of adding different categories of 
 *  datasources to Vue. 
 
 * @author  rsaigal
 */

import javax.swing.JComponent;

public interface DataSource{
    /*  
     *  Return the title or display name associated with the DataSource.
     *  (any length restrictions?)
     */
    public String getDisplayName();
    /*
     * Set the Display Name of Data Source
     */
   
    public void setDisplayName(String DisplayName);
     /**
     * Return the address associated with the DataSource. It may be an IP address or any 
     * address that makes sense for the DataSource. It is a string 
     */
    public String getAddress();
     /**
       *Returns an id for the DataSource. 
       *
       */
    public String getId();
    /**
     *Return a integer defining the  PublishMode
     *
     */
    public int getPublishMode();
    /**
     *Returns a boolean defining whether or not we need to AutoConnect to the
     *source.
     *
     */
    public boolean isAutoConnect();
   
    /**
     *Set the Resource Viewer Associated with the data source
     *
     *
     */
       
    public void setResourceViewer();
    
     /**
     *Returns a JComponent that is the Viewer for the DataSource
     *
     */
    
    public JComponent getResourceViewer();
    
    /**
     *Set the add panel with the data source
     *
     *
     */
       
     
  // public void setAddDataSourcePanel();
   
   /**
     *Set the edit panel associated with the data source
     *
     *
     */
       
   
  // public void setEditDataSourcePanel();
   
     /**
     *Returns a JComponent that is the panel to add the datasource
     *
     */
   public JComponent getAddDataSourcePanel();
   
    /**
     *Returns a JComponent that is the panel to add the datasource
     *
     */
    
   public JComponent getEditDataSourcePanel();
    
   
       
}

