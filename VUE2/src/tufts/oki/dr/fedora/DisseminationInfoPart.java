/*
 * DisseminationInfoPart.java
 *
 * Created on October 10, 2003, 4:49 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class DisseminationInfoPart implements osid.dr.InfoPart {
    java.util.Vector infoPartsVector = new java.util.Vector();
    osid.dr.InfoStructure behaviorInfoStructure = null;
    //private osid.OsidOwner owner = null;
    //private java.util.Map configuration = null;
    private String displayName = "Dissemination InfoPart";
    private String description = "this describes the dissemination";
    private osid.shared.Id id = null;
    private boolean populatedByDR = false;
    private boolean mandatory = false;
    private boolean repeatable = false;
    private DR dr;
    
    /** Creates a new instance of DisseminationInfoPart */
    public DisseminationInfoPart() {
    }
    
    public DisseminationInfoPart(DR dr,String id,BehaviorInfoStructure behaviorInfoStructure) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException{
        this.dr = dr;
        this.id = new PID(id);
        this.displayName = id;
        this.behaviorInfoStructure = behaviorInfoStructure;
        repeatable = true;
    }
    
    
    public String getDescription() throws osid.dr.DigitalRepositoryException{
        return this.description;
    }
    
    public String getDisplayName() throws osid.dr.DigitalRepositoryException{
        return this.displayName;
    }
    
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException{
        return this.id;
    }
    
    
    public osid.dr.InfoPartIterator getInfoParts() throws osid.dr.DigitalRepositoryException{
        return new InfoPartIterator(infoPartsVector);
    }
    
    public osid.dr.InfoStructure getInfoStructure() throws osid.dr.DigitalRepositoryException{
        return behaviorInfoStructure;
    }
    
    public boolean isMandatory() throws osid.dr.DigitalRepositoryException{
        return mandatory;
    }
    
    public boolean isPopulatedByDR() {
        return populatedByDR;
    }
    
    public boolean isRepeatable() {
        return repeatable;
    }
    
    public boolean validateInfoField(osid.dr.InfoField infoField) {
        return false;
    }   
}