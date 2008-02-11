
/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import edu.tufts.vue.metadata.CategoryModel;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.ontology.OntType;
import edu.tufts.vue.rdf.RDFIndex;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;

import tufts.vue.ActiveEvent;
import tufts.vue.ActiveListener;
import tufts.vue.LWComponent;
import tufts.vue.VUE;
import tufts.vue.gui.GUI;

/*
 * MetadataEditor.java
 *
 * Created on June 29, 2007, 2:37 PM
 *
 * @author dhelle01
 */
public class MetadataEditor extends JPanel implements ActiveListener,MetadataList.MetadataListListener {
    
    private static final boolean DEBUG_LOCAL = true;
    
    public final static String CC_ADD_LOCATION_MESSAGE = "<html> Click [+] to add this <br> custom category to your own list. </html>";
    
    // for best results: modify next two in tandem (at exchange rate of one pirxl from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    public final static int ROW_HEIGHT = 39;
    public final static int ROW_GAP = 7;
    
    public final static int ROW_INSET = 5;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    public final static java.awt.Color SAVED_KEYWORD_BORDER_COLOR = new java.awt.Color(196,196,196);
    
    public final static boolean LIMITED_FOCUS = false;
    
    public final static int CC_ADD_LEFT = 0;
    public final static int CC_ADD_RIGHT = 1;
    
    //public final static String TAG_ONT = "http://vue.tufts.edu/vue.rdfs#Tag";
    public final static String NONE_ONT = "http://vue.tufts.edu/vue.rdfs#none";
    
    private JTable metadataTable;
    private JScrollPane scroll;
    private LWComponent current;
    private LWComponent previousCurrent;
    
    private JList ontologyTypeList;
    
    private int buttonColumn = 1;
    
    private boolean showOntologicalMembership;
    
    private boolean ontologicalMembershipVisible;
    
    private JPanel ontologicalMembershipPane;
    
    private boolean focusToggle = false;
    
    // also for VUE-846 -- but not fully operational yet..
    // will likely switch to setPreferredSize on creation
    // and on addition/subtraction of rows
    /*
    public java.awt.Dimension getPreferredSize()
    {
        int height = Math.max(metadataTable.getModel().getRowCount()*50,200);
        return new java.awt.Dimension(300,metadataTable.getModel().getRowCount()*50);
    }*/
    
