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
package edu.tufts.osidimpl.repository.sakai;

public class Properties
implements org.osid.shared.Properties
{

    public org.osid.shared.ObjectIterator getKeys()
    throws org.osid.shared.SharedException
    {
        return new ObjectIterator(new java.util.Vector());
    }

    public java.io.Serializable getProperty(java.io.Serializable key)
    throws org.osid.shared.SharedException
    {
        throw new org.osid.shared.SharedException(org.osid.shared.SharedException.UNKNOWN_KEY);
    }

    public org.osid.shared.Type getType()
    throws org.osid.shared.SharedException
    {
        return new Type("mit.edu","properties","general");
    }
}
