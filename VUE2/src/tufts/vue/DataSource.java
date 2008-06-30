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

public interface DataSource {

    public void setConfiguration(java.util.Properties map);
    
    /*  
     *  Return the title or display name associated with the DataSource.
     *  (any length restrictions?)
     */
    public String getDisplayName();
    /*
     * Set the Display Name of Data Source
     */
   
    public void setDisplayName(String DisplayName) throws DataSourceException;

    /** Return a user-friendly name for the type of this data-source */
    public String getTypeName();
    
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
     *Returns a JComponent that is the Viewer for the DataSource
     *
     */
    
    public JComponent getResourceViewer();
    
    /**
     *Set the add panel with the data source
     *
     *
     */
	
    public boolean isIncludedInSearch();
	
    public void setIncludedInSearch(boolean included);

 
    //    public JComponent getAddDataSourcePanel();
   
    //     /**
    //      *Returns a JComponent that is the panel to add the datasource
    //      *
    //      */
    
    //    public JComponent getEditDataSourcePanel();

    
   
       
}

