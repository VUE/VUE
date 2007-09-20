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
public class FedoraObjectIterator implements  osid.dr.AssetIterator {
    
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
