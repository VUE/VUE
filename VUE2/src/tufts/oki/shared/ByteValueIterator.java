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
