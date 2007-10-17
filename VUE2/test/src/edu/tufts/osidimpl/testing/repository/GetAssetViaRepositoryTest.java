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

public class GetAssetViaRepositoryTest extends TestCase
{
	public GetAssetViaRepositoryTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their assets to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.ASSET_VIA_REPOSITORY_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		for (int i=0; i < numRepositories; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoriesNodeList.item(i);
			String repositoryIdString = repositoryElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);
			String assetIdString = repositoryElement.getAttribute(OsidTester.ASSET_ID_ATTR);
			if ( (repositoryIdString != null) && (assetIdString != null) ) {
				try {
					org.osid.shared.Id repositoryId = Utilities.getIdManager().getId(repositoryIdString);
					org.osid.shared.Id assetId = Utilities.getIdManager().getId(assetIdString);
					org.osid.repository.Asset asset = repositoryManager.getRepository(repositoryId).getAsset(assetId);
					System.out.println("PASSED: Asset By Id Via Repository " + repositoryIdString + " " + assetIdString);
					
					// check asset metadata, if specified
					AssetMetadataTest amt = new AssetMetadataTest(asset,repositoryElement,"");
				} catch (Throwable t) {
					t.printStackTrace();
					fail("No Repository with the ID " + repositoryIdString + " or no Asset with ID " + assetIdString + " or ID Manager Failed");
				}
			}
		}
	}
}