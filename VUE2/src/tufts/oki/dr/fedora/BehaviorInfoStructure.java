/*
 * BehaviorInfoStructure.java
 *
 * Created on October 10, 2003, 12:14 PM
 */

package    tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */


public class BehaviorInfoStructure implements osid.dr.InfoStructure {
    
    private java.util.Map configuration = null;
    java.util.Vector infoPartsVector = new java.util.Vector();
    private String displayName = "Behavior InfoStructure";
    private String description = "Defines the structure of generic Fedora Behavior";
    private osid.shared.Id id = null;
    private String schema = null;
    private String format = "Java Object";
    private DR dr;
    
    
    /** Creates a new instance of BehaviorInfoStructure */
    
    public BehaviorInfoStructure() {
    }
    
    public BehaviorInfoStructure(DR dr,String id,java.util.Iterator infoPartsIterator) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException{
        this.dr = dr;
        this.id = new PID(id);
        this.displayName = id;
        while(infoPartsIterator.hasNext()){
            String strInfoPart = (String)infoPartsIterator.next();
            infoPartsVector.add(new DisseminationInfoPart(dr,strInfoPart,this)); 
        }
        
    }
    
    public String getDescription() throws osid.dr.DigitalRepositoryException {
        return this.description;
    }
    
    public String getDisplayName() throws osid.dr.DigitalRepositoryException {
        return this.displayName;
    }
    
    public String getFormat() throws osid.dr.DigitalRepositoryException {
       return this.format;
    }
    
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return this.id;
    }
    
    public osid.dr.InfoPartIterator getInfoParts() throws osid.dr.DigitalRepositoryException {
          return (osid.dr.InfoPartIterator) (new InfoPartIterator(infoPartsVector,configuration));
    }
    
    public String getSchema() throws osid.dr.DigitalRepositoryException {
        return this.schema;
    }
    
    public boolean validateInfoRecord(osid.dr.InfoRecord infoRecord) throws osid.dr.DigitalRepositoryException {
        return false;
    }
    
}
