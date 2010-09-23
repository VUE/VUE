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
 * ByteValueIterator.java
 *
 * Created on September 20, 2003, 6:32 PM
 */

package tufts.oki.shared2;
import java.util.*;

/**
 *
 * @author  Mark Norton
 *
 *  @description
 *  ByteValueIterator is fully implemented.
 *  Unlike the various object iterators in osid.shared, value iterators like this one
 *  are based on an array instead of a vector.
 */
public class ByteValueIterator implements org.osid.shared.ByteValueIterator {
    
    private byte[] byte_array = null;
    
    private int offset = 0;
    
    /**
     *  @author Mark Norton
     *
     *  @return A ByteValueIterator which lists each byte in the array passed.
     */
    public ByteValueIterator(byte[] bytes) {
        byte_array = bytes;
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return True if there is another byte to be returned from the array.
     */
    public boolean hasNextByteValue() throws org.osid.shared.SharedException {
        return (offset < byte_array.length);
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return The next byte in the byte array.
     */
    public byte nextByteValue() throws org.osid.shared.SharedException {
        byte next_byte = byte_array[offset];
        offset++;
        return next_byte;
    }
    
}
