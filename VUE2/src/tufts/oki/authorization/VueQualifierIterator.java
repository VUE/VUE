/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

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
