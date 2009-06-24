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
package edu.tufts.vue.preferences.implementations;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import tufts.vue.VueResources;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.generics.BasePref;

public class ShowIconsPreference extends BasePref implements ItemListener
{
	private static final boolean DEBUG = false;
	private String category;
	private String name;
	private String description;
	
	private Object defaultValue;
	
	private Object previousResourceValue = null;
	//private Object previousBehaviorValue = null;
	private Object previousNotesValue = null;
	private Object previousPathwayValue = null;
	private Object previousMetaDataValue = null;
	private Object previousHierarchyValue = null;
	
	private JCheckBox resourceCheckbox = new JCheckBox();	
	//private JCheckBox behaviorCheckbox = new JCheckBox();
	private JCheckBox notesCheckbox = new JCheckBox();
	private JCheckBox pathwayCheckbox = new JCheckBox();
	private JCheckBox metaDataCheckbox = new JCheckBox();
	private JCheckBox hierarchyCheckbox = new JCheckBox();
	
	private String resourceIconName = "resourceIcon";
	//private String behaviorIconName = "behaviorIcon";
	private String notesIconName = "notesIcon";
	private String pathwayIconName = "pathwayIcon";
	private String metaDataIconName = "metaDataIcon";
	private String hierarchyIconName = "hierarchyIcon";

	private boolean resourceIconDefault = true;
	//private boolean behaviorIconDefault = true;
	private boolean notesIconDefault = true;
	private boolean pathwayIconDefault = true;
	private boolean metaDataIconDefault = true;
	private boolean hierarchyIconDefault = true;

	public ShowIconsPreference()
	{
		this.category=edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY;
		//this.key = "showNodeIcons";
		this.name = VueResources.getString("preference.showicon.title");
		this.description = VueResources.getString("preference.showicon.decsription");
		this.defaultValue = true;

		edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
	}

