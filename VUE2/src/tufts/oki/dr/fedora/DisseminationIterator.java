/*
 * DisseminationIterator.java
 *
 * Created on June 18, 2003, 4:55 PM
 */

package tufts.oki.dr.fedora;

import osid.dr.*;
/**
 *
 * @author  akumar03
 */
public class DisseminationIterator implements InfoFieldIterator {
    
    java.util.Vector vector = new java.util.Vector();
    java.util.Map configuration = null;
    int i = 0;
    
    
    /** Creates a new instance of DisseminationIterator */
    public DisseminationIterator(java.util.Vector vector) {
       this.vector = vector;
    }
    
    /**     Return true if there are additional  InfoFields ; false otherwise.
     *     @return boolean
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public boolean hasNext() throws osid.dr.DigitalRepositoryException {
          return (i < vector.size());
    }
    
    /**     Return the next InfoFields.
     *     @return InfoField
     *     @throws DigitalRepositoryException if there is a general failure or if all objects have already been returned.
     */
    public InfoField next() throws osid.dr.DigitalRepositoryException {
        if (i >= vector.size()) 
            throw new osid.dr.DigitalRepositoryException("No more Behaviors");
        return (osid.dr.InfoField) vector.elementAt(i++);
    }
    
}
