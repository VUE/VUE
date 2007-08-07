
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
import java.awt.event.FocusAdapter;
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
public class MetadataEditor extends JPanel implements ActiveListener {
    
    // for best results: modify next two in tandem (at exchange rate of one pirxl from ROW_GAP for 
    // each two in ROW_HEIGHT in order to maintain proper text box height
    public final static int ROW_HEIGHT = 39;
    public final static int ROW_GAP = 7;
    
    public final static int ROW_INSET = 5;
    
    public final static int BUTTON_COL_WIDTH = 35;
    
    private JTable metadataTable;
    private JScrollPane scroll;
    private tufts.vue.LWComponent current;
    
    public MetadataEditor(tufts.vue.LWComponent current)
    {
        this.current = current;
        
        metadataTable = new JTable(new MetadataTableModel());
        metadataTable.setGridColor(new java.awt.Color(255,255,255,0));
        metadataTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
        {
                   public void mousePressed(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         //System.out.println("metadata: mouse pressed" + evt);
                         VueMetadataElement vme = new VueMetadataElement();
                         vme.setObject("");
                         metadataTable.getModel().setValueAt(vme,metadataTable.getRowCount()+1,0);
                       
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
                   public void mouseReleased(java.awt.event.MouseEvent evt)
                   {
                       if(evt.getX()>metadataTable.getWidth()-BUTTON_COL_WIDTH)
                       {
                         if(metadataTable.getSelectedColumn()==1)
                            MetadataEditor.this.current.getMetadataList().getMetadata().remove(metadataTable.getSelectedRow());
                            metadataTable.repaint();
                            requestFocusInWindow();
                       }
                   }
        }); 
        metadataTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
        metadataTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
        metadataTable.getColumnModel().getColumn(0).setMinWidth(200-BUTTON_COL_WIDTH);
        metadataTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);
        
        metadataTable.setDefaultRenderer(Object.class,new MetadataTableRenderer());
        metadataTable.setDefaultEditor(Object.class, new MetadataTableEditor());
        
        metadataTable.setRowHeight(ROW_HEIGHT);
        metadataTable.getTableHeader().setReorderingAllowed(false);
        
        scroll = new JScrollPane(metadataTable);
        metadataTable.addFocusListener(new FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent fe)
            {
                if(metadataTable.getSelectedColumn() == 1)
                    return;
                
                metadataTable.editCellAt(metadataTable.getSelectedRow(),0);
                MetadataTableEditor editor = (MetadataTableEditor)metadataTable.getCellEditor();
                if(editor!=null)
                  editor.focusField();
            }
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(scroll);
        tufts.vue.VUE.addActiveListener(tufts.vue.LWComponent.class,this);
        
        validate();
    }
    
    public void activeChanged(ActiveEvent e)
    {
       if(e!=null)
       {
         LWComponent active = (LWComponent)e.active;
         
         current = active;
         
         ((MetadataTableModel)metadataTable.getModel()).refresh();
       }
    }
    
    class MetadataTableHeaderRenderer extends DefaultTableCellRenderer
    {   
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JComponent comp = new JPanel();
           if(col == 0)
               comp =  new JLabel("Tag");
           else if(col == 1)
           {
               comp = new JLabel();
               ((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
           }
           
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           return comp;
       }
    }
    
    class MetadataTableRenderer extends DefaultTableCellRenderer
    {   
        
       public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col)
       {
           JPanel comp = new JPanel();
           comp.setLayout(new java.awt.BorderLayout());
           if(col == 0)
           {
               if(value instanceof edu.tufts.vue.ontology.OntType)
                 comp.add(new JLabel(((edu.tufts.vue.ontology.OntType)value).getLabel()));
               else
                 comp.add(new JTextField(current.getMetadataList().getMetadata().get(row).getObject().toString()));
           }
           else if(col == 1)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           
           return comp;
       }
    }
    
    class MetadataTableEditor extends DefaultCellEditor
    {   
        
       private JTextField field; 
        
       public MetadataTableEditor()
       {
           super(new JTextField());
           field = (JTextField)getComponent();
       }
       
       public void focusField()
       {
           field.requestFocus();
       }
        
       public java.awt.Component getTableCellEditorComponent(final JTable table,final Object value,boolean isSelected,final int row,final int col)
       {
           field = new JTextField();
           field.addFocusListener(new FocusAdapter(){
              public void focusLost(java.awt.event.FocusEvent fe)
              {  
                  VueMetadataElement vme = new VueMetadataElement();
                  vme.setObject(field.getText());
                  table.getModel().setValueAt(vme,row,col);
                  stopCellEditing();
              }
           });
           final JPanel comp = new JPanel();
           comp.setLayout(new java.awt.BorderLayout());
           if(col == 0)
           {
               field.setText(current.getMetadataList().getMetadata().get(row).getObject().toString());
               comp.add(field);
           }
           else if(col == 1)               
           {
               JLabel buttonLabel = new JLabel();
               buttonLabel.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.delete.up"));
               comp.add(buttonLabel);
           }
           
           comp.setOpaque(false);
           comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,ROW_INSET,ROW_GAP,ROW_INSET));
           
           field.addMouseListener(new MouseAdapter() {
               public void mouseExited(MouseEvent me)
               {
                   //System.out.println("MetadataEditor table cell - exited: " + me);
                   stopCellEditing();
               }
           });
           
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
               return current.getMetadataList().getMetadata().size();
             }
             else
               return 1;
         }
         
         public int getColumnCount()
         {
             return cols;
         }
         
         public boolean isCellEditable(int row,int column)
         {
             if(getValueAt(row,column) instanceof edu.tufts.vue.ontology.OntType)
                 return false;
             if(column == 0)
                 return true;
             else
                 return false;
         }
         
         public Object getValueAt(int row, int column)
         {
             
             if(current == null)
                 return "null";
             
             if(column == 0 && current != null)
             {    
               return current.getMetadataList().getMetadata().get(row).getObject();
             }
             else
             {
               return "delete button";
             }
         }
         
         public void setValueAt(Object value,int row, int column)
         {
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
             fireTableDataChanged();
         }
         
         public void refresh()
         {
             fireTableDataChanged();
         }
         
    }
    
}
