package tufts.vue;

import javax.swing.border.TitledBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
    JPanel fedoraSearchResults;
    JTabbedPane fedoraPane;
    public DRBrowser()
    {
       
        setBorder(new TitledBorder("DR Browser"));
               
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
      
    }
    /**
     *@return JTabbedPane fedoraPane
     */
    
    JTabbedPane getFedoraPane() {
        fedoraPane = new JTabbedPane();
        VueDragTree fedoraTree = null;
        DigitalRepository dr = new DR();
        fedoraSearchResults  = new JPanel();
        try {
            fedoraTree = new VueDragTree(dr.getAssets(),"Fedora Tree");
        } catch(OsidException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Cannot connect to FEDORA Server:\n"
                                          + e.getClass().getName() + ":\n" + e.getMessage(),
                                          "FEDORA Alert",
                                          JOptionPane.ERROR_MESSAGE);
        }
        
        if(fedoraTree != null) {
            JScrollPane jspFedora  = new JScrollPane(fedoraTree);
            fedoraPane.addTab("Browse",jspFedora);
        }
        fedoraPane.addTab("Search", getSearchPanel());
        fedoraPane.addTab("Search Results",fedoraSearchResults);
        return fedoraPane;
    }
   /**  
    * @return JPanel searchPanel
    */
    
    JPanel getSearchPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel fedoraSearchPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        fedoraSearchPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        c.fill = GridBagConstraints.HORIZONTAL;
        
     //adding the top label   
        c.weightx = 1.0;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=3;
        c.ipady = 10;
        c.insets = defaultInsets;
        c.anchor = GridBagConstraints.NORTH;
        JLabel topLabel = new JLabel("Search inside: Tufts FEDORA");
        gridbag.setConstraints(topLabel, c);
        fedoraSearchPanel.add(topLabel);
        
        
    //adding the search box and the button
        c.gridx=0;
        c.gridy=1;
        c.gridwidth=2;
        c.ipady=0;
        JTextField keywords = new JTextField();
        keywords.setPreferredSize(new Dimension(120,20));
        gridbag.setConstraints(keywords, c);
        fedoraSearchPanel.add(keywords);
        
        c.gridx=2;
        c.gridy=1;
        c.gridwidth=1;
        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80,20));
        searchButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               DRBrowser.this.fedoraPane.setSelectedComponent(DRBrowser.this.fedoraSearchResults);
           }
        });
        
        gridbag.setConstraints(searchButton,c);
        fedoraSearchPanel.add(searchButton);
        
      // adding the number of search results tab.
        c.gridx=0;
        c.gridy=2;
        c.gridwidth=2;
        JLabel returnLabel = new JLabel("Maximum number of returns?");
        gridbag.setConstraints(returnLabel, c);
        fedoraSearchPanel.add(returnLabel);
        
        c.gridx=2;
        c.gridy=2;
        c.gridwidth=1;
        JTextField maxReturns = new JTextField("20");
        maxReturns.setPreferredSize(new Dimension(40,20));
        gridbag.setConstraints(maxReturns,c);
        fedoraSearchPanel.add(maxReturns);
        
      //adding the advanced search tab.
        c.gridx=0;
        c.gridy=3;
        c.insets = new Insets(10, 2, 100, 2);
        JLabel advancedLabel = new JLabel("Advanced Search");
        gridbag.setConstraints(advancedLabel,c);
        fedoraSearchPanel.add(advancedLabel);
        
        outerPanel.add(fedoraSearchPanel,BorderLayout.NORTH);
        return outerPanel;
        
    }
    
}
