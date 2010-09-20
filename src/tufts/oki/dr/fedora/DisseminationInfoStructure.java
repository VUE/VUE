/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
 * DisseminationInfoStructure.java
 *
 * Created on March 7, 2004, 8:31 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class DisseminationInfoStructure implements osid.dr.InfoStructure {
    private osid.OsidOwner owner = null;
    java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "Dissemination";
    private String description = "Provides information needed to get the dissemination";
    private osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    
    private osid.dr.InfoPart BDEFInfoPart  = null;
    private osid.dr.InfoPart disseminationURLInfoPart = null;
    private osid.dr.InfoPart parameterInfoPart = null;
    
    /** Creates a new instance of DisseminationInfoStructure */
    public DisseminationInfoStructure(DR dr) throws osid.shared.SharedException, osid.dr.DigitalRepositoryException{
        this.id = new PID(FedoraUtils.getFedoraProperty(dr, "DisseminationInfoStructureId"));
        this.BDEFInfoPart = new BDEFInfoPart(this, dr);
        this.disseminationURLInfoPart = new DisseminationURLInfoPart(this,dr);
        this.parameterInfoPart = new ParameterInfoPart(this,dr);
        partsVector.add(this.BDEFInfoPart);
        partsVector.add(this.disseminationURLInfoPart);
        partsVector.add(this.parameterInfoPart);
    }


    /**
     * Get the display name for this InfoStructure.
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
     * Get the description for this InfoStructure.
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
     * Get the unique Id for this InfoStructure.
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
     * Get the schema for this InfoStructure.  The schema is defined by the
     * implementation, e.g. Dublin Core.
     *
     * @return String
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public String getSchema() throws osid.dr.DigitalRepositoryException {
        return this.schema;
    }
    
    /**
     * Get the format for this InfoStructure.  The format is defined by the
     * implementation, e.g. XML.
     *
     * @return String
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public String getFormat() throws osid.dr.DigitalRepositoryException {
        return this.format;
    }
    
    /**
     * Get all the InfoParts in the InfoStructure.  Iterators return a set, one
     * at a time.  The Iterator's hasNext method returns true if there are
     * additional objects available; false otherwise.  The Iterator's nextthrows osid.dr.DigitalRepositoryException
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
     * Validate an InfoRecord against its InfoStructure.  Return true if valid;
     * false otherwise.  The status of the Asset holding this InfoRecord is
     * not changed through this method.  The implementation may throw an
     * Exception for any validation failures and use the Exception's message
     * to identify specific causes.
     *
     * @param InfoRecord
     *
     * @return boolean
     *
     * @throws An exception with one of the following messages defined in
     *         osid.dr.DigitalRepositoryException may be thrown:
     */
    public boolean validateInfoRecord(osid.dr.InfoRecord infoRecord)
    throws osid.dr.DigitalRepositoryException {
        return true;
    }
    
    public osid.dr.InfoPart getBDEFInfoPart() throws osid.dr.DigitalRepositoryException {
        if(this.BDEFInfoPart == null)
            throw new osid.dr.DigitalRepositoryException("BDEF InfoPart doesn't exist");
        return this.BDEFInfoPart;
    }
    
    public osid.dr.InfoPart getDisseminationURLInfoPart() throws osid.dr.DigitalRepositoryException {
        if(this.disseminationURLInfoPart == null)
            throw new osid.dr.DigitalRepositoryException("Dissemination URL InfoPart doesn't exist");
        return this.disseminationURLInfoPart;
    }
    public osid.dr.InfoPart getParameterInfoPart() throws osid.dr.DigitalRepositoryException {
        if(this.parameterInfoPart == null)
            throw new osid.dr.DigitalRepositoryException("Parameter InfoPart doesn't exist");
        return this.parameterInfoPart;
    }

    public String toString() {
        return super.toString()
            + "[" + displayName + "]";
        /*
            + "[id=" + id
            + " name=" + displayName
            + " desc=" + description
            + " schema=" + schema
            + " format=" + format
            + "]";
        */
    }
    
}
