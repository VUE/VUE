
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
 **********************************************
 *
 * MapsSelectionPanel.java
 *
 * Created on May 3, 2007, 11:17 AM
 *
 * @version $Revision: 1.8 $ / $Date: 2007-05-21 18:58:49 $ / $Author: dan $
 * @author dhelle01
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import tufts.vue.*;
import tufts.vue.action.ActionUtil;

public class MapsSelectionPanel extends JPanel  {
    
    // probably should save settings when load LWMergeMap
    // so perhaps a second instance is warranted depending on 
    // currently active map.
    private static MapsSelectionPanel singleton = new MapsSelectionPanel();
    
    public static MapsSelectionPanel getMapSelectionPanel()
    {
        return singleton;
    }
    
    public static final int OPEN_MAP = 0;
    public static final int LOCAL_FILE = 1;
    
    public static final String stepOneMessage = "1. Create a set of maps from currently open maps and/or from maps stored on your computer";
    public static final String stepTwoMessage = "2. Pick a \"guide\" map to define the layout of the new merged map";
    
    // originally from Visualization Settings Panel:
    public final static String filterOnBaseMapMessageString = "Only include items found on the layout map";
    
    private JScrollPane scroll;
    private JTable maps;
    private JTextField fileName;
    private JButton browseButton;
    
    private JPanel bottomPanel;
    private JCheckBox filterOnBaseMap;
    
