
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
import javax.swing.event.*;

/*
 * VoteVisualizationSettingsPanel.java
 *
 * Created on May 14, 2007, 2:08 PM
 *
 * @author dhelle01
 */
public class VoteVisualizationSettingsPanel extends JPanel {
    
    public final static String defineThresholdMessage = "Define threshold for nodes and links:";
    
    private JCheckBox filterChoice;
    //private JPanel votePanel;
    //private WeightVisualizationSettingsPanel weightPanel;
    private JSlider nodeThresholdSlider;
    //private boolean nodeChangeProgrammatic;
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
        //defineThresholdMessageLabel.setBorder(BorderFactory.createEmptyBorder(0,0,20,0));
        
        //JPanel moreLessLabel = new JPanel();
        /*{
          public Dimension getPreferredSize()
          {
              return nodeThresholdSlider.getSize();
          }
        };*/
        
        //JLabel moreLabel = new JLabel("<< more",JLabel.LEFT);
        //JLabel lessLabel = new JLabel("less >>",JLabel.RIGHT);
        
        //JLabel moreLabel = new JLabel("<< more nodes",JLabel.LEFT);
        //moreLabel.setFont(new Font("Courier",Font.PLAIN,10));
        //JLabel lessLabel = new JLabel("less nodes >>",JLabel.RIGHT);
        //lessLabel.setFont(new Font("Courier",Font.PLAIN,10));
        
        //moreLessLabel.setLayout(new BorderLayout());
        //moreLessLabel.add(BorderLayout.WEST,moreLabel);
        //moreLessLabel.add(BorderLayout.EAST,lessLabel);
        
        nodeThresholdSlider = new JSlider(0,100,50);
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
        nodeThresholdSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent me)
            {
                mousePressed = true;
            }
        });
        
        
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
        
        
        //$
           nodeThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        //$
        
        JLabel nodeLabel = new JLabel("Nodes:");
        
        //voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //$
          //voteConstraints.gridwidth = 2;
          //voteConstraints.gridx = 0;
          //voteConstraints.gridy = 0;
        //$
        voteConstraints.anchor = GridBagConstraints.WEST;
        voteConstraints.insets = new Insets(40,0,20,0);
       // voteConstraints.anchor = GridBagConstraints.NORTHWEST;
        //voteConstraints.weightx = 1.0;
        voteLayout.setConstraints(defineThresholdMessageLabel,voteConstraints);
        add(defineThresholdMessageLabel);
        
        //voteConstraints.weightx =1.0;
        voteConstraints.anchor = GridBagConstraints.CENTER;
        voteConstraints.fill = GridBagConstraints.HORIZONTAL;
        voteConstraints.insets = new Insets(0,0,0,0);
        //$
          voteConstraints.gridwidth = 1;
          //voteConstraints.gridx = 1;
          //voteConstraints.gridy = 1;
        //$
       // voteLayout.setConstraints(moreLessLabel,voteConstraints);
        //xvotePanel.add(moreLessLabel);
        
        //$
          //voteConstraints.gridx = 0;
          //voteConstraints.gridy = 2;
        //$
        voteConstraints.fill = GridBagConstraints.NONE;
        voteConstraints.anchor = GridBagConstraints.WEST;
        //voteConstraints.weightx = 1.0;
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        //voteConstraints.gridwidth = GridBagConstraints.RELATIVE;
        voteConstraints.gridwidth = 1;
        //$
           //nodeLabel.setOpaque(true);
           //nodeLabel.setBackground(Color.YELLOW);
           //voteConstraints.gridx = 0;
           //voteConstraints.gridy = 2;
        //$
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteLayout.setConstraints(nodeLabel,voteConstraints);
        add(nodeLabel);
        //voteConstraints.insets = new java.awt.Insets(0,0,0,0);
        //$
          //voteConstraints.gridx = 1;
          //voteConstraints.gridy = 2;
        //$
        voteLayout.setConstraints(nodeThresholdSlider,voteConstraints);

        //add(nodeThresholdSlider);
        /*String nodePercentageText = "";
        if(nodeThresholdSlider.getValue()<10)
        {
            nodePercentageText = nodeThresholdSlider.getValue() + "%  ";
        }
        if(nodeThresholdSlider.getValue()<100)
        {
            nodePercentageText = nodeThresholdSlider.getValue() + "% ";
        }
        else
        {
            nodePercentageText = nodeThresholdSlider.getValue() + "%";
        }
        percentageDisplay = new JLabel("");
        percentageDisplay.setText(nodePercentageText);*/
        percentageDisplay = new JLabel("Nodes that are on at least " + nodeThresholdSlider.getValue() + "% of the maps will be included");
        /*{
           public Dimension getPreferredSize()
           {
               return (new JLabel("100%").getPreferredSize());
           }
        };*/
        //have created methods below to turn this on and off (so that changes during setup don't affect the map)
        //boolean method could be used to turn this on (if not already on) from outside this constructor
        //nodeThresholdSlider.addChangeListener(this);
        //voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //$
          //voteConstraints.gridx = 2;
          //voteConstraints.gridy = 2;
        //$
        //voteConstraints.insets = new java.awt.Insets(0,0,0,40);
        voteLayout.setConstraints(percentageDisplay,voteConstraints);
        add(percentageDisplay);
        add(nodeThresholdSlider);

        JLabel linkPanel = new JLabel("Links:");
        //voteConstraints.gridwidth = 1;
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        //$
          // voteConstraints.gridx = 0;
          // voteConstraints.gridy = 3;
        //$
        voteLayout.setConstraints(linkPanel,voteConstraints);
        add(linkPanel);
        
        linkThresholdSlider = new JSlider(0,100,50);
        
               // this was for undo handling within panel - may move to LWMergeMap
        /*
        linkThresholdSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent me)
            {
                mousePressed = true;
            }
        });*/
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
        
        //$
           linkThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        //$
        
        //add(linkThresholdSlider);
        linkPercentageDisplay = new JLabel("Links that are on at least " + linkThresholdSlider.getValue()+"% of the maps will be included");
        /*{
           public Dimension getPreferredSize()
           {
               return (new JLabel("100%").getPreferredSize());
           }
        };*/
        //linkThresholdSlider.addChangeListener(this);
        //voteConstraints.insets = new java.awt.Insets(0,40,100,0);
        //voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //$
           //voteConstraints.gridx = 2;
           //voteConstraints.gridy = 3;
        //$
        voteLayout.setConstraints(linkPercentageDisplay,voteConstraints);
        add(linkPercentageDisplay);
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        //$
           //voteConstraints.gridx = 1;
           //voteConstraints.gridy = 3;
        //$
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
        
 
       
        
        //voteConstraints.gridx = 0;
        //voteConstraints.gridy = 4;
        //filterChoice = new JCheckBox();
        //voteLayout.setConstraints(filterChoice,voteConstraints);
        
    }
    
    public int getNodeThresholdSliderValue()
    {
        return nodeThresholdSlider.getValue();
    }
    
    public int getLinkThresholdSliderValue()
    {
        return linkThresholdSlider.getValue();
    }
    
}
