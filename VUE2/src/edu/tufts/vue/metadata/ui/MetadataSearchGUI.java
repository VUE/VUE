
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.metadata.ui;

import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.metadata.action.*;
import edu.tufts.vue.ontology.OntType;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;
import javax.swing.table.*;
import tufts.vue.*;

import java.util.*;

/*
 * MetadataSearchGUI.java
 *
 *
 * Created on July 19, 2007, 1:31 PM
 *
 * @author dhelle01
 */
public class MetadataSearchGUI extends JPanel {
    
    public static final int ONE_LINE = 0;
    public static final int MULTIPLE_FIELDS = 1;
    
    // for best results: modify next two in tandem (at exchange rate of one pixel from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    public final static int ROW_HEIGHT = 39;
    public final static int ROW_GAP = 7;
    
    public final static int ROW_INSET = 5;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    //ONE_LINE
    private JTextField searchField;
    private JTable searchTable;
    private JButton searchButton;
    private List<URI> found = null;
    private List<List<URI>> finds = null;
    
    //TEXT FIELD BASED
    private JPanel optionsPanel;
    private JComboBox searchTypesChoice;
    private String[] searchTypes = {"Basic","Categories","Advanced"};
    
    private JPanel fieldsPanel;
    private JTable searchTermsTable;

    private int buttonColumn = 1;
    
    private List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
    
    public MetadataSearchGUI() 
    {
        setUpOneLineSearch();      
    }
    
    public MetadataSearchGUI(int type)
    {
        
        if(type == ONE_LINE)
        {
           setUpOneLineSearch();
        }
        else
        {
           setUpFieldsSearch();
        }
        
    }
    
