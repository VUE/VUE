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
 * FavortiesDataSource.java
 *
 * Created on October 15, 2003, 5:28 PM
 */

package tufts.vue;
import javax.swing.*;
import java.net.URL;
/**
 *
 * @author  rsaigal
 */


public class FedoraDataSource extends VueDataSource{
 
    private JComponent resourceViewer;
    private String address;
    private String username;
    private String password;
    public FedoraDataSource(String DisplayName, String address, String username, String password){
          this.setDisplayName(DisplayName); 
          this.setAddress(address);
          this.setUserName(username);
          this.setPassword(password);
          this.setResourceViewer();
        
     }
    
      public void setAddress(String address){
        
        this.address = address;
        
    }
     public String getAddress(){
        
        return this.address;
        
    }
     
     public void setUserName(String username){
        
        this.username = username;
        
    }
     public String getUserName(){
        
        return this.username;
        
    }
     
      public void setPassword(String password){
        
        this.password = password;
        
    }
        public String getPassword(){
        
        return this.password;
        
    }
   
    
   public void  setResourceViewer(){
             
       try{
          this.resourceViewer = new DRViewer("fedora.conf",this.getDisplayName(),this.getDisplayName(),this.getDisplayName(),new URL("http",this.getAddress(),8080,"fedora/"),this.getUserName(),this.getPassword());
              
       }catch (Exception ex){}; 
   }

   public JComponent getResourceViewer(){
       
          return this.resourceViewer;   
       
   }
 
   
    
}


    