    public MetadataEditor(tufts.vue.LWComponent current,boolean showOntologicalMembership,boolean followAllActive)
    {
        //VUE-846 -- special case related to VUE-845 (see comment on opening from keyword item in menu) 
        if(getSize().width < 100)
        {
           setSize(new java.awt.Dimension(300,200)); 
        }
        
        
        this.current = current;
        
        if(DEBUG_LOCAL)
        {
            System.out.println("MetadataEditor - just created new instance for (current,followActive) (" + current +"," + followAllActive + ")");
        }
        
        metadataTable = new JTable(new MetadataTableModel())
        {
            public String getToolTipText(MouseEvent location)
            {
                if(getEventIsOverAddLocation(location))
                {
                    return CC_ADD_LOCATION_MESSAGE;
                }
                else
                {
                    return null;
                }
            }
        };

        //metadataTable.setGridColor(new java.awt.Color(getBackground().getRed(),getBackground().getBlue(),getBackground().getGreen(),0));
        metadataTable.setShowGrid(false);
        metadataTable.setIntercellSpacing(new java.awt.Dimension(0,0));
        
        metadataTable.setBackground(getBackground());
        metadataTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
        {
                   public void mousePressed(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         if(DEBUG_LOCAL) 
                         {
                           System.out.println("metadata table header: mouse pressed" + evt);
                           System.out.println("current at table header mouse press: " + MetadataEditor.this.current);
                         }
                         
                         
                         addNewRow();
                         
                         SwingUtilities.invokeLater(new Runnable(){
                            public void run()
                            {
                              scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());                              
                            }
                         });
                         
                       }
                   }
       });
       
       metadataTable.addMouseListener(new java.awt.event.MouseAdapter()
       {
          public void mouseClicked(java.awt.event.MouseEvent evt)
          {
             if(DEBUG_LOCAL)
             {
               System.out.println("MetadataEditor: mouse clicked on metadatatable ");
             }
                       
             if(evt.getClickCount() == 2)
             {
               /*int row = evt.getY()/metadataTable.getRowHeight();
               ((MetadataTableModel)metadataTable.getModel()).setClickedTwice(row);
               ((MetadataTableModel)metadataTable.getModel()).refresh();
               metadataTable.repaint();*/
             }
          }
           
          public void mouseReleased(java.awt.event.MouseEvent evt)
          {
             if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
             {
               java.util.List<VueMetadataElement> metadataList = MetadataEditor.this.current.getMetadataList().getMetadata();
               int selectedRow = metadataTable.getSelectedRow();
                         
               // VUE-912, stop short-circuiting on row 0, delete button has also been returned
               if(/*selectedRow > 0 &&*/ metadataTable.getSelectedColumn()==buttonColumn && metadataList.size() > selectedRow)
               {
                 metadataList.remove(selectedRow);
                 metadataTable.repaint();
                 requestFocusInWindow();
               }
             }

                       if(DEBUG_LOCAL)
                       {
                           System.out.println("MetadataEditor: ClickCustomCategoryAddLocation(CC_ADD_LEFT) " +
                                   getCustomCategoryAddLocation(CC_ADD_LEFT));
                           System.out.println("MetadataEditor mouse pressed, x location: " + evt.getX());
                       }
                               
                       if(getEventIsOverAddLocation(evt))
                       {
                           
                           if(DEBUG_LOCAL)
                           {
                               System.out.println("MetadataEditor: Mouse pressed over custom add widget");
                           }
                           
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           
                           
                           int row = metadataTable.rowAtPoint(evt.getPoint());//evt.getY()/metadataTable.getRowHeight();
                           
                          // if(((MetadataTableModel)metadataTable.getModel()).getCategoryFound(row))
                          //     return;
                           
                           String category = metadataTable.getModel().getValueAt(
                                                              row,
                                                              0).toString();
                           
                           int separatorLocation = category.indexOf(RDFIndex.ONT_SEPARATOR);
                           if(separatorLocation != -1)
                           {
                               category = category.substring(separatorLocation + 1);
                           }
                           
                           cats.addCustomCategory(category);
                           cats.saveCustomOntology();
                           //((MetadataTableModel)metadataTable.getModel()).refresh();
                           metadataTable.getCellEditor().stopCellEditing();
                           metadataTable.repaint();
                       }
                   }
        }); 

        adjustColumnModel();
        
        metadataTable.setDefaultRenderer(Object.class,new MetadataTableRenderer());
        metadataTable.setDefaultEditor(Object.class, new MetadataTableEditor());
        ((DefaultCellEditor)metadataTable.getDefaultEditor(java.lang.Object.class)).setClickCountToStart(1);
        
        metadataTable.setRowHeight(ROW_HEIGHT);
        metadataTable.getTableHeader().setReorderingAllowed(false);
        
        scroll = new JScrollPane(metadataTable);
        //scroll.setOpaque(false);
        scroll.setBackground(getBackground());
        scroll.getViewport().setBackground(getBackground());
        //System.out.println("MetadataEditor - scroll background: " + getBackground());
       /* metadataTable.addFocusListener(new FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent fe)
            {
                if(metadataTable.getSelectedColumn() == buttonColumn)
                    return;
                
                //System.out.println("MetadataEditor: selected column in focus gained " + metadataTable.getSelectedColumn());
                
                metadataTable.editCellAt(metadataTable.getSelectedRow(),metadataTable.getSelectedColumn());
                //metadataTable.editCellAt(metadataTable.getSelectedRow(),buttonColumn-1);
                MetadataTableEditor editor = (MetadataTableEditor)metadataTable.getCellEditor();
                if(editor!=null)
                {
                  //System.out.println("MetadataEditor, editor class type: " + editor.getClass());
                  if(metadataTable.getSelectedColumn() == (buttonColumn - 1) )
                    editor.focusField();
                  else
                    editor.focusCombo();
                }
            }
        }); */

        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());
        
        JPanel metaPanel = new JPanel(new BorderLayout());
        JPanel tablePanel = new JPanel(new BorderLayout());

        //add(scroll);
        
        //re-enable for VUE-953 (revert back to categories/keywords) 
        tablePanel.add(metadataTable.getTableHeader(),BorderLayout.NORTH);
        
        tablePanel.add(metadataTable);
        metaPanel.add(tablePanel);
        
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        //back to "assign categories" as per VUE-953
        //final JLabel optionsLabel = new JLabel("Use full metadata schema");
        final JLabel optionsLabel = new JLabel("Assign Categories");
        optionsLabel.setFont(GUI.LabelFace);
        //final JButton advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));//tufts.vue.gui.VueButton("advancedSearchMore");
        final JCheckBox advancedSearch = new JCheckBox();
        //advancedSearch.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        advancedSearch.addActionListener(new java.awt.event.ActionListener(){
           public void actionPerformed(java.awt.event.ActionEvent e)
           {
               MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
               if(model.getColumnCount() == 2)
               {
                 buttonColumn = 2;
                 model.setColumns(3);
                 //advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
                 advancedSearch.setSelected(true);
                 //optionsLabel.setText("less options");
               }
               else
               {
                 buttonColumn = 1;
                 model.setColumns(2);
                 //advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
                 advancedSearch.setSelected(false);
                 //optionsLabel.setText("more options");
               }
               
               adjustColumnModel();

               //System.out.println("table model set to 3 columns");
               revalidate();
           }
        });
        optionsPanel.add(advancedSearch);
        optionsPanel.add(optionsLabel);
        
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        //reverts to top of table as per VUE-953
        //JPanel addPanel = new JPanel();
        //JLabel addButton = new JLabel();
        //addButton.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
        //addPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,ROW_INSET+4));
        
        /*
        addButton.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                addNewRow();
            }
        });*/
        
        //see above: VUE-953
        //addPanel.add(addButton);
        controlPanel.add(optionsPanel);
        // see above: VUE-953
        //controlPanel.add(addPanel,BorderLayout.EAST);
        
        metaPanel.add(controlPanel,BorderLayout.SOUTH);
        
        ontologicalMembershipPane = new JPanel();
        
        //ontologicalMembershipPane.setAlignmentX(0.5f);
        
        //ontologicalMembershipPane.setLayout(new BoxLayout(ontologicalMembershipPane,BoxLayout.Y_AXIS));
        
        ontologicalMembershipPane.setLayout(new BorderLayout());
        ontologicalMembershipPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5),BorderFactory.createLoweredBevelBorder()));
        ontologyTypeList = new JList(new OntologyTypeListModel());
        JScrollPane ontologyListScroll = new JScrollPane(ontologyTypeList);
        //ontologyListScroll.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        ontologyListScroll.getViewport().setOpaque(false);
        JLabel membershipLabel = new JLabel("Ontological Membership: ");
        membershipLabel.setFont(GUI.LabelFace);
        ontologicalMembershipPane.add(membershipLabel,BorderLayout.NORTH);
        //ontologicalMembershipPane.add(ontologyListScroll);
        ontologicalMembershipPane.add(ontologyTypeList);
        //ontologyListScroll.setAlignmentX(0.0f);
        //membershipLabel.setAlignmentX(0.0f);
        
        add(metaPanel,BorderLayout.NORTH);
        
        if(showOntologicalMembership && current !=null && current.getMetadataList().getOntologyListSize() > 0)
        {
          // this functionality is now in a seperate pane  
          //add(ontologicalMembershipPane/*,BorderLayout.SOUTH*/);
          ontologicalMembershipVisible = true;
        }
        
        // followAllActive needed for MapInspector
        if(followAllActive)
        {
          tufts.vue.VUE.addActiveListener(tufts.vue.LWComponent.class,this);
        }
        else
        {
          tufts.vue.VUE.addActiveListener(tufts.vue.LWMap.class,this); 
        }
        
        //setMinimumSize(new java.awt.Dimension(getWidth(),200));
        
        MetadataList.addListener(this);
        
        setBorder(BorderFactory.createEmptyBorder(10,8,15,6));
        scroll.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        validate();
    }
    
    public void addNewRow()
    {
        VueMetadataElement vme = new VueMetadataElement();
        String[] emptyEntry = {NONE_ONT,""};
        vme.setObject(emptyEntry);
        vme.setType(VueMetadataElement.CATEGORY);
        //metadataTable.getModel().setValueAt(vme,metadataTable.getRowCount()+1,0);
        MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
        ((MetadataTableModel)metadataTable.getModel()).refresh();
    }
    
    public void listChanged()
    {
        //validate();
        //repaint();
        
        if(DEBUG_LOCAL)
        {
            System.out.println("MetadataEditor: list changed ");
        }
        
        ((MetadataTableModel)metadataTable.getModel()).refresh();
        ((OntologyTypeListModel)ontologyTypeList.getModel()).refresh();
        validate();
    }
    
    public boolean getEventIsOverAddLocation(MouseEvent evt)
    {
       boolean locationIsOver =  evt.getX() > getCustomCategoryAddLocation(CC_ADD_LEFT) && 
                          evt.getX() < getCustomCategoryAddLocation(CC_ADD_RIGHT);
       
       int row = metadataTable.rowAtPoint(evt.getPoint());//evt.getY()/metadataTable.getRowHeight();
                           
       boolean categoryIsNotInList = !((MetadataTableModel)metadataTable.getModel()).getCategoryFound(row);
                               
       return locationIsOver && categoryIsNotInList;
       
    }
    
    /**
     *
     *  returns -1 if add widget is not available (i.e. if in basic
     *  mode and/or not in "assign categories" mode)
     *
     **/
    public int getCustomCategoryAddLocation(int ccLocationConstant)
    {
        if(metadataTable.getModel().getColumnCount() == 2)
        {
            return -1;
        }
        
        if(ccLocationConstant == CC_ADD_LEFT)
        {
            return metadataTable.getColumnModel().getColumn(0).getWidth() - 20;
        }
        else if(ccLocationConstant == CC_ADD_RIGHT)
        {
            return metadataTable.getColumnModel().getColumn(0).getWidth();
        }
        else
        {
            return -1;
        }
    }
    
    public void adjustColumnModel()
    {
        int editorWidth = MetadataEditor.this.getWidth();
        //if(MetadataEditor.this.getTopLevelAncestor() != null)
        //  editorWidth = MetadataEditor.this.getTopLevelAncestor().getWidth();
        
        if(metadataTable == null)
            return;
        
        if(metadataTable.getModel().getColumnCount() == 2)
        {
          metadataTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(0).setMaxWidth(editorWidth-BUTTON_COL_WIDTH);
          metadataTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);   
        }
        else
        {
          metadataTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(2).setHeaderRenderer(new MetadataTableHeaderRenderer());
          metadataTable.getColumnModel().getColumn(0).setMaxWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          metadataTable.getColumnModel().getColumn(1).setMaxWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
          metadataTable.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH); 
        }
        
        //place holder for boolean to only create header renderers once
        //renderersCreated = true;
    }
    
    public void repaint()
    {
        super.repaint();
        adjustColumnModel();
    }
    
    public void activeChanged(ActiveEvent e)
    {
       if(e!=null)
       {
          
         focusToggle = false;  
           
         if(DEBUG_LOCAL)
         {
             System.out.println("MetadataEditor: active changed - " + e + "," + this);
         }
           
         LWComponent active = (LWComponent)e.active;
         
         metadataTable.removeEditor();
         
         previousCurrent = current;
         
         current = active;
         
         /*MetadataList metadata = null;
         
         if(metadataTable.hasFocus())
         {
             if(DEBUG_LOCAL)
             {
                 System.out.println("MetadataTable - has Focus in active changed ");
             }
             metadata = previousCurrent;
         }
         else
         {
             metadata = current;
         }*/
         
         if(current!=null && MetadataEditor.this.current.getMetadataList().getMetadata().size() == 0)
         {
           VueMetadataElement vme = new VueMetadataElement();
           String[] emptyEntry = {NONE_ONT,""};
           vme.setObject(emptyEntry);
           vme.setType(VueMetadataElement.CATEGORY);

           MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
         }
        
         if(current != null && current.getMetadataList().getOntologyListSize() > 0)
         {
             if(!ontologicalMembershipVisible)
             {
                // this functionality is now in a seperate pane
                //add(ontologicalMembershipPane);
                ontologicalMembershipVisible = true;
             }
         }
         else
         {
             if(ontologicalMembershipVisible)
             {
                // this functionality is now in a seperate pane
                //remove(ontologicalMembershipPane);
                ontologicalMembershipVisible = false;
             } 
         }
         
         
         ((MetadataTableModel)metadataTable.getModel()).refresh();
         ((OntologyTypeListModel)ontologyTypeList.getModel()).refresh();
        
         
         //adjustColumnModel();
       }
    }
    
    class MetadataTableHeaderRenderer extends DefaultTableCellRenderer
    {   
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JComponent comp = new JPanel();
           if(col == 0)
           {
               
               if(((MetadataTableModel)table.getModel()).getColumnCount() == 2)
               {    
                 // back to "Keywords:" -- VUE-953  
                 comp =  new JLabel("Keywords:");
               }
               else
               {
                 // back to "Categories" -- VUE-953
                 //comp = new JLabel("Fields:");
                 comp = new JLabel("Categories:");
               }
               comp.setFont(GUI.LabelFace);
           }
           else if(col == 1 && col != buttonColumn)
           {
               if(((MetadataTableModel)table.getModel()).getColumnCount() == 3)
               {    
                 comp =  new JLabel("Keywords:");
               }
               comp.setFont(GUI.LabelFace);
           }
           else if(col == buttonColumn)
           {
               comp = new JLabel();    
               ((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
           }
           else
           {
               comp = new JLabel("");
           }

           comp.setOpaque(true);
           comp.setBackground(MetadataEditor.this.getBackground());
           //comp.setForeground(java.awt.Color.RED);
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           //comp.setOpaque(false);
           
           //System.out.println("MetadataEditor - Table Header Renderer background color: " + MetadataEditor.this.getBackground());
           return comp;
       }
    }
    
    public boolean findCategory(Object currValue,int row,int col,int n,JComboBox categories)
    {
    
               boolean found = false;
        
               if(DEBUG_LOCAL)
               {
                   System.out.println("MetadataEditor findCategory - " + currValue);
               }
        
               //Object currValue = table.getModel().getValueAt(row,col); //.toString();
               //System.out.println("Editor -- currValue: " + currValue);
               for(int i=0;i<n;i++)
               {
                   //System.out.println("MetadataTable looking for category currValue: " + currValue);
                   //System.out.println("MetadataTable looking for category model element at " + i + " " +
                   //      categories.getModel().getElementAt(i));
                   
                   Object item = categories.getModel().getElementAt(i);
                   String currLabel = "";
                   if(currValue instanceof OntType)
                       currLabel = ((OntType)currValue).getLabel();
                   else
                       currLabel = currValue.toString();
                   
                   //System.out.println("MetadataTable currLabel:" + currLabel);
                   
                   //if(item instanceof OntType)
                    // System.out.println("MetadataTable looking at category model OntType element at " + i + " " +
                    //       ((OntType)item).getLabel());
                   
                   //System.out.println("MetadataTable ------------------- " + i );
                   
                   if(item instanceof OntType &&
                           // was temporarily rolled back for search bug
                           (((OntType)item).getBase()+"#"+((OntType)item).getLabel()).equals(currLabel))
                           //old version:
                           //((OntType)item).getLabel().equals(currLabel))
                   {
                       //System.out.println("found category");
                       categories.setSelectedIndex(i);
                       found = true;
                   }
                   
               }
               
               return found;
               
    }
    
    class MetadataTableRenderer extends DefaultTableCellRenderer
    {   
        
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JPanel comp = new JPanel();
           JComboBox categories = new JComboBox();
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           
           comp.setLayout(new BorderLayout());
           if(col == buttonColumn-2)
           {
               int n = categories.getModel().getSize();
               
               if(current.getMetadataList().getMetadata().size() <= row)
               {
                   comp.add(categories);
                   return comp;
               }
               
               
               Object currObject = current.getMetadataList().getMetadata().get(row).getObject();//table.getModel().getValueAt(row,col);
               if(!(currObject instanceof String[]))
               {
                   comp.add(categories);
                   return comp;
               }
               Object currValue = /*(edu.tufts.vue.ontology.OntType)*/(((String[])currObject)[0]);
               boolean found = findCategory(currValue,row,col,n,categories); 
              
               MetadataTableModel model = (MetadataTableModel)table.getModel();
               
               if(found || model.getClickedTwice(row))
               {    
                 model.setCategoryFound(row,true);  
                   
                 if((((String[])currObject)[1]).length() == 0)  
                  comp.add(categories);
                 else
                 {
                   String customName = currValue.toString();
                   int ontSeparatorLocation = customName.indexOf(edu.tufts.vue.rdf.RDFIndex.ONT_SEPARATOR);
                   if( ontSeparatorLocation != -1 && ontSeparatorLocation != customName.length() - 1)
                   {
                       customName = customName.substring(ontSeparatorLocation + 1,customName.length());
                   }
                   JLabel custom = new JLabel(customName);
                   custom.setFont(GUI.LabelFace);
                   comp.add(custom);
                 }
               }
               else
               {
                 model.setCategoryFound(row,false);  
                 String customName = currValue.toString();
                 int ontSeparatorLocation = customName.indexOf(edu.tufts.vue.rdf.RDFIndex.ONT_SEPARATOR);
                 if( ontSeparatorLocation != -1 && ontSeparatorLocation != customName.length() - 1)
                 {
                     customName = customName.substring(ontSeparatorLocation + 1,customName.length());
                 }
                 JLabel custom = new JLabel(customName+"*");
                 custom.setFont(GUI.LabelFace);
                 comp.add(custom);
                 JLabel addLabel = new JLabel("[+]");
                 comp.add(addLabel,BorderLayout.EAST);
               }
               
           }
           else if(col == buttonColumn-1)
           {
               if(value instanceof OntType)
               {
                 comp.add(new JLabel(((OntType)value).getLabel()));
               }
               else
               if(value instanceof VueMetadataElement)
               {
                   VueMetadataElement vme = (VueMetadataElement)value;
                   if(vme.getType() == VueMetadataElement.CATEGORY  && !vme.getValue().equals(""))
                   {
                     //comp.add(new JTextField(vme.getValue()));
                     comp.add(new JLabel(vme.getValue()));
                   }
                   else if(vme.getType() == VueMetadataElement.CATEGORY && vme.getValue().equals("") )
                   {
                     comp.add(new JTextField(vme.getValue()));
                   }
               }
               else
               {
                 if(value.toString().trim().equals(""))
                 {
                   JTextField field = new JTextField(value.toString());
                   field.setFont(GUI.LabelFace);
                   comp.add(field);  
                 }
                 else
                 {
                   JLabel label = new JLabel(value.toString());
                   label.setFont(GUI.LabelFace);
                   comp.add(label);
                 }
               }
                 
           }
           else if(col == buttonColumn)               
           {
               JLabel buttonLabel = new JLabel();
               
               // VUE-912, put delete button back -- also add back mouselistener
               //if(row!=0)
               //{    
                 buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               //}
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
           
           comp.setBorder(getMetadataCellBorder(row,col));
           
           /*if(col != buttonColumn && row != metadataTable.getModel().getRowCount() - 1)
           {    
             comp.setBorder(BorderFactory.createCompoundBorder(
                     BorderFactory.createMatteBorder(1,1,0,1,SAVED_KEYWORD_BORDER_COLOR),
                     BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET)
                     ));
           }
           else if(col != buttonColumn)
           {
             comp.setBorder(BorderFactory.createCompoundBorder(
                     BorderFactory.createMatteBorder(1,1,1,1,SAVED_KEYWORD_BORDER_COLOR),
                     BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET)
                     ));  
           }
           else
           {    
             comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           }*/
           
           return comp;
       } 
           
    }
    
    class MetadataTableEditor extends DefaultCellEditor
    {   
        
       private JTextField field; 
       private JComboBox categories;
        
       public MetadataTableEditor()
       {
           super(new JTextField());
           field = (JTextField)getComponent();
           //field.attachProperty(current, tufts.vue.LWKey.Notes);
           //setClickCountToStart(1);
       }
       
       public void focusField()
       {           
           field.requestFocus();
       }
       
       public void focusCombo()
       {
          // categories.requestFocus();
           //categories.firePopupMenuWillBecomeVisible();
           categories.showPopup();
       }
       
       public boolean stopCellEditing()
       {
           if(DEBUG_LOCAL)
           {
               System.out.println("MetadataEditor MetadataTableEditor - stop cell editing - set previousCurrent to current");
           }
           previousCurrent = current;
           return super.stopCellEditing();
       } 
        
       public java.awt.Component getTableCellEditorComponent(final JTable table,final Object value,boolean isSelected,final int row,final int col)
       {
           final JTextField field = new JTextField();
           categories = new JComboBox();
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           final JLabel categoryLabel = new JLabel();
           final JLabel notEditable = new JLabel();
           final JPanel comp = new JPanel();
           
           notEditable.setFont(GUI.LabelFace);
           field.setFont(GUI.LabelFace);
           
           if(DEBUG_LOCAL)
           {
               System.out.println("MetadataEditor getTableCellEditorComponent -- about to add item listener -- ");
           }
           
           categories.addItemListener(new ItemListener() 
           {
               public void itemStateChanged(ItemEvent e) 
               {
                   if(DEBUG_LOCAL)
                   {    
                      System.out.println("MetdataEditor categories item listener itemStateChanged: " + e);
                   }
                   
                   
                   if(e.getStateChange() == ItemEvent.SELECTED)
                   {   
                       if(e.getItem() instanceof edu.tufts.vue.metadata.gui.EditCategoryItem)
                       {
                           if(DEBUG_LOCAL)
                           {    
                             System.out.println("MetdataEditor edit item selected: " + e);
                           }
                           //tufts.vue.gui.DockWindow ec = tufts.vue.gui.GUI.createDockWindow("Edit Categories", new CategoryEditor());
                           //ec.setBounds(475,300,300,250);
                           //ec.pack();
                           //ec.setVisible(true);
                           
                           JDialog ecd = new JDialog(VUE.getApplicationFrame(),"Edit Categories");
                           ecd.setModal(true);
                           ecd.add(new CategoryEditor(ecd,categories,MetadataEditor.this,current,row,col));
                           ecd.setBounds(475,300,300,250);
                           //ecd.pack();
                           ecd.setVisible(true);
                           
                           metadataTable.getCellEditor().stopCellEditing();
                           ((MetadataTableModel)metadataTable.getModel()).refresh();
                           
                          /* int n = categories.getModel().getSize();
                           Object currObject = current.getMetadataList().getMetadata().get(row).getObject();
                           Object currValue = (((String[])currObject)[0]);
                           findCategory(currValue,row,col,n,categories);*/
                       }
                   }
               }
           });
           
          /* categories.addFocusListener(new FocusAdapter(){
               public void focusLost(java.awt.event.FocusEvent fe)
               {
                   System.out.println("MetadataEditor - focus lost on categories drop down ");
                   System.out.println("value: " + value);
                   System.out.println("vme value " + table.getModel().getValueAt(row,buttonColumn - 1));
                   //table.getModel().setValueAt(vme,row,col);
               }
           });*/
           
           categories.addItemListener(new java.awt.event.ItemListener(){
              public void itemStateChanged(java.awt.event.ItemEvent ie)
              {
                  
                  if(DEBUG_LOCAL)
                  {
                      System.out.println("Categories - item listener - item state changed - " + ie);
                  }    
                  
                  if(ie.getStateChange()==java.awt.event.ItemEvent.SELECTED)
                  {
                      
                    if(categories.getSelectedItem() instanceof edu.tufts.vue.metadata.gui.EditCategoryItem)
                    {
                        return;
                    }
                      
                    if(!(categories.getSelectedItem() instanceof OntType) || !(ie.getItem() instanceof OntType))
                    {
                        if(DEBUG_LOCAL)
                        {
                            System.out.println("MetadataEditor -- non ontology category type selected " + categories.getSelectedItem().getClass());
                        }
                        return;
                    }
                    
                    if(DEBUG_LOCAL)
                    {
                      OntType item = (OntType)(ie.getItem());//categories.getSelectedItem();  
                        
                      System.out.println("MetadataEditor - categories item state changed: " + ie);
                      System.out.println("MetadataEditor - category item base - " + item.getBase());
                      System.out.println("MetadataEditor - category item label - " + item.getLabel());
                      
                      //System.out.println("MetadataEditor -- textfield value: " + table.getModel().getValueAt(row,buttonColumn - 1));
                    }
                    VueMetadataElement vme = new VueMetadataElement();
                    
                    // was temporarily rolled back for search bug
                    String[] keyValuePair = {((OntType)categories.getSelectedItem()).getBase()+"#"+((OntType)categories.getSelectedItem()).getLabel(),table.getModel().getValueAt(row,buttonColumn - 1).toString()};
                    //old version:
                    //String[] keyValuePair = {((OntType)categories.getSelectedItem()).getLabel(),table.getModel().getValueAt(row,buttonColumn - 1).toString()};
                    
                    
                    vme.setObject(keyValuePair);
                    vme.setType(VueMetadataElement.CATEGORY);
                    //table.getModel().setValueAt(vme,row,col);
                    if(current.getMetadataList().getMetadata().size() > (row))
                    {
                      current.getMetadataList().getMetadata().set(row,vme);
                    }
                    else
                    {
                      current.getMetadataList().getMetadata().add(vme); 
                    }
                  }
              }
           });
           
           field.addFocusListener(new FocusAdapter(){
              public void focusLost(java.awt.event.FocusEvent fe)
              {  
                  if(current == null || VUE.getActiveMap() == null || VUE.getActiveViewer() == null)
                  {
                      return;
                  }
                  
                  //tufts.vue.UndoManager undo = tufts.vue.VUE.getActiveMap().getUndoManager();
                  //undo.mark("metadata value");
                  VUE.getActiveMap().markAsModified();
                  
                  if(DEBUG_LOCAL)
                  {
                    System.out.println("metadata value change marked...");
                    System.out.println("MetadataEditor focuslost row -- " + row);
                    System.out.println("MetadataEditor focuslost current -- " + current);
                    try
                    {
                      System.out.println("MetadataEditor focuslost opposite component " + fe.getOppositeComponent().getClass() );
                    }
                    catch(Exception e)
                    {
                      System.out.println("MetadataEditor debug -- exception in finding focus lost opposite component: " + e);
                    }
                  }
                  
                  if(fe!= null && fe.getOppositeComponent() == categories)
                  {
                      return;
                  }
                  
                  java.util.List<VueMetadataElement> metadata = null;
                  
                  if(previousCurrent == null && current == null)
                  {
                      if(DEBUG_LOCAL)
                      {
                          System.out.println("MetadataEditor - there was no previous current or current");
                          System.out.println("exiting focusLost");
                      }
                      return;
                  }
                  else
                  if(previousCurrent != null && !focusToggle)
                  {
                     metadata = previousCurrent.getMetadataList().getMetadata();
                  }    
                  else 
                  {
                     metadata = current.getMetadataList().getMetadata();
                  }
                  
                  VueMetadataElement currentVME = null;
                  
                  if(metadata.size() > 0)
                   currentVME = metadata.get(row);
                  
                  VueMetadataElement vme = new VueMetadataElement();
                  //System.out.println("MetadataEditor -- value at position where text field focus lost: " +  table.getModel().getValueAt(row,col));
                  
                  if(currentVME==null)
                  {
                               //VueMetadataElement vme = new VueMetadataElement();
                     String[] emptyEntry = {NONE_ONT,""};
                     vme.setObject(emptyEntry);
                     vme.setType(VueMetadataElement.CATEGORY);  
                      
                      
                    //vme.setObject(field.getText());   
                  }
                  else
                  if(!(currentVME.getObject() instanceof String[]))
                  {
                    //vme.setObject(field.getText());
                      
                               //VueMetadataElement vme = new VueMetadataElement();
                     String[] emptyEntry = {NONE_ONT,""};
                     vme.setObject(emptyEntry);
                     vme.setType(VueMetadataElement.CATEGORY);
                  }
                  else
                  {
                    String[] obj = (String[])currentVME.getObject();  
                    String[] pairedValue = {obj[0],field.getText()};
                    vme.setObject(pairedValue);
                  }
                  //table.getModel().setValueAt(vme,row,col);
                  if(metadata.size() > (row))
                  {
                    metadata.set(row,vme);
                  }
                  else
                  {
                    metadata.add(vme); 
                  }
                  
                  ((MetadataTableModel)metadataTable.getModel()).setClickedOnce(row);
                     
                  current.layout();
                  
                  VUE.getActiveViewer().repaint();
                  
                  comp.remove(field);
                  comp.add(notEditable);
                  metadataTable.repaint();
              }
              
              public void focusGained(java.awt.event.FocusEvent fe)
              {
                  focusToggle = true;
              }
           });
           //final JPanel comp = new JPanel();
           comp.setLayout(new BorderLayout());
           if(col == buttonColumn - 2)
           {
               //categories.setSelectedIndex(1);
               //int loc = 0;
               final int n = categories.getModel().getSize();
               
               if(current.getMetadataList().getMetadata().size() <= row)
               {
                   comp.add(categories);
                   return comp;
               }

               Object currObject = current.getMetadataList().getMetadata().get(row).getObject();//table.getModel().getValueAt(row,col);
               if(!(currObject instanceof String[]))
               {
                   comp.add(categories);
                   return comp;
               }
               final Object currValue = /*(edu.tufts.vue.ontology.OntType)*/(((String[])currObject)[0]);
               Object currFieldValue = (((String[])currObject)[1]);
               
               if(currFieldValue.toString().length() != 0)
               {
                   String displayString = currValue.toString();
                   int nameIndex = displayString.indexOf("#");
                   if(nameIndex != -1 && ( (nameIndex + 1)< displayString.length()) ) 
                   {
                     displayString = displayString.substring(nameIndex + 1);
                   }
                   //JLabel categoryLabel = new JLabel(displayString);
                   categoryLabel.setText(displayString);
                   
                   categoryLabel.addMouseListener(new MouseAdapter(){
                    public void mouseClicked(MouseEvent me)
                    {
                        if(DEBUG_LOCAL)
                        {
                            System.out.println("Metadataeditor: Mouse clicked on category label");
                        }
                        
                        if(me.getClickCount() == 2)
                        {
                            comp.remove(categoryLabel);
                            findCategory(currValue,row,col,n,categories);
                            comp.add(categories);
                            //field.requestFocusInWindow();
                            //((MetadataTableModel)table.getModel()).setClickedTwice(row);
                            ((MetadataTableModel)table.getModel()).refresh();
                        }
                    }
                   });
               //}
                   
                   //categoryLabel.setText(displayString);
                   categoryLabel.setFont(GUI.LabelFace);
                   
                   boolean found = findCategory(currValue,row,col,n,categories);
                   
                   if(found)
                   {    
                     comp.add(categoryLabel);
                   }
                   else
                   {
                     categoryLabel.setText(categoryLabel.getText() + "*"); 
                     comp.add(categoryLabel);
                   }    
                   comp.setBorder(getMetadataCellBorder(row,col));
                   return comp;
               }
               findCategory(currValue,row,col,n,categories); 
               
               /*
               //System.out.println("Editor -- currValue: " + currValue);
               for(int i=0;i<n;i++)
               {
                   System.out.println("MetadataTable looking for category currValue: " + currValue);
                   System.out.println("MetadataTable looking for category model element at " + i + " " +
                           categories.getModel().getElementAt(i));
                   
                   Object item = categories.getModel().getElementAt(i);
                   String currLabel = "";
                   if(currValue instanceof OntType)
                       currLabel = ((OntType)currValue).getLabel();
                   else
                       currLabel = currValue.toString();
                   
                   if(item instanceof OntType)
                     System.out.println("MetadataTable looking at category model OntType element at " + i + " " +
                           ((OntType)item).getLabel());
                   
                   if(item instanceof OntType &&
                           ((OntType)item).getLabel().equals(currLabel))
                   {
                       //System.out.println("found category");
                       categories.setSelectedIndex(i);
                   }
               }*/
               comp.add(categories);
           }
           else
           if(col == (buttonColumn - 1))
           {
               if(value instanceof String)
                 field.setText(value.toString());
               if(value instanceof VueMetadataElement)
               {
                 VueMetadataElement vme = (VueMetadataElement)value;
                 field.setText(vme.getValue());
               }
               if(((MetadataTableModel)table.getModel()).getClickedTwice(row) || value.toString().equals(""))
               {
                 comp.add(field);
               }
               else
               {
                 //final JLabel notEditable = new JLabel(field.getText() + "test");  
                 notEditable.setText(field.getText());
                 comp.add(notEditable);
                 notEditable.addMouseListener(new MouseAdapter(){
                    public void mouseClicked(MouseEvent me)
                    {
                        if(DEBUG_LOCAL)
                        {
                            System.out.println("Mouse clicked on notEditable label");
                        }
                        
                        if(me.getClickCount() == 2)
                        {
                            comp.remove(notEditable);
                            comp.add(field);
                            field.requestFocusInWindow();
                            ((MetadataTableModel)table.getModel()).setClickedTwice(row);
                            ((MetadataTableModel)table.getModel()).refresh();
                        }
                    }
                 });
               }
           }
           else if(col ==  buttonColumn)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
           //comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           comp.setBorder(getMetadataCellBorder(row,col));
           
           if(LIMITED_FOCUS)
           {
             field.addMouseListener(new MouseAdapter() {
                 public void mouseExited(MouseEvent me)
                 {
                     //System.out.println("MetadataEditor table cell - exited: " + me);
                     //System.out.println("MetadataEditor -  mouse exited table - " + me);
                       stopCellEditing();
                 }
             });
           }
           
           //comp.setOpaque(true);
           //comp.setBackground(java.awt.Color.RED);
           
           return comp;
       }
       
    }
    
    public Border getMetadataCellBorder(int row,int col)
    {
      Border border = BorderFactory.createEmptyBorder();
      Border insetBorder = BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET);
      Border fullBox = BorderFactory.createMatteBorder(1,1,1,1,SAVED_KEYWORD_BORDER_COLOR);
      Border verticalFollowingBox = BorderFactory.createMatteBorder(1,1,0,1,SAVED_KEYWORD_BORDER_COLOR);
      Border leftLeadingFullBox = BorderFactory.createMatteBorder(1,1,1,0,SAVED_KEYWORD_BORDER_COLOR);
      Border leftLeadingVerticalFollowingBox = BorderFactory.createMatteBorder(1,1,0,0,SAVED_KEYWORD_BORDER_COLOR);
        
      if(col != buttonColumn &&  
              (
                row != metadataTable.getModel().getRowCount() - 1 
                && row != metadataTable.getModel().getRowCount() - 2 
              )
        )
      {    
        if(col == buttonColumn - 2)
        {    
          border = BorderFactory.createCompoundBorder(leftLeadingVerticalFollowingBox,insetBorder);
        }
        else
        {
          border = BorderFactory.createCompoundBorder(verticalFollowingBox,insetBorder);
        }
      }
      else if(col != buttonColumn && (row == metadataTable.getModel().getRowCount() - 2 )  )
      {
        if(col == buttonColumn - 2)
        {
          border = BorderFactory.createCompoundBorder(leftLeadingFullBox,insetBorder);  
        }
        else
        {
          border = BorderFactory.createCompoundBorder(fullBox,insetBorder);
        }
      }
      else
      {    
        border = insetBorder;
      }
        
       return border;
     }
    
    /**
     *
     * watch out for current == null
     *
     **/
    class MetadataTableModel extends AbstractTableModel
    {
         // this is only the default, hopefully any changes to column model
         // in future will be heard here and reflected in this data
         // (there will be 3 columns for "advanced view"
         private int cols = 2;
         
         private java.util.ArrayList<Boolean> categoryIncluded = new java.util.ArrayList<Boolean>();
         private java.util.HashMap<Integer,Integer> rowClicks = new java.util.HashMap<Integer,Integer>();
        
         public void setClickedTwice(int row)
         {
            rowClicks.put(new Integer(row),new Integer(2));
         }
         
         public void setClickedOnce(int row)
         {
            rowClicks.put(new Integer(row),new Integer(1));
         }
         
         public boolean getClickedTwice(int row)
         {
             if(rowClicks.get(new Integer(row)) == null || rowClicks.get(new Integer(row)).equals(new Integer(1)))
             {
                 return false;
             }
             else
             {
                 return true;
             }
               
         }
         
         public boolean getCategoryFound(int row)
         {
            try
            {  
              return Boolean.parseBoolean(categoryIncluded.get(row).toString());
            }
            catch(Exception e)
            {
              return Boolean.TRUE;
            }
         }
         
         public void setCategoryFound(int row,boolean found)
         {
             
             if(categoryIncluded.size() <= row)
             {
                 for(int i = categoryIncluded.size();i<row + 1;i++)
                 {
                     categoryIncluded.add(Boolean.TRUE);
                 }
             } 
             
            categoryIncluded.set(row,Boolean.valueOf(found));
         }
         
         public int getRowCount()
         {
             // awkward? -- current getMetadata in LWcomponent does something else
             // how about getMetadataBundle.getMetadataList()?
             if(current !=null)
             {   
               MetadataList.CategoryFirstList list = (MetadataList.CategoryFirstList)current.getMetadataList().getMetadata();
               int size = list.getCategoryEndIndex();
               //int size = current.getMetadataList().getMetadata().size();
               if(size > 0)
                   return size;
               else
                   return 1;
             }
             else
               return 1;
         }
         
         public int getColumnCount()
         {
             return cols;
         }
         
         public void setColumns(int cols)
         {
             this.cols = cols;
             fireTableStructureChanged();
         }
         
         public boolean isCellEditable(int row,int column)
         {
             if(getValueAt(row,column) instanceof OntType)
                 return false;
             if( (column == buttonColumn -1) || (column == buttonColumn - 2) )
                 return true;
             else
                 return false;
         }
         
         public Object getValueAt(int row, int column)
         {
             
             /*if(categoryIncluded.size() < row + 1)
             {
                 while(categoryIncluded.size() < row)
                 {
                     categoryIncluded.add(Boolean.TRUE);
                 }
             }*/
             
             categoryIncluded.trimToSize();
             
             if(current == null)
                 return "null";
             
             if(column == buttonColumn - 2)
             {
                java.util.List<VueMetadataElement> list = current.getMetadataList().getMetadata();  
                MetadataList.CategoryFirstList cfList = (MetadataList.CategoryFirstList)list;
                if(row!=0 || cfList.getCategoryEndIndex() > 0)  
                {
                  VueMetadataElement ele = list.get(row);
                  //$
                    if(!(ele.getObject() instanceof String[]))
                    {
                      //System.out.println("MetadataEditor: returning non string[] element of type -- " + ele.getObject().getClass());
                      return ele.getObject();
                    }
                  //$
                  String[] obj = (String[])ele.getObject();
                  if(obj == null)
                  {
                    return ele.getKey();
                  }
                  else
                  {
                    return obj[0];
                  }
                }
                else
                {
                  //System.out.println("MetadataEditor - creating new empty tag in getValueAt() ");
                  VueMetadataElement vme = new VueMetadataElement();
                  String[] emptyEntry = {NONE_ONT,""};
                  vme.setObject(emptyEntry);
                  vme.setType(VueMetadataElement.CATEGORY);
                  current.getMetadataList().getMetadata().add(vme);
                  return vme;
                }
             }
             else
             if( (column == buttonColumn - 1) && current != null)
             {    
               java.util.List<VueMetadataElement> list = current.getMetadataList().getMetadata();
               MetadataList.CategoryFirstList cfList = (MetadataList.CategoryFirstList)list;
               if(row!=0 || cfList.getCategoryEndIndex() > 0)  
               {
                 /*if(list.size() == 0)
                 {
                   VueMetadataElement vme = new VueMetadataElement();
                   String[] emptyEntry = {NONE_ONT,""};
                   vme.setObject(emptyEntry);
                   vme.setType(VueMetadataElement.CATEGORY);
                   current.getMetadataList().getMetadata().add(vme);
                   return vme;
                 }*/
                 VueMetadataElement ele = list.get(row);
                 /*if(ele.getObject()!=null)
                 {    
                   return ele.getObject();
                 }
                 else
                 {*/
                   if(ele.getType() == VueMetadataElement.ONTO_TYPE)
                   {
                      OntType type = new OntType();
                      type.setLabel(ele.getValue().substring(ele.getValue().lastIndexOf("#")+1,ele.getValue().length()));
                      type.setBase(ele.getValue().substring(0,ele.getValue().lastIndexOf("#")-1));
                      return type;
                   }  
                   else if(ele.getType() == VueMetadataElement.CATEGORY)
                   {
                       String[] valuePair = (String[])ele.getObject();
                       if(valuePair == null)
                       {    
                         return ele.getValue();
                       }
                       else
                       {
                         return valuePair[1];
                       }
                   }
                   else
                   {
                    return ele.getValue();
                   }
                 //}
               }
               else
               {
                  //System.out.println("MetadataEditor - creating new empty tag in getValueAt() from text field column ");
                  VueMetadataElement vme = new VueMetadataElement();
                  String[] emptyEntry = {NONE_ONT,""};
                  vme.setObject(emptyEntry);
                  vme.setType(VueMetadataElement.CATEGORY);
                  current.getMetadataList().getMetadata().add(vme);
                  return vme;
               }
             }
             else
             {
               return "delete button";
             }
         }
         
         public void setValueAt(Object value,int row, int column)
         {
             
             //System.out.println("MetadataEditor setValueAt -- value class -- " + value.getClass());
             //System.out.println("MetadataEditor setValueAt -- value -- " + value);
             
             /*
             
             if(row == (buttonColumn - 2))
             {
                   
             }
             if(row == (buttonColumn - 1))
             {
                 
             }
             
             if(current.getMetadataList().getMetadata().size() > row)
             {
               if(value instanceof String)
                 current.getMetadataList().getMetadata().get(row).setObject(value);
               if(value instanceof VueMetadataElement)  
                 current.getMetadataList().getMetadata().set(row,(VueMetadataElement)value);
             }
             else
             {
               VueMetadataElement vme = new VueMetadataElement();
               if(value instanceof VueMetadataElement)    
                 vme.setObject(((VueMetadataElement)value).getObject());
               else 
                 vme.setObject(value);
               current.getMetadataList().getMetadata().add(vme);
             }
             fireTableDataChanged(); */
         }
         
         public void refresh()
         {
             fireTableDataChanged();
         }
         
    }
    
    class OntologyTypeListModel extends DefaultListModel
    {
        /*public void removeListDataListener(ListDataListener l)
        {
            
        }
        
        public void addListDataListener(ListDataListener l)
        {
            
        }*/
        
        public Object getElementAt(int i)
        {
            Object ele = current.getMetadataList().getOntologyListElement(i).getObject();
            if(ele != null && ele instanceof OntType)
                return ((OntType)ele).getLabel();
            else if(ele != null)
                return ele.toString();
            else
                return "";
        }
        
        public int getSize()
        {
            if(current !=null)
              return current.getMetadataList().getOntologyListSize();
            else
              return 0;
        }
        
        public void refresh()
        {
             fireContentsChanged(this,0,getSize());
        }
    }
    
}
