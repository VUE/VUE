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
 *
 **********************************************
 *
 * MapsSelectionPanel.java
 *
 * Created on May 3, 2007, 11:17 AM
 *
 * @version $Revision: 1.44 $ / $Date: 2010-02-03 19:25:31 $ / $Author: mike $
 * @author dhelle01
 */

package edu.tufts.vue.compare.ui;

import edu.tufts.vue.compare.Util;
import edu.tufts.vue.compare.Util.MP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.beans.*;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import tufts.vue.*;
import tufts.vue.action.ActionUtil;
import tufts.vue.gui.VueFileChooser;
import tufts.vue.gui.GUI;
import tufts.vue.gui.GUI.ComboKey;

public class MapsSelectionPanel extends JPanel  {

    private static final String[] MergePropertyChoices = {
        VueResources.local("combobox.mergepropertychoices.label"),
        "Resource",
        VueResources.local("combobox.mergepropertychoices.ontologicalmembership"),
        VueResources.local("combobox.mergepropertychoices.ontologicalmembershiplabel")
    };
    
    
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
    
    public String stepOneMessage = VueResources.getString("dialog.selectionpane.stepOneMessage");
    public String stepTwoMessage = VueResources.getString("dialog.selectionpane.stepTwoMessage");
    public String stepThreeMessage = VueResources.getString("dialog.selectionpane.stepThreeMessage");

    public String showAllMessage = VueResources.getString("dialog.selectionpane.showAllMessage");
    public String filterOnPrimaryMapMessage = VueResources.getString("dialog.selectionpane.filterOnPrimaryMapMessage");
    public String excluePrimaryMapNodesMessage = VueResources.getString("dialog.selectionpane.excluePrimaryMapNodesMessage");
    
    public String filterOnBaseMapMessageString = VueResources.getString("dialog.selectionpane.filterOnBaseMapMessageString");
    
    private String AddMaps = VueResources.getString("dialog.selectionpane.AddMaps");
    
    private String MergeProrerty = VueResources.getString("dialog.selectionpane.MergeProrerty");
    private JScrollPane scroll;
    private JTable maps;
    //private JTextField fileNameField;
    private JButton browseButton;
    
    //private JPanel bottomPanel;
    
    private String[] mapFilterChoices = {showAllMessage,filterOnPrimaryMapMessage,excluePrimaryMapNodesMessage};
    private JComboBox mapFilterChoice;
    
    private JCheckBox filterOnBaseMap;
    
    private boolean deleteDown = false;
    
    private File lastDirectory = null;
    
