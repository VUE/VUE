package edu.tufts.osidimpl.test.repository;

import junit.framework.TestCase;

public class RepositoryMetadataTest extends TestCase
{
	private static final String DISPLAY_NAME_TAG = "displayname";
	private static final String DESCRIPTION_TAG = "description";
	private static final String ID_TAG = "id";
	private static final String TYPE_TAG = "type";
	
	public RepositoryMetadataTest(org.osid.repository.Repository repository, org.w3c.dom.Element repositoryElement)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// check which attributes to test
		String expected = Utilities.expectedValue(repositoryElement,DISPLAY_NAME_TAG);
		if (expected != null) {
			assertEquals(expected,repository.getDisplayName());
			System.out.println("PASSED: Repository Display Name " + expected);
		}
		
		expected = Utilities.expectedValue(repositoryElement,DESCRIPTION_TAG);
		if (expected != null) {
			assertEquals(expected,repository.getDescription());
			System.out.println("PASSED: Repository Description " + expected);
		}
		
		expected = Utilities.expectedValue(repositoryElement,ID_TAG);
		if (expected != null) {
			org.osid.shared.Id id = repository.getId();
			try {
				String idString = id.getIdString();
				assertEquals(expected,idString);
				System.out.println("PASSED: Repository Id " + expected);
			} catch (org.osid.shared.SharedException iex) {
				// ignore since this means something is amiss with Id
			}
		}
		
		expected = Utilities.expectedValue(repositoryElement,TYPE_TAG);
		if (expected != null) {
			assertEquals(expected,Utilities.typeToString(repository.getType()));
			System.out.println("PASSED: Repository Type " + expected);
		}
	}
}