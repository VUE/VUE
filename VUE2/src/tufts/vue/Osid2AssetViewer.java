 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */


/*
 * OsidAsseViewer.java
 *
 * Created on October 16, 2003, 9:40 AM
 */

package tufts.vue;

/**
 *
 * @author  jkahn
 */

import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import org.osid.repository.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

import tufts.vue.gui.VueButton;
import tufts.oki.repository.fedora.*;
import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;

public class Osid2AssetViewer extends JPanel implements ActionListener,KeyListener {
    
    public int countError = 0;
    ConditionsTableModel m_model;
    JTable conditionsTable;
    JTabbedPane tabbedPane;
    JPanel SearchResults;
    JPanel Search;
    JPanel AdvancedSearch;
    org.osid.repository.AssetIterator resultObjectsIterator;
    
    JTextField keywords;
    JComboBox maxReturns;
    JComboBox searchTypesComboBox;
    JPanel searchTypesPanel;
    JComboBox endusersComboBox;
    CellEditor defaultCellEditor;
    
    java.util.Vector repositoryVector = new java.util.Vector(); // Repositories connected to.
    
    org.osid.repository.AssetIterator assetIterator;
    SearchCriteria searchCriteria;
    JButton searchButton = new JButton();
    JButton advancedSearchButton = new JButton();
    JButton nextButton = new JButton();
    JPanel nextButtonPanel = new JPanel();
    JButton previousButton;
    JLabel noResultsLabel;
    JLabel returnLabel;
    JLabel returnLabelAdvancedSearch;  //label for advanced search
    JComboBox maxReturnsAdvancedSearch;  // combobox for advanced search.
    JScrollPane jsp = new JScrollPane();
    int count = 0;
    String[] maxReturnItems = {
        "10",
        "20",
    };
    private org.osid.OsidContext context = new org.osid.OsidContext();
    
