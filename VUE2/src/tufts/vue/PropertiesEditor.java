/*
 * ResourcePropertiesEditor.java
 *
 * Created on February 3, 2004, 8:37 PM
 */

package tufts.vue;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;

import java.util.*;
import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;
import java.util.ArrayList;

/**
 *
 * @author  akumar03
 */

public class PropertiesEditor extends JPanel implements DublinCoreConstants {
    
    PropertiesTableModel tableModel;
    
    /** Creates a new instance of ResourcePropertiesEditor */

    public PropertiesEditor(Properties properties,boolean editable) {
        tableModel = new PropertiesTableModel(properties, editable);
        setResourePropertiesPanel(); 
    }
    
    
    private void setResourePropertiesPanel() {
        JTable propertiesTable=new JTable(tableModel);
        propertiesTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        JScrollPane conditionsScrollPane=new JScrollPane(propertiesTable);
        conditionsScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel propertiesPanel=new JPanel();
        propertiesPanel.setLayout(new BorderLayout());
        propertiesPanel.add(conditionsScrollPane, BorderLayout.CENTER);
        propertiesPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        // GRID: addConditionButton
        JButton addPropertyButton=new JButton(VueResources.getImageIcon("addLight"));
        addPropertyButton.setPreferredSize(new Dimension(17, 17));
        
        // GRID: deleteConditionButton
        JButton deletePropertyButton=new JButton(VueResources.getImageIcon("deleteLight"));
        deletePropertyButton.setPreferredSize(new Dimension(17, 17));

        //setting the listeners
        PropertiesSelectionListener sListener = new PropertiesSelectionListener(deletePropertyButton,-1);
        propertiesTable.getSelectionModel().addListSelectionListener(sListener);
        addPropertyButton.addActionListener(new AddPropertiesButtonListener(tableModel));
        deletePropertyButton.addActionListener(new DeletePropertiesButtonListener(tableModel,sListener));

        // setting the properties editor for fields 
        JComboBox comboBox = new JComboBox(DC_FIELDS);
        propertiesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        
        //disable buttons if not editable
        if(tableModel.isEditable()) {
            topPanel.add(addPropertyButton);
            topPanel.add(deletePropertyButton);
        }
        
        // setting the panel for top buttons
        
        
        // setting box layout
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        
        JLabel label = new JLabel("Resource Metadata");
        
        label.setAlignmentX(Label.LEFT_ALIGNMENT);
        innerPanel.add(label);
        innerPanel.add(propertiesPanel);
        innerPanel.add(topPanel);
        
        add(innerPanel);
        setSize(300, 300);
        
        validate();
         
    }
    
    
    // a model for Properties table
     public class PropertiesTableModel extends AbstractTableModel {
         java.util.List m_conditions;
         Properties properties;
         boolean editable;

         public PropertiesTableModel(Properties properties,boolean editable) {
            this.editable = editable;
            setProperties(properties);   
        }

        public java.util.List getConditions() {
            return m_conditions;
        }
      
        public Properties getProperties() {
            return properties;
        }
        
        public void setProperties(Properties properties) {
            m_conditions=new ArrayList();
            this.properties = properties;
            Enumeration e = properties.keys();
            while(e.hasMoreElements()) {
                Condition cond = new Condition();
                String key = (String)e.nextElement();
                cond.setProperty(key);
                cond.setOperator(ComparisonOperator.eq);
                cond.setValue(properties.getProperty(key));
                m_conditions.add(cond);
            }
        }
        
       public boolean isEditable() {
           return editable;
       }
        
        public String getColumnName(int col) {
            if (col==0) {
                return "Field";
            } else {
                return "Value";
            }
        }

        public int getRowCount() {
            return m_conditions.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Object getValueAt(int row, int col) {
            Condition cond=(Condition) m_conditions.get(row);
            if (col==0) 
                return cond.getProperty();
            else
                return cond.getValue();
           
        }
        
        public void addProperty(String key, String value) {
            Condition cond = new Condition();
            cond.setProperty(key);
            cond.setOperator(ComparisonOperator.eq);
            cond.setValue(value);
            properties.put(key, value);
            m_conditions.add(cond);
        }
            
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return editable;
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
     
        public void setValueAt(Object value, int row, int col) {
            properties.remove(((Condition)m_conditions.get(row)).getProperty());
            Condition cond;
            if(row == -1)
                cond  = new Condition();
            else
                cond = (Condition) m_conditions.get(row);
            if(col == 0)
                cond.setProperty((String)value);
            if(col == 1)
                cond.setValue((String)value);
            // row = -1 adds new condions else replace the existing one.
            if (row==-1)
               m_conditions.add(cond);
            else 
                m_conditions.set(row, cond);
            
            properties.setProperty(cond.getProperty(), cond.getValue());
            fireTableCellUpdated(row, col);
        }
    

    }
     
     public class PropertiesSelectionListener  implements ListSelectionListener {        
        private int m_selectedRow;
        private JButton m_deleteButton;
        
        public PropertiesSelectionListener(JButton deleteButton, int selectedRow) {
            m_selectedRow=selectedRow;
            m_deleteButton=deleteButton;
            updateButtons();
        }
        
        public void valueChanged(ListSelectionEvent e) {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) return;
            
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if (lsm.isSelectionEmpty()) {
                m_selectedRow=-1;
            } else {
                m_selectedRow=lsm.getMinSelectionIndex();
            }
            updateButtons();
        }
        
        public int getSelectedRow() {
            return m_selectedRow;
        }
        
        public void setSelectedRow(int row) {
            this.m_selectedRow = row;
        }
        
        private void updateButtons() {
            if (getSelectedRow()==-1) {
                m_deleteButton.setEnabled(false);
            } else {
                m_deleteButton.setEnabled(true);
            }
        }
    }
    
     public class AddPropertiesButtonListener
            implements ActionListener {

        private PropertiesTableModel m_model;

        public AddPropertiesButtonListener(PropertiesTableModel model) {
            m_model=model;
           
        }

        public void actionPerformed(ActionEvent e) {
         
            m_model.addProperty(DC_FIELDS[0], "");
            m_model.fireTableDataChanged();
        }
          
    }

     public class DeletePropertiesButtonListener implements ActionListener { 
          private PropertiesTableModel m_model;
          private PropertiesSelectionListener m_sListener;
          
          public DeletePropertiesButtonListener(PropertiesTableModel model, PropertiesSelectionListener sListener) {
              m_model=model;
              m_sListener=sListener;
          }
          
          public void actionPerformed(ActionEvent e) {
              // will only be invoked if an existing row is selected
              int r=m_sListener.getSelectedRow();
              m_model.getConditions().remove(r);
              if(r > 0) 
                  m_sListener.setSelectedRow(r-1);
              m_model.fireTableRowsDeleted(r,r);
          }
      }
      
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
}
