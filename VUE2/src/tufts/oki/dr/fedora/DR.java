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
import fedora.client.utility.ingest.AutoIngestor;


public class DR implements osid.dr.DigitalRepository {
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
    private java.util.Vector infoStructures = new java.util.Vector();
    private java.util.Vector assetTypes = new java.util.Vector();
    private java.util.Vector searchTypes = new java.util.Vector();
    private java.util.Vector assets = new java.util.Vector();
    private osid.shared.Id id = null;
    private URL configuration = null;

    
    // this object stores the information to access soap.  These variables will not be required if Preferences becomes serializable
    private Properties fedoraProperties;
    /** Creates a new instance of DR */
    public DR(String conf,String id,String displayName,String description,URL address,String userName,String password)
        throws osid.dr.DigitalRepositoryException, osid.shared.SharedException
    {
        System.out.println("DR CONSTRUCTING["
                           + conf + ", "
                           + id + ", "
                           + displayName + ", "
                           + description + ", "
                           + address + ", "
                           + userName + ", "
                           + password + "] " + this);
        try
        {
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
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
    
    /** sets a soap call to perform all digital repository operations
     * @throws DigitalRepositoryException if Soap call can't be made
     */
    
    public void setFedoraProperties(Properties fedoraProperties) {
        this.fedoraProperties = fedoraProperties;
    }
    
    public void setFedoraProperties(java.net.URL conf) {
        String url = address.getProtocol()+"://"+address.getHost()+":"+address.getPort()+"/"+address.getFile();
        System.out.println("FEDORA Address = "+ url);
        fedoraProperties = new Properties();
        try {
            System.out.println("Fedora Properties " + conf);
            prefs = FedoraUtils.getPreferences(this);
            fedoraProperties.setProperty("url.fedora.api", prefs.get("url.fedora.api",""));
            fedoraProperties.setProperty("url.fedora.type", prefs.get("url.fedora.type", ""));
            fedoraProperties.setProperty("url.fedora.soap.access",url+ prefs.get("url.fedora.soap.access", ""));
            fedoraProperties.setProperty("url.fedora.get", url+prefs.get("url.fedora.get", ""));
            fedoraProperties.setProperty("fedora.types", prefs.get("fedora.types",""));
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
        while(iter.hasNext()) {
            osid.dr.InfoStructure infoStructure = (osid.dr.InfoStructure)iter.next();
            if(infoStructures.indexOf(infoStructure) < 0)
                infoStructures.add(infoStructure);
        }
        assetTypes.add(fedoraObjectAssetType);
        return fedoraObjectAssetType;
    }
    
    /** AssetTypes are loaded from the configuration file. In future versions these will be loaded directly from FEDORA.
     *  OKI Team recommends having  an object in digital repository that maintains this information.
     * @ throws DigitalRepositoryException
     */
    
    private void loadAssetTypes() throws  osid.dr.DigitalRepositoryException {
        FedoraObjectAssetType fedoraObjectAssetType = new FedoraObjectAssetType(this,"TUFTS_STD_IMAGE");
    }
    
    public FedoraObjectAssetType getAssetType(String type) throws osid.dr.DigitalRepositoryException {
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
    
    
    public osid.dr.AssetIterator getAssets() throws osid.dr.DigitalRepositoryException {
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
        return (osid.dr.AssetIterator) new FedoraObjectIterator(assetVector);
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
        condition[0] = new Condition();
        condition[0].setProperty("pid");
        condition[0].setOperator(ComparisonOperator.eq);
        
        try {
            System.out.println("Searching for object ="+assetId.getIdString());
            condition[0].setValue(assetId.getIdString());
        } catch(osid.shared.SharedException ex) {
            throw new osid.dr.DigitalRepositoryException(ex.getMessage());
        }
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setConditions(condition);
        searchCriteria.setMaxReturns("1");
        osid.dr.AssetIterator mAssetIterator = FedoraSoapFactory.advancedSearch(this,searchCriteria);
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
    
    public osid.dr.AssetIterator getAssets(java.io.Serializable searchCriteria, osid.shared.Type searchType) throws osid.dr.DigitalRepositoryException {

        SearchCriteria lSearchCriteria = null;
        if (searchCriteria instanceof String)
        {
            lSearchCriteria = new SearchCriteria();
            lSearchCriteria.setKeywords((String)searchCriteria);
            lSearchCriteria.setMaxReturns("10");
            lSearchCriteria.setSearchOperation(SearchCriteria.FIND_OBJECTS);
            lSearchCriteria.setResults(0);
        }
        else if (searchCriteria instanceof SearchCriteria)
        {
            lSearchCriteria = (SearchCriteria)searchCriteria;            
        }
        else
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.UNKNOWN_TYPE);
        }
                
        if ( (searchType.getKeyword().equals("Search")) ) 
        {
            return (FedoraSoapFactory.search(this,lSearchCriteria));
        }
        else if(searchType.getKeyword().equals("Advanced Search")) 
        {
            return (FedoraSoapFactory.advancedSearch(this,lSearchCriteria));
        }
        else
        {
            if (!(searchCriteria instanceof String))
            {
                throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.UNKNOWN_TYPE);
            }
            try
            {
                java.util.Vector results = new java.util.Vector();
                java.util.Vector ids = new java.util.Vector();
                java.util.StringTokenizer st = new java.util.StringTokenizer((String)searchCriteria,",");
                while (st.hasMoreTokens())
                {
                    String nextKeyword = st.nextToken().trim();
                    lSearchCriteria = new SearchCriteria();
                    lSearchCriteria.setKeywords(nextKeyword);
                    lSearchCriteria.setMaxReturns("10");
                    lSearchCriteria.setSearchOperation(SearchCriteria.FIND_OBJECTS);
                    lSearchCriteria.setResults(0);
                    osid.dr.AssetIterator ai = FedoraSoapFactory.search(this,lSearchCriteria);
                    while (ai.hasNext())
                    {
                        osid.dr.Asset asset = ai.next();
                        String idString = asset.getId().getIdString();
                        if (ids.indexOf(idString) == -1)
                        {
                            results.addElement(asset);
                            ids.addElement(idString);
                        }
                    }
                }
                return new AssetIterator(results);
            }
            catch (Throwable t)
            {
                throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.OPERATION_FAILED);
            }
        }
    }
    
    public osid.shared.Type getType() throws osid.dr.DigitalRepositoryException {
        throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.UNIMPLEMENTED);
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
    
    public  osid.shared.Id ingest(String fileName,String templateFileName,String fileType, File file,Properties properties) throws osid.dr.DigitalRepositoryException, java.net.SocketException,java.io.IOException,osid.shared.SharedException,javax.xml.rpc.ServiceException{
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
        client.storeFile(fileName,new FileInputStream(file.getAbsolutePath().replaceAll("%20"," ")));
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
        
      //  AutoIngestor a = new AutoIngestor(address.getHost(), address.getPort(),FedoraUtils.getFedoraProperty(this,"admin.fedora.username"),FedoraUtils.getFedoraProperty(this,"admin.fedora.username"));
        //THIS WILL NOT WORK IN NEWER VERSION OF FEDORA
       // String pid =  AutoIngestor.ingestAndCommit(new FileInputStream(METSfile),"foxml1.","Test Ingest");
        if(DEBUG) System.out.println("INGESTING FILE TO FEDORA: Ingest complete:"+(System.currentTimeMillis()-sTime));
        String pid = "Method Not Supported any more";
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
                f = null;
            }
        }
        if (url == null)
            url = getClass().getResource(name);
        System.out.println("DR.getResource(" + name + ") = " + url + " f=" + f);
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


