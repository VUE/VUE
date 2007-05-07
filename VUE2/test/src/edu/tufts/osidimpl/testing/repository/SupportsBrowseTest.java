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

public class SupportsBrowseTest extends TestCase
{
	public SupportsBrowseTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are there repositories to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.SUPPORTS_BROWSING_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		for (int i=0; i < numRepositories; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoriesNodeList.item(i);
			String idString = repositoryElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);
			if (idString != null) {
				try {
					org.osid.shared.Id id = Utilities.getIdManager().getId(idString);
					org.osid.repository.Repository repository = repositoryManager.getRepository(id);
					String expected = null;
					try {
						expected = repositoryElement.getFirstChild().getNodeValue();
						expected = expected.trim().toLowerCase();
						if (expected.equals("true")) {
							org.osid.repository.AssetIterator ai = repository.getAssets();
							assertTrue(ai.hasNextAsset());
							System.out.println("PASSED: Supports Browse for Repository " + idString);
						} else {
							try {
								org.osid.repository.AssetIterator ai = repository.getAssets();
								if (ai.hasNextAsset()) {
									fail("FAILED: Supports Browse for Repository " + idString);
								} else {
									System.out.println("PASSED: Supports Browse for Repository " + idString);
								}
							} catch (org.osid.repository.RepositoryException rex) {
								if (rex.getMessage().equals(org.osid.OsidException.UNIMPLEMENTED)) {
									System.out.println("PASSED: Supports Browse for Repository " + idString);
								} else {
									fail("FAILED: Supports Browse for Repository " + idString);
								}
							}
						}
					} catch (java.lang.NullPointerException npe) {
					}
				} catch (Throwable t) {
					t.printStackTrace();
					fail("ID Manager Failed");
				}
			}
		}
	}
}