/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.tufts.osidimpl.repository.sakai;

public class ObjectIterator implements org.osid.shared.ObjectIterator {
    java.util.Iterator mIterator = null;

    protected ObjectIterator(java.util.Vector vector)
        throws org.osid.shared.SharedException {
        mIterator = vector.iterator();
    }

    public boolean hasNextObject() throws org.osid.shared.SharedException {
        return mIterator.hasNext();
    }

    public java.io.Serializable nextObject()
        throws org.osid.shared.SharedException {
        try {
            return (java.io.Serializable) mIterator.next();
        } catch (java.util.NoSuchElementException e) {
            throw new org.osid.shared.SharedException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
    }
}
