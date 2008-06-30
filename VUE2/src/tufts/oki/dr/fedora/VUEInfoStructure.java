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
 * VUEInfoStructure.java
 *
 * Created on March 24, 2004, 7:53 PM
 */

package  tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class VUEInfoStructure implements osid.dr.InfoStructure {
    private osid.OsidOwner owner = null;
    java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "VUE Specific Data";
    private String description = "Provides information to be used by VUE";
    private osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    
    private osid.dr.InfoPart sVUEDefaultViewInfoPart = null;
    
    /** Creates a new instance of DisseminationInfoStructure */
    public VUEInfoStructure(DR dr) throws osid.shared.SharedException, osid.dr.DigitalRepositoryException{
        this.id = new PID(FedoraUtils.getFedoraProperty(dr, "VUEInfoStructureId"));
        this.sVUEDefaultViewInfoPart= new VUEDefaultViewInfoPart(this, dr);
        partsVector.add(this.sVUEDefaultViewInfoPart);
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
    
    public osid.dr.InfoPart getVUEDefaultViewInfoPart() throws osid.dr.DigitalRepositoryException {
        if(this.sVUEDefaultViewInfoPart == null)
            throw new osid.dr.DigitalRepositoryException("BDEF InfoPart doesn't exist");
        return this.sVUEDefaultViewInfoPart;
    }
    
    public static InfoRecord createVUEInfoRecord(String pid,VUEInfoStructure infoStructure,DR dr,PID objectId,FedoraObjectAssetType assetType)  throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {
        InfoRecord infoRecord = new InfoRecord(new PID(pid),infoStructure);
        if(assetType.getKeyword().equals("TUFTS_STD_IMAGE"))
            infoRecord.createInfoField(infoStructure.getVUEDefaultViewInfoPart().getId(),dr.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString()+"/bdef:11/getDefaultView/");
        else if(assetType.getKeyword().equals("XML_TO_HTMLDOC"))
            infoRecord.createInfoField(infoStructure.getVUEDefaultViewInfoPart().getId(),dr.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString()+"/bdef:11/getDefaultView/");
        else
            infoRecord.createInfoField(infoStructure.getVUEDefaultViewInfoPart().getId(),dr.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString());
        return infoRecord;
    }
    
}