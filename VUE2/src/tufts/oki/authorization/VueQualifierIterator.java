/*
 * VueQualifierIterator.java
 *
 * Created on December 12, 2003, 4:26 PM
 */

package tufts.oki.authorization;
import java.util.*;

/**
 *
 * @author  Mark Norton
 */
public class VueQualifierIterator implements osid.authorization.QualifierIterator {
    
    private Vector qualifier_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of VueQualifierIterator */
    public VueQualifierIterator(Vector vector) {
        qualifier_vector = vector;
    }
    
    /**
     *  Determine if there is at least one more Qualifier in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return True if there a next qualifier in the iterator.
     */
    public boolean hasNext() {
        return (offset < qualifier_vector.size());
    }
    
    /**
     *  Get the next Qualifier in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return The next qualifier in the iteration list.
     */
    public osid.authorization.Qualifier next() {
        osid.authorization.Qualifier qual = (osid.authorization.Qualifier) qualifier_vector.elementAt(offset);
        offset++;
        return qual;
    }
        
}
