/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

public class RelationPartStructure
implements org.osid.repository.PartStructure
{
    private String displayName = "Relation";
    private String description = "http://purl.org/dc/elements/1.1/#Relation";
    private String idString = "Relation.PartStructureId";
    private org.osid.shared.Id id = null;
    private org.osid.shared.Type type = new Type("mit.edu","partStructure","relation","Relation");
	private static RelationPartStructure relationPartStructure = new RelationPartStructure();
	
	public static RelationPartStructure getInstance()
	{
		return relationPartStructure;
	}
	
    protected RelationPartStructure()
    {
        try
        {
            this.id = Utilities.getIdManager().getId(this.idString);
        }
        catch (Throwable t)
        {
            Utilities.log(t);
        }
    }

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public String getDescription()
    throws org.osid.repository.RepositoryException
    {
        return this.description;
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException
    {
        return this.type;
    }

    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException
    {
        return new PartStructureIterator(new java.util.Vector());
    }

    public org.osid.repository.RecordStructure getRecordStructure()
    throws org.osid.repository.RepositoryException
    {
        return new RecordStructure();
    }

    public boolean isMandatory()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }

    public boolean isPopulatedByRepository()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }

    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }

    public boolean validatePart(org.osid.repository.Part part)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
}
