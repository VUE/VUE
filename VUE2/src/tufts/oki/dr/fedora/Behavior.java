package tufts.oki.dr.fedora;

/*
 * Behavior.java
 *
 * Created on May 5, 2003, 6:23 PM
 */
 
 
 
import osid.dr.*;
/**
 *
 * @author  akumar03
 */
public class Behavior implements InfoRecord  {
    
    private PID pid;
    private String ID;
    private java.util.Vector disseminationList = new java.util.Vector();
    private FedoraObject obj;
    private boolean isMultivalued = false;
    private BehaviorInfoStructure  infoStructure;
    
    /** Creates a new instance of Behavior */
    public Behavior() {
    }
    
    public Behavior(osid.shared.Id id,FedoraObject obj) throws osid.dr.DigitalRepositoryException{
        this.pid = (PID)id;
        this.obj = obj;
        try {
        disseminationList = FedoraSoapFactory.getDisseminators(this);
        } catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException(ex.getMessage());
        }
      
    }
    
    public Behavior(String id,FedoraObject obj,BehaviorInfoStructure infoStructure) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {
        this.obj = obj;
        this.infoStructure = infoStructure;
        this.pid = new PID(id);
        InfoPartIterator i = (InfoPartIterator)infoStructure.getInfoParts();
        while(i.hasNext()) {
            
            DisseminationInfoPart infoPart = (DisseminationInfoPart)i.next();
            disseminationList.add(new Dissemination(infoPart.getDisplayName(),this,infoPart));
        }  
    }
    // set-get methods.
    public  void setDisseminationList(java.util.Vector disseminationList) {
        this.disseminationList  = disseminationList;
    }
    public  java.util.Vector getDisseminationList() {
        return this.disseminationList;
    }
    public void setID (String pid) throws  osid.shared.SharedException{
        this.ID = pid;
        this.pid = new PID(pid) ;
    }
    public  String getID() {
        return this.ID;
    }
    public void setFedoraObject(FedoraObject obj) {
        this.obj = obj;
    }
    public FedoraObject getFedoraObject(){
        return obj;
    }
    
    
    /**     Create an InfoField.  InfoRecords are composed of InfoFields. InfoFields can also contain other InfoFields.  Each InfoRecord is associated with a specific InfoStructure and each InfoField is associated with a specific InfoPart.
     *     @param osid.shared.Id infoPartId
     *     @param java.io.Serializable value
     *     @return InfoField
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoField createInfoField(osid.shared.Id infoPartId, java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Delete an InfoField and all its InfoFields.
     *     @param osid.shared.Id infoFieldId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void deleteInfoField(osid.shared.Id infoFieldId) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Get the Unique Id for this InfoRecord.
     *     @return osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return pid;
    }
    
    /**     Get all the InfoFields in the InfoRecord.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoFieldIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoFieldIterator getInfoFields() throws osid.dr.DigitalRepositoryException {
        return new DisseminationIterator(disseminationList);
    }
    
    /**     Get the InfoStructure associated with this InfoRecord.
     *     @return InfoStructure
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoStructure getInfoStructure() throws osid.dr.DigitalRepositoryException {
        return infoStructure;
    }
    
    /**     Return true if this InfoRecord is multi-valued; false otherwise.  This is determined by the implementation.
     *     @return boolean
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public boolean isMultivalued() throws osid.dr.DigitalRepositoryException {
        return this.isMultivalued;
    }
    
    public void addObject(Object obj) {
      //  System.out.print("ignored"+obj);
       //-- ignore
    }
}


