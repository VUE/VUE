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

package edu.tufts.vue.preferences.interfaces;

import javax.swing.JComponent;

import edu.tufts.vue.preferences.VuePrefListener;

/**
 * @author mkorcy01
 *
 */
public interface VuePreference<T> {
	
	String getCategoryKey();
	JComponent getPreferenceUI();
	String getTitle();
	String getDescription();
	String getPrefName();
	void addVuePrefListener(VuePrefListener p);
	T getDefaultValue();
	T getValue();
	T getPreviousValue();
	void setValue(T i);
}
