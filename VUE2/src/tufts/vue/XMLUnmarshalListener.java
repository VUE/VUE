package tufts.vue;

/**
 * Any object that implements this interface and VUE restores
 * via Castor will get called as the below events happen.
 */
public interface XMLUnmarshalListener {

    /** object's has been constructed with call to public no-arg constructor */
    public void XML_initialized();

    /** all attributes and elements have been processed: the values for this object are set */
    public void XML_completed();

    /** a child field value has been de-serialized and constructed to be set on/provided to it's parent  */
    public void XML_fieldAdded(String name, Object child);
    
    /** the object has been added to it's parent (the parent's setter was just called with the child) */
    public void XML_addNotify(String name, Object parent);
}

