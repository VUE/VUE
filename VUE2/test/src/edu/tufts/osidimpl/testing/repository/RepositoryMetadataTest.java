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
	public RepositoryMetadataTest(org.osid.repository.Repository repository, org.w3c.dom.Element repositoryElement)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// check which attributes to test
		String expected = Utilities.expectedValue(repositoryElement,OsidTester.DISPLAY_NAME_TAG);
		if (expected != null) {
			if (Utilities.isVerbose()) System.out.println("Found display name " + repository.getDisplayName());
			assertEquals("seeking display name " + expected,expected,repository.getDisplayName());
			System.out.println("PASSED: Repository Display Name " + expected);
		}
		
		expected = Utilities.expectedValue(repositoryElement,OsidTester.DESCRIPTION_TAG);
		if (expected != null) {
			if (Utilities.isVerbose()) System.out.println("Found description " + repository.getDescription());
			assertEquals("seeking description " + expected,expected,repository.getDescription());
			System.out.println("PASSED: Repository Description " + expected);
		}
		
		expected = Utilities.expectedValue(repositoryElement,OsidTester.ID_TAG);
		if (expected != null) {
			org.osid.shared.Id id = repository.getId();
			try {
				String idString = id.getIdString();
				if (Utilities.isVerbose()) System.out.println("Found id " + idString);
				assertEquals("seeking id " + expected,expected,idString);
				System.out.println("PASSED: Repository Id " + expected);
			} catch (org.osid.shared.SharedException iex) {
				// ignore since this means something is amiss with Id
			}
		}
		
		expected = Utilities.expectedValue(repositoryElement,OsidTester.TYPE_TAG);
		if (expected != null) {
			if (Utilities.isVerbose()) System.out.println("Found type " + Utilities.typeToString(repository.getType()));
			assertEquals("seeking repository type " + expected,expected,Utilities.typeToString(repository.getType()));
			System.out.println("PASSED: Repository Type " + expected);
		}
	}
}