
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

import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.ontology.OntType;
import edu.tufts.vue.rdf.RDFIndex;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import tufts.vue.*;

/*
 * MetadataEditor.java
 *
 * Created on June 29, 2007, 2:37 PM
 *
 * @author dhelle01
 */
public class MetadataEditor extends JPanel implements ActiveListener,MetadataList.MetadataListListener {
    
    private static final boolean DEBUG_LOCAL = true;
    
    // for best results: modify next two in tandem (at exchange rate of one pirxl from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    public final static int ROW_HEIGHT = 39;
    public final static int ROW_GAP = 7;
    
    public final static int ROW_INSET = 5;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    public final static boolean LIMITED_FOCUS = false;
    
    public final static String TAG_ONT = "http://vue.tufts.edu/vue.rdfs#Tag";
    
    private JTable metadataTable;
    private JScrollPane scroll;
    private tufts.vue.LWComponent current;
    private tufts.vue.LWComponent previousCurrent;
    
    private JList ontologyTypeList;
    
    private int buttonColumn = 1;
    
    private boolean showOntologicalMembership;
    
    private boolean ontologicalMembershipVisible;
    
    private JPanel ontologicalMembershipPane;
    
    private boolean focusToggle = false;
    
    public MetadataEditor(tufts.vue.LWComponent current,boolean showOntologicalMembership,boolean followAllActive)
    {
        this.current = current;
        
        if(DEBUG_LOCAL)
        {
            System.out.println("MetadataEditor - just created new instance for (current,followActive) (" + current +"," + followAllActive + ")");
        }
        
        metadataTable = new JTable(new MetadataTableModel());
       // metadataTable.setGridColor(new java.awt.Color(255,255,255,0));
        metadataTable.setGridColor(new java.awt.Color(getBackground().getRed(),getBackground().getBlue(),getBackground().getGreen(),0));
        //metadataTable.setGridColor(getBackground());
        //metadataTable.setOpaque(false);
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
                         VueMetadataElement vme = new VueMetadataElement();
                         String[] emptyEntry = {TAG_ONT,""};
                         vme.setObject(emptyEntry);
                         vme.setType(VueMetadataElement.CATEGORY);
                         //metadataTable.getModel().setValueAt(vme,metadataTable.getRowCount()+1,0);
                         MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
                         ((MetadataTableModel)metadataTable.getModel()).refresh();
                         
                         SwingUtilities.invokeLater(new Runnable(){
                            public void run()
                            {
                              scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());                              
                            }
                         });
                         
