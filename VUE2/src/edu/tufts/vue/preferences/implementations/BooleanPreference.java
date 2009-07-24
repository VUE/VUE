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

import edu.tufts.vue.preferences.interfaces.VuePreference;

public class BooleanPreference extends edu.tufts.vue.preferences.generics.GenericBooleanPreference
{

	private String category;
	private String name;
	private String key;
	private String description;
	private String message;
	private Boolean defaultValue;

	public static BooleanPreference create
            (String category,
             String key,
             String name,
             String desc,
             String message,
             Boolean defaultValue,
             boolean showInUI)
	{
            return new BooleanPreference(category,key,name,desc,message,defaultValue,showInUI);
	}
	
	public static BooleanPreference create
            (String category,
             String key,
             String name,
             String desc,
             Boolean defaultValue,
             boolean showInUI)
	{
            return new BooleanPreference(category,key,name,desc,null,defaultValue,showInUI);
	}
	/**
	 * Example Usage:
	 * private static VuePreference iconPref = BooleanPreference.create(
	 *		edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY,
	 *		"showNodeIcons", 
	 *		"Show Icons", 
	 *		"Display rollover icons in map nodes",
	 *		true);
	 */
	//show in UI defaults to true
	public static BooleanPreference create
            (String category,
             String key,
             String name,
             String desc,
             Boolean defaultValue)
	{
            return new BooleanPreference(category,key,name,desc,null,defaultValue,true);
	}
	
	private BooleanPreference(String category, String key, String name, String desc, String message, Boolean defaultValue, boolean showInUI)
	{
		super(key,defaultValue);
		this.category=category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = defaultValue;
		
		if (message != null)
			this.message = message;
		else
			this.message = name;

                super.cacheCurrentValue();
                
		if (showInUI)
                    edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);

                
	}
		
	public String getMessage(){
		return message;
	}
	
    @Override public Boolean getDefaultValue()
    {
        return defaultValue;
    }
		
	public String getDescription() { 
		return description;
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
}
