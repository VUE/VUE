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

package edu.tufts.osidimpl.repository.artifact;

public class PeriodPartStructure
implements org.osid.repository.PartStructure
{
    private org.osid.shared.Id periodPartStructureId = null;
    private org.osid.shared.Type type = new Type("mit.edu","partStructure","period","Period");
    private String displayName = "Period";
    private String description = "Period";
    private boolean mandatory = false;
    private boolean populatedByRepository = true;
    private boolean repeatable = false;
	private static PeriodPartStructure periodPartStructure = new PeriodPartStructure();
	
	protected static PeriodPartStructure getInstance()
	{
		return periodPartStructure;
	}
	
    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return displayName;
    }

    public String getDescription()
    throws org.osid.repository.RepositoryException
    {
        return description;
    }

    public boolean isMandatory()
    throws org.osid.repository.RepositoryException
    {
        return mandatory;
    }

    public boolean isPopulatedByRepository()
    throws org.osid.repository.RepositoryException
    {
        return populatedByRepository;
    }

    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException
    {
        return repeatable;
    }

    protected PeriodPartStructure()
    {
        try
        {
            periodPartStructureId = Utilities.getIdManager().getId("PeriodPartStructureId");
        }
        catch (Throwable t)
        {
        }        
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return periodPartStructureId;
    }

    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException
    {
        return type;
    }

    public org.osid.repository.RecordStructure getRecordStructure()
    throws org.osid.repository.RepositoryException
    {
        return RecordStructure.getInstance();
    }

    public boolean validatePart(org.osid.repository.Part part)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException
    {
        return new PartStructureIterator(new java.util.Vector());
    }
}
