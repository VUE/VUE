
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
    
    private static final boolean DEBUG_LOCAL = false;
    
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
    private JPanel topPanel;
    private JPanel innerTopPanel;
    private OptionsPanel optionsPanel;
    //private String[] searchTypes = {"Basic","Categories","Advanced","All"};
    //private String[] searchTypes = {"Basic","Categories","Advanced"};
    private String[] searchTypes = {"Basic","Categories"};
    private String[] locationTypes = {SELECTED_MAP_STRING,ALL_MAPS_STRING};
    private String[] resultsTypes = {"Show","Hide","Select"};
    
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
    
    private static tufts.vue.gui.DockWindow dockWindow;
    
    private static MetadataSearchGUI content;
    
    public static tufts.vue.gui.DockWindow getDockWindow()
    {
        if(dockWindow == null)
        {
            dockWindow = tufts.vue.gui.GUI.createDockWindow("Search");
            dockWindow.setLocation(350,300);
            
            content = new MetadataSearchGUI(MULTIPLE_FIELDS);

            //dockWindow.pack();
        }    
        
        return dockWindow;
    }
    
    public static void afterDockVisible()
    {
            dockWindow.setContent(content);
            
            content.adjustColumnModel();

            dockWindow.setSize(300,200); 
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
           setBasicSearch();
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
        
        setLayout(new BorderLayout());
        
        topPanel = new JPanel(new BorderLayout());
        innerTopPanel = new JPanel(new BorderLayout());
        
        optionsPanel = new OptionsPanel();
        
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
        
        fieldsPanel = new JPanel(new java.awt.BorderLayout());

        ItemListener searchTypesListener = new ItemListener()
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
        };
                
        ItemListener locationChoiceListener = new ItemListener()
        {
           public void itemStateChanged(ItemEvent e)
           {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
                 String type = e.getItem().toString(); //locationChoice.getSelectedItem().toString();
                 
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
        };
        
        ItemListener resultsTypeListener = new ItemListener()
        {
           public void itemStateChanged(ItemEvent e)
           {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
                 String resultsTypeChoice = e.getItem().toString();
                 allSearch.setResultsType(resultsTypeChoice);
                 termsAction.setResultsType(resultsTypeChoice);
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
        innerTopPanel.add(advancedSearchPanel,BorderLayout.NORTH);
        
        //innerTopPanel.add(optionsPanel);
        
        //optionsPanel.add(advancedSearch);
        //optionsPanel.add(optionsLabel);
       

        optionsPanel.addLabel("Search Type:");
        optionsPanel.addCombo(searchTypes,searchTypesListener);
        
        optionsPanel.addLabel("Location:");
        optionsPanel.addCombo(locationTypes,locationChoiceListener);
        
        optionsPanel.addLabel("Results Type:");
        optionsPanel.addCombo(resultsTypes,resultsTypeListener);
        
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
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(getBackground());
        fieldsPanel.add(scroll);
        
        topPanel.add(innerTopPanel,BorderLayout.NORTH);
        fieldsPanel.add(linePanel,BorderLayout.NORTH);
        topPanel.add(fieldsPanel);
        //topPanel.add(innerTopPanel,BorderLayout.NORTH);
        
        buttonPanel = new JPanel(new BorderLayout());
        termsAction = new SearchAction(searchTerms);
        //SearchAction.revertGlobalSearchSelection();
        //termsAction.setResultsType(resultsTypeChoice.getSelectedItem().toString());
        
        searchButton = new JButton(termsAction);
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
            
           innerTopPanel.add(optionsPanel);
           remove(topPanel);
           remove(buttonPanel);
           
           setUpLayout();
            
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
           optionsLabel.setText("hide options");
           optionsLabel.setFont(tufts.vue.gui.GUI.LabelFace);
           
           validate();
           
           if(dockWindow != null)
             dockWindow.pack();
           
           optionsToggle = HIDE_OPTIONS;
        }
        else if(optionsToggle == HIDE_OPTIONS)
        {
           innerTopPanel.remove(optionsPanel);
           remove(topPanel);
           remove(buttonPanel);
           
           setUpLayout();
            
           advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
           optionsLabel.setText("show options");
           
           validate();
           
           if(dockWindow != null)
           {
              dockWindow.pack();
           }
           
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
        termsAction.setBasic(false);
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
             comp.add(field);
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
               String [] conditions = {"starts with","contains"};
               final JComboBox conditionCombo = new JComboBox(conditions);
               conditionCombo.setFont(tufts.vue.gui.GUI.LabelFace);
               conditionCombo.addItemListener(new ItemListener()
               {
                  public void itemStateChanged(ItemEvent ie)
                  {
                      if(ie.getStateChange() == ItemEvent.SELECTED)
                      {
                          String condition = conditionCombo.getSelectedItem().toString();
                          if(condition.equals("starts with"))
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
       //List<JComboBox> comboBoxes = new ArrayList<JComboBox>();
       
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
           //comboBoxes.add(newCombo);            
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
              comp.setText("Keywords:");
            }
            if(singleLine == true && (col == (buttonColumn)))
            {
              comp.setText("");
            }
            else
            if(col == buttonColumn && singleLine == false)
              comp.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
            else if( table.getModel().getColumnCount() == 2 && col == valueColumn)
              comp.setText("Keyword:");
            else if( (table.getModel().getColumnCount() == 3 || table.getModel().getColumnCount() == 4) && col == categoryColumn)
            {
              comp.setText("Category:");
              //comp.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            }
            else if( (table.getModel().getColumnCount() == 3  || table.getModel().getColumnCount() == 4 ) && col == valueColumn )
              comp.setText("Keyword:");
            else if(table.getModel().getColumnCount() == 4 && col == conditionColumn)
              comp.setText("Operator");
            else
              comp.setText("");
            
            if(comp.getText().equals("Category:") || comp.getText().equals("Operator"))
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
                
                String statementObject[] = {VueResources.getString("metadata.vue.url") +"#none","",edu.tufts.vue.rdf.Query.Qualifier.STARTS_WITH.toString()};
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
