/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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

    /** Set the enabled state of this editor. Note that if the LWEditor implementor is a subclass of
     * java.awt.Component, this already matches the signature there, and thus this method
     * is already implemented for the interface in those cases. */
    public void setEnabled(boolean enabled);
    
}
