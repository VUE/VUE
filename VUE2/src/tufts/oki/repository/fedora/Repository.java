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
 * DR.java
 *
 * Created on May 7, 2003, 2:03 PM
 */

/**
 *
 * @author  akumar03
 */


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
import fedora.client.ingest.AutoIngestor;


public class Repository implements org.osid.repository.Repository {
    public final boolean DEBUG = false;
    public static final String DC_NAMESPACE = "dc:";
    public static final String[] DC_FIELDS = {"title","creator","subject","date","type","format","identifier","collection","coverage"};
    
    private Preferences prefs = null;
    private String displayName = "";
    private String description = "";
    private URL address = null;
    private String userName = null;
    private String password = null;
    private String conf = null;
    private java.util.Vector recordStructures = new java.util.Vector();
    private java.util.Vector assetTypes = new java.util.Vector();
    private java.util.Vector searchTypes = new java.util.Vector();
    private java.util.Vector assets = new java.util.Vector();
    private org.osid.shared.Id id = null;
    private URL configuration = null;

    
    // this object stores the information to access soap.  These variables will not be required if Preferences becomes serializable
    private Properties fedoraProperties;
    /** Creates a new instance of Repository */
    public Repository(String conf,String id,String displayName,String description,URL address,String userName,String password)
        throws org.osid.repository.RepositoryException
    {
        /*
        System.out.println("Repository CONSTRUCTING["
                           + conf + ", "
                           + id + ", "
                           + displayName + ", "
                           + description + ", "
                           + address + ", "
                           + userName + ", "
                           + password + "] " + this);
        */
        try
        {
            this.id = new PID(id);
        }
        catch (Throwable t) { t.printStackTrace(); }
        this.displayName = displayName;
        this.description = description;
        this.address = address;
        this.userName = userName;
        this.password = password;
        this.conf = conf;
        this.configuration = getResource(conf);
        
        setFedoraProperties(configuration);
        loadFedoraObjectAssetTypes();
        //setFedoraProperties(FedoraUtils.CONF);
        searchTypes.add(new SimpleSearchType());
        searchTypes.add(new AdvancedSearchType());
        searchTypes.add(new org.osid.types.mit.KeywordSearchType());
        //loadAssetTypes();
    }
    
    /** sets a soap call to perform all digital repository operations
     * @throws RepositoryException if Soap call can't be made
     */
    
    public void setFedoraProperties(Properties fedoraProperties) {
        this.fedoraProperties = fedoraProperties;
    }
    
    public void setFedoraProperties(java.net.URL conf) {
        String url = address.getProtocol()+"://"+address.getHost()+":"+address.getPort()+"/"+address.getFile();
        //System.out.println("FEDORA Address = "+ url);
        fedoraProperties = new Properties();
        try {
            //System.out.println("Fedora Properties " + conf);
            prefs = FedoraUtils.getPreferences(this);
            fedoraProperties.setProperty("url.fedora.api", prefs.get("url.fedora.api","http://www.fedora.info/definitions/1/0/api/"));
            fedoraProperties.setProperty("url.fedora.type", prefs.get("url.fedora.type", "http://www.fedora.info/definitions/1/0/types/"));

            fedoraProperties.setProperty("url.fedora.soap.access",url+ prefs.get("url.fedora.soap.access", "access/soap"));
            fedoraProperties.setProperty("url.fedora.get", url+prefs.get("url.fedora.get", "get/"));
            fedoraProperties.setProperty("url.seastar.fedora.get", "http://seastar.lib.tufts.edu:8080/fedora/get/");
            fedoraProperties.setProperty("fedora.types", prefs.get("fedora.types","TUFTS_STD_IMAGE,XML_TO_HTMLDOC,TUFTS_BINARY_FILE,TUFTS_VUE_CONCEPT_MAP,UVA_EAD_FINDING_AID,UVA_STD_IMAGE,UVA_MRSID_IMAGE,SIMPLE_DOC,MassIngest"));
        } catch (Exception ex) { System.out.println("Unable to load fedora Properties"+ex);}
        
    }
    