	public JComponent getPreferenceUI() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);

		JLabel titleLabel = new JLabel(getTitle());
		Font f = titleLabel.getFont().deriveFont(Font.BOLD);
		titleLabel.setFont(f);

		JTextArea descTextArea = new JTextArea(getDescription());
		final Font defaultFont = panel.getFont();
		descTextArea.setFont(defaultFont);
		descTextArea.setColumns(30);
		descTextArea.setLineWrap(true);
		descTextArea.setWrapStyleWord(true);

		GridBagConstraints gbConstraints = new GridBagConstraints();
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.gridwidth = 1;
		gbConstraints.fill = GridBagConstraints.HORIZONTAL;
		gbConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
		gbConstraints.weightx = 1;
		gbConstraints.weighty = 0;
		gbConstraints.insets = new Insets(15, 10, 0, 10);

		panel.add(titleLabel, gbConstraints);

		gbConstraints.gridy = 1;
		panel.add(descTextArea, gbConstraints);

		resourceCheckbox.setBackground(Color.WHITE);
		//behaviorCheckbox.setBackground(Color.WHITE);
		notesCheckbox.setBackground(Color.WHITE);
		pathwayCheckbox.setBackground(Color.WHITE);
		metaDataCheckbox.setBackground(Color.WHITE);
		hierarchyCheckbox.setBackground(Color.WHITE);

		resourceCheckbox.setText(VueResources.getString("jlabel.resource"));
		notesCheckbox.setText(VueResources.getString("jlabel.notes"));
		pathwayCheckbox.setText(VueResources.getString("jlabel.pathways"));
		metaDataCheckbox.setText(VueResources.getString("jlabel.keywords"));
		hierarchyCheckbox.setText(VueResources.getString("jlabel.hierarchy"));

		gbConstraints.gridy = 2;
		gbConstraints.weightx = 0;
		gbConstraints.fill = GridBagConstraints.NONE;
		gbConstraints.insets = new Insets(15, 30, 0, 10);
		panel.add(resourceCheckbox, gbConstraints);

		//gbConstraints.gridy=3;
		//panel.add(behaviorCheckbox, gbConstraints);

		gbConstraints.gridy=4;
		gbConstraints.insets = new Insets(10, 30, 0, 10);
		panel.add(notesCheckbox, gbConstraints);

		gbConstraints.gridy=5;
		panel.add(pathwayCheckbox, gbConstraints);

		gbConstraints.gridy=6;
		panel.add(metaDataCheckbox, gbConstraints);

		gbConstraints.gridy=7;
		gbConstraints.fill = GridBagConstraints.REMAINDER;
		gbConstraints.weighty = 1;
		panel.add(hierarchyCheckbox, gbConstraints);

		resourceCheckbox.addItemListener(this);
		//behaviorCheckbox.addItemListener(this);
		notesCheckbox.addItemListener(this);
		pathwayCheckbox.addItemListener(this);
		metaDataCheckbox.addItemListener(this);
		hierarchyCheckbox.addItemListener(this);

		resourceCheckbox.setSelected(((Boolean)getValue(resourceIconName)).booleanValue());
		//behaviorCheckbox.setSelected(((Boolean)getValue(behaviorIconName)).booleanValue());
		notesCheckbox.setSelected(((Boolean)getValue(notesIconName)).booleanValue());
		pathwayCheckbox.setSelected(((Boolean)getValue(pathwayIconName)).booleanValue());
		metaDataCheckbox.setSelected(((Boolean)getValue(metaDataIconName)).booleanValue());
		hierarchyCheckbox.setSelected(((Boolean)getValue(hierarchyIconName)).booleanValue());

		if (DEBUG) {
			panel.setBackground(Color.CYAN);
			titleLabel.setBackground(Color.MAGENTA);
			titleLabel.setOpaque(true);
			descTextArea.setBackground(Color.MAGENTA);
			descTextArea.setOpaque(true);
			resourceCheckbox.setBackground(Color.MAGENTA);
			resourceCheckbox.setOpaque(true);
			notesCheckbox.setBackground(Color.MAGENTA);
			notesCheckbox.setOpaque(true);
			pathwayCheckbox.setBackground(Color.MAGENTA);
			pathwayCheckbox.setOpaque(true);
			metaDataCheckbox.setBackground(Color.MAGENTA);
			metaDataCheckbox.setOpaque(true);
			hierarchyCheckbox.setBackground(Color.MAGENTA);
			hierarchyCheckbox.setOpaque(true);
		}

		return panel;
	}

	public boolean getMetaDataIconValue()
	{
		return ((Boolean)getValue(metaDataIconName)).booleanValue();
	}
	
	public boolean getHierarchyIconValue()
	{
		return ((Boolean)getValue(hierarchyIconName)).booleanValue();
	}
	
	public boolean getResourceIconValue()
	{
		return ((Boolean)getValue(resourceIconName)).booleanValue();				
	}
	
	public boolean getBehaviorIconValue()
	{
		return false;//((Boolean)getValue(behaviorIconName)).booleanValue();
	}
	
	public boolean getNotesIconValue()
	{
		return ((Boolean)getValue(notesIconName)).booleanValue();
	}
	
	public boolean getPathwayIconValue()
	{
		return ((Boolean)getValue(pathwayIconName)).booleanValue();
	}
	
	public void itemStateChanged(ItemEvent e) {
		JCheckBox box = (JCheckBox)e.getSource();
		//Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		//p.putBoolean(getPrefName(), box.isSelected());
		if (box.equals(resourceCheckbox))
			setValue(resourceIconName,Boolean.valueOf(box.isSelected()));
		//else if (box.equals(behaviorCheckbox))
		//	setValue(behaviorIconName,Boolean.valueOf(box.isSelected()));
		else if (box.equals(notesCheckbox))
			setValue(notesIconName,Boolean.valueOf(box.isSelected()));
		else if (box.equals(pathwayCheckbox))
			setValue(pathwayIconName,Boolean.valueOf(box.isSelected()));
		else if (box.equals(metaDataCheckbox))
			setValue(metaDataIconName,Boolean.valueOf(box.isSelected()));
		else if (box.equals(hierarchyCheckbox))
			setValue(hierarchyIconName,Boolean.valueOf(box.isSelected()));
		
	}
	
	public JCheckBox getCheckBox()
	{
		return resourceCheckbox;
	}
	
	public void setValue(String prefName, Object b)
	{
		
		if (prefName.equals(resourceIconName))
			previousResourceValue = Boolean.valueOf(resourceCheckbox.isSelected());
		//else if (prefName.equals(behaviorIconName))
		//	previousBehaviorValue = Boolean.valueOf(behaviorCheckbox.isSelected());
		else if (prefName.equals(notesIconName))
			previousNotesValue = Boolean.valueOf(notesCheckbox.isSelected());
		else if (prefName.equals(pathwayIconName))
			previousPathwayValue = Boolean.valueOf(pathwayCheckbox.isSelected());
		else if (prefName.equals(metaDataIconName))
			previousMetaDataValue = Boolean.valueOf(metaDataCheckbox.isSelected());
		else if (prefName.equals(hierarchyIconName))	
			previousHierarchyValue = Boolean.valueOf(hierarchyCheckbox.isSelected());		
		 
		
		
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		p.putBoolean(getPrefName(prefName), ((Boolean)b).booleanValue());
		_fireVuePrefEvent(prefName);
	}
	
	public Object getValue(String prefName){
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		Boolean b = Boolean.valueOf(p.getBoolean(getPrefName(prefName), ((Boolean)getDefaultValue(prefName)).booleanValue()));
		return b;
	
	}
	
	public Object getPreviousValue(String prefName)
	{
		if (prefName.equals(resourceIconName))
			return (previousResourceValue == null) ? getDefaultValue(prefName) : previousResourceValue;
		//else if (prefName.equals(behaviorIconName))
		//	return (previousBehaviorValue == null) ? getDefaultValue(prefName) : previousBehaviorValue;
		else if (prefName.equals(notesIconName))
			return (previousNotesValue == null) ? getDefaultValue(prefName) : previousNotesValue;
		else if (prefName.equals(pathwayIconName))
			return (previousPathwayValue == null) ? getDefaultValue(prefName) : previousPathwayValue;
		else if (prefName.equals(metaDataIconName))
			return (previousMetaDataValue == null) ? getDefaultValue(prefName) : previousMetaDataValue;
		else if (prefName.equals(hierarchyIconName))	
			return (previousHierarchyValue == null) ? getDefaultValue(prefName) : previousHierarchyValue;
		else
			return new Boolean(true);
	}

	public Object getDefaultValue(String prefName)
	{
		if (prefName.equals(resourceIconName))
			return resourceIconDefault;
		//else if (prefName.equals(behaviorIconName))
		//	return behaviorIconDefault;
		else if (prefName.equals(notesIconName))
			return notesIconDefault;
		else if (prefName.equals(pathwayIconName))
			return pathwayIconDefault;
		else if (prefName.equals(metaDataIconName))
			return metaDataIconDefault;
		else if (prefName.equals(hierarchyIconName))	
			return hierarchyIconDefault;
		else
			return new Boolean(true);
	}

	protected synchronized void _fireVuePrefEvent(String prefName) {
		VuePrefEvent event = new VuePrefEvent(this,getPreviousValue(prefName),getValue());

		Iterator listeners = _listeners.iterator();

		while(listeners.hasNext()) {
			((VuePrefListener)listeners.next()).preferenceChanged(event);
		}
	}

	public String getDescription() { 
		return description;
	}

	public String getMessage()
	{
		return name;
	}
	public String getTitle() {
		return name;
	}

	public String getCategoryKey() { 
		return category;
	}

	public String getPrefName(String key) {
		return category + "." + key;
	}

	public Object getDefaultValue() {
		return new Boolean("true");
	}

	public String getPrefName() {
		return "";
	}

	public Object getValue() {
		return new Boolean("true");
	}

	public void setValue(Object i) {
	}

	public Object getPreviousValue() {
		return null;
	}	
}
