
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
 *****************************************
 *
 * MergeMapsControlPanel.java
 *
 * Created on May 8, 2007, 1:31 PM
 *
 * @version $Revision: 1.10 $ / $Date: 2007-06-04 14:45:26 $ / $Author: dan $
 * @author dhelle01
 *
 * 
 *
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
    
    private MapsSelectionPanel mapSelectionPanel;
    private VisualizationSettingsPanel visualizationSettingsPanel;
    private JButton closeButton;
    private JButton generateButton;
    private boolean dynamic = true;
    
    public MergeMapsControlPanel(final DockWindow dw) 
    {
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
                    dw.setSize(650,550);
                    dw.repaint();
                }
                if(tabs.getSelectedIndex() == 1)
                {
                    dw.setSize(535,540);
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
        generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
               //merge.setMapList(mapSelectionPanel.getMapList());
               //merge.setBaseMap(mapSelectionPanel.getBaseMap());
               
               setMergeMapSettings(merge);
               
               if(visualizationSettingsPanel.getVisualizationSettingsType() == VisualizationSettingsPanel.VOTE)
                 merge.fillAsVoteMerge();
               else
                 merge.fillAsWeightMerge();
               MapViewer v = VUE.displayMap(merge);
               
               //tufts.vue.LWCEvent event = new tufts.vue.LWCEvent(v,merge,new LWComponent.Key("Merge Map Displayed"));
               //v.LWCChanged(event);
           }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        buttonPanel.add(generateButton);
        add(buttonPanel,BorderLayout.SOUTH);
        dw.setContent(this);
        dw.setSize(650,550);
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
        
        dw.setVisible(true);
    }
    
    public void setMergeMapSettings(LWMergeMap map)
    {
        map.setMapList(mapSelectionPanel.getMapList());
        map.setBaseMap(mapSelectionPanel.getBaseMap());
        map.setActiveMapList(mapSelectionPanel.getCheckList());
        map.setIntervalBoundaries(); 
        map.setNodeThresholdSliderValue(visualizationSettingsPanel.getNodeThresholdSliderValue());
        map.setLinkThresholdSliderValue(visualizationSettingsPanel.getLinkThresholdSliderValue());
        //moved from Visualization panel to Maps Panel:
        //map.setFilterOnBaseMap(visualizationSettingsPanel.getFilterOnBaseMap());
        map.setFilterOnBaseMap(mapSelectionPanel.getFilterOnBaseMap());
    }
    
    public void adjustGUIToMergeMap(LWMergeMap map)
    {
        visualizationSettingsPanel.setNodeThresholdSliderValue(map.getNodeThresholdSliderValue());
        visualizationSettingsPanel.setLinkThresholdSliderValue(map.getLinkThresholdSliderValue());
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
