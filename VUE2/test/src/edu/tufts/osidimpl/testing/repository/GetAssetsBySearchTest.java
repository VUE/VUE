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

public class GetAssetsBySearchTest extends TestCase
{
	public GetAssetsBySearchTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// is there a mention of searching for assets from a specific repository?
		org.w3c.dom.NodeList assetsBySearchNodeList = document.getElementsByTagName(OsidTester.ASSETS_BY_SEARCH_TAG);
		int numSearches = assetsBySearchNodeList.getLength();
		for (int i=0; i < numSearches; i++) {
			org.osid.shared.Type searchType = null;
			org.osid.repository.AssetIterator assetIterator = null;
				
			// is there a repository with the associated id
			org.w3c.dom.Element assetsBySearchElement = (org.w3c.dom.Element)assetsBySearchNodeList.item(i);
			String repositoryIdString = repositoryIdString = assetsBySearchElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);			
			org.osid.shared.Id repositoryId = null;
			try {
				repositoryId = Utilities.getIdManager().getId(repositoryIdString);
			} catch (Throwable t) {
				fail("Trouble with Id Manager implentation");
			}
			org.osid.repository.Repository repository = null;
			try {
				repository = repositoryManager.getRepository(repositoryId);
			} catch (Throwable t) {
				fail("No repositoryid attribute or no Repository found");
			}
				
