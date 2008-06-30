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
 * FedoraObject.java
 *
 * Created on April 6, 2006, 1:57 PM
 *
 */
/**
 *
 * @author  akumar03
 */

package  edu.tufts.osidimpl.repository.fedora_2_2;

import org.osid.repository.*;
import java.net.*;
import java.io.*;
import java.util.Iterator;






public class Asset implements org.osid.repository.Asset{
    private PID pid;
    private org.osid.shared.Type  assetType;
    private String location;
    private java.util.Vector recordStructures = new java.util.Vector();
    private Repository repository;
    private String displayName;
    private String description ="Fedora Repository";
    // private BehaviorIterator behaviorIterator;
    java.util.Vector recordVector = new java.util.Vector();
    java.util.Vector recordStructureVector = new java.util.Vector();
    java.util.Vector mandatoryInfoStructureVector = new java.util.Vector();
    private long effectiveDate = 0;
    private long expirationDate = 0;
    
    private java.util.Properties ppt;
    
    /** Creates a new instance of FedoraObject */
    public Asset() {
    }
    
    public Asset(org.osid.shared.Id id){
        this.pid  = (PID)id;
    }
    public Asset(org.osid.shared.Id id, Repository repository) throws org.osid.repository.RepositoryException{
        this(id);
        this.repository = repository;
    }
    
    public Asset(Repository repository,String displayName,org.osid.shared.Type assetType)  throws org.osid.repository.RepositoryException,org.osid.shared.SharedException{
    }
    
    public Asset(Repository repository, String id, String displayName,org.osid.shared.Type assetType)  throws org.osid.repository.RepositoryException,org.osid.shared.SharedException{
        this.repository = repository;
        this.displayName = displayName;
        this.assetType = assetType;
        this.pid = new PID(id);
        // inforecords are not added to BDEFs and BMECHs
        if(!(assetType.getKeyword().equals(Repository.BDEF) || assetType.getKeyword().equals(Repository.BMECH))) {
            if(assetType.getKeyword().equals(repository.getFedoraProperties().getProperty("type.image"))){
                recordVector.add(ImageRecordStructure.createImageRecord(id,(ImageRecordStructure)((FedoraObjectAssetType) assetType).getImageRecordStructure(),																		repository,
                        pid,
                        (FedoraObjectAssetType) assetType,
                        getDisplayName(),
                        getId().getIdString()));
           
            } else if(repository.getConfiguration() != null && repository.getConfiguration().getProperty("thumbnailSuffix")!=null && repository.getConfiguration().getProperty("thumbnailSuffix").length() >0) {
               ImageRecordStructure imageRecordStructure = (ImageRecordStructure)((FedoraObjectAssetType) assetType).getImageRecordStructure();
               Record record  = new Record(pid,imageRecordStructure );
               record.createPart(imageRecordStructure.getThumbnailPartStructure().getId(), Utilities.formatObjectUrl(pid.getIdString(), repository.getConfiguration().getProperty("thumbnailSuffix"), repository));
               recordVector.add(record);
               recordVector.add(DefaultRecordStructure.createDefaultRecord(id,(DefaultRecordStructure)((FedoraObjectAssetType)assetType).getDefaultRecordStructure(),repository,pid,(FedoraObjectAssetType) assetType,getDisplayName(),getId().getIdString()));
            } else {
                recordVector.add(DefaultRecordStructure.createDefaultRecord(id,(DefaultRecordStructure)((FedoraObjectAssetType)assetType).getDefaultRecordStructure(),repository,pid,(FedoraObjectAssetType) assetType,getDisplayName(),getId().getIdString()));
            }
            
        }
    }
    
    //this method is for viewing objects in vue.  will be gone soon.
    
    // set-get methods.
    public  void setLocation(String location) {
        this.location = location;
    }
    
    public  String getLocation() {
        return this.location;
    }
    
    
    public void setPID(String pid) throws org.osid.shared.SharedException{
        this.pid = new PID(pid) ;
    }
    public PID getPID() throws org.osid.repository.RepositoryException {
        return this.pid;
    }
    public Repository getRepositoryObject() {
        return this.repository;
    }
    public void setRepositoryObject(Repository repository) {
        this.repository = repository;
    }
    
