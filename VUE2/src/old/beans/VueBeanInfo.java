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
