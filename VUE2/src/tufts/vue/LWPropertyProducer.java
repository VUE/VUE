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
 * Intended for gui components that in total represent a <b>SINGLE</b> property, 
 * indicated by getPropertyKey(), and can provide or set the current
 * property value.  Although property keys are currently always
 * strings, this API is defined using objects in case we decide
 * to create a property key object in the future.  If we do
 * so, key.toString() should always produce the appropriate
 * unique property name.
 *
 * Usage: e.g., a color menu, that could be instantiated several times
 * with different property names for adjusting different LWComponent
 * color values, or a collection of gui components that together
 * represent a single font value (family,size,bold/italic).
 */

public interface LWPropertyProducer {
    // rename LWPropertyHolder

    /** @return the property key (LWKey) for the property we hold */
    public Object getPropertyKey();
    
    /** @return the current property value */
    // rename produceValue?
    public Object getPropertyValue();
    
    /** set the current property value (e.g.: gui component changes it's representation)
     * @param propertyValue must be of type expected for the given key */
    // rename takeValue?
    public void setPropertyValue(Object propertyValue);
}
