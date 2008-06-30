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

/*
 * InfoField.java
 *
 * Created on March 8, 2004, 11:06 AM
 */

package  tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class InfoField implements osid.dr.InfoField {
    
    /** Creates a new instance of InfoField */
    private osid.OsidOwner owner = null;
    private java.util.Vector infoFieldVector = new java.util.Vector();
    private osid.dr.InfoStructure infoStructure = null;
    private osid.shared.Id id = null;
    private java.io.Serializable value = null;
    private osid.dr.InfoPart infoPart = null;
    
    public InfoField() {
    }
    
    public InfoField(osid.shared.Id id,osid.dr.InfoStructure infoStructure, osid.dr.InfoPart infoPart,java.io.Serializable value) {
        this.id = id;
        this.infoStructure = infoStructure;
        this.infoPart = infoPart;
        this.value = value;
    }

    public String toString() {
        return getClass().getName() + "[" + id + " " + value + "]";
    }
    
    /**
     * Get the unique Id for this InfoField.
     *
     * @return osid.shared.Id A unique Id that is usually set by a create
     *         method's implementation
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return this.id;
    }
    
    /**
     * Create an InfoField.  InfoRecords are composed of InfoFields. InfoFields
     * can also contain other InfoFields.  Each InfoRecord is associated with
     * a specific InfoStructure and each InfoField is associated with a
     * specific InfoPart.
     *
     * @param infoPartId
     * @param value
     *
     * @return osid.dr.InfoField
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED, NULL_ARGUMENT, UNKNOWN_ID
     */
    public osid.dr.InfoField createInfoField(osid.shared.Id infoPartId,
    java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
        if ((infoPartId == null) || (value == null)) {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }
        
        osid.dr.InfoPartIterator ipi = infoStructure.getInfoParts();
        
        while (ipi.hasNext()) {
            osid.dr.InfoPart infoPart = ipi.next();
            try {
                if (infoPartId.isEqual(infoPart.getId())) {
                    osid.dr.InfoField infoField = new InfoField(infoPartId,infoStructure,infoPart, value);
                    infoFieldVector.addElement(infoField);
                    this.infoPart = infoPart;
                    return infoField;
                }
            } catch (osid.OsidException oex) {
                throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.OPERATION_FAILED);
            }
        }
        
        throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.UNKNOWN_ID);
    }
    
    /**
     * Delete an InfoField and all its InfoFields.
     *
     * @param infoFieldId
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED, NULL_ARGUMENT, UNKNOWN_ID
     */
    public void deleteInfoField(osid.shared.Id infoFieldId)  throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException("A field can't be deleted from FEDORA Repository");
    }
    
    /**
     * Get all the InfoFields in the InfoField.  Iterators return a set, one at
     * a time.  The Iterator's hasNext method returns true if there are
     * additional objects available; false otherwise.  The Iterator's next
     * method returns the next object.
     *
     * @return osid.dr.InfoFieldIterator  The order of the objects returned by
     *         the Iterator is not guaranteed.
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public osid.dr.InfoFieldIterator getInfoFields() throws osid.dr.DigitalRepositoryException {
        return (osid.dr.InfoFieldIterator) (new InfoFieldIterator(infoFieldVector));
    }
    
    /**
     * Get the for this InfoField.
     *
     * @return java.io.Serializable
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public java.io.Serializable getValue() throws osid.dr.DigitalRepositoryException {
        
        return this.value;
    }
    
    /**
     * Update the for this InfoField.
     *
     * @param
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED, NULL_ARGUMENT
     */
    public void updateValue(java.io.Serializable value)
    throws osid.dr.DigitalRepositoryException {
        if (value == null) {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }
        
        this.value = value;
        
    }
    
    /**
     * Get the InfoPart associated with this InfoField.
     *
     * @return osid.dr.InfoPart
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public osid.dr.InfoPart getInfoPart()
    throws osid.dr.DigitalRepositoryException {
        return this.infoPart;
    }
    
}
