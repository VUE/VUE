/*
 * NodeFilterEditor.java
 *
 * Created on February 16, 2004, 2:10 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;

import java.util.*;
import java.util.ArrayList;

public class NodeFilterEditor extends JPanel{
    NodeFilterTableModel nodeFilterTableModel;
    boolean editable = false;
    JComboBox keyEditor;
    JComboBox operatorEditor;
    JTable nodeFilterTable;
    
    /** Creates a new instance of NodeFilterEditor */
    public NodeFilterEditor( NodeFilter nodeFilter,boolean editable) {
        nodeFilterTableModel = new NodeFilterTableModel(nodeFilter,editable);
        setNodeFilterPanel();
    }
    
    private void setNodeFilterPanel() {
        nodeFilterTable = new JTable(nodeFilterTableModel);
        nodeFilterTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        JScrollPane nodeFilterScrollPane=new JScrollPane(nodeFilterTable);
        nodeFilterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  nodeFilterPanel=new JPanel();
        nodeFilterPanel.setLayout(new BorderLayout());
        nodeFilterPanel.add( nodeFilterScrollPane, BorderLayout.CENTER);
        nodeFilterPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        // GRID: addConditionButton
        JButton addButton=new tufts.vue.VueButton("add");
        addButton.addActionListener(new AddButtonListener(nodeFilterTableModel));
        
        
        // GRID: deleteConditionButton
        JButton deleteButton=new tufts.vue.VueButton("delete");
        deleteButton.setEnabled(false);
        // adding the delete functionality */
        NodeFilterSelectionListener sListener= new NodeFilterSelectionListener(deleteButton, -1);
        nodeFilterTable.getSelectionModel().addListSelectionListener(sListener);
        deleteButton.addActionListener(new DeleteButtonListener(nodeFilterTable, sListener));
         
        /** setting editors ***/
        keyEditor = new JComboBox(tufts.vue.VUE.getActiveMap().getMapFilterModel());
        nodeFilterTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(keyEditor)); 
        //operatorEditor = new JComboBox((Vector)((Key)keyEditor.getItemAt(0)).getType().getOperators());
        nodeFilterTable.getColumnModel().getColumn(NodeFilterTableModel.OPERATOR_COL).setCellEditor(new OperatorCellEditor()); 
             
       
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        
        
        
        //innerPanel.add(labelPanel);
        innerPanel.add(nodeFilterPanel);
        innerPanel.add(bottomPanel);
        
        add(innerPanel);
        //setSize(300, 300);
        
        validate();
    }
    public class NodeFilterTableModel extends AbstractTableModel {
        public static final int KEY_COL = 0;
        public static final int OPERATOR_COL = 1;
        public static final int VALUE_COL = 2;
        public static final int TYPE_COL = 3;
        
        
        boolean editable;
        NodeFilter nodeFilter;
        
        public NodeFilterTableModel(NodeFilter nodeFilter,boolean editable) {
            this.editable = editable;
            this.nodeFilter  = nodeFilter;
        }
        
        public NodeFilter getNodeFilter() {
            return this.nodeFilter;
        }
        
        public void addStatement(Statement statement) {
            nodeFilter.add(statement);
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
            return nodeFilter.size();
        }
        
        public int getColumnCount() {
            return 3;
        }
     /**   
        public Class getColumnClass(int c) {
            if(getValueAt(0,c) != null)
                return getValueAt(0, c).getClass();
            return Object.class;
        }
        */
        public Object getValueAt(int row, int col) {
            Statement statement = (Statement) nodeFilter.get(row);
            if (col== KEY_COL)
                return statement.getKey().toString();
            else if(col == OPERATOR_COL)
                return statement.getOperator();
            else  
                return statement.getValue();
          
        }
        public void setValueAt(Object value, int row, int col) {
            Statement statement = (Statement) nodeFilter.get(row);
            Key key = statement.getKey();
            if(col == VALUE_COL) { 
                if(statement.getKey().getType().getDisplayName().equals(Type.INTEGER_TYPE)) {
                    statement.setValue(new Integer((String)value));
                } else {
                     statement.setValue(value);
                }
            } else if(col == KEY_COL)  {
                statement.setKey((Key)value);
                setValueAt(((Key)value).getType().getDefaultOperator(),row,OPERATOR_COL);
                setValueAt(((Key)value).getDefaultValue(), row,VALUE_COL);
                //statement.setOperator(((Key)value).getType().getDefaultOperator());
                //statement.setValue(((Key)value).getDefaultValue());
               // fireTableRowsUpdated(row,row);
                
            }else if(col == OPERATOR_COL)  {
                if(value instanceof Operator)
                    statement.setOperator((Operator)value);
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
        private  NodeFilterTableModel model;
        public AddButtonListener(NodeFilterTableModel model) {
            this.model=model;
        }

        public void actionPerformed(ActionEvent e) {
           if(tufts.vue.VUE.getActiveMap().getMapFilterModel().size() > 0) {
                Key key = (Key) tufts.vue.VUE.getActiveMap().getMapFilterModel().get(0);
                Statement stmt = new Statement();
                stmt.setKey(key);
                stmt.setOperator(key.getType().getDefaultOperator());
                stmt.setValue(key.getDefaultValue());
                model.addStatement(stmt);
                model.fireTableDataChanged();
            }
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
            if (tableModel instanceof NodeFilterTableModel) {
                NodeFilterTableModel nodeFilterTableModel  = (NodeFilterTableModel) tableModel;
                editor =  new JComboBox((Vector)((Statement)(nodeFilterTableModel.getNodeFilter().get(row))).getKey().getType().getSettableOperators());
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
    public class AddDialog extends JDialog {
        NodeFilterTableModel model;
        JLabel keyLabel;
        JLabel typeLabel;
        JLabel operatorLabel;
        JLabel valueLabel;
        JComboBox keyEditor;
        JComboBox operatorEditor;
        JTextField valueEditor;
        JTextField typeEditor;
        
        Vector allTypes;
        
        public AddDialog(NodeFilterTableModel model) {
            super(tufts.vue.VUE.getInstance(),"Add Key",true);
            this.model = model;
            allTypes = (Vector)TypeFactory.getAllTypes();
            keyLabel = new JLabel("Key");
            typeLabel = new JLabel("Type");
            operatorLabel = new JLabel("Operator");
            valueLabel = new JLabel("Value");
            keyEditor = new JComboBox(tufts.vue.VUE.getActiveMap().getMapFilterModel());
            
                
            operatorEditor = new JComboBox();
            valueEditor = new JTextField();
            typeEditor = new JTextField();
            typeEditor.setEditable(false);
            
            keyEditor.setPreferredSize(new Dimension(80,20));
            keyEditor.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if( e.getStateChange() == e.SELECTED) {
                        Key key = (Key) e.getItem();
                        operatorEditor.addItem("Hello");
                        typeEditor.setText(key.getType().getDisplayName());
                        
                    }
                }
            });
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c  = new GridBagConstraints();
            c.insets = new Insets(2,2, 2, 2);
            JPanel panel = new JPanel();
            panel.setLayout(gridbag);
            
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE;
            gridbag.setConstraints(keyLabel, c);
            panel.add(keyLabel);
            
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridbag.setConstraints(keyEditor, c);
            panel.add(keyEditor);
            
            
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE;
            gridbag.setConstraints(operatorLabel, c);
            panel.add(operatorLabel);
            
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridbag.setConstraints(operatorEditor, c);
            panel.add(operatorEditor);
             
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE;
            gridbag.setConstraints(valueLabel, c);
            panel.add(valueLabel);
            
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridbag.setConstraints(valueEditor, c);
            panel.add(valueEditor);
            
            c.anchor = GridBagConstraints.EAST;
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = GridBagConstraints.RELATIVE;
            gridbag.setConstraints(typeLabel, c);
            panel.add(typeLabel);
            
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridbag.setConstraints(typeEditor, c);
            panel.add(typeEditor);
            
            // SOUTH: southPanel(cancelButton, okButton)
            
            JButton okButton=new JButton("Ok");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateModelAndNotify();
                    setVisible(false);
                }
            });
            
            JButton cancelButton=new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            
            JPanel southPanel=new JPanel();
            southPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            southPanel.add(okButton);
            southPanel.add(cancelButton);
         
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridbag.setConstraints(southPanel, c);
            panel.add(southPanel);
           
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(panel,BorderLayout.CENTER);
       
            pack();
            setLocation(500,300);
            show();
            
        }
        
        private void updateModelAndNotify(){
            
            model.fireTableDataChanged();
        }
    }
    
    public class NodeFilterSelectionListener  implements ListSelectionListener {        
        private int m_selectedRow;
        private JButton m_deleteButton;
        
        public NodeFilterSelectionListener(JButton deleteButton, int selectedRow) {
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
          private NodeFilterSelectionListener m_sListener;
          
          public DeleteButtonListener(JTable table,NodeFilterSelectionListener sListener) {
             this.table = table;
              m_sListener=sListener;
          }
          
          public void actionPerformed(ActionEvent e) {
              // will only be invoked if an existing row is selected
              int r=m_sListener.getSelectedRow();
              ((NodeFilterTableModel) table.getModel()).getNodeFilter().remove(r);
              ((NodeFilterTableModel) table.getModel()).fireTableRowsDeleted(r,r);
               if(r> 0)
                table.setRowSelectionInterval(r-1, r-1);
              else if(table.getRowCount() > 0)
                  table.setRowSelectionInterval(0,0);
          }
      }
    
}
