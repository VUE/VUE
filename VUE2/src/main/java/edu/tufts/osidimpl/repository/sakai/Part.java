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

public class Part
implements org.osid.repository.Part
{
    private org.osid.repository.PartStructure partStructure = null;
    private org.osid.shared.Id partStructureId = null;
    private java.io.Serializable value = null;
    private String displayName = null;
    private org.osid.shared.Id id = null;

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    protected Part(org.osid.shared.Id partStructureId
                 , java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        this.partStructureId = partStructureId;
		
        this.value = value;

		try
        {
			if (partStructureId.isEqual(URLPartStructure.getInstance().getId())) {
				this.partStructure = URLPartStructure.getInstance();
				this.id = Utilities.getIdManager().getId("URL");
			}
        }
        catch (Throwable t)
        {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }

    }

    public org.osid.repository.Part createPart(org.osid.shared.Id partStructureId
                                             , java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void deletePart(org.osid.shared.Id partStructureId)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.PartIterator getParts()
    throws org.osid.repository.RepositoryException
    {
        return new PartIterator(new java.util.Vector());
    }

    public org.osid.repository.PartStructure getPartStructure()
    throws org.osid.repository.RepositoryException
    {
		return this.partStructure;
    }

    public java.io.Serializable getValue()
    throws org.osid.repository.RepositoryException
    {
        return this.value;
    }

    public void updateValue(java.io.Serializable value)
    throws org.osid.repository.RepositoryException
    {
        this.value = value;
    }
}