    public void setUpOneLineSearch()
    {
        setLayout(new BorderLayout());
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(9,9,9,9),
                              BorderFactory.createLineBorder(new java.awt.Color(200,200,200),1)));
        JPanel buttonPanel = new JPanel(new BorderLayout());
        searchButton = new JButton(new SearchAction(searchField));
        searchButton.setBackground(java.awt.Color.WHITE);
        buttonPanel.setBackground(java.awt.Color.WHITE);
        buttonPanel.add(BorderLayout.EAST,searchButton);
        add(BorderLayout.NORTH,searchField);
        add(buttonPanel);
    }
    
    public void setUpFieldsSearch()
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        optionsPanel = new JPanel();
        fieldsPanel = new JPanel(new java.awt.BorderLayout());
        
        
         /*VueMetadataElement newElement = new VueMetadataElement();
         String pairedObject[] = {"Tag",""};
         newElement.setObject(pairedObject);
         newElement.setType(VueMetadataElement.CATEGORY);
         searchTerms.add(newElement);*/
               //((SearchTermsTableModel)searchTermsTable.getModel()).refresh();
        
        searchTypesChoice = new JComboBox(searchTypes);
        final JLabel optionsLabel = new JLabel("show options");
        final JButton advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
        advancedSearch.setBorder(BorderFactory.createEmptyBorder());
        advancedSearch.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent e)
           {
              /*System.out.println("advanced search button in search gui");
               if(searchTermsTable!=null)
               {
                 System.out.println("advanced search button in search gui - changing columns");
                 ((SearchTermsTableModel)searchTermsTable.getModel()).setColumns(3);
               }*/
               
               SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
               if(model.getColumnCount() == 2)
               {
                 buttonColumn = 2;
                 model.setColumns(3);
                 advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
                 optionsLabel.setText("hide options");
               }
               else
               {
                 buttonColumn = 1;
                 model.setColumns(2);
                 advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
                 optionsLabel.setText("show options");
               }
               
               adjustColumnModel(); 
           }
        });
        optionsPanel.add(advancedSearch);
        optionsPanel.add(optionsLabel);
        
        
        searchTermsTable = new JTable(new SearchTermsTableModel());
        adjustColumnModel();
        searchTermsTable.setDefaultRenderer(java.lang.Object.class,new SearchTermsTableRenderer());
        searchTermsTable.setDefaultEditor(java.lang.Object.class,new SearchTermsTableEditor());
        searchTermsTable.setRowHeight(ROW_HEIGHT);
        searchTermsTable.getTableHeader().setReorderingAllowed(false);
        searchTermsTable.setGridColor(new java.awt.Color(getBackground().getRed(),getBackground().getBlue(),getBackground().getGreen(),0));
        searchTermsTable.setBackground(getBackground());
        searchTermsTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter(){
           public void mousePressed(MouseEvent e)
           {
               VueMetadataElement newElement = new VueMetadataElement();
               String pairedObject[] = {"Tag",""};
               newElement.setObject(pairedObject);
               newElement.setType(VueMetadataElement.CATEGORY);
               searchTerms.add(newElement);
               ((SearchTermsTableModel)searchTermsTable.getModel()).refresh();
           }
        });
        JScrollPane scroll = new JScrollPane(searchTermsTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(getBackground());
        fieldsPanel.add(scroll);
        
        add(optionsPanel);
        add(fieldsPanel);
        
        JPanel buttonPanel = new JPanel(new BorderLayout());
        searchButton = new JButton(new SearchAction(searchTerms));
        searchButton.setBackground(java.awt.Color.WHITE);
        buttonPanel.setBackground(java.awt.Color.WHITE);
        buttonPanel.add(BorderLayout.EAST,searchButton);
        //add(BorderLayout.NORTH,searchField);
        add(buttonPanel);
    }
    
    public void adjustColumnModel()
    {
        int editorWidth = this.getWidth();
        if(searchTermsTable.getModel().getColumnCount() == 2)
        {
          searchTermsTable.getColumnModel().getColumn(0).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(1).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(0).setMinWidth(editorWidth-BUTTON_COL_WIDTH);
          searchTermsTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);   
        }
        else
        {
          searchTermsTable.getColumnModel().getColumn(0).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(1).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(2).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(0).setMaxWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          searchTermsTable.getColumnModel().getColumn(1).setMaxWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          searchTermsTable.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH); 
        }
    }
    
    public void findCategory(Object currValue,int row,int col,JComboBox categories)
    {
   
        int n = categories.getModel().getSize();
        for(int i=0;i<n;i++)
        {
                   
           Object item = categories.getModel().getElementAt(i);
           String currLabel = "";
           if(currValue instanceof OntType)
              currLabel = ((OntType)currValue).getLabel();
           else
              currLabel = currValue.toString();
                  
                   
           if(item instanceof OntType && ((OntType)item).getLabel().equals(currLabel))
           {
              categories.setSelectedIndex(i);
           }
                   
        }
               
    }

    
    public java.awt.Component createRendererComponent(Object value,final int row,int col)
    {   
           JPanel comp = new JPanel();
           
           comp.setLayout(new java.awt.BorderLayout()); 
           
           if(col == (buttonColumn - 1))
           {
             final JTextField field = new JTextField();
             field.addFocusListener(new FocusAdapter(){
                public void focusLost(FocusEvent fe)
                {
                    VueMetadataElement searchTerm = searchTerms.get(row);
                    //Object searchTerm = searchTerms.get(row).getObject();
                    if(searchTerm.getObject() instanceof String[])
                    {
                        VueMetadataElement newElement = new VueMetadataElement();
                        String[] newTerm = {((String[])searchTerms.get(row).getObject())[0],field.getText()};
                        searchTerm.setObject(newTerm);
                        newElement.setType(VueMetadataElement.CATEGORY);
                    }
                }
             });
             field.setText(value.toString());
             comp.add(field);
           }
           else
           if(col == (buttonColumn - 2))
           {           
             final JComboBox categories = new JComboBox();
             categories.setModel(new CategoryComboBoxModel());
             categories.setRenderer(new CategoryComboBoxRenderer());
             
             categories.addItemListener(new ItemListener(){
                public void itemStateChanged(ItemEvent ie)
                {
                    if(ie.getStateChange() == ItemEvent.SELECTED)
                    {
                        
                        if(!(categories.getSelectedItem() instanceof OntType))
                            return;
                        
                        //String[] pairedObject = {searchTerms.get(row).getValue(),categories.getModel().getElementAt(row).toString()};
                        String[] pairedObject = {((OntType)categories.getSelectedItem()).getLabel(),searchTerms.get(row).getValue()};
                        VueMetadataElement ele = new VueMetadataElement();
                        ele.setType(VueMetadataElement.CATEGORY);
                        ele.setObject(pairedObject);
                        searchTerms.set(row,ele);

                    }
                }
             });
             
             Object currValueObject = searchTerms.get(row).getObject();
             if(currValueObject instanceof String[])
             {    
               findCategory(((String[])searchTerms.get(row).getObject())[0],row,col,categories);
             }
             
             comp.add(categories);
           }
           else
           if(col == buttonColumn)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           else
           {
             comp.add(new JLabel(value.toString()));
           }
           
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
          
           return comp;
    }
    
    class SearchTermsTableHeaderRenderer extends DefaultTableCellRenderer
    {
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
        {
            JLabel comp = new JLabel();
            if(col == buttonColumn)
              comp.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
            else if(table.getModel().getColumnCount() == 2 || col == buttonColumn - 2)
              comp.setText("Keywords:");
            else
              comp.setText("");
               
            comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
            comp.setOpaque(true);
            comp.setBackground(MetadataSearchGUI.this.getBackground());
            return comp;
        }
    }
    
    class SearchTermsTableRenderer extends DefaultTableCellRenderer
    {
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
        {
            //setText(value.toString());
            //setBackground(java.awt.Color.BLUE);
            //return this;
            return createRendererComponent(value,row,col);
        }
    }
    
    class SearchTermsTableEditor extends DefaultCellEditor
    {
        public SearchTermsTableEditor()
        {
            super(new JTextField());
        }
        
        public java.awt.Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int col)
        {
            //JLabel label = new JLabel();
            //label.setBackground(java.awt.Color.GREEN);
            //return label;
            return createRendererComponent(value,row,col);
        }
    }
    
    class SearchTermsTableModel extends AbstractTableModel
    {
        
        //public List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
        //private java.util.List<String> searchTerms = new java.util.ArrayList<String>();
        private int columns = 2;
        
       /* public SearchTermsTableModel()
        {
        }*/
        
        public int getRowCount()
        {
            return searchTerms.size();
        }
        
        public int getColumnCount()
        {
            return columns;
        }
        
        public void setColumns(int columns)
        {
            this.columns = columns;
            fireTableStructureChanged();
        }
        
        public boolean isCellEditable(int row,int col)
        {
            if( (col == buttonColumn - 1) || (col == buttonColumn - 2) )
                return true;
            else
                return false;
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == buttonColumn)
              return "delete button";
            else
            if(col == buttonColumn - 1)
              return searchTerms.get(row).getValue();
            else
              return searchTerms.get(row).getKey();
        }
        
        public void refresh()
        {
            fireTableDataChanged();
        }
        
    }
        
}
