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
 * ByteValueIterator.java
 *
 * Created on September 20, 2003, 6:32 PM
 */

package tufts.oki.shared;
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
public class ByteValueIterator implements osid.shared.ByteValueIterator {
    
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
    public boolean hasNext() throws osid.shared.SharedException {
        return (offset < byte_array.length);
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return The next byte in the byte array.
     */
    public byte next() throws osid.shared.SharedException {
        byte next_byte = byte_array[offset];
        offset++;
        return next_byte;
    }
    
}
