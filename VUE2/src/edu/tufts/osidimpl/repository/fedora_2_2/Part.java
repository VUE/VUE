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
package  edu.tufts.osidimpl.repository.fedora_2_2;

public class Part
implements org.osid.repository.Part
{
    private java.util.Vector partVector = new java.util.Vector();
    private org.osid.repository.RecordStructure recordStructure = null;
    private org.osid.shared.Id id = null;
    private java.io.Serializable value = null;
    private org.osid.repository.PartStructure partStructure = null;

    protected Part(org.osid.shared.Id id
                 , org.osid.repository.RecordStructure recordStructure
                 , org.osid.repository.PartStructure partStructure
                 , java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        this.id = id;
        this.recordStructure = recordStructure;
        this.partStructure = partStructure;
        this.value = value;
    }

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return "VUE Part";
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    public org.osid.repository.Part createPart(org.osid.shared.Id partStructureId
                                         , java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        if ( (partStructureId == null) || (value == null) )
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }

        org.osid.repository.PartStructureIterator psi = recordStructure.getPartStructures();
        
        while (psi.hasNextPartStructure()) 
        {
            org.osid.repository.PartStructure partStructure = psi.nextPartStructure();
            try 
            {
                if (partStructureId.isEqual(partStructure.getId())) 
                {
                    org.osid.repository.Part part = new Part(partStructureId, recordStructure, partStructure, value);
                    partVector.addElement(part);
                    this.partStructure = partStructure;
                    return part;
                }
            } 
            catch (org.osid.OsidException oex) 
            {
                throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
            }
        }
        throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.UNKNOWN_ID);
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
        if (value == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        this.value = value;
    }

    public org.osid.repository.PartStructure getPartStructure()
    throws org.osid.repository.RepositoryException
    {
        return this.partStructure;
    }
}
