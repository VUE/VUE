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
 * SerializableObjectIterator.java
 *
 * Created on Oct. 22, 2003,  7:31 AM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *  Provides a way to interate over lists of serializable objects.
 *
 * @author  Mark Norton
 */
public class SerializableObjectIterator implements osid.shared.SerializableObjectIterator {
    
    private Vector object_vector = null;
    private int offset = 0;
    
    /** 
     *  Creates a new instance of SerializableObjectIterator 
     *
     *  @author Mark Norton
     */
    public SerializableObjectIterator(Vector vect) {
        object_vector = vect;
    }
    
    /**
     *  See if there is another object left in the interation.
     *
     *  @author Mark Norton
     *
     *  @return true if there is at least one more object left in the list.
     */
    public boolean hasNext() {
        return (offset < object_vector.size());
    }
    
    /**
     *  Get the next object in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return the next object in the list.
     */
    public java.io.Serializable next() {
        java.io.Serializable obj = (java.io.Serializable) object_vector.elementAt(offset);
        offset++;
        return obj;
    }
    
}
