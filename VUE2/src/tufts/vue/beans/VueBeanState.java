/*******
**  VuePropertyDescriptor
**
**
*********/

package tufts.vue.beans;


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
public class VueBeanState implements VueBeanInfo   {

	//////////////
	// Fields
	/////////////
	
	/** the info for this sitate **/
	private VueBeanInfo mInfo = null;
	
	/** proeprty values map **/
	private Map mProperties = new HashMap();
	
	/** change listeners **/
	private PropertyChangeSupport mSupport = null;
		
	
	
	
	//////////////////
	// Constructor
	///////////////////
	
	public VueBeanState() {
		super();
	}
	
	
	/////////////////////
	// Methods
	////////////////////
	
	/**
	 * initializeFromObject
	 * Inits the state from the passed object.
	 * "The state will reflect the VueBeanInfo for the
	 * object akak properties and values.
	 *
	 * @param pObject the source Object.
	 */
	public void initializeFrom(Object pObject)  {
	
		mInfo = VueBeans.getBeanInfo( pObject);
		mProperties.clear();

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
		String [] names = null;
		if( mInfo != null) {
			names = mInfo.getPropertyNames();
			}
		return names;
	}	

	
	/**
	 * getPropertyDescriptor
	 * Returns the property descriptor forthe cur the name; null in none.
	 **/
	public VuePropertyDescriptor[] getPropertyDescriptors()  {
		VuePropertyDescriptor [] descs = null;
		return descs;
	}
	
	
	
	/**
	 * hasProperty()
	 * Returns true if has property name; false if not
	 **/
	public boolean hasProperty( String pName) {
		boolean hasIt = false;
		
		return hasIt;
	} 


	
	/**
	 * 
	 **/
	public VuePropertyDescriptor getPropertyDescriptor( String pName) {
		VuePropertyDescriptor desc = null;
		
		return desc;
	}
	
	
	public Object getPropertyValue( String pName) {
		return mProperties.get( pName);
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
	public void applyState( Object pBean) {
		
		String[] propertyNames = getPropertyNames();
		System.out.println( "*** applyState: "+ pBean.getClass().getName() );
		System.out.println( "  names:  "+ propertyNames );
		if( propertyNames != null) {
			for (int i = 0; i < propertyNames.length; i++) {
				String name = propertyNames[i];
				Object value = getPropertyValue(name);
				System.out.println("    "+name+"  value: "+value);
				VueBeans.setPropertyValue( pBean, name, value);
				}
			}
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


}