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

import javax.swing.JComponent;

import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.generics.GenericBooleanPreference;

/**
 * @author Mike Korcynski
 *
 */
public class WindowPropertysPreference extends GenericBooleanPreference{

	public String getPreferenceCategory() {
		return PreferenceConstants.WINDOW_CATEGORY;
	}
	
	public String getTitle()
	{
		return new String("Window props");
	}
	
	public String getDescription()
	{
		return "Enables remembering locationa and size of Dock Windows";
	}
	public String getMessage()
	{
		return new String("Remember Size and Location of Dock Windows?");
	}
	
	public String getPrefName()
	{
		return new String("windows.sizeAndPosition");
	}

	public String getCategoryKey() {
		return "windows";
	}

	public void preferenceChanged() {
		// TODO Auto-generated method stub
		
	}
	
	
}
