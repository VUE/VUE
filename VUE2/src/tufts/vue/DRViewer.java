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
                "Cannot Create Digital Repository\n" + ex.getClass().getName() + ":\n" + ex.getMessage(),
                "FEDORA Alert",
                JOptionPane.ERROR_MESSAGE);
        }
        keywords = new JTextField();
        setSearchPanel();
        setAdvancedSearchPanel();
        tabbedPane.addTab("Search" , DRSearch);
        tabbedPane.addTab("Advanced Search",advancedSearchPanel);
        tabbedPane.addTab("Search Results",DRSearchResults);   
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
         JButton addConditionButton=new JButton(VueResources.getImageIcon("addLight"));
         addConditionButton.setPreferredSize(new Dimension(17, 17));
         
         // GRID: modifyConditionButton
         
         // GRID: deleteConditionButton
         JButton deleteConditionButton=new JButton( VueResources.getImageIcon("deleteLight"));
         deleteConditionButton.setPreferredSize(new Dimension(17, 17));
         
         JButton advancedSearchButton = new JButton("Advanced Search");
         advancedSearchButton.setSize(new Dimension(100,20));
         advancedSearchButton.addActionListener(this);
         // Now that buttons are available, register the
         // list selection listener that sets their enabled state.
         conditionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         
         // setting editors for columns
         // field column.
         try {
            JComboBox comboBox = new JComboBox(FedoraUtils.getAdvancedSearchFields((tufts.oki.dr.fedora.DR)dr));
            conditionsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));
            comboBox = new JComboBox(FedoraUtils.getAdvancedSearchOperators((tufts.oki.dr.fedora.DR)dr));
            conditionsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comboBox));
         } catch(Exception ex) {
             System.out.println("Can't set the editors"+ex);
         }
         ConditionSelectionListener sListener= new ConditionSelectionListener(deleteConditionButton, -1);
         conditionsTable.getSelectionModel().addListSelectionListener(sListener);
         // ..and add listeners to the buttons
         
         
         addConditionButton.addActionListener(new AddConditionButtonListener(m_model));
         deleteConditionButton.addActionListener(new DeleteConditionButtonListener(m_model, sListener));
         
         JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
         topPanel.add(addConditionButton);
         topPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
         topPanel.add(deleteConditionButton);
         
         JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
         bottomPanel.add(advancedSearchButton);
         bottomPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
         
        
        
         advancedSearchPanel=new JPanel();
         advancedSearchPanel.setLayout(new BoxLayout(advancedSearchPanel, BoxLayout.Y_AXIS));
         advancedSearchPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
         
         advancedSearchPanel.add(topPanel);
         advancedSearchPanel.add(innerConditionsPanel);
         advancedSearchPanel.add(bottomPanel);
        
        
         //advancedSearchPanel.add(advancedSearchButton,BorderLayout.SOUTH);
         advancedSearchPanel.validate();
     }
     
   
     
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
           
            searchCriteria.setConditions((fedora.server.types.gen.Condition[])m_model.getConditions().toArray(new Condition[0]));
            searchCriteria.setMaxReturns(maxReturns.getSelectedItem().toString());
            resultObjectsIterator = dr.getAssetsBySearch(searchCriteria,advancedSearchType); 
            VueDragTree tree = new VueDragTree(getAssetResourceIterator(resultObjectsIterator),"Fedora Search Results");
            tree.setRootVisible(false);
            JScrollPane jsp = new JScrollPane(tree);
            DRSearchResults.setLayout(new BorderLayout());
            DRSearchResults.add(jsp,BorderLayout.CENTER,0);
            tabbedPane.setSelectedComponent(DRSearchResults);
        } catch (Exception ex) {
            ex.printStackTrace();
                       
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
            Condition cond = new Condition();
            cond.setProperty("label");
            cond.setOperator(ComparisonOperator.has);
            cond.setValue("");
            m_conditions.add(cond);
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
                return "Criteria";
            } else {
                return "Search";
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
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return true;
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
     
        public void setValueAt(Object value, int row, int col) {
            Condition cond;
            if(row == -1)
                cond  = new Condition();
            else
                cond = (Condition) m_conditions.get(row);
            if(col == 0)
                cond.setProperty((String)value);
            if(col == 1) {
                try {
                    cond.setOperator(ComparisonOperator.fromValue(FedoraUtils.getAdvancedSearchOperatorsActuals((tufts.oki.dr.fedora.DR)dr,(String)value)));
                } catch (Exception ex) {
                    System.out.println("Value = "+value+": Not supported -"+ex);
                    cond.setOperator(ComparisonOperator.ge);
                }
            }
            if(col == 2)
                cond.setValue((String)value);
            // row = -1 adds new condions else replace the existing one.
            if (row==-1)
               m_conditions.add(cond);
            else 
                m_conditions.set(row, cond);
            fireTableCellUpdated(row, col);
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
       private JButton m_deleteButton;

        public ConditionSelectionListener(JButton deleteButton, int selectedRow) {
            m_selectedRow=selectedRow;
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
               m_deleteButton.setEnabled(false);
            } else {
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
            Condition cond=new Condition();
            try {
                cond.setProperty(FedoraUtils.getAdvancedSearchFields((tufts.oki.dr.fedora.DR)dr)[0]);
            } catch (Exception ex) {
               cond.setProperty("label");
            }
            cond.setOperator(ComparisonOperator.has);
            cond.setValue("");
            m_model.getConditions().add(cond);
            m_model.fireTableDataChanged();
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
