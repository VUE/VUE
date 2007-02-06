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

package tufts.vue;

/**
   
 * Data interface to an editor for a single property.

 * Intended for gui components that in total represent a <b>SINGLE</b>
 * property referenced by key, indicated by getKey(), and can provide
 * or set the current property value.

 * Usage: e.g., a color menu, that could be instantiated several times
 * with different property names for adjusting different LWComponent
 * color values, or a collection of gui components that together
 * represent a single font value (family,size,bold/italic).
 
 */

public interface LWEditor<T> {
    /** @return the property key for the property we hold */
    public Object getPropertyKey();
    
    /** @return the current property value represented by the editor state */
    public T produceValue();
    
    /** change the editor state to represent the given property value 
     * @param propertyValue must be of type expected for the given key */
    public void displayValue(T propertyValue);
}
