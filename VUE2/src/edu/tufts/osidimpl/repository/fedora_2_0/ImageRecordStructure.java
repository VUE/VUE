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

package   edu.tufts.osidimpl.repository.fedora_2_0;


public class ImageRecordStructure
        implements org.osid.repository.RecordStructure {
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "Image Specific Data";
    private String description = "Provides information about and image (thumbnail and large view URLs)";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("edu.mit","recordStructure","image");
    private org.osid.repository.PartStructure sThumbnailPartStructure = null;
    private org.osid.repository.PartStructure sURLPartStructure = null;
    private org.osid.repository.PartStructure sMediumImagePartStructure = null;
    
    protected ImageRecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException {
        try {
            this.id = new PID("ImageRecordStructureId");
        } catch (org.osid.shared.SharedException sex) {
        }
        this.sThumbnailPartStructure = new ThumbnailPartStructure(this, repository);
        this.sURLPartStructure = new URLPartStructure(this, repository);
        this.sMediumImagePartStructure = new MediumImagePartStructure(this,repository);
        
        this.partsVector.add(this.sThumbnailPartStructure);
        this.partsVector.add(this.sURLPartStructure);
        this.partsVector.add(this.sMediumImagePartStructure);
    }
    
    public String getDisplayName()
    throws org.osid.repository.RepositoryException {
        return this.displayName;
    }
    
    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public String getDescription()
    throws org.osid.repository.RepositoryException {
        return this.description;
    }
    
    public String getFormat()
    throws org.osid.repository.RepositoryException {
        return this.format;
    }
    
    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException {
        return this.id;
    }
    
    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException {
        return new PartStructureIterator(this.partsVector);
    }
    
    public String getSchema()
    throws org.osid.repository.RepositoryException {
        return this.schema;
    }
    
    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException {
        return this.type;
    }
    
    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException {
        return true;
    }
    
    public org.osid.repository.PartStructure getThumbnailPartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sThumbnailPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sThumbnailPartStructure;
    }
    
    public org.osid.repository.PartStructure getURLPartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sURLPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sURLPartStructure;
    }
    
    public org.osid.repository.PartStructure getMediumImagePartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sThumbnailPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sMediumImagePartStructure;
    }
    
    public static Record createImageRecord(String pid
            , ImageRecordStructure recordStructure
            , Repository repository
            , PID objectId
            , FedoraObjectAssetType assetType)
            throws org.osid.repository.RepositoryException {
        Record record = null;
        try {
            record = new Record(new PID(pid),recordStructure);
            System.out.println("creating " + recordStructure.getType().getKeyword() + " " + recordStructure.getDisplayName());
            if(assetType.getKeyword().equals("tufts/image/archival")) {
                record.createPart(recordStructure.getThumbnailPartStructure().getId(),
                        "http://"+repository.getAddress()+":"+"8080/fedora/get/" +objectId.getIdString()+"/bdef:TuftsImage/getThumbnail/");
                record.createPart(recordStructure.getURLPartStructure().getId(),
                        "http://"+repository.getAddress()+":"+repository.getPort()+"/fedora/get/" +objectId.getIdString()+"/bdef:AssetDef/getFullView/");
                record.createPart(recordStructure.getMediumImagePartStructure().getId(), Utilities.formatObjectUrl(objectId.getIdString(),"bdef:TuftsImage/getMediumRes/",repository));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return record;
    }
    
    
    
}
