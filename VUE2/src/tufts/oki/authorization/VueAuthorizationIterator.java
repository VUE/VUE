/*
 * VueAuthorizationIterator.java
 *
 * Created on December 12, 2003, 4:49 PM
 */

package tufts.oki.authorization;
import java.util.*;

/**
 *
 * @author  Mark Norton
 */
public class VueAuthorizationIterator implements osid.authorization.AuthorizationIterator {
    
    private Vector authorization_vector = null;
    private int offset = 0;
    
    /** Creates a new instance of VueAuthorizationIterator */
    public VueAuthorizationIterator(Vector vector) {
        authorization_vector = vector;
    }
    
    /**
     *  Determine if there is at least one more Authorization in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return True if there a next Authorization in the iterator.
     */
    public boolean hasNext() {
        return (offset < authorization_vector.size());
    }
    
    /**
     *  Get the next Authorization in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return The next Authorization in the iteration list.
     */
    public osid.authorization.Authorization next() {
        osid.authorization.Authorization auth = (osid.authorization.Authorization) authorization_vector.elementAt(offset);
        offset++;
        return auth;
    }
        
}
