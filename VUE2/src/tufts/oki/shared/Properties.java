/*
 * Properties.java
 *
 * Created on Oct. 21, 2003, 19:21 PM
 */

package src.tufts.shared;
import java.util.*;
import osid.shared.SharedException;

/**
 *  Properties implement a means to collect key / value pairs together into a typed
 *  collection (called a Properties).
 *  <p>
 *  This implementation conforms exactly to the osid.shared.Properties definition in
 *  v1.0 rc6.1.  There are some real limitations to using this as a general purpose
 *  mechanism to handle properties.  In particular, there no way to iterate over the
 *  individual property pairs saved in a Properties collection.  Also, the original
 *  definition doesn't include a way to add properties.  This is corrected by the addition
 *  of addProperty() below.
 *
 *  @author  Mark Norton
 */
public class Properties implements osid.shared.Properties {
    
    private HashMap map = null;
    private osid.shared.Type prop_type = null;
    
    /** 
     *  Creates a new instance of Properties 
     *
     *  @author Mark Norton
     */
    public Properties(osid.shared.Type type) throws osid.shared.SharedException {
        if (type == null)
            throw new osid.shared.SharedException (osid.shared.SharedException.NULL_ARGUMENT);
        map = new HashMap (100);
        prop_type = type;
    }
    
    /**
     *  Get a list of keys for properties in this collection.
     *
     *  @author Mark Norton
     *
     *  @return A SerializalbeObjectIterator which walks a list of property keys.
     */
    public osid.shared.SerializableObjectIterator getKeys() {
        Set set = map.keySet();
        Vector map_vector = new Vector(100);
        map_vector.addAll(set);
        SerializableObjectIterator it = new SerializableObjectIterator(map_vector);
        return it;
    }
    
    /**
     *  Get the value associated with a key in this properties collection.
     *
     *  @author Mark Norton
     *
     *  @return The value associated with this key.
     */
    public java.io.Serializable getProperty(java.io.Serializable key) {
        return (java.io.Serializable)map.get((Object)key);
    }
    
    /**
     *  Get the type of this properties collection.
     *
     *  @author Mark Norton
     *
     *  @return The type associated with this Properties object.
     */
    public osid.shared.Type getType() throws osid.shared.SharedException {
        return prop_type;
    }
    
    /**
     *  Add a key / value pair to this properties collection.
     *  <p>
     *  Note that this is an extension to osid.shared.Properties.
     *
     *  @author Mark Norton
     */
    public void addProperty (java.io.Serializable key, java.io.Serializable value) {
        map.put (key, value);
    }
    
    /**
     *  Get the internal hash map used to store key /value pairs.
     *  <p>
     *  This is an extension to osid.shared.Properties to support properties in 
     *  osid.filing.
     *
     *  @author Mark Norton
     *
     *  @return Get the internal hash map and return it.
     */
    public HashMap getPropertySet () {
        return map;
    }
}
