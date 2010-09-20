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

public class GetContentHelper implements edu.tufts.osidimpl.testing.repository.ContentGetter
{
	public boolean test(java.io.Serializable serializableObject)
	{
		try {
			System.out.println("Prepare to download");
			edu.tufts.osidimpl.repository.sakai.SakaiContentObject obj = (edu.tufts.osidimpl.repository.sakai.SakaiContentObject)serializableObject; 
			java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(obj.getBytes());
			java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(obj.getDisplayName()));
			byte block[] = new byte[4096];
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
		} catch (Throwable t) {
			return false;
		}
		return true;
	}
}
