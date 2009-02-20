
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

package edu.tufts.vue.compare.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.*;
import tufts.vue.*;
import tufts.vue.gui.*;

public class MergeMapsControlPanel extends JPanel implements ActiveListener<LWMap> {
    
    private static final boolean DEBUG_LOCAL = false;
    
    private MapsSelectionPanel mapSelectionPanel;
    private VisualizationSettingsPanel visualizationSettingsPanel;
    private JButton closeButton;
    private JButton generateButton;
    private boolean dynamic = false;
    
    private DockWindow dw;
    
    public MergeMapsControlPanel(final DockWindow dw) 
    {
        this.dw = dw;
        
        
        String dynamicString = VueResources.getString("merge.view.settings.dynamic");
        if(dynamicString.equals("true"))
        {
            dynamic = true;
        }
        
        setLayout(new BorderLayout());
        mapSelectionPanel = MapsSelectionPanel.getMapSelectionPanel();
        visualizationSettingsPanel = new VisualizationSettingsPanel();
        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Select Maps",mapSelectionPanel);
        tabs.addTab("Visualization Settings",visualizationSettingsPanel);
        
        /*tabs.addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                revalidate();
                repaint();
                if(DEBUG_LOCAL)
                {
                  System.out.println("MMCP: tabbed pane property change event " + e.getPropertyName());
                }
        });*/
        
        tabs.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                //System.out.println("MMCP: tabbed pane change event " + e);
                if(tabs.getSelectedIndex() == 0)
                {
                    dw.setSize(535,535);
                    dw.repaint();
                }
                if(tabs.getSelectedIndex() == 1)
                {
                    dw.setSize(535,535);
                    dw.repaint();
                }
                
            }
        });
        
        
        add(tabs);
        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               dw.setVisible(false);
           }
        });
        generateButton = new JButton("Generate New Map");
        generateButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               //final LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
               //merge.setMapList(mapSelectionPanel.getMapList());
               //merge.setBaseMap(mapSelectionPanel.getBaseMap());
               
               //setMergeMapSettings(merge);
               
               Runnable merger = new Runnable()
               {
                   boolean stopped = false;
                   
                   public void run()
                   {
                      try
                      {        
                        generateButton.setEnabled(false);
                        
                        final JPanel loadingPanel = new JPanel(new BorderLayout());
                        
                        //LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
                        //setMergeMapSettings(merge);
                        JLabel loadingLabelImage = new JLabel(tufts.vue.VueResources.getImageIcon("dsv.statuspanel.waitIcon"));
                        //JPanel loadingLabel = new JPanel(new BorderLayout());
                        
                        //VUE-1058 -- only warn on no maps for now.
                        // this is too slow to do this here, however.
                        // (not exactly sure why? look in MapsSelectionPanel model)
                        // do this after the loading label starts
                        // probably makes sense to check in the thread anyway in 
                        // case someday are merging from networks/etc.
                        // except.. the new getNumberOfSelections method seems
                        // to be fast enough -- so use this for now (approaching 2.1
                        // build rapidly..)
                        boolean noMapsSelected = false;
                        
                        if(mapSelectionPanel.getNumberOfSelections() == 0)
                        {
                            noMapsSelected = true;
                        }
                        
                        JPanel loadingLabel = new JPanel();
                        JLabel progressLabel = new JLabel();
                        if(noMapsSelected)
                        {
                          progressLabel.setText(" No maps were selected");  
                        }
                        else
                        {    
                          progressLabel.setText("In Progress...");
                        }
                        loadingLabel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
                        
                        if(!noMapsSelected)
                        {    
                          loadingLabel.add(loadingLabelImage);
                        }
                        loadingLabel.add(progressLabel);
                        
                        
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                                                BorderFactory.createEmptyBorder(0,10,0,10),
                                                BorderFactory.createMatteBorder(1,0,0,0,new java.awt.Color(153,153,153))));
                        
                        JButton stopButton = new JButton("cancel");
                        
                        buttonPanel.add(stopButton);
                        
                        loadingPanel.add(loadingLabel);
                        loadingPanel.add(buttonPanel,BorderLayout.SOUTH);
                        
                        stopButton.addActionListener(new java.awt.event.ActionListener(){
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                //Thread.currentThread().interrupt();
                                //Thread.currentThread().stop();
                                stopped = true;
                                //dw.setContent(new JLabel("'stopped'"));
                                dw.setContent(MergeMapsControlPanel.this);
                                dw.repaint();
                                generateButton.setEnabled(true);
                            }
                        });
                        //loadingLabel.add(stopButton);
                      
                        Thread innerThread = new Thread()
                        {
                            public void run()
                            {
                               dw.setContent(loadingPanel); 
                            }
                        };
                        
                        SwingUtilities.invokeLater(innerThread); 
                        
                        if(!noMapsSelected)
                        {
                          final LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
                          setMergeMapSettings(merge);
                      
                          Thread innerThread2 = new Thread()
                          {
                            public void run()
                            {
                               VUE.displayMap(merge);
                               
                               java.util.Iterator<LWComponent> elements = merge.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
                               
                               while(elements.hasNext())
                               {
                                   //if(elements instanceof LWNode)
                                   //{
                                     elements.next().layout();
                                   //}
                               }
                               
                               dw.setContent(MergeMapsControlPanel.this);
                               dw.repaint();   
                               generateButton.setEnabled(true); 
                            }
                          };
                       
                          if(visualizationSettingsPanel.getVisualizationSettingsType() == VisualizationSettingsPanel.VOTE)
                            merge.fillAsVoteMerge();
                          else
                            merge.fillAsWeightMerge();
                          if(!stopped)
                          {    
                            SwingUtilities.invokeLater(innerThread2);
                          }
                        }
                               
                     }
                     catch(Exception e)
                     {
                        //stopped = true;
                         
                        System.out.println("MergeMaps error " + e); 
                        
                        if(DEBUG_LOCAL)
                        {    
                          e.printStackTrace();
                        }
                         
                        //use to automatically bail from window instead of using button
                        /*dw.setContent(MergeMapsControlPanel.this);
                        dw.repaint();
                        generateButton.setEnabled(true);*/
                     }
                   }
               };
               
               try
               {        
                 Thread mergeThread = new Thread(merger);
                 mergeThread.start();
               }
               catch(Exception te)
               {
                   System.out.println("Merge Exception -- " + te);
               }
               //SwingUtilities.invokeLater(merger);
               
               /*if(visualizationSettingsPanel.getVisualizationSettingsType() == VisualizationSettingsPanel.VOTE)
                 merge.fillAsVoteMerge();
               else
                 merge.fillAsWeightMerge();
               MapViewer v = VUE.displayMap(merge);*/
               
               //tufts.vue.LWCEvent event = new tufts.vue.LWCEvent(v,merge,new LWComponent.Key("Merge Map Displayed"));
               //v.LWCChanged(event);
           }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        buttonPanel.add(generateButton);
        add(buttonPanel,BorderLayout.SOUTH);
        dw.setContent(this);
        dw.setSize(535,535);
        dw.setResizeEnabled(false);
        // unfortunately setting the default button conflicts with the enter key in main VUE window:
        // if(getRootPane()!=null)
        //{    
        //  getRootPane().setDefaultButton(generateButton);
        //}
        
        if(dynamic)
        {    
          VUE.addActiveListener(LWMap.class, this);
          LWMap currentlyLoadedMap = VUE.getActiveMap();
          if(currentlyLoadedMap != null && (currentlyLoadedMap instanceof LWMergeMap))
              adjustGUIToMergeMap((LWMergeMap)currentlyLoadedMap);
        }
        
       // dw.setVisible(true);
    }
    
    public void setMergeMapSettings(LWMergeMap map)
    {
        java.util.List<LWMap> mapList = mapSelectionPanel.getMapList();
        map.setMapList(mapList);
        //int baseMapIndex = mapSelectionpanel.getBaseMapIndex();
        //if(baseMapIndex)
        map.setBaseMap(mapList.get(mapSelectionPanel.getBaseMapIndex()));
        map.setActiveMapList(mapSelectionPanel.getCheckList());
        // this was the default method before editable labels added (see 
        // WeightVisualizationSettingsPanel
        //map.setIntervalBoundaries();
        map.setNodeIntervalBoundaries(visualizationSettingsPanel.getNodeIntervalBoundaries()); 
        map.setLinkIntervalBoundaries(visualizationSettingsPanel.getLinkIntervalBoundaries()); 
        map.setNodeThresholdSliderValue(visualizationSettingsPanel.getNodeThresholdSliderValue());
        map.setLinkThresholdSliderValue(visualizationSettingsPanel.getLinkThresholdSliderValue());
        //moved from Visualization panel to Maps Panel:
        //map.setFilterOnBaseMap(visualizationSettingsPanel.getFilterOnBaseMap());
        map.setFilterOnBaseMap(mapSelectionPanel.getFilterOnBaseMap());
        map.setExcludeNodesFromBaseMap(mapSelectionPanel.getExcludeNodesFromBaseMap());
        map.setVisualizationSelectionType(visualizationSettingsPanel.getVisualizationSettingsType());
    }
    
    public void adjustGUIToMergeMap(LWMergeMap map)
    {
        visualizationSettingsPanel.setNodeThresholdSliderValue(map.getNodeThresholdSliderValue());
        visualizationSettingsPanel.setLinkThresholdSliderValue(map.getLinkThresholdSliderValue());
        visualizationSettingsPanel.setVisualizationSettingsType(map.getVisualizationSelectionType());
    }
    
    public void activeChanged(ActiveEvent<LWMap> e)
    {
        //System.out.println("Merge Maps Control Panel: active changed - " + e.active);
        if(e.active instanceof LWMergeMap)
        {
            adjustGUIToMergeMap((LWMergeMap)e.active);
        }
    }
    
}
