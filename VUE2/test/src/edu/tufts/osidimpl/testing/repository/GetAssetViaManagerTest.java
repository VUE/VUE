package edu.tufts.osidimpl.test.repository;

import junit.framework.TestCase;

public class GetAssetViaManagerTest extends TestCase
{
	public GetAssetViaManagerTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their assets to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.ASSET_VIA_MANAGER_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		for (int i=0; i < numRepositories; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoriesNodeList.item(i);
			String idString = repositoryElement.getAttribute(OsidTester.ASSET_ID_ATTR);
			if (idString != null) {
				try {
					org.osid.shared.Id id = Utilities.getIdManager().getId(idString);
					org.osid.repository.Asset asset = repositoryManager.getAsset(id);
					System.out.println("PASSED: Asset By Id Via Manager " + idString);
					
					// check asset metadata, if specified
					AssetMetadataTest amt = new AssetMetadataTest(asset,repositoryElement,"");
				} catch (Throwable t) {
					fail("ID Manager Failed");
				}
			}
		}
	}
}