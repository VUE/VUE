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


    static public void applyPropertyValueToSelection(LWSelection s, String pName, Object pValue)
    {
        if (s == null || s.isEmpty())
            return;
        Iterator i = s.iterator();
        // FIX:  THis may be a bad assumption that the mapper will work for 
        // all items in the selection. [it should apply properties as it can]
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink && pName == LWKey.FillColor)
                continue;
            if (tufts.vue.DEBUG.SELECTION) System.out.println("applying " + pName + " to " + c);
            c.setProperty(pName, pValue);
            //VueBeans.setPropertyValue(c, pName, pValue);
        }
    }
}
