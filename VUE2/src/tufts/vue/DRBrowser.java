package tufts.vue;

import javax.swing.border.TitledBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import osid.dr.*;
import osid.OsidException;
import tufts.oki.dr.fedora.*;
import java.util.Vector;
import java.util.ArrayList;
import java.io.*;


import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;

/**
 * Digital Repositor Browser
 */
class DRBrowser extends JPanel {
    JPanel fedoraSearchResults;
    JTabbedPane fedoraPane;
    JTextField keywords;
    JComboBox maxReturns;
    DigitalRepository dr;
    osid.shared.Type assetType;
    public static DataSourceViewer dsViewer = null;
   
    public DRBrowser()
    {
        //super(JSplitPane.VERTICAL_SPLIT);
        //setOneTouchExpandable(true);
        //setContinuousLayout(false);
        //setResizeWeight(0.25);
        setLayout(new BorderLayout());
        dsViewer = new DataSourceViewer(this);
        dsViewer.setName("Data Source Viewer");
        add(dsViewer,BorderLayout.NORTH);
        //setTopComponent(panel);
       // JDesktopPane jDP = new JDesktopPane();
       // jDP.add(dsViewer);
        //add("Test", dsViewer);
        //setLeftComponent(dsViewer);
      /*** OLD CODE - to be removed 
       // setBorder(new TitledBorder("DR Browser"));
               
         //File System
         //Possible Oki Filing implementation - Later
              Vector fileVector  = new Vector();
         fileVector.add(new File("C:\\"));
      //   fileVector.add(new File("D:\\"));
          VueDragTree fileTree = new VueDragTree(fileVector.iterator(),"File System");
          JScrollPane jSP = new JScrollPane(fileTree);
           add("File", jSP);  
        
       //Fedora 
       // add fedora pane
         add("FEDORA",getFedoraPane());
       
        //Search
        SearchPanel searchPanel = new SearchPanel();
        add("Search", searchPanel);
        add("test",new DataSourceViewer());
       */
    }
    /**
     *@return JTabbedPane fedoraPane
     */
    
    
    
}
