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

public class GetAssetsTest extends TestCase
{
	public GetAssetsTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		try {
		// are there assets to test for?
		org.w3c.dom.NodeList assetsNodeList = document.getElementsByTagName(OsidTester.ALL_ASSETS_TAG);
		int numTests = assetsNodeList.getLength();
		//System.out.println("all assets " + numTests);
		for (int i=0; i < numTests; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)assetsNodeList.item(i);
			String anyTestOnly = repositoryElement.getAttribute("any");
			//System.out.println("looking for repository attr in pass " + i);
			String repositoryIdString = repositoryElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);
			//System.out.println("repository attr in pass " + i + " " + repositoryIdString);
			if (repositoryIdString != null) {
				try {
					org.osid.shared.Id repositoryId = Utilities.getIdManager().getId(repositoryIdString);
					//System.out.println("looking for repository with id " + repositoryIdString);
					org.osid.repository.Repository repository = repositoryManager.getRepository(repositoryId);
					
					String assetIdString = repositoryElement.getAttribute(OsidTester.ASSET_ID_ATTR);
					//System.out.println("ais " + assetIdString);
					if ((assetIdString != null) && (assetIdString.trim().length() > 0)) {
						// we are looking for sub-assets of some asset
						//System.out.println("looking for asset with id " + assetIdString);
						org.osid.shared.Id assetId = Utilities.getIdManager().getId(assetIdString);
						org.osid.repository.Asset asset = repository.getAsset(assetId);
						
						if (anyTestOnly.toLowerCase().trim().equals("true")) {
							// we are going to look for anything
							org.osid.repository.AssetIterator assetIterator = asset.getAssets();
							assertTrue(assetIterator.hasNextAsset());
							//System.out.println("PASSED: All Sub-Assets - any result is sufficient");
							// we are done since no exception was raised
						} else {
							// we are going to look for specific results
							org.w3c.dom.NodeList subAssetsNodeList = repositoryElement.getElementsByTagName(OsidTester.ASSETS_TAG);
							int num = subAssetsNodeList.getLength();
							if (num > 0) {
								org.w3c.dom.Element subAssetsElement = (org.w3c.dom.Element)subAssetsNodeList.item(0);
								org.osid.repository.AssetIterator assetIterator = asset.getAssets();
								for (int j=0; j < num; j++) {
									org.w3c.dom.Element subAssetElement = (org.w3c.dom.Element)subAssetsNodeList.item(j);
									org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
									// check asset metadata, if specified
									AssetMetadataTest amt = new AssetMetadataTest(nextAsset,subAssetElement,(new Integer(j)).toString());
								}
							}
						}
					} else {
						// we are looking for assets of some repository
						if (anyTestOnly.toLowerCase().trim().equals("true")) {
							// we are going to look for anything
							org.osid.repository.AssetIterator assetIterator = repository.getAssets();
							assertTrue(assetIterator.hasNextAsset());
							//System.out.println("PASSED: All Repository Assets - any result is sufficient");
							// we are done since no exception was raised
						} else {
							// we are going to look for specific results
							org.w3c.dom.NodeList subAssetsNodeList = repositoryElement.getElementsByTagName(OsidTester.ASSETS_TAG);
							int num = subAssetsNodeList.getLength();
							if (num > 0) {
								org.w3c.dom.Element subAssetsElement = (org.w3c.dom.Element)subAssetsNodeList.item(0);
								org.osid.repository.AssetIterator assetIterator = repository.getAssets();
								for (int j=0; j < num; j++) {
									org.w3c.dom.Element subAssetElement = (org.w3c.dom.Element)subAssetsNodeList.item(j);
									org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
									// check asset metadata, if specified
									AssetMetadataTest amt = new AssetMetadataTest(nextAsset,subAssetElement,(new Integer(j)).toString());
								}
							}
						}
					}
				} catch (java.lang.NullPointerException npe) {
					npe.printStackTrace();					
				} catch (org.osid.id.IdException iex) {
					iex.printStackTrace();
				}
			}
		}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}