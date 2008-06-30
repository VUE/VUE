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
 * BDEFInfoPart.java
 *
 * Created on March 5, 2004, 6:48 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class BDEFInfoPart implements osid.dr.InfoPart {
    
    java.util.Vector partsVector = new java.util.Vector();
    osid.dr.InfoStructure disseminationInfoStructure = null;
    private osid.OsidOwner owner = null;
    private java.util.Map configuration = null;
    private String displayName = "BDEF";
    private String description = "Behaviour Definition of Fedora Object";
    private osid.shared.Id id = null;
    private boolean populatedByDR = true;
    private boolean mandatory = true;
    private boolean repeatable = false;
    private osid.dr.InfoStructure infoStructure = (osid.dr.InfoStructure) disseminationInfoStructure;

    /** Creates a new instance of BDEFInfoPart */
    public BDEFInfoPart(osid.dr.InfoStructure infoStructure,DR dr) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {
        this.infoStructure = infoStructure;
        this.id = new PID(FedoraUtils.getFedoraProperty(dr, "BDEFInfoPartId"));
    }
    
      /**
     * Get the display name for this InfoPart.
     *
     * @return String the display name
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public String getDisplayName() throws osid.dr.DigitalRepositoryException {
        return this.displayName;
    }

    /**
     * Get the description for this InfoPart.
     *
     * @return String the description
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public String getDescription() throws osid.dr.DigitalRepositoryException {
        return this.description;
    }

    /**
     * Get the unique Id for this InfoPart.
     *
     * @return osid.shared.Id A unique Id that is usually set by a create
     *         method's implementation
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return this.id;
    }

    /**
     * Return true if this InfoPart is automatically populated by the
     * DigitalRepository; false otherwise.  Examples of the kind of InfoParts
     * that might be populated are a timestamp or the Agent setting the data.
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public boolean isPopulatedByDR() throws osid.dr.DigitalRepositoryException {
        return this.populatedByDR;
    }

    /**
     * Return true if this InfoPart is mandatory; false otherwise.
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public boolean isMandatory() throws osid.dr.DigitalRepositoryException {
        return this.mandatory;
    }

    /**
     * Return true if this InfoPart is repeatable; false otherwise.
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public boolean isRepeatable() throws osid.dr.DigitalRepositoryException {
        return this.repeatable;
    }

    /**
     * Get the InfoPart associated with this InfoStructure.
     *
     * @return InfoStructure
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public osid.dr.InfoStructure getInfoStructure()
        throws osid.dr.DigitalRepositoryException {
        return this.infoStructure;
    }
    
    /**
     * Get all the InfoParts in the InfoPart.  Iterators return a set, one at a
     * time.  The Iterator's hasNext method returns true if there are
     * additional objects available; false otherwise.  The Iterator's next
     * method returns the next object.
     *
     * @return InfoPartIterator  The order of the objects returned by the
     *         Iterator is not guaranteed.
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public osid.dr.InfoPartIterator getInfoParts()
        throws osid.dr.DigitalRepositoryException {
        return (osid.dr.InfoPartIterator) (new InfoPartIterator(partsVector));
    }

    /**
     * Validate an InfoField against its InfoPart.  Return true if valid; false
     * otherwise.  The status of the Asset holding this InfoRecord is not
     * changed through this method.  The implementation may throw an Exception
     * for any validation failures and use the Exception's message to identify
     * specific causes.
     *
     * @param InfoField
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public boolean validateInfoField(osid.dr.InfoField infoField)
        throws osid.dr.DigitalRepositoryException {
        return true;
    } 
    
}
