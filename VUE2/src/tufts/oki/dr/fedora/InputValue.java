package tufts.oki.dr.fedora;

/*
 * InputValue.java
 *
 * Created on May 5, 2003, 6:26 PM
 */

   
 

import osid.dr.*;

/**
 *
 * @author  akumar03
 */
public class InputValue implements InfoField {
    private PID pid;
    /** Creates a new instance of InputValue */
    public InputValue() {
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
        return null;
    }
    
    /**     Get the for this InfoField.
     *     @return java.io.Serializable
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public java.io.Serializable getValue() throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Update the for this InfoField.
     *     @param java.io.Serializable
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateValue(java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
      
    }
    
}
