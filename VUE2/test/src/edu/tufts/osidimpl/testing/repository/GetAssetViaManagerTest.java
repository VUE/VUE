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

public class GetAssetViaManagerTest extends TestCase
{
	public GetAssetViaManagerTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their assets to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.ASSET_VIA_MANAGER_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		for (int j=0; j < numRepositories; j++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoriesNodeList.item(j);
			String idString = repositoryElement.getAttribute(OsidTester.ASSET_ID_ATTR);
			if (idString != null) {
				try {
					org.osid.shared.Id id = Utilities.getIdManager().getId(idString);
					org.osid.repository.Asset asset = repositoryManager.getAsset(id);
					System.out.println("PASSED: Asset By Id Via Manager " + idString);
					// check asset metadata, if specified
					AssetMetadataTest amt = new AssetMetadataTest(asset,repositoryElement,"");
				} catch (Throwable t) {
					//t.printStackTrace();
					fail(t.getMessage());
				}
			}
		}
	}
}