
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
 * @version $Revision: 1.1 $ / $Date: 2007-05-11 15:53:24 $ / $Author: dan $
 * @author dhelle01
 *
 * 
 *
 */

package edu.tufts.vue.compare.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;

public class MergeMapsControlPanel extends JPanel {
    
    private MapsSelectionPanel mapSelectionPanel;
    private JButton generateButton;
    
    public MergeMapsControlPanel() 
    {
        //setSize(new java.awt.Dimension(650,550));
        setLayout(new BorderLayout());
        mapSelectionPanel = MapsSelectionPanel.getMapSelectionPanel();
        JTabbedPane tabs = new JTabbedPane();
        //mapSelectionPanel.setBackground(tabs..getBackground());
        tabs.addTab("Select Maps",mapSelectionPanel);
        //tabs.addTab("Visualization Settings",new WeightVisualizationSettingsPanel());
        add(tabs);
        generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               LWMergeMap merge = new LWMergeMap(LWMergeMap.getTitle());
               merge.setMapList(mapSelectionPanel.getMapList());
               merge.setBaseMap(mapSelectionPanel.getBaseMap());
               merge.fillAsVoteMerge();
               MapViewer v = VUE.displayMap(merge);
               
               tufts.vue.LWCEvent event = new tufts.vue.LWCEvent(v,merge,new LWComponent.Key("Merge Map Displayed"));
               v.LWCChanged(event);
           }
        });
        // has to wait for DockWindow:
        //getRootPane().setDefaultButton(generateButton);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(generateButton);
        add(buttonPanel,BorderLayout.SOUTH);
    }
    
    /*public java.awt.Dimension getPreferredSize()
    {
        return new Dimension(650,550);
    }*/
}
