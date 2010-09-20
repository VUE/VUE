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

public class GetAssetsByTypeTest extends TestCase
{
	public GetAssetsByTypeTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are there assets to test for?
		org.w3c.dom.NodeList assetsByTypeNodeList = document.getElementsByTagName(OsidTester.ASSETS_BY_TYPE_TAG);
		int numTests = assetsByTypeNodeList.getLength();
		for (int i=0; i < numTests; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)assetsByTypeNodeList.item(i);
			String idString = repositoryElement.getAttribute(OsidTester.REPOSITORY_ID_ATTR);
			if (idString != null) {
				try {
					org.osid.shared.Id id = Utilities.getIdManager().getId(idString);
					org.osid.repository.Repository repository = repositoryManager.getRepository(id);
					
					org.w3c.dom.NodeList typesNodeList = repositoryElement.getElementsByTagName(OsidTester.ASSET_TYPE_TAG);
					int numTypes = typesNodeList.getLength();
					for (int j=0; j < numTypes; j++) {
						org.w3c.dom.Element typeElement = (org.w3c.dom.Element)typesNodeList.item(j);
						String assetTypeString = null;
						assetTypeString = typeElement.getFirstChild().getNodeValue();
						org.osid.shared.Type assetType = Utilities.stringToType(assetTypeString);
  						
						org.w3c.dom.NodeList assetNodeList = repositoryElement.getElementsByTagName(OsidTester.ASSET_TAG);
						int numAssets = assetNodeList.getLength();
						if (numAssets > 0) {
							
							// we are ready to search
							for (int k=0; k < numAssets; k++) {
								org.w3c.dom.Element assetElement = (org.w3c.dom.Element)assetNodeList.item(k);
								org.osid.repository.AssetIterator assetIterator = repository.getAssetsByType(assetType);
								org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
								
								// check asset metadata, if specified
								System.out.println("PASSED: Assets by Type");
								AssetMetadataTest amt = new AssetMetadataTest(nextAsset,assetElement,(new Integer(i)).toString());
							}
						}
					} 
				} catch (java.lang.NullPointerException npe) {
				} catch (org.osid.id.IdException iex) {
				}
			}
		}
	}
}