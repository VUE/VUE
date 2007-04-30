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

public class GetRepositoryTypesTest extends TestCase
{
	public GetRepositoryTypesTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their repository types to test for?
		org.w3c.dom.NodeList repositoryTypesNodeList = document.getElementsByTagName(OsidTester.REPOSITORY_TYPES_TAG);
		int numRepositoryTypes = repositoryTypesNodeList.getLength();
		if (numRepositoryTypes > 0) {
			org.osid.shared.TypeIterator typeIterator = null;
			for (int i=0; i < numRepositoryTypes; i++) {				
				org.w3c.dom.Element repositoryTypeElement = (org.w3c.dom.Element)repositoryTypesNodeList.item(i);
				if (typeIterator == null) {
					// only need to get the iterator once
					typeIterator = repositoryManager.getRepositoryTypes();
				}
				// check which attributes to test
				String expected = Utilities.expectedValue(repositoryTypeElement,OsidTester.TYPE_TAG);
				if (expected != null) {
					try {
						org.osid.shared.Type type = typeIterator.nextType();
						assertEquals(expected,Utilities.typeToString(type));
						System.out.println("PASSED: Repository Types " + expected);
					} catch (org.osid.shared.SharedException sex) {
						// ignore since this means something is amiss with shared
					}
				}				
			}
		}
	}
	

}