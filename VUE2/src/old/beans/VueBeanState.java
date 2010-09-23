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

package tufts.vue.beans;

import tufts.vue.DEBUG;

import java.io.*;
import java.util.*;
import java.beans.*;

import java.awt.*;
import java.lang.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;

/**
* VueBeanState
* This class holds a list of properties and balues for a
* given object and can manage chagnes to properties.
*
**/
public class VueBeanState implements VueBeanInfo
{
    /** the info for this state **/
    private VueBeanInfo mInfo = null;
	
    /** property values map **/
    private Map mProperties = new HashMap();
	
    /** change listeners **/
    private PropertyChangeSupport mSupport = null;
		
	
    public VueBeanState() {
        super();
    }
	
	
    /**
     * initializeFromObject
     * Inits the state from the passed object.
     * "The state will reflect the VueBeanInfo for the
     * object akak properties and values.
     *
     * @param pObject the source Object.
     */
    public void initializeFrom(Object pObject)  {
	
        mInfo = VueBeans.getBeanInfo(pObject);
        mProperties.clear();
        if (mInfo == null) {
            System.err.println(this + " couldn't initialize, no beaninfo from " + pObject);
            return;
        }

        String[] propertyNames = mInfo.getPropertyNames();
        for (int i = 0; propertyNames != null && i < propertyNames.length; i++)  {
            String name = propertyNames[i];
            Object value = VueBeans.getPropertyValue( pObject, name);
            if (value != null) {
                mProperties.put(name, value);
            }
        }
    }
	
    /**
     * getProeprtyNames
     * Returns a list of property names
     *
     **/
    public String [] getPropertyNames() {
        String[] names = null;
        if (mInfo != null)
            names = mInfo.getPropertyNames();
        return names;
    }	

	
    /**
     * getPropertyDescriptor
     * Returns the property descriptor forthe cur the name; null in none.
     **/
    public VuePropertyDescriptor[] getPropertyDescriptors()  {
        VuePropertyDescriptor [] descs = null;
        // nice: this was never even implemented
        throw new UnsupportedOperationException();
        //return descs;
    }
	
    /**
     * hasProperty()
     * Returns true if has property name; false if not
     **/
    public boolean hasProperty( String pName) {
        return mProperties.containsKey(pName);
    } 

    /** If a property with the given name is in the state, remove it.
     * @return the existing property value if there was one, null otherwise
     */
    public Object removeProperty(String pName) {
        return mProperties.remove(pName);
    }

    public VuePropertyDescriptor getPropertyDescriptor( String pName) {
        VuePropertyDescriptor desc = null;
        return desc;
    }
	
	
    public Object getPropertyValue( String pName) {
        return mProperties.get( pName);
    }
	
    public String getStringValue(String key) {
        return (String) mProperties.get( key);
    }
	
    public int getIntValue(String key) {
        return ((Integer) mProperties.get(key)).intValue();
    }
	
    public float getFloatValue(String key) {
        return ((Float) mProperties.get(key)).floatValue();
    }
	
	
    /**
     * setPropertyValue
     * Sets teh property value for named property
     **/
    public void setPropertyValue( String pName, Object pValue) {
        mProperties.put( pName, pValue);
    }
	
	
    /**
     * applyState
     * This method applies teh property VueBeanState to an object
     * @param Object pBean - the object to take the properties
     **/
    public void applyState(Object pBean)
    {
        if (DEBUG.TOOL) System.out.println("VueBeanState.applyState -> " + pBean);
        Iterator i = mProperties.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (DEBUG.TOOL) System.out.println("\t" + name + " <- " + value);
            VueBeans.setPropertyValue(pBean, name, value);
        }
        /*
        String[] propertyNames = getPropertyNames();
        if (propertyNames != null) {
            for (int i = 0; i < propertyNames.length; i++) {
                String name = propertyNames[i];
                Object value = getPropertyValue(name);
                if (DEBUG.TOOL) System.out.println("\t"+name+" <- "+value);
                VueBeans.setPropertyValue(pBean, name, value);
            }
        }
        */
    }


    /***
     * addPropertyChangeListener
     * Register a listener for the PropertyChange event.  The component state
     * should fire a PropertyChange event whenever it is changed.
     *
     * @param listener  An object to be invoked when a PropertyChange
     *		event is fired.
     **/
    public synchronized void addPropertyChangeListener (PropertyChangeListener listener) {
        if (mSupport == null) {
            mSupport = new PropertyChangeSupport(this);
        }
        mSupport.addPropertyChangeListener(listener);
    }

    /**
     * removePropertyChangeListener
     * Remove a listener for the PropertyChange event.
     *
     * @param listener  The PropertyChange listener to be removed.
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (mSupport != null) {
            mSupport.removePropertyChangeListener(listener);
        }
    }
  

    /**
     * Fire a PropertyChange event to all registered listeners.  Occurs
     * when setPropertyValue() is called.
     */
    protected synchronized void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (mSupport != null) {
            mSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public String toString()
    {
        return "VueBeanState[" + mInfo + " " + mProperties + "]";
    }


}
