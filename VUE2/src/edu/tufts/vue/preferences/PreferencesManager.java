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

package edu.tufts.vue.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.prefs.*;

public class PreferencesManager {

	  private static PreferencesManager ref;
	  private static List preferences = new ArrayList();
	  private static Vector categories = new Vector();
	  static
	  {
		PreferencesManager.registerPreference(edu.tufts.vue.preferences.implementations.AutoZoomPreference.class);  
		//PreferencesManager.registerPreference(edu.tufts.vue.preferences.implementations.WindowPropertysPreference.class);
		
		categories.add(PreferenceConstants.MAPDISPLAY_CATEGORY);
		//categories.add(PreferenceConstants.WINDOW_CATEGORY);
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

	  public static boolean getBooleanPrefValue(String s)
	  {
		//  return java.util.prefs.Preferences.userNodeForPackage(PreferencesManager.class).getBoolean(s, PreferenceConstants.defaultAutoZoomPref);
		  return true;
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
