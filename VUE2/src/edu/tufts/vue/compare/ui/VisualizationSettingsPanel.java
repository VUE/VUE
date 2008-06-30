
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/*

 *
 * @author dhelle01
 */
public class VisualizationSettingsPanel extends JPanel implements ActionListener {

    // visualization types
    public final static int VOTE = 1;
    public final static int WEIGHT = 0;
    
    public final static String VOTE_STRING = "Vote";
    public final static String WEIGHT_STRING = "Weight";
    public final static String visualizationSettingsChoiceMessage = "Which type of visualization would you like to use?";
    //moving to Select Maps Panel (and changing to "layout")
    //public final static String filterOnBaseMapMessageString = "Only include items found on the guide map";
    
    private JComboBox visualizationChoice;
    
    private GridBagLayout gridBag;
    private GridBagConstraints gridBagConstraints;
    
    private VoteVisualizationSettingsPanel votePanel = VoteVisualizationSettingsPanel.getSharedPanel();
    private WeightVisualizationSettingsPanel weightPanel;// = WeightVisualizationSettingsPanel.getSharedPanel();
    
    private JComboBox weightParameterChoice;
    
    private JLabel weightParameterChoiceLabel;
    
    // see below -- next variable is for VUE-607 (todo: use an ItemListener on the combo box instead)
    private int oldVisualizationSetting = WEIGHT;
   
    // the following boolean specifies to the weight panel a design decision that
    // makes layout of nodes/links drop alignment with visualization choice drop down a bit faster
    // to implement -- in the old interface (tufts.vue.MergeMapsChooser) this drop down aligned with
    // other weight controls 
    // the following flag provides a means to choose between these options dynamically in future.
    // Could be useful if weight visualization panel separates from visualization panel at any point
    // (or it becomes some sort of plug in)
    // note: the combo box itself is still defined and instantiated in the weight panel
    private static final boolean weightParameterChoiceDisplayedHere = true;
    
    public VisualizationSettingsPanel() 
    {
        
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        weightPanel = new WeightVisualizationSettingsPanel(weightParameterChoiceDisplayedHere);
        //weightPanel.setBorder(BorderFactory.createEmptyBorder(15,0,0,0));
        weightParameterChoice = weightPanel.getParameterCombo();
        
        setOpaque(false);
        
        /*if(tufts.Util.isWindowsPlatform())
        {
            setOpaque(true);
            setBackground(java.awt.Color.WHITE);
        }*/
        
        gridBag = new GridBagLayout();
        gridBagConstraints = new GridBagConstraints();
        
        gridBagConstraints.weighty =0.0;
        
        setLayout(gridBag);
        
        
        final String[] choices = {"Weight","Vote"};
        visualizationChoice = new JComboBox(choices)
        {
            public java.awt.Dimension getMinimumSize()
            {
               return new java.awt.Dimension(/*getGraphics().getFontMetrics().charsWidth(choices[0].toCharArray(),0,choices[0].length())+*/80,
                                             super.getPreferredSize().height);      
            }
        };
        visualizationChoice.setFont(tufts.vue.gui.GUI.LabelFace);
        visualizationChoice.addActionListener(this);
        
        JLabel visualizationSettingsChoiceLabel = new JLabel(visualizationSettingsChoiceMessage);
        visualizationSettingsChoiceLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        
        gridBagConstraints.weightx = 0.5;
        
        //gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(15,10,15,8);
        gridBag.setConstraints(visualizationSettingsChoiceLabel,gridBagConstraints);
        add(visualizationSettingsChoiceLabel);
        gridBagConstraints.insets = new java.awt.Insets(0,0,0,0);
        
        //gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        //gridBagConstraints.anchor = GridBagConstraints.CENTER;
        //gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(visualizationChoice,gridBagConstraints);
        add(visualizationChoice);
           
        setUpParameterChoiceGUI();        
     
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        //gridBagConstraints.insets = new java.awt.Insets(0,0,60,0);
        gridBag.setConstraints(votePanel,gridBagConstraints);
        gridBag.setConstraints(weightPanel,gridBagConstraints);
        add(weightPanel);
        
    }
    