    private void loadFedoraObjectAssetTypes() {
        try {
            Vector fedoraTypesVector = FedoraUtils.stringToVector(fedoraProperties.getProperty("fedora.types"));
            Iterator i =fedoraTypesVector.iterator();
            while(i.hasNext()) {
                createFedoraObjectAssetType((String)i.next());
            }
        } catch (Throwable t) { System.out.println("Unable to load fedora types"+t.getMessage());}
    }
    public Properties getFedoraProperties() {
        return fedoraProperties;
    }
    
    public URL getConfiguration() {
        return configuration;
    }
    
 
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
        return new RecordStructureIterator(new java.util.Vector());
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
        org.osid.repository.AssetIterator mAssetIterator = FedoraSoapFactory.advancedSearch(this,searchCriteria);
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
        //System.out.println("SEARCHING FEDORA = "+ this.fedoraProperties.getProperty("url.fedora.soap.access"));

        SearchCriteria lSearchCriteria = null;
        
        org.osid.shared.Type keywordType = new org.osid.types.mit.KeywordSearchType();
        if ( (searchCriteria instanceof String) && (searchType.isEqual(keywordType)) )
        {
            lSearchCriteria = new SearchCriteria();
            lSearchCriteria.setKeywords((String)searchCriteria);
            lSearchCriteria.setMaxReturns("10");
            lSearchCriteria.setSearchOperation(SearchCriteria.FIND_OBJECTS);
            lSearchCriteria.setResults(0);
            return FedoraSoapFactory.search(this,lSearchCriteria);
        }
        else if (searchCriteria instanceof SearchCriteria)
        {
            lSearchCriteria = (SearchCriteria)searchCriteria;
            if(searchType.isEqual(new SimpleSearchType()))
            {
                return FedoraSoapFactory.search(this,lSearchCriteria);
            } 
            else if(searchType.isEqual(new AdvancedSearchType())) 
            {
                return FedoraSoapFactory.advancedSearch(this,lSearchCriteria);
            }
            else
            {
                throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.UNKNOWN_TYPE);
            }
        }
        else
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.UNKNOWN_TYPE);
        }
    }
    
    public org.osid.shared.Type getType() throws org.osid.repository.RepositoryException {
        return new Type("tufts.edu","repository","fedoraImage");
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
    
    public  org.osid.shared.Id ingest(String fileName,String templateFileName,String fileType, File file,Properties properties) throws org.osid.repository.RepositoryException, java.net.SocketException,java.io.IOException,org.osid.shared.SharedException,javax.xml.rpc.ServiceException{
        long sTime = System.currentTimeMillis();
        if(DEBUG) System.out.println("INGESTING FILE TO FEDORA:fileName ="+fileName+"fileType ="+fileType+"t = 0");
        // this part transfers file to a ftp server.  this is required since the content management part of fedora server needs object to be on web server
        String host = FedoraUtils.getFedoraProperty(this,"admin.ftp.address");
        String url = FedoraUtils.getFedoraProperty(this,"admin.ftp.url");
        int port = Integer.parseInt(FedoraUtils.getFedoraProperty(this,"admin.ftp.port"));
        String userName = FedoraUtils.getFedoraProperty(this,"admin.ftp.username");
        String password = FedoraUtils.getFedoraProperty(this,"admin.ftp.password");
        String directory = FedoraUtils.getFedoraProperty(this,"admin.ftp.directory");
        FTPClient client = new FTPClient();
        client.connect(host,port);
        client.login(userName,password);
        client.changeWorkingDirectory(directory);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.storeFile(fileName,new FileInputStream(file));
        client.logout();
        client.disconnect();
        if(DEBUG) System.out.println("INGESTING FILE TO FEDORA: Writting to FTP Server:"+(System.currentTimeMillis()-sTime));
        fileName = url+fileName;
        // this part does the creation of METSFile
        int BUFFER_SIZE = 10240;
        StringBuffer sb = new StringBuffer();
        String s = new String();
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(new File(getResource(templateFileName).getFile().replaceAll("%20"," "))));
        //FileInputStream fis = new FileInputStream(new File(templateFileName));
        //DataInputStream in = new DataInputStream(fis);
        byte[] buf = new byte[BUFFER_SIZE];
        int ch;
        int len;
        while((len =fis.read(buf)) > 0) {
            s = s+ new String(buf);
        }
        fis.close();
        if(DEBUG) System.out.println("INGESTING FILE TO FEDORA: Read Mets File:"+(System.currentTimeMillis()-sTime));
        
        //in.close();
        //  s = sb.toString();
        //String r =  s.replaceAll("%file.location%", fileName).trim();
        String r = updateMetadata(s, fileName,file.getName(),fileType,properties);
        if(DEBUG) System.out.println("INGESTING FILE TO FEDORA: Resplaced Metadata:"+(System.currentTimeMillis()-sTime));
        
        //writing the to outputfile
        File METSfile = File.createTempFile("vueMETSMap",".xml");
        FileOutputStream fos = new FileOutputStream(METSfile);
        fos.write(r.getBytes());
        fos.close();
        
        AutoIngestor a = new AutoIngestor(address.getHost(), address.getPort(),FedoraUtils.getFedoraProperty(this,"admin.fedora.username"),FedoraUtils.getFedoraProperty(this,"admin.fedora.username"));
        String pid = a.ingestAndCommit(new FileInputStream(METSfile),"Test Ingest");
        if(DEBUG) System.out.println("INGESTING FILE TO FEDORA: Ingest complete:"+(System.currentTimeMillis()-sTime));
        
        System.out.println(" METSfile= " + METSfile.getPath()+" PID = "+pid);
        return new PID(pid);
    }
    
    private String updateMetadata(String s,String fileLocation, String fileTitle, String fileType,Properties dcFields) {
        Calendar calendar = new GregorianCalendar();
        //String created = calendar.get(Calendar.YEAR)+"-"+calendar.get(Calendar.MONTH)+"-"+calendar.get(Calendar.DAY_OF_MONTH);
        //created += "T"+calendar.get(Calendar.HOUR)+":"+calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND);
        java.text.SimpleDateFormat date = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String created = date.format(calendar.getTime());
        String dcMetadata;
        s = s.replaceAll("%file.location%", fileLocation).trim();
        s = s.replaceAll("%file.title%", fileTitle);
        s = s.replaceAll("%file.type%",fileType);
        s = s.replaceAll("%file.created%", created);
        s = s.replaceAll("%dc.Metadata%", getMetadataString(dcFields));
        return s;
        
    }
    
    private java.net.URL getResource(String name) {
        java.net.URL url = null;
        java.io.File f = new java.io.File(name);
        if (f.exists()) {
            try {
                url = f.toURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (url == null)
            url = getClass().getResource(name);
        //System.out.println("fedora.conf = "+url.getFile());
        return url;
    }
    
    public static boolean isSupportedMetadataField(String field){
        for(int i=0;i<DC_FIELDS.length;i++) {
            if(DC_FIELDS[i].equalsIgnoreCase(field))
                return true;
        }
        return false;
    }
    public static String getMetadataString(Properties dcFields) {
        String metadata = "";
        Enumeration e = dcFields.keys();
        while(e.hasMoreElements()) {
            String field = (String)e.nextElement();
            if(isSupportedMetadataField(field))
                metadata += "<"+DC_NAMESPACE+field+">"+dcFields.getProperty(field)+"</"+DC_NAMESPACE+field+">";
        }
        return metadata;
    }
    
    public String getAddress() {
        return this.address.getHost();
    }
    public void setAddress(String address) throws java.net.MalformedURLException {
        
        this.address = new URL("http",address,8080,"fedora/");
        
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
    public Preferences getPrefernces() {
        return this.prefs;
    }
    public void setConf(Preferences prefs) {
        this.prefs = prefs;
    }
}


