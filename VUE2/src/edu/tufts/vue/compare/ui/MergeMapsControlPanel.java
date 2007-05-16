
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
 * @version $Revision: 1.3 $ / $Date: 2007-05-16 02:14:14 $ / $Author: dan $
 * @author dhelle01
 *
 * 
 *
 */

package edu.tufts.vue.compare.ui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import tufts.vue.*;
import tufts.vue.gui.*;

public class MergeMapsControlPanel extends JPanel {
    
    private MapsSelectionPanel mapSelectionPanel;
    private VisualizationSettingsPanel visualizationSettingsPanel;
    private JButton generateButton;
    
    public MergeMapsControlPanel(final DockWindow dw) 
    {
        setLayout(new BorderLayout());
        mapSelectionPanel = MapsSelectionPanel.getMapSelectionPanel();
        visualizationSettingsPanel = new VisualizationSettingsPanel();
        final JTabbedPane tabs = new JTabbedPane();
        //mapSelectionPanel.setBackground(tabs..getBackground());
        tabs.addTab("Select Maps",mapSelectionPanel);
        //tabs.addTab("Visualization Settings",new WeightVisualizationSettingsPanel());
        //tabs.addTab("Visualization Settings",new VoteVisualizationSettingsPanel());
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
        
        tabs.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent e)
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
        generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
               merge.setMapList(mapSelectionPanel.getMapList());
               merge.setBaseMap(mapSelectionPanel.getBaseMap());
               //merge.setNodeThresholdSliderValue(VoteVisualizationSettingsPanel.getSharedPanel().getNodeThresholdSlider());
               
               setMergeMapSettings(merge);
               
               if(visualizationSettingsPanel.getVisualizationSettingsType() == VisualizationSettingsPanel.VOTE)
                 merge.fillAsVoteMerge();
               else
                 merge.fillAsWeightMerge();
               MapViewer v = VUE.displayMap(merge);
               
               tufts.vue.LWCEvent event = new tufts.vue.LWCEvent(v,merge,new LWComponent.Key("Merge Map Displayed"));
               v.LWCChanged(event);
           }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(generateButton);
        add(buttonPanel,BorderLayout.SOUTH);
        dw.setContent(this);
        dw.setSize(650,550);
        dw.setResizeEnabled(false);
        if(getRootPane()!=null)
        {    
          getRootPane().setDefaultButton(generateButton);
        }
        dw.setVisible(true);
    }
    
    public void setMergeMapSettings(LWMergeMap map)
    {
        map.setIntervalBoundaries(); 
    }
    
    /*public java.awt.Dimension getPreferredSize()
    {
        return new Dimension(650,550);
    }*/
}
