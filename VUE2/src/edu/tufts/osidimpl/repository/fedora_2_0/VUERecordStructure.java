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

public class VUERecordStructure
implements org.osid.repository.RecordStructure
{
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "VUE Specific Data";
    private String description = "Provides information to be used by VUE";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("tufts.edu","recordStructure","vue");
    private org.osid.repository.PartStructure sVUEDefaultViewPartStructure = null;

    protected VUERecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException
    {
        try
        {
            this.id = new PID("VUEInfoStructureId");
        }
        catch (org.osid.shared.SharedException sex)
        {
        }
        this.sVUEDefaultViewPartStructure = new VUEDefaultViewPartStructure(this, repository);
        this.partsVector.add(this.sVUEDefaultViewPartStructure);        
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

    public String getFormat()
    throws org.osid.repository.RepositoryException
    {
        return this.format;
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException
    {
        return new PartStructureIterator(this.partsVector);
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
        return false;
    }

    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public org.osid.repository.PartStructure getVUEDefaultViewPartStructure()
    throws org.osid.repository.RepositoryException
    {
        if (this.sVUEDefaultViewPartStructure == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sVUEDefaultViewPartStructure;
    }

    public static Record createVUERecord(String pid
                                       , VUERecordStructure recordStructure
                                       , Repository repository
                                       , PID objectId
                                       , FedoraObjectAssetType assetType)
    throws org.osid.repository.RepositoryException
    {
        Record record = null;
        try
        {
            record = new Record(new PID(pid),recordStructure);
            if(assetType.getKeyword().equals("TUFTS_STD_IMAGE"))
            {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.fedora.get") + "/"+objectId.getIdString()+"/bdef:11/getDefaultView/");
            }
            else if(assetType.getKeyword().equals("XML_TO_HTMLDOC"))
            {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString()+"/demo:77/getDocument/");
            }
            else
            {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString());
            }
                }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return record;
    }

	
 
}
