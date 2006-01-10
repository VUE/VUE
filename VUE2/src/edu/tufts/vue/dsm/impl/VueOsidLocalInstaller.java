package edu.tufts.vue.dsm.impl;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

public class VueOsidLocalInstaller
implements edu.tufts.vue.dsm.OsidLocalInstaller
{
	private static edu.tufts.vue.dsm.OsidLocalInstaller installer = new VueOsidLocalInstaller();

	public static edu.tufts.vue.dsm.OsidLocalInstaller getInstance() {
		return installer;
	}
	
	private VueOsidLocalInstaller() {
		
	}

	/**
		Copies the input stream to the OSID download directory and gives it the filename.
		If the plugin has its own installer, it will do whatever it needs to, perhaps
	    extracting files to various locations and making additions to Extensions.XML.
	*/
	public void installPlugin(String filename,
							  java.io.InputStream in) {
		try {
			java.io.File file = new java.io.File(edu.tufts.vue.util.Utilities.getOsidDownloadDirectory() + filename);
			java.io.OutputStream out = new java.io.FileOutputStream(file);
			
			try
			{
				int i = 0;
				while ( (i = in.read()) != -1 )
				{
					out.write(i);
				}
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
			in.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	*/
	public void deinstallPlugin(edu.tufts.vue.dsm.DataSource dataSource) {
		
	}
}
