package tufts.vue;

import javax.swing.border.TitledBorder;
import javax.swing.*;

import osid.dr.*;
import osid.OsidException;
import tufts.dr.fedora.*;
/**
 * Digital Repositor Browser
 */
class DRBrowser extends javax.swing.JTabbedPane
{
    public DRBrowser()
    {
        setBorder(new TitledBorder("DR Browser"));
        
        DragTree tree = new DragTree();
        JInternalFrame fileBrowser = new JInternalFrame("File System");
        JScrollPane jSP = new JScrollPane(tree);
        SearchPanel searchPanel = new SearchPanel(400,600);
   
        DigitalRepository dr = new DR();
        
        FedoraTree  fedoraTree = null;
        try {
            fedoraTree = new FedoraTree(dr);
        } catch(OsidException e) {
            JOptionPane.showMessageDialog(this,"Cannot connect to FEDORA Server.","FEDORA Alert", JOptionPane.ERROR_MESSAGE);
          //  System.out.println(e);
        }
        if(fedoraTree != null) {
            JScrollPane jspFedora  = new JScrollPane(fedoraTree);
            add("FEDORA",jspFedora);
        }
        add("File", jSP);  
        add("Search", searchPanel);
        
    }
    
}