                         //MetadataEditor.this.getRootPane().repaint();
                         //MetadataEditor.this.validateTree();
                         //MetadataEditor.this.repaint();
                       }
                   }
       });
       metadataTable.addMouseListener(new java.awt.event.MouseAdapter()
               {
                   public void mouseReleased(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         java.util.List<VueMetadataElement> metadataList = MetadataEditor.this.current.getMetadataList().getMetadata();
                         int selectedRow = metadataTable.getSelectedRow();
                         if(metadataTable.getSelectedColumn()==buttonColumn && metadataList.size() > selectedRow)
                         {
                            metadataList.remove(selectedRow);
                            metadataTable.repaint();
                            requestFocusInWindow();
                         }
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
        tablePanel.add(metadataTable.getTableHeader(),BorderLayout.NORTH);
        tablePanel.add(metadataTable);
        metaPanel.add(tablePanel);
        
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        final JLabel optionsLabel = new JLabel("more options");
        optionsLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        final JButton advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));//tufts.vue.gui.VueButton("advancedSearchMore");
        advancedSearch.setBorder(BorderFactory.createEmptyBorder());
        advancedSearch.addActionListener(new java.awt.event.ActionListener(){
           public void actionPerformed(java.awt.event.ActionEvent e)
           {
               MetadataTableModel model = (MetadataTableModel)metadataTable.getModel();
               if(model.getColumnCount() == 2)
               {
                 buttonColumn = 2;
                 model.setColumns(3);
                 advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw")));
                 optionsLabel.setText("less options");
               }
               else
               {
                 buttonColumn = 1;
                 model.setColumns(2);
                 advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));
                 optionsLabel.setText("more options");
               }
               
               adjustColumnModel();

               //System.out.println("table model set to 3 columns");
               revalidate();
           }
        });
        optionsPanel.add(advancedSearch);
        optionsPanel.add(optionsLabel);
        metaPanel.add(optionsPanel,BorderLayout.SOUTH);
        
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
        membershipLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        ontologicalMembershipPane.add(membershipLabel,BorderLayout.NORTH);
        //ontologicalMembershipPane.add(ontologyListScroll);
        ontologicalMembershipPane.add(ontologyTypeList);
        //ontologyListScroll.setAlignmentX(0.0f);
        //membershipLabel.setAlignmentX(0.0f);
        
        add(metaPanel,BorderLayout.NORTH);
        
        if(showOntologicalMembership && current !=null && current.getMetadataList().getOntologyListSize() > 0)
        {
          add(ontologicalMembershipPane/*,BorderLayout.SOUTH*/);
          ontologicalMembershipVisible = true;
        }
        
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
        
        setBorder(BorderFactory.createEmptyBorder(10,8,0,6));
        scroll.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        validate();
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
           String[] emptyEntry = {TAG_ONT,""};
           vme.setObject(emptyEntry);
           vme.setType(VueMetadataElement.CATEGORY);

           MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
         }
        
         if(current != null && current.getMetadataList().getOntologyListSize() > 0)
         {
             if(!ontologicalMembershipVisible)
             {
                add(ontologicalMembershipPane);
                ontologicalMembershipVisible = true;
             }
         }
         else
         {
             if(ontologicalMembershipVisible)
             {
                remove(ontologicalMembershipPane);
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
               comp =  new JLabel("Keywords:");
               comp.setFont(tufts.vue.gui.GUI.LabelFace);
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
    
    
    public void findCategory(Object currValue,int row,int col,int n,JComboBox categories)
    {
    
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
                   }
                   
               }
               
    }
    
    class MetadataTableRenderer extends DefaultTableCellRenderer
    {   
        
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JPanel comp = new JPanel();
           JComboBox categories = new JComboBox();
           categories.setModel(new CategoryComboBoxModel());
           categories.setRenderer(new CategoryComboBoxRenderer());
           
           comp.setLayout(new java.awt.BorderLayout());
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
               findCategory(currValue,row,col,n,categories); 
              
               comp.add(categories); 
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
                   if(vme.getType() == VueMetadataElement.CATEGORY)
                   {
                      comp.add(new JTextField(vme.getValue()));
                   }
               }
               else
                 comp.add(new JTextField(value.toString()));
           }
           else if(col == buttonColumn)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           
           //comp.setOpaque(true);
           //comp.setBackground(java.awt.Color.BLUE);
           
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
                           ecd.add(new CategoryEditor());
                           ecd.setBounds(475,300,300,250);
                           //ecd.pack();
                           ecd.setVisible(true);
                           
                           //findCategory(currValue,row,col,n,categories)
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
                  
                  if(DEBUG_LOCAL)
                  {
                    System.out.println("MetadataEditor focuslost row -- " + row);
                    System.out.println("MetadataEditor focuslost current -- " + current);
                    System.out.println("MetadataEditor focuslost opposite component " + fe.getOppositeComponent().getClass() );
                  }
                  
                  if(fe.getOppositeComponent() == categories)
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
                     String[] emptyEntry = {TAG_ONT,""};
                     vme.setObject(emptyEntry);
                     vme.setType(VueMetadataElement.CATEGORY);  
                      
                      
                    //vme.setObject(field.getText());   
                  }
                  else
                  if(!(currentVME.getObject() instanceof String[]))
                  {
                    //vme.setObject(field.getText());
                      
                               //VueMetadataElement vme = new VueMetadataElement();
                     String[] emptyEntry = {TAG_ONT,""};
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


              }
              
              public void focusGained(java.awt.event.FocusEvent fe)
              {
                  focusToggle = true;
              }
           });
           final JPanel comp = new JPanel();
           comp.setLayout(new java.awt.BorderLayout());
           if(col == buttonColumn - 2)
           {
               //categories.setSelectedIndex(1);
               //int loc = 0;
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
               comp.add(field);
           }
           else if(col ==  buttonColumn)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           
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
                  String[] emptyEntry = {TAG_ONT,""};
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
                  String[] emptyEntry = {TAG_ONT,""};
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
