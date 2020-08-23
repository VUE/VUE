/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
 * TypeIterator.java
 *
 * Created on October 22, 2003, 7:15 AM
 */

package tufts.oki.shared2;
import java.util.*;

/**
 *  Implements a means to iterate over lists of OsidType objects.
 *
 *  @author  Mark Norton
 */
public class TypeIterator implements org.osid.shared.TypeIterator {
    
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
    public boolean hasNextType() {
        return (offset < type_vector.size());
    }
    
    /**
     *  Get the next Type in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return  the next Type in the interation list.
     */
    public org.osid.shared.Type nextType() {
        org.osid.shared.Type type = (org.osid.shared.Type) type_vector.elementAt(offset);
        offset++;
        return type;
    }
    
}
