
package tufts.vue.beans;



/**
 * VueBeanInfo
 *
 * This interface is used to describe a set of properties that
 * occur together and have consistent behavior.
 *
 **/

public interface VueBeanInfo  {



	public VuePropertyDescriptor[] getPropertyDescriptors();

	public boolean hasProperty( String pName);  

	public String [] getPropertyNames();

	public VuePropertyDescriptor getPropertyDescriptor( String pName);

}
