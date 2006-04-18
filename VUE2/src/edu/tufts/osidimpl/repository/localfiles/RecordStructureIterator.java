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

package edu.tufts.osidimpl.repository.localfiles;

public class RecordStructureIterator
implements org.osid.repository.RecordStructureIterator
{
    private java.util.Iterator iterator = null;

    public RecordStructureIterator(java.util.Vector vector)
    throws org.osid.repository.RepositoryException
    {
        this.iterator = vector.iterator();
    }

    public boolean hasNextRecordStructure()
    throws org.osid.repository.RepositoryException
    {
        return (this.iterator.hasNext());
    }

    public org.osid.repository.RecordStructure nextRecordStructure()
    throws org.osid.repository.RepositoryException
    {
		try {
			return (org.osid.repository.RecordStructure)iterator.next();
		} catch (Throwable t) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
		}
    }
}
