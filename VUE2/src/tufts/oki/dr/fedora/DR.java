package tufts.oki.dr.fedora;

/*
 * DR.java
 *
 * Created on May 7, 2003, 2:03 PM
 */

/**
 *
 * @author  akumar03
 */


import osid.dr.*;
import tufts.oki.shared.TypeIterator;


import java.util.Vector;
import java.util.Properties;
import java.util.Iterator;
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


public class DR implements osid.dr.DigitalRepository {
    
    
    // using the vue.conf file.  This is the default file. the file name will be set in the constructor in future.
    private String displayName;
    private String description;
    private URL address;
    private String userName;
    private String password;
    private String conf;
    private java.util.Vector infoStructures = new java.util.Vector();
    private java.util.Vector assetTypes = new java.util.Vector();
    private java.util.Vector searchTypes = new java.util.Vector();
    private java.util.Vector assets = new java.util.Vector();
    private osid.shared.Id id;
    private URL configuration;
    // this object stores the information to access soap.  These variables will not be required if Preferences becomes serializable
    private Properties fedoraProperties;
    /** Creates a new instance of DR */
    public DR(String conf,String id,String displayName,String description,URL address,String userName,String password) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {
        this.id = new PID(id);
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
        searchTypes.add(new SearchType("Search"));
        searchTypes.add(new SearchType("Advanced Search"));
        //loadAssetTypes();
    }
    
    /** sets a soap call to perform all digital repository operations
     * @throws igitalRepositoryException if Soap call can't be made
     */
   
    public void setFedoraProperties(Properties fedoraProperties) {
        this.fedoraProperties = fedoraProperties;
    }
   
    public void setFedoraProperties(java.net.URL conf) {
        String url = address.getProtocol()+"://"+address.getHost()+":"+address.getPort()+"/"+address.getFile()+"/";
        fedoraProperties = new Properties();
        java.util.prefs.Preferences   prefs = java.util.prefs.Preferences.userRoot().node("/");
        try {
            System.out.println("Fedora Properties"+conf.getPath());
            FileInputStream fis = new FileInputStream(conf.getPath());
            prefs.importPreferences(fis);
            fedoraProperties.setProperty("url.fedora.api", prefs.get("url.fedora.api",""));
            fedoraProperties.setProperty("url.fedora.type", prefs.get("url.fedora.type", ""));
            fedoraProperties.setProperty("url.fedora.soap.access",url+ prefs.get("url.fedora.soap.access", ""));
            fedoraProperties.setProperty("url.fedora.get", url+prefs.get("url.fedora.get", ""));
            fedoraProperties.setProperty("fedora.types", prefs.get("fedora.types",""));
            fis.close();
            System.out.println("Fedora soap access = "+fedoraProperties.getProperty("url.fedora.soap.access"));
        } catch (Exception ex) { System.out.println("Unable to load fedora Properties"+ex);}
 
    }
    private void loadFedoraObjectAssetTypes() {
        try {
             Vector fedoraTypesVector = FedoraUtils.stringToVector(fedoraProperties.getProperty("fedora.types"));
             Iterator i =fedoraTypesVector.iterator();
             while(i.hasNext()) {
                 createFedoraObjectAssetType((String)i.next());
             }
        } catch (Exception ex) { System.out.println("Unable to load fedora types"+ex);}
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
     *@throws osid.dr.DigitalRepositoryException
     */
 
    public FedoraObjectAssetType createFedoraObjectAssetType(String type) throws osid.dr.DigitalRepositoryException {
        java.util.Iterator i = assetTypes.iterator();
        while(i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if(fedoraObjectAssetType.getType().equals(type))
                return fedoraObjectAssetType;
        }
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this,type);
        osid.dr.InfoStructureIterator iter =  fedoraObjectAssetType.getInfoStructures();
        while(iter.hasNext())
            infoStructures.add(iter.next());
        assetTypes.add(fedoraObjectAssetType);
        return fedoraObjectAssetType;
    }
    
    /** AssetTypes are loaded from the configuration file. In future versions these will be loaded directly from FEDORA.
     *  OKI Team recommends having  an object in digital repository that maintains this information.
     * @ throws DigitalRepositoryException 
     */
    
