/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.oki.repository.fedora;

/*
 * FedoraObject.java
 *
 * Created on May 6, 2003, 1:57 PM
 * Model Updated Feb 5, 2004.  The model was cahnged to support any implementation of Fedora 1.1
 * It supports Diseminator view of the the object
 */
/**
 *
 * @author  akumar03
 */


import osid.repository.*;
import java.net.*;
import java.io.*;
import java.util.Iterator;


// these classses are required for soap implementation of
import javax.xml.namespace.QName;

import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;

//axis files
import org.apache.axis.encoding.ser.*;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException ;


public class Asset implements osid.repository.Asset{
    private PID pid;
    private osid.shared.Type  assetType;
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
    
    public Asset(osid.shared.Id id){
        this.pid  = (PID)id;
    }
    public Asset(osid.shared.Id id, Repository repository) throws osid.repository.RepositoryException{
        this(id);
        this.repository = repository;
    }
    
    public Asset(Repository repository,String displayName,osid.shared.Type assetType)  throws osid.repository.RepositoryException,osid.shared.SharedException{
    }
    
    public Asset(Repository repository, String id, String displayName,osid.shared.Type assetType)  throws osid.repository.RepositoryException,osid.shared.SharedException{
        this.repository = repository;
        this.displayName = displayName;
        this.assetType = assetType;
        this.pid = new PID(id);
        // inforecords are not added toe BDEFs and BMECHs
        if(!(assetType.getKeyword().equals("fedora:BDEF") || assetType.getKeyword().equals("fedora:BMECH")))
            recordVector = FedoraSoapFactory.getDisseminationRecords(id,((FedoraObjectAssetType) assetType).getDissemiationRecordStructure(),repository);
        recordVector.add(VUERecordStructure.createVUERecord(id,(VUERecordStructure)((FedoraObjectAssetType) assetType).getVUERecordStructure(), repository,pid,(FedoraObjectAssetType) assetType));
        
    }
    
    //this method is for viewing objects in vue.  will be gone soon.
    
    // set-get methods.
    public  void setLocation(String location) {
        this.location = location;
    }
    
    public  String getLocation() {
        return this.location;
    }
    
    
    public void setPID(String pid) throws osid.shared.SharedException{
        this.pid = new PID(pid) ;
    }
    public PID getPID() throws osid.repository.RepositoryException {
        return this.pid;
    }
    public Repository getRepositoryObject() {
        return this.repository;
    }
    public void setRepositoryObject(Repository repository) {
        this.repository = repository;
    }
    
