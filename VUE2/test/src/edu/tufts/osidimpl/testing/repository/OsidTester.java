package edu.tufts.osidimpl.test.repository;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

public class OsidTester extends TestCase
{
	public static final String ASSET_TAG = "asset";
	public static final String ASSET_VIA_MANAGER_TAG = "assetbyidviamanager";	
	public static final String ASSET_VIA_REPOSITORY_TAG = "assetbyidviarepository";
	public static final String ASSETS_BY_SEARCH_TAG = "assetsBySearch";
	public static final String ASSETS_TAG = "assets";
	public static final String CONFIGURATION_TAG = "configuration";
	public static final String CRITERIA_TAG = "criteria";	
	public static final String DESCRIPTION_TAG = "description";
	public static final String DISPLAY_NAME_TAG = "displayname";
	public static final String ID_TAG = "id";
	public static final String MANAGER_TAG = "managerImpl";
	public static final String PACKAGENAME_TAG = "packagename";
	public static final String PROPERTY_TAG = "property";
	public static final String REPOSITORIES_BY_TYPE_TAG = "repositoriesbytype";
	public static final String REPOSITORIES_TAG = "repositories";
	public static final String REPOSITORY_BY_ID_TAG = "repositorybyid";
	public static final String REPOSITORY_ID_TAG = "repositoryid";
	public static final String REPOSITORY_TAG = "repository";
	public static final String REPOSITORY_TYPES_TAG = "repositorytypes";	
	public static final String SEARCH_TAG = "search";
	public static final String SEARCH_TYPES_TAG = "searchtypes";	
	public static final String TYPE_TAG = "type";
	public static final String VALUE_TAG = "value";

	public static final String ANY_ATTR = "any";
	public static final String ASSET_ID_ATTR = "assetid";
	public static final String AUTO_ATTR = "auto";
	public static final String ID_ATTR = "id";	
	public static final String KEY_ATTR = "key";
	public static final String NAME_ATTR = "name";
	public static final String OSID_ATTR = "osid";
	public static final String PART_TAG = "part";
	public static final String REPOSITORY_ID_ATTR = "repositoryid";	
	public static final String VERSION_ATTR = "version";
	public static final String VALUE_ATTR = "value";
	
	private org.osid.repository.RepositoryManager _repositoryManager = null;
	private org.w3c.dom.Document _document = null;
	private String _packagename = null;

	protected void setUp()
	{
		try {
			// get environment variable with test file
			java.util.Properties systemProperties = System.getProperties();
			String xmlFilepath = systemProperties.getProperty("testProfile");
			
			java.io.InputStream xmlStream = new java.io.FileInputStream(xmlFilepath);
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			_document = db.parse(xmlStream);
			
			_repositoryManager = getRepositoryManager();
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	public void testGetRepositories()
	{
		try {
			new GetRepositoriesTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}

	public void testGetRepositoryById()
	{
		try {
			new GetRepositoryTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	public void testGetRepositoryTypes()
	{
		try {
			new GetRepositoryTypesTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	public void testGetRepositoriesByType()
	{
		try {
			new GetRepositoriesByTypeTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	public void testGetSearchTypes()
	{
		try {
			new GetSearchTypesTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	public void testGetAssetByIdViaManager()
	{
		try {
			new GetAssetViaManagerTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	public void testGetAssetByIdViaRepository()
	{
		try {
			new GetAssetViaRepositoryTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}

	public void testGetAssetsBySearch()
	{
		try {
			new GetAssetsBySearchTest(_repositoryManager,_document);
		} catch (Throwable t) {
			//t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	private org.osid.repository.RepositoryManager getRepositoryManager()
		throws org.osid.repository.RepositoryException
	{
		try {
			String name = null;
			String osid = null;
			String version = null;
			_packagename = null;
			org.osid.OsidContext context = new org.osid.OsidContext();
			java.util.Properties properties = new java.util.Properties();
			
			org.w3c.dom.NodeList managers = _document.getElementsByTagName(MANAGER_TAG);
			org.w3c.dom.Element managerElement = (org.w3c.dom.Element)managers.item(0);
			name = managerElement.getAttribute(NAME_ATTR);
			osid = managerElement.getAttribute(OSID_ATTR);
			version = managerElement.getAttribute(VERSION_ATTR);
			
			if (!version.equals("2.0")) throw new org.osid.repository.RepositoryException(org.osid.OsidException.CONFIGURATION_ERROR);
			
			org.w3c.dom.NodeList nl = managerElement.getElementsByTagName(PACKAGENAME_TAG);
			org.w3c.dom.Element packagenameElement = (org.w3c.dom.Element)nl.item(0);
			_packagename = packagenameElement.getFirstChild().getNodeValue();
				
			//TODO: add support for OsidContext
			
			nl = managerElement.getElementsByTagName(CONFIGURATION_TAG);
			int numConfigurations = nl.getLength();
			if (numConfigurations > 0) {
				org.w3c.dom.Element configurationElement = (org.w3c.dom.Element)nl.item(0);
				nl = configurationElement.getElementsByTagName(PROPERTY_TAG);
				int numProperties = nl.getLength();
				for (int j=0; j < numProperties; j++) {
					org.w3c.dom.Element el = (org.w3c.dom.Element)nl.item(j);
					String key = el.getAttribute(KEY_ATTR);
					// assume values are not encrypted
					String value = el.getAttribute(VALUE_ATTR);
					properties.setProperty(key,value);
				}		
			}
			
			/* If we are running in VUE, we need to use its method for loading OSID implementations
			   The approach uses a factory and assumes a package name and repository id -- a "loadkey".
			   We only care about the manager, so we add a dummy repository id, @foo, to fool the factory.
			*/
			try {
				return edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRepositoryManagerInstance(_packagename + "@foo",
																										context,
																										properties);
			} catch (Throwable t) {
				return (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager("org.osid.repository.RepositoryManager",
																							 _packagename,
																							 context,
																							 properties);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.CONFIGURATION_ERROR);
		}
	}
}