    /** Creates a new instance of Viewer */
    public Osid2AssetViewer(String implementation, org.osid.OsidContext context) throws DataSourceException
    {
        this.context = context;
        setLayout(new BorderLayout());
        SearchResults = new JPanel();
        tabbedPane = new JTabbedPane();
        
        returnLabel = new JLabel("Maximum number of returns?");
        returnLabel.setFont(new Font("Arial",Font.PLAIN, 12));
        
        maxReturns = new JComboBox(maxReturnItems);
        maxReturns.setEditable(true);
        
        // Jlabel and
        returnLabelAdvancedSearch = new JLabel("Maximum number of returns?");
        returnLabelAdvancedSearch.setFont(new Font("Arial",Font.PLAIN, 12));
        
        maxReturnsAdvancedSearch = new JComboBox(maxReturnItems);
        maxReturnsAdvancedSearch.setEditable(true);
        
        searchCriteria  = new SearchCriteria();
//        searchType = new SearchType("Search");
//        advancedSearchType = new SearchType("Advanced Search");

        try 
        {
            org.osid.OsidManager osidManager = osidManager = tufts.vue.OsidLoader.getManager(
                "org.osid.repository.RepositoryManager",
                implementation,
                context,
                new java.util.Properties());            
            org.osid.repository.RepositoryManager repositoryManager = (org.osid.repository.RepositoryManager)osidManager;
            org.osid.repository.RepositoryIterator repositoryIterator = repositoryManager.getRepositories();
            while (repositoryIterator.hasNextRepository())
            {
                this.repositoryVector.addElement(repositoryIterator.nextRepository());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        keywords = new JTextField();
        setSearchTypes();
        setSearchPanel();
        setAdvancedSearchPanel();
        tabbedPane.addTab("Type" , searchTypesPanel);
        tabbedPane.addTab("Search" , Search);
        tabbedPane.addTab("Advanced Search",AdvancedSearch);
        tabbedPane.addTab("Search Results",SearchResults);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                /**
                 * if(((JTabbedPane)e.getSource()).getSelectedComponent() == DRViewer.this.Search) {
                 * DRViewer.this.SearchResults.removeAll();
                 * }
                 */
            }
        });
        add(tabbedPane,BorderLayout.CENTER);
System.out.println("abc");
    }
    
    private void setSearchTypes()
    {
        //Create a combo box for the search types
        java.util.Vector typeVector = new java.util.Vector();
        try
        {
            for (int i=0, size = this.repositoryVector.size(); i < size; i++)
            {
                org.osid.shared.TypeIterator typeIterator = 
                    ((org.osid.repository.Repository)this.repositoryVector.elementAt(i)).getSearchTypes();
                while (typeIterator.hasNextType())
                {
                    org.osid.shared.Type type = typeIterator.nextType();
                    String s = new String(type.getAuthority() + "," + type.getDomain() + "," + type.getKeyword());
                    // avoid duplicates
                    if (typeVector.indexOf(s) == -1)
                    {
                        typeVector.addElement(s);
                    }
                }
            }
        }
        catch (Throwable t)
        {
        }
        String items[] = new String[typeVector.size()];
        for (int i=0; i < typeVector.size(); i++)
        {
            items[i] = (String)typeVector.elementAt(i);
        }
        searchTypesComboBox = new javax.swing.JComboBox(items);
        searchTypesComboBox.setVisible(true);
        searchTypesComboBox.setEditable(false);
        
        this.searchTypesPanel = new JPanel();
        JLabel typesLabel = new JLabel("Search Types: ");
        typesLabel.setFont(new Font("Arial",Font.PLAIN, 12));
        searchTypesPanel.add(typesLabel);
        searchTypesPanel.add(searchTypesComboBox);
        searchTypesPanel.setVisible(true);
        searchTypesComboBox.validate();
        searchTypesPanel.validate();
    }
    
    /**
     * @Setup  searchPanel
     */
    
    
    private void  setSearchPanel() {
        Search = new JPanel(new BorderLayout());
        Search.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        JPanel SearchPanel = new JPanel();
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        SearchPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
                
        //adding the label Keywords
        c.gridx = 0;
        c.gridy = 1;
        
        c.insets = new Insets(10,2,2, 2);
        JLabel keyLabel = new JLabel("Keywords: ");
        keyLabel.setFont(new Font("Arial",Font.PLAIN, 12));
        gridbag.setConstraints(keyLabel, c);
        SearchPanel.add(keyLabel);
        
        //adding the serch box
        c.gridx=1;
        c.gridy=1;
        c.gridwidth= 2;
        c.insets = new Insets(10, 2,2, 2);
        //keywords.setPreferredSize(new Dimension(120,20));
        keywords.addKeyListener(this);
        gridbag.setConstraints(keywords, c);
        SearchPanel.add(keywords);
        
        // adding the number of search results tab.
        c.gridx=0;
        c.gridy=2;
        c.gridwidth=2;
        c.insets = defaultInsets;
        
        gridbag.setConstraints(returnLabel, c);
        SearchPanel.add(returnLabel);
        
        c.gridx=2;
        c.gridy=2;
        c.gridwidth=1;
        // maxReturns.setPreferredSize(new Dimension(40,20));
        gridbag.setConstraints(maxReturns,c);
        SearchPanel.add(maxReturns);
        
        c.gridx=2;
        c.gridy=3;
        c.insets = new Insets(10, 2,2,2);
        searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(40,20));
        searchButton.addActionListener(this);
        gridbag.setConstraints(searchButton,c);
        SearchPanel.add(searchButton);
        Search.add(SearchPanel,BorderLayout.NORTH);
        Search.validate();        
    }
    
    private void setAdvancedSearchPanel() {
        AdvancedSearch= new JPanel(new BorderLayout());
        m_model=new ConditionsTableModel();
        conditionsTable=new JTable(m_model);
        conditionsTable.setPreferredScrollableViewportSize(new Dimension(100,100));
        conditionsTable.addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                if(conditionsTable.isEditing()) {
                    conditionsTable.getCellEditor(conditionsTable.getEditingRow(),conditionsTable.getEditingColumn()).stopCellEditing();
                }
                conditionsTable.removeEditor();
            }
            public void focusGained(FocusEvent e) {
            }
        });
        JScrollPane conditionsScrollPane=new JScrollPane(conditionsTable);
        conditionsScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel innerConditionsPanel=new JPanel();
        innerConditionsPanel.setLayout(new BorderLayout());
        innerConditionsPanel.add(conditionsScrollPane, BorderLayout.CENTER);
        
        // GRID: addConditionButton
        JButton addConditionButton=new VueButton("add");
        addConditionButton.setBackground(this.getBackground());
        addConditionButton.setToolTipText("Add Condition");
        // GRID: deleteConditionButton
        JButton deleteConditionButton=new VueButton("delete");
        deleteConditionButton.setBackground(this.getBackground());
        deleteConditionButton.setToolTipText("Delete Condition");
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setToolTipText("Add or Delete conditions using +/- buttons. Click on table cell to modify  conditions");
        
        advancedSearchButton = new JButton("Advanced Search");
        advancedSearchButton.setSize(new Dimension(100,20));
        advancedSearchButton.setEnabled(false);
        advancedSearchButton.addActionListener(this);
        // Now that buttons are available, register the
        // list selection listener that sets their enabled state.
        conditionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // setting editors for columns
        // field column.
        try {
            String fields[] = { "identifier","title","language","description","end user", "age range" };
            String endusers[] = { "author","counsellor","learner","manager","parent","teacher","other" };
            String operators[] = { "eq","lt","lte","gt","gte" };
            
            JComboBox fieldsComboBox = new JComboBox(fields);
            fieldsComboBox.addActionListener(this);
            this.endusersComboBox = new JComboBox(endusers);
            JComboBox operatorsComboBox = new JComboBox(operators);
            
            conditionsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(fieldsComboBox));
            conditionsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(operatorsComboBox));
            this.defaultCellEditor = conditionsTable.getColumnModel().getColumn(2).getCellEditor();
        } catch(Exception ex) {
            System.out.println("Can't set the editors"+ex);
        }
        
        ConditionSelectionListener sListener= new ConditionSelectionListener(deleteConditionButton, -1);
        conditionsTable.getSelectionModel().addListSelectionListener(sListener);
        // ..and add listeners to the buttons
        
        
        addConditionButton.addActionListener(new AddConditionButtonListener(m_model));
        deleteConditionButton.addActionListener(new DeleteConditionButtonListener(m_model, sListener));
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        topPanel.add(addConditionButton);
        topPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,0));
        topPanel.add(deleteConditionButton);
        topPanel.add(questionLabel);
        
        JPanel returnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2, 0));
        returnPanel.setBorder(BorderFactory.createEmptyBorder(4,6,6,0));
        returnPanel.add(returnLabelAdvancedSearch);
        returnPanel.add(maxReturnsAdvancedSearch);
        
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        bottomPanel.add(advancedSearchButton);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,0));
        
        JPanel advancedSearchPanel=new JPanel();
        advancedSearchPanel.setLayout(new BoxLayout(advancedSearchPanel, BoxLayout.Y_AXIS));
        advancedSearchPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        
        advancedSearchPanel.add(topPanel);
        advancedSearchPanel.add(innerConditionsPanel);
        advancedSearchPanel.add(returnPanel);
        advancedSearchPanel.add(bottomPanel);
        
        AdvancedSearch.add(advancedSearchPanel,BorderLayout.NORTH);
        AdvancedSearch.validate();
        //advancedSearchPanel.add(advancedSearchButton,BorderLayout.SOUTH);
        
    }
    
    
     /*
      *@ sets up search result panel.
      */
    
    private void setSearchResultsPanel() {
        
        // need to organize this part
        //noResultsLabel = new JLabel("Search Returned no Results");
        //noResultsLabel.setFont(VueConstants.MediumFont);
        //noResultsLabel.setBackground(Color.WHITE);
        SearchResults.remove(jsp);
        nextButtonPanel.remove(nextButton);
        SearchResults.remove(nextButtonPanel);
        
        nextButton = new JButton("More");
        nextButton.addActionListener(this);
        nextButtonPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        
        nextButtonPanel.add(nextButton);
        nextButtonPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,0));
        SearchResults.setLayout(new BorderLayout());
        
        
        SearchResults.add(jsp,BorderLayout.CENTER,0);
        /**
         * if(searchCriteria.getResults() == 0)
         * SearchResults.add(this.noResultsLabel,BorderLayout.NORTH,0);
         */
        if(searchCriteria.getToken() != null)
            SearchResults.add(nextButtonPanel,BorderLayout.SOUTH,0);
        SearchResults.validate();
        tabbedPane.setSelectedComponent(SearchResults);
    }
    
    
    public org.osid.shared.Type stringToType(String typeString)
    {
        String authority = "";
        String domain = "";
        String keyword = "";
        
        java.util.StringTokenizer st = new java.util.StringTokenizer(typeString,",");
        int tokenCount = 0;
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (tokenCount == 0)
            {
                authority = token;
            }
            else if (tokenCount == 1)
            {
                domain = token;
            }
            else if (tokenCount == 2)
            {
                keyword = token;
            }
            tokenCount++;
        }
        return new tufts.oki.repository.fedora.Type(authority,domain,keyword,"");
    }

    private void performSearch() 
    {
        try 
        {
            searchButton.setEnabled(false);
            System.out.println("Searching OSID Repository...");
            
            org.osid.shared.Type searchType = null;
            String selectedType = (String)(this.searchTypesComboBox.getSelectedItem());
            if (selectedType != null)
            {            
                searchType = stringToType(selectedType);
            }
            VueDragTree tree = null;
            
            java.util.Vector results = new java.util.Vector();
            org.osid.shared.Properties sharedProperties = new tufts.oki.shared2.SharedProperties();
            String criteria = keywords.getText();
            
            org.osid.repository.Repository nextRepository = null;
            for (int j=0, size = this.repositoryVector.size(); j < size; j++)
            {
                nextRepository = (org.osid.repository.Repository)this.repositoryVector.elementAt(j);
                //optionally add a separate thread here
                try
                {
                    org.osid.repository.AssetIterator assetIterator = 
                        nextRepository.getAssetsBySearch(criteria,searchType,sharedProperties);
                    while (assetIterator.hasNextAsset())
                    {
                        results.addElement(new Osid2AssetResource(assetIterator.nextAsset(),this.context));
                    }
                }
                catch (Throwable t) 
                {
                    t.printStackTrace();
                }
            }            
                        
//            resultObjectsIterator = new tufts.oki.repository.fedora.AssetIterator(results);
//            resultObjectsIterator = nextRepository.getAssetsBySearch(criteria,searchType,sharedProperties);
//            tree = new VueDragTree(getOsidAssetResourceIterator(resultObjectsIterator),"Repository Search Results");
            tree = new VueDragTree(results.iterator(),"Repository Search Results");

            tree.setRootVisible(false);
            jsp = new JScrollPane(tree);
            setSearchResultsPanel();
            countError = 0;
        }  catch (Throwable t) {
                VueUtil.alert(this, "Exception thrown during getAssetsBySearch() implementation","Search Error");
            System.out.println("OSID Viewer performing search :"+t.getMessage());
        } finally {
            searchButton.setEnabled(true);
        }
    }
    
    private void performAdvancedSearch() {
        try {
            performSearch();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
//            advancedSearchButton.setEnabled(true);
        }
    }
    
    private void performMoreSearch() {
        try {
            nextButton.setEnabled(false);
            searchCriteria.setSearchOperation(SearchCriteria.RESUME_FIND_OBJECTS);
            searchCriteria.setResults(0);

            String selectedType = (String)(this.searchTypesComboBox.getSelectedItem());
            org.osid.shared.Type searchType = stringToType(selectedType);
            org.osid.shared.Properties sharedProperties = new tufts.oki.shared2.SharedProperties();
            String criteria = keywords.getText();
            
            VueDragTree tree = null;

            java.util.Vector results = new java.util.Vector();
            for (int j=0, size = this.repositoryVector.size(); j < size; j++)
            {
                org.osid.repository.Repository nextRepository = (org.osid.repository.Repository)this.repositoryVector.elementAt(j);
                //optionally add a separate thread here
                try
                {
                    org.osid.repository.AssetIterator assetIterator = 
                        nextRepository.getAssetsBySearch(criteria,searchType,sharedProperties);
                    while (assetIterator.hasNextAsset())
                    {
                        results.addElement(assetIterator.nextAsset());
                    }
                }
                catch (Throwable t) 
                {
                    // log exceptions but don't stop searching
                    t.printStackTrace();
                }
            }            
                        
            resultObjectsIterator = new tufts.oki.repository.fedora.AssetIterator(results);
            tree = new VueDragTree(resultObjectsIterator,"Search Results");

            tree.setRootVisible(false);
            jsp = new JScrollPane(tree);
            setSearchResultsPanel();
            countError = 0;
        } catch (Throwable t) {
            VueUtil.alert(this, "","Search Error");
            System.out.println("Viewer.performMoreSearch :"+t.getMessage());
        }finally {
            nextButton.setEnabled(true);
        }
    }    
    
    private Iterator getOsidAssetResourceIterator(org.osid.repository.AssetIterator assetIterator)  
    throws org.osid.repository.RepositoryException
    {
        Vector resources = new Vector();
        while(assetIterator.hasNextAsset()) 
        {
            org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
            resources.add(new Osid2AssetResource(nextAsset,this.context));
        }
        return resources.iterator();
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Search")) {
            Thread t = new Thread() {
                public void run() {
                    performSearch();
                }
            };
            t.start();
        }
        if(e.getActionCommand().equals("Advanced Search")) {
            Thread t = new Thread() {
                public void run() {
                    performAdvancedSearch();
                }
            };
            t.start();
            
        }
        if(e.getActionCommand().equals("More")) {
            Thread t = new Thread() {
                public void run() {
                    performMoreSearch();
                }
            };
            t.start();
        }
    
