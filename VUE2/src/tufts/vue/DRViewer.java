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
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import tufts.oki.dr.fedora.*;
import java.util.ArrayList;
import java.util.Hashtable;
import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;


public class DRViewer extends JPanel implements ActionListener {
    JTabbedPane tabbedPane;
    JPanel DRSearchResults;
    JPanel DRSearch;
    JTextField keywords;
    JComboBox maxReturns;
    osid.dr.DigitalRepository dr;
    osid.dr.AssetIterator assetIterator;
    SearchCriteria searchCriteria;
    SearchType searchType;
    SearchType advancedSearchType;
    JPanel advancedSearchPanel;
    
    String[] maxReturnItems = { 
            "5",
            "10",
            "20" 
      };
    JTextField idField = new JTextField();
    JTextField titleField = new JTextField();
    JTextField creatorField = new JTextField();
    JTextField typeField = new JTextField();
    JTextField coverageField = new JTextField();
    
    Object[] labelTextPairs = {
            "ID", idField,
            "title", titleField,
            "Creator", creatorField ,
            "type", typeField,
            "coverage", coverageField
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
        searchCriteria  = new SearchCriteria();
        searchType = new SearchType("Search");
        advancedSearchType = new SearchType("Advanced Search");
        try {
            dr = new DR(id,displayName,description);
            // this part will be taken from the configuration file later.
       } catch(osid.OsidException ex) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot Create Digital Repository\n"
                                          + ex.getClass().getName() + ":\n" + ex.getMessage(),
                                          "FEDORA Alert",
                                          JOptionPane.ERROR_MESSAGE);
        }
        keywords = new JTextField();
        setSearchPanel();
        tabbedPane.add("Search", DRSearch);
        tabbedPane.add("Search Results",DRSearchResults);   
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                /**
                if(((JTabbedPane)e.getSource()).getSelectedComponent() == DRViewer.this.DRSearch) {
                    DRViewer.this.DRSearchResults.removeAll();
                }
                 */
            }
        });
        add(tabbedPane,BorderLayout.CENTER);
    }
    
    
   /**  
    * @return JPanel searchPanel
    */
    
     private void  setSearchPanel() {
        DRSearch = new JPanel(new BorderLayout());
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
        searchButton.addActionListener(this);
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
        
        
        //adding the line
        c.gridx =0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        PolygonIcon line = new PolygonIcon(new Rectangle(0,0,1000, 1),Color.BLACK);
        JLabel lineLabel = new JLabel(line);
       // lineLabel.setPreferredSize(new Dimension(100, 1));
        gridbag.setConstraints(lineLabel,c);
        DRSearchPanel.add(lineLabel);
        
      //adding the advanced search tab.
        c.gridx=0;
        c.gridy=4;
        c.gridwidth = 3;
        c.insets = new Insets(10, 2, 10, 2);
       
        JLabel advancedLabel = new JLabel("Advanced Search");
        gridbag.setConstraints(advancedLabel,c);
        DRSearchPanel.add(advancedLabel);
        
        c.gridx=0;
        c.gridy=5;
        c.insets = defaultInsets;
        setAdvancedSearchPanel();
        gridbag.setConstraints(advancedSearchPanel,c);
        DRSearchPanel.add(advancedSearchPanel);
        
      
        
        
        DRSearch.add(DRSearchPanel,BorderLayout.NORTH);
    }
    private void setAdvancedSearchPanel() {
        advancedSearchPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        advancedSearchPanel.setLayout(gridBag);
        addLabelTextRows(labelTextPairs,gridBag,advancedSearchPanel);
    }
    
    
    private void addLabelTextRows(Object[] labelTextPairs,
                                  GridBagLayout gridbag,
                                  Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;

        for (int i = 0; i < num; i += 2) {
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default

            String txt = (String) labelTextPairs[i];
            txt += ": ";

            JLabel label = new JLabel(txt);
            //JLabel label = new JLabel(labels[i]);
            //label.setFont(VueConstants.SmallFont);
            gridbag.setConstraints(label, c);
            container.add(label);

            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;

            JComponent field = (JComponent) labelTextPairs[i+1];
            //field.setFont(VueConstants.SmallFont);
            if (field instanceof JTextField)
                ((JTextField)field).addActionListener(this);
            gridbag.setConstraints(field, c);
            container.add(field);
        }
        
        c.insets = new Insets(20, 10, 10, 10);
        c.anchor = GridBagConstraints.EAST;
        JButton advancedSearchButton = new JButton("Advanced Search");
        advancedSearchButton.setSize(new Dimension(160,20));
        advancedSearchButton.addActionListener(this);
        gridbag.setConstraints(advancedSearchButton,c);
        container.add(advancedSearchButton);
    }   
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Search")) {
          osid.dr.AssetIterator resultObjectsIterator;
                try {
                    searchCriteria.setKeywords(keywords.getText());
                    searchCriteria.setMaxReturns(maxReturns.getSelectedItem().toString());
                    resultObjectsIterator = dr.getAssetsBySearch(searchCriteria,searchType); 
                    VueDragTree tree = new VueDragTree(resultObjectsIterator,"Fedora Search Results");
                    tree.setRootVisible(false);
                    JScrollPane jsp = new JScrollPane(tree);
                    DRSearchResults.setLayout(new BorderLayout());
                    DRSearchResults.add(jsp,BorderLayout.CENTER,0);
                    tabbedPane.setSelectedComponent(DRSearchResults);
                } catch (Exception ex) {
			 	System.out.println(ex);
		}
        }
         if(e.getActionCommand().equals("Advanced Search")) {
          osid.dr.AssetIterator resultObjectsIterator;
                try {
                    java.util.Vector conditionVector = new java.util.Vector();
                    Condition condition=new Condition();
                    if(idField.getText().length() > 0) {
                        condition.setValue(idField.getText());
                        condition.setProperty("pid");
                        condition.setOperator(ComparisonOperator.eq);
                        conditionVector.add(condition);
                    }
                    if(titleField.getText().length()>0) {
                        condition.setValue(titleField.getText());
                        condition.setProperty("title");
                        condition.setOperator(ComparisonOperator.has);
                        conditionVector.add(condition);
                    }
                     if(creatorField.getText().length()>0) {
                        condition.setValue(creatorField.getText());
                        condition.setProperty("creator");
                        condition.setOperator(ComparisonOperator.has);
                        conditionVector.add(condition);
                    }
                    if(typeField.getText().length()>0) {
                        condition.setValue(typeField.getText());
                        condition.setProperty("type");
                        condition.setOperator(ComparisonOperator.has);
                        conditionVector.add(condition);
                    }
                     if(coverageField.getText().length()>0) {
                        condition.setValue(coverageField.getText());
                        condition.setProperty("coverage");
                        condition.setOperator(ComparisonOperator.has);
                        conditionVector.add(condition);
                    }
                    Condition[] cond=new Condition[conditionVector.size()];
                    for(int i=0;i<cond.length;i++)
                        cond[i] = (Condition)conditionVector.get(i);
                    searchCriteria.setConditions(cond);
                    searchCriteria.setMaxReturns(maxReturns.getSelectedItem().toString());
                    resultObjectsIterator = dr.getAssetsBySearch(searchCriteria,advancedSearchType); FedoraSoapFactory.advancedSearch((DR)dr,cond,maxReturns.getSelectedItem().toString());
                    //VueDragTree tree = new VueDragTree(resultTitles.iterator(),"Fedora Search Results");
                    VueDragTree tree = new VueDragTree(resultObjectsIterator,"Fedora Search Results");
                    tree.setRootVisible(false);
                    JScrollPane jsp = new JScrollPane(tree);
                    DRSearchResults.setLayout(new BorderLayout());
                    DRSearchResults.add(jsp,BorderLayout.CENTER,0);
                    tabbedPane.setSelectedComponent(DRSearchResults);
                } catch (Exception ex) {
			 	System.out.println(ex);
		}
        }
    }
    
}