    private MapsSelectionPanel() 
    {
        setOpaque(false);
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
               String name = choose.getSelectedFile().getAbsolutePath();//.getName();
               ((MapTableModel)maps.getModel()).addRow(name);
               fileName.setText(name);
               revalidate();
               scroll.getViewport().revalidate();
           }
        });
        VUE.getTabbedPane().addContainerListener(new java.awt.event.ContainerListener()
        {
            public void componentAdded(java.awt.event.ContainerEvent e)
            {
                //System.out.println("MSP: VUE tabbed pane component added " + e);
                revalidate();
            }
            
            public void componentRemoved(java.awt.event.ContainerEvent e)
            {
                //System.out.println("MSP: VUE tabbed pane component removed " + e);
                revalidate();
            }
        });
        VUE.getTabbedPane().addChangeListener(new ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent e)
            {
                //System.out.println("MSP: VUE tabbed pane change event " + e);
            }
        });
        
        VUE.getTabbedPane().addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                revalidate();
                repaint();
                //System.out.println("MSP: VUE tabbed pane property change event " + e.getPropertyName());
            }
        });

        JLabel stepOneLabel = new JLabel(stepOneMessage);
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(stepOneLabel,gridBagConstraints);
        stepOneLabel.setBorder(BorderFactory.createEmptyBorder(15,0,0,0));
        add(stepOneLabel);
        
        JLabel stepTwoLabel = new JLabel(stepTwoMessage);
        //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(stepTwoLabel,gridBagConstraints);
        stepTwoLabel.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        add(stepTwoLabel);  
               
        bottomPanel = new JPanel();
        filterOnBaseMap = new JCheckBox();
        JLabel filterOnBaseMapMessage = new JLabel(filterOnBaseMapMessageString);
        //filterOnBaseMapMessage.setOpaque(false);
        bottomPanel.setOpaque(false);
        bottomPanel.add(filterOnBaseMap);
        bottomPanel.add(filterOnBaseMapMessage);
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(bottomPanel,gridBagConstraints);
        add(bottomPanel);
        
        JLabel selectMapsLabel = new JLabel("Select maps:");
        selectMapsLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        gridBagConstraints.gridwidth = 1;
        gridBag.setConstraints(selectMapsLabel,gridBagConstraints);
        add(selectMapsLabel);
        
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(fileName,gridBagConstraints);
        //fileName.setBorder(BorderFactory.createEmptyBorder(0,0,5,5));
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
            if(maps.getSelectedColumn() == 2)
            {
                ((MapTableModel)maps.getModel()).setBaseMapIndex(maps.getSelectedRow());
            }
            repaint();
          }
        });
        maps.setDefaultRenderer(Object.class,new MapTableCellRenderer());
        maps.getColumnModel().getColumn(0).setMaxWidth(50);
        maps.getColumnModel().getColumn(2).setMaxWidth(100);
        maps.getColumnModel().getColumn(3).setMaxWidth(100);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.fill = gridBagConstraints.BOTH;
        scroll = new JScrollPane(maps);
        gridBag.setConstraints(scroll,gridBagConstraints);
        add(scroll);
        
    }
    
    /**
     *
     * List of Maps for immediate merging.
     * (merge functionality can be found in tufts.vue.LWMergeMap)
     *
     * maps specified by files are loaded, open
     * maps are passed in their current state.
     *
     * note: underlying model list may currently be changing
     * during this operation and data may get corrupted.
     * todo: consider copying the list before starting to load
     * and/or changing the underlying table model to be 
     * threadsafe
     *
     * Perhaps consider a way to clone and "freeze" the model
     * for the duration of this method call?
     *
     **/
    public List<LWMap> getMapList()
    {
        ArrayList mapList = new ArrayList();
        MapTableModel model = (MapTableModel)maps.getModel();
        int numberOfMaps = maps.getModel().getRowCount();
        for(int i=0;i<numberOfMaps;i++)
        {
            if(!model.isSelected(i))
            {
                continue;
            }
            if(model.getMapType(i) == OPEN_MAP)
            {
                mapList.add(model.getMap(i));
            }
            else if(model.getMapType(i) == LOCAL_FILE)
            {
                File file = null;
                LWMap map = null;
                try
                {
                  //file = new File((String)model.getValueAt(i,1));
                  //file = new File((String)model.localFiles.get(i));
                  String fileName ="file:///" + (String)model.localFiles.get(i);
                  map = ActionUtil.unmarshallMap(new java.net.URL(fileName));
                }
                catch(java.io.IOException exc)
                {
                    System.out.println("MSP: IO Exception: " + exc);
                }
              
                if(model.isSelected(i))
                   mapList.add(map);
            }
        }
        return mapList;
    }
    
    public LWMap getBaseMap()
    {
        MapTableModel model = (MapTableModel)maps.getModel();
        int i = model.getBaseMapIndex();
        if(model.getMapType(i) == OPEN_MAP)
        {
           return model.getMap(i);
        }
        else
        {
           File file = null;
           LWMap map = null;
           try
           {
             //file = new File((String)model.getValueAt(1,i));
             //map = ActionUtil.unmarshallMap(file);
             String fileName ="file:///" + (String)model.localFiles.get(i);
             map = ActionUtil.unmarshallMap(new java.net.URL(fileName));
           }
           catch(java.io.IOException exc)
           {
             System.out.println("MSP: IO Exception: " + exc);
           }
              
           return map;
        }      
        
    }
    
    public boolean getFilterOnBaseMap()
    {
        if(filterOnBaseMap.isSelected())
            return true;
        else
            return false;
    }
    
    /* class MapTableModelRow
    {
        private int type;
        private boolean selected;
        private LWMap map;
        private String file;
        private boolean isBaseMap;
       
        public MapTableModelRow(int type,boolean selected,LWMap map,String file,boolean isBaseMap)
        {
            this.type = type;
            this.selected = selected;
            this.map = map;
            this.file = file;
            this.isBaseMap = isBaseMap;
        }
        
        public void setSelected(boolean value)
        {
            selected = value;
        }
        
        public void toggleSelected()
        {
            if(selected)
                selected = false;
            else
                selected = true;
        }
        
        /**
         *
         * Loads map, if needed
         *
         **/
       /* public LWMap getMap()
        {
           
        }
        
        public int getType()
        {
            
        }
        
    }*/
    
    class MapTableModel implements TableModel
    {
       private ArrayList localFiles = new ArrayList(); 
       private ArrayList localFileSelectionStates = new ArrayList();
       private HashMap openMapSelections = new HashMap();
       private int baseMapIndex = 0;
       
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
               
               if(map instanceof LWMergeMap && (selected == null || !selected.equals("Selected")) ) 
               {
                   return false;
               }
               
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
               //String selected = (String)openMapSelections.get(getMap(row));
               //if(selected != null && selected.equals("Selected"))
               if(!select)
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
           //int openMapIndex = row-getFirstOpenMapRow()-1;
           int openMapIndex = row-getFirstOpenMapRow();
           if(openMapIndex < VUE.getLeftTabbedPane().getTabCount() && openMapIndex > -1)
                map =  VUE.getLeftTabbedPane().getMapAt(openMapIndex);
                // ****need list here, not iterator? ***** //map =  VUE.getLeftTabbedPane().getAllMaps().get(openMapIndex);
           // messing up selection?
           //if(map instanceof LWMergeMap)
           //    map = null;
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
       
       private int getBaseMapIndex()
       {
           return baseMapIndex;
       }
       
       private void setBaseMapIndex(int row)
       {
           baseMapIndex = row;
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
                   int mapRow = row-getFirstOpenMapRow();
                   LWMap map = VUE.getLeftTabbedPane().getMapAt(mapRow);
                   // creates exceptions - can't set row height to 0 to hide'
                   /*if(map instanceof LWMergeMap)
                   {
                       maps.setRowHeight(mapRow,0);
                   }*/
                   return VUE.getLeftTabbedPane().getMapAt(row-getFirstOpenMapRow()).getLabel();
               }
           }
           else if(col == 2)
           {
               if(getBaseMapIndex() == row)
               {
                  return "Guide";
               }
               else
               {
                   return "Not Guide";
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
           else return "";
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
           if(col == 0)
               return "Set";
           if(col == 1)
               return "Name";
           if(col == 2)
               return "Layout";
           if(col == 3)
               return "Location";
           else
               return "";
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
        private JRadioButton button = new JRadioButton();
        
        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
        {
            MapTableModel model = (MapTableModel)table.getModel();
            
            if(col == 0)
            {
                if(model.isSelected(row))
                    checkBox.setSelected(true);
                else
                    checkBox.setSelected(false);
                return checkBox;
            }
            if(col == 2)
            {
                if(model.getBaseMapIndex() == row)
                {
                    button.setSelected(true);
                }
                else
                {
                    button.setSelected(false);
                }
                return button;
            }
            else
            {
                label.setText(value.toString());
                return label;
            }
        }
    }
     
}
