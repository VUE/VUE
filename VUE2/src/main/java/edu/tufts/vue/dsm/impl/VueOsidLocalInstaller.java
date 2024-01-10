/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.tufts.vue.dsm.impl;

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
