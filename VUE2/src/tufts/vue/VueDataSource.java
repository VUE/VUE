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
  
    private String displayName;
    private String address;
    private String Id;
    private boolean isAutoConnect;
    private int publishMode;
    private JPanel resourceViewer; 
    private JPanel addDataSourcePanel;
    private JPanel editDataSourcePanel;
    
    
    public VueDataSource(){
        
        
    }
    
    public VueDataSource(String DisplayName){
        
     this.displayName = DisplayName;   
     this.setResourceViewer();
        
    }
    
    
     public String getDisplayName(){
         
      return this.displayName;   
     }
   
    public void setDisplayName(String DisplayName){
        
        this.displayName = DisplayName;  
        
      }
    
    public void setisAutoConnect()
    {
        this.isAutoConnect = false;
     
    }
     
     public String getAddress(){
         
         return this.address;
         
         
     }
     public void setAddress(String address){
         
         this.address = address;
         
         
     }
    
    
    
    /*
       *Returns an id for the DataSource. 
       *
       */
  
    
    
    public String getId(){
        
        return this.Id; 
    }
   
    public void setId(String Id){
      
        this.Id = Id;
        
    }
    public int getPublishMode(){
        
        return this.publishMode;   
        
    }
   
    public boolean isAutoConnect(){
         return this.isAutoConnect;   
        
    }
    
    public void  setResourceViewer(){
        
     this.resourceViewer = new JPanel();   
        
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
    
    
}



