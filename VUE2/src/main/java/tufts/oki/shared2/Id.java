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
 * Id.java
 *
 * Created on October 21, 2003, 4:36 PM
 */

package tufts.oki.shared2;
import java.lang.*;


/**
 *  This is a complete implementation of the org.osid.shared.Id object.  It uses a constant
 *  called IdSource to hold a string which idenfies this particular source of Identifiers.
 *  This makes the Id strings less likely to collide with other systems which use this
 *  implementation.
 *  <p>
 *  The identifier is a string consisting of the IdSource constant concatenated with
 *  the current time (in milliseconds) epxressed as a hex string.
 *
 *  @author  Mark Norton
 */
public class Id implements org.osid.shared.Id {
    public  static int counter = 0;
    private String id_str;
    private String IdSource = "tufts.edu";

    
    /**
     *  Uses current time in milliseconds converted to a hex string as the basis for
     *  a unique identifier.  This is appended to an IdSource string.
     *
     *  @author Mark Norton
     */
    public Id() throws org.osid.shared.SharedException {
        long now = System.currentTimeMillis();

        id_str = IdSource + "-" + java.lang.Long.toHexString(now) + "-" + tufts.oki.shared.Id.counter++;
    }
    
    /**
     *  This was taken from the OKI Implementation examples as another way to 
     *  create an Id object.  There are certain objects in OKI, such as Cabinet and ByteStore
     *  which might use a file name as an idenfiier.  This constructor is provided
     *  for that kind of operation, but isn't garaunteed to be globally unique.
     *
     *  @author OkiImpl_1_0_rc05
     *
     */
    public Id(String idString) throws org.osid.shared.SharedException {
        if (idString == null) {
            throw new org.osid.shared.SharedException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }

        this.id_str = idString;
    }

    /**
     *  Get the ID string for this ID object.
     *
     *  @author Mark Norton
     *
     *  @return The string form of the id.
     */
    public String getIdString() throws org.osid.shared.SharedException {
        return id_str;
    }
    
    /**
     *  Check to see if this ID equals the one passed.
     *
     *  @author Mark Norton
     *
     *  @return True if this Id is equal to the one passed.
     */
    public boolean isEqual(org.osid.shared.Id id)  throws org.osid.shared.SharedException {
        return id_str.equals(id.getIdString());
    }

    public String toString() {
        return "Id[" + id_str + "]";
    }
    
}
