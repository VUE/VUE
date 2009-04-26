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
 * NodeFilterEditor.java
 * This is the panel to edit nodeFilters.  Note Filters are called custom metadata Elements.
 * The panel is the view component for NodeFilter. It renders them and enables addition, deletion
 *  of  filters to Map Element
 * Created on February 16, 2004, 2:10 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import tufts.vue.VueResources;

public class NodeFilterEditor extends JPanel implements MapFilterModel.Listener,tufts.vue.ActiveListener<tufts.vue.LWMap> {
    public static final String ADD_ELEMENT = VueResources.getString("nodefilter.definenew.label");
    public static final String SELECT_ELEMENT = VueResources.getString("nodefilter.select.label");
    public static final String NODE_FILTER_INFO = tufts.vue.VueResources.getString("info.filter.node");
    NodeFilter nodeFilter;
    boolean editable = false;
    KeyCellEditor keyEditor;
    OperatorCellEditor operatorEditor;
    JTable nodeFilterTable;
    AddButtonListener addButtonListener = null;
    DeleteButtonListener deleteButtonListener = null;
    NodeFilterSelectionListener sListener = null;
    JButton addButton=new tufts.vue.gui.VueButton(VueResources.getString("button.add.label"));
    JButton deleteButton=new tufts.vue.gui.VueButton(VueResources.getString("button.delete.label"));
    JLabel questionLabel = new JLabel(tufts.vue.VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
    JLabel elementLabel = new JLabel(VueResources.getString("nodefilter.selectelement.label"));
    JComboBox elements;
    ElementComboBoxModel elementsModel;
    Vector elementsVector;
    private tufts.vue.LWMap lWMap = null; // for reference;
    /** Creates a new instance of NodeFilterEditor */
    public NodeFilterEditor( NodeFilter nodeFilter,boolean editable) {
        this.nodeFilter = nodeFilter;
        tufts.vue.LWMap map = tufts.vue.VUE.getActiveMap();
        if (map != null) 
            map.getMapFilterModel().addListener(this);
        tufts.vue.VUE.addActiveListener(tufts.vue.LWMap.class, this);
        questionLabel.setToolTipText(NodeFilterEditor.NODE_FILTER_INFO);
        setNodeFilterPanel();
        
    }
    
    public NodeFilterEditor( NodeFilter nodeFilter) {
        this(nodeFilter,true);
        
    }
    public void mapFilterModelChanged(MapFilterModelEvent e) {
        elementsModel.setElements(e.getMapFilterModel().getKeyVector());
        nodeFilter.fireTableDataChanged();
    }
    
    private void setNodeFilterPanel() {
        lWMap= tufts.vue.VUE.getActiveMap();
        addButton.setToolTipText(VueResources.getString("nodefilter.addfilter.tooltip"));
        deleteButton.setToolTipText(VueResources.getString("nodefilter.deletefilter.tooltip"));
        elementsModel = new ElementComboBoxModel();
        elementsModel.setElements(lWMap.getMapFilterModel().getKeyVector());
        elements = new JComboBox(elementsModel);
        Font f = elements.getFont();
        Font menuFont = new Font(f.getFontName(), f.getStyle(), f.getSize() - 2);
        elements.setFont(menuFont);
        //elements.setPreferredSize(new Dimension(150,20));
        /// this is not user friendly may want to fix later.
        /**
         * elements.addItemListener(new ItemListener() {
         * public void itemStateChanged(ItemEvent e) {
         * if(e.getStateChange() == ItemEvent.SELECTED) {
         * if(e.getItem().toString() == ADD_ELEMENT) {
         * } else if(e.getItem() instanceof Key) {
         * Statement stmt = new Statement();
         * stmt.setKey((Key)e.getItem());
         * nodeFilter.addStatement(stmt);
         * nodeFilter.fireTableDataChanged();
         * } else {
         * System.out.println("Not Supported");
         * }
         * }
         * }
         * });
         **/
        
        nodeFilterTable = new JTable(nodeFilter);
        nodeFilterTable.addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                if(nodeFilterTable.isEditing()) {
                    nodeFilterTable.getCellEditor(nodeFilterTable.getEditingRow(),nodeFilterTable.getEditingColumn()).stopCellEditing();
                }
                nodeFilterTable.removeEditor();
            }
            public void focusGained(FocusEvent e) {
            }
        });
        nodeFilterTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(nodeFilterTable.getSelectedRow() == (nodeFilterTable.getRowCount()-1) && e.getKeyCode() == e.VK_ENTER){
                    addButtonListener.addStatement();
                }
            }
        });
        
        nodeFilterTable.setPreferredScrollableViewportSize(new Dimension(240,100));
        JScrollPane nodeFilterScrollPane=new JScrollPane(nodeFilterTable);
        nodeFilterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  nodeFilterPanel=new JPanel();
        nodeFilterPanel.setLayout(new BorderLayout());
        nodeFilterPanel.add( nodeFilterScrollPane, BorderLayout.CENTER);
        addButtonListener = new AddButtonListener(nodeFilter);
        addButton.addActionListener(addButtonListener);
        deleteButton.setEnabled(false);
        sListener= new NodeFilterSelectionListener(deleteButton, -1);
        nodeFilterTable.getSelectionModel().addListSelectionListener(sListener);
        deleteButtonListener = new DeleteButtonListener(nodeFilterTable, sListener);
        deleteButton.addActionListener(deleteButtonListener);
        
        //keyEditor = new KeyCellEditor();
        //nodeFilterTable.getColumnModel().getColumn(NodeFilter.KEY_COL).setCellEditor(keyEditor);
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        //bottomPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        bottomPanel.add(elementLabel);
        bottomPanel.add(elements);
        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(questionLabel);
        //innerPanel.add(labelPanel);
        innerPanel.add(bottomPanel);
        innerPanel.add(nodeFilterPanel);
        setLayout(new BorderLayout());
        add(innerPanel,BorderLayout.CENTER);
        
        validate();
    }
    
    public void setNodeFilter(NodeFilter nodeFilter)  {
        this.nodeFilter = nodeFilter;
        nodeFilterTable.setModel(nodeFilter);
        addButton.removeActionListener(addButtonListener);
        addButtonListener = new AddButtonListener(nodeFilter);
        addButton.addActionListener(addButtonListener);
        deleteButton.removeActionListener(deleteButtonListener);
        deleteButtonListener = new DeleteButtonListener(nodeFilterTable, sListener);
        deleteButton.addActionListener(deleteButtonListener);
        keyEditor = new KeyCellEditor();
        nodeFilterTable.getColumnModel().getColumn(NodeFilter.KEY_COL).setCellEditor(keyEditor);
    }
    
    public void activeChanged(tufts.vue.ActiveEvent<tufts.vue.LWMap> e) {
        this.lWMap = e.active;
        if (lWMap != null) {
            lWMap.getMapFilterModel().addListener(this);
            elementsModel.setElements(lWMap.getMapFilterModel().getKeyVector());
        }
    }
    
    public class ElementComboBoxModel extends DefaultComboBoxModel {
        public void setElements(Vector v) {
            removeAllElements();
            Iterator i = v.iterator();
            while(i.hasNext()) {
                addElement(i.next());
            }
            addElement(new String(ADD_ELEMENT)) ;
            fireContentsChanged(this,0,v.size()+1);
        }
    }
    
    public class AddButtonListener implements ActionListener {
        private  NodeFilter model;
        public AddButtonListener(NodeFilter model) {
            this.model=model;
        }
        
        public void actionPerformed(ActionEvent e) {
            addStatement();
            //AllFilterDialog aDilaog  = new AllFilterDialog();
        }
        
        void addStatement() {
            Object obj = elements.getSelectedItem();
            if(obj.toString().equals(ADD_ELEMENT)) {
                AddElementDialog addDialog = new AddElementDialog(lWMap.getMapFilterModel());
            } else if(obj instanceof Key) {
                Statement stmt = new Statement();
                stmt.setKey((Key)obj);
                nodeFilter.addStatement(stmt);
                nodeFilter.fireTableDataChanged();
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
            super(new JComboBox());
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            
            TableModel tableModel = table.getModel();
            if (tableModel instanceof NodeFilter) {
                NodeFilter nodeFilter  = (NodeFilter) tableModel;
                editor =  new JComboBox((Vector)((Statement)(nodeFilter.get(row))).getKey().getType().getSettableOperators());
                return editor;
            }
            return (new JComboBox());
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
            super(new JComboBox());
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            TableModel tableModel = table.getModel();
            if (tableModel instanceof NodeFilter) {
                editor = new JComboBox((Vector)tufts.vue.VUE.getActiveMap().getMapFilterModel().getKeyVector());
                return editor;
            }
            return (new JComboBox());// if no editor present
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
            super(tufts.vue.VUE.getDialogParentAsFrame(),VueResources.getString("dialog.addkey.title"),true);
            this.model = model;
            allTypes = (Vector)TypeFactory.getAllTypes();
            keyLabel = new JLabel(VueResources.getString("button.key.label"));
            typeLabel = new JLabel(VueResources.getString("nodefilter.type.label"));
            operatorLabel = new JLabel(VueResources.getString("nodefilter.operator.label"));
            valueLabel = new JLabel(VueResources.getString("nodefilter.value.label"));
            keyEditor = new JComboBox(tufts.vue.VUE.getActiveMap().getMapFilterModel().getKeyVector());
            
            
            operatorEditor = new JComboBox();
            valueEditor = new JTextField();
            typeEditor = new JTextField();
            typeEditor.setEditable(false);
            
            keyEditor.setPreferredSize(new Dimension(80,20));
            
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
            
            JButton okButton=new JButton(VueResources.getString("button.ok.label"));
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateModelAndNotify();
                    setVisible(false);
                }
            });
            
            JButton cancelButton=new JButton(VueResources.getString("button.cancel.lable"));
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
            setVisible(true);
            
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
            if(r> 0)
                table.setRowSelectionInterval(r-1, r-1);
            else if(table.getRowCount() > 0)
                table.setRowSelectionInterval(0,0);
        }
    }
    public class AddElementDialog extends JDialog {
        MapFilterModel model;
        JLabel keyLabel;
        JLabel typeLabel;
        JTextField keyEditor;
        JComboBox typeEditor;
        Vector allTypes;
        
        public AddElementDialog(MapFilterModel model) {
            super(tufts.vue.VUE.getDialogParentAsFrame(),VueResources.getString("dialog.addkey.title"),true);
            this.model = model;
            allTypes = (Vector)TypeFactory.getAllTypes();
            keyLabel = new JLabel(VueResources.getString("nodefilter.field.label"));
            typeLabel = new JLabel(VueResources.getString("nodefilter.type.label"));
            keyEditor = new JTextField();
            typeEditor = new JComboBox(allTypes);
            keyEditor.setPreferredSize(new Dimension(80,20));
            JPanel keyPanel=new JPanel();
            keyPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            keyPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            keyPanel.add(keyLabel);
            keyPanel.add(keyEditor);
            
            JPanel typePanel=new JPanel();
            typePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            typePanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            typePanel.add(typeLabel);
            typePanel.add(typeEditor);
            
            // SOUTH: southPanel(cancelButton, okButton)
            
            JButton okButton=new JButton(VueResources.getString("button.ok.label"));
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateModelAndNotify();
                    setVisible(false);
                }
            });
            
            JButton cancelButton=new JButton(VueResources.getString("button.cancel.lable"));
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            
            JPanel southPanel=new JPanel();
            southPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            southPanel.add(okButton);
            southPanel.add(cancelButton);
            BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
            
            
            
            getContentPane().setLayout(layout);
            getContentPane().add(keyPanel);
            getContentPane().add(typePanel);
            getContentPane().add(southPanel);
            pack();
            setLocation(NodeFilterEditor.this.getLocationOnScreen());
            setVisible(true);
            
        }
        
        private void updateModelAndNotify(){
            Key key = new Key(keyEditor.getText(),(Type)typeEditor.getSelectedItem());
            model.addKey(key);
            Statement stmt = new Statement();
            stmt.setKey(key);
            nodeFilter.addStatement(stmt);
            nodeFilter.fireTableDataChanged();
            System.out.println("ADDED KEY of Type = "+((Type)typeEditor.getSelectedItem()).getDisplayName());
            model.fireTableDataChanged();
        }
    }
}
