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
