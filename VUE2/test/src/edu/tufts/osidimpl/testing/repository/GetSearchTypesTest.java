package edu.tufts.osidimpl.test.repository;

import junit.framework.TestCase;

public class GetSearchTypesTest extends TestCase
{
	public GetSearchTypesTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		org.osid.shared.Id id = null;
		String repositoryIdString = null;
		org.osid.repository.Repository repository = null;
		org.osid.shared.TypeIterator typeIterator = null;

		// are there search types to test for?
		org.w3c.dom.NodeList repositoryTypesNodeList = document.getElementsByTagName(OsidTester.SEARCH_TYPES_TAG);
		int numRepositorySearchTypes = repositoryTypesNodeList.getLength();
		for (int i=0; i < numRepositorySearchTypes; i++) {				
			org.w3c.dom.Element repositorySearchTypeElement = (org.w3c.dom.Element)repositoryTypesNodeList.item(i);
			
			// get repository
			repositoryIdString = repositorySearchTypeElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);
			if (repositoryIdString != null) {
				try {
					id = Utilities.getIdManager().getId(repositoryIdString);
				} catch (Throwable t) {
					fail("ID Manager failed");
				}
				repository = repositoryManager.getRepository(id);
			}

			// get type to look for
			org.w3c.dom.NodeList typeNodeList = repositorySearchTypeElement.getElementsByTagName(OsidTester.TYPE_TAG);
			int numTypes = typeNodeList.getLength();
			for (int j=0; j < numTypes; j++) {
				org.w3c.dom.Element typeElement = (org.w3c.dom.Element)typeNodeList.item(j);
				String expected = null;
				try {
					expected = typeElement.getFirstChild().getNodeValue();
				} catch (java.lang.NullPointerException npe) {
				}
				if (expected != null) {
					if (typeIterator == null) {
						// only need to get the iterator once
						typeIterator = repository.getSearchTypes();
					}
					try {
						org.osid.shared.Type type = typeIterator.nextType();
						assertEquals(expected,Utilities.typeToString(type));
						System.out.println("PASSED: Repository " + repositoryIdString + " Search Type " + expected);
					} catch (org.osid.shared.SharedException sex) {
						// ignore since this means something is amiss with shared
					}
				}				
			}
		}
	}
}