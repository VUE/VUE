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

public class PropertiesIterator
implements org.osid.shared.PropertiesIterator
{
    private java.util.Iterator iterator = null;

    public PropertiesIterator(java.util.Vector vector)
    throws org.osid.shared.SharedException
    {
        this.iterator = vector.iterator();
    }

    public boolean hasNextProperties()
    throws org.osid.shared.SharedException
    {
        return iterator.hasNext();
    }

    public org.osid.shared.Properties nextProperties()
    throws org.osid.shared.SharedException
    {
        if (iterator.hasNext())
        {
            return (org.osid.shared.Properties)iterator.next();
        }
        else
        {
            throw new org.osid.shared.SharedException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
    }
}
