package edu.tufts.vue.osidimpl.registry;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 * RegistryManager implements a "straw-man" registry interface.
 * </p>
 * 
 * @author Massachusetts Institute of Technology
 */
public class RegistryManager
implements org.osid.registry.RegistryManager
{
	private static String xmlFilename = null;
	private static final String FILE_NOT_FOUND_MESSAGE = "Cannot find or open ";
	
	private static final String REGISTRY_TAG = "registry";
	private static final String PROVIDER_RECORD_TAG = "record";
	private static final String PROVIDER_ID_TAG = "oki:providerid";
	private static final String OSID_SERVICE_TAG = "oki:osidservice";
	private static final String OSID_MAJOR_VERSION_TAG = "oki:osidmajorversion";
	private static final String OSID_MINOR_VERSION_TAG = "oki:osidminorversion";
	private static final String OSID_LOAD_KEY_TAG = "oki:osidloadkey";
	private static final String DISPLAY_NAME_TAG = "oki:displayname";
	private static final String DESCRIPTION_TAG = "oki:description";
	private static final String KEYWORDS_TAG = "oki:keywords";
	private static final String KEYWORD_TAG = "oki:keywordentry";
	private static final String CATEGORIES_TAG = "oki:categories";
	private static final String CATEGORY_TAG = "oki:categoryentry";
	private static final String CATEGORY_TYPE_TAG = "oki:categorytype";
	private static final String CREATOR_TAG = "oki:creator";
	private static final String PUBLISHER_TAG = "oki:publisher";
	private static final String PUBLISHER_URL_TAG = "oki:publisherurl";
	private static final String PROVIDER_MAJOR_VERSION_TAG = "oki:providermajorversion";
	private static final String PROVIDER_MINOR_VERSION_TAG = "oki:providerminonrversion";
	private static final String RELEASE_DATE_TAG = "oki:releasedate";
	private static final String CONTACT_NAME_TAG = "oki:contactname";
	private static final String CONTACT_PHONE_TAG = "oki:contactphone";
	private static final String CONTACT_EMAIL_TAG = "oki:contactemail";
	private static final String LICENSE_AGREEMENT_TAG = "oki:licenseagreement";
	private static final String RIGHTS_TAG = "oki:rights";
	private static final String RIGHT_TAG = "oki:rightentry";
	private static final String RIGHT_TYPE_TAG = "oki:righttype";
	private static final String README_TAG = "oki:readme";
	private static final String IMPLEMENTATION_LANGUAGE_TAG = "oki:implementationlanguage";
	private static final String SOURCE_AVAILABLE_TAG = "oki:sourceavailable";
	private static final String REPOSITORY_ID_TAG = "oki:repositoryid";
	private static final String REPOSITORY_IMAGE_TAG = "oki:repositoryimage";
	private static final String REGISTRATION_DATE_TAG = "oki:registrationdate";
	private static final String FILENAMES_TAG = "oki:filenames";
	private static final String FILENAME_TAG = "oki:filenameentry";
	private static final String FILE_DISPLAY_NAME_TAG = "oki:filedisplayname";
	private static final String OKI_NAMESPACE = "xmlns:oki";
	private static final String OKI_NAMESPACE_URL = "http://www.okiproject.org/registry/elements/1.0/" ;

    private org.osid.OsidContext passedInContext = null;
	private org.osid.OsidContext emptyContext = new org.osid.OsidContext();
    private java.util.Properties configuration = null;
	private java.util.Properties managerProperties = new java.util.Properties();
	private org.osid.id.IdManager idManager = null;
	
	/**
	 * Return the OsidContext assigned earlier.
	 */
    public org.osid.OsidContext getOsidContext()
		throws org.osid.registry.RegistryException
    {
        return this.passedInContext;
    }

	/**
	 * Store away an OsidContext from the consumer.
	 */
    public void assignOsidContext(org.osid.OsidContext context)
		throws org.osid.repository.RepositoryException
    {
        this.passedInContext = context;
    }

	/**
	 * Store the configuration from the consumer and perform other intialization.
	 * This method should be called after assignOsidContext() and before any others.
	 * The default OsidLoader does this automatically.
	 */
    public void assignConfiguration(java.util.Properties configuration)
		throws org.osid.repository.RepositoryException
    {
        this.configuration = configuration;
		try {
			if (this.idManager == null) {
				this.idManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance();
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
		}
	}
	
	/*
		Look in directories beneath the install directory for a registry file.
	 */

	private String[] getXMLFilenames()
	{
		java.util.Vector filenameVector = new java.util.Vector();
		try {
			String targetFilename = tufts.vue.VueResources.getString("OSIDRegistryXmlFilename");
			//System.out.println("target filename " + targetFilename);
			String installDirectory = tufts.vue.VueResources.getString("dataSourceInstallDirectory");
			//System.out.println("install Directory " + installDirectory);
			java.io.File root = new java.io.File(installDirectory);
			java.io.File[] files = root.listFiles();
			for (int i=0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					java.io.File[] subfiles = files[i].listFiles();
					for (int j=0; j < subfiles.length; j++) {
						if (subfiles[j].getName().equals(targetFilename)) {
							filenameVector.addElement(subfiles[j].getAbsolutePath());
							//System.out.println("added " + filenameVector.lastElement());
						}
					}
				}
			}			
		} catch (Exception ex) {
			edu.tufts.vue.util.Logger.log(ex);
		}
		int size = filenameVector.size();
		String result[] = new String[size];
		for (int i=0; i < size; i++) {
			result[i] = (String)filenameVector.elementAt(i);
		}
		return result;
	}
	
	/**
	 * Examine the registry XML for information and maker Providers
	 */
	public org.osid.registry.ProviderIterator getProviders()
		throws org.osid.registry.RegistryException
	{
		java.util.Vector result = new java.util.Vector();
		String files[] = getXMLFilenames();
		for (int f=0; f < files.length; f++) {
			try {
				java.io.InputStream istream = new java.io.FileInputStream(files[f]);
				if (istream == null) {
					edu.tufts.vue.util.Logger.log(FILE_NOT_FOUND_MESSAGE + files[f]);
					throw new org.osid.registry.RegistryException(org.osid.OsidException.CONFIGURATION_ERROR);
				}
				
				javax.xml.parsers.DocumentBuilderFactory dbf = null;
				javax.xml.parsers.DocumentBuilder db = null;
				org.w3c.dom.Document document = null;
				
				dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
				db = dbf.newDocumentBuilder();
				document = db.parse(istream);
				
				org.w3c.dom.NodeList records = document.getElementsByTagName(PROVIDER_RECORD_TAG);
				int numRecords = records.getLength();
				for (int i=0; i < numRecords; i++) {
					String providerId = null;
					String osidService = null;
					int osidMajorVersion = 0;
					int osidMinorVersion = 0;
					String osidLoadKey = null;				
					
					String displayName = null;
					String description = null;
					java.util.Vector keywordVector = new java.util.Vector();
					java.util.Vector categoryVector = new java.util.Vector();
					java.util.Vector categoryTypeVector = new java.util.Vector();
					
					String creator = null;
					String publisher = null;
					String publisherURL = null;
					int providerMajorVersion = 0;
					int providerMinorVersion = 0;
					String releaseDate = null;
					String contactName = null;
					String contactPhone = null;
					String contactEMail = null;
					String licenseAgreement = null;
					
					java.util.Vector rightVector = new java.util.Vector();
					java.util.Vector rightTypeVector = new java.util.Vector();
					String readme = null;
					String implementationLanguage = null;
					boolean sourceAvailable = false;
					
					String repositoryId = null;
					String repositoryImage = null;
					String registrationDate = null;
					java.util.Vector filenameVector = new java.util.Vector();
					java.util.Vector fileDisplayNameVector = new java.util.Vector();
					
					org.w3c.dom.Element record = (org.w3c.dom.Element)records.item(i);
					org.w3c.dom.NodeList nodeList = record.getElementsByTagName(PROVIDER_ID_TAG);
					int numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							providerId = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(OSID_SERVICE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							osidService = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(OSID_MAJOR_VERSION_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							osidMajorVersion = (new Integer(e.getFirstChild().getNodeValue())).intValue();
						}
					}
					
					nodeList = record.getElementsByTagName(OSID_MINOR_VERSION_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							osidMinorVersion = (new Integer(e.getFirstChild().getNodeValue())).intValue();
						}
					}
					
					nodeList = record.getElementsByTagName(OSID_LOAD_KEY_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							osidLoadKey = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(DISPLAY_NAME_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							displayName = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(DESCRIPTION_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							description = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(KEYWORDS_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						
						org.w3c.dom.NodeList keywords = e.getElementsByTagName(KEYWORD_TAG);
						int numKeywords = keywords.getLength();
						for (int j=0; j < numKeywords; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)keywords.item(j);
							if (ex.hasChildNodes()) {
								keywordVector.addElement(ex.getFirstChild().getNodeValue());
							}
						}
					}
					
					nodeList = record.getElementsByTagName(CATEGORIES_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						
						org.w3c.dom.NodeList categories = e.getElementsByTagName(CATEGORY_TAG);
						int numCategories = categories.getLength();
						for (int j=0; j < numCategories; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)categories.item(j);
							if (ex.hasChildNodes()) {
								categoryVector.addElement(ex.getFirstChild().getNodeValue());
							}
						}
						
						org.w3c.dom.NodeList types = e.getElementsByTagName(CATEGORY_TYPE_TAG);
						int numTypes = types.getLength();
						for (int j=0; j < numTypes; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)types.item(j);
							if (ex.hasChildNodes()) {
								org.osid.shared.Type type = edu.tufts.vue.util.Utilities.stringToType(ex.getFirstChild().getNodeValue());
								categoryTypeVector.addElement(type);
							}
						}
					}
					
					nodeList = record.getElementsByTagName(CREATOR_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							creator = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(PUBLISHER_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							publisher = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(PUBLISHER_URL_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							publisherURL = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(PROVIDER_MAJOR_VERSION_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							providerMajorVersion = (new Integer(e.getFirstChild().getNodeValue())).intValue();
						}
					}
					
					nodeList = record.getElementsByTagName(PROVIDER_MINOR_VERSION_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							providerMinorVersion = (new Integer(e.getFirstChild().getNodeValue())).intValue();
						}
					}
					
					nodeList = record.getElementsByTagName(RELEASE_DATE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							releaseDate = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(CONTACT_NAME_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							contactName = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(CONTACT_PHONE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							contactPhone = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(CONTACT_EMAIL_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							contactEMail = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(LICENSE_AGREEMENT_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							licenseAgreement = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(RIGHTS_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						
						org.w3c.dom.NodeList rights = e.getElementsByTagName(RIGHT_TAG);
						int numRights = rights.getLength();
						for (int j=0; j < numRights; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)rights.item(j);
							if (ex.hasChildNodes()) {
								rightVector.addElement(ex.getFirstChild().getNodeValue());
							}
						}
						
						org.w3c.dom.NodeList types = e.getElementsByTagName(RIGHT_TYPE_TAG);
						int numTypes = types.getLength();
						for (int j=0; j < numTypes; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)types.item(j);
							if (ex.hasChildNodes()) {
								org.osid.shared.Type type = edu.tufts.vue.util.Utilities.stringToType(ex.getFirstChild().getNodeValue());
								rightTypeVector.addElement(type);
							}
						}
					}
					
					nodeList = record.getElementsByTagName(README_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							readme = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(IMPLEMENTATION_LANGUAGE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							implementationLanguage = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(SOURCE_AVAILABLE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							sourceAvailable = (new Boolean(e.getFirstChild().getNodeValue())).booleanValue();
						}
					}
					
					nodeList = record.getElementsByTagName(REPOSITORY_ID_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							repositoryId = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(REPOSITORY_IMAGE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							repositoryImage = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(REGISTRATION_DATE_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						if (e.hasChildNodes()) {
							registrationDate = e.getFirstChild().getNodeValue();
						}
					}
					
					nodeList = record.getElementsByTagName(FILENAMES_TAG);
					numNodes = nodeList.getLength();
					for (int k=0; k < numNodes; k++) {
						org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
						
						org.w3c.dom.NodeList filenames = e.getElementsByTagName(FILENAME_TAG);
						int numFiles = filenames.getLength();
						for (int j=0; j < numFiles; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)filenames.item(j);
							if (ex.hasChildNodes()) {
								filenameVector.addElement(ex.getFirstChild().getNodeValue());
							}
						}
						
						org.w3c.dom.NodeList names = e.getElementsByTagName(FILE_DISPLAY_NAME_TAG);
						int numNames = names.getLength();
						for (int j=0; j < numNames; j++) {
							org.w3c.dom.Element ex = (org.w3c.dom.Element)names.item(j);
							if (ex.hasChildNodes()) {
								fileDisplayNameVector.addElement(ex.getFirstChild().getNodeValue());
							}
						}
					}
					
					/*
					 System.out.println(providerId);
					 System.out.println(osidService);
					 System.out.println(osidMajorVersion);
					 System.out.println(osidMinorVersion);
					 System.out.println(osidLoadKey);
					 System.out.println(displayName);
					 System.out.println(description);
					 System.out.println(keywordVector.toArray());
					 System.out.println(categoryVector.toArray());
					 System.out.println(categoryTypeVector.toArray());
					 System.out.println(creator);
					 System.out.println(publisher);
					 System.out.println(publisherURL);
					 System.out.println(majorVersion);
					 System.out.println(minorVersion);
					 System.out.println(releaseDate);
					 System.out.println(contactName);
					 System.out.println(contactPhone);
					 System.out.println(contactEMail);
					 System.out.println(licenseAgreement);
					 System.out.println(rightVector.toArray());
					 System.out.println(rightTypeVector.toArray());
					 System.out.println(readme);
					 System.out.println(implementationLanguage);
					 System.out.println(sourceAvailable);
					 System.out.println(registrationDate);
					 System.out.println(filenameVector.toArray());
					 System.out.println(fileDisplayNameVector.toArray());
					 */
					
					result.addElement(new Provider(this,
												   this.idManager.getId(providerId),
												   osidService,
												   osidMajorVersion,
												   osidMinorVersion,
												   osidLoadKey,				
												   displayName,
												   description,
												   keywordVector,
												   categoryVector,
												   categoryTypeVector,
												   creator,
												   publisher,
												   publisherURL,
												   providerMajorVersion,
												   providerMinorVersion,
												   releaseDate,
												   contactName,
												   contactPhone,
												   contactEMail,
												   licenseAgreement,
												   rightVector,		
												   rightTypeVector,
												   readme,
												   implementationLanguage,
												   sourceAvailable,
												   this.idManager.getId(repositoryId),
												   repositoryImage,
												   registrationDate,
												   filenameVector,
												   fileDisplayNameVector));
				}
			} catch (Throwable t) {
				edu.tufts.vue.util.Logger.log(t);
			}
		}
		return new ProviderIterator(result);
	}
	
	/**
	 * Unimplemented method.  We have no Provider Types for the community at this time.
	 */
	public org.osid.registry.ProviderIterator getProvidersByType(org.osid.shared.Type providerType)
		throws org.osid.registry.RegistryException
	{
		throw new org.osid.registry.RegistryException(org.osid.OsidException.UNIMPLEMENTED);
	}
	
	/**
	 *  Return a Provider from the content in the registry XML file 
	 */
	public org.osid.registry.Provider getProvider(org.osid.shared.Id providerId)
		throws org.osid.registry.RegistryException
	{
		org.osid.registry.ProviderIterator providerIterator = getProviders();
		try {
			while (providerIterator.hasNextProvider())
			{
				org.osid.registry.Provider provider = providerIterator.nextProvider();
				if (provider.getProviderId().isEqual(providerId)) {
					return provider;
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
			throw new org.osid.registry.RegistryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.UNKNOWN_ID);
	}
	
	/**
	 *  Append a new record element to registry XML file.  Omit any nodes where the input argument is
	 *  null, except for the Registration Date.  If that is null, insert the current date and time.
	 */
	public org.osid.registry.Provider createProvider(org.osid.shared.Id providerId,
													 String osidService,
													 int osidMajorVersion,
													 int osidMinorVersion,
													 String osidLoadKey,				
													 String displayName,
													 String description,
													 java.util.Vector keywordVector,
													 java.util.Vector categoryVector,
													 java.util.Vector categoryTypeVector,
													 String creator,
													 String publisher,
													 String publisherURL,
													 int providerMajorVersion,
													 int providerMinorVersion,
													 String releaseDate,
													 String contactName,
													 String contactPhone,
													 String contactEMail,
													 String licenseAgreement,
													 java.util.Vector rightVector,					   
													 java.util.Vector rightTypeVector,					   
													 String readme,
													 String implementationLanguage,
													 boolean sourceAvailable,
													 org.osid.shared.Id repositoryId,
													 String repositoryImage,
													 String registrationDate,
													 java.util.Vector filenameVector,
													 java.util.Vector fileDisplayNameVector)
		throws org.osid.registry.RegistryException
	{
        try {
			String now = null;

			java.io.InputStream istream = new java.io.FileInputStream(this.xmlFilename);;
            if (istream == null) {
				edu.tufts.vue.util.Logger.log(FILE_NOT_FOUND_MESSAGE + this.xmlFilename);
				throw new org.osid.registry.RegistryException(org.osid.OsidException.CONFIGURATION_ERROR);
            }
			
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			org.w3c.dom.Document document = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			document = db.parse(istream);

			org.w3c.dom.NodeList nodeList = document.getElementsByTagName(REGISTRY_TAG);
			int numNodes = nodeList.getLength();
			org.w3c.dom.Element records = (org.w3c.dom.Element)(nodeList.item(0));
			
			org.w3c.dom.Element record = document.createElement(PROVIDER_RECORD_TAG);
			record.setAttribute(OKI_NAMESPACE,OKI_NAMESPACE_URL);
			
			org.w3c.dom.Element e;
			if (providerId != null) {
				e = document.createElement(PROVIDER_ID_TAG);
				e.appendChild(document.createTextNode(providerId.getIdString()));
				record.appendChild(e);
			}			
			
			if (osidService != null) {
				e = document.createElement(OSID_SERVICE_TAG);
				e.appendChild(document.createTextNode(osidService));
				record.appendChild(e);
			}			
			e = document.createElement(OSID_MAJOR_VERSION_TAG);
			e.appendChild(document.createTextNode( (new Integer(osidMajorVersion)).toString() ));
			record.appendChild(e);
			
			e = document.createElement(OSID_MINOR_VERSION_TAG);
			e.appendChild(document.createTextNode( (new Integer(osidMinorVersion)).toString() ));
			record.appendChild(e);
			
			if (osidLoadKey != null) {
				e = document.createElement(OSID_LOAD_KEY_TAG);
				e.appendChild(document.createTextNode(osidLoadKey));
				record.appendChild(e);
			}			
			
			if (displayName != null) {
				e = document.createElement(DISPLAY_NAME_TAG);
				e.appendChild(document.createTextNode(displayName));
				record.appendChild(e);
			}			
			
			if (description != null) {
				e = document.createElement(DESCRIPTION_TAG);
				e.appendChild(document.createTextNode(description));
				record.appendChild(e);
			}			
			
			if (keywordVector.size() > 0) {
				e = document.createElement(KEYWORDS_TAG);
				for (int j=0, size = keywordVector.size(); j < size; j++) {
					org.w3c.dom.Element el = document.createElement(KEYWORD_TAG);
					el.appendChild(document.createTextNode((String)keywordVector.elementAt(j)));
					e.appendChild(el);
				}
				record.appendChild(e);
			}			
			
			if (categoryVector.size() > 0) {
				e = document.createElement(CATEGORIES_TAG);
				for (int j=0, size = categoryVector.size(); j < size; j++) {
					org.w3c.dom.Element el1 = document.createElement(CATEGORY_TAG);
					el1.appendChild(document.createTextNode((String)categoryVector.elementAt(j)));
					org.w3c.dom.Element el2 = document.createElement(CATEGORY_TYPE_TAG);
					String typeString = edu.tufts.vue.util.Utilities.typeToString((org.osid.shared.Type)categoryTypeVector.elementAt(j));
					el2.appendChild(document.createTextNode(typeString));
					e.appendChild(el1);
					e.appendChild(el2);
				}
				record.appendChild(e);
			}			
			
			if (creator != null) {
				e = document.createElement(CREATOR_TAG);
				e.appendChild(document.createTextNode(creator));
				record.appendChild(e);
			}			
			
			if (publisher != null) {
				e = document.createElement(PUBLISHER_TAG);
				e.appendChild(document.createTextNode(publisher));
				record.appendChild(e);
			}			

			if (publisherURL != null) {
				e = document.createElement(PUBLISHER_URL_TAG);
				e.appendChild(document.createTextNode(publisherURL));
				record.appendChild(e);
			}			
			
			e = document.createElement(PROVIDER_MAJOR_VERSION_TAG);
			e.appendChild(document.createTextNode( (new Integer(providerMajorVersion)).toString() ));
			record.appendChild(e);
			
			e = document.createElement(PROVIDER_MINOR_VERSION_TAG);
			e.appendChild(document.createTextNode( (new Integer(providerMinorVersion)).toString() ));
			record.appendChild(e);
			
			if (releaseDate != null) {
				now = releaseDate;
			} else {
				java.util.Calendar calendar = java.util.Calendar.getInstance();
				java.util.Date date = calendar.getTime();
				now = edu.tufts.vue.util.Utilities.dateToString(date);
			}
			e = document.createElement(RELEASE_DATE_TAG);
			e.appendChild(document.createTextNode(now));
			record.appendChild(e);
			
			if (contactName != null) {
				e = document.createElement(CONTACT_NAME_TAG);
				e.appendChild(document.createTextNode(contactName));
				record.appendChild(e);
			}			
			
			if (contactPhone != null) {
				e = document.createElement(CONTACT_PHONE_TAG);
				e.appendChild(document.createTextNode(contactPhone));
				record.appendChild(e);
			}			
			
			if (contactEMail != null) {
				e = document.createElement(CONTACT_EMAIL_TAG);
				e.appendChild(document.createTextNode(contactEMail));
				record.appendChild(e);
			}			
			
			if (licenseAgreement != null) {
				e = document.createElement(LICENSE_AGREEMENT_TAG);
				e.appendChild(document.createTextNode(licenseAgreement));
				record.appendChild(e);
			}			
			
			if (rightVector.size() > 0) {
				e = document.createElement(RIGHTS_TAG);
				for (int j=0, size = rightVector.size(); j < size; j++) {
					org.w3c.dom.Element el1 = document.createElement(RIGHT_TAG);
					el1.appendChild(document.createTextNode((String)rightVector.elementAt(j)));
					org.w3c.dom.Element el2 = document.createElement(RIGHT_TYPE_TAG);
					String typeString = edu.tufts.vue.util.Utilities.typeToString((org.osid.shared.Type)rightTypeVector.elementAt(j));
					el2.appendChild(document.createTextNode(typeString));
					e.appendChild(el1);
					e.appendChild(el2);
				}
				record.appendChild(e);
			}			
			
			if (readme != null) {
				e = document.createElement(README_TAG);
				e.appendChild(document.createTextNode(readme));
				record.appendChild(e);
			}			
			
			if (implementationLanguage != null) {
				e = document.createElement(IMPLEMENTATION_LANGUAGE_TAG);
				e.appendChild(document.createTextNode(implementationLanguage));
				record.appendChild(e);
			}			
			
			e = document.createElement(SOURCE_AVAILABLE_TAG);
			e.appendChild(document.createTextNode( (new Boolean(sourceAvailable)).toString() ));
			
			if (repositoryId != null) {
				e = document.createElement(REPOSITORY_ID_TAG);
				e.appendChild(document.createTextNode(repositoryId.getIdString()));
				record.appendChild(e);
			}			
			
			if (repositoryImage != null) {
				e = document.createElement(REPOSITORY_IMAGE_TAG);
				e.appendChild(document.createTextNode(repositoryImage));
				record.appendChild(e);
			}			
			
			if (registrationDate != null) {
				now = registrationDate;
			} else {
				java.util.Calendar calendar = java.util.Calendar.getInstance();
				java.util.Date date = calendar.getTime();
				now = edu.tufts.vue.util.Utilities.dateToString(date);
			}
			e = document.createElement(REGISTRATION_DATE_TAG);
			e.appendChild(document.createTextNode(now));
			record.appendChild(e);
			
			if (filenameVector.size() > 0) {
				e = document.createElement(FILENAMES_TAG);
				for (int j=0, size = filenameVector.size(); j < size; j++) {
					org.w3c.dom.Element el1 = document.createElement(FILENAME_TAG);
					el1.appendChild(document.createTextNode((String)filenameVector.elementAt(j)));
					org.w3c.dom.Element el2 = document.createElement(FILE_DISPLAY_NAME_TAG);
					el2.appendChild(document.createTextNode((String)fileDisplayNameVector.elementAt(j)));
					e.appendChild(el1);
					e.appendChild(el2);
				}
				record.appendChild(e);
			}			
			
			record.appendChild(e);
			
			records.appendChild(record);

			javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
			javax.xml.transform.Transformer transformer = tf.newTransformer();
			java.util.Properties properties = new java.util.Properties();
			properties.put("indent","yes");
			transformer.setOutputProperties(properties);
			javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
			javax.xml.transform.stream.StreamResult result = 
				new javax.xml.transform.stream.StreamResult (this.xmlFilename);
			transformer.transform(domSource,result);

			return new Provider(this,
								providerId,
								osidService,
								osidMajorVersion,
								osidMinorVersion,
								osidLoadKey,				
								displayName,
								description,
								keywordVector,
								categoryVector,
								categoryTypeVector,
								creator,
								publisher,
								publisherURL,
								providerMajorVersion,
								providerMinorVersion,
								releaseDate,
								contactName,
								contactPhone,
								contactEMail,
								licenseAgreement,
								rightVector,					   
								rightTypeVector,
								readme,
								implementationLanguage,
								sourceAvailable,
								repositoryId,
								repositoryImage,
								registrationDate,
								filenameVector,
								fileDisplayNameVector);
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
		}
		throw new org.osid.registry.RegistryException(org.osid.OsidException.OPERATION_FAILED);
	}
	
	/**
	 * Find the record element in the registry XML file whose identifier node matches the
	 * input.  Remove that record and re-save the XML.
	 */
	public void deleteProvider(org.osid.shared.Id providerId)
		throws org.osid.registry.RegistryException
	{
		if (providerId == null) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}

		java.io.InputStream istream;
		try {
			istream = new java.io.FileInputStream(this.xmlFilename);
		} catch (Exception ex) {
			edu.tufts.vue.util.Logger.log(FILE_NOT_FOUND_MESSAGE + this.xmlFilename);
			throw new org.osid.registry.RegistryException(org.osid.OsidException.CONFIGURATION_ERROR);
		}
		
		try {
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			org.w3c.dom.Document document = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			document = db.parse(istream);

			org.w3c.dom.NodeList registryEntries = document.getElementsByTagName(REGISTRY_TAG);
			org.w3c.dom.Element registry = (org.w3c.dom.Element)registryEntries.item(0);
			
			org.w3c.dom.NodeList records = document.getElementsByTagName(PROVIDER_RECORD_TAG);
			int numRecords = records.getLength();
			for (int i=0; i < numRecords; i++) {
				org.w3c.dom.Element record = (org.w3c.dom.Element)records.item(i);
				org.w3c.dom.NodeList nodeList = record.getElementsByTagName(PROVIDER_ID_TAG);
				int numNodes = nodeList.getLength();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					if (e.hasChildNodes()) {
						String idString = e.getFirstChild().getNodeValue();
						org.osid.shared.Id id = this.idManager.getId(idString);
						if (id.isEqual(providerId)) {
							registry.removeChild(record);
							
							javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
							javax.xml.transform.Transformer transformer = tf.newTransformer();
							java.util.Properties properties = new java.util.Properties();
							properties.put("indent","yes");
							transformer.setOutputProperties(properties);
							javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
							javax.xml.transform.stream.StreamResult result = 
								new javax.xml.transform.stream.StreamResult (this.xmlFilename);
							transformer.transform(domSource,result);
							
							return;
						}
					}
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
			throw new org.osid.registry.RegistryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.UNKNOWN_ID);
	}
	
	public java.io.InputStream downloadProviderImplementation(org.osid.shared.Id providerId) {
		try {
			org.osid.registry.Provider provider = getProvider(providerId);
			String filenames[] = provider.getFilenames();
			
			edu.tufts.vue.dsm.OsidLocalInstaller installer = edu.tufts.vue.dsm.impl.VueOsidLocalInstaller.getInstance();
			String uploadDirectory = edu.tufts.vue.util.Utilities.getOsidUploadDirectory();
			
			for (int i=0; i < filenames.length; i++) {
				java.io.InputStream in = new java.io.FileInputStream(uploadDirectory + filenames[i]);		
				installer.installPlugin(filenames[i],in);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
	
	public void uploadProviderImplementation(org.osid.shared.Id providerId,
											 String filenames[],
											 String fileDisplayNames[],
											 java.io.InputStream[] istreams) {
		try {
			String uploadDirectory = edu.tufts.vue.util.Utilities.getOsidUploadDirectory();
			
			org.osid.registry.Provider provider = getProvider(providerId);

			for (int i=0; i < filenames.length; i++) {
				provider.addFilename(filenames[i],fileDisplayNames[i]);

				java.io.File file = new java.io.File(uploadDirectory + filenames[i]);
				java.io.OutputStream out = new java.io.FileOutputStream(file);
				java.io.InputStream istream = istreams[i];
				
				try
				{
					int j = 0;
					while ( (j = istream.read()) != -1 )
					{
						out.write(j);
					}
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				istream.close();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Checked by the org.osid.OsidLoader.getManager() method
	 */
	public void osidVersion_2_0()
		throws org.osid.registry.RegistryException
	{
	}
}

	