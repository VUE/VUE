package tufts.vue;

/**
 * For gui components who store a property key (see LWKey)
 * and can set & get a value based on the key.
 */
public interface LWPropertyHandler {
    public void setPropertyValue(Object propertyValue);
    public Object getPropertyValue();
    public Object getPropertyKey();
}
