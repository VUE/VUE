
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

package edu.tufts.vue.compare.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.*;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;

public class MergeMapsControlPanel extends JPanel /* implements ActiveListener<LWMap> */
{
    
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MergeMapsControlPanel.class);

    /** This was for code to re-load slider settings and vote v.s weight choice from existing LWMergeMaps */
    private static final boolean DynamicImpl = VueResources.getBool("merge.view.settings.dynamic");

    private static final boolean DEBUG_LOCAL = false;
    
    private MapsSelectionPanel mapSelectionPanel;
    private VisualizationSettingsPanel visualizationSettingsPanel;
    private JButton closeButton;
    private JButton generateButton;
    
    private DockWindow dw;

    private boolean stopWasPressed;
    
    public MergeMapsControlPanel(final DockWindow dw) 
    {
        this.dw = dw;
        
        setLayout(new BorderLayout());
        mapSelectionPanel = MapsSelectionPanel.getMapSelectionPanel();
        visualizationSettingsPanel = new VisualizationSettingsPanel();
        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(VueResources.local("dialog.mergemap.selectmaps"),mapSelectionPanel);
        tabs.addTab(VueResources.local("dialog.mergemap.visualizationsettings"),visualizationSettingsPanel);
        
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
        closeButton = new JButton(VueResources.local("dialog.mergemap.close"));
        closeButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               dw.setVisible(false);
           }
        });
        generateButton = new JButton(VueResources.local("dialog.mergemap.generatenewmap"));
        generateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //final LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
               //merge.setMapList(mapSelectionPanel.getMapList());
               //merge.setBaseMap(mapSelectionPanel.getBaseMap());
               //setMergeMapSettings(merge);
                    final Runnable mergeMaker = new Runnable() {
                            public void run() {
                                stopWasPressed = false; // AWT Thread
                                try {
                                    runMergeProcess();
                                } catch(Exception e) {
                                    //stopped = true;
                                    Log.error("mergeProcess", e); 
                                    if(DEBUG_LOCAL) e.printStackTrace();
                                    //use to automatically bail from window instead of using button
                                    /*dw.setContent(MergeMapsControlPanel.this);
                                      dw.repaint();
                                      generateButton.setEnabled(true);*/
                                }
                            }
                        };
               
                    try {        
                        Thread mergeThread = new Thread(mergeMaker);
                        mergeThread.start();
                    } catch(Exception te) {
                        Log.warn("kicking mergeThread -- " + te);
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
        
        if (DynamicImpl) {
            Log.error("DynamicImpl disabled", new Throwable("HERE"));
            // code not currently deployed
            // VUE.addActiveListener(LWMap.class, this);
            // final LWMap activeMap = VUE.getActiveMap();
            // if (activeMap != null && (activeMap instanceof LWMergeMap)) // could install meta-data instead
            //     adjustGUIToMergeMap((LWMergeMap)activeMap);
        }
        
       // dw.setVisible(true);
    }

    private void runMergeProcess()
        throws java.lang.InterruptedException
    {
        generateButton.setEnabled(false);
                        
        final JPanel loadingPanel = new JPanel(new BorderLayout());
        final JLabel loadingLabelImage = new JLabel(tufts.vue.VueResources.getImageIcon("dsv.statuspanel.waitIcon"));
                        
        //VUE-1058 -- only warn on no maps for now.  this is too slow to do this here, however.
        // (not exactly sure why? look in MapsSelectionPanel model) do this after the loading
        // label starts probably makes sense to check in the thread anyway in case someday are
        // merging from networks/etc.  except.. the new getNumberOfSelections method seems to
        // be fast enough -- so use this for now (approaching 2.1 build rapidly..)
        boolean noMapsSelected = false;
                        
        if (mapSelectionPanel.getNumberOfSelections() == 0) {
            // todo: also, if for all the selected maps, map.hasContent() == false
            noMapsSelected = true;
        }

        final JPanel loadingLabel = new JPanel();
        final JLabel progressLabel = new JLabel();
        if(noMapsSelected) {
            // No need to do this this: should just disable the "generate" button
            progressLabel.setText(VueResources.local("dialog.mergemap.nomapsselected"));  
        } else {    
            progressLabel.setText(VueResources.local("dialog.mergemap.inprogress"));
        }
        loadingLabel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
                        
        if (!noMapsSelected)
            loadingLabel.add(loadingLabelImage);

        loadingLabel.add(progressLabel);

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder
                              (BorderFactory.createEmptyBorder(0,10,0,10),
                               BorderFactory.createMatteBorder(1,0,0,0,new java.awt.Color(153,153,153))));
                        
        final JButton stopButton = new JButton(Util.upCaseInitial(VueResources.local("dialog.mergemap.cancel")));
                        
        buttonPanel.add(stopButton);
                        
        loadingPanel.add(loadingLabel);
        loadingPanel.add(buttonPanel,BorderLayout.SOUTH);
                        
        stopButton.addActionListener(new java.awt.event.ActionListener(){
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    //Thread.currentThread().interrupt();
                    //Thread.currentThread().stop();
                    stopWasPressed = true; // AWT thread
                    //dw.setContent(new JLabel("'stopped'"));
                    dw.setContent(MergeMapsControlPanel.this);
                    dw.repaint();
                    generateButton.setEnabled(true);
                }
            });
        //loadingLabel.add(stopButton);

        // These Runnables let us move these code segments back
        // to the AWT thread.
        
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            dw.setContent(loadingPanel);  // Run back on AWT thread
        }});
                        
        if (noMapsSelected) // do we 
            return;
        
                       
        // final LWMergeMap mergedMap = getConfiguredLWMergeMap();
        // if (visualizationSettingsPanel.getVisualizationSettingsType() == VisualizationSettingsPanel.VOTE)
        //     mergedMap.fillAsVoteMerge();
        // else
        //     mergedMap.fillAsWeightMerge();

        final LWMap mergedMap = createMergedMap();
        
        // Thread.sleep(5000);
        final Runnable displayTask = new Runnable() {
                public void run() {
                    Log.info("invoke-later for displayMap " + mergedMap);
                    VUE.displayMap(mergedMap);
                    dw.setContent(MergeMapsControlPanel.this);
                    dw.repaint();   
                    generateButton.setEnabled(true); 
                }
            };

                            
        if (!stopWasPressed) { // technically should be synced: was set in AWT thread
            // The only thing we actually stop is the final display.
            SwingUtilities.invokeLater(displayTask); // run this back on AWT thread
        }
    }

    private LWMap createMergedMap()
    {
        final java.util.List<LWMap> mapList = mapSelectionPanel.getMapList();

        final LWMap baseMap = mapList.get(mapSelectionPanel.getBaseMapIndex());
        
        final MergeMapFactory mm = new MergeMapFactory(baseMap, mapList, mapSelectionPanel.getCheckList());
        
        //mm.setMapList(mapList);
        //mm.setBaseMap(mapList.get(mapSelectionPanel.getBaseMapIndex()));
        //mm.setActiveMapList(mapSelectionPanel.getCheckList());
        
        mm.setNodeIntervalBoundaries(visualizationSettingsPanel.getNodeIntervalBoundaries()); 
        mm.setLinkIntervalBoundaries(visualizationSettingsPanel.getLinkIntervalBoundaries()); 
        mm.setNodeThresholdSliderValue(visualizationSettingsPanel.getNodeThresholdSliderValue());
        mm.setLinkThresholdSliderValue(visualizationSettingsPanel.getLinkThresholdSliderValue());
        mm.setFilterOnBaseMap(mapSelectionPanel.getFilterOnBaseMap());
        mm.setExcludeNodesFromBaseMap(mapSelectionPanel.getExcludeNodesFromBaseMap());
        
        if (visualizationSettingsPanel.getVisualizationSettingsType() == VisualizationSettingsPanel.VOTE)
            return mm.createAsVoteMerge();
        else
            return mm.createAsWeightMerge();
    }
    
    // private LWMergeMap getConfiguredLWMergeMap()
    // {
    //     final LWMergeMap map = new LWMergeMap("Hello " + LWMergeMap.getTitle()); // wierd construction...
    //     final java.util.List<LWMap> mapList = mapSelectionPanel.getMapList();
    //     map.setMapList(mapList);
    //     map.setBaseMap(mapList.get(mapSelectionPanel.getBaseMapIndex()));
    //     map.setActiveMapList(mapSelectionPanel.getCheckList());
    //     map.setNodeIntervalBoundaries(visualizationSettingsPanel.getNodeIntervalBoundaries()); 
    //     map.setLinkIntervalBoundaries(visualizationSettingsPanel.getLinkIntervalBoundaries()); 
    //     map.setNodeThresholdSliderValue(visualizationSettingsPanel.getNodeThresholdSliderValue());
    //     map.setLinkThresholdSliderValue(visualizationSettingsPanel.getLinkThresholdSliderValue());
    //     map.setFilterOnBaseMap(mapSelectionPanel.getFilterOnBaseMap());
    //     map.setExcludeNodesFromBaseMap(mapSelectionPanel.getExcludeNodesFromBaseMap());
    //     map.setVisualizationSelectionType(visualizationSettingsPanel.getVisualizationSettingsType()); // not needed: just switch for fillAsVote v.s. fillAsWeight
    //     return map;
    // }
    
    // public void adjustGUIToMergeMap(LWMergeMap map) {
    //     visualizationSettingsPanel.setNodeThresholdSliderValue(map.getNodeThresholdSliderValue());
    //     visualizationSettingsPanel.setLinkThresholdSliderValue(map.getLinkThresholdSliderValue());
    //     visualizationSettingsPanel.setVisualizationSettingsType(map.getVisualizationSelectionType());
    // }
    
    // public void activeChanged(ActiveEvent<LWMap> e) {
    //     if (e.active instanceof LWMergeMap)
    //         adjustGUIToMergeMap((LWMergeMap)e.active);
    // }
    
}
