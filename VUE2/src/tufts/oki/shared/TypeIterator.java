/*
 * TypeIterator.java
 *
 * Created on October 22, 2003, 7:15 AM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *  Implements a means to iterate over lists of OsidType objects.
 *
 *  @author  Mark Norton
 */
public class TypeIterator implements osid.shared.TypeIterator {
    
    private Vector type_vector = null;
    
    private int offset = 0;
    
    /** 
     *  Creates a new instance of TypeIterator
     *
     *  @author Mark Norton
     */
    public TypeIterator(Vector vector) {
        type_vector = vector;
    }
    
    /**
     *  Determine if there is at least one more Type in this iteration.
     *
     *  @author Mark Norton
     *
     *  @return true if there another Type left in this iteration.
     */
    public boolean hasNext() {
        return (offset < type_vector.size());
    }
    
    /**
     *  Get the next Type in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return  the next Type in the interation list.
     */
    public osid.shared.Type next() {
        osid.shared.Type type = (osid.shared.Type) type_vector.elementAt(offset);
        offset++;
        return type;
    }
    
}
