/*******
**  VuePropertyDescriptor
**
**
*********/

package tufts.vue.beans;


import java.io.*;
import java.util.*;
import java.awt.*;
import java.lang.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import tufts.vue.*;

/**
* VueBeans
* This class holds a list of properties and balues for a
* given object and can manage chagnes to properties.
*
**/
public class VueBeans   {

	//////////////
	// Fields
	/////////////
	
	/** a list of property mappers **/
	static private Map sPropertyMappers = new HashMap();
	
	/** proeprty map **/
	private Map mDiscriptors = new HashMap();
	
	static String kBackgroundColor = "background";
	static String kForeColor = "background";
	static String kLineColor = "background";
		
	
	
	
	//////////////////
	// Constructor
	///////////////////
	
	public VueBeans() {
		super();
	}
	
	
	/////////////////////
	// Methods
	////////////////////
	
	/**
	 * getBeanInfo
	 * Looks up the VueBeanInfo based on the class or tries to build one
	 * from scratch
	 * $param pObject = the object to get info about
	 * @return VueBeanInfo the info 
	 **/
	static public VueBeanInfo getBeanInfo( Object pObject) {
		VuePropertyMapper mapper = null;
		VueBeanInfo info = null;
		mapper = getPropertyMapper( pObject);
		if( mapper != null) {
			info = mapper.getBeanInfo( pObject);
			}
		return info;
	}
	
	
	/**
	 * getPropertyValue
	 * Returns a property value given the object and the property name
	 * @param pObject - the object
	 * @param String the name
	 * @return the vuale
	 **/
	 static public Object getPropertyValue( Object pObject, String pName) {
	 	
	 	Object value = null;
	 	VuePropertyManager mgr = VuePropertyManager.getManager();
	 	VuePropertyMapper mapper = mgr.getPropertyMapper( pObject);
	 	if( mapper != null) {
	 		value = mapper.getPropertyValue( pObject, pName);
	 		}
	 	return value;
	 }
	
	
	
	/**
	 * setPropertyValue
	 * Sets the value for the given the object and the property name
	 * @param pObject - the object
	 * @param String the name
	 * @param pValue the value
	 **/
	 static public void setPropertyValue( Object pObject, String pName, Object pValue) {
	 	
	 	VuePropertyManager mgr = VuePropertyManager.getManager();
	 	VuePropertyMapper mapper = mgr.getPropertyMapper( pObject);
	 	if( mapper != null) {
	 		mapper.setPropertyValue( pObject, pName, pValue);
	 		}
	 	else {
	 	System.out.println("  No mapper fournd for class: "+pObject.getClass().getName() );
	 	}
	 }
	
	
	
	/**
	 * registerPropertyMapper
	 * THis method registers a property mapper for a given class
	 * @param Class the class
	 * @param VueProeprtyMapper - the mapper for the class
	 **/
	static public void registerPropertyMapper( Class pClass, VuePropertyMapper pMapper) {
		sPropertyMappers.put( pClass.getName(), pMapper);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * getPropertyMapper
	 * This method returns a VuePropertyMapper for the given object.
	 * If not mapper can be found for the object, null is returned.
	 * @param Object - the object in question
	 * @return VuePropertyMapper teh mapper for the given object
	 **/
	static public VuePropertyMapper getPropertyMapper( Object pObject) {
		VuePropertyManager mgr = VuePropertyManager.getManager();
		VuePropertyMapper mapper = mgr.getPropertyMapper( pObject);
		return mapper;
	}
	
	/**
	 * getState
	 * This returns a VueBeanState built from the passed bean.
	 * @param Object the bean to generate a BeanState
	 * @return VueBeanState - a property value set of the bean
	 **/
	static public VueBeanState getState( Object pBean) {
		
		VueBeanState state = new VueBeanState();
		state.initializeFrom( pBean);
		
		
		return state;
	}


	static public void setPropertyValueForLWSelection(LWSelection pSelection, String pName, Object pValue  ) {
		
		if(  (pSelection == null) || pSelection.isEmpty()) {
			System.out.println("  Selection null or empty.");
			return;
			}
		Iterator it = pSelection.iterator();
		Object object = null;
		// FIX:  THis may be a bad assumption that the mapper will work for 
		// all items in the selection.
		while( it.hasNext() ) {
			object = it.next();
			VueBeans.setPropertyValue( object, pName, pValue);
			}
	}
}