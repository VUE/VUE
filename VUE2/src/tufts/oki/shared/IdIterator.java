/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * IdIterator.java
 *
 * Created on October 21, 2003, 4:43 PM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *  This provides a means to iterate over lists of osid.Id objects.  Like most OSID
 *  iterators, it uses a hasNext() test to check for more data, and a getNext() method
 *  to get the next Id in the list.
 *
 * @author  Mark Norton
 *
 */
public class IdIterator implements osid.shared.IdIterator {
    
    private Vector id_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of IdIterator */
    public IdIterator(Vector vector) {
        id_vector = vector;
    }
    
    /**
     *  Check to see if there an another Id left in the interation.
     *
     *  @author Mark Norton
     *
     *  @return True if there is another Id in the iterator list.
     */
    public boolean hasNext() {
        return (offset < id_vector.size());
    }
    
    /**
     *  Get the next Id in the iteration.
     *
     *  @author Mark Norton
     *
     *  @return The next Id in the iterator list.
     */
    public osid.shared.Id next() {
        osid.shared.Id id = (osid.shared.Id) id_vector.elementAt(offset);
        offset++;
        return id;
    }
    
}
