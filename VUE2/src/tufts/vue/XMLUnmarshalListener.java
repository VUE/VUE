package tufts.vue;

/**
 * Any object that implements this interface and is restored
 * by Castor will get called as the below events happen.
 */
public interface XMLUnmarshalListener {

    /** object's has been constructed with call to public no-arg constructor */
    public void XML_initialized();

    /** all attributes and elements have been processed: the values for this object are set */
    public void XML_completed();

    /** the object has been added to it's parent (the parent's setter was just called with the child) */
    public void XML_addNotify(String name, Object parent);
}

