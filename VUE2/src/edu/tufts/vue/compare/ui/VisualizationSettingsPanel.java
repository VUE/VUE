
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
 *******************************************
 *
 * VisualizationSettingsPanel.java
 *
 * Created on May 14, 2007, 2:45 PM
 *
 * @version $Revision: 1.8 $ / $Date: 2007-05-31 15:15:03 $ / $Author: dan $
 * @author dhelle01
 *
 *
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
    public final static int VOTE = 0;
    public final static int WEIGHT = 1;
    
    public final static String VOTE_STRING = "Vote";
    public final static String WEIGHT_STRING = "Weight";
    public final static String visualizationSettingsChoiceMessage = "Which type of visualization would you like to use?";
    //moving to Select Maps Panel (and changing to "layout")
    //public final static String filterOnBaseMapMessageString = "Only include items found on the guide map";
    
    private JComboBox visualizationChoice;
    
    private GridBagLayout gridBag;
    private GridBagConstraints gridBagConstraints;
    
    private VoteVisualizationSettingsPanel votePanel = VoteVisualizationSettingsPanel.getSharedPanel();
    private WeightVisualizationSettingsPanel weightPanel = WeightVisualizationSettingsPanel.getSharedPanel();
   
    //moved to Maps Selection Panel
    //private JPanel bottomPanel;
    //private JCheckBox filterOnBaseMap;
    
    public VisualizationSettingsPanel() 
    {
        setOpaque(false);
        gridBag = new GridBagLayout();
        gridBagConstraints = new GridBagConstraints();
        setLayout(gridBag);
        
        String[] choices = {"Weight","Vote"};
        visualizationChoice = new JComboBox(choices);
        visualizationChoice.addActionListener(this);
        
        JLabel visualizationSettingsChoiceLabel = new JLabel(visualizationSettingsChoiceMessage);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(15,10,15,8);
        gridBag.setConstraints(visualizationSettingsChoiceLabel,gridBagConstraints);
        add(visualizationSettingsChoiceLabel);
        gridBagConstraints.insets = new java.awt.Insets(0,0,0,0);
        
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(visualizationChoice,gridBagConstraints);
        add(visualizationChoice);
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0,0,60,0);
        gridBag.setConstraints(votePanel,gridBagConstraints);
        gridBag.setConstraints(weightPanel,gridBagConstraints);
        add(weightPanel);
        
        //bottomPanel = new JPanel();
        //filterOnBaseMap = new JCheckBox();
        //JLabel filterOnBaseMapMessage = new JLabel(filterOnBaseMapMessageString);
        //bottomPanel.add(filterOnBaseMap);
        //bottomPanel.add(filterOnBaseMapMessage);
        //gridBagConstraints.weighty = 0.0;
        //gridBag.setConstraints(bottomPanel,gridBagConstraints);
        //add(bottomPanel);
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
        if(e.getSource() == visualizationChoice)
        {
            if(getVisualizationSettingsType() == VOTE)
            {
                remove(weightPanel);
                //moved bottonPanel to Maps Selection Panel
                //remove(bottomPanel);
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0,0,60,0);
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
                gridBag.setConstraints(votePanel,gridBagConstraints);
                add(votePanel);
                //add(bottomPanel);
                revalidate();
                repaint();
            }
            if(getVisualizationSettingsType() == WEIGHT)
            {
                remove(votePanel);
                //remove(bottomPanel);
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0,0,60,0);
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
                gridBag.setConstraints(weightPanel,gridBagConstraints);
                add(weightPanel);
                //add(bottomPanel);
                revalidate();
                repaint();
            }
        }
    }
    
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
    
    
    /*
     *moved to SelectMapsPanel
    public boolean getFilterOnBaseMap()
    {
        if(filterOnBaseMap.isSelected())
            return true;
        else
            return false;
    }*/
    
}
