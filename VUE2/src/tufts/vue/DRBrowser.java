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
        add("File", new JInternalFrame("Local Files"));
        add("FEDORA",new JInternalFrame("FEDORA"));
    }
    
}
