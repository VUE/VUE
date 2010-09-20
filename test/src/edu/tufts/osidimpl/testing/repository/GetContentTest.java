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
import junit.framework.TestSuite;
import junit.framework.Test;

public class GetContentTest extends TestCase
{
	public GetContentTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		try {
			// are there assets to test for?
			String assetIdString = null;
			org.w3c.dom.NodeList assetsNodeList = document.getElementsByTagName(OsidTester.CONTENT_GET_TAG);
			int numTests = assetsNodeList.getLength();
			for (int i=0; i < numTests; i++) {
				org.w3c.dom.Element assetElement = (org.w3c.dom.Element)assetsNodeList.item(i);
				assetIdString = assetElement.getAttribute(OsidTester.ASSET_ID_ATTR);
				if ((assetIdString != null) && (assetIdString.trim().length() > 0)) {
					org.osid.shared.Id assetId = Utilities.getIdManager().getId(assetIdString);
					org.osid.repository.Asset asset = repositoryManager.getAsset(assetId);
					
					String classname = assetElement.getAttribute(OsidTester.CLASS_ATTR);
					Class c = Class.forName(classname);
					ContentGetter cg = (ContentGetter)c.newInstance();
					
					cg.test(asset.getContent());
					System.out.println("PASSED Get Content " + assetIdString);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Get Content Failed");
		}
	}
}