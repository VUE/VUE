package tufts.vue;
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

/*
 * LocalFileDataSource.java
 *
 * Created on October 15, 2003, 5:28 PM
 */



import javax.swing.*;
import java.util.Vector;
import java.util.*;
import java.awt.*;

import javax.swing.border.*;

import java.io.*;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.oki.shared.*;



/**
 *
 * @author  rsaigal
 */


public class LocalFileDataSource extends VueDataSource{
 
    private JComponent resourceViewer;
    
      public LocalFileDataSource(){
        
        
    }
   
    public LocalFileDataSource(String DisplayName, String address){
          this.setDisplayName(DisplayName); 
          this.setAddress(address);
          this.setResourceViewer();
        
     }

  
   public void  setResourceViewer(){
          
             Vector cabVector = new Vector();
            try{
            LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
         
            if (this.getAddress().compareTo("") == 0){     
                LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots();
                osid.shared.Agent agent = null; //  This may cause problems later.
              while(rootCabs.hasNext()){  
                    LocalCabinetEntry rootNode = (LocalCabinetEntry)rootCabs.next();
                    CabinetResource res = new CabinetResource(rootNode);
                    cabVector.add(res);
                }
                
            }
          
            // setPublishMode(Publisher.PUBLISH_CMAP);
           
            
            else {
                osid.shared.Agent agent = null;
                LocalCabinet rootNode = new LocalCabinet(this.getAddress(),agent,null);
                CabinetResource res = new CabinetResource(rootNode);
                cabVector.add(res); 
            }
               }catch (Exception ex) {VueUtil.alert(null,ex.getMessage(),"Error Setting Reseource Viewer");}
            VueDragTree fileTree = new VueDragTree(cabVector.iterator(), this.getDisplayName());
            fileTree.setRootVisible(true);
            fileTree.setShowsRootHandles(true);
            fileTree.expandRow(0);
            fileTree.setRootVisible(false);
            JPanel localPanel = new JPanel();
            JScrollPane rSP = new JScrollPane(fileTree);
            localPanel.setMinimumSize(new Dimension(290,100));
            localPanel.setLayout(new BorderLayout());
            localPanel.add(rSP,BorderLayout.CENTER);
            this.resourceViewer = localPanel;
   
   }

   public JComponent getResourceViewer(){
       
          return this.resourceViewer;   
       
   }
 
   
    
}


    






