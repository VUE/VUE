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

public class ParameterPart
implements org.osid.repository.Part
{
    private java.util.Vector partVector = new java.util.Vector();
    private org.osid.repository.RecordStructure recordStructure = null;
    private org.osid.shared.Id id = null;
    private java.io.Serializable value = null;
    private org.osid.repository.PartStructure partStructure = null;

    protected ParameterPart(org.osid.shared.Id id
                          , java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
    }

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.Part createPart(org.osid.shared.Id partStructureId
                                         , java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        if ( (partStructureId == null) || (value == null) )
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }

        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void deletePart(org.osid.shared.Id partId)
    throws org.osid.repository.RepositoryException
    {
        if (partId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }

        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.PartIterator getParts()
    throws org.osid.repository.RepositoryException
    {
        return new PartIterator(this.partVector);
    }

    public java.io.Serializable getValue()
    throws org.osid.repository.RepositoryException
    {
        return this.value;
    }

    public void updateValue(java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        this.value = (String)value;
    }

    public org.osid.repository.PartStructure getPartStructure()
    throws org.osid.repository.RepositoryException
    {
        return this.partStructure;
    }
 
}
