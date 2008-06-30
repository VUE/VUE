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
import java.util.prefs.Preferences;

import javax.swing.JComponent;

import edu.tufts.vue.preferences.generics.BasePref;
import edu.tufts.vue.preferences.interfaces.VuePreference;

public class ColorPreference extends BasePref
{

	private String category;
	private String name;
	private String key;
	private String description;
	private Object defaultValue;
	private Object previousValue;
	private Preferences p = Preferences.userNodeForPackage(getPrefRoot());
	
	public static ColorPreference create(String category, String key, String name, String desc, Color defaultValue, boolean showInUI)
	{
		return new ColorPreference(category,key,name,desc,defaultValue,showInUI);
	}
	
	//show in UI defaults to true
	public static ColorPreference create(String category, String key, String name, String desc, Color defaultValue)
	{
		return new ColorPreference(category,key,name,desc,defaultValue,true);
	}
	
	protected ColorPreference(String category, String key, String name, String desc, Color defaultValue, boolean showInUI)
	{
		this.category=category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = colorToString(defaultValue);
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

	static Color makeColor(String hex) {
        if (hex.startsWith("#"))
            hex = hex.substring(1);
        boolean hasAlpha = hex.length() > 6;
        int bits = Long.valueOf(hex, 16).intValue();
        Color c = new Color(bits, hasAlpha);
        //System.out.println("From " + hex + " made " + c + " alpha=" + c.getAlpha());
        return c;
    }

	private static Color convertStringToColor(String s)
	{
		Color value = null;
	       
        if (s != null) 
        {
             s.trim();
             value = makeColor(s);
        }            
        return value;      
	}
	
	private static String colorToString(Color c) {
		if (c == null)
			return null;
		
		char[] buf = new char[7];
		buf[0] = '#';
		String s = Integer.toHexString(c.getRed());
		if (s.length() == 1) {
			buf[1] = '0';
			buf[2] = s.charAt(0);
		}
		else {
			buf[1] = s.charAt(0);
			buf[2] = s.charAt(1);
		}
		s = Integer.toHexString(c.getGreen());
		if (s.length() == 1) {
			buf[3] = '0';
			buf[4] = s.charAt(0);
		}
		else {
			buf[3] = s.charAt(0);
			buf[4] = s.charAt(1);
		}
		s = Integer.toHexString(c.getBlue());
		if (s.length() == 1) {
			buf[5] = '0';
			buf[6] = s.charAt(0);
		}
		else {
			buf[5] = s.charAt(0);
			buf[6] = s.charAt(1);
		}
		return String.valueOf(buf);
	}
	
	public Object getValue(){
		String s = (String)(p.get(getPrefName(), (String)getDefaultValue()));
		return convertStringToColor(s);			
	}

	public void setValue(Object s)
	{
		previousValue = getValue(); 		
		p.put(getPrefName(), colorToString((Color)s));
		_fireVuePrefEvent();
	}
}
