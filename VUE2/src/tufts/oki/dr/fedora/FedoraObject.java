package tufts.oki.dr.fedora;

/*
 * FedoraObject.java
 *
 * Created on May 6, 2003, 1:57 PM
 */

 /**
 *
 * @author  akumar03
 */
    

import osid.dr.*;
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


public class FedoraObject implements osid.dr.Asset{
    private PID pid;
    private osid.shared.Type  assetType;
    private String location; 
    private java.util.Vector behaviorList = new java.util.Vector();
    private java.util.Vector infoStructures = new java.util.Vector();
    private DR dr;
    private String displayName;
    private String description ="Fedora Repository";
   // private BehaviorIterator behaviorIterator;
    private java.util.Properties ppt;
   
    /** Creates a new instance of FedoraObject */
    public FedoraObject() {
    }
    
    public FedoraObject(osid.shared.Id id){
        this.pid  = (PID)id;
      //  getObject();
    }   
    public FedoraObject(osid.shared.Id id, DR dr) throws osid.dr.DigitalRepositoryException{
        this.pid  = (PID)id;
        this.dr = dr;
        try {
            System.out.println("Creating object "+pid.getIdString());
            behaviorList = FedoraSoapFactory.getBehaviors(this);
            System.out.println("Ferora Object Behavior count ="+behaviorList.size());
        
        } catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException(ex.getMessage());
        }
    }
    
    public FedoraObject(DR dr,String displayName,osid.shared.Type assetType)  throws osid.dr.DigitalRepositoryException{
        this.dr = dr;
        this.displayName = displayName;
        this.assetType = assetType;
        try{
            this.pid = new PID(displayName);
        } catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraObject "+ex);
        }
    }
     
    public FedoraObject(DR dr, String id, String displayName,osid.shared.Type assetType)  throws osid.dr.DigitalRepositoryException,osid.shared.SharedException{
        this.dr = dr;
        this.displayName = displayName;
        this.assetType = assetType;
        this.pid = new PID(id);
        
        osid.dr.InfoStructureIterator i = ((FedoraObjectAssetType)assetType).getInfoStructures();
        while(i.hasNext()) {
            BehaviorInfoStructure behaviorInfoStructure = (BehaviorInfoStructure) i.next();
            infoStructures.add(behaviorInfoStructure);
            behaviorList.add(new Behavior(behaviorInfoStructure.getDisplayName(), this, behaviorInfoStructure));
        }
    }

    //this method is for viewing objects in vue.  will be gone soon.
    public String getDefaultViewURL() throws osid.dr.DigitalRepositoryException, osid.OsidException{ 
        if(assetType.getKeyword().equals("TUFTS_STD_IMAGE"))
            return getDR().getFedoraProperties().getProperty("url.fedora.get")+"/"+pid.getIdString()+"/demo:60/getStandard/";
        if(assetType.getKeyword().equals("XML_TO_HTMLDOC"));
            return getDR().getFedoraProperties().getProperty("url.fedora.get")+"/"+pid.getIdString()+"/demo:77/getDocument/";
    }
    // set-get methods.
    public  void setLocation(String location) {
        this.location = location;
    }
    
    public  String getLocation() {
        return this.location;
    }
    
    public  void setBehaviorList(java.util.Vector behaviorList) {
        this.behaviorList  = behaviorList;
    }
    
    public  java.util.Vector getBehaviorList() {
        return this.behaviorList;
    }
    public void setPID (String pid) throws osid.shared.SharedException{
        this.pid = new PID(pid) ;
    }
    public  PID getPID() throws osid.dr.DigitalRepositoryException {
        return this.pid;
    }
    public DR getDR() {
        return this.dr;
    }
    public void setDR(DR dr) {
        this.dr = dr;
    }
    
    /**     Add an Asset to this Asset.
     *     @param osid.shared.Id assetId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void addAsset(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
  
    }
    
    /**     Add the specified InfoStructure and all the related InfoRecords from the specified asset.
     *     @param osid.shared.Id assetId
     *     @param osid.shared.Id infoStructureId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void copyInfoStructure(osid.shared.Id assetId, osid.shared.Id infoStructureId) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Create a new Asset InfoRecord of the specified InfoStructure.  The implementation of this method sets the Id for the new object.
     *     @param osid.shared.Id infoStructureId
     *     @return InfoRecord
     *     @throws DigitalRepositoryException if there is a general failure or if the InfoStructure is unknown or not defined by this Asset's AssetType.
     */
    public InfoRecord createInfoRecord(osid.shared.Id infoStructureId) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Delete an InfoRecord.  If the specified InfoRecord has content that is inherited by other InfoRecords, those
     *     @param osid.shared.Id infoRecordId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void deleteInfoRecord(osid.shared.Id infoRecordId) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Description_getAssetTypes=Get the AssetType of this Asset.  AssetTypes are used to categorize Assets.
     *     @return osid.shared.Type
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Type getAssetType() throws osid.dr.DigitalRepositoryException {
        return assetType;
    }
    
    /**     Get all the Assets in this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public AssetIterator getAssets() throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Get an Asset's content.  This method can be a convenience if one is not interested in all the structure of the InfoRecords.
     *     @return java.io.Serializable
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public java.io.Serializable getContent() throws osid.dr.DigitalRepositoryException {
        return null;       
    }
    
    /**     Get the description for this Asset.
     *     @return String the name
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public String getDescription() throws osid.dr.DigitalRepositoryException {
        return description;
    }
    
    /**     Get the DigitalRepository in which this Asset resides.  This is set by the DigitalRepository's createAsset method.
     *     @return DigitalRepository
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Id getDigitalRepository() throws osid.dr.DigitalRepositoryException {
        return dr.getId();
    }
    
    /**     Get the name for this Asset.
     *     @return String the name
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public String getDisplayName() throws osid.dr.DigitalRepositoryException {
        return displayName;
    }
    
    /**     Get the Unique Id for this Asset.
     *     @return osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return pid;
    }
    
    /**     Get all the InfoRecords for this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoRecordIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoRecordIterator getInfoRecords() throws osid.dr.DigitalRepositoryException {
        return new BehaviorIterator(behaviorList);
    }
    
    /**     Get all the InfoRecords of the specified InfoStructure for this Asset.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @param osid.shared.Id infoStructureId
     *     @return InfoRecordIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoRecordIterator getInfoRecords(osid.shared.Id infoStructureId) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Get all the InfoStructures for this Asset.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public InfoStructureIterator getInfoStructures() throws osid.dr.DigitalRepositoryException {
        return (osid.dr.InfoStructureIterator) new BehaviorIterator(infoStructures);
    }
    
    /**     Add the specified InfoStructure and all the related InfoRecords from the specified asset.  The current and future content of the specified InfoRecord is synchronized automatically.
     *     @param osid.shared.Id assetId
     *     @param osid.shared.Id infoStructureId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void inheritInfoStructure(osid.shared.Id assetId, osid.shared.Id infoStructureId) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Remove an Asset to this Asset.  This method does not delete the Asset from the DigitalRepository.
     *     @param osid.shared.Id assetId
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void removeAsset(osid.shared.Id assetId, boolean includeChildren) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Update an Asset's content.
     *     @param java.io.Serializable
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateContent(java.io.Serializable content) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Update the description for this Asset.
     *     @param String description
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateDescription(String description) throws osid.dr.DigitalRepositoryException {
    }
    
    /**     Update the name for this Asset.
     *     @param String name
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateDisplayName(String displayName) throws osid.dr.DigitalRepositoryException {
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
    
    public AssetIterator getAssets(osid.shared.Type assetType) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    public InfoStructure getContentInfoStructure() throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    public osid.dr.AssetIterator getAssetsByType(osid.shared.Type type) throws osid.dr.DigitalRepositoryException {
       return  getAssets(type);
    }
    
    public java.util.Calendar getEffectiveDate() throws osid.dr.DigitalRepositoryException {
          throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public java.util.Calendar getExpirationDate() throws osid.dr.DigitalRepositoryException {
          throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public osid.dr.InfoField getInfoField(osid.shared.Id id) throws osid.dr.DigitalRepositoryException {
        Iterator i = behaviorList.iterator();
        while(i.hasNext()) {
            InfoRecord infoRecord = (InfoRecord)i.next();
            InfoFieldIterator infoFieldIterator = infoRecord.getInfoFields();
            while(infoFieldIterator.hasNext()){
                InfoField infoField = infoFieldIterator.next();
                try {
                    if(infoField.getId().isEqual(id)) 
                        return infoField;
                } catch (osid.shared.SharedException ex) {
                    throw new osid.dr.DigitalRepositoryException(ex.getMessage());
                }
            }
        }
        return null;
    }
    
    public osid.dr.InfoFieldIterator getInfoFieldByPart(osid.shared.Id id) throws osid.dr.DigitalRepositoryException {
         throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public java.io.Serializable getInfoFieldValue(osid.shared.Id id) throws osid.dr.DigitalRepositoryException {
         throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public osid.shared.SerializableObjectIterator getInfoFieldValueByPart(osid.shared.Id id) throws osid.dr.DigitalRepositoryException {
         throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public osid.dr.InfoRecord getInfoRecord(osid.shared.Id id) throws osid.dr.DigitalRepositoryException {
        Iterator i = behaviorList.iterator();
        while(i.hasNext()) {
            InfoRecord infoRecord = (InfoRecord)i.next();
            try {
                if(infoRecord.getId().isEqual(id)) 
                    return infoRecord;
            } catch (osid.shared.SharedException ex) {
                throw new osid.dr.DigitalRepositoryException(ex.getMessage());
            }
        }
        return null;
    }
    
    public osid.dr.InfoRecordIterator getInfoRecordsByInfoStructure(osid.shared.Id id) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    public void updateEffectiveDate(java.util.Calendar calendar) throws osid.dr.DigitalRepositoryException {
    }
    
    public void updateExpirationDate(java.util.Calendar calendar) throws osid.dr.DigitalRepositoryException {
    }
    
/**
   
 */
    
}
