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
package  edu.tufts.osidimpl.repository.fedora_2_0;

public class LongValueIterator
        implements org.osid.shared.LongValueIterator {
    private java.util.Vector vector = new java.util.Vector();
    private int i = 0;
    
    public LongValueIterator(java.util.Vector vector)
    throws org.osid.shared.SharedException {
        this.vector = vector;
    }
    
    public boolean hasNextLongValue()
    throws org.osid.shared.SharedException {
        return i < vector.size();
    }
    
    public long nextLongValue()
    throws org.osid.shared.SharedException {
        if (i < vector.size()) {
            return ((Long)vector.elementAt(i++)).longValue();
        } else {
            throw new org.osid.shared.SharedException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
    }
    
}
