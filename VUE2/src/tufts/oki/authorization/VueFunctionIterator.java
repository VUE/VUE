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