    private MapsSelectionPanel() 
    {
        setOpaque(false);
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        setLayout(gridBag);
        
		//fileNameField = new JTextField(20);
        browseButton = new JButton(AddMaps);
        browseButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               VueFileChooser choose = VueFileChooser.getVueFileChooser();
               if(lastDirectory!=null)
                   choose.setCurrentDirectory(lastDirectory);
               VueFileFilter vff = new VueFileFilter(VueFileFilter.VUE_DESCRIPTION);
               choose.setFileFilter(vff);
               choose.setFileSelectionMode(VueFileChooser.FILES_AND_DIRECTORIES);
               int cancel = choose.showOpenDialog(MapsSelectionPanel.this);
               if(cancel == VueFileChooser.CANCEL_OPTION)
                   return;
               File choice = choose.getSelectedFile();
               if(choose.getSelectedFile().getParentFile().isDirectory())
                 lastDirectory = choose.getSelectedFile().getParentFile();
               if(choice.isDirectory())
               {
                  java.io.FileFilter iff = new java.io.FileFilter()
                  {
                      public boolean accept(File file)
                      {
                          if(file.getName().substring(file.getName().lastIndexOf(".")+1,file.getName().length()).equals("vue"))
                            return true;
                          else 
                            return false;
                      }
                      
                      public String getDescription()
                      {
                          return "VUE FILE";
                      }
                  };
                  File[] files =  choice.listFiles(iff);
                  for(int i=0;i<files.length;i++)
                  {
                      File file = files[files.length-1-i];
                      String name = file.getAbsolutePath();
                      ((MapTableModel)maps.getModel()).addRow(name);
                      //String shortName = getShortNameForFile(name); 
                      //fileNameField.setText(shortName);
                      revalidate();
                      scroll.getViewport().revalidate();
                  }
               }
               else
               {    
                  String name = choice.getAbsolutePath();
                  ((MapTableModel)maps.getModel()).addRow(name);
                  //fileNameField.setText(getShortNameForFile(name));
                  revalidate();
                  scroll.getViewport().revalidate();
               }
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
                scroll.getViewport().revalidate();
                //System.out.println("MSP: VUE tabbed pane property change event " + e.getPropertyName());
            }
        });

        JLabel stepOneLabel = new JLabel(stepOneMessage);
        stepOneLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBag.setConstraints(stepOneLabel,gridBagConstraints);
        //stepOneLabel.setBorder(BorderFactory.createEmptyBorder(15,0,0,0));
        stepOneLabel.setBorder(BorderFactory.createEmptyBorder(5+2,15,5+2,7));
        add(stepOneLabel);
        
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        
        //gridBagConstraints.insets = new Insets(3,0,0,0);
        
        //gridBagConstraints.gridwidth = 1;
        // //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(browseButton,gridBagConstraints);
        browseButton.setOpaque(false);
        browseButton.setFont(tufts.vue.gui.GUI.LabelFace);
        add(browseButton);
        
        gridBagConstraints.insets = new Insets(0,0,0,0);
        
        JLabel stepTwoLabel = new JLabel(stepTwoMessage);
        stepTwoLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(stepTwoLabel,gridBagConstraints);
        stepTwoLabel.setBorder(BorderFactory.createEmptyBorder(5+2,15,5+2,5));
        add(stepTwoLabel);  

        JLabel stepThreeLabel = new JLabel(stepThreeMessage);
        stepThreeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.gridwidth = 1;
        gridBag.setConstraints(stepThreeLabel,gridBagConstraints);
        stepThreeLabel.setBorder(BorderFactory.createEmptyBorder(5+2,15,5+2,7));
        add(stepThreeLabel);  
        
        mapFilterChoice = new JComboBox(mapFilterChoices);
        mapFilterChoice.setFont(tufts.vue.gui.GUI.LabelFace);
        
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        
        //gridBagConstraints.insets = new Insets(15,5,15,5);
        
        //gridBagConstraints.gridwidth = 1;
        // //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(mapFilterChoice,gridBagConstraints);
        add(mapFilterChoice);
        
        
		JLabel mergePropertyLabel = new JLabel(MergeProrerty);
        mergePropertyLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        
        final JComboBox mergePropertyChoice = new JComboBox(MergePropertyChoices);
        
        mergePropertyChoice.addItemListener(new ItemListener(){
           public void itemStateChanged(ItemEvent ie) {
               if (ie.getStateChange() == ItemEvent.SELECTED) {
                        if (ie.getItem().equals(MergePropertyChoices[0])) Util.setMergeProperty(MP.LABEL);
                   else if (ie.getItem().equals(MergePropertyChoices[1])) Util.setMergeProperty(MP.RESOURCE);
                   else if (ie.getItem().equals(MergePropertyChoices[2])) Util.setMergeProperty(MP.TYPE);
                   else if (ie.getItem().equals(MergePropertyChoices[3])) Util.setMergeProperty(MP.BOTH); // historical name
                   else {
                       System.err.println("MSP unknown choice: " + ie.getItem());
                       Util.setMergeProperty(MP.LABEL);
                   }
               }
           }
        });
        
        mergePropertyChoice.setFont(tufts.vue.gui.GUI.LabelFace);
        gridBagConstraints.gridwidth = 1;
        gridBag.setConstraints(mergePropertyLabel,gridBagConstraints);
        mergePropertyLabel.setBorder(BorderFactory.createEmptyBorder(5+2,15,5+2,5));
        if(!VueResources.getString("merge.ontologyType.gui").equals(VueResources.getString("merge.ontologyType.guioff")))
        {    
          add(mergePropertyLabel);
        }
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(mergePropertyChoice,gridBagConstraints);
        if(!VueResources.getString("merge.ontologyType.gui").equals(VueResources.getString("merge.ontologyType.guioff")))
        {
          add(mergePropertyChoice);
        }
        
        gridBagConstraints.insets = new Insets(0,0,0,0);
        
        //bottomPanel = new JPanel();
        filterOnBaseMap = new JCheckBox();
        JLabel filterOnBaseMapMessage = new JLabel(filterOnBaseMapMessageString);
        ////filterOnBaseMapMessage.setOpaque(false);
        //bottomPanel.setOpaque(false);
        //bottomPanel.add(filterOnBaseMap);
        //bottomPanel.add(filterOnBaseMapMessage);
        //gridBagConstraints.weighty = 0.0;
        //gridBagConstraints.anchor = GridBagConstraints.WEST;
        //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //gridBag.setConstraints(bottomPanel,gridBagConstraints);
        //add(bottomPanel);
        
        
        //JLabel selectMapsLabel = new JLabel("Select maps:");
        //selectMapsLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        //gridBagConstraints.gridwidth = 1;
        //gridBag.setConstraints(selectMapsLabel,gridBagConstraints);
        //add(selectMapsLabel);
        
        //gridBagConstraints.weightx = 1.0;
        //gridBagConstraints.weighty = 0.0;
        //gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        //gridBag.setConstraints(fileNameField,gridBagConstraints);
        //add(fileNameField);
        
        
        //gridBagConstraints.weightx = 0.0;
        //gridBagConstraints.anchor = GridBagConstraints.WEST;
        //gridBagConstraints.gridwidth = 1;
        // //gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //gridBag.setConstraints(browseButton,gridBagConstraints);
        //browseButton.setOpaque(false);
        //add(browseButton);
        
        
        gridBagConstraints.weightx = 1.0;
        
        gridBagConstraints.gridwidth = 1;
        
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBag.setConstraints(filterOnBaseMap,gridBagConstraints);
        filterOnBaseMap.setOpaque(false);
        //add(filterOnBaseMap);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(0,0,0,5);
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        filterOnBaseMapMessage.setForeground(new Color(77,109,109));
        gridBag.setConstraints(filterOnBaseMapMessage,gridBagConstraints);
        //add(filterOnBaseMapMessage);
        gridBagConstraints.insets = new Insets(0,0,0,0);
        maps = new JTable(new MapTableModel());
        maps.setRowHeight(maps.getRowHeight()+6);
        maps.getTableHeader().setReorderingAllowed(false);
        
        ((DefaultTableCellRenderer)(maps.getTableHeader().getDefaultRenderer())).setHorizontalAlignment(SwingConstants.LEFT);
                
        maps.addMouseListener(new java.awt.event.MouseAdapter() {
          public void mousePressed(java.awt.event.MouseEvent e)
          {
            MapTableModel model = (MapTableModel)maps.getModel();
            //System.out.println("MSP mouse pressed, row column: " + maps.getSelectedRow() + "," + maps.getSelectedColumn());
            if(maps.getSelectedColumn() == 0)
            {
                if(model.isSelected(maps.getSelectedRow()))
                    model.setSelected(false,maps.getSelectedRow());
                else
                    model.setSelected(true,maps.getSelectedRow());
            }
            if(maps.getSelectedColumn() == 2)
            {
                model.setBaseMapIndex(maps.getSelectedRow());
            }
            if(maps.getSelectedColumn() == 3)
            {
                deleteDown = true;
            }
            else
            {
                deleteDown = false;
            }
            repaint();
            
          }
          
          public void mouseReleased(java.awt.event.MouseEvent e)
          {
            if(maps.getSelectedColumn() == 3 && ((MapTableModel)maps.getModel()).getMapType(maps.getSelectedRow()) == LOCAL_FILE)
            {
                ((MapTableModel)maps.getModel()).localFiles.remove(maps.getSelectedRow());
                deleteDown = false;
            }     
            repaint();
          }
          
        });
        maps.setDefaultRenderer(Object.class,new MapTableCellRenderer());
        maps.getColumnModel().getColumn(0).setMinWidth(35);
        maps.getColumnModel().getColumn(0).setMaxWidth(35);
        maps.getColumnModel().getColumn(1).setMinWidth(250);
        maps.getColumnModel().getColumn(2).setMinWidth(35);
        //maps.getColumnModel().getColumn(2).setMaxWidth(45);
        maps.getColumnModel().getColumn(3).setMinWidth(80);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.fill = gridBagConstraints.BOTH;
        scroll = new JScrollPane(maps);
        //scroll.setViewportBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        //gridBag.setConstraints(scroll,gridBagConstraints);
        JPanel scrollPanel = new JPanel(new java.awt.BorderLayout());
        scrollPanel.setOpaque(false);
        
        // VUE-924
        if(tufts.Util.isWindowsPlatform())
        {
            scroll.getViewport().setBackground(java.awt.Color.WHITE);
        }
        
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        scrollPanel.add(scroll);
        gridBag.setConstraints(scrollPanel,gridBagConstraints);
        //add(scroll);
        add(scrollPanel);
        
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
     * (threadsafe model will likely have to be contain clones
     *  of current model elements and regularly synched with
     *  actual file list)
     *
     **/
    public List<LWMap> getMapList()
    {
        ArrayList<LWMap> mapList = new ArrayList<LWMap>();
        MapTableModel model = (MapTableModel)maps.getModel();
        int numberOfMaps = maps.getModel().getRowCount();
        for(int i=0;i<numberOfMaps;i++)
        {
            //if(!model.isSelected(i))
            //{
            //    continue;
            //}
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
              
                //if(model.isSelected(i))
                   mapList.add(map);
            }
        }
        return mapList;
    }
    
    public List<Boolean> getCheckList()
    {
        ArrayList<Boolean> checkList = new ArrayList<Boolean>();
        MapTableModel model = (MapTableModel)maps.getModel();
        int numberOfMaps = maps.getModel().getRowCount();
        for(int i=0;i<numberOfMaps;i++)
        {
            if(!model.isSelected(i))
            {
               checkList.add(Boolean.FALSE);
            }
            else
            {
               checkList.add(Boolean.TRUE);
            }
        }
        return checkList;
    }
    
    /*public LWMap getBaseMap()
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
             String fileName ="file:///" + (String)model.localFiles.get(i);
             map = ActionUtil.unmarshallMap(new java.net.URL(fileName));
           }
           catch(java.io.IOException exc)
           {
             System.out.println("MSP: IO Exception: " + exc);
           }
              
           return map;
        }      
        
    }*/
    
    public int getBaseMapIndex()
    {
        MapTableModel model = (MapTableModel)maps.getModel();
        if(model == null)
            return 0;
        int baseIndex = model.getBaseMapIndex();
        if(baseIndex < 0)
            return 0;
        int numberOfMaps = getMapList().size();
        if(baseIndex > numberOfMaps )
            return Math.max(numberOfMaps - 1,0);
        return baseIndex;
    }
    
    public boolean getFilterOnBaseMap()
    {
        //if(filterOnBaseMap.isSelected())
        if(mapFilterChoice.getSelectedItem().equals(filterOnPrimaryMapMessage))
            return true;
        else
            return false;
    }
    
    public boolean getExcludeNodesFromBaseMap()
    {
        if(mapFilterChoice.getSelectedItem().equals(excluePrimaryMapNodesMessage))
            return true;
        else
            return false;
    }
    
    public static String getShortNameForFile(String absolutePath)
    {
       int lastDot = absolutePath.lastIndexOf(".");
       int lastSlash = absolutePath.lastIndexOf(java.io.File.separator);
       int endIndex = absolutePath.length();
       if(lastSlash!=-1 && (lastSlash < (absolutePath.length() - 1) ))
       {
          if(lastDot > lastSlash)
          {
             endIndex = lastDot;
          }
          return absolutePath.substring(lastSlash+1,endIndex);
       }
       else
          return absolutePath;
    }
    
    public int getNumberOfSelections()
    {
        //return ((MapTableModel)maps.getModel()).getNumberOfSelections();
        return java.util.Collections.frequency(getCheckList(),Boolean.TRUE);
    }
    
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
       
       // seems to be a bug in this somewhere with newly opened maps .. using getCheckList
       // in the panel instead for now
       /*public int getNumberOfSelections()
       {
           int locals = java.util.Collections.frequency(localFileSelectionStates,"Selected");
           int opens = java.util.Collections.frequency(openMapSelections.values(),"Selected");    
                   
           return locals + opens;
       }*/
       
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
               
               if(map.hasClientData(tufts.vue.MergeMapFactory.class) && (selected == null || !selected.equals("Selected")) ) 
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
           int openMapIndex = row-getFirstOpenMapRow();
           if(openMapIndex < VUE.getLeftTabbedPane().getTabCount() && openMapIndex > -1)
                map =  VUE.getLeftTabbedPane().getMapAt(openMapIndex);
           return map;
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
                   return VueResources.getString("dialog.mapselection.open");
               }
               else
               {
                   return "Local File";
               }
               //return VueResources.getImageIcon("presentationDialog.button.delete.up");
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
           if(col == 3)
               return JPanel.class;
           else
               return String.class;
       }
       
       public String getColumnName(int col)
       {
           if(col == 0)
               return VueResources.getString("dialog.mapselection.colset");
           if(col == 1)
               return VueResources.getString("dialog.mapselection.colname");
           if(col == 2)
               return VueResources.getString("dialog.mapselection.colprimary");
           if(col == 3)
               return VueResources.getString("dialog.mapselection.collocation");
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
        private JPanel checkBoxPanel = new JPanel(new BorderLayout());
        private JLabel label = new JLabel();
        private JRadioButton button = new JRadioButton();
        private JPanel baseButtonPanel = new JPanel(new BorderLayout());
        private JPanel deletePanel = new JPanel(new BorderLayout());
        private JLabel imageLabel = new JLabel();
        private JLabel typeLabel = new JLabel();
        
        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int col)
        {
            MapTableModel model = (MapTableModel)table.getModel();
            
            if(col == 0)
            {
                checkBox.setBackground(Color.WHITE);
                checkBoxPanel.setBackground(Color.WHITE);
                if(model.isSelected(row))
                    checkBox.setSelected(true);
                else
                    checkBox.setSelected(false);
                checkBoxPanel.add(checkBox);
                checkBoxPanel.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(229,229,229)));
                return checkBoxPanel;
            }
            if(col == 1)
            {
                String name = value.toString();
                label.setText(getShortNameForFile(name));
                label.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(229,229,229)));
                return label;
            }
            if(col == 2)
            {
                baseButtonPanel.setBackground(Color.WHITE);
                button.setBackground(Color.WHITE);
                if(model.getBaseMapIndex() == row)
                {
                    button.setSelected(true);
                }
                else
                {
                    button.setSelected(false);
                }
                baseButtonPanel.add(button,BorderLayout.WEST);
                baseButtonPanel.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(229,229,229)));
                return baseButtonPanel;
            }
            else
            if(col == 3)
            {
                typeLabel.setText(value.toString());
                
                typeLabel.setBackground(Color.WHITE);
                deletePanel.setOpaque(true);
                deletePanel.setBackground(Color.WHITE);
                
                deletePanel.add(typeLabel,BorderLayout.WEST);
                if(model.getMapType(row)==LOCAL_FILE)
                {
                   if(isSelected && deleteDown)
                     imageLabel.setIcon(VueResources.getImageIcon("merge.selectmaps.delete.down"));
                   else
                     imageLabel.setIcon(VueResources.getImageIcon("merge.selectmaps.delete.up"));
                   deletePanel.add(imageLabel,BorderLayout.EAST);
                }
                else
                {
                    deletePanel.remove(imageLabel);
                }
                deletePanel.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(229,229,229)));
                return deletePanel;
            }
            else
            {
                label.setText(value.toString());
                return label;
            }
        }
    }
     
}
