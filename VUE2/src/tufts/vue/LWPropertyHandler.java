package tufts.vue;

/**
 * Interface for anythying that is both a property producer & consumer.
 *
 * Intended for gui components that in total represent a <b>SINGLE</b> property, 
 * indicated by getPropertyKey(), and can provide or set the current
 * property value.  Although property keys are currently always
 * strings, this API is defined using objects in case we decide
 * to create a property key object in the future.  If we do
 * so, key.toString() should always produce the appropriate
 * unique property name.
 *
 * E.g.: a color menu, that could be instantiated several times
 * with different property names for adjusting different LWComponent
 * color values, or a collection of gui components the together
 * represent a single font value (family,size,bold/italic).
 */

// rename LWPropertyHolder or LWPropertyProducer: key is that this is for a SINGLE property holder
public interface LWPropertyHandler {

    /** @return the property key (LWKey) for the property we hold */
    public Object getPropertyKey();
    
    /** @return the current property value */
    public Object getPropertyValue();
    
    /** set the current property value (e.g.: gui component changes it's representation)
     * @param propertyValue must be of type expected for the given key */
    public void setPropertyValue(Object propertyValue);
}
