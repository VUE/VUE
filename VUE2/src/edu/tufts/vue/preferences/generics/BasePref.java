package edu.tufts.vue.preferences.generics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.AutoZoomPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

public abstract class BasePref implements VuePreference {
	  private List _listeners = new ArrayList();	  	  	  	  	  
		 	
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
