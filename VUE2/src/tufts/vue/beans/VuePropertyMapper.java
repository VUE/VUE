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


import java.beans.*;

/** This interface describes a DynamicPropertyMapper: an object that
* can get and set dynamic properties of dynamic beans belonging to
* Java classes or interfaces for which it has registered.
*
*
**/

public interface VuePropertyMapper {

	/**
	 * getPropertyValue
	 * Gets the value of the property from the specified object.
	 * @param Object - the object
	 * @param String name
	 * @return Object the value
	 */
	public Object getPropertyValue(Object pBean, String pName);

	/**
	 * setPropertyValue
	 * This sets the property with of the object with the passed value
	 * @param OpObject - the object
	 * @param String pName the proeprty name
	 * @param pValue - the value of the named property
	 **/
	public void setPropertyValue(Object pObject, String pName, Object pValue) ;


	/**
	 * getBeanInfo
	 * Returns the VueBeanInfo for the object.
	 * @param Object - the object
	 * @return VueBeanInfo the info for the object.
	 **/
	public VueBeanInfo getBeanInfo(Object pObject);
}
