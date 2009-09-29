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

package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import tufts.vue.gui.VueButton;
import tufts.vue.propertyeditor.*;
//import fedora.server.types.gen.ComparisonOperator;
//import fedora.server.types.gen.Condition;

/**
 * A field:value editor currently specialized for resource properties displayed on
 * the object inspector info tab.
 *
 * @version $Revision: 1.33 $ / $Date: 2009-09-29 21:36:55 $ / $Author: mike $ 
 * @author  akumar03
 */

// todo: generalize
public class PropertiesEditor extends JPanel implements DublinCoreConstants {
    
    PropertiesTableModel tableModel;
    JTable propertiesTable;
    PropertiesSelectionListener sListener;
    AddPropertiesButtonListener addPropertiesButtonListener;
    DeletePropertiesButtonListener deletePropertiesButtonListener;
    JButton addPropertyButton=new VueButton(VueResources.getString("button.add.label"));
    JButton deletePropertyButton=new VueButton(VueResources.getString("button.delete.label"));
    JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        
    
    /** Creates a new instance of ResourcePropertiesEditor */
    public PropertiesEditor(boolean editable) {
        tableModel = new PropertiesTableModel(editable);
        setResourePropertiesPanel();
    }
    public PropertiesEditor(PropertyMap properties, boolean editable) {
        tableModel = new PropertiesTableModel(properties, editable);
        setResourePropertiesPanel();
    }
    
