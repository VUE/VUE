/*
 * VueFunctionIterator.java
 *
 * Created on December 12, 2003, 9:36 AM
 */

package tufts.oki.authorization;
import java.util.*;

/**
 *  Implements a means to iterate over a list of Function objects.
 *
 * @author  Mark Norton
 */
public class VueFunctionIterator implements osid.authorization.FunctionIterator {
    private Vector function_vector = null;    
    private int offset = 0;
    
    /** Creates a new instance of VueFunctionIterator */
    public VueFunctionIterator(Vector vector) {
        function_vector = vector;
    }
    
    /**
     *  Determine if there is at least one more Function in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return True if there a next function in the iterator.
     */
    public boolean hasNext() {
        return (offset < function_vector.size());
    }
    
    /**
     *  Get the next Function in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return The next funtion in the iteration list.
     */
    public osid.authorization.Function next() {
        osid.authorization.Function ftn = (osid.authorization.Function) function_vector.elementAt(offset);
        offset++;
        return ftn;
    }
    
}
