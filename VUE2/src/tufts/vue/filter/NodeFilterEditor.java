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
    NodeFilter nodeFilter;
    boolean editable = false;
    JComboBox keyEditor;
    JComboBox operatorEditor;
    JTable nodeFilterTable;
    AddButtonListener addButtonListener = null;
    DeleteButtonListener deleteButtonListener = null;
    NodeFilterSelectionListener sListener = null;
    JButton addButton=new tufts.vue.VueButton("add");
    JButton deleteButton=new tufts.vue.VueButton("delete");
    
    /** Creates a new instance of NodeFilterEditor */
    public NodeFilterEditor( NodeFilter nodeFilter,boolean editable) {
        System.out.println("Created new instance of node filter editor");
        this.nodeFilter = nodeFilter;
        setNodeFilterPanel();
    }
    
    public NodeFilterEditor( NodeFilter nodeFilter) {
        this.nodeFilter = nodeFilter;
        setNodeFilterPanel();
    }
    
    private void setNodeFilterPanel() {
        nodeFilterTable = new JTable(nodeFilter);
        nodeFilterTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        JScrollPane nodeFilterScrollPane=new JScrollPane(nodeFilterTable);
        nodeFilterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  nodeFilterPanel=new JPanel();
        nodeFilterPanel.setLayout(new BorderLayout());
        nodeFilterPanel.add( nodeFilterScrollPane, BorderLayout.CENTER);
        nodeFilterPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        // GRID: addConditionButton
        addButtonListener = new AddButtonListener(nodeFilter);
        addButton.addActionListener(addButtonListener);
        
        
        // GRID: deleteConditionButton
        
        deleteButton.setEnabled(false);
        // adding the delete functionality */
        sListener= new NodeFilterSelectionListener(deleteButton, -1);
        nodeFilterTable.getSelectionModel().addListSelectionListener(sListener);
        deleteButtonListener = new DeleteButtonListener(nodeFilterTable, sListener);
        deleteButton.addActionListener(deleteButtonListener);
        
        /** setting editors ***/
        keyEditor = new JComboBox(tufts.vue.VUE.getActiveMap().getMapFilterModel().getKeyVector());
        nodeFilterTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(keyEditor));
        //operatorEditor = new JComboBox((Vector)((Key)keyEditor.getItemAt(0)).getType().getOperators());
        nodeFilterTable.getColumnModel().getColumn(NodeFilter.OPERATOR_COL).setCellEditor(new OperatorCellEditor());
        
        
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
    
    public void setNodeFilter(NodeFilter nodeFilter)  {
        nodeFilterTable.setModel(nodeFilter);
        addButton.removeActionListener(addButtonListener);
        addButtonListener = new AddButtonListener(nodeFilter);
        addButton.addActionListener(addButtonListener);
        deleteButton.removeActionListener(deleteButtonListener);
        deleteButtonListener = new DeleteButtonListener(nodeFilterTable, sListener);
        deleteButton.addActionListener(deleteButtonListener);
    }
    
    public class AddButtonListener implements ActionListener {
        private  NodeFilter model;
        public AddButtonListener(NodeFilter model) {
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
            if (tableModel instanceof NodeFilter) {
                NodeFilter nodeFilter  = (NodeFilter) tableModel;
                editor =  new JComboBox((Vector)((Statement)(nodeFilter.get(row))).getKey().getType().getSettableOperators());
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
    
    public class KeyCellEditor extends DefaultCellEditor {
        JComboBox editor = null;
        public KeyCellEditor() {
            super(new JTextField(""));
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            TableModel tableModel = table.getModel();
            if (tableModel instanceof NodeFilter) {
                editor = new JComboBox((Vector)tufts.vue.VUE.getActiveMap().getMapFilterModel().getKeyVector());
                return editor;
            }
            return (new JTextField(""));// if no editor present
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
        NodeFilter model;
        JLabel keyLabel;
        JLabel typeLabel;
        JLabel operatorLabel;
        JLabel valueLabel;
        JComboBox keyEditor;
        JComboBox operatorEditor;
        JTextField valueEditor;
        JTextField typeEditor;
        
        Vector allTypes;
        
        public AddDialog(NodeFilter model) {
            super(tufts.vue.VUE.getInstance(),"Add Key",true);
            this.model = model;
            allTypes = (Vector)TypeFactory.getAllTypes();
            keyLabel = new JLabel("Key");
            typeLabel = new JLabel("Type");
            operatorLabel = new JLabel("Operator");
            valueLabel = new JLabel("Value");
            keyEditor = new JComboBox(tufts.vue.VUE.getActiveMap().getMapFilterModel().getKeyVector());
            
            
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
            ((NodeFilter) table.getModel()).remove(r);
            ((NodeFilter) table.getModel()).fireTableRowsDeleted(r,r);
            if(r> 0)
                table.setRowSelectionInterval(r-1, r-1);
            else if(table.getRowCount() > 0)
                table.setRowSelectionInterval(0,0);
        }
    }
    
}
