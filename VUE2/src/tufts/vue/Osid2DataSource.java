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
 * OsidDataSource.java
 *
 * Created on June 14, 2004
 */

package tufts.vue;
import javax.swing.*;
import java.net.URL;
/**
 *
 * @author  rsaigal, jkahn
 */


public class Osid2DataSource extends VueDataSource{
 
    private JComponent resourceViewer;
    private String address;
    
    public Osid2DataSource() {
    }
    
    public Osid2DataSource(String DisplayName, String address) throws DataSourceException{
          this.setDisplayName(DisplayName); 
          this.setAddress(address);
//          this.setAutoConnect(false);
          this.setResourceViewer();        
     }
    
      public void setAddress(String address){
        
        this.address = address;
        
    }
     public String getAddress(){
        
        return this.address;
        
    }
     
   public void  setResourceViewer() throws DataSourceException{
             
       
          this.resourceViewer = new Osid2AssetViewer(this.address,new org.osid.OsidContext());
System.out.println("in Osid2DataSource");
   }

   public JComponent getResourceViewer(){
       
          return this.resourceViewer;   
       
   }
 
   
    
}


    








