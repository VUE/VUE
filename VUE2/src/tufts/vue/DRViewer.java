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

import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import tufts.oki.dr.fedora.*;
import osid.dr.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;
import java.util.List;
import java.util.ArrayList;


public class DRViewer extends JPanel implements ActionListener,KeyListener {
    ConditionsTableModel m_model;
    JTabbedPane tabbedPane;
    JPanel DRSearchResults;
    JPanel DRSearch;
    JPanel advancedSearchPanel;
    
    JTextField keywords;
    JComboBox maxReturns;
    
    osid.dr.DigitalRepository dr; // Digital Repository connected to.
    
    osid.dr.AssetIterator assetIterator;
    SearchCriteria searchCriteria;
    SearchType searchType;
    SearchType advancedSearchType;
    
    
    String[] maxReturnItems = { 
            "5",
            "10",
            "25" 
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
    
    public DRViewer(String conf,  String id,String displayName,String description, URL address, String userName, String password) {
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
            dr = new DR(conf,id,displayName,description,address,userName,password);
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
        setAdvancedSearchPanel();
        tabbedPane.addTab("Search" , DRSearch);
        tabbedPane.addTab("Advanced Search",advancedSearchPanel);
        tabbedPane.add("Search Results",DRSearchResults);   
       // tabbedPane.setBackground(new Color(200,200,50));
       // tabbedPane.setBackgroundAt(1,new Color(100,100,50));
        //tabbedPane.setForeground(new Color(100,100,240));
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
        //DRSearchPanel.setBackground(Color.LIGHT_GRAY);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        DRSearchPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        
     //adding the top label   
       // c.weightx = 1.0;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=3;
        c.insets = defaultInsets;
        JLabel topLabel = new JLabel("Search inside: Tufts FEDORA");
        topLabel.setFont(new Font("Arial",Font.PLAIN, 12));
        gridbag.setConstraints(topLabel, c);
        DRSearchPanel.add(topLabel);
        
        
    //adding the search box 
        c.gridx=0;
        c.gridy=1;
        c.gridwidth=3;
        //keywords.setPreferredSize(new Dimension(120,20));
        keywords.addKeyListener(this);
        gridbag.setConstraints(keywords, c);
        DRSearchPanel.add(keywords);
        
        
        
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
       // maxReturns.setPreferredSize(new Dimension(40,20));
        gridbag.setConstraints(maxReturns,c);
        DRSearchPanel.add(maxReturns);
        
        c.gridx=2;
        c.gridy=3;
        c.insets = new Insets(10, 2,2,2);
        JButton searchButton = new JButton("Search");
        //searchButton.setPreferredSize(new Dimension(40,20));
        searchButton.addActionListener(this);
        gridbag.setConstraints(searchButton,c);
        DRSearchPanel.add(searchButton);
        
        /**
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
        //setAdvancedSearchPanel();
        
        setAdvancedSearchPane();
        gridbag.setConstraints(advancedSearchPanel,c);
        DRSearchPanel.add(advancedSearchPanel);
         *
         */
        
        DRSearch.add(DRSearchPanel,BorderLayout.NORTH);
        DRSearch.validate();
        
    }
     
     private void setAdvancedSearchPanel() {
         m_model=new ConditionsTableModel();
         JTable conditionsTable=new JTable(m_model);      
         conditionsTable.setPreferredScrollableViewportSize(new Dimension(100,200));
         JScrollPane conditionsScrollPane=new JScrollPane(conditionsTable);
         conditionsScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,6,6));
         JPanel innerConditionsPanel=new JPanel();
         innerConditionsPanel.setLayout(new BorderLayout());
         innerConditionsPanel.add(conditionsScrollPane, BorderLayout.CENTER);
         
         // EAST: modifyConditionsOuterPanel(modifyConditionsInnerPanel)
         
         // NORTH: modifyConditionsInnerPanel
         
         // GRID: addConditionButton
         JButton addConditionButton=new JButton("Add..");
         addConditionButton.setSize(new Dimension(100,20));
         
