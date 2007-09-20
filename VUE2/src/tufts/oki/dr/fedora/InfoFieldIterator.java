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