    /**     Add an Asset to this Asset.
     *     @param osid.shared.Id assetId
     *     @throws RepositoryException if there is a general failure
     */
    public void addAsset(osid.shared.Id assetId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Add the specified InfoStructure and all the related Records from the specified asset.
     *     @param osid.shared.Id assetId
     *     @param osid.shared.Id infoStructureId
     *     @throws RepositoryException if there is a general failure
     */
    public void copyInfoStructure(osid.shared.Id assetId, osid.shared.Id infoStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Create a new Asset Record of the specified InfoStructure.  The implementation of this method sets the Id for the new object.
     *     @param osid.shared.Id infoStructureId
     *     @return Record
     *     @throws RepositoryException if there is a general failure or if the InfoStructure is unknown or not defined by this Asset's AssetType.
     */
    public osid.repository.Record createRecord(osid.shared.Id infoStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Delete an Record.  If the specified Record has content that is inherited by other Records, those
     *     @param osid.shared.Id recordId
     *     @throws RepositoryException if there is a general failure
     */
    public void deleteRecord(osid.shared.Id recordId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Description_getAssetTypes=Get the AssetType of this Asset.  AssetTypes are used to categorize Assets.
     *     @return osid.shared.Type
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.Type getAssetType() throws osid.repository.RepositoryException {
        return assetType;
    }
    
    /**     Get all the Assets in this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.AssetIterator getAssets() throws osid.repository.RepositoryException {
        return null;
    }
    
    public osid.repository.RecordIterator getRecordsByRecordStructure(osid.shared.Id recordStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public void copyRecordStructure(osid.shared.Id assetId, osid.shared.Id recordStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Get an Asset's content.  This method can be a convenience if one is not interested in all the structure of the Records.
     *     @return java.io.Serializable
     *     @throws RepositoryException if there is a general failure
     */
    public java.io.Serializable getContent() throws osid.repository.RepositoryException {
        return null;
    }
    
    /**     Get the description for this Asset.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDescription() throws osid.repository.RepositoryException {
        return description;
    }
    
    /**     Get the Repository in which this Asset resides.  This is set by the Repository's createAsset method.
     *     @return Repository
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.Id getRepository() throws osid.repository.RepositoryException {
        return repository.getId();
    }
    
    /**     Get the name for this Asset.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDisplayName() throws osid.repository.RepositoryException {
        return displayName;
    }
    
    /**     Get the Unique Id for this Asset.
     *     @return osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws RepositoryException if there is a general failure
     */
    public osid.shared.Id getId() throws osid.repository.RepositoryException {
    
        return pid;
    }
    
    /**     Get all the Records for this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return RecordIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.RecordIterator getRecords() throws osid.repository.RepositoryException {
        return new RecordIterator(recordVector);
    }
    
    public osid.repository.RecordIterator getRecordsByRecordStructureType(osid.shared.Type recordStructureType) throws osid.repository.RepositoryException {
        return new RecordIterator(new java.util.Vector());
    }
    
    /**     Get all the Records of the specified InfoStructure for this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @param osid.shared.Id infoStructureId
     *     @return RecordIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.RecordIterator getRecords(osid.shared.Id recordStructureId) throws osid.repository.RepositoryException {
        if (recordStructureId == null) {
            throw new osid.repository.RepositoryException(osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        java.util.Vector result = new java.util.Vector();
        
        for (int i = 0, size = recordVector.size(); i < size; i++) {
            try {
                osid.repository.Record record = (osid.repository.Record) recordVector.elementAt(i);
                if (recordStructureId.isEqual(record.getRecordStructure().getId())) {
                    result.addElement(record);
                }
            } catch (osid.OsidException oex) {
                throw new osid.repository.RepositoryException(osid.repository.RepositoryException.OPERATION_FAILED);
            }
        }
        
        return (osid.repository.RecordIterator) (new RecordIterator(result));
    }
    
    /**     Get all the InfoStructures for this Asset.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public osid.repository.RecordStructureIterator getRecordStructures() throws osid.repository.RepositoryException {
        return (osid.repository.RecordStructureIterator) new RecordStructureIterator(recordStructures);
    }
    
    /**     Add the specified InfoStructure and all the related Records from the specified asset.  The current and future content of the specified Record is synchronized automatically.
     *     @param osid.shared.Id assetId
     *     @param osid.shared.Id infoStructureId
     *     @throws RepositoryException if there is a general failure
     */
    public void inheritRecordStructure(osid.shared.Id assetId, osid.shared.Id recordStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Remove an Asset to this Asset.  This method does not delete the Asset from the Repository.
     *     @param osid.shared.Id assetId
     *     @throws RepositoryException if there is a general failure
     */
    public void removeAsset(osid.shared.Id assetId, boolean includeChildren) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Update an Asset's content.
     *     @param java.io.Serializable
     *     @throws RepositoryException if there is a general failure
     */
    public void updateContent(java.io.Serializable content) throws osid.repository.RepositoryException {
    }
    
    /**     Update the description for this Asset.
     *     @param String description
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDescription(String description) throws osid.repository.RepositoryException {
        this.description = description;
    }
    
    /**     Update the name for this Asset.
     *     @param String name
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDisplayName(String displayName) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public void addObject(Object obj) {
        //  System.out.print("ignored"+obj);
        //-- ignore
    }
    
    public String toString() {
        //try {
        return this.displayName;
        // }catch(osid.OsidException ex) {
        //     return  this.getClass().getName()+" has no Id set";
        //  }
    }
    
    public osid.repository.AssetIterator getAssets(osid.shared.Type assetType) throws osid.repository.RepositoryException {
        return null;
    }
    
    public osid.repository.RecordStructure getContentRecordStructure() throws osid.repository.RepositoryException {
        return null;
    }
    
    public osid.repository.AssetIterator getAssetsByType(osid.shared.Type type) throws osid.repository.RepositoryException {
        return  getAssets(type);
    }
    
    public long getEffectiveDate() throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public long getExpirationDate() throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public osid.repository.PartIterator getPartsByPartStructure(osid.shared.Id partStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public osid.repository.Part getPart(osid.shared.Id id) throws osid.repository.RepositoryException {
        Iterator i =recordVector.iterator();
        while(i.hasNext()) {
            Record record = (Record)i.next();
            osid.repository.PartIterator partIterator = record.getParts();
            while(partIterator.hasNextPart()){
                osid.repository.Part part = partIterator.nextPart();
//                try {
//                    if(part.getId().isEqual(id))
                        return part;
//                } catch (osid.shared.SharedException ex) {
//                    throw new osid.repository.RepositoryException(ex.getMessage());
//                }
            }
        }
        return null;
    }
    
    public osid.repository.PartIterator getPartByPart(osid.shared.Id id) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public java.io.Serializable getPartValue(osid.shared.Id id) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public osid.shared.ObjectIterator getPartValueByPartStructure(osid.shared.Id id) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public osid.shared.ObjectIterator getPartValuesByPartStructure(osid.shared.Id partStructureId) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.repository.Record getRecord(osid.shared.Id id) throws osid.repository.RepositoryException {
        Iterator i = recordVector.iterator();
        while(i.hasNext()) {
            osid.repository.Record record = (Record)i.next();
            try {
                if(record.getId().getIdString().equals(id.getIdString()))
                    return record;
            } catch (osid.shared.SharedException ex) {
                throw new osid.repository.RepositoryException(ex.getMessage());
            }
        }
        return null;
    }
    
    public osid.repository.RecordIterator getRecordsByInfoStructure(osid.shared.Id id) throws osid.repository.RepositoryException {
        java.util.Vector result = new java.util.Vector();
        Iterator i = recordVector.iterator();
        while(i.hasNext()) {
            osid.repository.Record record = (Record)i.next();
            try {  
                if(record.getRecordStructure().getId().getIdString().equals(id.getIdString()))
                    result.add(record);
            } catch (osid.shared.SharedException ex) {
                throw new osid.repository.RepositoryException(ex.getMessage());
            }
        }
        return new RecordIterator(result);
    }
    
    public void updateEffectiveDate(long date) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
    
    public void updateExpirationDate(long date) throws osid.repository.RepositoryException {
        throw new osid.repository.RepositoryException(osid.OsidException.UNIMPLEMENTED);
    }
}
