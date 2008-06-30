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
 * InfoRecord.java
 *
 * Created on March 8, 2004, 11:21 AM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class InfoRecord implements osid.dr.InfoRecord {
    private osid.dr.InfoStructure infoStructure = null;
    private java.util.Vector infoFieldVector = new java.util.Vector();
    private osid.OsidOwner owner = null;
    private osid.shared.Id id = null;
    private boolean isMultivalued = false;
    /** Creates a new instance of InfoRecord */
    public InfoRecord(osid.shared.Id id, osid.dr.InfoStructure infoStructure) {
        this.id = id;
        this.infoStructure = infoStructure;
    }
    
      
    /**
     * Get the unique Id for this InfoRecord.
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
    public void deleteInfoField(osid.shared.Id infoFieldId)
        throws osid.dr.DigitalRepositoryException {
        
        throw new osid.dr.DigitalRepositoryException("An info Record can't be deleted from FEDORA object");
    }

    /**
     * Get all the InfoFields in the InfoRecord.  Iterators return a set, one
     * at a time.  The Iterator's hasNext method returns true if there are
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
    public osid.dr.InfoFieldIterator getInfoFields()
        throws osid.dr.DigitalRepositoryException {
        return (osid.dr.InfoFieldIterator) (new InfoFieldIterator(infoFieldVector));
    }

    /**
     * Return true if this InfoRecord is multi-valued; false otherwise.  This
     * is determined by the implementation.
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public boolean isMultivalued() throws osid.dr.DigitalRepositoryException {
        return this.isMultivalued;
    }

    /**
     * Get the InfoStructure associated with this InfoRecord.
     *
     * @return osid.dr.InfoStructure
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     *         OPERATION_FAILED
     */
    public osid.dr.InfoStructure getInfoStructure()
        throws osid.dr.DigitalRepositoryException {
        return this.infoStructure;
    }
    
    public osid.dr.InfoField getInfoField(osid.shared.Id infoPartId) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {
        java.util.Iterator i = infoFieldVector.iterator();
        while(i.hasNext()) {
            InfoField infoField = (InfoField)i.next();
            if(infoField.getInfoPart().getId().getIdString().equals(infoPartId.getIdString())) {
                return infoField;
            }
        }
        throw new osid.dr.DigitalRepositoryException("No InfoField Found");
    }

    public String toString() {
        return getClass().getName() + "[fields=" + infoFieldVector.size() + " structure=" + infoStructure + "]";
    }
    
}
