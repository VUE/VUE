package edu.tufts.vue.preferences.implementations;

import java.util.prefs.Preferences;

import javax.swing.JComponent;

import edu.tufts.vue.preferences.generics.BasePref;
import edu.tufts.vue.preferences.interfaces.VuePreference;

public class StringPreference extends BasePref
{

	private String category;
	private String name;
	private String key;
	private String description;
	private Object defaultValue;
	private Object previousValue;
	private Preferences p = Preferences.userNodeForPackage(getPrefRoot());
	
	public static StringPreference create(String category, String key, String name, String desc, Object defaultValue, boolean showInUI)
	{
		return new StringPreference(category,key,name,desc,defaultValue,showInUI);
	}
	
	//show in UI defaults to true
	public static StringPreference create(String category, String key, String name, String desc, Object defaultValue)
	{
		return new StringPreference(category,key,name,desc,defaultValue,true);
	}
	
	protected StringPreference(String category, String key, String name, String desc, Object defaultValue, boolean showInUI)
	{
		this.category=category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = defaultValue;
		if (showInUI)
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

	public JComponent getPreferenceUI() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getPreviousValue()
	{
		if (previousValue == null)
			return getDefaultValue();
		else
			return previousValue;
	}

	public Object getValue(){
		String s = (String)(p.get(getPrefName(), (String)getDefaultValue()));
		return s;
	
	}

	public void setValue(Object s)
	{
		previousValue = getValue(); 		
		p.put(getPrefName(), (String)s);
		_fireVuePrefEvent();
	}
}
