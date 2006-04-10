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

public class UVARecordStructure
implements org.osid.repository.RecordStructure
{
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "UVA Specific Data";
    private String description = "Provides information to be used by VUE";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("edu.uva","recordStructure","image");
    private org.osid.repository.PartStructure sThumbnailPartStructure = null;

    protected UVARecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException
    {
        try
        {
            this.id = new PID("UVARecordStructureId");
        }
        catch (org.osid.shared.SharedException sex)
        {
        }
        this.sThumbnailPartStructure = new ThumbnailPartStructure(this, repository);
        this.partsVector.add(this.sThumbnailPartStructure);        
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

    public org.osid.repository.PartStructure getThumbnailPartStructure()
    throws org.osid.repository.RepositoryException
    {
        if (this.sThumbnailPartStructure == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sThumbnailPartStructure;
    }

    public static Record createUVARecord(String pid
                                       , UVARecordStructure recordStructure
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
	                record.createPart(recordStructure.getThumbnailPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.seastar.fedora.get") +objectId.getIdString()+"/fedora-system:3/getItem?itemID=THUMB");
            }
		}
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return record;
    }

 
}
