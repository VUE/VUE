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

package edu.tufts.osidimpl.repository.google.global;

public class RecordStructure
implements org.osid.repository.RecordStructure
{
    private String idString = "GoogleResultRecordStructureId";
    private String displayName = "Result from Google";
    private String description = "Holds metadata for a Google result";
    private String format = "";
    private String schema = "";
    private org.osid.shared.Type type = new Type("edu.tufts","recordStructure","localFile");
    private boolean repeatable = false;
	private static RecordStructure recordStructure = new RecordStructure();
    private org.osid.shared.Id id = null;
	
	protected static RecordStructure getInstance()
	{
		return recordStructure;
	}
	
    protected RecordStructure()
    {
        try
		{
			id = Utilities.getIdManager().getId(this.idString);
		}
		catch (Throwable t)
		{
			System.out.println(t.getMessage());
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

    public String getFormat()
    throws org.osid.repository.RepositoryException
    {
        return this.format;
    }

    public String getSchema()
    throws org.osid.repository.RepositoryException
    {
        return this.schema;
    }

    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException
    {
        return this.type;
    }

    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException
    {
        return this.repeatable;
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException
    {
        java.util.Vector results = new java.util.Vector();
        try
        {
            results.addElement(URLPartStructure.getInstance());
        }
        catch (Throwable t)
        {
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
        return new PartStructureIterator(results);
    }

    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }
}
