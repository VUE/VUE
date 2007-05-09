
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.compare.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.*;

//import java.net.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import tufts.vue.*;

/*
 * MapsSelectionPanel.java
 *
 * Created on May 3, 2007, 11:17 AM
 *
 * @author dhelle01
 */
public class MapsSelectionPanel extends JPanel  {
    
    // probably should save settings when load LWMergeMap
    // so perhaps a second instance is warranted dependent on 
    // currently active map.
    private static MapsSelectionPanel singleton = new MapsSelectionPanel();
    
    public static MapsSelectionPanel getMapSelectionPanel()
    {
        return singleton;
    }
    
    public static final int OPEN_MAP = 0;
    public static final int LOCAL_FILE = 1;
    
    private JTable maps;
    private JTextField fileName;
    private JButton browseButton;
    
    private MapsSelectionPanel() 
    {
        setOpaque(false);
       // setBackground(java.awt.Color.BLUE);
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        setLayout(gridBag);
        
        fileName = new JTextField(20);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               JFileChooser choose = new JFileChooser();
               choose.showOpenDialog(MapsSelectionPanel.this);
               String name = choose.getSelectedFile().getName();
               ((MapTableModel)maps.getModel()).addRow(name);
               fileName.setText(name);
               revalidate();
           }
        });
        VUE.getTabbedPane().addContainerListener(new java.awt.event.ContainerListener()
        {
            public void componentAdded(java.awt.event.ContainerEvent e)
            {
                System.out.println("MSP: VUE tabbed pane component added " + e);
                revalidate();
            }
            
            public void componentRemoved(java.awt.event.ContainerEvent e)
            {
                System.out.println("MSP: VUE tabbed pane component removed " + e);
                revalidate();
            }
        });
        VUE.getTabbedPane().addChangeListener(new ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent e)
            {
                System.out.println("MSP: VUE tabbed pane change event " + e);
            }
        });
        
        VUE.getTabbedPane().addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                revalidate();
                repaint();
                System.out.println("MSP: VUE tabbed pane property change event " + e.getPropertyName());
            }
        });
        //VUE.getMainWindow().addPropertyChangeListener()
        
        //gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(fileName,gridBagConstraints);
        add(fileName);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(browseButton,gridBagConstraints);
        add(browseButton);
        maps = new JTable(new MapTableModel());
        maps.addMouseListener(new java.awt.event.MouseAdapter() {
          public void mousePressed(java.awt.event.MouseEvent e)
          {
            System.out.println("MSP mouse pressed, row column: " + maps.getSelectedRow() + "," + maps.getSelectedColumn());
            if(maps.getSelectedColumn() == 0)
            {
                if(((MapTableModel)maps.getModel()).isSelected(maps.getSelectedRow()))
                    ((MapTableModel)maps.getModel()).setSelected(false,maps.getSelectedRow());
                else
                    ((MapTableModel)maps.getModel()).setSelected(true,maps.getSelectedRow());
            }
            repaint();
          }
        });
        maps.setDefaultRenderer(Object.class,new MapTableCellRenderer());
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBag.setConstraints(maps,gridBagConstraints);
        add(maps);
    }
    
    class MapTableModel implements TableModel
    {
       private ArrayList localFiles = new ArrayList(); 
       private ArrayList localFileSelectionStates = new ArrayList();
       private HashMap openMapSelections = new HashMap();
       
       //private JCheckBox checkBox = new JCheckBox();
       
       /**
        *
        * listeners might not see all changes on this model as we
        * are not currently listening to changes in the vue tabs.
        *
        **/
       public void removeTableModelListener(TableModelListener listener)
       {
           
       }
       
       /**
        *
        * listeners might not see all changes on this model as we
        * are not currently listening to changes in the vue tabs.
        *
        *
        **/
       public void addTableModelListener(TableModelListener listener)
       {
           
       }
       
       /**
        *
        * does nothing if row >= getFirstOpenMapRow()
        *
        **/
       public void setValueAt(Object value,int row,int col)
       {
           
       }
       
       /**
        *
        * Postcondition: list is still in order
        * local files | open maps
        *
        * merge maps should not be added? (how detect this
        * before doing the merge?)
        *
        **/
       public void addRow(String fileName)
       {
           localFileSelectionStates.add("Selected");
           localFiles.add(fileName);
       }
       
       /**
        *
        * Only guaranteed to work correctly
        * if getMapType(int row) is currently 
        * accurate when called.
        *
        * This should always be true if the preconditions
        * for getMapType are met and the open
        * file list is not modified during this call. 
        *
        **/
       public boolean isSelected(int row)
       {
           if(getMapType(row) == OPEN_MAP)
           {
               LWMap map = getMap(row);
               String selected = (String)openMapSelections.get(getMap(row));
               if(selected == null || selected.equals("Selected"))
                   return true;
               else
                 return false;
           }
           else
           {
               if(localFileSelectionStates.get(row).equals("Selected"))
               {
                   return true;
               }
               else
               {
                   return false;
               }
           }
           
           //return true;
       }
       
       public void setSelected(boolean select,int row)
       {
           if(getMapType(row) == OPEN_MAP)
           {
               String selected = (String)openMapSelections.get(getMap(row));
               if(selected != null && selected.equals("Selected"))
                 openMapSelections.put(getMap(row),"Not Selected");
               else
                 openMapSelections.put(getMap(row),"Selected");
           }
           else
           {
               if(select)
                   localFileSelectionStates.set(row,"Selected");
               else
                   localFileSelectionStates.set(row,"UnSelected");
           }
       }
       
       /**
        *
        *  Gets the map from the Left Tabbed Pane
        *  if the map type is correct.
        *
        *  If row contains a file type or tab contains
        *  an LWMerge Map, return null
        *
        *  to work correctly this function 
        *  presupposes that list is stored in order:
        *  local files | open files
        *
        **/
       public LWMap getMap(int row)
       {
           LWMap map = null;
           int openMapIndex = row-getFirstOpenMapRow()-1;
           if(openMapIndex < VUE.getLeftTabbedPane().getTabCount() && openMapIndex > -1)
                map =  VUE.getLeftTabbedPane().getMapAt(openMapIndex);
                // ****need list here, not iterator? ***** //map =  VUE.getLeftTabbedPane().getAllMaps().get(openMapIndex);
           if(map instanceof LWMergeMap)
               map = null;
           return map;
           //return null;
       }
       
       /**
        *
        * presupposes that list is stored in order:
        * local files | open files
        *
        **/
       public int getMapType(int row)
       {
           if(row<getFirstOpenMapRow())
               return LOCAL_FILE;
           else
               return OPEN_MAP;
       }
       
       /**
        *
        * presupposes that list is stored in order:
        * local files | open files
        *
        **/
       private int getFirstOpenMapRow()
       {
           return localFiles.size();
       }
       
       public Object getValueAt(int row,int col)
       {
           if(col == 0)
           {
               return "Selected";
           }
           if(col == 1)
           {
               if(row<getFirstOpenMapRow())
               {
                   return localFiles.get(row);
               }
               else
               {
                   return VUE.getLeftTabbedPane().getMapAt(row-getFirstOpenMapRow()).getLabel();
               }
           }
           else if(col == 3)
           {
               if(getMapType(row) == OPEN_MAP)
               {
                   return "Open";
               }
               else
               {
                   return "Local File";
               }
           }
           else return "TBD";
       }
       
       public boolean isCellEditable(int row,int col)
       {
           return false;
       }
       
       public Class getColumnClass(int col)
       {
           if(col == 0)
               return JCheckBox.class;
           else
               return String.class;
       }
       
       public String getColumnName(int col)
       {
           return null;
       }
       
       public int getColumnCount()
       {
           return 4;
       }
       
       public int getRowCount()
       {
           return VUE.getLeftTabbedPane().getTabCount() + localFiles.size();
       }
       
    }
    
    class MapTableCellRenderer extends DefaultTableCellRenderer
    {
        private JCheckBox checkBox = new JCheckBox();
        private JLabel label = new JLabel();
        
        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
        {
            if(col !=0)
            {
                label.setText(value.toString());
                return label;
            }
            else
            {
                if(((MapTableModel)table.getModel()).isSelected(row))
                    checkBox.setSelected(true);
                else
                    checkBox.setSelected(false);
                return checkBox;
            }
        }
    }
     
}
