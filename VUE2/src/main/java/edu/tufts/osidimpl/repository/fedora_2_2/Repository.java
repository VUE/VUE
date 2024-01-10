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
 * Repository.java
 *
 * Created on April 7, 2006, 2:03 PM
 */

/**
 *
 * @author  akumar03
 */
package  edu.tufts.osidimpl.repository.fedora_2_2;


import org.osid.repository.*;
import tufts.oki.shared.TypeIterator;


import java.util.prefs.Preferences;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.JOptionPane;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;
import org.xml.sax.InputSource;

// these classses are required for soap implementation of
import javax.xml.namespace.QName;

import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;

//axis files
import org.apache.axis.encoding.ser.*;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.client.Service;
//import org.apache.axis.client.Call;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException ;

// for FTP
import org.apache.commons.net.ftp.*;

// APIM
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;


public class Repository implements org.osid.repository.Repository {
    public final boolean DEBUG = false;
    public static final String DC_NAMESPACE = "dc:";
    public static final String[] DC_FIELDS = {"title","creator","subject","date","type","format","identifier","collection","coverage"};
    public static final String FEDORA_PROTOCOL = "http://";
    public static final String FEDORA_URL = "/fedora/";
    public static final String BDEF="fedora:BDEF";
    public static final String BMECH="fedora:BMECH";
    private Preferences prefs = null;
    private String displayName = "";
    private String description = "";
    private String address = null;
    private int port = 8080; // default port for fedora
    private String userName = null;
    private String password = null;
    private String conf = null;
    private java.util.Vector recordStructures = new java.util.Vector();
    private java.util.Vector assetTypes = new java.util.Vector();
    private java.util.Vector searchTypes = new java.util.Vector();
    private java.util.Vector assets = new java.util.Vector();
    private java.util.Properties configuration;
    private org.osid.shared.Id id = null;
    // private URL configuration = null;
	private org.osid.shared.Type keywordSearchType = new Type("mit.edu","search","keyword");
	private org.osid.shared.Type multiFieldSearchType = new Type("mit.edu","search","multiField");      
    private org.osid.shared.Type repositoryType = new Type("tufts.edu","repository","fedora_2_2");
    
    // this object stores the information to access soap.  These variables will not be required if Preferences becomes serializable
    private Properties fedoraProperties;
    /** Creates a new instance of Repository */

      public Repository(String conf,
					  String id,
					  String displayName,
					  String address,
					  String port,
					  String userName,
					  String password)
    throws org.osid.repository.RepositoryException {
        try {
            this.id = new PID(id);
			this.displayName = displayName;
			this.description = description;
			setAddress(address);
			this.port = new Integer(port).intValue();
			this.userName = userName;
			this.password = password;
			this.conf = conf;
			setFedoraProperties();
			loadFedoraObjectAssetTypes();
			searchTypes.add(keywordSearchType);
			searchTypes.add(multiFieldSearchType);
        } catch (Throwable t) { t.printStackTrace(); }
    }
    
     // This constructor shall eventually replace the above constructor. 
     //This is robust and has access to all the configuration informaiton from osid 
          public Repository(Properties configuration, String id) throws org.osid.repository.RepositoryException {
        try {
            this.id = new PID(id);
            
            	String  displayName = configuration.getProperty("fedora22DisplayName");
                if (displayName != null) this.displayName = displayName;
                String address = configuration.getProperty("fedora22Address");
                if(address != null) setAddress(address);
		String port = configuration.getProperty("fedora22Port");
                if(port !=null) this.port = new Integer(port).intValue();
                String userName = configuration.getProperty("fedora22UserName");
                if(userName != null ) this.userName = userName;
                String  password = configuration.getProperty("fedora22Password");
		if(password != null) this.password = password;	
                setFedoraProperties();
                loadFedoraObjectAssetTypes();
                searchTypes.add(keywordSearchType);
                searchTypes.add(multiFieldSearchType);
                this.configuration   = configuration;
        } catch (Throwable t) { t.printStackTrace(); }
    }
    
    /** sets a soap call to perform all digital repository operations
     * @throws RepositoryException if Soap call can't be made
     */
    
