
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
    
    private static final boolean DEBUG_LOCAL = false;
    
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
    
    public final static Border insetBorder = BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET);
    public final static Border fullBox = BorderFactory.createMatteBorder(1,1,1,1,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border verticalStartingBox = BorderFactory.createMatteBorder(0,1,1,1,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border verticalFollowingBox = BorderFactory.createMatteBorder(1,1,0,1,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border leftLeadingFullBox = BorderFactory.createMatteBorder(1,1,1,0,SAVED_KEYWORD_BORDER_COLOR);
    public final static Border leftLeadingVerticalFollowingBox = BorderFactory.createMatteBorder(1,1,0,0,SAVED_KEYWORD_BORDER_COLOR);
    
    
    private JTable metadataTable;
    private LWComponent current;
    private LWComponent previousCurrent;
    
    private int buttonColumn = 1;

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
        
        setMinimumSize(new java.awt.Dimension(300,200));
        
        this.current = current;
        
        //if(DEBUG_LOCAL)
        //{
        //    System.out.println("MetadataEditor - just created new instance for (current,followActive) (" + current +"," + followAllActive + ")");
        //}
        
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

        metadataTable.setShowGrid(false);
        metadataTable.setIntercellSpacing(new java.awt.Dimension(0,0));
        
        metadataTable.setBackground(getBackground());
        metadataTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
        {
                   public void mousePressed(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         addNewRow();                         
                       }
                       else
                       {
                           int row = metadataTable.rowAtPoint(evt.getPoint());
                           ((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
                           if(metadataTable.getCellEditor() !=null)
                           {    
                             metadataTable.getCellEditor().stopCellEditing();
                           }
                           metadataTable.repaint();
                       }
                   }
       });
       
       addMouseListener(new java.awt.event.MouseAdapter()
       {
          public void mousePressed(java.awt.event.MouseEvent evt)
          {
              int row = metadataTable.rowAtPoint(evt.getPoint());
              int lsr = ((MetadataTableModel)metadataTable.getModel()).lastSavedRow;
              if(lsr != row)
              {    
                ((MetadataTableModel)metadataTable.getModel()).setSaved(lsr,true);  
                ((MetadataTableModel)metadataTable.getModel()).lastSavedRow = -1;
              }
              ((MetadataTableModel)metadataTable.getModel()).refresh();
              
              TableCellEditor editor = metadataTable.getCellEditor();
              if(editor != null)
                  editor.stopCellEditing();
              
              metadataTable.repaint();
          }
       });
       
       metadataTable.addMouseListener(new java.awt.event.MouseAdapter()
       {
          public void mousePressed(MouseEvent evt)
          {
              MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
              if(model.lastSavedRow != metadataTable.rowAtPoint(evt.getPoint()))
                model.setSaved(model.lastSavedRow,true);
              metadataTable.repaint();
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
                               
                       if(getEventIsOverAddLocation(evt))
                       {
                           
                           
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           
                           
                           int row = metadataTable.rowAtPoint(evt.getPoint());//evt.getY()/metadataTable.getRowHeight();
                           
                          // if(((MetadataTableModel)metadataTable.getModel()).getCategoryFound(row))
                          //     return;
                           
                           String category = ((VueMetadataElement)(metadataTable.getModel().getValueAt(
                                                              row,
                                                              0))).getKey();
                           
                           int separatorLocation = category.indexOf(RDFIndex.ONT_SEPARATOR);
                           if(separatorLocation != -1)
                           {
                               category = category.substring(separatorLocation + 1);
                           }
                           
                           cats.addCustomCategory(category);
                           cats.saveCustomOntology();
                           //((MetadataTableModel)metadataTable.getModel()).refresh();
                           if(metadataTable.getCellEditor() != null)
                             metadataTable.getCellEditor().stopCellEditing();
                           metadataTable.repaint();
                       }
                   }
        }); 

        adjustColumnModel();
        
        metadataTable.setDefaultRenderer(Object.class,new MetadataTableRenderer());
        metadataTable.setDefaultEditor(Object.class, new MetadataTableEditor());
        ((DefaultCellEditor)metadataTable.getDefaultEditor(java.lang.Object.class)).setClickCountToStart(2);
        
        metadataTable.setRowHeight(ROW_HEIGHT);
        metadataTable.getTableHeader().setReorderingAllowed(false);

        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());
        
        JPanel metaPanel = new JPanel(new BorderLayout());
        JPanel tablePanel = new JPanel(new BorderLayout());
        
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
        
        add(metaPanel,BorderLayout.NORTH);
        

        
        // followAllActive needed for MapInspector, it only follows the map, 
        // not any particular component
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
        
        validate();
    }
    
    public void refresh()
    {
    	validate();
        repaint();
    	((MetadataTableModel)metadataTable.getModel()).refresh();    
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
        if(DEBUG_LOCAL)
        {
            System.out.println("MetadataEditor: list changed ");
        }
        
        ((MetadataTableModel)metadataTable.getModel()).refresh();
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
        

         
         // clear focus and saved information
         ((MetadataTableModel)metadataTable.getModel()).clearGUIInfo();
         
         ((MetadataTableModel)metadataTable.getModel()).refresh();
         //((OntologyTypeListModel)ontologyTypeList.getModel()).refresh();
        
         
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
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           return comp;
       }
    }
    
    public boolean findCategory(Object currValue,int row,int col,int n,JComboBox categories)
    {
    
               boolean found = false;
        
               //if(DEBUG_LOCAL)
               //{
               //    System.out.println("MetadataEditor findCategory - " + currValue);
               //}
        
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
        
       private JComboBox categories = new JComboBox(); 
        
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JPanel comp = new JPanel();
           //JComboBox categories = new JComboBox();
           categories.setFont(GUI.LabelFace);
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           
           comp.setLayout(new BorderLayout());
           if(col == buttonColumn-2)
           {
               int n = categories.getModel().getSize();
               
               /*if(current.getMetadataList().getMetadata().size() <= row)
               {
                   comp.add(categories);
                   return comp;
               }*/
               
               
               Object currObject = current.getMetadataList().getMetadata().get(row).getObject();//table.getModel().getValueAt(row,col);
               /*if(!(currObject instanceof String[]))
               {
                   comp.add(categories);
                   return comp;
               }*/
               Object currValue = /*(edu.tufts.vue.ontology.OntType)*/(((String[])currObject)[0]);
               boolean found = findCategory(currValue,row,col,n,categories); 
              
               MetadataTableModel model = (MetadataTableModel)table.getModel();
               
               if((found) || !model.getIsSaved(row))//model.getClickedTwice(row))
               {    
                 model.setCategoryFound(row,true);  
                   
                 if(!model.getIsSaved(row) )//|| (((String[])currObject)[1]).length() == 0 )
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
               
               boolean saved = ((MetadataTableModel)metadataTable.getModel()).getIsSaved(row);
               
               if(value instanceof VueMetadataElement)
               {
                   VueMetadataElement vme = (VueMetadataElement)value;
                   if(saved == true)
                   //if(vme.getType() == VueMetadataElement.CATEGORY  
                   //   && !vme.getValue().equals("") )
                   {
                     //comp.add(new JTextField(vme.getValue()));
                     JLabel custom = new JLabel(vme.getValue());
                     custom.setFont(GUI.LabelFace);
                     comp.add(custom);
                   }
                   else //if(vme.getType() == VueMetadataElement.CATEGORY 
                        //   && vme.getValue().equals("")
                        //   && saved == fals)
                   if(saved == false)
                   {
                     JTextField field = new JTextField(vme.getValue());
                     field.setFont(GUI.LabelFace);
                     comp.add(field);
                   }
               } 
               else
               {
                   if(DEBUG_LOCAL)
                   {
                       System.out.println("MetadataEditor -- renderer for field not vme: " + value.getClass());
                   }
               }
               //else
               //{
               //  if(value.toString().trim().equals(""))
               //  {
               //    JTextField field = new JTextField(value.toString());
               //    field.setFont(GUI.LabelFace);
               //    comp.add(field);  
               //  }
               //  else
               //  {
               //    JLabel label = new JLabel(value.toString());
               //    label.setFont(GUI.LabelFace);
               //    comp.add(label);
               //  }
               //}
                 
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
           
           return comp;
       } 
           
    }
    
    class MetadataTableEditor extends DefaultCellEditor
    {   
        
       // main entry point for editor interaction 
       public boolean isCellEditable(java.util.EventObject object)
       {
           //if(DEBUG_LOCAL)
           //{
               //System.out.println("MetadataTableEditor cell editor isCellEditable -- source :" + object.getSource());
               //System.out.println("MetadataTableEditor cell editor isCellEditable -- object :" + object);
               
               //if(object.getClass().getSuperclass() != null)
               //{
               //  System.out.println("MetadataTableEditor cell editor isCellEditable -- object super class :" + object.getClass().getSuperclass());
               //}
               
               //if(object instanceof MouseEvent)
               //{    
               //  System.out.println("MetadataTableEditor cell editor isCellEditable -- row selected: " + 
               //                       metadataTable.rowAtPoint(((MouseEvent)object).getPoint()));
               //}
 
           //}

           int row = -1;
           
           if(object instanceof MouseEvent)
           {    
                row = metadataTable.rowAtPoint(((MouseEvent)object).getPoint());
           }
           
           if(object instanceof MouseEvent)
           {    
             MouseEvent event = (MouseEvent)object;
             if(event.getClickCount() == 2 ) 
             {    
               ((MetadataTableModel)metadataTable.getModel()).setSaved(row,false);
               int lsr = ((MetadataTableModel)metadataTable.getModel()).lastSavedRow;
               if(lsr != row)
               {    
                 ((MetadataTableModel)metadataTable.getModel()).setSaved(lsr,true);
               }
               
               ((MetadataTableModel)metadataTable.getModel()).refresh();
               metadataTable.repaint();
               
               return true;
             }
             else if( row != -1 &&
                      !(((MetadataTableModel)metadataTable.getModel()).getIsSaved(row)) )
                      // really need "not saved" on row -for single click (not "value empty or anything like that"
             {
                 ((MetadataTableModel)metadataTable.getModel()).setSaved(row,false);
                 return true;
             }
             else
             {
               return false;
             }
           }
           else
             return false;
       }
        
        
       private JTextField field; 
       //private JComboBox categories = new JComboBox();
       //private JLabel categoryLabel = new JLabel();
       //private JLabel notEditable = new JLabel();
       //private JPanel comp = new JPanel();
       
       private int currentRow;
        
       public MetadataTableEditor()
       {
           super(new JTextField());
           field = (JTextField)getComponent();
           
           //categories.setFont(GUI.LabelFace);
           //categories.setModel(new CategoryComboBoxModel());
           //categories.setRenderer(new CategoryComboBoxRenderer());

           
       }
       
       public int getRow()
       {
           return currentRow;
       }
       
       public void focusField()
       {           
           field.requestFocus();
       }
       
       /*public void focusCombo()
       {
           categories.showPopup();
       }*/
       
       public boolean stopCellEditing()
       {
           //if(DEBUG_LOCAL)
           //{
           //    System.out.println("MetadataEditor MetadataTableEditor - stop cell editing - set previousCurrent to current");
           //}
           previousCurrent = current;
           return super.stopCellEditing();
       } 
        
       public java.awt.Component getTableCellEditorComponent(final JTable table,final Object value,boolean isSelected,final int row,final int col)
       {
           final JTextField field = new JTextField();
           final JComboBox categories = new JComboBox();
           categories.setFont(GUI.LabelFace);
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           final JLabel categoryLabel = new JLabel();
           final JLabel notEditable = new JLabel();
           final JPanel comp = new JPanel();
           
           comp.addMouseListener(new MouseAdapter(){
              public void mousePressed(MouseEvent e)
              {
                  ((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
                  stopCellEditing();
              }
           });
           
           currentRow = row;
           
           notEditable.setFont(GUI.LabelFace);
           field.setFont(GUI.LabelFace);
           
           //if(DEBUG_LOCAL)
           //{
           //    System.out.println("MetadataEditor getTableCellEditorComponent -- about to add item listener -- ");
           //}
           
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
                           //if(DEBUG_LOCAL)
                           //{    
                           //  System.out.println("MetdataEditor edit item selected: " + e);
                           //}
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
           
           categories.addFocusListener(new FocusAdapter(){
               public void focusLost(java.awt.event.FocusEvent fe)
               {      
                   MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
                   if(model.lastSavedRow != row)
                   {    
                     model.setSaved(row,true);
                   }
               }
           });
           
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
                      
                    /*if(!(categories.getSelectedItem() instanceof OntType) || !(ie.getItem() instanceof OntType))
                    {
                        if(DEBUG_LOCAL)
                        {
                            System.out.println("MetadataEditor -- non ontology category type selected " + categories.getSelectedItem().getClass());
                        }
                        return;
                    }*/
                    
                    //if(DEBUG_LOCAL)
                    //{
                    //  OntType item = (OntType)(ie.getItem());//categories.getSelectedItem();  
                        
                    //  System.out.println("MetadataEditor - categories item state changed: " + ie);
                    //  System.out.println("MetadataEditor - category item base - " + item.getBase());
                    //  System.out.println("MetadataEditor - category item label - " + item.getLabel());
                      
                      // //System.out.println("MetadataEditor -- textfield value: " + table.getModel().getValueAt(row,buttonColumn - 1));
                    //}
                    VueMetadataElement vme = new VueMetadataElement();
                    
                    // was temporarily rolled back for search bug
                    String[] keyValuePair = {((OntType)categories.getSelectedItem()).getBase()+"#"+((OntType)categories.getSelectedItem()).getLabel(),
                                               ((VueMetadataElement)table.getModel().getValueAt(row,buttonColumn - 1)).getValue()};
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
           
           field.addKeyListener(new java.awt.event.KeyAdapter(){
              public void keyPressed(java.awt.event.KeyEvent e)
              {
                  if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)
                  {
                      ((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
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
                  
                    if(DEBUG_LOCAL)
                    {
                      Object editor = metadataTable.getCellEditor();
                      
                      MetadataTableEditor mte = null;
                      
                      System.out.println("MetadataEditor -- field focus lost -- CellEditor: " + 
                                           editor);
                      
                      if(editor != null)
                      {
                          mte = (MetadataTableEditor)editor;
                          System.out.println("MetadataEditor -- field focus lost -- CellEditor row: " + 
                                           mte.getRow());
                      }
                      
                      
                    }
                  
                  
                   MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
                   if(model.lastSavedRow != row)
                   {    
                     model.setSaved(row,true);
                   }
                  
                    //((MetadataTableModel)metadataTable.getModel()).setSaved(row,true);
                  //$
                  
                  
                  //tufts.vue.UndoManager undo = tufts.vue.VUE.getActiveMap().getUndoManager();
                  //undo.mark("metadata value");
                  VUE.getActiveMap().markAsModified();
                  
                  //if(DEBUG_LOCAL)
                  //{
                  //  System.out.println("metadata value change marked...");
                  //  System.out.println("MetadataEditor focuslost row -- " + row);
                  //  System.out.println("MetadataEditor focuslost current -- " + current);
                  //  try
                  //  {
                  //    System.out.println("MetadataEditor focuslost opposite component " + fe.getOppositeComponent().getClass() );
                  //  }
                  //  catch(Exception e)
                  //  {
                  //    System.out.println("MetadataEditor debug -- exception in finding focus lost opposite component: " + e);
                  //  }
                  //}
                  
                  if(fe!= null && fe.getOppositeComponent() == categories)
                  {
                      return;
                  }
                  
                  java.util.List<VueMetadataElement> metadata = null;
                  
                  if(previousCurrent == null && current == null)
                  {
                      //if(DEBUG_LOCAL)
                      //{
                      //    System.out.println("MetadataEditor - there was no previous current or current");
                      //    System.out.println("exiting focusLost");
                      //}
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
                  
                  if(current.getMetadataList().getCategoryListSize() > 0)
                   currentVME = metadata.get(row);
                  
                  VueMetadataElement vme = new VueMetadataElement();
                  //System.out.println("MetadataEditor -- value at position where text field focus lost: " +  table.getModel().getValueAt(row,col));
                  
                  if(currentVME==null)
                  {
                               //VueMetadataElement vme = new VueMetadataElement();
                     String[] emptyEntry = {NONE_ONT,field.getText()};
                     vme.setObject(emptyEntry);
                     vme.setType(VueMetadataElement.CATEGORY);  
                      
                      
                    //vme.setObject(field.getText());   
                  }
                  /*else
                  if(!(currentVME.getObject() instanceof String[]))
                  {
                    //vme.setObject(field.getText());
                      
                               //VueMetadataElement vme = new VueMetadataElement();
                     String[] emptyEntry = {NONE_ONT,""};
                     vme.setObject(emptyEntry);
                     vme.setType(VueMetadataElement.CATEGORY);
                  }*/
                  else
                  {
                    String[] obj = (String[])currentVME.getObject();  
                    String[] pairedValue = {obj[0],field.getText()};
                    vme.setObject(pairedValue);
                  }
                  //table.getModel().setValueAt(vme,row,col);
                  if(current.getMetadataList().getCategoryListSize() > (row))
                  {
                    metadata.set(row,vme);
                  }
                  else
                  {
                    metadata.add(vme); 
                  }
                     
                  current.layout();
                  
                  VUE.getActiveViewer().repaint();
                  
                  // only if this is not the currently selected ("editing"?) -- only if not "saved"
                  // (and can't be saved if empty info? just do in getter...)
                  // row....
                  //comp.remove(field);
                  //comp.add(notEditable);
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
               
               /*if(current.getMetadataList().getMetadata().size() <= row)
               {
                   comp.add(categories);
                   return comp;
               }*/

               Object currObject = current.getMetadataList().getMetadata().get(row).getObject();//table.getModel().getValueAt(row,col);
               /*if(!(currObject instanceof String[]))
               {
                   comp.add(categories);
                   return comp;
               }*/
               final Object currValue = /*(edu.tufts.vue.ontology.OntType)*/(((String[])currObject)[0]);
               Object currFieldValue = (((String[])currObject)[1]);
               
               //if(currFieldValue.toString().length() != 0
               //   && !((MetadataTableModel)table.getModel()).getClickedTwice(row))
               if(false) /// with new isCellEditable() logic on JTable...
               {
                   String displayString = currValue.toString();
                   int nameIndex = displayString.indexOf("#");
                   if(nameIndex != -1 && ( (nameIndex + 1)< displayString.length()) ) 
                   {
                     displayString = displayString.substring(nameIndex + 1);
                   }
                   categoryLabel.setText(displayString);
                   
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
                   
                                               //comp.setBorder(getMetadataCellBorder(row,col));
                            //comp.setBorder(getMetadataCellBorder(row,col));
                            ((MetadataTableModel)table.getModel()).refresh();
                            metadataTable.repaint();
                   
                   return comp;
               }
               findCategory(currValue,row,col,n,categories); 
               

               comp.add(categories);
               
               comp.setBorder(getMetadataCellBorder(row,col));
               //comp.setBorder(getMetadataCellBorder(row,col));
               ((MetadataTableModel)table.getModel()).refresh();
               metadataTable.repaint();
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
               //if(((MetadataTableModel)table.getModel()).getClickedTwice(row) || value.toString().equals(""))
               if(true) // new isCellEditable stuff
               {
                 comp.add(field);
               }
               else
               {
                 //final JLabel notEditable = new JLabel(field.getText() + "test");  
                 notEditable.setText(field.getText());
                 comp.add(notEditable);
                 /*notEditable.addMouseListener(new MouseAdapter(){
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
                            //((MetadataTableModel)table.getModel()).setClickedTwice(row);
                            ((MetadataTableModel)table.getModel()).setSaved(row,false);
                            ((MetadataTableModel)table.getModel()).refresh();
                            metadataTable.repaint();
                        }
                        //if(me.getClickCount() == 1)
                        //{
                        //    ((MetadataTableModel)table.getModel()).setClickedOnce(row);
                        //}
                        
                        comp.setBorder(getMetadataCellBorder(row,col));
                        ((MetadataTableModel)table.getModel()).refresh();
                        //metadataTable.repaint();
                    }
                 }); */ // mouse listener -- replacing with isCellEditable perhaps..
               }
           }
           else if(col ==  buttonColumn)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
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
           
           //field.setFont(GUI.LabelFace);
           
           comp.setBorder(getMetadataCellBorder(row,col));
           return comp;
       }
       
    }
    
    public Border getMetadataCellBorder(int row,int col)
    {

      
      if(col == buttonColumn)
          return insetBorder;
      
      MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
      
      boolean saved = model.getIsSaved(row);
      
      if(saved)
      {
          boolean aboveSaved = false;
          
          if(row > 0)
              aboveSaved = model.getIsSaved(row - 1);
          else if(row == -1)
              aboveSaved = false;
          
          boolean belowSaved = false;
          
          if(row < model.getRowCount()/* && row > 0 */)
            belowSaved = model.getIsSaved(row + 1);
          else if(row == model.getRowCount() - 1 && row > 0)
            belowSaved = false;
          
          if(col == buttonColumn -1 )
          {    
            if(!aboveSaved && !belowSaved)
            {    
              return BorderFactory.createCompoundBorder(fullBox,insetBorder);
            }
            else if(!aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(verticalFollowingBox,insetBorder);
            }
            else if(aboveSaved && !belowSaved)
            {
              return BorderFactory.createCompoundBorder(fullBox,insetBorder);
            }
            else if(aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(verticalFollowingBox,insetBorder);
            }
          }
          else
          {
            if(!aboveSaved && !belowSaved)
            {    
              return BorderFactory.createCompoundBorder(leftLeadingFullBox,insetBorder);
            }
            else if(!aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(leftLeadingVerticalFollowingBox,insetBorder);
            }
            else if(aboveSaved && !belowSaved)
            {
              return BorderFactory.createCompoundBorder(leftLeadingFullBox,insetBorder);
            }
            else if(aboveSaved && belowSaved)
            {
              return BorderFactory.createCompoundBorder(leftLeadingVerticalFollowingBox,insetBorder);
            }
          }    
      }
      
      return insetBorder;
      
     }
    
    /**
     *
     * watch out for current == null
     *
     **/
    class MetadataTableModel extends AbstractTableModel
    {
         //default:
         private int cols = 2;
         
         private java.util.ArrayList<Boolean> categoryIncluded = new java.util.ArrayList<Boolean>();
         private java.util.ArrayList<Boolean> saved = new java.util.ArrayList<Boolean>();

         private int lastSavedRow = -1;
         
         public void clearGUIInfo()
         {
             // actually if current!=null should probably fill saved with TRUES up 
             // to row count
             saved = new java.util.ArrayList<Boolean>();
             
             for(int i=0;i<getRowCount();i++)
             {
                 saved.add(Boolean.TRUE);
             }
             
             lastSavedRow = -1;
         }
         
         public boolean getIsSaved(int row)
         {
            try
            {  
              return Boolean.parseBoolean(saved.get(row).toString()) && 
                       ((VueMetadataElement)getValueAt(row,buttonColumn -1)).getValue().length() != 0;
            }
            catch(Exception e)
            {
              return Boolean.FALSE;
            }
         }
         
         public void setSaved(int row,boolean isSaved)
         {
             
             if(row == -1)
                 return;
             
             if(saved.size() <= row)
             {

                 //saved.ensureCapacity(row + 1);
                 //for(int i = 0;i<row-saved.size() + 1;i++)
                 for(int i = 0;i<row + 1;i++)
                 {
                     saved.add(Boolean.TRUE);
                 }
             } 
            

            saved.set(row,Boolean.valueOf(isSaved));
            lastSavedRow = row;
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
             
             categoryIncluded.trimToSize();
             
             if(current == null)
                 return "null";
             
             java.util.List<VueMetadataElement> list = current.getMetadataList().getMetadata();
             while(list.size() < row + 1)
               addNewRow();
             
             return current.getMetadataList().getCategoryListElement(row);
             //return current.getMetadataList().getMetadata().get(row);
         }
         
         public void setValueAt(Object value,int row, int column)
         {
             //fireTableDataChanged(); 
         }
         
         public void refresh()
         {
             fireTableDataChanged();
         }
         
    }
    
}
