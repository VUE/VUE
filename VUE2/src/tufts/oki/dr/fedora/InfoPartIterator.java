/*
 * InfoPartIterator.java
 *
 * Created on October 13, 2003, 11:21 AM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class InfoPartIterator implements osid.dr.InfoPartIterator {
    
    java.util.Vector vector = new java.util.Vector();
    java.util.Map configuration = null;
    int i = 0;

    public InfoPartIterator(java.util.Vector vector)  throws osid.dr.DigitalRepositoryException {
        this.vector  = vector;
    }
    
    
    public InfoPartIterator(java.util.Vector vector, java.util.Map configuration)
        throws osid.dr.DigitalRepositoryException {
        this.vector = vector;
        this.configuration = configuration;
    }

    /**
     * Return true if there are additional  InfoParts ; false otherwise.
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public boolean hasNext() throws osid.dr.DigitalRepositoryException {
        return (i < vector.size());
    }

    /**
     * Return the next InfoParts.
     *
     * @return osid.dr.InfoPart
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED, NO_MORE_ITERATOR_ELEMENTS
     */
    public osid.dr.InfoPart next() throws osid.dr.DigitalRepositoryException {
        if (i >= vector.size()) {
            throw new osid.dr.DigitalRepositoryException("No more Behaviors");
        }

        return (osid.dr.InfoPart) vector.elementAt(i++);
    }

}
