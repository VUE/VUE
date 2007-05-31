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
package edu.tufts.osidimpl.testing.sakai;

public class UpdateContentHelper implements edu.tufts.osidimpl.testing.repository.ContentUpdater
{
	public java.io.Serializable getSerializableObject()
	{
		try {
			// upload an object
			System.out.println("Preparing to upload");
			edu.tufts.osidimpl.repository.sakai.SakaiContentObject obj = new edu.tufts.osidimpl.repository.sakai.SakaiContent();
			obj.setDisplayName("Giunti Image");
			obj.setDescription("Giunti Logo JPEG");
			obj.setMIMEType("image/jpg");
			
			// convert file to byte array so it can later be converted to a Base64 String for Sakai to accept via web service
			java.io.File testFile= new java.io.File ("./giunti_logo.jpg");
			java.io.FileInputStream inStream = new java.io.FileInputStream (testFile);
			java.io.DataInputStream inData = new java.io.DataInputStream (inStream);
			int size = inData.available();
			byte[] data = new byte[size];
			if (inData.read(data) != size) {
				 System.out.println ("Error on reading file ");
				 return null;
			}
			obj.setBytes(data);
			return obj;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public boolean testAssetAfterUpload(org.osid.repository.RepositoryManager repositoryManager,org.osid.repository.Asset asset)
	{
		// we could check something, but for now do nothing
		return true;
	}
}
