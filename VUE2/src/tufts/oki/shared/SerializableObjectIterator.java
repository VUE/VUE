/*
 * SerializableObjectIterator.java
 *
 * Created on Oct. 22, 2003,  7:31 AM
 */

package src.tufts.shared;
import java.util.*;

/**
 *  Provides a way to interate over lists of serializable objects.
 *
 * @author  Mark Norton
 */
public class SerializableObjectIterator implements osid.shared.SerializableObjectIterator {
    
    private Vector object_vector = null;
    private int offset = 0;
    
    /** 
     *  Creates a new instance of SerializableObjectIterator 
     *
     *  @author Mark Norton
     */
    public SerializableObjectIterator(Vector vect) {
        object_vector = vect;
    }
    
    /**
     *  See if there is another object left in the interation.
     *
     *  @author Mark Norton
     *
     *  @return true if there is at least one more object left in the list.
     */
    public boolean hasNext() {
        return (offset < object_vector.size());
    }
    
    /**
     *  Get the next object in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return the next object in the list.
     */
    public java.io.Serializable next() {
        java.io.Serializable obj = (java.io.Serializable) object_vector.elementAt(offset);
        offset++;
        return obj;
    }
    
}
