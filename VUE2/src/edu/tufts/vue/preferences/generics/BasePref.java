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
package edu.tufts.vue.preferences.generics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.AutoZoomPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

public abstract class BasePref implements VuePreference {
	  protected List _listeners = new ArrayList();	  	  	  	  	  
		 	
	  	public Class getPrefRoot()
		{
			return edu.tufts.vue.preferences.PreferencesManager.class;
		}
	  	
	  	public synchronized void addVuePrefListener( VuePrefListener l ) {
	        _listeners.add( l );
	    }
	    
	    public synchronized void removeMoodListener( VuePrefListener l ) {
	        _listeners.remove( l );
	    }
	     
	    protected synchronized void _fireVuePrefEvent() {
	        VuePrefEvent event = new VuePrefEvent(this,getPreviousValue(),getValue());
	        
	        Iterator listeners = _listeners.iterator();
	        while(listeners.hasNext()) {
	            ((VuePrefListener)listeners.next()).preferenceChanged(event);
	        }
	    }
}
