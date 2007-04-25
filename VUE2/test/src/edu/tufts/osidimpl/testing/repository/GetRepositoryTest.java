package edu.tufts.osidimpl.test.repository;

import junit.framework.TestCase;

public class GetRepositoryTest extends TestCase
{
	public GetRepositoryTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their repositories to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.REPOSITORY_BY_ID_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		for (int i=0; i < numRepositories; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoriesNodeList.item(i);
			String idString = repositoryElement.getAttribute(OsidTester.ID_ATTR);
			if (idString != null) {
				try {
					org.osid.shared.Id id = Utilities.getIdManager().getId(idString);
					org.osid.repository.Repository repository = repositoryManager.getRepository(id);
					System.out.println("PASSED: Repository By Id " + idString);
				} catch (Throwable t) {
					fail("ID Manager Failed");
				}
			}
		}
	}
}