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
    JTable propertiesTable;
    PropertiesSelectionListener sListener;
    AddPropertiesButtonListener addPropertiesButtonListener;
    DeletePropertiesButtonListener deletePropertiesButtonListener;
    JButton addPropertyButton=new VueButton("add");
    JButton deletePropertyButton=new VueButton("delete");
    
    
    /** Creates a new instance of ResourcePropertiesEditor */
    public PropertiesEditor(boolean editable) {
        tableModel = new PropertiesTableModel(editable);
        setResourePropertiesPanel();
    }
    public PropertiesEditor(Properties properties,boolean editable) {
        tableModel = new PropertiesTableModel(properties, editable);
        setResourePropertiesPanel();
    }
    
    
    private void setResourePropertiesPanel() {
        propertiesTable=new JTable(tableModel);
        propertiesTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        propertiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        propertiesTable.addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                if(propertiesTable.isEditing()) {
                    propertiesTable.getCellEditor(propertiesTable.getEditingRow(),propertiesTable.getEditingColumn()).stopCellEditing();
                }
                propertiesTable.removeEditor();
            }
            public void focusGained(FocusEvent e) {
            }
        });
        propertiesTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(propertiesTable.getSelectedRow() == (propertiesTable.getRowCount()-1) && e.getKeyCode() == e.VK_ENTER){
                    tableModel.addDefaultProperty();
                }
            }
        });
        JScrollPane conditionsScrollPane=new JScrollPane(propertiesTable);
        conditionsScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel propertiesPanel=new JPanel();
        propertiesPanel.setLayout(new BorderLayout());
        propertiesPanel.add(conditionsScrollPane, BorderLayout.CENTER);
        propertiesPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        
        // setting the properties editor for fields
        
        
        JPanel labelPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,2,0));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        JLabel label = new JLabel("Resource Metadata");
        label.setAlignmentX(Label.LEFT_ALIGNMENT);
        labelPanel.add(label);
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        
        //disable buttons if not editable
        if(tableModel.isEditable()) {
            sListener = new PropertiesSelectionListener(deletePropertyButton,-1);
            propertiesTable.getSelectionModel().addListSelectionListener(sListener);
            addPropertiesButtonListener = new AddPropertiesButtonListener(tableModel);
            addPropertyButton.addActionListener(addPropertiesButtonListener);
            deletePropertiesButtonListener = new DeletePropertiesButtonListener(propertiesTable,sListener);
            deletePropertyButton.addActionListener(deletePropertiesButtonListener);
            topPanel.add(addPropertyButton);
            topPanel.add(deletePropertyButton);
            JComboBox comboBox = new JComboBox(DC_FIELDS);
            propertiesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));
            
        }
        
        // setting the panel for top buttons
        
        
        // setting box layout
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        
        
        //innerPanel.add(labelPanel);
        innerPanel.add(propertiesPanel);
        innerPanel.add(topPanel);
        
        add(innerPanel);
        //setSize(300, 300);
        
        validate();
        
    }
    
    public void setProperties(Properties properties,boolean editable) {
        tableModel = new PropertiesTableModel(properties,editable);
        propertiesTable.setModel(tableModel);
        if(tableModel.isEditable()) {
            sListener = new PropertiesSelectionListener(deletePropertyButton,-1);
            propertiesTable.getSelectionModel().addListSelectionListener(sListener);
            addPropertyButton.removeActionListener(addPropertiesButtonListener);
            addPropertiesButtonListener = new AddPropertiesButtonListener(tableModel);
            addPropertyButton.addActionListener(addPropertiesButtonListener);
            deletePropertyButton.removeActionListener(deletePropertiesButtonListener);
            deletePropertiesButtonListener = new DeletePropertiesButtonListener(propertiesTable,sListener);
            deletePropertyButton.addActionListener(deletePropertiesButtonListener);
            JComboBox comboBox = new JComboBox(DC_FIELDS);
            propertiesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));
        }
        
    }
    
    public void clear() {
        setProperties(new Properties(), true);
    }
    
    // a model for Properties table
    public class PropertiesTableModel extends AbstractTableModel {
        java.util.List m_conditions;
        Properties properties;
        boolean editable;
        
        public PropertiesTableModel(boolean editable) {
            this(new Properties(), editable);
        }
        
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
            
            if(m_conditions.size() == 0) {
                addProperty(DC_FIELDS[0], "");
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
            fireTableDataChanged();
        }
        public void addDefaultProperty() {
            addProperty(DC_FIELDS[0], "");
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
    
    public class AddPropertiesButtonListener implements ActionListener {
        
        private PropertiesTableModel m_model;
        
        public AddPropertiesButtonListener(PropertiesTableModel model) {
            m_model=model;
            
        }
        
        public void actionPerformed(ActionEvent e) {
            m_model.addDefaultProperty();
        }
    }
    
    public class DeletePropertiesButtonListener implements ActionListener {
        
        private PropertiesSelectionListener m_sListener;
        private JTable table;
        
        public DeletePropertiesButtonListener(JTable table,PropertiesSelectionListener sListener) {
            
            this.table = table;
            m_sListener=sListener;
        }
        
        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            int r=m_sListener.getSelectedRow();
            ((PropertiesTableModel)table.getModel()).getConditions().remove(r);
            ((PropertiesTableModel)table.getModel()).fireTableRowsDeleted(r,r);
            if(r> 0)
                table.setRowSelectionInterval(r-1, r-1);
            else if(table.getRowCount() > 0)
                table.setRowSelectionInterval(0,0);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
}
