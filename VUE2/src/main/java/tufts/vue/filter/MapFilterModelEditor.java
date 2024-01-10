/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
 * MapFilterModelEditor.java
 *
 * Created on February 15, 2004, 7:10 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tufts.vue.VueResources;

public class MapFilterModelEditor extends JPanel {
    public static final String MAP_FILTER_INFO = tufts.vue.VueResources.getString("info.filter.map");

    MapFilterModel mapFilterModel;
    JTable mapFilterTable;
    AddButtonListener addButtonListener = null;
    DeleteButtonListener deleteButtonListener = null;
    MapFilterModelSelectionListener sListener = null;
    boolean editable = false;
    JButton addButton=new tufts.vue.gui.VueButton(VueResources.getString("button.add.label"));
    JButton deleteButton=new tufts.vue.gui.VueButton(VueResources.getString("button.delete.label"));
    JLabel questionLabel = new JLabel(tufts.vue.VueResources.getImageIcon("smallInfo"), JLabel.LEFT);

    /** Creates a new instance of MapFilterModelEditor */
    public MapFilterModelEditor(MapFilterModel mapFilterModel) {
        this.mapFilterModel = mapFilterModel;
        questionLabel.setToolTipText(MAP_FILTER_INFO);
        setMapFilterModelPanel();

    }
    private void setMapFilterModelPanel() {
        addButton.setToolTipText(VueResources.getString("mapfiltermodelpanel.addbutton.tooltip"));
        deleteButton.setToolTipText(VueResources.getString("mapfiltermodelpanel.delbutton.tooltip"));
        mapFilterTable = new JTable(mapFilterModel);
        mapFilterTable.addFocusListener(new FocusListener() {
             public void focusLost(FocusEvent e) {
                 if(mapFilterTable.isEditing()) {
                     mapFilterTable.getCellEditor(mapFilterTable.getEditingRow(),mapFilterTable.getEditingColumn()).stopCellEditing();
                 }
                 mapFilterTable.removeEditor();
             }
             public void focusGained(FocusEvent e) {
             }
         });
        mapFilterTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        JScrollPane mapFilterScrollPane=new JScrollPane(mapFilterTable);
        mapFilterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  mapFilterPanel=new JPanel();
        mapFilterPanel.setLayout(new BorderLayout());
        mapFilterPanel.add( mapFilterScrollPane, BorderLayout.CENTER);
        //mapFilterPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        // addConditionButton
        addButtonListener = new AddButtonListener(mapFilterModel);
        addButton.addActionListener(addButtonListener);

        sListener= new MapFilterModelSelectionListener(deleteButton, -1);
        mapFilterTable.getSelectionModel().addListSelectionListener(sListener);
        deleteButtonListener = new DeleteButtonListener(mapFilterTable, sListener);
        deleteButton.addActionListener(deleteButtonListener);

        deleteButton.setEnabled(false);
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        //innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        //bottomPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(questionLabel);
        //innerPanel.add(labelPanel);
        innerPanel.add(bottomPanel);

        innerPanel.add(mapFilterPanel);
        setLayout(new BorderLayout());
        add(innerPanel,BorderLayout.CENTER);
        //setSize(300, 300);
        validate();

    }

    public void setMapFilterModel(MapFilterModel mapFilterModel) {
        this.mapFilterModel = mapFilterModel;
        mapFilterTable.setModel(mapFilterModel);
        addButton.removeActionListener(addButtonListener);
        addButtonListener = new AddButtonListener(mapFilterModel);
        addButton.addActionListener(addButtonListener);
        deleteButton.removeActionListener(deleteButtonListener);
        deleteButtonListener = new DeleteButtonListener(mapFilterTable, sListener);
        deleteButton.addActionListener(deleteButtonListener);
    }


    public class AddButtonListener implements ActionListener {
        private  MapFilterModel model;
        public AddButtonListener(MapFilterModel model) {
            this.model=model;
        }
        public void actionPerformed(ActionEvent e) {
            AddDialog addDialog = new AddDialog(model);
        }
    }

    public class AddDialog extends JDialog {
        MapFilterModel model;
        JLabel keyLabel;
        JLabel typeLabel;
        JTextField keyEditor;
        JComboBox typeEditor;
        Vector allTypes;

        public AddDialog(MapFilterModel model) {
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
            setLocation(MapFilterModelEditor.this.getLocationOnScreen());
            setVisible(true);

        }

        private void updateModelAndNotify(){
            tufts.vue.filter.Key key = new tufts.vue.filter.Key(keyEditor.getText(),(tufts.vue.filter.Type)typeEditor.getSelectedItem());
            model.addKey(key);
            System.out.println("ADDED KEY of Type = "+((tufts.vue.filter.Type)typeEditor.getSelectedItem()).getDisplayName());
            model.fireTableDataChanged();
        }
    }

    public class MapFilterModelSelectionListener  implements ListSelectionListener {
        private int m_selectedRow;
        private JButton m_deleteButton;

        public MapFilterModelSelectionListener(JButton deleteButton, int selectedRow) {
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
        private MapFilterModelSelectionListener m_sListener;

        public DeleteButtonListener(JTable table,MapFilterModelSelectionListener sListener) {
            this.table = table;
            m_sListener=sListener;
        }

        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            if (JOptionPane
                .showConfirmDialog(tufts.vue.VUE.getDialogParentAsFrame(),
                                   VueResources.getString("dialog.delcustommetdata.message"),
                                   VueResources.getString("dialog.delcustommetdata.title"),
                                   JOptionPane.YES_NO_OPTION,
                                   JOptionPane.QUESTION_MESSAGE,
                                   tufts.vue.VueResources.getImageIcon("vueIcon32x32")) == JOptionPane.YES_OPTION) {
                int r=m_sListener.getSelectedRow();
                ((MapFilterModel) table.getModel()).remove(r);
                ((MapFilterModel) table.getModel()).fireTableRowsDeleted(r,r);
                if(r> 0)
                    table.setRowSelectionInterval(r-1, r-1);
                else if(table.getRowCount() > 0)
                    table.setRowSelectionInterval(0,0);
            }
        }
    }
}
