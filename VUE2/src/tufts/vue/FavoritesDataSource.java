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
import javax.swing.JComponent;
/**
 *
 * @author  rsaigal
 */


public class FavoritesDataSource extends VueDataSource{
 
    private JComponent resourceViewer;
    
     public FavoritesDataSource(){
        
        
    }
    public FavoritesDataSource(String DisplayName){
          this.setDisplayName(DisplayName);   
           this.setResourceViewer();
        
     }
    
    
   
    
   public void  setResourceViewer(){
       
           this.resourceViewer = new FavoritesWindow(this.getDisplayName());


        
   }

   public JComponent getResourceViewer(){
       
          return this.resourceViewer;   
       
   }
 
   
    
}


    




