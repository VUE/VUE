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


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;

import java.util.*;
import java.util.ArrayList;

public class MapFilterModelEditor extends JPanel {
    MapFilterTableModel mapFilterTableModel;
    boolean editable = false;
    
    /** Creates a new instance of MapFilterModelEditor */
    public MapFilterModelEditor(MapFilterModel mapFilterModel) {
        mapFilterTableModel = new MapFilterTableModel(mapFilterModel,editable);
        setMapFilterModelPanel();
    }
    
    private void setMapFilterModelPanel() {
        JTable mapFilterTable = new JTable(mapFilterTableModel);
        mapFilterTable.setPreferredScrollableViewportSize(new Dimension(200,100));
        JScrollPane mapFilterScrollPane=new JScrollPane(mapFilterTable);
        mapFilterScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JPanel  mapFilterPanel=new JPanel();
        mapFilterPanel.setLayout(new BorderLayout());
        mapFilterPanel.add( mapFilterScrollPane, BorderLayout.CENTER);
        mapFilterPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        
        // GRID: addConditionButton
        JButton addButton=new tufts.vue.VueButton("add");
        addButton.addActionListener(new AddButtonListener(mapFilterTableModel));
        
        
        // GRID: deleteConditionButton
        JButton deleteButton=new tufts.vue.VueButton("delete");
        deleteButton.setEnabled(false);

        //setting the listeners
         
        JPanel innerPanel=new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(2,6,6,6));
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        
        
        
        //innerPanel.add(labelPanel);
        innerPanel.add(mapFilterPanel);
        innerPanel.add(bottomPanel);
        
        add(innerPanel);
        //setSize(300, 300);
        
        validate();
        
    }
    public class MapFilterTableModel extends AbstractTableModel {
        
        boolean editable;
        MapFilterModel mapFilterModel;
        
        public MapFilterTableModel(MapFilterModel mapFilterModel,boolean editable) {
            this.editable = editable;
            this.mapFilterModel = mapFilterModel;
        }
        
        public MapFilterModel getMapFilterModel() {
            return this.mapFilterModel;
        }
        
        public void addKey(Key key) {
            mapFilterModel.add(key);
        }
        
        public boolean isEditable() {
            return editable;
        }
        
        public String getColumnName(int col) {
            if (col==0) {
                return "Field";
            } else {
                return "Type";
            }
        }
        
        public int getRowCount() {
            return mapFilterModel.size();
        }
        
        public int getColumnCount() {
            return 2;
        }
        
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        public Object getValueAt(int row, int col) {
            Key  key=(Key) mapFilterModel.get(row);
            if (col==0)
                return key.getKey().toString();
            else
                return key.getType().getDisplayName();
            
        }
        public void setValueAt(Object value, int row, int col) {
           
            Key key = (Key)mapFilterModel.get(row);
            if(col == 0)
                key.setKey((String)value);
            // row = -1 adds new condions else replace the existing one.
            
           fireTableCellUpdated(row, col);
        }
    
        
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
          //  return editable;
            if(col == 0) 
                return true;
            else 
                return false;
        }
    }
    
    public class AddButtonListener implements ActionListener {

        private  MapFilterTableModel model;

        public AddButtonListener(MapFilterTableModel model) {
            this.model=model;
        }

        public void actionPerformed(ActionEvent e) {
            //m_model.addProperty(DC_FIELDS[0], "");
           AddDialog addDialog = new AddDialog(model);
        
        }
          
    }
    
    public class AddDialog extends JDialog {
        MapFilterTableModel model;
        JLabel keyLabel;
        JLabel typeLabel;
        JTextField keyEditor;
        JComboBox typeEditor;
        Vector allTypes;
        
        public AddDialog(MapFilterTableModel model) {
            super(tufts.vue.VUE.getInstance(),"Add Key",true);
            this.model = model;
            allTypes = (Vector)TypeFactory.getAllTypes();
            keyLabel = new JLabel("Field");
            typeLabel = new JLabel("Type");
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
            BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
            
          
           
            getContentPane().setLayout(layout);
            getContentPane().add(keyPanel);
            getContentPane().add(typePanel);
            getContentPane().add(southPanel);
            pack();
            setLocation(500,300);
            show();
            
        }
        
        private void updateModelAndNotify(){
            Key key = new Key(keyEditor.getText(),(Type)typeEditor.getSelectedItem());
            model.addKey(key);
            model.fireTableDataChanged();
        }
    }
}
