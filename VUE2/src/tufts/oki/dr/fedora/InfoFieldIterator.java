/*
 * InfoFieldIterator.java
 *
 * Created on March 5, 2004, 6:32 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class InfoFieldIterator implements osid.dr.InfoFieldIterator{
    
    /** Creates a new instance of InfoFieldIterator */
    java.util.Vector vector = new java.util.Vector();
    java.util.Map configuration = null;
    int i = 0;
    public InfoFieldIterator(java.util.Vector vector)  throws osid.dr.DigitalRepositoryException {
        this.vector  = vector;
    }
    
    
    public InfoFieldIterator(java.util.Vector vector, java.util.Map configuration)
        throws osid.dr.DigitalRepositoryException {
        this.vector = vector;
        this.configuration = configuration;
    }
    
    public boolean hasNext() throws osid.dr.DigitalRepositoryException {
            return (i < vector.size());
    }
    
    public osid.dr.InfoField next() throws osid.dr.DigitalRepositoryException {
          if (i >= vector.size()) {
            throw new osid.dr.DigitalRepositoryException("No more InforFields");
        }
        return (osid.dr.InfoField) vector.elementAt(i++);
    }
    
}
