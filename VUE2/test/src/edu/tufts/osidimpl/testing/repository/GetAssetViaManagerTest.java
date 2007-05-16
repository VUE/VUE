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
					
					// upload an object
					System.out.println("Preparing to upload");
					edu.tufts.osidimpl.repository.sakai.SakaiContentObject obj = new edu.tufts.osidimpl.repository.sakai.SakaiContent();
					obj.setDisplayName("bss jpeg1");
					obj.setDescription("bss image");
					obj.setMIMEType("image/jpg");
					
					// convert file to byte array so it can later be converted to a Base64 String for Sakai to accept via web service
					java.io.File file = new java.io.File("giunti_logo.jpg");
					java.io.FileInputStream inStream = new java.io.FileInputStream(file);
					java.io.DataInputStream inData = new java.io.DataInputStream(inStream);
					int size = inData.available();
					byte[] data = new byte[size];
					if (inData.read(data) != size) {
						throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
					}
					obj.setBytes(data);

					asset.updateContent(obj);
					System.out.println("Done uploading");
/*					
					System.out.println("Prepare to download");
					obj = (edu.tufts.osidimpl.repository.sakai.SakaiContentObject)(asset.getContent()); 
					java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(obj.getBytes());
					java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(obj.getDisplayName()));
					int i = 0;
					try {
						while (i != -1) {
							i = in.read(block, 0, 4096);
							out.write(block, 0, 4096);
							System.out.print(".");
							if (i == -1) {
								out.flush();
								System.out.println();
							}
						}
					} catch (java.io.IOException ex) {
						out.flush();
					}
					System.out.println("Done downloading");
*/				
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