         // GRID: modifyConditionButton
         JButton modifyConditionButton=new JButton("Change..");
         modifyConditionButton.setSize(new Dimension(100,20));
         
         // GRID: deleteConditionButton
         JButton deleteConditionButton=new JButton("Delete");
         deleteConditionButton.setSize(new Dimension(100,20));
         
         JButton advancedSearchButton = new JButton("Advanced Search");
         advancedSearchButton.setSize(new Dimension(100,20));
         advancedSearchButton.addActionListener(this);
         // Now that buttons are available, register the
         // list selection listener that sets their enabled state.
         conditionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         ConditionSelectionListener sListener= new ConditionSelectionListener(modifyConditionButton,deleteConditionButton, -1);
         conditionsTable.getSelectionModel().addListSelectionListener(sListener);
         // ..and add listeners to the buttons
         
         addConditionButton.addActionListener(new AddConditionButtonListener(m_model));
         modifyConditionButton.addActionListener(new ChangeConditionButtonListener(m_model, sListener));
         deleteConditionButton.addActionListener(new DeleteConditionButtonListener(m_model, sListener));
         
         JPanel modifyConditionsInnerPanel=new JPanel();
         modifyConditionsInnerPanel.setLayout(new GridLayout(4, 1));
         modifyConditionsInnerPanel.add(addConditionButton);
         modifyConditionsInnerPanel.add(modifyConditionButton);
         modifyConditionsInnerPanel.add(deleteConditionButton);
         modifyConditionsInnerPanel.add(advancedSearchButton);
         
         
         JPanel modifyConditionsOuterPanel=new JPanel();
         modifyConditionsOuterPanel.setLayout(new BorderLayout());
         modifyConditionsOuterPanel.add(modifyConditionsInnerPanel, BorderLayout.NORTH);

        
         advancedSearchPanel=new JPanel();
         advancedSearchPanel.setLayout(new BorderLayout());
         advancedSearchPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
         advancedSearchPanel.add(innerConditionsPanel, BorderLayout.CENTER);
         advancedSearchPanel.add(modifyConditionsOuterPanel, BorderLayout.EAST);
         //advancedSearchPanel.add(advancedSearchButton,BorderLayout.SOUTH);
         advancedSearchPanel.validate();
     }
     
     /**
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
            if (field instanceof JTextField) {
                ((JTextField)field).addActionListener(this);
                ((JTextField)field).addKeyListener(this);
            }
            gridbag.setConstraints(field, c);
            container.add(field);
        }
        
        c.insets = new Insets(20, 40, 10, 5);
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        JButton advancedSearchButton = new JButton("Advanced Search");
        advancedSearchButton.setSize(new Dimension(160,20));
        advancedSearchButton.addActionListener(this);
        gridbag.setConstraints(advancedSearchButton,c);
        container.add(advancedSearchButton);
    }   
    **/
     
    private void performSearch() {
        osid.dr.AssetIterator resultObjectsIterator;
        try {
            searchCriteria.setKeywords(keywords.getText());
            searchCriteria.setMaxReturns(maxReturns.getSelectedItem().toString());
            resultObjectsIterator = dr.getAssetsBySearch(searchCriteria,searchType); 
            VueDragTree tree = new VueDragTree(getAssetResourceIterator(resultObjectsIterator),"Fedora Search Results");
            tree.setRootVisible(false);
            JScrollPane jsp = new JScrollPane(tree);
            DRSearchResults.setLayout(new BorderLayout());
            DRSearchResults.add(jsp,BorderLayout.CENTER,0);
            tabbedPane.setSelectedComponent(DRSearchResults);
        } catch (Exception ex) {
                        System.out.println("DRViewer.performSearch :"+ex);
        }
    }
    
    private void performAdvancedSearch() {
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
            VueDragTree tree = new VueDragTree(getAssetResourceIterator(resultObjectsIterator),"Fedora Search Results");
            tree.setRootVisible(false);
            JScrollPane jsp = new JScrollPane(tree);
            DRSearchResults.setLayout(new BorderLayout());
            DRSearchResults.add(jsp,BorderLayout.CENTER,0);
            tabbedPane.setSelectedComponent(DRSearchResults);
        } catch (Exception ex) {
                        System.out.println(ex);
        }
    }
    
    public Iterator getAssetResourceIterator(AssetIterator i)  throws osid.dr.DigitalRepositoryException, osid.OsidException{
        Vector assetResources = new Vector();
        while(i.hasNext()) {
                assetResources.add(new AssetResource(i.next()));
        }
        return assetResources.iterator();
    }
    
        
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Search")) {
          performSearch();
        }
         if(e.getActionCommand().equals("Advanced Search")) {
          performAdvancedSearch();
        }
    }
    
    
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar()== KeyEvent.VK_ENTER) {
            if(e.getComponent() == keywords) {
                performSearch();
            } else {
                performAdvancedSearch();
            }
        }
    }
    
    public osid.dr.DigitalRepository getDR() {
        return dr;
    }
    
    
    public class ConditionsTableModel extends AbstractTableModel {

        List m_conditions;

        public ConditionsTableModel() {
            m_conditions=new ArrayList();
        }

        public ConditionsTableModel(List conditions) {
            m_conditions=conditions;
        }

        public List getConditions() {
            return m_conditions;
        }

        public String getColumnName(int col) {
            if (col==0) {
                return "Field";
            } else if (col==1) {
                return "Operator";
            } else {
                return "Value";
            }
        }

        public int getRowCount() {
            return m_conditions.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Object getValueAt(int row, int col) {
            Condition cond=(Condition) m_conditions.get(row);
            if (col==0) {
                return cond.getProperty();
            } else if (col==1) {
                return getNiceName(cond.getOperator().toString());
            } else {
                return cond.getValue();
            }
        }

        private String getNiceName(String operString) {
            if (operString.equals("has")) return "contains";
            if (operString.equals("eq")) return "equals";
            if (operString.equals("lt")) return "is less than";
            if (operString.equals("le")) return "is less than or equal to";
            if (operString.equals("gt")) return "is greater than";
            return "is greater than or equal to";
        }

    }
    
     public class ConditionSelectionListener
            implements ListSelectionListener {

        private int m_selectedRow;
        private JButton m_modifyButton;
        private JButton m_deleteButton;

        public ConditionSelectionListener(JButton modifyButton,
                JButton deleteButton, int selectedRow) {
            m_selectedRow=selectedRow;
            m_modifyButton=modifyButton;
            m_deleteButton=deleteButton;
            updateButtons();
        }

        public void valueChanged(ListSelectionEvent e) {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) return;

            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if (lsm.isSelectionEmpty()) {
                m_selectedRow=-1;
            } else {
                m_selectedRow=lsm.getMinSelectionIndex();
            }
            updateButtons();
        }

        public int getSelectedRow() {
            return m_selectedRow;
        }

        private void updateButtons() {
            if (getSelectedRow()==-1) {
                m_modifyButton.setEnabled(false);
                m_deleteButton.setEnabled(false);
            } else {
                m_modifyButton.setEnabled(true);
                m_deleteButton.setEnabled(true);
            }
        }
    }
      public class AddConditionButtonListener
            implements ActionListener {

        private ConditionsTableModel m_model;

        public AddConditionButtonListener(ConditionsTableModel model) {
            m_model=model;
        }

        public void actionPerformed(ActionEvent e) {
            ModConditionDialog dialog=new ModConditionDialog(m_model, -1);
            dialog.setVisible(true);
        }
    }
      
      public class ChangeConditionButtonListener
            implements ActionListener {

        private ConditionsTableModel m_model;
        private ConditionSelectionListener m_sListener;

        public ChangeConditionButtonListener(ConditionsTableModel model,
                ConditionSelectionListener sListener) {
            m_model=model;
            m_sListener=sListener;
        }

        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            ModConditionDialog dialog=new ModConditionDialog(m_model,
                    m_sListener.getSelectedRow());
            dialog.setVisible(true);
        }
    }

      public class DeleteConditionButtonListener
            implements ActionListener {

        private ConditionsTableModel m_model;
        private ConditionSelectionListener m_sListener;

        public DeleteConditionButtonListener(ConditionsTableModel model,
                ConditionSelectionListener sListener) {
            m_model=model;
            m_sListener=sListener;
        }

        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            int r=m_sListener.getSelectedRow();
            m_model.getConditions().remove(r);
            m_model.fireTableRowsDeleted(r,r);
        }
     }
      

    public class ModConditionDialog
            extends JDialog {

        private ConditionsTableModel m_model;
        private int m_rowNum;
        private JComboBox m_fieldBox;
        private JComboBox m_operatorBox;
        private JTextField m_valueField;
        private String[] s_fieldArray = {"pid", "label", "fType", "bDef",
            "bMech", "cModel", "state", "locker", "cDate", "mDate",
            "dcmDate", "title", "creator", "subject", "description",
            "publisher", "contributor", "date", "type", "format",
            "identifier", "source", "language", "relation", "coverage",
            "rights"};
     private String[] s_operatorArray = {"contains", "equals",
            "is less than", "is less than or equal to", "is greater than",
            "is greater than or equal to"};
    private  String[] s_operatorActuals = {"has", "eq", "lt", "le",
            "gt", "ge"};

        public ModConditionDialog(ConditionsTableModel model, int rowNum) {
            //super(null, "Enter Condition", true);
            m_model=model;
            m_rowNum=rowNum;

            // mainPanel(northPanel, southPanel)

                // NORTH: northPanel(fieldBox,operatorBox,valueField)

                    m_fieldBox=new JComboBox(s_fieldArray);
                    m_operatorBox=new JComboBox(s_operatorArray);
                    m_valueField=new JTextField(10);
                    if (rowNum!=-1) {
                        // if this is an edit, start with current values
                        m_fieldBox.setSelectedIndex(indexOf((String) m_model.getValueAt(rowNum, 0)));
                        m_operatorBox.setSelectedIndex(indexOf((String) m_model.getValueAt(rowNum, 1)));
                        m_valueField.setText((String) m_model.getValueAt(rowNum, 2));
                    }

                JPanel northPanel=new JPanel();
                northPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
                northPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
                northPanel.add(m_fieldBox);
                northPanel.add(m_operatorBox);
                northPanel.add(m_valueField);

                // SOUTH: southPanel(cancelButton, okButton)

                    JButton okButton=new JButton("Ok");
                    okButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            updateModelAndNotify();
                            setVisible(false);
                        }
                    });

                    JButton cancelButton=new JButton("Cancel");
                    cancelButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setVisible(false);
                        }
                    });

                JPanel southPanel=new JPanel();
                southPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
                southPanel.add(okButton);
                southPanel.add(cancelButton);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(northPanel, BorderLayout.NORTH);
            getContentPane().add(southPanel, BorderLayout.SOUTH);
            pack();
            //setLocation(Administrator.getInstance().getCenteredPos(getSize().width, getSize().height));
        }

        private int indexOf(String s) {
            for (int i=0; i<s_fieldArray.length; i++) {
                if (s_fieldArray[i].equals(s)) return i;
            }
            for (int i=0; i<s_operatorArray.length; i++) {
                if (s_operatorArray[i].equals(s)) return i;
            }
            return -1;
        }

        public void updateModelAndNotify() {
            // create a Condition given the current values
            Condition cond=new Condition();
            cond.setProperty(s_fieldArray[m_fieldBox.getSelectedIndex()]);
            cond.setOperator(ComparisonOperator.fromValue(
                    s_operatorActuals[m_operatorBox.getSelectedIndex()]));
            cond.setValue(m_valueField.getText());
            // if rowNum is -1, add it
            if (m_rowNum==-1) {
               // if it wasn't there before, add it
               m_model.getConditions().add(cond);
            } else {
               // else replace existing condition
               m_model.getConditions().set(m_rowNum, cond);
            }
            m_model.fireTableDataChanged();
        }


    }
}
