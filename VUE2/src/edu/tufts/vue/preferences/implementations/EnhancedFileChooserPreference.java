package edu.tufts.vue.preferences.implementations;

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
public class EnhancedFileChooserPreference extends GenericBooleanPreference {

	private static EnhancedFileChooserPreference _instance; 
	
	private EnhancedFileChooserPreference()
	{
		super();
	}	
	
	 // For lazy initialization
	 public static synchronized EnhancedFileChooserPreference getInstance() {
	  if (_instance==null) {
	   _instance = new EnhancedFileChooserPreference();
	  }
	  return _instance;
	 }	
	 	
	public Object getDefaultValue()
	{
		return Boolean.TRUE;
	}
	
	public String getMessage()
	{
		return "enable enhanced filechooser";
	}
	public String getTitle()
	{
		return new String("Enhanced filechooser");
	}
	
	public String getDescription()
	{
		return new String("Toggles between native windows file chooser and a simple file chooser.  If you experience abnormally long start up times using the native file chooser you may want to disable this option. (Known Java Bug)");
	}
	
	public String getPrefName()
	{
		return "windows.FileChooser";
	}

	public String getCategoryKey() {
		return PreferenceConstants.WINDOW_CATEGORY;
	}			
}
