
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
