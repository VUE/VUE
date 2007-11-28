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



public class VueDataSource implements DataSource{
  
    public static final String RESOURCEVIEWER_ERROR = "No ResourceViewer Available";
    protected String displayName;
    protected String address;
    protected String Id;
    protected boolean isAutoConnect;
	protected boolean isIncludedInSearch;
    protected int publishMode;
    protected JPanel resourceViewer; 
    protected JPanel addDataSourcePanel;
    protected JPanel editDataSourcePanel;
    
    
    public VueDataSource(){
        
        
    }
    
    public VueDataSource(String DisplayName) throws DataSourceException{
        
     this.displayName = DisplayName;   
     this.setResourceViewer();
        
    }
    
    
     public String getDisplayName(){
         
      return this.displayName;   
     }
   
    public void setDisplayName(String DisplayName)  throws DataSourceException{
        
        this.displayName = DisplayName;  
        
      }
    
    public void setisAutoConnect()
    {
        this.isAutoConnect = false;
     
    }
     
     public String getAddress(){
         
         return this.address;
         
         
     }
     public void setAddress(String address)  throws DataSourceException{
         
         this.address = address;
         
         
     }
    
    
    
    /*
       *Returns an id for the DataSource. 
       *
       */
  
    
    
    public String getId(){
        
        return this.Id; 
    }
   
    public void setId(String Id)  throws DataSourceException{
      
        this.Id = Id;
        
    }
    public int getPublishMode(){
        
        return this.publishMode;   
        
    }
   
    public boolean isAutoConnect(){
         return this.isAutoConnect;   
        
    }
    
    public void setAutoConnect(boolean b)  throws DataSourceException
    {
        this.isAutoConnect = b;
    }
    
    public void  setResourceViewer() throws DataSourceException{
        
     this.resourceViewer = new JPanel();  
     throw new DataSourceException(RESOURCEVIEWER_ERROR);
        
    }
    /**
     *Returns a JPanel that is the Viewer for the DataSource
     *
     */
    public JComponent getResourceViewer(){
           return this.resourceViewer;   
        
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
   
   public String toString() {
       return getDisplayName();
   }
    
	public boolean isIncludedInSearch()
	{
		return this.isIncludedInSearch;
	}
	
	public void setIncludedInSearch(boolean included)
	{
		this.isIncludedInSearch = included;
	}
    
}

