
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

import edu.tufts.vue.metadata.*;
import edu.tufts.vue.ontology.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

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
    
    // for best results: modify next two in tandem (at exchange rate of one pirxl from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    public final static int ROW_HEIGHT = 39;
    public final static int ROW_GAP = 7;
    
    public final static int ROW_INSET = 5;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    private JTable metadataSetTable;
    private JTable customCategoryTable;
    
    private JPanel setPanel;
    private JPanel customPanel;
    
    private boolean newCategoryRequested;
    
    public CategoryEditor() 
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setPanel = new JPanel();
        metadataSetTable = new JTable(new FullMetadataSetTableModel());
        metadataSetTable.setDefaultRenderer(java.lang.Object.class,new SetTableRenderer());
        metadataSetTable.setDefaultEditor(java.lang.Object.class,new SetTableEditor());
        setPanel.add(metadataSetTable);
        //setPanel.add(new JScrollPane(metadataSetTable));
        customPanel = new JPanel();
        customCategoryTable = new JTable(new MetadataCategoryTableModel());
        customCategoryTable.setDefaultRenderer(java.lang.Object.class,new CustomCategoryTableRenderer());
        customCategoryTable.setDefaultEditor(java.lang.Object.class,new CustomCategoryTableEditor());
        customCategoryTable.getColumnModel().getColumn(0).setHeaderRenderer(new CustomCategoryTableHeaderRenderer());
        customCategoryTable.getColumnModel().getColumn(1).setHeaderRenderer(new CustomCategoryTableHeaderRenderer());
        int editorWidth = getWidth();
        customCategoryTable.getColumnModel().getColumn(0).setMinWidth(editorWidth-BUTTON_COL_WIDTH);
        customCategoryTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);  
        
        final JScrollPane scroll = new JScrollPane(customCategoryTable);
        
        customCategoryTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
        {
                   public void mousePressed(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>customCategoryTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                           
                         newCategoryRequested = true;
                         ((MetadataCategoryTableModel)customCategoryTable.getModel()).refresh();

                         SwingUtilities.invokeLater(new Runnable(){
                            public void run()
                            {
                              scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());                              
                            }
                         });
                       }
                   }
       });

        
        customPanel.add(new JScrollPane(scroll));
        add(setPanel);
        // add separator
        add(customPanel);
    }
    
    class CustomCategoryTableHeaderRenderer extends DefaultTableCellRenderer
    {   
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JComponent comp = new JPanel();
           if(col == 0)
               comp =  new JLabel("Categories:");
           else if(col == 1)
           {
               comp = new JLabel();
               ((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
           }
           else
           {
               comp = new JLabel("");
           }

           comp.setOpaque(true);
           comp.setBackground(CategoryEditor.this.getBackground());
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));

           //System.out.println("MetadataEditor - Table Header Renderer background color: " + MetadataEditor.this.getBackground());
           return comp;
       }
    }

    
    class SetTableRenderer extends DefaultTableCellRenderer
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
                
                System.out.println("CategoryEditor: " + value.getClass());
                
                if(value instanceof Ontology)
                {
                    label.setText(((Ontology)value).getLabel());
                }

                return label;
            }
            
            label.setText("ERROR");
            return label;
        }
    }
    
    
    class SetTableEditor extends DefaultCellEditor
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
                
                System.out.println("CategoryEditor: " + value.getClass());
                
                if(value instanceof Ontology)
                {
                    label.setText(((Ontology)value).getLabel());
                }

                return label;
            }
            
            label.setText("ERROR");
            return label;
        }
    }
    
    class CustomCategoryTableRenderer extends DefaultTableCellRenderer
    {
        JPanel checkPanel = new JPanel();
        JCheckBox check = new JCheckBox();
        JTextField label = new JTextField();
        
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,
                                                                boolean hasFocus,int row,int col)
        {
            if(col == 1)
            {
                checkPanel.add(check);
                return checkPanel;
            }
            if(col == 0)
            {
                
                System.out.println("CategoryEditor: " + value.getClass());
                
                if(value instanceof OntType)
                {
                    label.setText(((OntType)value).getLabel());
                    System.out.println("Label text in category component: " + label.getText());
                }
                else
                {
                  label.setText(value.toString());
                }
                return label;
            }
            
            label.setText("ERROR");
            return label;
        }
    }
    
    
    class CustomCategoryTableEditor extends DefaultCellEditor
    {
        JPanel checkPanel = new JPanel();
        JCheckBox check = new JCheckBox();
        JTextField label = new JTextField();
        
        public CustomCategoryTableEditor()
        {
            super(new JTextField());
            label = (JTextField)getComponent();
        }
        
        public java.awt.Component getTableCellEditorComponent(final JTable table,Object value,boolean isSelected,
                                                                final int row,int col)
        {
            if(col == 1)
            {
                checkPanel.add(check);
                return checkPanel;
            }
            if(col == 0)
            {
                label = new JTextField();
                
                System.out.println("CategoryEditor: " + value.getClass());
                
                if(value instanceof OntType)
                {
                    label.setText(((OntType)value).getLabel());
                }
                
                label.addFocusListener(new java.awt.event.FocusAdapter(){
                   public void focusLost(java.awt.event.FocusEvent fe)
                   {
                       if( (row == table.getModel().getRowCount() - 1) && newCategoryRequested )
                       {
                           tufts.vue.VUE.getCategoryModel().addCustomCategory(label.getText());
                       }
                   }
                });

                return label;
            }
            
            label.setText("ERROR");
            return label;
        }
    }
    
    class FullMetadataSetTableModel extends AbstractTableModel
    {
        List<Boolean> selections = new ArrayList<Boolean>();
        private CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
        
        public int getRowCount()
        {
            return vueCategoryModel.size();
        }
        
        /**
         *
         * first column contains checkbox that will remove or add ontologies
         * second column contains ontology names 
         *
         **/
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
    }
    
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
            if(col == 0)
                return true;
            else
                return false;
        }
        
        public int getRowCount()
        {
            if(!newCategoryRequested)
              return vueCategoryModel.getCustomOntology().getOntTypes().size();
            else
              return vueCategoryModel.getCustomOntology().getOntTypes().size() + 1;
            // vueCategoryModel.getCustomCategories.getSize();
        }
        
        public Object getValueAt(int row,int col)
        {
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