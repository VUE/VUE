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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;

import java.util.*;
import tufts.vue.filter.*;

public class FilterEditor extends JPanel{
    public static final Key keyLabel = new Key("Label", TypeFactory.getStringType());
    public static final Key keyAnywhere = new Key("Anywhere", TypeFactory.getStringType());
    public static final Key keyNotes = new Key("Notes", TypeFactory.getStringType());
    
    FilterTableModel filterTableModel;
    boolean editable = true;
    JComboBox operatorEditor;
    JTable filterTable;
    Statement defaultStatement; // statement that is added when add(+) is clicked.
    
    
    /** Creates a new instance ofFilterEditor */
    public FilterEditor() {
        filterTableModel = new FilterTableModel();
        setNodeFilterPanel();
    }
    public FilterEditor(FilterTableModel filterTableModel) {
        this.filterTableModel = filterTableModel;
    }
    
    public FilterTableModel getFilterTableModel() {
        return filterTableModel;
    }
    private void setNodeFilterPanel() {
        filterTable = new JTable(filterTableModel);
        filterTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        filterTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        JScrollPane filterScrollPane=new JScrollPane(filterTable);
        filterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  filterPanel=new JPanel();
        filterPanel.setLayout(new BorderLayout());
        filterPanel.add(filterScrollPane, BorderLayout.CENTER);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        // GRID: addConditionButton
        JButton addButton=new tufts.vue.VueButton("add");
        addButton.addActionListener(new AddButtonListener(filterTableModel));
        
        
        // GRID: deleteConditionButton
        JButton deleteButton=new tufts.vue.VueButton("delete");
        deleteButton.setEnabled(false);
        // adding the delete functionality */
        FilterSelectionListener sListener= new  FilterSelectionListener(deleteButton, -1);
        filterTable.getSelectionModel().addListSelectionListener(sListener);
        deleteButton.addActionListener(new DeleteButtonListener(filterTable, sListener));
        
        filterTable.getColumnModel().getColumn(FilterTableModel.KEY_COL).setCellEditor(new KeyCellEditor());
        filterTable.getColumnModel().getColumn(FilterTableModel.OPERATOR_COL).setCellEditor(new OperatorCellEditor());
        
        
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        
        
        
        //innerPanel.add(labelPanel);
        innerPanel.add(filterPanel);
        innerPanel.add(bottomPanel);
        
        add(innerPanel);
        //setSize(300, 300);
        
        validate();
    }
    public class FilterTableModel extends AbstractTableModel {
        public static final int KEY_COL = 0;
        public static final int OPERATOR_COL = 1;
        public static final int VALUE_COL = 2;
        public static final int TYPE_COL = 3;
        
        
        boolean editable;
        Vector filters;
        
        public FilterTableModel() {
            this.editable = editable;
            filters = new Vector();
            
        }
        
        public Vector getFilters() {
            return this.filters;
        }
        
        public void addStatement(Statement statement) {
            filters.add(statement);
        }
        
        public boolean isEditable() {
            return editable;
        }
        
        public String getColumnName(int col) {
            if (col==0) {
                return "Field";
            } else if(col == 1) {
                return "Criteria";
            } else
                return "Search";
            
            
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
            super(new JTextField(""));
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            
            TableModel tableModel = table.getModel();
            if (tableModel instanceof FilterTableModel) {
                FilterTableModel filterTableModel  = (FilterTableModel) tableModel;
                editor =  new JComboBox((Vector)((Statement)(filterTableModel.getFilters().get(row))).getKey().getType().getOperators());
                return editor;
            }
            return (new JTextField(""));
        }
        
        public Object getCellEditorValue() {
            if(editor!= null) {
                return editor.getSelectedItem();
            } else
                throw new RuntimeException("No Keys present");
            
        }
        public int getClickCountToStart() {
            return 0;
        }
        
    }
    
    public class KeyCellEditor extends DefaultCellEditor {
        /** setting the defaultCellEditor **/
        Vector keys;
        JComboBox editor = null;
        public KeyCellEditor() {
            super(new JTextField(""));
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            TableModel tableModel = table.getModel();
            if (tableModel instanceof FilterTableModel) {
                FilterTableModel filterTableModel  = (FilterTableModel) tableModel;
                keys = new Vector();
                keys.add(keyAnywhere);
                keys.add(keyLabel);
                keys.add(keyNotes);
                keys.addAll(VUE.getActiveMap().getMapFilterModel().getKeyVector());
                editor = new JComboBox(keys);
                return editor;
            }
            return (new JTextField(""));
        }
        
        
        public Object getCellEditorValue() {
            if(editor!= null) {
                return editor.getSelectedItem();
            } else
                throw new RuntimeException("No Keys present");
            
            
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