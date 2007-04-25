package edu.tufts.osidimpl.test.repository;

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
					fail("ID Manager Failed");
				}
			}
		}
	}
}