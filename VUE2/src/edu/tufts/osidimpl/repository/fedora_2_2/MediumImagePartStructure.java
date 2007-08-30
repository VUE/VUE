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

public class MediumImagePartStructure
        implements org.osid.repository.PartStructure {
    private org.osid.shared.Id MEDIUM_IMAGE_PART_STRUCTURE_ID = null;
    private org.osid.shared.Type type = new Type("mit.edu","partStructure","mediumImage","Medium Image");
    private String displayName = "Medium Image";
    private String description = "Medium sized image";
    private boolean mandatory = false;
    private boolean populatedByRepository = true;
    private boolean repeatable = false;
    private org.osid.repository.RecordStructure recordStructure;
    
    protected MediumImagePartStructure(org.osid.repository.RecordStructure recordStructure
            , Repository repository)
            throws org.osid.repository.RepositoryException {
        this.recordStructure = recordStructure;
        try {
            this.MEDIUM_IMAGE_PART_STRUCTURE_ID = new PID("MediumImagePartStructureId");
        } catch (org.osid.shared.SharedException ex) {
            ex.printStackTrace();
        }
    }
    
    public String getDisplayName()
    throws org.osid.repository.RepositoryException {
        return this.displayName;
    }
    
    public String getDescription()
    throws org.osid.repository.RepositoryException {
        return this.description;
    }
    
    public boolean isMandatory()
    throws org.osid.repository.RepositoryException {
        return this.mandatory;
    }
    
    public boolean isPopulatedByRepository()
    throws org.osid.repository.RepositoryException {
        return this.populatedByRepository;
    }
    
    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException {
        return this.repeatable;
    }
    
    
    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException {
        return this.MEDIUM_IMAGE_PART_STRUCTURE_ID;
    }
    
    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException {
        return this.type;
    }
    
    public org.osid.repository.RecordStructure getRecordStructure()
    throws org.osid.repository.RepositoryException {
        return this.recordStructure;
    }
    
    public boolean validatePart(org.osid.repository.Part part)
    throws org.osid.repository.RepositoryException {
        return true;
    }
    
    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException {
        return new PartStructureIterator(new java.util.Vector());
    }
}