    private void setResourePropertiesPanel() {
        addPropertyButton.setToolTipText("Add Metadata");
        deletePropertyButton.setToolTipText("Delete Metadata");
        questionLabel.setToolTipText("Add or delete resource metadata");
        // TODO: make table resize with window just like the JTable in the
        // NodeFilterEditor.  
        propertiesTable=new JTable(tableModel);
        //propertiesTable.setCellSelectionEnabled(true);
        //propertiesTable.setBackground(Color.red);
        //propertiesTable.setOpaque(false); // will need to subclass and make transparent the returned renderers
        propertiesTable.setPreferredScrollableViewportSize(new Dimension(200,150));
        propertiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        propertiesTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        propertiesTable.getColumnModel().getColumn(1).setPreferredWidth(700);
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
                if (propertiesTable.getSelectedRow() == (propertiesTable.getRowCount()-1)
                    && e.getKeyCode() == KeyEvent.VK_ENTER && tableModel.isEditable())
                {
                    tableModel.addDefaultProperty();
                }
            }
        });

        
        JScrollPane conditionsScrollPane=new JScrollPane(propertiesTable);
        //conditionsScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel propertiesPanel=new JPanel();
        propertiesPanel.setLayout(new BorderLayout());
        propertiesPanel.add(conditionsScrollPane, BorderLayout.CENTER);
        //propertiesPanel.setBorder(BorderFactory.createEmptyBorder(2,0,3,0));
        
        
        // setting the properties editor for fields
        
        
        JPanel labelPanel=new JPanel(new BorderLayout());
        //labelPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        JLabel label = new JLabel(VueResources.getString("jlabel.resourcemetadata"));
        label.setAlignmentX(Label.LEFT_ALIGNMENT);
        labelPanel.add(label,BorderLayout.WEST);
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        //topPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        
        //disable buttons if not editable
        //System.out.println("Editable: "+tableModel.isEditable());
        if(tableModel.isEditable()) {
            addPropertyButton.setEnabled(true);
            deletePropertyButton.setEnabled(true);
            sListener = new PropertiesSelectionListener(deletePropertyButton,-1);
            propertiesTable.getSelectionModel().addListSelectionListener(sListener);
            addPropertiesButtonListener = new AddPropertiesButtonListener(tableModel);
            addPropertyButton.addActionListener(addPropertiesButtonListener);
            deletePropertiesButtonListener = new DeletePropertiesButtonListener(propertiesTable,sListener);
            deletePropertyButton.addActionListener(deletePropertiesButtonListener);
            topPanel.add(addPropertyButton);
            topPanel.add(deletePropertyButton);
            topPanel.add(questionLabel);
            JComboBox comboBox = new JComboBox(DC_FIELDS);
            //comboBox.setLightWeightPopupEnabled(true);
            propertiesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));
            
        } else {
            addPropertyButton.setEnabled(false);
            deletePropertyButton.setEnabled(false);
        }
        labelPanel.add(topPanel,BorderLayout.EAST);
        // setting the panel for top buttons
        
        
        // setting box layout
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BorderLayout());
        //innerPanel.setBorder(BorderFactory.createEmptyBorder(0,6,6,6));
        
        
        innerPanel.add(labelPanel,BorderLayout.NORTH);
        innerPanel.add(propertiesPanel,BorderLayout.CENTER);
        //innerPanel.add(topPanel);
        //innerPanel.setBorder(BorderFactory.createTitledBorder("Metadata"));
        setLayout(new BorderLayout());
        add(innerPanel,BorderLayout.CENTER);
        //setSize(300, 300);
        
        validate();
        
    }
    
    public void setProperties(PropertyMap properties,boolean editable) {

        tableModel.setEditable(editable);
        tableModel.setProperties(properties);

        /*
        tableModel = new PropertiesTableModel(properties,editable);
        // todo: do NOT set a new table model here: just tell
        // the old one to load new data and send a refresh.
        // That way user dragged settings of column widths
        // aren't blown away every time we display new data.
        propertiesTable.setModel(tableModel);
        propertiesTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        propertiesTable.getColumnModel().getColumn(1).setPreferredWidth(700);
        */

        if(tableModel.isEditable()) {
            addPropertyButton.setEnabled(true);
            deletePropertyButton.setEnabled(true);
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
        } else {
            addPropertyButton.setEnabled(false);
            deletePropertyButton.setEnabled(false);
        }
        
    }
    
    public void clear() {
        //setProperties(new Properties(), tableModel.isEditable());
        //tableModel.setProperties(new Properties());
        tableModel.setProperties(new PropertyMap());
    }
    
    public PropertiesTableModel getPropertiesTableModel() {
        return tableModel;
    }
    // a model for Properties table
    private static String PROPERTY_NOT_SET = "";
    public static class PropertiesTableModel extends AbstractTableModel {
        java.util.List m_conditions;
        Map mProperties;
        boolean editable;
        
        public PropertiesTableModel(boolean editable) {
            this(new PropertyMap(), editable);
        }
        
        public PropertiesTableModel(PropertyMap properties, boolean editable) {
            this.editable = editable;
            setProperties(properties);
        }
        
        public java.util.List getConditions() {
            return m_conditions;
        }
        
        public Map getProperties() {
            return mProperties;
        }
        
        public void setProperties(PropertyMap properties) {
            m_conditions=new ArrayList();
            this.mProperties = properties;
            
            ArrayList keys = new ArrayList(properties.keySet());
            Collections.sort(keys);
            
            Iterator i = keys.iterator();
            while (i.hasNext()) {
                Condition cond = new Condition();
                String key = (String) i.next();
                cond.setProperty(key);
                cond.setOperator(ComparisonOperator.eq);
                cond.setValue(properties.getProperty(key));
                m_conditions.add(cond);
            }

            if (isEditable() && m_conditions.size() == 0)
                addProperty(DC_FIELDS[0], PROPERTY_NOT_SET);

            fireTableDataChanged();
        }

        public void setEditable(boolean canEdit) {
            this.editable = canEdit;
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
            if (value != PROPERTY_NOT_SET)
                mProperties.put(key, value);
            m_conditions.add(cond);
            fireTableDataChanged();
        }
        public void addDefaultProperty() {
            addProperty(DC_FIELDS[0], PROPERTY_NOT_SET);
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
            mProperties.remove(((Condition)m_conditions.get(row)).getProperty());
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
            
            //mProperties.setProperty(cond.getProperty(), cond.getValue());
            mProperties.put(cond.getProperty(), cond.getValue());
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
            if(PropertiesEditor.this.tableModel.isEditable()){
                if (getSelectedRow()==-1) {
                    m_deleteButton.setEnabled(false);
                } else {
                    m_deleteButton.setEnabled(true);
                }
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
