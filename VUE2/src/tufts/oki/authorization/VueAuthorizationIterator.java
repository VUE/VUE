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
