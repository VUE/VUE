package tufts.vue;

import javax.swing.border.TitledBorder;
import javax.swing.*;

import osid.dr.*;
import osid.OsidException;
import tufts.dr.fedora.*;
import java.util.Vector;
import java.io.*;
/**
 * Digital Repositor Browser
 */
class DRBrowser extends javax.swing.JTabbedPane
{
    public DRBrowser()
    {
       
        setBorder(new TitledBorder("DR Browser"));
               
         //File System
         //Possible Oki Filing implementation - Later
              Vector fileVector  = new Vector();
         fileVector.add(new File("C:\\"));
         fileVector.add(new File("D:\\"));
          VueDragTree fileTree = new VueDragTree(fileVector.iterator(),"File System");
          JScrollPane jSP = new JScrollPane(fileTree);
           add("File", jSP);  
        
       //Fedora
           
        VueDragTree fedoraTree = null;
        DigitalRepository dr = new DR();
        
        try {
            fedoraTree = new VueDragTree(dr.getAssets(),"Fedora Tree");
        } catch(OsidException e) {
            System.err.println(e);
            JOptionPane.showMessageDialog(this,
                                          "Cannot connect to FEDORA Server:\n"
                                          + e.getClass().getName() + ":\n" + e.getMessage(),
                                          "FEDORA Alert",
                                          JOptionPane.ERROR_MESSAGE);
        }
        if(fedoraTree != null) {
            JScrollPane jspFedora  = new JScrollPane(fedoraTree);
            add("FEDORA",jspFedora);
        }
        
        //Search
        SearchPanel searchPanel = new SearchPanel();
  
        add("Search", searchPanel);
      
    }
    
}
