/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import tufts.vue.MapViewer;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.generics.GenericBooleanPreference;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.*;

/**
 * @author Mike Korcynski
 *
 */
public class AutoZoomPreference extends GenericBooleanPreference {

	private static AutoZoomPreference _instance; 
	
	private AutoZoomPreference()
	{
		super();
	}	
	
	 // For lazy initialization
	 public static synchronized AutoZoomPreference getInstance() {
	  if (_instance==null) {
	   _instance = new AutoZoomPreference();
	  }
	  return _instance;
	 }	
	 	
	public Object getDefaultValue()
	{
		return Boolean.FALSE;
	}
	
	public String getMessage()
	{
		return "enable auto-zoom";
	}
	public String getTitle()
	{
            return "Auto Zoom";
	}
	
	public String getDescription()
	{
		return new String("Enables zooming when rolling over a node");
	}
	
	public String getPrefName()
	{
		return "mapDisplay.AutoZoom";
	}

	public String getCategoryKey() {
		return PreferenceConstants.MAPDISPLAY_CATEGORY;
	}			
}
