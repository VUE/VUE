package edu.tufts.vue.preferences.implementations;

import edu.tufts.vue.preferences.interfaces.VuePreference;

public class BooleanPreference extends edu.tufts.vue.preferences.generics.GenericBooleanPreference
{

	private String category;
	private String name;
	private String key;
	private String description;
	private Object defaultValue;

	public static BooleanPreference create(String category, String key, String name, String desc, Object defaultValue, boolean showInUI)
	{
		return new BooleanPreference(category,key,name,desc,defaultValue,showInUI);
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
	public static BooleanPreference create(String category, String key, String name, String desc, Object defaultValue)
	{
		return new BooleanPreference(category,key,name,desc,defaultValue,true);
	}
	
	private BooleanPreference(String category, String key, String name, String desc, Object defaultValue, boolean showInUI)
	{
		super(key,defaultValue);
		this.category=category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = defaultValue;
	
		edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
	}
		
	public Object getDefaultValue()
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
}
