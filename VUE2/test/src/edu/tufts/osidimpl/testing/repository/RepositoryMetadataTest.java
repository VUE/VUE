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

package edu.tufts.osidimpl.testing.repository;

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