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

public class VUEDefaultViewPartStructure
implements org.osid.repository.PartStructure
{
    private java.util.Vector partsVector = new java.util.Vector();
    private org.osid.repository.RecordStructure disseminationRecordStructure = null;
    private String displayName = "VUE Default View Part Structure";
    private String description = "Store URL for VUE to view the object";
    private org.osid.shared.Id id = null;
    private boolean populatedByRepository = false;
    private boolean mandatory = false;
    private boolean repeatable = false;
    private org.osid.shared.Type type = new Type("tufts.edu","partStructure","defaultView");
    private org.osid.repository.RecordStructure recordStructure = (org.osid.repository.RecordStructure)disseminationRecordStructure;

    protected VUEDefaultViewPartStructure(org.osid.repository.RecordStructure recordStructure
                                        , Repository repository)
    throws org.osid.repository.RepositoryException
	{
        this.recordStructure = recordStructure;
        try
        {
            this.id = new PID("VUEDefaultViewInfoPartId");
        }
        catch (org.osid.shared.SharedException sex)
        {
        }
    }

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
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
        return new PartStructureIterator(this.partsVector);
    }

    public org.osid.repository.RecordStructure getRecordStructure()
    throws org.osid.repository.RepositoryException
    {
        return this.recordStructure;
    }

    public boolean isMandatory()
    throws org.osid.repository.RepositoryException
    {
        return this.mandatory;
    }

    public boolean isPopulatedByRepository()
    throws org.osid.repository.RepositoryException
    {
        return this.populatedByRepository;
    }

    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException
    {
        return this.repeatable;
    }

    public boolean validatePart(org.osid.repository.Part part)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }
 
}
