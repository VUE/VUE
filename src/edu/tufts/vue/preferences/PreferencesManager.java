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

package edu.tufts.vue.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.prefs.*;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.implementations.SeasrRepositoryPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

/**
 * @author Mike Korcynski
 *
 */
public class PreferencesManager {

	  private static PreferencesManager ref;
	  private static List preferences = new ArrayList();
	  private static Vector categories = new Vector();
	
	  static
	  {
              //PreferencesManager.registerPreference(edu.tufts.vue.preferences.implementations.ImageSizePreference.class);
		  //PreferencesManager.registerPreference(edu.tufts.vue.preferences.implementations.AutoZoomPreference.class);  		
	        SeasrRepositoryPreference srr = edu.tufts.vue.preferences.implementations.SeasrRepositoryPreference.getInstance();

		  if (tufts.Util.isWindowsPlatform())
			  PreferencesManager.registerPreference(edu.tufts.vue.preferences.implementations.EnhancedFileChooserPreference.class);
		  //PreferencesManager.registerPreference(edu.tufts.vue.preferences.implementations.WindowPropertiesPreference.class);
		
		categories.add(PreferenceConstants.MAPDISPLAY_CATEGORY);
		categories.add(PreferenceConstants.INTERACTIONS_CATEGORY);
		categories.add(PreferenceConstants.PRESENTATION_CATEGORY);
		categories.add(PreferenceConstants.METADATA_CATEGORY);
		categories.add(PreferenceConstants.WINDOW_CATEGORY);
		categories.add(PreferenceConstants.LANGUAGE_CATEGORY);
		categories.add(PreferenceConstants.DATA_CATEGORY);
		categories.add(PreferenceConstants.EXPERIMENTAL_CATEGORY);
	  }
	  
	  public static void registerPreference(Object o)
	  {
		  preferences.add(o);
	  }

	  public static void registerPreference(Class pref)
	  {
		  preferences.add(pref);
		  return;
	  }
	  
	  public static Vector getCategories()
	  {
		  return categories;
	  }
	  
	  public static List getPreferences()
	  {
		  return preferences;
	  }
	  
	  public static PreferencesManager getSingletonObject()
	  {
	    if (ref == null)
	        ref = new PreferencesManager();		
	    return ref;
	  }

	  public static int getIntegerPrefValue(Object s)
	  {
		  VuePreference p = (VuePreference)s;

		  return java.util.prefs.Preferences.userNodeForPackage(PreferencesManager.class).getInt(p.getPrefName(), ((Integer)p.getDefaultValue()).intValue());		  
	  }
	  
	  public static String mapCategoryKeyToName(String key)
	  {
		  return tufts.vue.VueResources.getString("preferences.category." + key);
	  }

	  public static boolean getBooleanPrefValue(Object s)
	  {		
		  VuePreference p = (VuePreference)s;
		  
		  return java.util.prefs.Preferences.userNodeForPackage(PreferencesManager.class).getBoolean(p.getPrefName(), ((Boolean)p.getDefaultValue()).booleanValue());		  
	  }

	  public Object clone()
		throws CloneNotSupportedException
	  {
	    throw new CloneNotSupportedException(); 
	  }

	  public static void addPreferenceListener(PreferenceChangeListener pcl)
	  {
		  java.util.prefs.Preferences.userNodeForPackage(PreferencesManager.class).addPreferenceChangeListener(pcl);
	  }
}