			// is there a search defined			
			org.w3c.dom.NodeList searchNodeList = assetsBySearchElement.getElementsByTagName(OsidTester.SEARCH_TAG);
			int num = searchNodeList.getLength();
			if (num > 0) {
					
				// does it have a type and criteria
				org.w3c.dom.Element element = (org.w3c.dom.Element)searchNodeList.item(0);
				String typeString = Utilities.expectedValue(element,OsidTester.TYPE_TAG);
				String criteria = Utilities.expectedValue(element,OsidTester.CRITERIA_TAG);
				if ((typeString != null) && (criteria != null)) {
					searchType = Utilities.stringToType(typeString);
					org.w3c.dom.NodeList assetsNodeList = document.getElementsByTagName(OsidTester.SEARCH_TAG);
				}
					
				// no support for SearchProperties at present
				org.osid.shared.Properties searchProperties = new SharedProperties();
					
				// is there a set of assets expected
				org.w3c.dom.NodeList assetsNodeList = assetsBySearchElement.getElementsByTagName(OsidTester.ASSETS_TAG);
				num = assetsNodeList.getLength();
				if (num > 0) {
					org.w3c.dom.Element assetsElement = (org.w3c.dom.Element)assetsNodeList.item(0);
					String anyTestOnly = assetsElement.getAttribute("any");
						
					if (anyTestOnly.toLowerCase().trim().equals("true")) {
						// time to search (if we are looking for anything)
						assetIterator = repository.getAssetsBySearch(criteria,
																	searchType,
																	searchProperties);
						assert(assetIterator.hasNextAsset());
						System.out.println("PASSED: Assets By Search - any result is sufficient");
					} else {
						// are there specific assets (or just any assets will do)
						org.w3c.dom.NodeList assetNodeList = assetsElement.getElementsByTagName(OsidTester.ASSET_TAG);
						num = assetNodeList.getLength();
						if (num > 0) {
							// we are ready to search
							for (int j=0; j < num; j++) {
								org.w3c.dom.Element assetElement = (org.w3c.dom.Element)assetNodeList.item(j);
								if (assetIterator == null) {
									assetIterator = repository.getAssetsBySearch(criteria,
																				searchType,
																				searchProperties);
								}
								org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
									
								// check asset metadata, if specified
								AssetMetadataTest amt = new AssetMetadataTest(nextAsset,assetElement,(new Integer(j)).toString());
							}
						}
					}
				}
			}
		}
	
		// is there a mention of searching for assets via manager?
		assetsBySearchNodeList = document.getElementsByTagName(OsidTester.ASSETS_BY_SEARCH_VIA_MANAGER_TAG);
		numSearches = assetsBySearchNodeList.getLength();
		for (int i=0; i < numSearches; i++) {
			org.osid.shared.Type searchType = null;
			org.osid.repository.AssetIterator assetIterator = null;
			
			// find repository records
			org.w3c.dom.Element assetsBySearchElement = (org.w3c.dom.Element)assetsBySearchNodeList.item(i);
			org.w3c.dom.NodeList repositoriesNodeList = assetsBySearchElement.getElementsByTagName(OsidTester.REPOSITORIES_TAG);
			int numRepositories = repositoriesNodeList.getLength();
			if (numRepositories != 0) {
				// only should be one of these
				org.w3c.dom.Element repositoriesElement = (org.w3c.dom.Element)repositoriesNodeList.item(0);
				org.w3c.dom.NodeList repositoryNodeList = repositoriesElement.getElementsByTagName(OsidTester.REPOSITORY_TAG);
				int numRepository = repositoryNodeList.getLength();
				org.osid.repository.Repository repositoryArray[] = new org.osid.repository.Repository[numRepositories];
				org.osid.id.IdManager idManager = Utilities.getIdManager();
				for (int r=0; r < numRepositories; r++) {
					org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoryNodeList.item(r);
					// don't decode
					String repositoryIdString = repositoryElement.getFirstChild().getNodeValue();
					try {
						repositoryArray[r] = repositoryManager.getRepository(idManager.getId(repositoryIdString));
					} catch (org.osid.repository.RepositoryException rex) {
						fail("No repository id or no Repository found");
					} catch (Throwable t) {
						fail("Trouble with Id Manager implentation");
					}
				}
				
				// is there a search defined			
				org.w3c.dom.NodeList searchNodeList = assetsBySearchElement.getElementsByTagName(OsidTester.SEARCH_TAG);
				int num = searchNodeList.getLength();
				if (num > 0) {
					
					// does it have a type and criteria
					org.w3c.dom.Element element = (org.w3c.dom.Element)searchNodeList.item(0);
					String typeString = Utilities.expectedValue(element,OsidTester.TYPE_TAG);
					String criteria = Utilities.expectedValue(element,OsidTester.CRITERIA_TAG);
					if ((typeString != null) && (criteria != null)) {
						searchType = Utilities.stringToType(typeString);
						org.w3c.dom.NodeList assetsNodeList = document.getElementsByTagName(OsidTester.SEARCH_TAG);
					}
					
					// no support for SearchProperties at present
					org.osid.shared.Properties searchProperties = new SharedProperties();
					
					// is there a set of assets expected
					org.w3c.dom.NodeList assetsNodeList = assetsBySearchElement.getElementsByTagName(OsidTester.ASSETS_TAG);
					num = assetsNodeList.getLength();
					if (num > 0) {
						org.w3c.dom.Element assetsElement = (org.w3c.dom.Element)assetsNodeList.item(0);
						String anyTestOnly = assetsElement.getAttribute("any");
						
						if (anyTestOnly.toLowerCase().trim().equals("true")) {
							// time to search (if we are looking for anything)
							assetIterator = repositoryManager.getAssetsBySearch(repositoryArray,
																				criteria,
																				searchType,
																				searchProperties);
							assert(assetIterator.hasNextAsset());
							System.out.println("PASSED: Assets By Search via Manager - any result is sufficient");
						} else {
							// are there specific assets (or just any assets will do)
							org.w3c.dom.NodeList assetNodeList = assetsElement.getElementsByTagName(OsidTester.ASSET_TAG);
							num = assetNodeList.getLength();
							if (num > 0) {
								// we are ready to search
								for (int j=0; j < num; j++) {
									org.w3c.dom.Element assetElement = (org.w3c.dom.Element)assetNodeList.item(j);
									if (assetIterator == null) {
										assetIterator = repositoryManager.getAssetsBySearch(repositoryArray,
																							criteria,
																							searchType,
																							searchProperties);
									}
									org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
									
									// check asset metadata, if specified
									AssetMetadataTest amt = new AssetMetadataTest(nextAsset,assetElement,(new Integer(j)).toString());
								}
							}
						}
					}
				}
			}
		}
	}
}