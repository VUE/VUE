
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

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import tufts.vue.VueResources;
import tufts.vue.gui.GUI;
import edu.tufts.vue.metadata.CategoryModel;
import edu.tufts.vue.ontology.OntType;
import edu.tufts.vue.ontology.Ontology;

//import tufts.vue.gui.DockWindow;

/*
 * CategoryEditor.java
 *
 * Created on August 17, 2007, 2:05 PM
 *
 * @author dhelle01
 */
public class CategoryEditor extends JPanel 
{
    private static final boolean DEBUG_LOCAL = false;
    
    // for best results: modify next two in tandem (at exchange rate of one pirxl from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    public final static int ROW_HEIGHT = 39;
    public final static int ROW_GAP = 7;
    
    public final static int ROW_INSET = 0;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    //private JTable metadataSetTable;
    private JTable customCategoryTable;
    
    //private JPanel setPanel;
    private JPanel customPanel;
    
    private JPanel buttonPanel;
    private JButton cancelButton;
    private JButton doneButton;
    
    private boolean newCategoryRequested;
    
    private JDialog dialog;
    
    public CategoryEditor(JDialog dialog,final JComboBox categories,
                          final MetadataEditor metadataEditor,
                          final tufts.vue.LWComponent current,
                          final int row,
                          final int col) 
    {
        this.dialog = dialog;
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        //setPanel = new JPanel();
        //metadataSetTable = new JTable(new FullMetadataSetTableModel());
        //metadataSetTable.setDefaultRenderer(java.lang.Object.class,new SetTableRenderer());
        //metadataSetTable.setDefaultEditor(java.lang.Object.class,new SetTableEditor());
        //setPanel.add(metadataSetTable);
        customPanel = new JPanel(new java.awt.BorderLayout());
        customCategoryTable = new JTable(new MetadataCategoryTableModel());
        customCategoryTable.setOpaque(false);
        customCategoryTable.setDefaultRenderer(java.lang.Object.class,new CustomCategoryTableRenderer());
        customCategoryTable.setDefaultEditor(java.lang.Object.class,new CustomCategoryTableEditor());
        ((DefaultCellEditor)customCategoryTable.getDefaultEditor(java.lang.Object.class)).setClickCountToStart(1);
        customCategoryTable.getColumnModel().getColumn(0).setHeaderRenderer(new CustomCategoryTableHeaderRenderer());
        customCategoryTable.getColumnModel().getColumn(1).setHeaderRenderer(new CustomCategoryTableHeaderRenderer());
        int editorWidth = getWidth();
        customCategoryTable.getColumnModel().getColumn(0).setMinWidth(editorWidth-BUTTON_COL_WIDTH);
        customCategoryTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH); 
        
        customCategoryTable.setRowHeight(ROW_HEIGHT);
        customCategoryTable.getTableHeader().setReorderingAllowed(false);
        
        //metadataSetTable.setRowHeight(ROW_HEIGHT);
        //metadataSetTable.getTableHeader().setReorderingAllowed(false);

        customCategoryTable.setGridColor(new java.awt.Color(getBackground().getRed(),getBackground().getBlue(),getBackground().getGreen(),0));
        customCategoryTable.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        customCategoryTable.setIntercellSpacing(new java.awt.Dimension(0,0));
        final JScrollPane scroll = new JScrollPane(customCategoryTable);
        scroll.setBackground(getBackground());
        scroll.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        scroll.getViewport().setOpaque(false);

        customCategoryTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
        {
                   public void mousePressed(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>customCategoryTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                           
                         newCategoryRequested = true;
                         
                         /*if(customCategoryTable.getCellEditor() != null)
                         {
                             customCategoryTable.getCellEditor().stopCellEditing();
                             //scroll.requestFocus();
                             CategoryEditor.this.requestFocus(false);
                         }*/
                         
                         ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();

                         SwingUtilities.invokeLater(new Runnable(){
                            public void run()
                            {
                              scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());                              
                            }
                         });
                       }
                   }
                   
                   
                   public void mouseEntered(java.awt.event.MouseEvent e)
                   {
                       // todo -- use mouse moved to make it any move near the add button.. 
                       if((customCategoryTable.getCellEditor() != null)/* && (e.getX()>customCategoryTable.getWidth()-BUTTON_COL_WIDTH)*/) 
                       {
                          customCategoryTable.getCellEditor().stopCellEditing(); 
                       }
                   }
       });
       
       /*customCategoryTable.addMouseListener(new java.awt.event.MouseAdapter()
               {
                   public void mouseReleased(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>customCategoryTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
                         java.util.List<OntType> ontTypes = vueCategoryModel.getCustomOntology().getOntTypes();
                         int selectedRow = customCategoryTable.getSelectedRow();
                         if(customCategoryTable.getSelectedColumn()== 1 && ontTypes.size() > selectedRow)
                         {
                            ontTypes.remove(selectedRow);
                            customCategoryTable.repaint();
                            requestFocusInWindow();
                            tufts.vue.VUE.getCategoryModel().saveCustomOntology();
                         }
                       }
                   }
        });*/ 


        
        //customPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));

        customPanel.add(scroll);
        //add(setPanel);
        // todo: add separator
        add(customPanel);

        //if(DEBUG_LOCAL)
        //{    
        //  System.out.println("scroll: " + scroll);
        //  System.out.println("categoryTable: " + customCategoryTable);
        //}
        
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,15));
        cancelButton = new JButton(VueResources.getString("button.cancel.lable"));
        cancelButton.setFont(GUI.LabelFace);
        doneButton = new JButton(VueResources.getString("button.done.lable"));
        doneButton.setFont(GUI.LabelFace);
        //buttonPanel.add(cancelButton);
        buttonPanel.add(doneButton);
        add(buttonPanel);
        
        doneButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                //categories.setSelectedItem(categories.getSelectedItem());
                
                ((CategoryComboBoxModel)categories.getModel()).refresh();
                int n = categories.getModel().getSize();
                Object currObject = null;
                
                tufts.vue.LWComponent currComponent = null;
                
                if(metadataEditor.getCurrentMultiples() !=null )
                {
                    currComponent = metadataEditor.getCurrentMultiples();
                }
                else if(metadataEditor.getCurrent() !=null )
                {
                    currComponent = metadataEditor.getCurrent();
                }
                else
                {
                    
                    CategoryEditor.this.dialog.dispose();
                    return;
                }
                        
                currObject = currComponent.getMetadataList().getMetadata().get(row).getObject();
                Object currValue = (((String[])currObject)[0]);
                metadataEditor.findCategory(currValue,row,col,n,categories);
                categories.repaint();
                
                CategoryEditor.this.dialog.dispose();
            }
        });
        
    }
    
    class CustomCategoryTableHeaderRenderer extends DefaultTableCellRenderer
    {   
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           //setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
           JComponent comp = new JPanel();
           if(col == 0)
           {    
               comp =  new JLabel(VueResources.getString("addcustomcategory.label"));
               comp.setFont(tufts.vue.gui.GUI.LabelFace);
           }
           else if(col == 1)
           {
               //comp = new JLabel();
               //((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
               //comp.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
               JPanel holder = new JPanel();
               holder.add(new tufts.vue.gui.VueButton("keywords.button.add"));
               comp.add(holder);
           }
           else
           {
               comp = new JLabel("");
           }

           comp.setOpaque(true);
           comp.setBackground(CategoryEditor.this.getBackground());
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));

           return comp;
       }
    }

    
    /*class SetTableRenderer extends DefaultTableCellRenderer
    {
        JPanel checkPanel = new JPanel();
        JCheckBox check = new JCheckBox();
        JLabel label = new JLabel();
        
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,
                                                                boolean hasFocus,int row,int col)
        {
            if(col == 0)
            {
                checkPanel.add(check);
                return checkPanel;
            }
            if(col == 1)
            {
                
                if(DEBUG_LOCAL)
                {    
                  System.out.println("CategoryEditor: " + value.getClass());
                }
                
                if(value instanceof Ontology)
                {
                    label.setText(((Ontology)value).getLabel());
                }

                return label;
            }
            
            label.setText("ERROR");
            return label;
        }
    } */
    
    
    /*class SetTableEditor extends DefaultCellEditor
    {
        JPanel checkPanel = new JPanel();
        JCheckBox check = new JCheckBox();
        JLabel label = new JLabel();
        
        public SetTableEditor()
        {
            super(new JTextField());
            //label = (JTextField)getComponent();
        }
        
        public java.awt.Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,
                                                                int row,int col)
        {
            if(col == 0)
            {
                checkPanel.add(check);
                return checkPanel;
            }
            if(col == 1)
            {
                
                if(DEBUG_LOCAL)
                {    
                  System.out.println("CategoryEditor: " + value.getClass());
                }
                
                if(value instanceof Ontology)
                {
                    label.setText(((Ontology)value).getLabel());
                }

                return label;
            }
            
            label.setText("ERROR");
            return label;
        }
    }*/
    
    class CustomCategoryTableRenderer extends DefaultTableCellRenderer
    {
        JPanel checkPanel = new JPanel();
        //JCheckBox check = new JCheckBox();
        JTextField label = new JTextField();
        tufts.vue.gui.VueButton deleteButton = new tufts.vue.gui.VueButton("keywords.button.delete");
        JPanel deletePanel = new JPanel();
        
        public CustomCategoryTableRenderer()
        {
            deletePanel.setBorder(BorderFactory.createEmptyBorder(5,1,0,0));
            deletePanel.add(deleteButton);
            label.setFont(tufts.vue.gui.GUI.LabelFace);
        }
        
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,
                                                                boolean hasFocus,int row,int col)
        {
            JPanel comp = new JPanel(new java.awt.BorderLayout());
            
            if(col == 1)
            {
                //JLabel button = new JLabel();
                //button.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
                //comp.add(button);
                
                //return checkPanel;
                
                /*if(DEBUG_LOCAL)
                {
                    System.out.println("CE: in table renderer -- showing delete button: " + row);
                }*/
                
                return deletePanel;
            }
            if(col == 0)
            {
                //label.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
                
                //if(DEBUG_LOCAL)
                //{    
                //  System.out.println("CategoryEditor: " + value.getClass() + " row: " + row + " col: " + col);
                //}
                
                if(value instanceof OntType)
                {
                    label.setText(((OntType)value).getLabel());
                    //if(DEBUG_LOCAL)
                    //{    
                    //  System.out.println("Label text in category component: " + label.getText());
                    //}
                }
                    
                else
                {
                  label.setText(value.toString());
                }
                comp.add(label);
                //return label;
            }
            
            comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
            //label.setText("ERROR");
            return comp;
        }
    }
    
    
    class CustomCategoryTableEditor extends DefaultCellEditor
    {
        //JPanel checkPanel = new JPanel();
        //JPanel deletePanel = new JPanel();
        //JCheckBox check = new JCheckBox();
        JTextField label = new JTextField();
        
        //private int row;
        //private int col;
        
        public CustomCategoryTableEditor()
        {
            super(new JTextField());
            label.setFont(GUI.LabelFace);
            label = (JTextField)getComponent();
            
            /*label.addFocusListener(new java.awt.event.FocusAdapter(){
                   public void focusLost(java.awt.event.FocusEvent fe)
                   {
                       if( (row == customCategoryTable.getModel().getRowCount() - 1) && newCategoryRequested )
                       {
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           cats.addCustomCategory(label.getText());
                           
                           ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();
                           
                           cats.saveCustomOntology();
                           
                           newCategoryRequested = false;
                       }
                       else
                       {
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           cats.addCustomCategory(label.getText(),cats.getCustomOntology().getOntTypes().get(row));
                           
                           ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();
                           
                           cats.saveCustomOntology(); 
                       }
                   }
                }); */
        }
        
        public java.awt.Component getTableCellEditorComponent(final JTable table,Object value,boolean isSelected,
                                                                final int row,int col)
        {
            JPanel comp = new JPanel(new java.awt.BorderLayout());
            
            if(col == 1)
            {
                //if(DEBUG_LOCAL)
                //{
                //  System.out.println("CE - creating delete button editor " + row);    
                //}
                
                //checkPanel.add(check);
                //comp.add(check);
                //return checkPanel;
                JPanel deletePanel = new JPanel();
                deletePanel.setBorder(BorderFactory.createEmptyBorder(5,1,0,0));
                tufts.vue.gui.VueButton deleteButton = new tufts.vue.gui.VueButton("keywords.button.delete");
                
                       
                deleteButton.addMouseListener(new java.awt.event.MouseAdapter()
                {
                   public void mouseReleased(java.awt.event.MouseEvent evt)
                   {
                       //if(evt.getX()>customCategoryTable.getWidth()-BUTTON_COL_WIDTH)
                       //{
                         CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
                         java.util.List<OntType> ontTypes = vueCategoryModel.getCustomOntology().getOntTypes();
                         //int selectedRow = customCategoryTable.getSelectedRow();
                         //if(customCategoryTable.getSelectedColumn()== 1 && ontTypes.size() > selectedRow)
                         //{
                            if(ontTypes.size() > row)
                              ontTypes.remove(row);
                            //customCategoryTable.repaint();
                            //requestFocusInWindow();
                            tufts.vue.VUE.getCategoryModel().saveCustomOntology();
                            
                            ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();
                            customCategoryTable.repaint();
                            requestFocusInWindow();
                         //}
                       //}
                   }
                }); 
                
                deletePanel.add(deleteButton);
                return deletePanel;
            }
            if(col == 0)
            {
                final JTextField label = new JTextField();
                label.setFont(GUI.LabelFace);
                //this.row = row;
                //this.col = col;
                
                //if(DEBUG_LOCAL)
                //{    
                //  System.out.println("CategoryEditor: " + value.getClass());
                //}
                if(value instanceof OntType)
                {
                    label.setText(((OntType)value).getLabel());
                }
                
                /*label.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseExited(java.awt.event.MouseEvent e)
                    {
                       if(customCategoryTable.getCellEditor() != null) 
                       {
                          customCategoryTable.getCellEditor().stopCellEditing(); 
                       }
                    }
                });*/
                
                label.addFocusListener(new java.awt.event.FocusAdapter(){
                   public void focusLost(java.awt.event.FocusEvent fe)
                   {
                       if(DEBUG_LOCAL)
                       {
                         System.out.println("CE: label focus lost -- " + row);    
                       }
                       
                       if( (row == table.getModel().getRowCount() - 1) && newCategoryRequested )
                       {
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           cats.addCustomCategory(label.getText());
                           
                           ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();
                           
                           cats.saveCustomOntology();
                           
                           newCategoryRequested = false;
                       }
                       else
                       {
                           CategoryModel cats = tufts.vue.VUE.getCategoryModel();
                           cats.addCustomCategory(label.getText(),cats.getCustomOntology().getOntTypes().get(row));
                           
                           ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();
                           
                           cats.saveCustomOntology(); 
                       }
                   }
                });

                comp.add(label);
                //return label;
            }

            comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
            return comp;
            //label.setText("ERROR");
            //return label;
        }
    }
    
    /*class FullMetadataSetTableModel extends AbstractTableModel
    {
        List<Boolean> selections = new ArrayList<Boolean>();
        private CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
        
        public int getRowCount()
        {
            return vueCategoryModel.size();
        }
        
        
         
        //  first column contains checkbox that will remove or add ontologies
        //  second column contains ontology names   
        public int getColumnCount()
        {
            return 2;
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == 1)
            {
              return vueCategoryModel.get(row);
            }
            else
            //if(col == 0)
            {
              //return selections.get(row);
              return Boolean.TRUE;
            }

        }
    }*/
    
    class MetadataCategoryTableModel extends AbstractTableModel
    {
        
        private CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
        
        /**
         *
         * column 1: category, which is an OntType and will appear
         * as a simple string
         *
         * column 2: remove button (should change CategoryModel)
         *
         **/
        public int getColumnCount()
        {
            return 2;
        }
        
        public boolean isCellEditable(int row,int col)
        {
            return true;
            /*if(col == 0)
                return true;
            else
                return false; */
        }
        
        public int getRowCount()
        {          
            Ontology customOntology = vueCategoryModel.getCustomOntology();
            
            if(customOntology == null || vueCategoryModel.getCustomOntology().getOntTypes().size() ==0)
            {
                newCategoryRequested = true;
                return 1;
            }
            

            if(/*(customOntology !=null) &&*/ !newCategoryRequested)
              return vueCategoryModel.getCustomOntology().getOntTypes().size();
            else if(customOntology != null)
              return vueCategoryModel.getCustomOntology().getOntTypes().size() + 1;
            else
              return 0;
            // vueCategoryModel.getCustomCategories.getSize();
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == 0 && vueCategoryModel.getCustomOntology() == null)
            {
                return "";
            }
            if(col == 0 && row < vueCategoryModel.getCustomOntology().getOntTypes().size())
            {
              OntType category = vueCategoryModel.getCustomOntology().getOntTypes().get(row);
              return category;
            }
            else
            if(col == 0 && row == vueCategoryModel.getCustomOntology().getOntTypes().size())
            {
                return "";
            }
            else
            //if(row == 1)
            {
              return "remove";
            }
        }

        public void refresh()
        {
           fireTableDataChanged();
        }
    }
    
}
