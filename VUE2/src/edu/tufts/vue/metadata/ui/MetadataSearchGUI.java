
/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
    
    private static final boolean DEBUG_LOCAL = false;
    
    // "false' is inner scroll pane just around search terms table
    private static final boolean SIDE_SCROLLBAR = false;
    
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
    
    // search types
    public final static int EVERYTHING = 0;
    public final static int LABEL = 1;
    public final static int KEYWORD = 2;
    
    public final static String AND = "and";
    public final static String OR = "or";
    
    public final static String SELECTED_MAP_STRING = "Current Map";
    public final static String ALL_MAPS_STRING = "All Open Maps";
    
    public final static String SEARCH_EVERYTHING = "Search everything";
    public final static String SEARCH_LABELS_ONLY = "Labels";
    public final static String SEARCH_ALL_KEYWORDS = "Keywords";
    public final static String SEARCH_CATEGORIES_AND_KEYWORDS = "Categories + Keywords";
    
    // combo box numbers within optionsPanel
    public final static int TYPES = 0;
    public final static int LOCATIONS = 1;
    public final static int RESULTS = 2;
    
    //ONE_LINE
    private JTextField searchField;
    private JTable searchTable;
    private JButton searchButton;
    private List<URI> found = null;
    private List<List<URI>> finds = null;
    
    //TEXT FIELD BASED
    private JPanel topPanel;
    private JPanel innerTopPanel;
    private OptionsPanel optionsPanel;
    //private String[] searchTypes = {"Basic","Categories","Advanced","All"};
    //private String[] searchTypes = {"Basic","Categories","Advanced"};
    //private String[] searchTypes = {"Basic","Categories"};
    
    private String[] searchTypes = {SEARCH_EVERYTHING,SEARCH_LABELS_ONLY,SEARCH_ALL_KEYWORDS,SEARCH_CATEGORIES_AND_KEYWORDS};
    //private String[] searchTypes = {SEARCH_LABELS_ONLY,SEARCH_ALL_KEYWORDS,SEARCH_CATEGORIES_AND_KEYWORDS};
            
    private String[] locationTypes = {SELECTED_MAP_STRING,ALL_MAPS_STRING};
    
    private String[] currentMapResultsTypes = {"Show","Hide","Select","Copy to new map"};
   // private String[] allOpenMapsResultsTypes = {"new map(tile)","new map(overlay)"};
    private String[] allOpenMapsResultsTypes = {"new map"};
   
    private JPanel fieldsPanel;
    private JTable searchTermsTable;
    
    private JPanel buttonPanel;

    private int buttonColumn = 1;
    private int categoryColumn = -1;
    private int valueColumn = 0;
    private int conditionColumn = -1;
    
    private List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
    
    private JButton advancedSearch;
    private JLabel optionsLabel;
    
    private int optionsToggle = 1;
    
    private SearchAction termsAction;
    private boolean singleLine = false;
    private JTextField allSearchField = new JTextField();
    private SearchAction allSearch = new SearchAction(allSearchField);
    
    private boolean treatNoneSpecially = false;
    
    private static tufts.vue.gui.DockWindow dockWindow;
    
    private static JComponent content;
    
    private int searchType = LABEL;
    
    private static tufts.vue.gui.WidgetStack stack;
    private static boolean initialized = false;
    
    private ButtonGroup andOrButtonGroup;
    private JPanel radioButtonPanel;
    private JPanel fieldsInnerPanel;
    
    private JPanel linePanel;
    
    private static MetadataSearchGUI basic;
    private static MetadataSearchGUI advanced;
    
    public static tufts.vue.gui.DockWindow getDockWindow()
    {
        if(dockWindow == null)
        {
            dockWindow = tufts.vue.gui.GUI.createDockWindow("Search");
            
            dockWindow.setLocation(350,300);
            
            basic = new MetadataSearchGUI(MULTIPLE_FIELDS);
            advanced = new MetadataSearchGUI(MULTIPLE_FIELDS);
            
            content = new JTabbedPane();
            
            ((JTabbedPane)content).addChangeListener(new javax.swing.event.ChangeListener(){
               public void stateChanged(javax.swing.event.ChangeEvent c)
               {
                   if(DEBUG_LOCAL)
                   {
                       System.out.println("MetadataSearchGUI tab change " + c);
                   }
                   
                   dockWindow.setSize(new java.awt.Dimension((int)dockWindow.getSize().getWidth() + 10,(int)dockWindow.getSize().getHeight() + 10));
                   dockWindow.validate();
                   basic.adjustColumnModel();
                   advanced.adjustColumnModel();
                   basic.validate();
                   advanced.validate();
                   content.validate();
                   //dockWindow.setSize(new java.awt.Dimension(305,255));
                   dockWindow.setSize(new java.awt.Dimension((int)dockWindow.getSize().getWidth() - 10,(int)dockWindow.getSize().getHeight() - 10));
                   dockWindow.repaint();
               }
            });
            
            if(SIDE_SCROLLBAR)
            {    
              stack = new tufts.vue.gui.WidgetStack();
            
              stack.setLayout(new BorderLayout());
              stack.setWantsScroller(true);
            
              stack.add(content);
            }
            
        }    
        
        return dockWindow;
    }
    
    public static void afterDockVisible()
    {   
        
            if(initialized)
                return;
            
            if(SIDE_SCROLLBAR)
            {
              dockWindow.setContent(stack);
            }
            else
            {
              dockWindow.setContent(content); 
            }
     
            //dockWindow.setSize(305,255); 
            dockWindow.setSize(315,360);
            
            advanced.toggleOptionsView();
            
            ((JTabbedPane)content).addTab("Basic",basic);
            ((JTabbedPane)content).addTab("Advanced",advanced);
            
            initialized = true;
    }
    
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
           //setLabelSearch();
           setEverythingSearch();
        }
        
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
    }
    
    public java.awt.Dimension getPreferredSize()
    {
        return new java.awt.Dimension(314,359);
    }
    
    public void setUpOneLineSearch()
    {
        setLayout(new BorderLayout());
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(9,9,9,9),
                              BorderFactory.createLineBorder(new java.awt.Color(200,200,200),1)));
        JPanel buttonPanel = new JPanel(new BorderLayout());
        searchButton = new JButton(new SearchAction(searchField));
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(BorderLayout.EAST,searchButton);
        add(BorderLayout.NORTH,searchField);
        add(buttonPanel);
    }
    
    public void setUpFieldsSearch()
    {
        
        setLayout(new BorderLayout());
        
        topPanel = new JPanel(new BorderLayout());
        innerTopPanel = new JPanel(new BorderLayout());
        
        optionsPanel = new OptionsPanel();
        
        linePanel = new JPanel() {
                protected void paintComponent(java.awt.Graphics g) {
                    g.setColor(java.awt.Color.DARK_GRAY);
                    g.drawLine(5,getHeight()/2, MetadataSearchGUI.this.getWidth()-15, getHeight()/2);
                }
                
                public java.awt.Dimension getMinimumSize()
                {
                        return new java.awt.Dimension(MetadataSearchGUI.this.getWidth(),30);
                }
        };
        
        fieldsPanel = new JPanel(new java.awt.BorderLayout());

        ItemListener searchTypesListener = new ItemListener()
        {
           public void itemStateChanged(ItemEvent ie)
           {
               if(ie.getStateChange() == ItemEvent.SELECTED)
               {
                   // currently not in use -- see menu items above
                   /*if(ie.getItem().equals("Basic"))
                   {
                       //System.out.println("Basic search selected");
                       setBasicSearch();
                   }
                   if(ie.getItem().equals("Categories"))
                   {
                       //System.out.println("Category search selected");
                       setCategorySearchWithNoneCase();
                       //setCategorySearch();
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
                   }*/
                   // end currently not in use
                   
                   if(ie.getItem().equals(SEARCH_EVERYTHING))
                   {
                       setEverythingSearch();
                   }
                   if(ie.getItem().equals(SEARCH_LABELS_ONLY))
                   {
                       //setBasicSearch();
                       setLabelSearch();
                   }
                   if(ie.getItem().equals(SEARCH_ALL_KEYWORDS))
                   {
                       setAllMetadataSearch();
                   }
                   if(ie.getItem().equals(SEARCH_CATEGORIES_AND_KEYWORDS))
                   {
                       setCategorySearch();
                   }
                   
               }
           }
        };
                
        ItemListener locationChoiceListener = new ItemListener()
        {
           public void itemStateChanged(ItemEvent e)
           {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
                 String type = e.getItem().toString(); 
                 
                 if(type.equals(ALL_MAPS_STRING))
                 {    
                   allSearch.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
                   termsAction.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
                   optionsPanel.switchChoices(RESULTS,allOpenMapsResultsTypes);
                 }
                 else // SELECTED_MAP_STRING as current default
                 {
                   allSearch.setLocationType(SearchAction.SEARCH_SELECTED_MAP); 
                   termsAction.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
                   optionsPanel.switchChoices(RESULTS,currentMapResultsTypes);
                 }    
               }
           }
        };
        
        ItemListener resultsTypeListener = new ItemListener()
        {
           public void itemStateChanged(ItemEvent e)
           {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
                 String resultsTypeChoice = e.getItem().toString();
                 /*if(resultsTypeChoice != null && allSearch != null && termsAction != null)
                 {
                   allSearch.setResultsType(resultsTypeChoice);
                   termsAction.setResultsType(resultsTypeChoice);
                 }*/
                 setResultsTypeInActions(resultsTypeChoice);
               }
           }
        };
        
        JPanel advancedSearchPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        optionsLabel = new JLabel("show options");
        advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
        advancedSearch.setBorder(BorderFactory.createEmptyBorder());
        advancedSearch.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent e)
           {   
               toggleOptionsView();
           }
        });
        advancedSearchPanel.add(optionsLabel);
        advancedSearchPanel.add(advancedSearch);
        
        //disabling this button VUE-869
        //innerTopPanel.add(advancedSearchPanel,BorderLayout.NORTH);
        
        //for default of advanced...
        //innerTopPanel.add(optionsPanel);
        
        //optionsPanel.add(advancedSearch);
        //optionsPanel.add(optionsLabel);
       

        optionsPanel.addLabel("Search Type:");
        optionsPanel.addCombo(searchTypes,searchTypesListener);
        
        optionsPanel.addLabel("Maps:");
        optionsPanel.addCombo(locationTypes,locationChoiceListener);
        
        optionsPanel.addLabel("Results:");
        optionsPanel.addCombo(currentMapResultsTypes,resultsTypeListener);
        
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
                 String statementObject[] = {VueResources.getString("metadata.vue.url") + "#none","",edu.tufts.vue.rdf.Query.Qualifier.STARTS_WITH.toString()};
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
        JPanel corner = new JPanel();
        corner.setOpaque(true);
        corner.setBackground(getBackground());
        //corner.setPreferredSize(100,100);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER,corner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(getBackground());

        //if(!SIDE_SCROLLBAR)
        //{    
        //  fieldsPanel.add(scroll);
        //}
        //else
        //{
          fieldsInnerPanel = new JPanel();
          /*{
            public java.awt.Dimension getPreferredSize()
            {
                return new java.awt.Dimension(super.getWidth(),90);
            }
          };*/
          fieldsInnerPanel.setLayout(new BorderLayout());
          
          radioButtonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
          andOrButtonGroup = new ButtonGroup();
          JRadioButton andButton = new JRadioButton(AND);
          andButton.setActionCommand(AND);
          JRadioButton orButton = new JRadioButton(OR);
          orButton.setActionCommand(OR);
          andOrButtonGroup.add(andButton);
          andOrButtonGroup.add(orButton);
          andOrButtonGroup.setSelected(andButton.getModel(),true);
          radioButtonPanel.add(andButton);
          radioButtonPanel.add(orButton);
          
          andButton.addActionListener(new java.awt.event.ActionListener(){
             public void actionPerformed(java.awt.event.ActionEvent e)
             {
                 termsAction.setOperator(SearchAction.AND);
             }
          });
          
          orButton.addActionListener(new java.awt.event.ActionListener(){
             public void actionPerformed(java.awt.event.ActionEvent e)
             {
                 termsAction.setOperator(SearchAction.OR);
             }
          });
          
          JPanel tablePanel = new JPanel();
          tablePanel.setLayout(new BorderLayout());
          tablePanel.add(searchTermsTable.getTableHeader(),BorderLayout.NORTH);
          
          if(SIDE_SCROLLBAR)
          {    
            tablePanel.add(searchTermsTable);  
          }
          else
          {
            tablePanel.add(scroll);
          }
          
          fieldsInnerPanel.add(tablePanel);
          // do this in toggleOptionsView
          //fieldsInnerPanel.add(radioButtonPanel,BorderLayout.SOUTH);
          
          fieldsPanel.add(fieldsInnerPanel);
        //}
        
        topPanel.add(innerTopPanel,BorderLayout.NORTH);
        //fieldsPanel.add(linePanel,BorderLayout.NORTH);
        topPanel.add(fieldsPanel);
        
        buttonPanel = new JPanel(new BorderLayout());
        termsAction = new SearchAction(searchTerms);
        //SearchAction.revertGlobalSearchSelection();
        //termsAction.setResultsType(resultsTypeChoice.getSelectedItem().toString());
        
        searchButton = new JButton(termsAction);
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(getBackground());
        JButton resetButton = new JButton(VueResources.getString("search.popup.reset"));
        resetButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               
               SearchAction.revertGlobalSearchSelectionFromMSGUI();
               
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
        
        /*
        add(topPanel);
        add(buttonPanel,BorderLayout.SOUTH);
        */
        
        setUpLayout();

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
        
        setResultsTypeInActions("Select");
        
    }
    
    public void setResultsTypeInActions(String resultsTypeChoice)
    {
        // String resultsTypeChoice = e.getItem().toString();
         if(resultsTypeChoice != null && allSearch != null && termsAction != null)
         {
                   allSearch.setResultsType(resultsTypeChoice);
                   termsAction.setResultsType(resultsTypeChoice);
         }
    }
    
    public void setUpLayout()
    {
        add(topPanel);
        add(buttonPanel,BorderLayout.SOUTH);        
    }
    
    public void repaint()
    {
        super.repaint();
        adjustColumnModel();
    }
    
    public void toggleOptionsView()
    {
        if(optionsToggle == SHOW_OPTIONS)
        {
            
           //dockWindow.setSize(300,250 + optionsPanel.getHeight()); 
           
           dockWindow.validate();
           fieldsPanel.add(linePanel,BorderLayout.NORTH);
           fieldsInnerPanel.add(radioButtonPanel,BorderLayout.SOUTH);
           innerTopPanel.add(optionsPanel);
           remove(topPanel);
           remove(buttonPanel);
           
           setUpLayout();
            
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
           optionsLabel.setText(VueResources.getString("advancedSearchLess.tooltip"));
           optionsLabel.setFont(tufts.vue.gui.GUI.LabelFace);
           
           //re-enable next two lines if ever go back to non tabbed mode
           //dockWindow.setSize(dockWindow.getWidth(),dockWindow.getHeight() + 95);
           //dockWindow.validate();
           
           optionsToggle = HIDE_OPTIONS;
        }
        else if(optionsToggle == HIDE_OPTIONS)
        {
           fieldsPanel.remove(linePanel);
           fieldsInnerPanel.remove(radioButtonPanel); 
           innerTopPanel.remove(optionsPanel);
           remove(topPanel);
           remove(buttonPanel);
           
           setUpLayout();
            
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
           optionsLabel.setText(VueResources.getString("advancedSearchMore.tooltip"));
           
           
           //re-enable next two lines if ever go back to non tabbed mode
           //dockWindow.setSize(dockWindow.getWidth(),dockWindow.getHeight() - 50);
           //dockWindow.validate();
           
           optionsToggle = SHOW_OPTIONS;
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
           optionsLabel.setText(VueResources.getString("advancedSearchLess.tooltip"));
        }
        else
        {
           buttonColumn = 1;
           valueColumn = 0;
           categoryColumn = -1;
           conditionColumn = -1;
           model.setColumns(2);
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
           optionsLabel.setText(VueResources.getString("advancedSearchMore.tooltip"));
        }
               
        adjustColumnModel(); 
    }
    
    public int getSelectedOperator()
    {
        
        if(andOrButtonGroup == null || andOrButtonGroup.getSelection() == null || andOrButtonGroup.getSelection().getActionCommand() == null)
        {
            return SearchAction.AND;
        }
        
        if(DEBUG_LOCAL)
        {    
          System.out.println("MetadataSearchGUI action command of selected operator will be: " + andOrButtonGroup.getSelection().getActionCommand());
        }
        
        String choice = andOrButtonGroup.getSelection().getActionCommand();
        if(choice.equals(OR))
        {
           return SearchAction.OR; 
        }
        else
        {
           return SearchAction.AND;  
        }
    }
 
    public void setCategorySearchWithNoneCase()
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
        
        treatNoneSpecially = true;
        termsAction.setNoneIsSpecial(true);
        
        termsAction.setTextOnly(false);
        termsAction.setBasic(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
        searchButton.setAction(termsAction);
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
        
        termsAction.setBasic(false);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
        searchButton.setAction(termsAction);
    }
    
    public void setEverythingSearch()
    {
        searchType = EVERYTHING;
        
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(2);
        adjustColumnModel();
        
        //termsAction = new SearchAction(searchTerms);
        
        termsAction.setBasic(false);
        termsAction.setTextOnly(true);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(true);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
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
        termsAction.setBasic(true);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
        searchButton.setAction(termsAction);
    }
    
    public void setLabelSearch()
    {
        searchType = LABEL;
        
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(2);
        adjustColumnModel();
        //termsAction = new SearchAction(searchTerms);
        termsAction.setBasic(true);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
        searchButton.setAction(termsAction);
    }
    
    public void setAllMetadataSearch()
    {
        searchType = KEYWORD;
        
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTable.getModel();
        buttonColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(2);
        adjustColumnModel();
        //termsAction = new SearchAction(searchTerms);
       
        termsAction.setBasic(false);
        termsAction.setTextOnly(true);
        termsAction.setMetadataOnly(true);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
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
        termsAction.setBasic(false);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        //termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
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
        
        if(searchTermsTable == null)
            return;
        
        int editorWidth = this.getWidth();
        if(searchTermsTable.getModel().getColumnCount() == 2)
        {
          searchTermsTable.getColumnModel().getColumn(0).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(1).setHeaderRenderer(new SearchTermsTableHeaderRenderer());
          searchTermsTable.getColumnModel().getColumn(0).setMaxWidth(editorWidth-BUTTON_COL_WIDTH);
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
        
        if(!(currValue instanceof String))
        {
            if(DEBUG_LOCAL)
            {
                System.out.println("MetadataSearchGUI - findCategory - currValue not instance of String -- returning ");
            }
            return;
        }    
        
        int n = categories.getModel().getSize();
        for(int i=0;i<n;i++)
        {
                   
           Object item = categories.getModel().getElementAt(i);
           
           if(DEBUG_LOCAL)
           {
             System.out.println("i: " + i);  
             if(item instanceof OntType)
             {
               System.out.println("MetadataSearchGUI - find category - item.getBase() and currValue  - " +"i :" + i + ":" + ((OntType)item).getBase() +"," + currValue);
               System.out.println("MetadataSearchGUI - ((OntType)item).getBase() + # + ((OntType)item).getLabel() " + 
                       ((OntType)item).getBase() + "#" + ((OntType)item).getLabel());
             }
           } 
             
           if(item instanceof OntType && ( ((OntType)item).getBase() + "#" + ((OntType)item).getLabel() ).equals(currValue))
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
             field.setFont(tufts.vue.gui.GUI.LabelFace);
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
             //#VUE-887 -- whoops -- metadataeditor not here
             comp.add(field);
             
             //JLabel label = new JLabel(val);
             //add(label);
             
           }
           else
           if(col == (categoryColumn))
           {           
             final JComboBox categories = new JComboBox();
             categories.setFont(tufts.vue.gui.GUI.LabelFace);
             categories.setModel(new CategoryComboBoxModel());
             categories.setRenderer(new CategoryComboBoxRenderer());
             
             categories.addItemListener(new ItemListener(){
                public void itemStateChanged(ItemEvent ie)
                {
                    if(ie.getStateChange() == ItemEvent.SELECTED)
                    {
                        
                        if(!(categories.getSelectedItem() instanceof OntType))
                            return;
   
                        OntType type = (OntType)categories.getSelectedItem();
                        String[] statement = {type.getBase() + "#" + type.getLabel(),searchTerms.get(row).getValue(),((String[])(searchTerms.get(row).getObject()))[2]};
                        
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
               findCategory(searchTerms.get(row).getKey(),row,col,categories);
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
               //String [] conditions = {"starts with","contains"};
               String [] conditions = {VueResources.getString("combobox.conditioncolumn.contains"),VueResources.getString("combobox.conditioncolumn.startswith")};
               final JComboBox conditionCombo = new JComboBox(conditions);
               conditionCombo.setFont(tufts.vue.gui.GUI.LabelFace);
               conditionCombo.addItemListener(new ItemListener()
               {
                  public void itemStateChanged(ItemEvent ie)
                  {
                      if(ie.getStateChange() == ItemEvent.SELECTED)
                      {
                          String condition = conditionCombo.getSelectedItem().toString();
                          if(condition.equals(VueResources.getString("combobox.conditioncolumn.startswith")))
                          {
                              condition = "STARTS_WITH";
                          }    
                          else
                          {
                              condition = "CONTAINS";
                          }
                          
                          String[] statement = {((String[])(searchTerms.get(row).getObject()))[0],searchTerms.get(row).getValue(),condition};
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
    
    class OptionsPanel extends JPanel
    {
       GridBagLayout optionsGrid;
       GridBagConstraints optionsConstraints;

       //in case easy access to the labels is ever needed:
       //they can also be found in the layout
       //List<JLabel> labels = new ArrayList<JLabel>();
       
       //in case easy access to the combo boxes is ever needed:
       //they can also be found in the layout
       // ** now needed for switching choices in the results combo
       // on location switch
       List<JComboBox> comboBoxes = new ArrayList<JComboBox>();
       
       OptionsPanel()
       {
           optionsGrid = new GridBagLayout();
           optionsConstraints = new GridBagConstraints();
           setLayout(optionsGrid);
           setBorder(BorderFactory.createEmptyBorder(5,5,5,20));
       }

       /**
        *
        * adds the label to the layout and sets its Font
        * and alignment
        * 
        * Precondition: ready for new line in layout
        *   (beginning of layout or combo just added)
        * Postcondition: ready to add a new combo box
        *
        **/
       void addLabel(String name)
       {
          JLabel label = new JLabel(name,JLabel.RIGHT);
          label.setFont(tufts.vue.gui.GUI.LabelFace);
          
          optionsConstraints.anchor = GridBagConstraints.EAST;
          optionsConstraints.weightx = 0.0;
          optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
          optionsConstraints.gridwidth = 1;
          optionsGrid.setConstraints(label,optionsConstraints);
          optionsPanel.add(label);
          
          //in case easy access to the label is needed:
          //it can also be found in the layout
          //labels.add(label); 
       }
       
       /**
        *
        * adds the combo box to the layout and a filler
        * component to fill out the label/combo line
        * 
        * Precondition: label has just been added
        * Postcondition: ready to add a new label
        *
        **/
       void addCombo(String[] choices,ItemListener listener)
       {
           JComboBox newCombo = new JComboBox(choices)
           {
             public java.awt.Dimension getMinimumSize()
             {
                return new java.awt.Dimension(125,super.getHeight());
             }
           };
           newCombo.setFont(tufts.vue.gui.GUI.LabelFace);
           
           newCombo.addItemListener(listener);
           
           optionsConstraints.anchor = GridBagConstraints.WEST;
           optionsConstraints.weightx = 0.0;
           optionsConstraints.insets = new java.awt.Insets(0,5,5,0);
           optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
           optionsConstraints.gridwidth = 1;
           optionsGrid.setConstraints(newCombo,optionsConstraints);
           optionsPanel.add(newCombo);
        
           optionsConstraints.anchor = GridBagConstraints.WEST;
           optionsConstraints.weightx = 1.0;
           optionsConstraints.insets = new java.awt.Insets(0,0,5,0);
           optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
           optionsConstraints.gridwidth = GridBagConstraints.REMAINDER;
           JPanel filler = new JPanel();
           optionsGrid.setConstraints(filler,optionsConstraints);
           optionsPanel.add(filler);
           
           //in case easy access to the combo ever is needed:
           //it can also be found in the layout
           comboBoxes.add(newCombo);            
           
           if(choices.length > 2 && choices[2].equals("Select"))
           {
               newCombo.setSelectedIndex(2);
           }
       }
       
       public void switchChoices(int i,String[] choices)
       {
           JComboBox box = comboBoxes.get(i);
           box.removeAllItems();
           for(int j=0;j<choices.length;j++)
           {
               box.addItem(choices[j]);
           }
           
           if(choices.length > 2 && i==RESULTS)
           {
               box.setSelectedIndex(2);
           }
           else if(choices.length > 1)
           {
               box.setSelectedItem(0);
           }
       }
    
    }
    
    class SearchTermsTableHeaderRenderer extends DefaultTableCellRenderer
    {
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
        {
            
            //System.out.println("getTableCellRendererComponent - col,getColumnCount() " + col + "," + table.getModel().getColumnCount());
            //System.out.println("category column,valuecolumn,conditioncolumn" + categoryColumn + "," + valueColumn + "," + conditionColumn);
            
            JLabel comp = new JLabel();
            comp.setFont(tufts.vue.gui.GUI.LabelFace);
            if(singleLine == true && (col == (valueColumn)))
            {
              comp.setText(VueResources.getString("advancedSearch.keywords"));
            }
            if(singleLine == true && (col == (buttonColumn)))
            {
              comp.setText("");
            }
            else
            if(col == buttonColumn && singleLine == false)
              comp.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
            else if( table.getModel().getColumnCount() == 2 && col == valueColumn)
            {    
              if(searchType == EVERYTHING)
              {
                comp.setText(VueResources.getString("advancedSearch.searcheverything"));                  
              }
              
              if(searchType == LABEL)
              {
                comp.setText(VueResources.getString("advancedSearch.label"));                  
              }
                
              if(searchType == KEYWORD)
              {
                comp.setText(VueResources.getString("advancedSearch.keywords")); 
              }

            }
            else if( (table.getModel().getColumnCount() == 3 || table.getModel().getColumnCount() == 4) && col == categoryColumn)
            {
              comp.setText(VueResources.getString("advancedSearch.category"));
              //comp.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            }
            else if( (table.getModel().getColumnCount() == 3  || table.getModel().getColumnCount() == 4 ) && col == valueColumn )
              comp.setText(VueResources.getString("advancedSearch.keywords"));
            else if(table.getModel().getColumnCount() == 4 && col == conditionColumn)
              comp.setText(VueResources.getString("advancedSearch.operator"));
            else
              comp.setText("");
            
            if(comp.getText().equals(VueResources.getString("advancedSearch.category"))|| comp.getText().equals(VueResources.getString("advancedSearch.operator")))
            {    
              comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,5+ROW_INSET,ROW_GAP,ROW_INSET-5));
            }
            else
            {    
              comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET-5));
            }
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
                
                String statementObject[] = {VueResources.getString("metadata.vue.url") +"#none","",edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString()};
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