    public void setUpParameterChoiceGUI()
    {
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;;
        weightParameterChoiceLabel = new JLabel("Set parameters for:");
        weightParameterChoiceLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        //gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(15,10,15,8);
        gridBag.setConstraints(weightParameterChoiceLabel,gridBagConstraints);
        add(weightParameterChoiceLabel);
        gridBagConstraints.insets = new java.awt.Insets(0,0,0,0);
        
        //gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        //gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(weightParameterChoice,gridBagConstraints);
        weightParameterChoice.setFont(tufts.vue.gui.GUI.LabelFace);
        add(weightParameterChoice); 
    }
    
    public void setVisualizationSettingsType(int type)
    {
        visualizationChoice.setSelectedIndex(type);
    }
    
    public int getVisualizationSettingsType()
    {   
        if(visualizationChoice.getSelectedIndex() == 0)
        {
            return WEIGHT;
        }
        else
        {
            return VOTE;
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        
        //VUE-607 (at least part of it) was when combo state has not actually changed
        if(getVisualizationSettingsType() == oldVisualizationSetting)
        {
            return;
        }
        else
        {
            oldVisualizationSetting = getVisualizationSettingsType();
        }
        
        if(e.getSource() == visualizationChoice)
        {
            if(getVisualizationSettingsType() == VOTE)
            {
                remove(weightParameterChoiceLabel);
                remove(weightParameterChoice);
                remove(weightPanel);
                //moved bottonPanel to Maps Selection Panel
                //remove(bottomPanel);
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0,0,60,0);
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
                gridBag.setConstraints(votePanel,gridBagConstraints);
                
                getTopLevelAncestor().setSize(new java.awt.Dimension(535,535));
                
                add(votePanel);
                //add(bottomPanel);
                getRootPane().setSize(new java.awt.Dimension(535,535));
                getRootPane().revalidate();
                getRootPane().repaint();
                
                //getTopLevelAncestor().setSize(new java.awt.Dimension(535,535));
                //getTopLevelAncestor().revalidate();
                getTopLevelAncestor().repaint();
                revalidate();
                repaint();
            }
            if(getVisualizationSettingsType() == WEIGHT)
            {
                remove(votePanel);
                //remove(bottomPanel);
                
                setUpParameterChoiceGUI();
                
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                gridBagConstraints.weighty = 1.0;
                //gridBagConstraints.insets = new java.awt.Insets(0,0,60,0);
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
                gridBag.setConstraints(weightPanel,gridBagConstraints);

                getTopLevelAncestor().setSize(new java.awt.Dimension(535,535));
                
                add(weightPanel);
                //add(bottomPanel);
                //getTopLevelAncestor().setSize(new java.awt.Dimension(535,540));
               // getTopLevelAncestor().revalidate();
                
                getRootPane().setSize(new java.awt.Dimension(535,535));
                getRootPane().revalidate();
                getRootPane().repaint();
                
                getTopLevelAncestor().repaint();
                //System.out.println("VSP: rootpane class: " + getRootPane().getClass());
                //System.out.println("VSP: top level ancestor: " + getTopLevelAncestor().getClass());
                revalidate();
                repaint();
            }
        }
    }
    
    public java.util.List<Double> getNodeIntervalBoundaries()
    {
        return weightPanel.getNodeIntervalBoundaries();
    }
    
    public java.util.List<Double> getLinkIntervalBoundaries()
    {
        return weightPanel.getLinkIntervalBoundaries();
    }
    
    // not yet needed -- implement if return to dynamic load and readjust
    // behavior as in tufts.vue.MergeMapsChooser
    /*public void setLinkIntervalBoundaries(java.util.List<Double> boundaries)
    {
        //weightPanel.setLinkIntervalBoundaries(boundaries);
    }*/
    
    public void setNodeThresholdSliderValue(int value)
    {
        votePanel.setNodeThresholdSliderValue(value);
    }
    
    public int getNodeThresholdSliderValue()
    {
        return votePanel.getNodeThresholdSliderValue();
    }
    
    public void setLinkThresholdSliderValue(int value)
    {
        votePanel.setLinkThresholdSliderValue(value);
    }
    
    public int getLinkThresholdSliderValue()
    {
        return votePanel.getLinkThresholdSliderValue();
    }
    
}
