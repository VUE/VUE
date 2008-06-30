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

/*
 * MetadaElementPartStructure.java
 *
 * Created on April 26, 2006, 12:30 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author AKumar03
 */
public class MetadataElementPartStructure implements org.osid.repository.PartStructure{
    
   private java.util.Vector partsVector = new java.util.Vector();
    private org.osid.repository.RecordStructure disseminationRecordStructure = null;
    private String displayName = "Part structure for metadata elements";
    private String description = "Stores metadata elements";
    private org.osid.shared.Id id = null;
    private boolean populatedByRepository = false;
    private boolean mandatory = false;
    private boolean repeatable = false;
    private org.osid.shared.Type type = new Type("tufts.edu","partStructure","metadataElement");
    private org.osid.repository.RecordStructure recordStructure = (org.osid.repository.RecordStructure)disseminationRecordStructure;

    protected MetadataElementPartStructure(org.osid.repository.RecordStructure recordStructure
                                        , Repository repository)
    throws org.osid.repository.RepositoryException
	{
        this.recordStructure = recordStructure;
        try
        {
            this.id = new PID("MetadataElementPartId");
        }
        catch (org.osid.shared.SharedException sex)
        {
        }
    }
    
    protected MetadataElementPartStructure(String id, org.osid.repository.RecordStructure recordStructure
                                        , Repository repository, org.osid.shared.Type type)
    throws org.osid.repository.RepositoryException
	{
        this.recordStructure = recordStructure;
        this.type = type;
        try
        {
            this.id = new PID(id);
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
