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
 * PropertiesIterator.java
 *
 * Created on October 22, 2003, 7:45 AM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *  Imlements a way to iterate over lists of Properties.
 *
 *  @author  Mark Norton
 */
public class PropertiesIterator implements osid.shared.PropertiesIterator {
    
    private Vector properties_vector = null;
    int offset = 0;
    
    /** 
     *  Creates a new instance of PropertiesIterator 
     *
     *  @author Mark Norton
     */
    public PropertiesIterator(Vector vector) {
        properties_vector = vector;
    }
    
    /**
     *  See if there is another Properties object in the list.
     *
     *  @author Mark Norton
     *
     *  @return true if there is at least one more Properties object in the list.
     */
    public boolean hasNext() {
        return (offset < properties_vector.size());
    }
    
    /**
     *  Get the next Properties object in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return the next Properties object in the iteration list.
     */
    public osid.shared.Properties next() {
        osid.shared.Properties prop = (osid.shared.Properties) properties_vector.elementAt(offset);
        offset++;
        return prop;
   }
    
}
