package tufts.oki.dr.fedora;

/*
 * FedoraObjectIterator.java
 *
 * Created on May 7, 2003, 3:25 PM
 */

 
import osid.dr.*;
import java.util.Iterator;
/**
 *
 * @author  akumar03
 */
public class FedoraObjectIterator implements  AssetIterator {
    
    /** Creates a new instance of FedoraObjectIterator */
    java.util.Vector vector = new java.util.Vector();
    java.util.Map configuration = null;
    int i = 0;
    
    public FedoraObjectIterator() {
    }
    public FedoraObjectIterator(java.util.Vector vector) {
           this.vector = vector;
    }
    
    /**     Return true if there are additional  Assets ; false otherwise.
     *     @return boolean
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public boolean hasNext() throws osid.dr.DigitalRepositoryException {
           return (i < vector.size());
    }
    
    /**     Return the next Assets.
     *     @return Asset
     *     @throws DigitalRepositoryException if there is a general failure or if all objects have already been returned.
     */
    public Asset next() throws osid.dr.DigitalRepositoryException {
      if (i >= vector.size()) {
            throw new osid.dr.DigitalRepositoryException("No more Behaviors");
        }

        return (osid.dr.Asset) vector.elementAt(i++);
    }
    
}
