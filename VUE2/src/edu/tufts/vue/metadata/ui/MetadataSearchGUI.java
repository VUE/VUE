
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
    
    public final static int SHOW_OPTIONS = 1;
    public final static int HIDE_OPTIONS = 0;
    
    public final static String SELECTED_MAP_STRING = "Selected Map";
    public final static String ALL_MAPS_STRING = "All Open Maps";
    
    //ONE_LINE
    private JTextField searchField;
    private JTable searchTable;
    private JButton searchButton;
    private List<URI> found = null;
    private List<List<URI>> finds = null;
    
    //TEXT FIELD BASED
    private JPanel optionsPanel;
    private JPanel locationPanel;
    private JPanel resultsTypePanel;
    private JComboBox resultsTypeChoice;
    private JComboBox locationChoice;
    private JComboBox searchTypesChoice;
    private String[] searchTypes = {"Basic","Categories","Advanced","All"};
    private String[] locationTypes = {SELECTED_MAP_STRING,ALL_MAPS_STRING};
    private String[] resultsTypes = {"Show","Hide","Select"};
    
    private JPanel fieldsPanel;
    private JTable searchTermsTable;

    private int buttonColumn = 1;
    private int categoryColumn = -1;
    private int valueColumn = 0;
    private int conditionColumn = -1;
    
    private List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
    
    private JButton advancedSearch;
    private JLabel optionsLabel;
    
    private int optionsToggle;
    
    private SearchAction termsAction;
    private boolean singleLine = false;
    private JTextField allSearchField = new JTextField();
    private SearchAction allSearch = new SearchAction(allSearchField);
    
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
        
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
    }
    
    public void setUpOneLineSearch()
    {
        setLayout(new BorderLayout());
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(9,9,9,9),
                              BorderFactory.createLineBorder(new java.awt.Color(200,200,200),1)));
        JPanel buttonPanel = new JPanel(new BorderLayout());
        searchButton = new JButton(new SearchAction(searchField));
        //searchButton.setBackground(java.awt.Color.WHITE);
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(BorderLayout.EAST,searchButton);
        add(BorderLayout.NORTH,searchField);
        add(buttonPanel);
    }
    
    public void setUpFieldsSearch()
    {
       // setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        
        //setLayout(new GridBagLayout());
        //GridbagConstraints c = new GridBagConstraints();
        
        final JButton options = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));//tufts.vue.gui.VueButton("advancedSearchMore");
        
        //optionsPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        //locationPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        //resultsTypePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        
        //optionsPanel = new JPanel(new java.awt.GridLayout(3,2));
        optionsPanel = new JPanel();
        GridBagLayout optionsGrid = new GridBagLayout();
        optionsPanel.setLayout(optionsGrid);
        GridBagConstraints optionsConstraints = new GridBagConstraints();
        
        //linePanel 
        
        /*tufts.vue.PolygonIcon lineIcon = //new tufts.vue.PolygonIcon(java.awt.Color.BLACK);//new java.awt.Color(153,153,153))
                                         new tufts.vue.PolygonIcon(new java.awt.Color(153,153,153))
        {
            public java.awt.Dimension getMinimumSize()
            {
                return new java.awt.Dimension(20,MetadataSearchGUI.this.getWidth()-20);
            }
        };*/
        //lineIcon.setIconWidth(getWidth()-10);
        //lineIcon.setIconWidth(50);
        //lineIcon.setIconHeight(1);
        //JLabel lineLabel = new JLabel(lineIcon);
        //JLabel lineLabel = new JLabel("test");
        
        JPanel linePanel = new JPanel() {
                protected void paintComponent(java.awt.Graphics g) {
                    //g.setColor(java.awt.Color.WHITE);
                    //g.fillRect(0,0,getWidth(),getHeight());
                    g.setColor(java.awt.Color.DARK_GRAY);
                    g.drawLine(5,getHeight()/2,/* this.getSize().width*/ MetadataSearchGUI.this.getWidth()-15, getHeight()/2);
                }
                
                public java.awt.Dimension getMinimumSize()
                {
                        return new java.awt.Dimension(MetadataSearchGUI.this.getWidth(),30);
                }
        };
        
        //linePanel.setOpaque(true);
        //linePanel.setMinimumSize(new java.awt.Dimension(300,30));
        //linePanel.setBackground(java.awt.Color.BLUE);
        
        fieldsPanel = new JPanel(new java.awt.BorderLayout());
        
        searchTypesChoice = new JComboBox(searchTypes)
        {
            public java.awt.Dimension getMinimumSize()
            {
                return new java.awt.Dimension(125,super.getHeight());
            }
        };
        /*    
            public java.awt.Dimension getPreferredSize()
            {
                return new java.awt.Dimension(250,super.getHeight());
            }
        };*/
        searchTypesChoice.addItemListener(new ItemListener()
        {
           public void itemStateChanged(ItemEvent ie)
           {
               if(ie.getStateChange() == ItemEvent.SELECTED)
               {
                   if(ie.getItem().equals("Basic"))
                   {
                       //System.out.println("Basic search selected");
                       setBasicSearch();
                   }
                   if(ie.getItem().equals("Categories"))
                   {
                       //System.out.println("Category search selected");
                       setCategorySearch();
                   }
                   if(ie.getItem().equals("Advanced"))
                   {
                       //System.out.println("Category search selected");
                       setConditionSearch();
                   }
                   if(ie.getItem().equals("All"))
                   {
                       //System.out.println("All search selected");
                       setAllSearch();
                   }
               }
           }
        });
        
        locationChoice = new JComboBox(locationTypes)
        {
            public java.awt.Dimension getMinimumSize()
            {
                return new java.awt.Dimension(125,super.getHeight());
            }
        };  
        /*    public java.awt.Dimension getPreferredSize()
            {
                return new java.awt.Dimension(250,super.getHeight());
            }
        };*/
        locationChoice.addItemListener(new ItemListener()
        {
           public void itemStateChanged(ItemEvent e)
           {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
                 String type = locationChoice.getSelectedItem().toString();
                 
                 if(type.equals(ALL_MAPS_STRING))
                 {    
                   allSearch.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
                   termsAction.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
                 }
                 else // SELECTED_MAP_STRING as current default
                 {
                   allSearch.setLocationType(SearchAction.SEARCH_SELECTED_MAP); 
                   termsAction.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
                 }    
               }
           }
        });
        
        resultsTypeChoice = new JComboBox(resultsTypes)
        {
            public java.awt.Dimension getMinimumSize()
            {
                return new java.awt.Dimension(125,super.getHeight());
            }
        };
        
        //resultsTypes.setFont(tufts.vue.gui.GUI.LabelFace);
       /*     
            public java.awt.Dimension getPreferredSize()
            {
                return new java.awt.Dimension(250,super.getHeight());
            }
        };*/
        resultsTypeChoice.addItemListener(new ItemListener()
        {
           public void itemStateChanged(ItemEvent e)
           {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
                 allSearch.setResultsType(resultsTypeChoice.getSelectedItem().toString());
                 termsAction.setResultsType(resultsTypeChoice.getSelectedItem().toString());
               }
           }
        });
        
        optionsLabel = new JLabel("show options");
        advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
        advancedSearch.setBorder(BorderFactory.createEmptyBorder());
        advancedSearch.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent e)
           {   
               toggleCategorySearch();
           }
        });
        //optionsPanel.add(advancedSearch);
        //optionsPanel.add(optionsLabel);
        
        JLabel searchTypeLabel = new JLabel("Search Type: ",JLabel.RIGHT);
        JLabel locationLabel = new JLabel("Location: ",JLabel.RIGHT);
        JLabel resultsTypeLabel = new JLabel("Results Type: ",JLabel.RIGHT);
        
        searchTypeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        locationLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        resultsTypeLabel.setFont(tufts.vue.gui.GUI.LabelFace);

                //SpringUtilities.makeCompactGrid(optionsPanel,3,2,5,5,5,5);
        
        optionsConstraints.anchor = GridBagConstraints.EAST;
        optionsConstraints.weightx = 0.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.gridwidth = 1; //GridBagConstraints.RELATIVE;
        optionsGrid.setConstraints(searchTypeLabel,optionsConstraints);
        optionsPanel.add(searchTypeLabel);
        optionsConstraints.anchor = GridBagConstraints.WEST;
        optionsConstraints.weightx = 0.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.gridwidth = 1;//GridBagConstraints.REMAINDER;
        optionsGrid.setConstraints(searchTypesChoice,optionsConstraints);
        optionsPanel.add(searchTypesChoice);
        
        optionsConstraints.anchor = GridBagConstraints.WEST;
        optionsConstraints.weightx = 1.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JPanel filler = new JPanel();
        optionsGrid.setConstraints(filler,optionsConstraints);
        optionsPanel.add(filler);
        
        optionsConstraints.anchor = GridBagConstraints.EAST;
        optionsConstraints.weightx = 0.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.NONE;
        optionsConstraints.gridwidth = 1; //GridBagConstraints.RELATIVE;
        optionsGrid.setConstraints(locationLabel,optionsConstraints);
        optionsPanel.add(locationLabel);
        optionsConstraints.anchor = GridBagConstraints.WEST;
        optionsConstraints.weightx = 0.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.gridwidth = 1;//GridBagConstraints.NONE;
        optionsGrid.setConstraints(locationChoice,optionsConstraints);
        optionsPanel.add(locationChoice);
        
        optionsConstraints.anchor = GridBagConstraints.WEST;
        optionsConstraints.weightx = 1.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JPanel filler2 = new JPanel();
        optionsGrid.setConstraints(filler2,optionsConstraints);
        optionsPanel.add(filler2);
        
        optionsConstraints.anchor = GridBagConstraints.EAST;
        optionsConstraints.weightx = 0.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.NONE;
        optionsConstraints.gridwidth = 1; //GridBagConstraints.RELATIVE;
        optionsGrid.setConstraints(resultsTypeLabel,optionsConstraints);
        optionsPanel.add(resultsTypeLabel);
        optionsConstraints.anchor = GridBagConstraints.WEST;
        optionsConstraints.weightx = 0.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.gridwidth = 1;//GridBagConstraints.REMAINDER;
        optionsGrid.setConstraints(resultsTypeChoice,optionsConstraints);
        optionsPanel.add(resultsTypeChoice);       
        
        optionsConstraints.anchor = GridBagConstraints.WEST;
        optionsConstraints.weightx = 1.0;
        optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JPanel filler3 = new JPanel();
        optionsGrid.setConstraints(filler3,optionsConstraints);
        optionsPanel.add(filler3);
        
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,20));

        
        //locationPanel.add(locationLabel);
        //locationPanel.add(locationChoice);
        //resultsTypePanel.add(resultsTypeLabel);
        //resultsTypePanel.add(resultsTypeChoice);
        
        
        searchTermsTable = new JTable(new SearchTermsTableModel());
        adjustColumnModel();
        searchTermsTable.setDefaultRenderer(java.lang.Object.class,new SearchTermsTableRenderer());
        searchTermsTable.setDefaultEditor(java.lang.Object.class,new SearchTermsTableEditor());
        ((DefaultCellEditor)searchTermsTable.getDefaultEditor(java.lang.Object.class)).setClickCountToStart(1);
        searchTermsTable.setRowHeight(ROW_HEIGHT);
        searchTermsTable.getTableHeader().setReorderingAllowed(false);
        searchTermsTable.setGridColor(new java.awt.Color(getBackground().getRed(),getBackground().getBlue(),getBackground().getGreen(),0));
        searchTermsTable.setBackground(getBackground());
        searchTermsTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter(){
           public void mousePressed(MouseEvent e)
           {
               if(e.getX()>searchTermsTable.getWidth()-BUTTON_COL_WIDTH)
               {
                 VueMetadataElement newElement = new VueMetadataElement();
                 String statementObject[] = {"Tag","",edu.tufts.vue.rdf.Query.Qualifier.STARTS_WITH.toString()};
                 newElement.setObject(statementObject);
                 newElement.setType(VueMetadataElement.SEARCH_STATEMENT);
                 searchTerms.add(newElement);
                 ((SearchTermsTableModel)searchTermsTable.getModel()).refresh();
               }
           }
        });
        JScrollPane scroll = new JScrollPane(searchTermsTable)
        {
            public java.awt.Dimension getPreferredSize()
            {
                return new java.awt.Dimension(super.getWidth(),90);
            }
        };
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(getBackground());
        fieldsPanel.add(scroll);
        
        topPanel.add(optionsPanel,BorderLayout.NORTH);
        //add(locationPanel);
        //add(resultsTypePanel);
        //add(lineLabel);
        fieldsPanel.add(linePanel,BorderLayout.NORTH);
        topPanel.add(fieldsPanel);
        
        JPanel buttonPanel = new JPanel(new BorderLayout());
        termsAction = new SearchAction(searchTerms);
        //SearchAction.revertGlobalSearchSelection();
        //termsAction.setResultsType(resultsTypeChoice.getSelectedItem().toString());
        
        searchButton = new JButton(termsAction);
        //searchButton.setBackground(java.awt.Color.WHITE);
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(getBackground());
        JButton resetButton = new JButton("Reset Map");
        resetButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               termsAction.revertSelections();
               allSearch.revertSelections();
               VUE.getActiveViewer().repaint();
           }
        });
        JPanel searchPanel = new JPanel();
        searchPanel.add(resetButton);
        searchPanel.add(searchButton);
        searchPanel.setOpaque(true);
        searchPanel.setBackground(getBackground());
        //buttonPanel.add(resetButton);
        buttonPanel.add(BorderLayout.EAST,searchPanel);
        //add(BorderLayout.NORTH,searchField);
        add(topPanel);
        add(buttonPanel,BorderLayout.SOUTH);
        
        searchTermsTable.addMouseListener(new java.awt.event.MouseAdapter()
               {
                   public void mouseReleased(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>searchTermsTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         //java.util.List<VueMetadataElement> searchTermsList = MetadataSearchGUI.this.searchTerms;
                         int selectedRow = searchTermsTable.getSelectedRow();
                         if(searchTermsTable.getSelectedColumn()==buttonColumn && searchTerms.size() > selectedRow)
                         {
                            searchTerms.remove(selectedRow);
                            searchTermsTable.repaint();
                            requestFocusInWindow();
                         }
                       }
                   }
        });
        
    }
    
    public void toggleOptionsView()
    {
        if(optionsToggle == SHOW_OPTIONS)
        {
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
           optionsLabel.setText("hide options");
        }
        else if(optionsToggle == HIDE_OPTIONS)
        {
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
           optionsLabel.setText("show options"); 
        }
    }

    public void toggleCategorySearch()
    {
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        if(model.getColumnCount() == 2)
        {
           buttonColumn = 2;
           valueColumn = 1;
           categoryColumn = 0;
           conditionColumn = -1;
           model.setColumns(3);
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
           optionsLabel.setText("hide options");
        }
        else
        {
           buttonColumn = 1;
           valueColumn = 0;
           categoryColumn = -1;
           conditionColumn = -1;
           model.setColumns(2);
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
           optionsLabel.setText("show options");
        }
               
        adjustColumnModel(); 
    }
    
    public void setCategorySearch()
    {
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 2;
        valueColumn = 1;
        categoryColumn = 0;
        conditionColumn = -1;
        model.setColumns(3);
        adjustColumnModel();
        //termsAction = new SearchAction(searchTerms);
        searchButton.setAction(termsAction);
    }
    
    public void setBasicSearch()
    {
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(2);
        adjustColumnModel();
        //termsAction = new SearchAction(searchTerms);
        searchButton.setAction(termsAction);
    }
    
    public void setConditionSearch()
    {
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 3;
        valueColumn = 2;
        categoryColumn = 0;
        conditionColumn = 1;
        model.setColumns(4);
        adjustColumnModel();
        //termsAction = new SearchAction(searchTerms);
        searchButton.setAction(termsAction);
    }
    
    public void setAllSearch()
    {
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(2);
        adjustColumnModel();
        
        //allSearchField.setText("FF");

        //allSearch.setResultsType(resultsTypeChoice.getSelectedItem().toString());
        //allSearch = new SearchAction(allSearchField);
        searchButton.setAction(allSearch);

        singleLine = true;
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
        else if(searchTermsTable.getModel().getColumnCount() == 3)
        {
          searchTermsTable.getColumnModel().getColumn(0).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(1).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(2).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(0).setMaxWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          searchTermsTable.getColumnModel().getColumn(1).setMaxWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          searchTermsTable.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH); 
        }
        else
        {
          searchTermsTable.getColumnModel().getColumn(0).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(1).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(2).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(3).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(0).setMaxWidth(editorWidth/3-BUTTON_COL_WIDTH/3);
          searchTermsTable.getColumnModel().getColumn(1).setMaxWidth(editorWidth/3-BUTTON_COL_WIDTH/3);
          searchTermsTable.getColumnModel().getColumn(2).setMaxWidth(editorWidth/3-BUTTON_COL_WIDTH/3);
          searchTermsTable.getColumnModel().getColumn(3).setMaxWidth(BUTTON_COL_WIDTH); 
        }
    }
    
    public void findCategory(Object currValue,int row,int col,JComboBox categories)
    {
   
        //System.out.println("MetadataSearchGUI: find category");
        
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
              //System.out.println("MetadataSearchGUI: find category - found - " + i);
              categories.setSelectedIndex(i);
           }
                   
        }
               
    }

    
    public java.awt.Component createRendererComponent(Object value,final int row,int col)
    {   
           JPanel comp = new JPanel();
           
           comp.setLayout(new java.awt.BorderLayout()); 
           
           if(col == (valueColumn))
           {
             final JTextField field = new JTextField();
             field.addFocusListener(new FocusAdapter(){
                public void focusLost(FocusEvent fe)
                {
                    VueMetadataElement searchTerm = searchTerms.get(row);
                    //Object searchTerm = searchTerms.get(row).getObject();
                    if(searchTerm.getObject() instanceof String[])
                    {
                        //VueMetadataElement newElement = new VueMetadataElement();
                        //System.out.println("MetadataSearchGUI creating newTerm in focus lost - category: " + ((String[])searchTerms.get(row).getObject())[0] );
                        //System.out.println("MetadataSearchGUI creating newTerm in focus lost - value: " + field.getText() );
                        String[] newStatement = {((String[])searchTerms.get(row).getObject())[0],field.getText(),((String[])searchTerms.get(row).getObject())[2]};
                        searchTerm.setObject(newStatement);
                        //newElement.setObject(newTerm);
                        //System.out.println("MetadataSearchGUI setObject to newTerm now look at key: " + newElement.getKey());
                        //newElement.setType(VueMetadataElement.CATEGORY);
                    }
                }
             });
             //System.out.println("MetadatasearchGUI renderer component: about to set field value -  " + value.toString() +"," + ((String[])searchTerms.get(row).getObject())[1]);
             //field.setText(value.toString());
             
             String val = ((String[])searchTerms.get(row).getObject())[1];
             field.setText(val);
             comp.add(field);
           }
           else
           if(col == (categoryColumn))
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
                        String[] statement = {((OntType)categories.getSelectedItem()).getLabel(),searchTerms.get(row).getValue(),((String[])(searchTerms.get(row).getObject()))[2]};
                        VueMetadataElement ele = new VueMetadataElement();
                        ele.setObject(statement);
                        ele.setType(VueMetadataElement.SEARCH_STATEMENT);
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
           if(col == conditionColumn)
           {
               String [] conditions = {"STARTS_WITH","CONTAINS"};
               final JComboBox conditionCombo = new JComboBox(conditions);
               conditionCombo.addItemListener(new ItemListener()
               {
                  public void itemStateChanged(ItemEvent ie)
                  {
                      if(ie.getStateChange() == ItemEvent.SELECTED)
                      {
                          String[] statement = {((String[])(searchTerms.get(row).getObject()))[0],searchTerms.get(row).getValue(),conditionCombo.getSelectedItem().toString()};
                          VueMetadataElement ele = new VueMetadataElement();
                          ele.setObject(statement);
                          ele.setType(VueMetadataElement.SEARCH_STATEMENT);
                          searchTerms.set(row,ele);
                      }
                  }
               });
               String currentCondition = ((String[])(searchTerms.get(row).getObject()))[2];
               if(currentCondition.equals("CONTAINS"))
                   conditionCombo.setSelectedIndex(1);
               comp.add(conditionCombo);
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
            if(singleLine == true && (col == (valueColumn)))
            {
              comp.setText("Keywords:");
            }
            if(singleLine == true && (col == (buttonColumn)))
            {
              comp.setText("");
            }
            else
            if(col == buttonColumn && singleLine == false)
              comp.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
            else if(table.getModel().getColumnCount() == 2 || col == categoryColumn)
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
            if(singleLine)
               if(col == 0)
               {
                  JPanel comp = new JPanel(new BorderLayout());
                  comp.add(allSearchField);
                  comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
                  return comp;
                }
                else
                  return new JLabel();
            else
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
            if(singleLine)
                if(col == 0)
                {
                  JPanel comp = new JPanel(new BorderLayout());
                  comp.add(allSearchField);
                  comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
                  return comp;
                }
                else
                  return new JLabel();
            else
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
            if(singleLine)
                return 1;
            if(searchTerms.size() > 0)
              return searchTerms.size();
            else
              return 1;
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
            if( (col == valueColumn) || (col == categoryColumn) || (col == conditionColumn) )
                return true;
            else
                return false;
        }
        
        public Object getValueAt(int row,int col)
        {
            if(row == 0 && searchTerms.size() == 0)
            {
                VueMetadataElement vme = new VueMetadataElement();
                //String pairedValue[] = {"Tag","STARTS_WITH",""};
                //vme.setObject(pairedValue);
                
                String statementObject[] = {"Tag","",edu.tufts.vue.rdf.Query.Qualifier.STARTS_WITH.toString()};
                vme.setObject(statementObject);
                vme.setType(VueMetadataElement.SEARCH_STATEMENT);
                
                searchTerms.add(vme);
            }
            if(col == buttonColumn)
              return "delete button";
            else
            if(col == valueColumn)
              return searchTerms.get(row).getValue();
            else
            if(col == conditionColumn)
            {
              String[] statement = (String[])searchTerms.get(row).getObject();
              if(statement.length > 2)
                return ((String[])(searchTerms.get(row)).getObject())[2]; 
              else
                return "";
            }
            else
              return searchTerms.get(row).getKey();
        }
        
        public void refresh()
        {
            fireTableDataChanged();
        }
        
    }
        
}
