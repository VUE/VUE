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
import java.awt.Dimension;
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

public class MetadataSchemaPreference extends BasePref implements ItemListener
{
	private String category;
	private String name;
	private String description;
	
	private Object previousVraValue = null;
	private Object previousDublinCoreValue = null;
	
	private JCheckBox dublinCoreCheckbox = new JCheckBox();		
	private JCheckBox vraCheckbox = new JCheckBox();
	
	private String dublinCoreName = "dublinCore";
	private String vraName = "vra";
    
	private boolean dublinCoreDefault = true;
	private boolean vraDefault = false;
    private boolean defaultValue = true;
    
    private static final MetadataSchemaPreference metadataSchemaPrefernece = new MetadataSchemaPreference();
    
    public static MetadataSchemaPreference getInstance()
    {
    	return metadataSchemaPrefernece;
    }
    
	private MetadataSchemaPreference()
	{
		
		this.category=edu.tufts.vue.preferences.PreferenceConstants.METADATA_CATEGORY;
		//this.key = "showNodeIcons";
		this.name = "Metadata Schema";
		if (tufts.Util.isWindowsPlatform())
			this.description = "Enable metadata schema for use with \"Keywords\" and \"Search\" tools.";
		else
			this.description = "Enable metadata schema for use with \"Keywords\"\nand \"Search\" tools.";
		this.defaultValue = true;
	
		edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
	}
		
	public JComponent getPreferenceUI() {
		JPanel panel = new JPanel();
		panel.setMinimumSize(new Dimension(420,400));
		panel.setMaximumSize(new Dimension(420,400));
		panel.setBackground(Color.WHITE);
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);
		JLabel titleLabel = new JLabel(getTitle());
		Font f2 = titleLabel.getFont();
		Font f = titleLabel.getFont().deriveFont(Font.BOLD);
		titleLabel.setFont(f);
		JTextArea descLabel = new JTextArea(getDescription());
		descLabel.setFont(f2);
		descLabel.setEditable(false);
		GridBagConstraints gbConstraints = new GridBagConstraints();
		dublinCoreCheckbox.setBackground(Color.WHITE);
        vraCheckbox.setBackground(Color.WHITE);
        
		gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 2;
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
		gbConstraints.weightx=0.1;
		gbConstraints.gridwidth=1;
        gbConstraints.weighty=0;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;        
        gbConstraints.insets = new Insets(15,30,0,0);                              
        panel.add(dublinCoreCheckbox,gbConstraints);
        
        gbConstraints.gridx=1;
		gbConstraints.gridy=2;
		gbConstraints.weightx=1;
        gbConstraints.weighty=0;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.insets = new Insets(18,0,0,0);
        panel.add(new JLabel(VueResources.getString("jlabel.dublincore")),gbConstraints);
        
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=3;
        gbConstraints.weightx=.1;
        gbConstraints.weighty=8;
        gbConstraints.fill=GridBagConstraints.VERTICAL;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.insets = new Insets(0,30,260,0);
        panel.add(vraCheckbox,gbConstraints);
        
        gbConstraints.gridx=1;
        gbConstraints.gridy=3;
        gbConstraints.weightx=1;
        gbConstraints.weighty=8;
        gbConstraints.insets = new Insets(0,0,260,0);
        panel.add(new JLabel(VueResources.getString("jlabel.vra")),gbConstraints);
        
        
        dublinCoreCheckbox.addItemListener(this);
        vraCheckbox.addItemListener(this);
        
        dublinCoreCheckbox.setSelected(((Boolean)getValue(dublinCoreName)).booleanValue());
        vraCheckbox.setSelected(((Boolean)getValue(vraName)).booleanValue());
        return panel;
	}

	
	
	public boolean getDublinCoreValue()
	{
		return ((Boolean)getValue(dublinCoreName)).booleanValue();				
	}
		
	public boolean getVRAValue()
	{
		return ((Boolean)getValue(vraName)).booleanValue();
	}
		
	public void itemStateChanged(ItemEvent e) {
		JCheckBox box = (JCheckBox)e.getSource();
		//Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		//p.putBoolean(getPrefName(), box.isSelected());
		if (box.equals(dublinCoreCheckbox))
                {    
			setValue(dublinCoreName,Boolean.valueOf(box.isSelected()));
                        if(box.isSelected())
                        {
                            edu.tufts.vue.metadata.CategoryModel cats =
                                    tufts.vue.VUE.getCategoryModel();
                            
                            cats.loadDefaultVUEOntology(
                                    edu.tufts.vue.metadata.CategoryModel.DUBLIN_CORE);
                                    
                        }
                        if(!box.isSelected())
                        {
                            edu.tufts.vue.metadata.CategoryModel cats =
                                    tufts.vue.VUE.getCategoryModel();
                            
                            cats.removeDefaultOntology(
                                    edu.tufts.vue.metadata.CategoryModel.DUBLIN_CORE);
                        }
                }
		else if (box.equals(vraCheckbox))
                {
			setValue(vraName,Boolean.valueOf(box.isSelected()));
                        if(box.isSelected())
                        {
                            edu.tufts.vue.metadata.CategoryModel cats =
                                    tufts.vue.VUE.getCategoryModel();
                            
                            cats.loadDefaultVUEOntology(
                                    edu.tufts.vue.metadata.CategoryModel.VRA);
                                    
                        }
                        if(!box.isSelected())
                        {
                            edu.tufts.vue.metadata.CategoryModel cats =
                                    tufts.vue.VUE.getCategoryModel();
                            
                            cats.removeDefaultOntology(
                                    edu.tufts.vue.metadata.CategoryModel.VRA);
                        }
                }
		
	}
	
	public JCheckBox getCheckBox()
	{
		return dublinCoreCheckbox;
	}
	
	public void setValue(String prefName, Object b)
	{
		
		if (prefName.equals(dublinCoreName))
			previousDublinCoreValue = Boolean.valueOf(dublinCoreCheckbox.isSelected());
		else if (prefName.equals(vraName))
			previousVraValue = Boolean.valueOf(vraCheckbox.isSelected());
		 
		
		
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
		if (prefName.equals(dublinCoreName))
			return (previousDublinCoreValue != null) ?  getDefaultValue(prefName) :  previousDublinCoreValue;
		else if (prefName.equals(vraName))
			return (previousVraValue != null) ?  getDefaultValue(prefName) :  previousVraValue;
		else
			return new Boolean(true);
				
	}
	public Object getDefaultValue(String prefName)
	{
		if (prefName.equals(dublinCoreName))
			return dublinCoreDefault;
		//else if (prefName.equals(behaviorIconName))
		//	return  behaviorIconDefault;
		else if (prefName.equals(vraName))
			return vraDefault;		
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
		// TODO Auto-generated method stub
		return new Boolean("true");
	}

	public String getPrefName() {
		// TODO Auto-generated method stub
		return "";
	}

	public Object getValue() {
		// TODO Auto-generated method stub
		return new Boolean("true");
	}

	public void setValue(Object i) {
		// TODO Auto-generated method stub
		
	}

	public Object getPreviousValue() {
		// TODO Auto-generated method stub
		return null;
	}	
}