    public void setFedoraProperties(Properties fedoraProperties) {
        this.fedoraProperties = fedoraProperties;
    }
    
    public void setFedoraProperties() {
        String url = FEDORA_PROTOCOL+this.address+":"+this.port+FEDORA_URL;
        fedoraProperties = new Properties();
        try {
           // prefs = FedoraUtils.getPreferences(this);
            fedoraProperties.setProperty("url.fedora.api", "http://www.fedora.info/definitions/1/0/api/");
            fedoraProperties.setProperty("url.fedora.type",  "http://www.fedora.info/definitions/1/0/types/");
            fedoraProperties.setProperty("url.fedora.soap.access",url+"access/soap");
            fedoraProperties.setProperty("url.fedora.get", url+"get/");
            fedoraProperties.setProperty("url.seastar.fedora.get", "http://seastar.lib.tufts.edu:8080/fedora/get/");
            fedoraProperties.setProperty("fedora.types","TUFTS_STD_IMAGE,XML_TO_HTMLDOC,TUFTS_BINARY_FILE,TUFTS_VUE_CONCEPT_MAP,UVA_EAD_FINDING_AID,UVA_STD_IMAGE,UVA_MRSID_IMAGE,SIMPLE_DOC,MassIngest");
            fedoraProperties.setProperty("ImageRecordStructureId","edu.mit.image.recordStructureId");
            fedoraProperties.setProperty("VUEDefaultViewInfoPartId","edu.tufts.defaultView.partStructureId");
            fedoraProperties.setProperty("ThumbnailPartStructureId","mit.edu.thumbnail.partStructureId");
            fedoraProperties.setProperty("URLPartStructureId","mit.edu.partStructureId");
            fedoraProperties.setProperty("dissemination.dc","bdef:TuftsMetadata/getDublinCore/");
            fedoraProperties.setProperty("type.image", "tufts/image/archival");
            fedoraProperties.setProperty("assetDef.fullView" , "bdef:AssetDef/getFullView");
            
            
        } catch (Exception ex) { ex.printStackTrace(); System.out.println("Unable to load fedora Properties"+ex);}
        
    }
    
    private void loadFedoraObjectAssetTypes() {
        try {
//			System.out.println("fedora types " + fedoraProperties.getProperty("fedora.types"));
            Vector fedoraTypesVector = FedoraUtils.stringToVector(fedoraProperties.getProperty("fedora.types"));
            Iterator i =fedoraTypesVector.iterator();
            while(i.hasNext()) {
                createFedoraObjectAssetType((String)i.next());
            }
        } catch (Throwable t) { 
			t.printStackTrace();
			System.out.println("Unable to load fedora types"+t.getMessage());
		}
    }
    public Properties getFedoraProperties() {
        return fedoraProperties;
    }
    
    //TODO: remove this method completely in future
    
    /**To create AssetTypes that don't exist when repository is loaded. OKI NEEDS to add such a feature
     *@ param String type
     *@ return FedoraObjectAssetType
     *@throws org.osid.repository.RepositoryException
     */
    
