package edu.tufts.osidimpl.test.repository;

import junit.framework.TestCase;

public class GetRepositoriesTest extends TestCase
{
	public GetRepositoriesTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their repositories to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.REPOSITORIES_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		if (numRepositories > 0) {
			org.osid.repository.RepositoryIterator repositoryIterator = null;
			org.w3c.dom.Element repositoriesElement = (org.w3c.dom.Element)repositoriesNodeList.item(0);
			
			String anyString = repositoriesElement.getAttribute(OsidTester.ANY_ATTR);
			if ( (anyString != null) && (anyString.trim().toLowerCase().equals("true")) ) {
				repositoryIterator = repositoryManager.getRepositories();
				assertTrue(repositoryIterator.hasNextRepository());
				System.out.println("PASSED: Found any repository");
			} else {
				org.w3c.dom.NodeList repositoryNodeList = repositoriesElement.getElementsByTagName(OsidTester.REPOSITORY_TAG);
				int numRepository = repositoryNodeList.getLength();
				if (numRepository > 0) {
					for (int i=0; i < numRepository; i++) {
						//System.out.println("scanning " + i + " of " + numRepository);
						org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoryNodeList.item(i);
						// proceed to get repositories from OSID impl
						if (repositoryIterator == null) {
							// only need to get the iterator once
							repositoryIterator = repositoryManager.getRepositories();
						}
						if (repositoryIterator.hasNextRepository()) {
							org.osid.repository.Repository repository = repositoryIterator.nextRepository();
							
							// test metadata, if present
							RepositoryMetadataTest rmt = new RepositoryMetadataTest(repository,repositoryElement);
						} else {
							fail("No repository found");
						}
					}
				}
			}
		}
	}
}