    /**     Add an Asset to this Asset.
     *     @param org.osid.shared.Id assetId
     *     @throws RepositoryException if there is a general failure
     */
    public void addAsset(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Add the specified InfoStructure and all the related Records from the specified asset.
     *     @param org.osid.shared.Id assetId
     *     @param org.osid.shared.Id infoStructureId
     *     @throws RepositoryException if there is a general failure
     */
    public void copyInfoStructure(org.osid.shared.Id assetId, org.osid.shared.Id infoStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Create a new Asset Record of the specified InfoStructure.  The implementation of this method sets the Id for the new object.
     *     @param org.osid.shared.Id infoStructureId
     *     @return Record
     *     @throws RepositoryException if there is a general failure or if the InfoStructure is unknown or not defined by this Asset's AssetType.
     */
    public org.osid.repository.Record createRecord(org.osid.shared.Id infoStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Delete an Record.  If the specified Record has content that is inherited by other Records, those
     *     @param org.osid.shared.Id recordId
     *     @throws RepositoryException if there is a general failure
     */
    public void deleteRecord(org.osid.shared.Id recordId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Description_getAssetTypes=Get the AssetType of this Asset.  AssetTypes are used to categorize Assets.
     *     @return org.osid.shared.Type
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.Type getAssetType() throws org.osid.repository.RepositoryException {
        return assetType;
    }
    
    /**     Get all the Assets in this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.repository.AssetIterator getAssets() throws org.osid.repository.RepositoryException {
        return null;
    }
    
    public org.osid.repository.RecordIterator getRecordsByRecordStructure(org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public void copyRecordStructure(org.osid.shared.Id assetId, org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Get an Asset's content.  This method can be a convenience if one is not interested in all the structure of the Records.
     *     @return java.io.Serializable
     *     @throws RepositoryException if there is a general failure
     */
    public java.io.Serializable getContent() throws org.osid.repository.RepositoryException {
        org.osid.repository.RecordIterator ri = getRecords();
        while (ri.hasNextRecord()) {
            org.osid.repository.Record r = ri.nextRecord();
            if (r.getRecordStructure().getDisplayName().equals("VUE Specific Data")) {
                org.osid.repository.PartIterator pi = r.getParts();
                while (pi.hasNextPart()) {
                    org.osid.repository.Part p = pi.nextPart();
                    if (p.getPartStructure().getDisplayName().equals("VUE Default View Part Structure")) {
                        return p.getValue();
                    }
                }
            }
        }
        return null;
    }
    
    /**     Get the description for this Asset.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDescription() throws org.osid.repository.RepositoryException {
        return description;
    }
    
    /**     Get the Repository in which this Asset resides.  This is set by the Repository's createAsset method.
     *     @return Repository
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.Id getRepository() throws org.osid.repository.RepositoryException {
        return repository.getId();
    }
    
    /**     Get the name for this Asset.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDisplayName() throws org.osid.repository.RepositoryException {
        return displayName;
    }
    
    /**     Get the Unique Id for this Asset.
     *     @return org.osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.Id getId() throws org.osid.repository.RepositoryException {
        
        return pid;
    }
    
    /**     Get all the Records for this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return RecordIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.repository.RecordIterator getRecords() throws org.osid.repository.RepositoryException {
        return new RecordIterator(recordVector);
    }
    
    public org.osid.repository.RecordIterator getRecordsByRecordStructureType(org.osid.shared.Type recordStructureType) throws org.osid.repository.RepositoryException {
        return new RecordIterator(new java.util.Vector());
    }
    
    /**     Get all the Records of the specified InfoStructure for this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @param org.osid.shared.Id infoStructureId
     *     @return RecordIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.repository.RecordIterator getRecords(org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        if (recordStructureId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        java.util.Vector result = new java.util.Vector();
        
        for (int i = 0, size = recordVector.size(); i < size; i++) {
            try {
                org.osid.repository.Record record = (org.osid.repository.Record) recordVector.elementAt(i);
                if (recordStructureId.isEqual(record.getRecordStructure().getId())) {
                    result.addElement(record);
                }
            } catch (org.osid.OsidException oex) {
                throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
            }
        }
        
        return (org.osid.repository.RecordIterator) (new RecordIterator(result));
    }
    
    /**     Get all the InfoStructures for this Asset.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.repository.RecordStructureIterator getRecordStructures() throws org.osid.repository.RepositoryException {
        return (org.osid.repository.RecordStructureIterator) new RecordStructureIterator(recordStructures);
    }
    
    /**     Add the specified InfoStructure and all the related Records from the specified asset.  The current and future content of the specified Record is synchronized automatically.
     *     @param org.osid.shared.Id assetId
     *     @param org.osid.shared.Id infoStructureId
     *     @throws RepositoryException if there is a general failure
     */
    public void inheritRecordStructure(org.osid.shared.Id assetId, org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Remove an Asset to this Asset.  This method does not delete the Asset from the Repository.
     *     @param org.osid.shared.Id assetId
     *     @throws RepositoryException if there is a general failure
     */
    public void removeAsset(org.osid.shared.Id assetId, boolean includeChildren) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Update an Asset's content.
     *     @param java.io.Serializable
     *     @throws RepositoryException if there is a general failure
     */
    public void updateContent(java.io.Serializable content) throws org.osid.repository.RepositoryException {
    }
    
    /**     Update the description for this Asset.
     *     @param String description
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDescription(String description) throws org.osid.repository.RepositoryException {
        this.description = description;
    }
    
    /**     Update the name for this Asset.
     *     @param String name
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDisplayName(String displayName) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public void addObject(Object obj) {
        //  System.out.print("ignored"+obj);
        //-- ignore
    }
    
    public String toString() {
        //try {
        return this.displayName;
        // }catch(org.osid.OsidException ex) {
        //     return  this.getClass().getName()+" has no Id set";
        //  }
    }
    
    public org.osid.repository.AssetIterator getAssets(org.osid.shared.Type assetType) throws org.osid.repository.RepositoryException {
        return null;
    }
    
    public org.osid.repository.RecordStructure getContentRecordStructure() throws org.osid.repository.RepositoryException {
        return null;
    }
    
    public org.osid.repository.AssetIterator getAssetsByType(org.osid.shared.Type type) throws org.osid.repository.RepositoryException {
        return  getAssets(type);
    }
    
    public long getEffectiveDate() throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public long getExpirationDate() throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.PartIterator getPartsByPartStructure(org.osid.shared.Id partStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.Part getPart(org.osid.shared.Id id) throws org.osid.repository.RepositoryException {
        Iterator i =recordVector.iterator();
        while(i.hasNext()) {
            Record record = (Record)i.next();
            org.osid.repository.PartIterator partIterator = record.getParts();
            while(partIterator.hasNextPart()){
                org.osid.repository.Part part = partIterator.nextPart();
//                try {
//                    if(part.getId().isEqual(id))
                return part;
//                } catch (org.osid.shared.SharedException ex) {
//                    throw new org.osid.repository.RepositoryException(ex.getMessage());
//                }
            }
        }
        return null;
    }
    
    public org.osid.repository.PartIterator getPartByPart(org.osid.shared.Id id) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public java.io.Serializable getPartValue(org.osid.shared.Id id) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.ObjectIterator getPartValueByPartStructure(org.osid.shared.Id id) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.ObjectIterator getPartValuesByPartStructure(org.osid.shared.Id partStructureId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.Record getRecord(org.osid.shared.Id id) throws org.osid.repository.RepositoryException {
        Iterator i = recordVector.iterator();
        while(i.hasNext()) {
            org.osid.repository.Record record = (Record)i.next();
            try {
                if(record.getId().getIdString().equals(id.getIdString()))
                    return record;
            } catch (org.osid.shared.SharedException ex) {
                throw new org.osid.repository.RepositoryException(ex.getMessage());
            }
        }
        return null;
    }
    
    public org.osid.repository.RecordIterator getRecordsByInfoStructure(org.osid.shared.Id id) throws org.osid.repository.RepositoryException {
        java.util.Vector result = new java.util.Vector();
        Iterator i = recordVector.iterator();
        while(i.hasNext()) {
            org.osid.repository.Record record = (Record)i.next();
            try {
                if(record.getRecordStructure().getId().getIdString().equals(id.getIdString()))
                    result.add(record);
            } catch (org.osid.shared.SharedException ex) {
                throw new org.osid.repository.RepositoryException(ex.getMessage());
            }
        }
        return new RecordIterator(result);
    }
    
    public void updateEffectiveDate(long date) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public void updateExpirationDate(long date) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
}