    public FedoraObjectAssetType createFedoraObjectAssetType(String type) throws org.osid.repository.RepositoryException {
        java.util.Iterator i = assetTypes.iterator();
        while(i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if(fedoraObjectAssetType.getType().equals(type))
                return fedoraObjectAssetType;
        }
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this,type);
        org.osid.repository.RecordStructureIterator iter =  fedoraObjectAssetType.getRecordStructures();
        while(iter.hasNextRecordStructure()) {
            org.osid.repository.RecordStructure recordStructure = (org.osid.repository.RecordStructure)iter.nextRecordStructure();
            if(recordStructures.indexOf(recordStructure) < 0)
                recordStructures.add(recordStructure);
        }
        assetTypes.add(fedoraObjectAssetType);
        return fedoraObjectAssetType;
    }
    
    /** AssetTypes are loaded from the configuration file. In future versions these will be loaded directly from FEDORA.
     *  OKI Team recommends having  an object in digital repository that maintains this information.
     * @ throws RepositoryException
     */
    
    private void loadAssetTypes() throws  org.osid.repository.RepositoryException {
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this,"TUFTS_STD_IMAGE");
    }
    
    public FedoraObjectAssetType getAssetType(String type) throws org.osid.repository.RepositoryException {
        java.util.Iterator i = assetTypes.iterator();
        while(i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if(fedoraObjectAssetType.getType().equals(type))
                return fedoraObjectAssetType;
        }
        return createFedoraObjectAssetType(type);
    }
    
    public boolean isFedoraObjectAssetTypeSupported(String type) {
        java.util.Iterator i = assetTypes.iterator();
        while(i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if(fedoraObjectAssetType.getType().equals(type))
                return true;
        }
        return false;
    }
    
    
    
    /**     Create a new Asset of this AssetType to this Repository.  The implementation of this method sets the Id for the new object.
     *     @return Asset
     *     @throws RepositoryException if there is a general failure or if the Type is unknown
     */
    public org.osid.repository.Asset createAsset(String displayName, String description, org.osid.shared.Type assetType) throws org.osid.repository.RepositoryException{
        if(!assetTypes.contains(assetType))
            assetTypes.add(assetType);
        try {
            org.osid.repository.Asset obj = new Asset(this,displayName,description,assetType);
            assets.add(obj);
            return obj;
        } catch(org.osid.shared.SharedException ex) {
            throw new org.osid.repository.RepositoryException("DR.createAsset"+ex.getMessage());
        }
    }
    
    /**     Delete an Asset from this Repository.
     *     @param org.osid.shared.Id
     *     @throws RepositoryException if there is a general failure  or if the object has not been created
     */
    public void deleteAsset(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.RecordStructureIterator getRecordStructuresByType(org.osid.shared.Type recordStructureType) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    
    /**     Get all the AssetTypes in this Repository.  AssetTypes are used to categorize Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return org.osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.TypeIterator getAssetTypes() throws org.osid.repository.RepositoryException {
        // this method needs an implementation of TypeIterator which has not yet been implemented
        return new tufts.oki.shared2.TypeIterator(assetTypes);
    }
    
    /**     Get all the Assets in this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    
    
    public org.osid.repository.AssetIterator getAssets() throws org.osid.repository.RepositoryException {
        Vector assetVector = new Vector();
        String assetId = "tufts:";
        String location = null;
        try {
            for(int i=1;i<=10;i++) {
                // location = getObject(assetId+i);
                // FedoraObject obj = createObject(location);
                org.osid.repository.Asset asset = new Asset(new PID(assetId+i),this);
                assetVector.add(asset);
            }
        } catch(Throwable t) {
            throw new RepositoryException(t.getMessage());
        }
        return (org.osid.repository.AssetIterator) new AssetIterator(assetVector);
    }
    
    /**     Get all the Assets of the specified AssetType in this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure   or if the Type is unknown
     */
    
    
    /**     Get the description for this Repository.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDescription() throws org.osid.repository.RepositoryException {
        return this.description;
    }
    
    /**     Get the name for this Repository.
     *     @return String the name
     *     @throws RepositoryException if there is a general failure
     */
    public String getDisplayName() throws org.osid.repository.RepositoryException {
        return displayName;
    }
    
    /**     Get the Unique Id for this Repository.
     *     @return org.osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.Id getId() throws org.osid.repository.RepositoryException {
        return id;
    }
    
    /**     Get all the InfoStructures in this Repository.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.repository.RecordStructureIterator getRecordStructures() throws org.osid.repository.RepositoryException {
        return (org.osid.repository.RecordStructureIterator) new RecordStructureIterator(recordStructures);
    }
    
    /**     Get the InfoStructures that this AssetType must support.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.repository.RecordStructureIterator getMandatoryRecordStructures(org.osid.shared.Type assetType) throws org.osid.repository.RepositoryException {
		java.util.Vector v = new java.util.Vector();
		v.addElement(new ImageRecordStructure(this));
        return new RecordStructureIterator(v);
    }
    
    /**     Get all the SearchTypes supported by this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return org.osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.TypeIterator getSearchTypes() throws org.osid.repository.RepositoryException {
        return new tufts.oki.shared2.TypeIterator(searchTypes);
    }
    
    /**     Get the the StatusTypes of this Asset.
     *     @return org.osid.shared.Type
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.Type getStatus(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Get all the StatusTypes supported by this Repository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return org.osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws RepositoryException if there is a general failure
     */
    public org.osid.shared.TypeIterator getStatusTypes() throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Update the description for this Repository.
     *     @param String description
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDescription(String description) throws org.osid.repository.RepositoryException {
        this.description = description;
    }
    
    /**     Update the "tufts/dr/fedora/temp/"name for this Repository.
     *     @param String name
     *     @throws RepositoryException if there is a general failure
     */
    public void updateDisplayName(String displayName) throws org.osid.repository.RepositoryException {
        this.displayName = displayName;
    }
    
    /**     Set the Asset's status Type accordingly and relax validation checking when creating InfoRecords and InfoFields or updating InfoField's values.
     *     @param org.osid.shared.Id
     *     @return boolean
     *     @throws RepositoryException if there is a general failure
     */
    public void invalidateAsset(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    /**     Validate all the InfoRecords for an Asset and set its status Type accordingly.  If the Asset is valid, return true; otherwise return false.  The implementation may throw an Exception for any validation failures and use the Exception's message to identify specific causes.
     *     @param org.osid.shared.Id
     *     @return boolean
     *     @throws RepositoryException if there is a general failure
     */
    public boolean validateAsset(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.Id copyAsset(org.osid.repository.Asset asset) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        Condition[] condition = new Condition[1];
        condition[0] = new Condition();
        condition[0].setProperty("pid");
        condition[0].setOperator(ComparisonOperator.eq);
        
        try {
            System.out.println("Searching for object ="+assetId.getIdString());
            condition[0].setValue(assetId.getIdString());
        } catch(org.osid.shared.SharedException ex) {
            throw new org.osid.repository.RepositoryException(ex.getMessage());
        }
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setConditions(condition);
        searchCriteria.setMaxReturns("1");
        org.osid.repository.AssetIterator mAssetIterator = FedoraRESTSearchAdapter.advancedSearch(this,searchCriteria);
        if(mAssetIterator.hasNextAsset())
            return  mAssetIterator.nextAsset();
        else
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.UNKNOWN_ID);
        
    }
    
    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId, long date) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.PropertiesIterator getProperties() throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.Properties getPropertiesByType(org.osid.shared.Type propertiesType) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.TypeIterator getPropertyTypes() throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.LongValueIterator getAssetDates(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.AssetIterator getAssets(java.io.Serializable searchCriteria, org.osid.shared.Type searchType) throws org.osid.repository.RepositoryException {
        if ( (searchCriteria instanceof String) && 
			 (searchType.isEqual(keywordSearchType) || searchType.isEqual(multiFieldSearchType)) ) {
			return new AssetIterator(this, searchCriteria, searchType);
		} else {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.UNKNOWN_TYPE);
		}		
    }
    
    public org.osid.shared.Type getType() throws org.osid.repository.RepositoryException {
        return  repositoryType;
    }
    
    public org.osid.repository.Asset getAssetByDate(org.osid.shared.Id id, long date) throws org.osid.repository.RepositoryException {
        return getAsset(id,date);
    }
    
    public org.osid.repository.AssetIterator getAssetsBySearch(java.io.Serializable serializable, org.osid.shared.Type type, org.osid.shared.Properties properties) throws org.osid.repository.RepositoryException {
        return getAssets(serializable, type);
    }
    
    public org.osid.repository.AssetIterator getAssetsByType(org.osid.shared.Type type) throws org.osid.repository.RepositoryException {
        return getAssets();
    }
    
    public boolean supportsUpdate() throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public boolean supportsVersioning() throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getUserName() {
        return this.userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return this.password;
    }
    public void setPassword() {
        this.password = password;
    }
    public String getConf() {
        return this.conf;
    }
    public void setConf(String conf) {
        this.conf = conf;
    }
    public int getPort() {
        return this.port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    
    public Properties getConfiguration() {
        return this.configuration;
    }
}