    private void loadAssetTypes() throws  osid.dr.DigitalRepositoryException {
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this,"TUFTS_STD_IMAGE");
        osid.dr.InfoStructureIterator i =  fedoraObjectAssetType.getInfoStructures();
        while(i.hasNext())
            infoStructures.add(i.next());
        assetTypes.add(fedoraObjectAssetType);
    }
    
    public FedoraObjectAssetType getAssetType(String type) throws osid.dr.DigitalRepositoryException {
        java.util.Iterator i = assetTypes.iterator();
        while(i.hasNext()) {
            FedoraObjectAssetType fedoraObjectAssetType = (FedoraObjectAssetType) i.next();
            if(fedoraObjectAssetType.getType().equals(type))
                return fedoraObjectAssetType;
        }
        throw new DigitalRepositoryException("DR.getAssetType "+type+"doesn't exist");
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
        
  
    
    /**     Create a new Asset of this AssetType to this DigitalRepository.  The implementation of this method sets the Id for the new object.
     *     @return Asset
     *     @throws DigitalRepositoryException if there is a general failure or if the Type is unknown
     */
    public Asset createAsset(String displayName, String description, osid.shared.Type assetType) throws osid.dr.DigitalRepositoryException{
        if(!assetTypes.contains(assetType))
            assetTypes.add(assetType);
        try {
            FedoraObject obj = new FedoraObject(this,displayName,description,assetType);
            assets.add(obj);
            return obj;
        } catch(osid.shared.SharedException ex) {
            throw new osid.dr.DigitalRepositoryException("DR.createAsset"+ex.getMessage());
        }
    }
    
    /**     Delete an Asset from this DigitalRepository.
     *     @param osid.shared.Id
     *     @throws DigitalRepositoryException if there is a general failure  or if the object has not been created
     */
    public void deleteAsset(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
        
    }
    
    /**     Get all the AssetTypes in this DigitalRepository.  AssetTypes are used to categorize Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.TypeIterator getAssetTypes() throws osid.dr.DigitalRepositoryException {
        // this method needs an implementation of TypeIterator which has not yet been implemented
      return new TypeIterator(assetTypes);
    }
    
    /**     Get all the Assets in this DigitalRepository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    
    
    public AssetIterator getAssets() throws osid.dr.DigitalRepositoryException {
        Vector assetVector = new Vector();
        String assetId = "tufts:";
        String location = null;
        try {
        for(int i=1;i<=10;i++) {
           // location = getObject(assetId+i);
          // FedoraObject obj = createObject(location);
            FedoraObject obj = new FedoraObject(new PID(assetId+i),this);
            assetVector.add(obj);
        }
        } catch(Exception ex) {
            throw new DigitalRepositoryException(ex.getMessage());
        }
        return (AssetIterator) new FedoraObjectIterator(assetVector);
    }
    
    /**     Get all the Assets of the specified AssetType in this DigitalRepository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return AssetIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure   or if the Type is unknown
     */
  
    
    /**     Get the description for this DigitalRepository.
     *     @return String the name
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public String getDescription() throws osid.dr.DigitalRepositoryException {
        return this.description;
    }
    
    /**     Get the name for this DigitalRepository.
     *     @return String the name
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public String getDisplayName() throws osid.dr.DigitalRepositoryException {
        return displayName;
    }
    
    /**     Get the Unique Id for this DigitalRepository.
     *     @return osid.shared.Id Unique Id this is usually set by a create method's implementation
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return id;
    }
    
    /**     Get all the InfoStructures in this DigitalRepository.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.dr.InfoStructureIterator getInfoStructures() throws osid.dr.DigitalRepositoryException {
        return (osid.dr.InfoStructureIterator) new InfoStructureIterator(infoStructures);
    }
    
    /**     Get the InfoStructures that this AssetType must support.  InfoStructures are used to categorize information about Assets.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return InfoStructureIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.dr.InfoStructureIterator getMandatoryInfoStructures(osid.shared.Type assetType) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    /**     Get all the SearchTypes supported by this DigitalRepository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.TypeIterator getSearchTypes() throws osid.dr.DigitalRepositoryException {
       return new TypeIterator(searchTypes);
    }
    
    /**     Get the the StatusTypes of this Asset.
     *     @return osid.shared.Type
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.Type getStatus(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
         throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    /**     Get all the StatusTypes supported by this DigitalRepository.  Iterators return a set, one at a time.  The Iterator's hasNext method returns true if there are additional objects available; false otherwise.  The Iterator's next method returns the next object.
     *     @return osid.shared.TypeIterator  The order of the objects returned by the Iterator is not guaranteed.
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public osid.shared.TypeIterator getStatusTypes() throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    /**     Update the description for this DigitalRepository.
     *     @param String description
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateDescription(String description) throws osid.dr.DigitalRepositoryException {
        this.description = description;
    }
    
    /**     Update the "tufts/dr/fedora/temp/"name for this DigitalRepository.
     *     @param String name
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void updateDisplayName(String displayName) throws osid.dr.DigitalRepositoryException {
        this.displayName = displayName;
    }
    
    /**     Set the Asset's status Type accordingly and relax validation checking when creating InfoRecords and InfoFields or updating InfoField's values.
     *     @param osid.shared.Id
     *     @return boolean
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public void invalidateAsset(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
         throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    /**     Validate all the InfoRecords for an Asset and set its status Type accordingly.  If the Asset is valid, return true; otherwise return false.  The implementation may throw an Exception for any validation failures and use the Exception's message to identify specific causes.
     *     @param osid.shared.Id
     *     @return boolean
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public boolean validateAsset(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
         throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public osid.shared.Id copyAsset(osid.dr.Asset asset) throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public Asset getAsset(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
        Condition[] condition = new Condition[1];
        try {
            condition[0].setValue(assetId.getIdString());
        } catch(osid.shared.SharedException ex) {
            throw new osid.dr.DigitalRepositoryException(ex.getMessage());
        }
        condition[0].setProperty("pid");
        condition[0].setOperator(ComparisonOperator.eq);
        AssetIterator mAssetIterator = FedoraSoapFactory.advancedSearch(this,condition,"1");
        if(mAssetIterator.hasNext()) 
            return  mAssetIterator.next();
        else 
             throw new osid.dr.DigitalRepositoryException("Object not found");
        
    }
    
    public Asset getAsset(osid.shared.Id assetId, java.util.Calendar date) throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public osid.shared.CalendarIterator getAssetDates(osid.shared.Id assetId) throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public AssetIterator getAssets(java.io.Serializable searchCriteria, osid.shared.Type searchType) throws osid.dr.DigitalRepositoryException {
        SearchCriteria lSearchCriteria = (SearchCriteria)searchCriteria;
        if(searchType.getKeyword().equals("Search")) {
              return FedoraSoapFactory.search(this,lSearchCriteria.getKeywords(),lSearchCriteria.getMaxReturns());
        } else if(searchType.getKeyword().equals("Advanced Search")) {
            return FedoraSoapFactory.advancedSearch(this,lSearchCriteria.getConditions(),lSearchCriteria.getMaxReturns());
        }else {
           throw new osid.dr.DigitalRepositoryException("Search Type Not Supported");
        }
    }
    
    public osid.shared.Type getType() throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException("Not Implemented");
    }
    
    public osid.dr.Asset getAssetByDate(osid.shared.Id id, java.util.Calendar calendar) throws osid.dr.DigitalRepositoryException {
        return getAsset(id,calendar);
    }
    
    public osid.dr.AssetIterator getAssetsBySearch(java.io.Serializable serializable, osid.shared.Type type) throws osid.dr.DigitalRepositoryException {
        return getAssets(serializable, type);
    }
    
    public osid.dr.AssetIterator getAssetsByType(osid.shared.Type type) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    public  osid.shared.Id ingest(String fileName,String templateFileName, File file,Properties properties) throws osid.dr.DigitalRepositoryException, java.net.SocketException,java.io.IOException,osid.shared.SharedException,javax.xml.rpc.ServiceException{
        // this part transfers file to a ftp server.  this is required since the content management part of fedora server needs object to be on web server
        String host = "dl.tccs.tufts.edu";
        String url = "http://dl.tccs.tufts.edu/~vue/fedora/";
        int port = 21;
        String userName = "vue";
        String password = "vue@at";
        String directory = "public_html/fedora"; 
        FTPClient client = new FTPClient();
        client.connect(host,port);
        client.login(userName,password);
        client.changeWorkingDirectory(directory); 
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.storeFile(fileName,new FileInputStream(file));
        client.logout();
        client.disconnect();
        fileName = url+fileName;
        // this part does the creation of METSFile
        int BUFFER_SIZE = 10240;
        StringBuffer sb = new StringBuffer();
        String s = new String();
       // FileInputStream fis = new FileInputStream(new File(getResource(templateFileName).getFile()));
        FileInputStream fis = new FileInputStream(new File(templateFileName));
        DataInputStream in = new DataInputStream(fis); 

        byte[] buf = new byte[BUFFER_SIZE];
        int ch;
        int len;
        while((len =fis.read(buf)) > 0) {
            s = s+ new String(buf);
          
        }
        fis.close();
        in.close();
      //  s = sb.toString();
        String r =  s.replaceAll("%file.location%", fileName).trim();
        
        //writing the to outputfile
        File METSfile = File.createTempFile("vueMETSMap",".xml");
        FileOutputStream fos = new FileOutputStream(METSfile);
        fos.write(r.getBytes());
        fos.close();
        AutoIngestor a = new AutoIngestor(address.getHost(), address.getPort(),"fedoraAdmin","fedoraAdmin");
        String pid = a.ingestAndCommit(new FileInputStream(METSfile),"Test Ingest"); 
        System.out.println(" METSfile= " + METSfile.getPath()+" PID = "+pid);
        return new PID(pid);
    }
    
    
   private java.net.URL getResource(String name)
    {
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
        System.out.println("fedora.conf = "+url.getFile());
       return url;
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
   
}


