package edu.tufts.vue.dsm.impl;

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
 This class loads and saves Data Source content from an XML file
 */

public class VueDataSourceManager
implements edu.tufts.vue.dsm.DataSourceManager
{
	private static edu.tufts.vue.dsm.DataSourceManager dataSourceManager = new VueDataSourceManager();
	private java.util.Vector dataSourceVector = new java.util.Vector();
	private static final String FILE_NOT_FOUND_MESSAGE = "Cannot find or open ";
	private static final String OKI_NAMESPACE = "xmlns:oki";
	private static final String OKI_NAMESPACE_URL = "http://www.okiproject.org/registry/elements/1.0/" ;
	private static final String REGISTRY_TAG = "registry";
	private static final String RECORDS_TAG = "records";
	private static final String PROVIDER_RECORD_TAG = "record";
	private static final String PROVIDER_ID_TAG = "oki:providerid";
	private static final String OSID_SERVICE_TAG = "oki:osidservice";
	private static final String OSID_MAJOR_VERSION_TAG = "oki:osidmajorversion";
	private static final String OSID_MINOR_VERSION_TAG = "oki:osidminorversion";
	private static final String OSID_LOAD_KEY_TAG = "oki:osidloadkey";
	private static final String DISPLAY_NAME_TAG = "oki:displayname";
	private static final String DESCRIPTION_TAG = "oki:description";
	private static final String CREATOR_TAG = "oki:creator";
	private static final String PUBLISHER_TAG = "oki:publisher";
	private static final String PUBLISHER_URL_TAG = "oki:publisherurl";
	private static final String PROVIDER_MAJOR_VERSION_TAG = "oki:providermajorversion";
	private static final String PROVIDER_MINOR_VERSION_TAG = "oki:providerminonrversion";
	private static final String RELEASE_DATE_TAG = "oki:releasedate";
	private static final String RIGHTS_TAG = "oki:rights";
	private static final String RIGHT_TAG = "oki:rightentry";
	private static final String RIGHT_TYPE_TAG = "oki:righttype";
	private static final String README_TAG = "oki:readme";
	private static final String REPOSITORY_ID_TAG = "oki:repositoryid";
	private static final String REPOSITORY_IMAGE_TAG = "oki:repositoryimage";
	private static final String REGISTRATION_DATE_TAG = "oki:registrationdate";
	private static final String HIDDEN_TAG = "oki:hidden";
	private static final String INCLUDED_IN_SEARCH_TAG = "okiincludedinsearch";
	private static String xmlFilename = null;
	
	public static edu.tufts.vue.dsm.DataSourceManager getInstance() {
		return dataSourceManager;
	}
	
	public VueDataSourceManager() {
		java.io.File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
		System.out.println("User's VUE folder is " + userFolder.getAbsolutePath());
		this.xmlFilename = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("dataSourceSaveToXmlFilename");
		System.out.println("DS file is " + this.xmlFilename);
		refresh();
	}

	public void refresh() {
		try {
			java.io.InputStream istream = new java.io.FileInputStream(xmlFilename);
            if (istream == null) {
				// assume there are no data sources saved
				System.out.println("no file " + xmlFilename);
				return;
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
				org.osid.shared.Id providerId = null;
				String osidService = null;
				int osidMajorVersion = 0;
				int osidMinorVersion = 0;
				String osidLoadKey = null;				
				
				String providerDisplayName = null;
				String providerDescription = null;
				String creator = null;
				String publisher = null;
				String publisherURL = null;
				int providerMajorVersion = 0;
				int providerMinorVersion = 0;
				java.util.Date releaseDate = null;
				String[] rights = null;
				org.osid.shared.Type[] rightTypes = null;
				org.osid.shared.Id repositoryId = null;
				String repositoryImage = null;
				java.util.Date registrationDate = null;
				boolean isHidden = false;
				boolean isIncludedInSearch = false;		
				
				org.w3c.dom.Element record = (org.w3c.dom.Element)records.item(i);
				org.w3c.dom.NodeList nodeList = record.getElementsByTagName(PROVIDER_ID_TAG);
				int numNodes = nodeList.getLength();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					if (e.hasChildNodes()) {
						String providerIdString = e.getFirstChild().getNodeValue();
						providerId = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(providerIdString);
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
						providerDisplayName = e.getFirstChild().getNodeValue();
					}
				}
				
				nodeList = record.getElementsByTagName(DESCRIPTION_TAG);
				numNodes = nodeList.getLength();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					if (e.hasChildNodes()) {
						providerDescription = e.getFirstChild().getNodeValue();
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
						String releaseDateString = e.getFirstChild().getNodeValue();
						releaseDate = edu.tufts.vue.util.Utilities.stringToDate(releaseDateString);
					}
				}
				
				nodeList = record.getElementsByTagName(RIGHTS_TAG);
				numNodes = nodeList.getLength();
				java.util.Vector rightVector = new java.util.Vector();
				java.util.Vector rightTypeVector = new java.util.Vector();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					
					org.w3c.dom.NodeList rightsNodeList = e.getElementsByTagName(RIGHT_TAG);
					int numRights = rightsNodeList.getLength();
					for (int j=0; j < numRights; j++) {
						org.w3c.dom.Element ex = (org.w3c.dom.Element)rightsNodeList.item(j);
						if (ex.hasChildNodes()) {
							rightVector.addElement(ex.getFirstChild().getNodeValue());
						}
					}

					org.w3c.dom.NodeList types = e.getElementsByTagName(RIGHT_TYPE_TAG);
					int numTypes = types.getLength();
					for (int j=0; j < numTypes; j++) {
						org.w3c.dom.Element ex = (org.w3c.dom.Element)types.item(j);
						if (ex.hasChildNodes()) {
							rightTypeVector.addElement(edu.tufts.vue.util.Utilities.stringToType(ex.getFirstChild().getNodeValue()));
						}
					}
				}
				int size = rightVector.size();
				rights = new String[size];
				for (int x=0; x < size; x++) rights[x] = (String)rightVector.elementAt(x);
				size = rightTypeVector.size();
				rightTypes = new org.osid.shared.Type[size];
				for (int x=0; x < size; x++) rightTypes[x] = (org.osid.shared.Type)rightTypeVector.elementAt(x);
				
				nodeList = record.getElementsByTagName(REPOSITORY_ID_TAG);
				numNodes = nodeList.getLength();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					if (e.hasChildNodes()) {
						String repositoryIdString = e.getFirstChild().getNodeValue();
						repositoryId = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(repositoryIdString);
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
						String registrationDateString = e.getFirstChild().getNodeValue();
						registrationDate = edu.tufts.vue.util.Utilities.stringToDate(registrationDateString);
					}
				}
				
				nodeList = record.getElementsByTagName(HIDDEN_TAG);
				numNodes = nodeList.getLength();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					if (e.hasChildNodes()) {
						String hiddenString = e.getFirstChild().getNodeValue();
						isHidden = (new Boolean(hiddenString)).booleanValue();
					}
				}
				
				nodeList = record.getElementsByTagName(INCLUDED_IN_SEARCH_TAG);
				numNodes = nodeList.getLength();
				for (int k=0; k < numNodes; k++) {
					org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(k);
					if (e.hasChildNodes()) {
						String includedString = e.getFirstChild().getNodeValue();
						isIncludedInSearch = (new Boolean(includedString)).booleanValue();
					}
				}
				/*
				 System.out.println(providerId.getIdString());
				 System.out.println(osidService);
				 System.out.println(osidMajorVersion);
				 System.out.println(osidMinorVersion);
				 System.out.println(osidLoadKey);
				 System.out.println(providerDisplayName);
				 System.out.println(providerDescription);
				 System.out.println(creator);
				 System.out.println(publisher);
				 System.out.println(providerMinorVersion);
				 System.out.println(releaseDate);
				 System.out.println(repositoryId.getIdString());
				 System.out.println(repositoryImage);
				 System.out.println(registrationDate);
				 System.out.println(isHidden);
				 System.out.println(isIncludedInSearch);
				 */ 
				edu.tufts.vue.dsm.Registry registry = edu.tufts.vue.dsm.impl.VueRegistry.getInstance();			
				org.osid.repository.Repository repository = registry.getRepository(providerId);
				
				// if we already have this data source, update it in place
				boolean found = false;
				for (int x=0, sizex = this.dataSourceVector.size(); x < sizex; x++) {
					edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(x);
					if (ds.getProviderId().isEqual(providerId)) {
						found = true;
						ds.setProviderId(providerId);
						ds.setOsidService(osidService);
						ds.setMajorOsidVersion(osidMajorVersion);
						ds.setMinorOsidVersion(osidMinorVersion);
						ds.setOsidLoadKey(osidLoadKey);
						ds.setProviderDisplayName(providerDisplayName);
						ds.setProviderDescription(providerDescription);
						ds.setCreator(creator);
						ds.setPublisher(publisher);
						ds.setPublisherURL(publisherURL);
						ds.setProviderMajorVersion(providerMajorVersion);
						ds.setProviderMinorVersion(providerMinorVersion);
						ds.setReleaseDate(releaseDate);
						ds.setRights(rights);
						ds.setRightTypes(rightTypes);
						ds.setRepositoryId(repositoryId);
						ds.setRepositoryImage(repositoryImage);
						ds.setRegistrationDate(registrationDate);
						ds.setHidden(isHidden);
						ds.setIncludedInSearch(isIncludedInSearch);
					}
				}
				if (!found) {
					this.dataSourceVector.addElement(new edu.tufts.vue.dsm.impl.VueDataSource(providerId,
																							  osidService,
																							  osidMajorVersion,
																							  osidMinorVersion,
																							  osidLoadKey,
																							  providerDisplayName,
																							  providerDescription,
																							  creator,
																							  publisher,
																							  publisherURL,
																							  providerMajorVersion,
																							  providerMinorVersion,
																							  releaseDate,
																							  rights,
																							  rightTypes,
																							  repositoryId,
																							  repositoryImage,
																							  registrationDate,
																							  isHidden,
																							  isIncludedInSearch));
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
		}

	}
	
	/**
	*/
	public void save() {
        try {
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			org.w3c.dom.Document document = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			document = db.newDocument(); // always rewrite complete file

			org.w3c.dom.Element top = document.createElement(REGISTRY_TAG);
			org.w3c.dom.Element records = document.createElement(RECORDS_TAG);
			for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
				edu.tufts.vue.dsm.DataSource dataSource = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
				
				org.w3c.dom.Element record = document.createElement(PROVIDER_RECORD_TAG);
				record.setAttribute(OKI_NAMESPACE,OKI_NAMESPACE_URL);

				String nextValue = dataSource.getProviderId().getIdString();
				org.w3c.dom.Element e;
				if (nextValue != null) {
					e = document.createElement(PROVIDER_ID_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}

				nextValue = dataSource.getOsidService();
				if (nextValue != null) {
					e = document.createElement(OSID_SERVICE_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = (new Integer(dataSource.getMajorOsidVersion())).toString();
				if (nextValue != null) {
					e = document.createElement(OSID_MAJOR_VERSION_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = (new Integer(dataSource.getMinorOsidVersion())).toString();
				if (nextValue != null) {
					e = document.createElement(OSID_MINOR_VERSION_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = dataSource.getOsidLoadKey();
				if (nextValue != null) {
					e = document.createElement(OSID_LOAD_KEY_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = dataSource.getProviderDisplayName();
				if (nextValue != null) {
					e = document.createElement(DISPLAY_NAME_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}

				nextValue = dataSource.getProviderDescription();
				if (nextValue != null) {
					e = document.createElement(DESCRIPTION_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = dataSource.getCreator();
				if (nextValue != null) {
					e = document.createElement(CREATOR_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = dataSource.getPublisher();
				if (nextValue != null) {
					e = document.createElement(PUBLISHER_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = dataSource.getPublisherURL();
				if (nextValue != null) {
					e = document.createElement(PUBLISHER_URL_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = (new Integer(dataSource.getProviderMajorVersion())).toString();
				if (nextValue != null) {
					e = document.createElement(PROVIDER_MAJOR_VERSION_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = (new Integer(dataSource.getProviderMinorVersion())).toString();
				if (nextValue != null) {
					e = document.createElement(PROVIDER_MINOR_VERSION_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = edu.tufts.vue.util.Utilities.dateToString(dataSource.getReleaseDate());
				if (nextValue != null) {
					e = document.createElement(RELEASE_DATE_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				String rights[] = dataSource.getRights();
				org.osid.shared.Type rightTypes[] = dataSource.getRightTypes();
				
				if (rights.length > 0) {
					e = document.createElement(RIGHTS_TAG);
					for (int j=0; j < rights.length; j++) {
						String nextRight = rights[j];
						if (nextRight != null) {
							org.w3c.dom.Element el1 = document.createElement(RIGHT_TAG);
							el1.appendChild(document.createTextNode(nextRight));
							e.appendChild(el1);

							if (rightTypes.length > j) {
								String typeString = edu.tufts.vue.util.Utilities.typeToString(rightTypes[j]);
								if (typeString != null) {
									org.w3c.dom.Element el2 = document.createElement(RIGHT_TYPE_TAG);
									el2.appendChild(document.createTextNode(typeString));
									e.appendChild(el2);
								}
							}
						}
					}
					record.appendChild(e);
				} 

				nextValue = dataSource.getRepositoryId().getIdString();
				if (nextValue != null) {
					e = document.createElement(REPOSITORY_ID_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = dataSource.getRepositoryImage();
				if (nextValue != null) {
					e = document.createElement(REPOSITORY_IMAGE_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}
				
				nextValue = edu.tufts.vue.util.Utilities.dateToString(dataSource.getRegistrationDate());
				if (nextValue != null) {
					e = document.createElement(REGISTRATION_DATE_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);					
				}

				nextValue = (dataSource.isHidden()) ? "true" : "false";
				if (nextValue != null) {
					e = document.createElement(HIDDEN_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);					
				}
								
				nextValue = (dataSource.isIncludedInSearch()) ? "true" : "false";
				if (nextValue != null) {
					e = document.createElement(INCLUDED_IN_SEARCH_TAG);
					e.appendChild(document.createTextNode(nextValue));
					record.appendChild(e);
				}

				records.appendChild(record);
			}
			
			top.appendChild(records);
			document.appendChild(records);
			
			javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
			javax.xml.transform.Transformer transformer = tf.newTransformer();
			java.util.Properties properties = new java.util.Properties();
			properties.put("indent","yes");
			transformer.setOutputProperties(properties);
			javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
			javax.xml.transform.stream.StreamResult result = 
				new javax.xml.transform.stream.StreamResult (this.xmlFilename);
			transformer.transform(domSource,result);
			
			refresh();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"saving data sources to XML");
		}
	}
	
	/**
	*/
	public edu.tufts.vue.dsm.DataSource[] getDataSources() {
		int size = this.dataSourceVector.size();
		edu.tufts.vue.dsm.DataSource dataSources[] = new edu.tufts.vue.dsm.DataSource[size];
		for (int i=0; i < size; i++) dataSources[i] = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
		return dataSources;
	}
	
	/**
	*/
	public void add(edu.tufts.vue.dsm.DataSource dataSource) {
		try {
			// we have to worry about duplicates
			org.osid.shared.Id providerId = dataSource.getProviderId();
			for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
				edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
				if (providerId.isEqual(ds.getProviderId())) {
					// duplicate, no error
					edu.tufts.vue.util.Logger.log("cannot add a data source with a provider id already in use");
					return;
				}
			}
			this.dataSourceVector.addElement(dataSource);
			save();
		} catch (Throwable t) {
			
		}
	}
	
	/**
	*/
	public void remove(edu.tufts.vue.dsm.DataSource dataSource) {
		try {
			org.osid.shared.Id providerId = dataSource.getProviderId();
			for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
				edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
				if (providerId.isEqual(ds.getProviderId())) {
					this.dataSourceVector.removeElementAt(i);
					save();
				}
			}
		} catch (Throwable t) {
			
		}
	}
	
	/**
	*/
	public edu.tufts.vue.dsm.DataSource getDataSource(org.osid.shared.Id repositoryId) {
		try {
			for (int i=0, size = this.dataSourceVector.size(); i < size; i++) {
				edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
				if (repositoryId.isEqual(ds.getRepositoryId())) {
					return ds;
				}
			}		
		} catch (Throwable t) {
			
		}
		return null;
	}
	
	/**
	*/
	public org.osid.repository.Repository[] getIncludedRepositories() {
		java.util.Vector results = new java.util.Vector();
		int size = this.dataSourceVector.size();
		for (int i=0; i < size; i++) {
			edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)this.dataSourceVector.elementAt(i);
			if (ds.isIncludedInSearch()) {
				results.addElement(ds.getRepository());
			}
		}
		size = results.size();
		org.osid.repository.Repository repositories[] = new org.osid.repository.Repository[size];
		for (int i=0; i < size; i++) {
			repositories[i] = (org.osid.repository.Repository)results.elementAt(i);
		}
		return repositories;
	}
	
	/**
	*/
	public java.awt.Image getImageForRepositoryType(org.osid.shared.Type repositoryType) {
		return null;
	}
	
	/**
	*/
	public java.awt.Image getImageForSearchType(org.osid.shared.Type searchType) {
		return null;
	}
	
	/**
	*/
	public java.awt.Image getImageForAssetType(org.osid.shared.Type assetType) {
		return null;
	}	
}	