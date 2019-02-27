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
package edu.tufts.vue.util;

public class Utilities
{
	private static String DATE_FORMAT = tufts.vue.VueResources.getString("dataSourceProviderDataFormat");
	private static java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
	private static String INSTALL_DIRECTORY_TOP_LEVEL = "/Library";
	private static String INSTALL_DIRECTORY_ROOT = "/Library/OSID";
	private static String INSTALL_DIRECTORY_UPLOADS = "/Library/OSID Uploads";
	private static String INSTALL_DIRECTORY_DOWNLOADS = "/Library/OSID Downloads";
	private static String INSTALL_DIRECTORY_COMMON = "/Library/OSID/Common";
	private static String INSTALL_DIRECTORY_RESOURCES = "/Library/OSID/Resources";
	
	public static org.osid.shared.Id getRepositoryIdFromLoadKey(String loadKey) {
		try {
			int index = loadKey.indexOf("@");
			//String managerString = loadKey.substring(0,index);
			String repositoryString = loadKey.substring(index+1);
			org.osid.id.IdManager idManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance();
			org.osid.shared.Id repositoryId = idManager.getId(repositoryString);
			return repositoryId;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to parse key " + loadKey);
		}
		return null;
	}
	
	public static String getManagerStringFromLoadKey(String loadKey) {
		try {
			int index = loadKey.indexOf("@");
			String managerString = loadKey.substring(0,index);
			return managerString;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to parse key " + loadKey);
		}
		return null;
	}

	public static java.awt.Image getImageFromReference(String imageFilename) {
		if (imageFilename != null) {
			String filename = System.getProperty("user.home") + INSTALL_DIRECTORY_RESOURCES + imageFilename;
			try {
				return (new javax.swing.ImageIcon(filename)).getImage();
			}catch (Throwable t) {
				edu.tufts.vue.util.Logger.log(t,"Trying to load image " + filename);
			}
		}
		return null;
	}

	public static org.osid.shared.Type stringToType(String typeString) {
		String authority = "_";
		String domain = "_";
		String keyword = "_";
		try {
			if (typeString != null) {
				int indexSlash = typeString.indexOf("/");
				if (indexSlash != -1) {
					domain = typeString.substring(0,indexSlash);
					int indexAt = typeString.indexOf("@");
					if (indexAt != -1) {
						keyword = typeString.substring(indexSlash+1,indexAt);
						authority = typeString.substring(indexAt+1);
					}
				}
			}
		} catch (Throwable t) {
			// ignore formatting error
		}
		return new Type(authority,domain,keyword);
	}
	
	public static String typeToString(org.osid.shared.Type type) {
		return type.getDomain() + "/" + type.getKeyword() + "@" + type.getAuthority();
	}
	
	public static String dateToString(java.util.Date date) {
		try {
			return (sdf.format(date,new StringBuffer(),new java.text.FieldPosition(0))).toString();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to convert date to string " + date);
		}
		return null;
	}
	
	public static java.util.Date stringToDate(String dateString) {
		try {
			return sdf.parse(dateString,new java.text.ParsePosition(0));
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to convert date string to date " + dateString);
			return null;
		}
	}
	
	public static void makeOsidDirectories() {
		try {
			String home = System.getProperty("user.home");
			String directories[] = new String[6];
			directories[0] = home + INSTALL_DIRECTORY_TOP_LEVEL;
			directories[1] = home + INSTALL_DIRECTORY_ROOT;
			directories[2] = home + INSTALL_DIRECTORY_UPLOADS;
			directories[3] = home + INSTALL_DIRECTORY_DOWNLOADS;
			directories[4] = home + INSTALL_DIRECTORY_COMMON;
			directories[5] = home + INSTALL_DIRECTORY_RESOURCES;
			
			for (int i=0; i < directories.length; i++) {
				java.io.File file = new java.io.File(directories[i]);
				//System.out.println(directories[i]);
				if ( !(file.exists()) ) {
					//System.out.println("making directory");
					file.mkdir();
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to create Osid directories");
		}
	}
	
	public static String getOsidUploadDirectory() {
		return System.getProperty("user.home") + INSTALL_DIRECTORY_UPLOADS;
	}
	
	public static String getOsidDownloadDirectory() {
		return System.getProperty("user.home") + INSTALL_DIRECTORY_DOWNLOADS;
	}
	
	public static String getOsidDirectory() {
		return System.getProperty("user.home") + INSTALL_DIRECTORY_ROOT;
	}
}