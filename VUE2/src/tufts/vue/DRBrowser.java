package tufts.vue;

import javax.swing.border.TitledBorder;
import javax.swing.*;

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
      
        
   
        add("File", jSP);
        add("FEDORA",new JInternalFrame("FEDORA"));
             add("Search", searchPanel);
        
    }
    
}
