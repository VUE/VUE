
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
 ******************************************
 *
 * VoteVisualizationSettingsPanel.java
 *
 * Created on May 14, 2007, 2:08 PM
 *
 * @version $Revision: 1.6 $ / $Date: 2007-05-31 15:15:03 $ / $Author: dan $
 * @author dhelle01
 *
 * 
 *
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
        
        GridBagLayout voteLayout = new GridBagLayout();
        GridBagConstraints voteConstraints = new GridBagConstraints();
        setLayout(voteLayout);
        
        tufts.vue.PolygonIcon lineIcon = new tufts.vue.PolygonIcon(new java.awt.Color(153,153,153));
        lineIcon.setIconWidth(500);
        lineIcon.setIconHeight(1);
        JLabel iconLabel = new JLabel(lineIcon);
        voteConstraints.insets = new Insets(35,0,0,0);
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteLayout.setConstraints(iconLabel,voteConstraints);
        add(iconLabel);
        
        JLabel defineThresholdMessageLabel = new JLabel(defineThresholdMessage);
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
        java.util.Dictionary labels = nodeThresholdSlider.getLabelTable();
        java.util.Enumeration e = labels.elements();
        while(e.hasMoreElements())
        {
            Object label = e.nextElement();
            if(label instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent)label).setFont(new Font("Courier",Font.PLAIN,9));
            }
        }
        
        nodeThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        
        JLabel nodeLabel = new JLabel("Nodes:");
        
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
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        voteConstraints.gridwidth = 1;
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteLayout.setConstraints(nodeLabel,voteConstraints);
        add(nodeLabel);

        voteLayout.setConstraints(nodeThresholdSlider,voteConstraints);

        percentageDisplay = new JLabel("Nodes that are on at least " + nodeThresholdSlider.getValue() + "% of the maps will be included");
        
        voteLayout.setConstraints(percentageDisplay,voteConstraints);
        add(percentageDisplay);
        add(nodeThresholdSlider);

        JLabel linkPanel = new JLabel("Links:");
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteConstraints.insets= new java.awt.Insets(30,40,0,0);

        voteLayout.setConstraints(linkPanel,voteConstraints);
        add(linkPanel);
        voteConstraints.insets = new Insets(0,40,0,0);
        
        linkThresholdSlider = new JSlider(0,100,tufts.vue.LWMergeMap.THRESHOLD_DEFAULT);
        
        linkThresholdSlider.setPaintTicks(true);
        linkThresholdSlider.setMajorTickSpacing(10);
        linkThresholdSlider.setPaintLabels(true);
        java.util.Dictionary linkLabels = linkThresholdSlider.getLabelTable();
        java.util.Enumeration le = linkLabels.elements();
        while(le.hasMoreElements())
        {
            Object linkLabel = le.nextElement();
            if(linkLabel instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent)linkLabel).setFont(new Font("Courier",Font.PLAIN,9));
            }
        }
        
        linkThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));

        linkPercentageDisplay = new JLabel("Links that are on at least " + linkThresholdSlider.getValue()+"% of the maps will be included");
        voteLayout.setConstraints(linkPercentageDisplay,voteConstraints);
        add(linkPercentageDisplay);
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        voteConstraints.anchor = GridBagConstraints.NORTHWEST;
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
