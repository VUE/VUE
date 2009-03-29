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

/*
 * FilterEditor.java
 *
 * Created on February 25, 2004, 10:56 AM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 *
 * This class allows creation and edition of filters. Filters are stored in a vector
 * in  FilterTableModel.
 */

import tufts.vue.filter.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

public class FilterEditor extends JPanel  {
    public static final Key keyLabel = new Key("Label", TypeFactory.getStringType());
    public static final Key keyAnywhere = new Key("Anywhere", TypeFactory.getStringType());
    public static final Key keyNotes = new Key("Notes", TypeFactory.getStringType());
    public static final JLabel triangleLabel = new JLabel(VueResources.getImageIcon("triangleDownIcon"));
    JLabel questionLabel = new JLabel(tufts.vue.VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
    public static final String FILTER_INFO = tufts.vue.VueResources.getString("info.filter.create");
    JButton addButton=new tufts.vue.gui.VueButton(VueResources.getString("button.add.label"));
    JButton deleteButton=new tufts.vue.gui.VueButton(VueResources.getString("button.delete.label"));
    
    
    FilterTableModel filterTableModel;
    boolean editable = true;
    JComboBox operatorEditor;
    JTable filterTable;
    Statement defaultStatement; // statement that is added when add(+) is clicked.
    
    
    /** Creates a new instance ofFilterEditor */
    public FilterEditor() {
        filterTableModel = new FilterTableModel();
        questionLabel.setToolTipText(FILTER_INFO);
        setFilterEditorPanel();
        
    }
    
    public FilterTableModel getFilterTableModel() {
        return this.filterTableModel;
    }
    
    /**
     * public FilterEditor(FilterTableModel filterTableModel) {
     * this.filterTableModel = filterTableModel;
     * setFilterEditorPanel();
     * }
     **/
    
    public void stopEditing() {
        if(filterTable.isEditing()) {
            filterTable.getCellEditor(filterTable.getEditingRow(),filterTable.getEditingColumn()).stopCellEditing();
        }
        filterTable.removeEditor();
    }
    
    
    private void setFilterEditorPanel() {
        ButtonGroup criteriaSelectionGroup = new ButtonGroup();
        filterTable = new JTable(filterTableModel);
        filterTable.addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                //  stopEditing();
            }
            public void focusGained(FocusEvent e) {
            }
        });
        filterTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(filterTable.getSelectedRow() == (filterTableModel.getRowCount()-1) && e.getKeyCode() == e.VK_ENTER){
                    filterTableModel.addStatement();
                }
            }
        });
        filterTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        filterTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        JScrollPane filterScrollPane=new JScrollPane(filterTable);
        filterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  filterPanel=new JPanel();
        filterPanel.setLayout(new BorderLayout());
        filterPanel.add(filterScrollPane, BorderLayout.CENTER);
        
        // GRID: addConditionButton
        addButton.addActionListener(new AddButtonListener(filterTableModel));
        addButton.setToolTipText("Add Criteria");
        // GRID: deleteConditionButton
        deleteButton.setEnabled(false);
        deleteButton.setToolTipText("Delete Criteria");
        // adding the delete functionality */
        FilterSelectionListener sListener= new  FilterSelectionListener(deleteButton, -1);
        filterTable.getSelectionModel().addListSelectionListener(sListener);
        deleteButton.addActionListener(new DeleteButtonListener(filterTable, sListener));
        filterTable.getColumnModel().getColumn(FilterTableModel.KEY_COL).setCellEditor(new KeyCellEditor());
        filterTable.getColumnModel().getColumn(FilterTableModel.KEY_COL).setCellRenderer(new KeyCellRenderer());
        filterTable.getColumnModel().getColumn(FilterTableModel.OPERATOR_COL).setCellEditor(new OperatorCellEditor());
        filterTable.getColumnModel().getColumn(FilterTableModel.OPERATOR_COL).setCellRenderer(new OperatorCellRenderer());
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        //innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        //bottomPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(questionLabel);
        innerPanel.add(bottomPanel);
        innerPanel.add(filterPanel);
        
        setLayout(new BorderLayout());
        add(innerPanel,BorderLayout.CENTER);
        //setSize(300, 300);
        
        validate();
    }
    
    
    public class FilterTableModel extends AbstractTableModel implements MapFilterModel.Listener{
        public static final int KEY_COL = 0;
        public static final int OPERATOR_COL = 1;
        public static final int VALUE_COL = 2;
        public static final int TYPE_COL = 3;
        
        
        boolean editable;
        Vector filters;
        
        public FilterTableModel() {
            filters = new Vector();
        }
        
        public Vector getFilters() {
            return this.filters;
        }
        
        public void setFilters(Vector filters) {
            tufts.vue.VUE.getActiveMap().getMapFilterModel().addListener(this);
            // this is a hack needs to be fixed.  Filtermodel should be aware of map it belongs to
            this.filters = filters;
            
            fireTableDataChanged();
            
        }
        
        public void addStatement(Statement statement) {
            filters.add(statement);
            fireTableDataChanged();
        }
        
        public void addStatement() {
            Statement stmt = new Statement();
            Key key = keyAnywhere;
            if(tufts.vue.VUE.getActiveMap().getMapFilterModel().size() > 0) {
                key = (Key) tufts.vue.VUE.getActiveMap().getMapFilterModel().get(0);
            }
            stmt.setKey(key);
            stmt.setOperator(key.getType().getDefaultOperator());
            stmt.setValue(key.getDefaultValue());
            addStatement(stmt);
        }
        
        public void mapFilterModelChanged(MapFilterModelEvent e) {
            filterTableModel.fireTableDataChanged();
        }
        
        public boolean isEditable() {
            return editable;
        }
        
        public String getColumnName(int col) {
            if (col==0) {
                return "Element Name";
            } else if(col == 1) {
                return "Operator";
            } else
                return "Value";
            
            
        }
        
        public int getRowCount() {
            return filters.size();
        }
        
        public int getColumnCount() {
            return 3;
        }
        
        public Object getValueAt(int row, int col) {
            Statement statement = (Statement) filters.get(row);
            if (col== KEY_COL)
                return statement.getKey();
            else if(col == OPERATOR_COL)
                return statement.getOperator();
            else
                return statement.getValue();
            
        }
        public void setValueAt(Object value, int row, int col) {
            Statement statement = (Statement) filters.get(row);
            Key key = statement.getKey();
            if(col == VALUE_COL) {
                if(statement.getKey().getType().getDisplayName().equals(Type.INTEGER_TYPE)) {
                    statement.setValue(new Integer(value.toString()));
                } else {
                    statement.setValue(value);
                }
            }else if(col == KEY_COL)  {
                
                if(value instanceof Key) {
                    
                    statement.setKey((Key)value);
                    setValueAt(((Key)value).getType().getDefaultOperator(),row,OPERATOR_COL);
                    setValueAt(((Key)value).getDefaultValue(), row,VALUE_COL);
                }
                //statement.setOperator(((Key)value).getType().getDefaultOperator());
                //statement.setValue(((Key)value).getDefaultValue());
                // fireTableRowsUpdated(row,row);
                
            }else if(col == OPERATOR_COL)  {
                if(value instanceof Operator) {
                    statement.setOperator((Operator)value);
                }
            }
            
            // row = -1 adds new condions else replace the existing one.
            
            fireTableCellUpdated(row, col);
            fireTableDataChanged();
        }
        
        
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            //  return editable;
            return true;
        }
        
        
    }
    
    public class AddButtonListener implements ActionListener {
        private  FilterTableModel model;
        public AddButtonListener(FilterTableModel model) {
            this.model=model;
        }
        
        public void actionPerformed(ActionEvent e) {
            //m_model.addProperty(DC_FIELDS[0], "");
            // AddDialog addDialog = new AddDialog(model);
            Statement stmt = new Statement();
            Key key = keyAnywhere;
            if(tufts.vue.VUE.getActiveMap().getMapFilterModel().size() > 0) {
                key = (Key) tufts.vue.VUE.getActiveMap().getMapFilterModel().get(0);
            }
            stmt.setKey(key);
            stmt.setOperator(key.getType().getDefaultOperator());
            stmt.setValue(key.getDefaultValue());
            model.addStatement(stmt);
            model.fireTableDataChanged();
            
        }
    }
    
    
    /** Tablecell editor for opertator columm.  Needed to be redone to
     * display the correct combobox based on the componet selected in the column.
     *
     *.
     *
     */
    
    public class OperatorCellEditor extends DefaultCellEditor {
        /** setting the defaultCellEditor **/
        JComboBox editor = null;
        public OperatorCellEditor() {
            super(new JComboBox());
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editor =  new JComboBox((Vector)((Statement)(VUE.getActiveMap().getLWCFilter().getStatements().get(row))).getKey().getType().getOperators());
            return editor;
        }
        
        public Object getCellEditorValue() {
            if(editor!= null) {
                return editor.getSelectedItem();
            } else
                throw new RuntimeException("No Keys present");
            
        }
        
    }
    
    public class OperatorCellRenderer extends DefaultTableCellRenderer {
        Vector keys = new Vector();;
        JComboBox editor = null;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if(value.toString().equals("")) 
                value = keyAnywhere.getType().getDefaultOperator().toString();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(0, 2,0,0));
            panel.add(new JLabel(value.toString()),BorderLayout.CENTER);
            panel.add(triangleLabel,BorderLayout.EAST);
            panel.setBackground(Color.WHITE);
            return panel;
        }
    }
    
    
    public class KeyCellEditor extends DefaultCellEditor {
        /** setting the defaultCellEditor **/
        Vector keys = new Vector();;
        JComboBox editor = null;
        public KeyCellEditor() {
            super(new JComboBox());
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            keys.removeAllElements();
            keys.add(keyAnywhere);
            keys.add(keyLabel);
            keys.add(keyNotes);
            keys.addAll(VUE.getActiveMap().getMapFilterModel().getKeyVector());
            editor = new JComboBox(keys);
            return editor;
        }
        
        
        
        public Object getCellEditorValue() {
            if(editor!= null) {
                return editor.getSelectedItem();
            } else
                throw new RuntimeException("No Keys present");
            
            
        }
        
        
        
    }
    
    public class KeyCellRenderer extends DefaultTableCellRenderer {
        Vector keys = new Vector();;
        JComboBox editor = null;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if(value.toString().equals("")) 
                value = keyAnywhere.toString();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(0, 2,0,0));
            panel.add(new JLabel(value.toString()),BorderLayout.CENTER);
            panel.add(triangleLabel,BorderLayout.EAST);
            panel.setBackground(Color.WHITE);
            return panel;
        }
    }
    
    /** not used currently.  **/
    
    
    public class FilterSelectionListener  implements ListSelectionListener {
        private int m_selectedRow;
        private JButton m_deleteButton;
        
        public FilterSelectionListener(JButton deleteButton, int selectedRow) {
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
    
    
    public class DeleteButtonListener implements ActionListener {
        private JTable table;
        private FilterSelectionListener m_sListener;
        
        public DeleteButtonListener(JTable table,FilterSelectionListener sListener) {
            this.table = table;
            m_sListener=sListener;
        }
        
        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            int r=m_sListener.getSelectedRow();
            ((FilterTableModel) table.getModel()).getFilters().remove(r);
            ((FilterTableModel) table.getModel()).fireTableRowsDeleted(r,r);
            if(r> 0)
                table.setRowSelectionInterval(r-1, r-1);
            else if(table.getRowCount() > 0)
                table.setRowSelectionInterval(0,0);
        }
    }
    
}