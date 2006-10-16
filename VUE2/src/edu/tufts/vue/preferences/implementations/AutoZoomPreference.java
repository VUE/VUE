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

package edu.tufts.vue.preferences.implementations;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import tufts.vue.MapViewer;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.generics.GenericBooleanPreference;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.*;

public class AutoZoomPreference extends GenericBooleanPreference {

	
	public AutoZoomPreference()
	{
		super();
	}	
	
	public String getPreferenceCategory() {
		return PreferenceConstants.MAPDISPLAY_CATEGORY;
	}
	
	public boolean getDefaultValue()
	{
		return true;
	}
	public boolean getValue()
	{
		return false;
		
	}
	public String getMessage()
	{
		return "enable auto-zoom";
	}
	public String getTitle()
	{
		return new String("Auto-Zoom");
	}
	
	public String getDescription()
	{
		return new String("Enables zooming when rolling over a node");
	}
	
	public String getPrefName()
	{
		return PreferenceConstants.autoZoomPref;
	}

	public String getCategoryKey() {
		return "mapDisplay";
	}
	
	public void preferenceChanged()
	{	
		MapViewer.setAutoZoomEnabled(getValue());			
	}
	
}
