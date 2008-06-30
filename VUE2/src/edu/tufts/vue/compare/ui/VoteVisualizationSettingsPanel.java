
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
import javax.swing.event.*;

public class VoteVisualizationSettingsPanel extends JPanel {
    
    public final static String defineThresholdMessage = "Define threshold for nodes and links:";
    
    private JCheckBox filterChoice;
    private JSlider nodeThresholdSlider;
    private boolean mousePressed;
    private JLabel percentageDisplay;
    private JSlider linkThresholdSlider;
    private JLabel linkPercentageDisplay;
    
    private static VoteVisualizationSettingsPanel panel = new VoteVisualizationSettingsPanel();
    
    public static VoteVisualizationSettingsPanel getSharedPanel()
    {
        return panel;
    }
    
    public VoteVisualizationSettingsPanel() 
    {
        setOpaque(false);
        
        //setBorder(BorderFactory.createEmptyBorder(100,0,0,15));
        
        GridBagLayout voteLayout = new GridBagLayout();
        GridBagConstraints voteConstraints = new GridBagConstraints();
        setLayout(voteLayout);
        
        tufts.vue.PolygonIcon lineIcon = new tufts.vue.PolygonIcon(new java.awt.Color(153,153,153));
        lineIcon.setIconWidth(500);
        lineIcon.setIconHeight(1);
        JLabel iconLabel = new JLabel(lineIcon);
        voteConstraints.insets = new Insets(40,0,0,0);
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteLayout.setConstraints(iconLabel,voteConstraints);
        add(iconLabel);
        
        JLabel defineThresholdMessageLabel = new JLabel(defineThresholdMessage);
        defineThresholdMessageLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        nodeThresholdSlider = new JSlider(0,100,tufts.vue.LWMergeMap.THRESHOLD_DEFAULT);
        nodeThresholdSlider.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                //if(!nodeThresholdSlider.getValueIsAdjusting())
                //{
                    //System.out.println("vvsp: node slider value change:" + nodeThresholdSlider.getValue() );
                    percentageDisplay.setText("Nodes that are on at least " + nodeThresholdSlider.getValue() + "% of the maps will be included");
                    percentageDisplay.repaint();
                //}
            }
        });
        
        
        // this was for undo with panel - may be moving to LWMergeMap
        /*nodeThresholdSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent me)
            {
                mousePressed = true;
            }
        });*/
        
        
        nodeThresholdSlider.setPaintTicks(true);
        nodeThresholdSlider.setMajorTickSpacing(10);
        nodeThresholdSlider.setPaintLabels(true);
       // nodeThresholdSlider.setOpaque(true);
        if(tufts.Util.isWindowsPlatform())
        {    
          nodeThresholdSlider.setBackground(java.awt.Color.WHITE);//getBackground());
        }
        
        java.util.Dictionary labels = nodeThresholdSlider.getLabelTable();
        java.util.Enumeration e = labels.elements();
        while(e.hasMoreElements())
        {
            Object label = e.nextElement();
            if(label instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent)label).setFont(tufts.vue.gui.GUI.LabelFace);//(new Font("Courier",Font.PLAIN,9));
            }
        }
        
        nodeThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        
        JLabel nodeLabel = new JLabel("Nodes:");
        nodeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        
        voteConstraints.anchor = GridBagConstraints.WEST;
        voteConstraints.insets = new Insets(40,15,20,0);
        voteLayout.setConstraints(defineThresholdMessageLabel,voteConstraints);
        add(defineThresholdMessageLabel);
        
        voteConstraints.anchor = GridBagConstraints.CENTER;
        voteConstraints.fill = GridBagConstraints.HORIZONTAL;
        voteConstraints.insets = new Insets(0,0,0,0);
        voteConstraints.gridwidth = 1;
   
        voteConstraints.fill = GridBagConstraints.NONE;
        voteConstraints.anchor = GridBagConstraints.WEST;
        voteConstraints.insets= new java.awt.Insets(0,40,5,0);
        voteConstraints.gridwidth = 1;
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteLayout.setConstraints(nodeLabel,voteConstraints);
        add(nodeLabel);

        percentageDisplay = new JLabel("Nodes that are on at least " + nodeThresholdSlider.getValue() + "% of the maps will be included");
        percentageDisplay.setFont(tufts.vue.gui.GUI.LabelFace);
        
        voteLayout.setConstraints(percentageDisplay,voteConstraints);
        add(percentageDisplay);
        
        
        voteConstraints.fill = GridBagConstraints.HORIZONTAL;
        voteConstraints.insets= new java.awt.Insets(0,40,0,35);
        voteLayout.setConstraints(nodeThresholdSlider,voteConstraints);
        add(nodeThresholdSlider);
        voteConstraints.fill = GridBagConstraints.NONE;
        
        JLabel linkPanel = new JLabel("Links:");
        linkPanel.setFont(tufts.vue.gui.GUI.LabelFace);
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteConstraints.insets= new java.awt.Insets(20,40,5,0);

        voteLayout.setConstraints(linkPanel,voteConstraints);
        add(linkPanel);
        voteConstraints.insets = new Insets(0,40,0,0);
        
        linkThresholdSlider = new JSlider(0,100,tufts.vue.LWMergeMap.THRESHOLD_DEFAULT);
        
        linkThresholdSlider.setPaintTicks(true);
        linkThresholdSlider.setMajorTickSpacing(10);
        linkThresholdSlider.setPaintLabels(true);
        //linkThresholdSlider.setBackground(getBackground());
        if(tufts.Util.isWindowsPlatform())
        {
            linkThresholdSlider.setBackground(java.awt.Color.WHITE);
        }
        java.util.Dictionary linkLabels = linkThresholdSlider.getLabelTable();
        java.util.Enumeration le = linkLabels.elements();
        while(le.hasMoreElements())
        {
            Object linkLabel = le.nextElement();
            if(linkLabel instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent)linkLabel).setFont(tufts.vue.gui.GUI.LabelFace);//new Font("Courier",Font.PLAIN,9));
            }
        }
        
        linkThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));

        linkPercentageDisplay = new JLabel("Links that are on at least " + linkThresholdSlider.getValue()+"% of the maps will be included");
        linkPercentageDisplay.setFont(tufts.vue.gui.GUI.LabelFace);
        voteLayout.setConstraints(linkPercentageDisplay,voteConstraints);
        add(linkPercentageDisplay);
        voteConstraints.insets= new java.awt.Insets(0,40,0,35);
        voteConstraints.anchor = GridBagConstraints.NORTHWEST;
        voteConstraints.fill = GridBagConstraints.HORIZONTAL;
        voteLayout.setConstraints(linkThresholdSlider,voteConstraints);
        add(linkThresholdSlider);
        
        
        linkThresholdSlider.addChangeListener(new ChangeListener()
        {
           public void stateChanged(ChangeEvent e)
           {
              linkPercentageDisplay.setText("Links that are on at least " + linkThresholdSlider.getValue()+"% of the maps will be included");
           }
        });
        
    }
    
    public void setNodeThresholdSliderValue(int value)
    {
        nodeThresholdSlider.setValue(value);
    }
    
    public int getNodeThresholdSliderValue()
    {
        return nodeThresholdSlider.getValue();
    }
    
    public void setLinkThresholdSliderValue(int value)
    {
        linkThresholdSlider.setValue(value);
    }
    
    public int getLinkThresholdSliderValue()
    {
        return linkThresholdSlider.getValue();
    }
    
}
