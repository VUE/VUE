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
 * ObjectIterator.java
 *
 * Created on Oct. 22, 2003,  7:31 AM
 */

package tufts.oki.shared2;
import java.util.*;

/**
 *  Provides a way to interate over lists of serializable objects.
 *
 * @author  Mark Norton
 */
public class ObjectIterator implements org.osid.shared.ObjectIterator {
    
    private Vector object_vector = null;
    private int offset = 0;
    
    /** 
     *  Creates a new instance of ObjectIterator 
     *
     *  @author Mark Norton
     */
    public ObjectIterator(Vector vect) {
        object_vector = vect;
    }
    
    /**
     *  See if there is another object left in the interation.
     *
     *  @author Mark Norton
     *
     *  @return true if there is at least one more object left in the list.
     */
    public boolean hasNextObject() {
        return (offset < object_vector.size());
    }
    
    /**
     *  Get the next object in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return the next object in the list.
     */
    public java.io.Serializable nextObject() {
        java.io.Serializable obj = (java.io.Serializable) object_vector.elementAt(offset);
        offset++;
        return obj;
    }
    
}
