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

public class GetAssetTypesTest extends TestCase
{
	public GetAssetTypesTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		org.osid.shared.Id id = null;
		String repositoryIdString = null;
		org.osid.repository.Repository repository = null;
		org.osid.shared.TypeIterator typeIterator = null;

		// are there search types to test for?
		org.w3c.dom.NodeList repositoryTypesNodeList = document.getElementsByTagName(OsidTester.ASSET_TYPES_TAG);
		int numRepositoryAssetTypes = repositoryTypesNodeList.getLength();
		for (int i=0; i < numRepositoryAssetTypes; i++) {				
			org.w3c.dom.Element repositoryAssetTypeElement = (org.w3c.dom.Element)repositoryTypesNodeList.item(i);
			
			// get repository
			repositoryIdString = repositoryAssetTypeElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);
			if (repositoryIdString != null) {
				try {
					id = Utilities.getIdManager().getId(repositoryIdString);
				} catch (Throwable t) {
					fail("ID Manager failed");
				}
				repository = repositoryManager.getRepository(id);
			}

			// get type to look for
			org.w3c.dom.NodeList typeNodeList = repositoryAssetTypeElement.getElementsByTagName(OsidTester.TYPE_TAG);
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
						typeIterator = repository.getAssetTypes();
					}
					try {
						org.osid.shared.Type type = typeIterator.nextType();
						assertEquals(expected,Utilities.typeToString(type));
						System.out.println("PASSED: Repository " + repositoryIdString + " Asset Type " + expected);
					} catch (org.osid.shared.SharedException sex) {
						// ignore since this means something is amiss with shared
					}
				}				
			}
		}
	}
}