/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.tufts.vue.preferences.generics;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.interfaces.VueIntegerPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

/**
 * @author Mike Korcynski
 *
 */
public abstract class GenericSliderPreference implements VueIntegerPreference, ChangeListener
{
	
	private String message;
	private JSlider slider = new JSlider();
	
	public GenericSliderPreference()
	{
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		slider.setBackground(Color.WHITE);
        slider.setOrientation(JSlider.VERTICAL);        
        slider.setMajorTickSpacing(83);
        slider.setPaintTicks(true);        
        slider.setSnapToTicks(true);
        slider.setMinimum(0);
        slider.setMaximum(500);
	}
			

	public JSlider getSlider()
	{
		return slider;
	}
	
	public abstract String getTitle();
	public abstract String getDescription();
	
	public JComponent getPreferenceUI() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);
		JLabel titleLabel = new JLabel(getTitle());
		Font f = titleLabel.getFont().deriveFont(Font.BOLD);
		titleLabel.setFont(f);
		JLabel descLabel = new JLabel(getDescription());
		GridBagConstraints gbConstraints = new GridBagConstraints();
	    
		gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 1;
        gbConstraints.fill=GridBagConstraints.HORIZONTAL;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.weightx=1;
        gbConstraints.weighty=0;
        gbConstraints.insets = new Insets(15,10,2,2);
        
        panel.add(titleLabel, gbConstraints);
    
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		panel.add(descLabel, gbConstraints);
		
		gbConstraints.gridx=0;
		gbConstraints.gridy=2;
		gbConstraints.weightx=1;
        gbConstraints.weighty=1;
        gbConstraints.insets = new Insets(15,30,15,30);
        
        JPanel booleanPanel = new JPanel();
        booleanPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        booleanPanel.setBackground(Color.WHITE);
        
        
        booleanPanel.add(slider);
        booleanPanel.add(new JLabel(getMessage()));
        panel.add(booleanPanel, gbConstraints);
	return panel;
	}
	
	public Class getPrefRoot()
	{
		return edu.tufts.vue.preferences.PreferencesManager.class;
	}
	
	public String getMessage(){
		return message;
	}

	public int getValue(){
		return slider.getValue();
	}
	
	public void setMessage(String s){
		this.message = s;
	}
	
	public void setValue(int b)
	{
		
	}
	
}
