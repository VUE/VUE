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

package edu.tufts.vue.preferences.generics;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.Preferences;

import javax.swing.*;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.interfaces.VuePreference;

/**
 * @author Mike Korcynski
 *
 */
public abstract class GenericBooleanPreference extends BasePref<Boolean> implements ItemListener 
{
    public static boolean getBoolean(String prefName, boolean defaultValue) {
        final Preferences p = Preferences.userNodeForPackage(getPrefRoot());
        return p.getBoolean(prefName, defaultValue);
    }

    public static Boolean getValue(String prefName, boolean defaultValue) {
        return getBoolean(prefName, defaultValue) ? Boolean.TRUE : Boolean.FALSE;
    }
    
    private JCheckBox value = new JCheckBox();
    private Boolean previousValue = null;

    // getValue is slow -- these may be checked constantly -- we locally
    // cache the value, and only update when the value changes.
    private boolean cachedValue;
	
    public GenericBooleanPreference() {}
	
    public GenericBooleanPreference(String prefName, Boolean defaultVal)
    { 
        cachedValue = getBoolean(prefName, defaultVal);
        getCheckBox().setSelected(cachedValue);
        //getCheckBox().setSelected(p.getBoolean(prefName,((Boolean)defaultVal).booleanValue()));        
    }

    protected void cacheCurrentValue() {
        cachedValue = getBoolean(getPrefName(), getDefaultValue());
    }
    
    /** interface VuePreference */
    public Boolean getValue() {
        return cachedValue ? Boolean.TRUE : Boolean.FALSE;
        
//         return getValue(getPrefName(), getDefaultValue().booleanValue());
// //         Preferences p = Preferences.userNodeForPackage(getPrefRoot());
// //         Boolean b = Boolean.valueOf(p.getBoolean(getPrefName(), ((Boolean)getDefaultValue()).booleanValue()));
// //         return b;
        
    }
	
    /** interface VuePreference */
    public void setValue(Boolean b) {
        previousValue = Boolean.valueOf(value.isSelected()); // better to use cachedValue?
        cachedValue = b.booleanValue();
        Preferences p = Preferences.userNodeForPackage(getPrefRoot());
        p.putBoolean(getPrefName(), cachedValue);
        _fireVuePrefEvent();
    }
    
    public final boolean isTrue() {
        return cachedValue;
    }
    public final boolean isFalse() {
        return !cachedValue;
    }

    public Boolean getPreviousValue()
    {
        if (previousValue == null)
            return (Boolean) getDefaultValue();
        else
            return previousValue;
    }
	
    public Boolean getDefaultValue()
    {
        return Boolean.TRUE;
    }
	
    public void itemStateChanged(ItemEvent e) {
        JCheckBox box = (JCheckBox)e.getSource();
        Preferences p = Preferences.userNodeForPackage(getPrefRoot());
        //p.putBoolean(getPrefName(), box.isSelected());
        setValue(Boolean.valueOf(box.isSelected()));
    }
	
    public JCheckBox getCheckBox()
    {
        return value;
    }
    public abstract String getTitle();
    public abstract String getDescription();
    public abstract String getMessage();
	
    public JComponent getPreferenceUI() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        GridBagLayout gbl = new GridBagLayout();
        panel.setLayout(gbl);
        JLabel titleLabel = new JLabel(getTitle());
        Font f = titleLabel.getFont().deriveFont(Font.BOLD);
        titleLabel.setFont(f);
        //JLabel descLabel = new JLabel(getDescription());
        JTextArea messageArea = new JTextArea(getDescription());
        final Font defaultFont = panel.getFont();
        messageArea.setFont(defaultFont);
        messageArea.setColumns(30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
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
        panel.add(messageArea, gbConstraints);
		
        gbConstraints.gridx=0;
        gbConstraints.gridy=2;
        gbConstraints.weightx=1;
        gbConstraints.weighty=1;
        gbConstraints.insets = new Insets(15,30,15,30);
        
        JPanel booleanPanel = new JPanel();
        booleanPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        booleanPanel.setBackground(Color.WHITE);
        
        value.setBackground(Color.WHITE);
        value.setLabel(getMessage());
        booleanPanel.add(value);
        booleanPanel.setOpaque(false);
     
        //  JLabel message = new JLabel(getMessage());
        // message.setBackground(Color.red);
        // message.setForeground(Color.black);
        // booleanPanel.add(message);
        getCheckBox().addItemListener(this);
        getCheckBox().setSelected(((Boolean)getValue()).booleanValue());
        panel.add(booleanPanel, gbConstraints);
	return panel;
    }
	
	
}
