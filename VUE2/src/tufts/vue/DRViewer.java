/*
 * DRViewer.java
 *
 * Created on October 16, 2003, 9:40 AM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import tufts.dr.fedora.*;
import java.util.ArrayList;
import java.util.Hashtable;
import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;


public class DRViewer extends JPanel {
    JTabbedPane tabbedPane;
    JPanel DRSearchResults;
    JTextField keywords;
    JComboBox maxReturns;
    osid.dr.DigitalRepository dr;
    osid.shared.Type assetType;
    Hashtable supportedAssetTypes = new Hashtable();
    
    String[] maxReturnItems = { 
            "5",
            "10",
            "20" 
      };
    
    /** Creates a new instance of DRViewer */
    public DRViewer() {
    }
    
    public DRViewer(String conf,  String id,String displayName,String description) {
       // setBorder(new TitledBorder(displayName));
        setLayout(new BorderLayout());
        DRSearchResults = new JPanel();
        tabbedPane = new JTabbedPane();
        maxReturns = new JComboBox(maxReturnItems);
        maxReturns.setEditable(true);
        try {
            dr = new DR(id,displayName,description);
            // this part will be taken from the configuration file later.
            assetType = ((tufts.dr.fedora.DR)dr).createFedoraObjectAssetType("TUFTS_STD_IMAGE");
            supportedAssetTypes.put("TUFTS_STD_IMAGE", assetType);
            assetType = ((tufts.dr.fedora.DR)dr).createFedoraObjectAssetType("XML_TO_HTMLDOC");
            supportedAssetTypes.put("XML_TO_HTMLDOC", assetType);
       } catch(osid.OsidException ex) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot Create Digital Repository\n"
                                          + ex.getClass().getName() + ":\n" + ex.getMessage(),
                                          "FEDORA Alert",
                                          JOptionPane.ERROR_MESSAGE);
        }
        keywords = new JTextField();
        
        tabbedPane.add("Search", getSearchPanel());
        tabbedPane.add("Search Results",DRSearchResults);   
        add(tabbedPane,BorderLayout.CENTER);
    }
    
    
   /**  
    * @return JPanel searchPanel
    */
    
    JPanel getSearchPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel DRSearchPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        DRSearchPanel.setLayout(gridbag);
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
        DRSearchPanel.add(topLabel);
        
        
    //adding the search box and the button
        c.gridx=0;
        c.gridy=1;
        c.gridwidth=2;
        c.ipady=0;
        keywords.setPreferredSize(new Dimension(120,20));
        gridbag.setConstraints(keywords, c);
        DRSearchPanel.add(keywords);
        
        c.gridx=2;
        c.gridy=1;
        c.gridwidth=1;
        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80,20));
        searchButton.addActionListener(new ActionListener() { 
           public void actionPerformed(ActionEvent e) {
                String[] resField=new String[4];
                resField[0]="pid";
                resField[1]="title";
                resField[2]="description";
                resField[3]="cModel";
                //FieldSearchResult methodDefs=new FieldSearchResult(); 
                System.out.println("Combo Box Selection"+DRViewer.this.maxReturns.getSelectedItem().toString());
                ArrayList resultTitles = new ArrayList();
                java.util.Vector resultObjects = new java.util.Vector();
                try {
                    FieldSearchResult methodDefs=(FieldSearchResult) FedoraSoapFactory.search((DR)DRViewer.this.dr,DRViewer.this.keywords.getText(),DRViewer.this.maxReturns.getSelectedItem().toString(),resField);
                    if (methodDefs != null){
                            ObjectFields[] fields= methodDefs.getResultList(); 
                            for(int i=0;i<fields.length;i++) {
                                if(DRViewer.this.supportedAssetTypes.containsKey(fields[i].getCModel())) {
                                    System.out.println(fields[i].getPid()+"  "+fields[i].getTitle()[0]+ " type "+fields[i].getCModel()); 
                                    resultTitles.add(fields[i].getTitle()[0]);
                                    resultObjects.add(new tufts.dr.fedora.FedoraObject((DR)DRViewer.this.dr,fields[i].getPid(),fields[i].getTitle()[0],(osid.shared.Type)DRViewer.this.supportedAssetTypes.get(fields[i].getCModel())));
                                }
                            }
                    } else {
                        System.out.println("search return no results");
                    }
                    //VueDragTree tree = new VueDragTree(resultTitles.iterator(),"Fedora Search Results");
                    VueDragTree tree = new VueDragTree(resultObjects.iterator(),"Fedora Search Results");
                    tree.setEditable(true);
                    tree.setRootVisible(false);
                    JScrollPane jsp = new JScrollPane(tree);
                    DRViewer.this.DRSearchResults.setLayout(new BorderLayout());
                    DRViewer.this.DRSearchResults.add(jsp,BorderLayout.CENTER,0);
                    DRViewer.this.tabbedPane.setSelectedComponent(DRViewer.this.DRSearchResults);
                } catch (Exception ex) {
			 	System.out.println(ex);
		}
           }
        });
        
        gridbag.setConstraints(searchButton,c);
        DRSearchPanel.add(searchButton);
        
      // adding the number of search results tab.
        c.gridx=0;
        c.gridy=2;
        c.gridwidth=2;
        JLabel returnLabel = new JLabel("Maximum number of returns?");
        gridbag.setConstraints(returnLabel, c);
        DRSearchPanel.add(returnLabel);
        
        c.gridx=2;
        c.gridy=2;
        c.gridwidth=1;
        maxReturns.setPreferredSize(new Dimension(40,20));
        gridbag.setConstraints(maxReturns,c);
        DRSearchPanel.add(maxReturns);
        
      //adding the advanced search tab.
        c.gridx=0;
        c.gridy=3;
        c.insets = new Insets(10, 2, 100, 2);
        JLabel advancedLabel = new JLabel("Advanced Search");
        gridbag.setConstraints(advancedLabel,c);
        DRSearchPanel.add(advancedLabel);
        
        outerPanel.add(DRSearchPanel,BorderLayout.NORTH);
        return outerPanel;
        
    }
}
