package tufts.vue;

import javax.swing.border.TitledBorder;
import javax.swing.*;

import osid.dr.*;
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
      /**
        DigitalRepository dr = new DR();
        FedoraTree  fedoraTree= new FedoraTree(dr);
        JScrollPane jspFedora  = new JScrollPane(fedoraTree);
        **/
        add("File", jSP);
       // add("FEDORA",jspFedora);
        add("Search", searchPanel);
        
    }
    
}
