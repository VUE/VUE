/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.util.prefs.Preferences;

import javax.swing.JComponent;

import edu.tufts.vue.preferences.generics.BasePref;
import edu.tufts.vue.preferences.interfaces.VuePreference;

public class IntegerPreference extends BasePref
{

	private String category;
	private String name;
	private String key;
	private String description;
	private Integer defaultValue;
	private Integer previousValue;
	private Preferences p = Preferences.userNodeForPackage(getPrefRoot());
	
	public static IntegerPreference create(String category, String key, String name, String desc, Integer defaultValue, boolean showInUI)
	{
		return new IntegerPreference(category,key,name,desc,defaultValue,showInUI);
	}
	
	//show in UI defaults to true
	public static IntegerPreference create(String category, String key, String name, String desc, Integer defaultValue)
	{
		return new IntegerPreference(category,key,name,desc,defaultValue,true);
	}
	
	protected IntegerPreference(String category, String key, String name, String desc, Integer defaultValue, boolean showInUI)
	{
		this.category=category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = defaultValue;
		if (showInUI)
			edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
	}
		
	public Integer getDefaultValue()
	{
		return defaultValue;
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

	public String getPrefName() {
		return category + "." + key;
	}

	public JComponent getPreferenceUI() {
		// TODO Auto-generated method stub
		return null;
	}

	public Integer getPreviousValue()
	{
		if (previousValue == null)
			return getDefaultValue();
		else
			return previousValue;
	}

	public Integer getValue(){
		Integer i = (p.getInt(getPrefName(), getDefaultValue().intValue()));
		return i;
	
	}

	public void setValue(Object o)
	{
		setValue((Integer)o);
	}
	public void setValue(Integer i)
	{
		previousValue = getValue(); 		
		p.putInt(getPrefName(), i.intValue());
		_fireVuePrefEvent();
	}
}