/*
        String selection = (String)(((JComboBox)e.getSource()).getSelectedItem());
        if (selection.equals("end user"))
        {
            this.conditionsTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(this.endusersComboBox));
        }
        else
        {
            this.conditionsTable.getColumnModel().getColumn(2).setCellEditor(this.defaultCellEditor);
        }
*/
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar()== KeyEvent.VK_ENTER) {
            if(e.getComponent() == keywords) {
                searchButton.doClick();
            } else {
//                advancedSearchButton.doClick();
            }
        }
    }
    
    public org.osid.repository.Repository getRepository() {
        // who calls this???
        return (org.osid.repository.Repository)this.repositoryVector.firstElement();
    }
    
    
    public class ConditionsTableModel extends AbstractTableModel {
        
        List m_conditions;
        
        public ConditionsTableModel() {
            m_conditions=new ArrayList();
            Condition cond = new Condition();
            cond.setProperty("");
            cond.setOperator(ComparisonOperator.eq);
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
/*
            if(col == 1) {
                try {
                    cond.setOperator(ComparisonOperator.fromValue(FedoraUtils.getAdvancedSearchOperatorsActuals((tufts.oki.dr.fedora.DR)dr,(String)value)));
                } catch (Exception ex) {
                    System.out.println("Value = "+value+": Not supported -"+ex);
                    cond.setOperator(ComparisonOperator.ge);
                }
            }
*/
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
            if (operString.equals("eq")) return "eq";
            if (operString.equals("lt")) return "lt";
            if (operString.equals("lte")) return "lte";
            if (operString.equals("gt")) return "gt";
            return "gte";
        }
        
    }
    //todo: need to have single thread for all searches
    public class SearchThread extends Thread {
        public void run() {
            
        }
    }
    
    public class ConditionSelectionListener  implements ListSelectionListener {
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
/*
            try {
                cond.setProperty(FedoraUtils.getAdvancedSearchFields((tufts.oki.dr.fedora.DR)dr)[0]);
            } catch (Exception ex) {
*/
                cond.setProperty("label");
//            }
            cond.setOperator(ComparisonOperator.has);
            cond.setValue("");
            m_model.getConditions().add(cond);
            m_model.fireTableDataChanged();
        }
        
    }
    
    public class DeleteConditionButtonListener implements ActionListener {
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
}
