package tufts.oki.dr.fedora;

/*
 * Dissemination.java
 *
 * Created on May 5, 2003, 6:20 PM
 */

 
 

import osid.dr.*;
import fedora.server.types.gen.*;
/**
 *
 * @author  akumar03
 */
public class Dissemination implements InfoField {
    
    private PID pid;
    private String name;
    private MIMETypedStream value;
    private Behavior behavior;
    private DisseminationInfoPart infoPart;
    /** Creates a new instance of Dissemination */
    public Dissemination() {
    }
    
    public Dissemination(osid.shared.Id id){
        this.pid  = (PID)id;
    }
    
    public Dissemination(osid.shared.Id id, Behavior behavior) throws osid.dr.DigitalRepositoryException{
        this.pid = (PID)id;
        this.behavior = behavior;
       // this.value = FedoraSoapFactory.getDisseminaionStream(this);
    }
    
    public Dissemination(String id,Behavior behavior,DisseminationInfoPart infoPart) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {
        this.pid = new PID(id);
        this.behavior = behavior;
        this.infoPart = infoPart;
    }
        
    //set-get methods
    public  void setName(String name) {
        this.name  = name;
    }
    public  String getName() {
        return this.name;
    }
    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }
    public Behavior getBehavior(){
        return this.behavior;
    }
    public void setValue(MIMETypedStream value) {
        this.value = value;
    }
    
    /**     Create an InfoField.  InfoRecords are composed of InfoFields. InfoFields can also contain other InfoFields.  Each InfoRecord is associated with a specific InfoStructure and each InfoField is associated with a specific InfoPart.
     *     @param osid.shared.Id infoPartId
     *     @param java.io.Serializable value
     *     @return InfoField
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoField createInfoField(osid.shared.Id infoPartId, java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
        return this;
    }
    
    /**     Delete an InfoField and all its InfoFields.
     *     @param osid.shared.Id infoFieldId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void deleteInfoField(osid.shared.Id infoFieldId) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Get the Unique Id for this InfoStructure.
     *     @return osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return pid;
    }
    
    /**     Get all the InfoFields in the InfoField.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoFieldIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoFieldIterator getInfoFields() throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Get the InfoPart associated with this InfoField.
     *     @return InfoPart
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoPart getInfoPart() throws osid.dr.DigitalRepositoryException {
        return infoPart;
    }
    
    /**     Get the for this InfoField.
     *     @return java.io.Serializable
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public java.io.Serializable getValue() throws osid.dr.DigitalRepositoryException {
        return FedoraSoapFactory.getDisseminaionStream(this);
    }
    
    /**     Update the for this InfoField.
     *     @param java.io.Serializable
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateValue(java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
    }
    
    public void addObject(Object obj) {
      //  System.out.print("ignored"+obj);
       //-- ignore
    }
}
