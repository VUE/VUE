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

package tufts.vue;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import javax.swing.*;
import javax.swing.border.*;

import javax.swing.JColorChooser;

import org.apache.commons.io.FilenameUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import tufts.vue.VueResources;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.action.OpenAction;

import org.apache.commons.io.*;

/**
 *
 * Various static utility methods for VUE.
 *
 * @version $Revision: 1.110 $ / $Date: 2010-05-21 18:44:08 $ / $Author: brian $
 * @author Scott Fraize
 * @author Helen Oliver, Imperial College London revisions added & initialled 2010-2012
 *
 */
public class VueUtil extends tufts.Util
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueUtil.class);
    
    public static final String DEFAULT_WINDOWS_FOLDER = "vue_2";
    public static final String DEFAULT_MAC_FOLDER = ".vue_2";
    public static final String VueExtension = VueResources.getString("vue.extension", ".vue");
    public static final String VueArchiveExtension = VueResources.getString("vue.archive.extension", ".vpk");
    // HO 12/01/2012 BEGIN ***********
    public static final String VueLockExtension = VueResources.getString("vue.lock.extension", ".vle");
    public static final String VueArchiveLockExtension = VueResources.getString("vue.archive.lock.extension", ".vlk");
    // HO 12/01/2012 END *************
    private static String currentDirectoryPath = "";
    
    // HO 24/02/2012 BEGIN ********
    // string manipulation constants
	private static String strPossPrefix = "file:";
	private static String strBackSlashPrefix = "\\\\";
	private static String strBackSlash = "\\";
	private static String strForwardSlashPrefix = "////";
	private static String strForwardSlash = "/";
	// I know. Don't say it.
	private static String strPossPrefixPlusForwardSlash = strPossPrefix + strForwardSlash;	
	private static String strFileProtocol = strPossPrefixPlusForwardSlash + strForwardSlash + strForwardSlash;
	private static String theColon = ":";
    // HO 24/02/2012 END **********
    
    public static void openURL(String platformURL)
        throws java.io.IOException
    {
        boolean isMailto = false;
        String logURL = platformURL;
        
        if (platformURL != null && platformURL.startsWith("mailto:")) {
            isMailto = true;
            if (platformURL.length() > 80) {
                // in case there's a big subject or body (e.g, ?subject=Foo&body=Bar in the URL), don't log the whole thing
                logURL = platformURL.substring(0,80) + "...";
            }
            Log.info("openURL[" + logURL + "]");
        } else
            Log.debug("openURL[" + logURL + "]");

        if (isMacPlatform() && VUE.inNativeFullScreen())
            tufts.vue.gui.FullScreen.dropFromNativeToWorking();
        else if (isUnixPlatform() && VUE.inNativeFullScreen())
        		tufts.vue.gui.FullScreen.dropFromNativeToFrame();
        // todo: spawn this in another thread just in case it hangs
        
        if (!isMailto) {
            String lowCaseURL = platformURL.toLowerCase();
                     
            if (lowCaseURL.endsWith(VueExtension) ||
                lowCaseURL.endsWith(VueArchiveExtension) ||
                lowCaseURL.endsWith(".zip") ||
                (DEBUG.Enabled && lowCaseURL.endsWith(".xml")))
            {
                if (lowCaseURL.startsWith("resource:")) {
                    // Special case for startup.vue which can be embedded in the classpath
                    java.net.URL url = VueResources.getURL(platformURL.substring(9));
                    VUE.displayMap(tufts.vue.action.OpenAction.loadMap(url));
                    return;
                }

                final File file = Resource.getLocalFileIfPresent(platformURL);

                if (file != null) {
                	// HO 02/03/2012 BEGIN **********
        			//VueUtil.alert("local file wasn't null, about to display map " + platformURL, "Progress");
        			// HO 02/03/2012 END ********** 
                    // TODO: displayMap should be changed to take either a URL or a random url/path spec-string,
                    // NOT a local file, as we can open maps at the other end of HTTP url's, and we need an
                    // object that abstracts both.
                    tufts.vue.VUE.displayMap(file);
                 // HO 02/03/2012 BEGIN **********
        			//VueUtil.alert("local file wasn't null, just displayed map " + platformURL, "Progress");
        			// HO 02/03/2012 END ********** 
                } else {
                	// HO 02/03/2012 BEGIN **********
                	LWMap theMap = null;
        			//VueUtil.alert("local file was null, about to create URL " + platformURL, "Progress");
        			java.net.URL newURL = null;
        			try {
        				newURL = new java.net.URL(platformURL);     
        			} catch (MalformedURLException e) {
        				//VueUtil.alert("Ohnoes, it's a MalformedURLException!", "Progress");
        			}
        			//VueUtil.alert("local file was null, just created URL " + platformURL, "Progress");
        			if (newURL != null) {
        				//VueUtil.alert("local file was null, about to load map " + newURL, "Progress");
        				theMap = tufts.vue.action.OpenAction.loadMap(newURL);
        				//VueUtil.alert("local file was null, just loaded map " + newURL, "Progress");
        			}
        			final LWMap loadMap = theMap;	
        			//final LWMap loadMap = tufts.vue.action.OpenAction.loadMap(new java.net.URL(platformURL));
        			// HO 02/03/2012 END ********** 
                 // HO 02/03/2012 BEGIN **********
        			//VueUtil.alert("local file was null, just loaded map " + loadMap, "Progress");
        			// HO 02/03/2012 END ********** 
        			// HO 02/03/2012 BEGIN **********
        			//VueUtil.alert("local file was null, about to display map " + loadMap, "Progress");
        			if (loadMap != null) {
        			// HO 02/03/2012 END ********** 
        				tufts.vue.VUE.displayMap(loadMap);
                        // HO 02/03/2012 BEGIN **********
            			//VueUtil.alert("local file was null, just displayed map " + loadMap, "Progress");
            			// HO 02/03/2012 END ********** 
            			// HO 02/03/2012 BEGIN **********
            			//VueUtil.alert("local file was null, setting file to null " + loadMap, "Progress");
            			// HO 02/03/2012 END ********** 
                        loadMap.setFile(null);
                     // HO 02/03/2012 BEGIN **********
            			//VueUtil.alert("local file was null, just set file to null " + loadMap, "Progress");
            			// HO 02/03/2012 END ********** 
        			}
        			else {
        				// HO 02/03/2012 BEGIN **********
            			//VueUtil.alert("loadMap was null", "Progress");
            			// HO 02/03/2012 END ********** 
        			}
        				
                }
                
                return;
            }
        }

        if (VUE.isApplet()) {
            java.net.URL url = null;
            try {
                url = new java.net.URL(platformURL);
                System.out.println("Applet URL display: " + url);
                VUE.getAppletContext().showDocument(url, "_blank");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            // already handled in Util.openURL
            //if (isMacPlatform() && platformURL.startsWith("/"))
            //    platformURL = "file:" + platformURL;
        	// HO 02/03/2012 BEGIN **********
			//VueUtil.alert("about to open platformURL " + platformURL, "Progress");
			// HO 02/03/2012 END **********    
            tufts.Util.openURL(platformURL);
         // HO 02/03/2012 BEGIN **********
			//VueUtil.alert("just opened platformURL " + platformURL, "Progress");
			// HO 02/03/2012 END ********** 
        }
    }
    
	/**

	* @param root_dir, the top of the file structure where we first started looking for the file
	* @param top_level_dir, the parent directory we are searching in now

	* @param file_to_search, the file we are searching for: filtered
	* to be a .vue or .vpk file

	* @return a File of the desired name if it is found,
	* without checking to make sure the target node is inside
	* @author Helen Oliver

	*/
	public static File lazyTargetFileExistInPath(String root_dir, String top_level_dir, String file_to_search) {
		String strRootDir = root_dir;
		int depth_limit = 6;
		File theFile = null;
		// get the files in the current directory
		File f = new File(top_level_dir);	
		File[] dir = f.listFiles(appropriateFilter(file_to_search));
		// if we have a list of files, cycle through them
		if (dir != null) {	
			for (int i = 0; i < dir.length; i++) {
				File file_test = dir[i];
				// don't waste time on Subversion folders
				if (".svn".equals(dir[i].getName().toString())) {
					continue;
				}
				// don't waste time on the Trash folder
				if (".Trash".equals(dir[i].getName().toString())) {
					continue;
				}
				if (file_test.isFile()) {	
					if (file_test.getName().equals(file_to_search)) {
						System.out.println("File Name :" + file_test);
						theFile = file_test;
						break;
					}
				} else if(file_test.isDirectory()){	
					// limit number of levels we go below the directory structure
					int count;
					File countFile = file_test;
					for (count = 0; count <=depth_limit; count++) {
						String stepUp = countFile.getParent();
						if (stepUp.equals(strRootDir)) {
							break;
						} else {
							countFile = new File(stepUp);
						}
					}
					if (count < (depth_limit+1)) { 
						theFile = lazyTargetFileExistInPath(strRootDir, file_test.getAbsolutePath(), file_to_search);
						if (theFile != null)
							break;
					} else {
						continue;
					} 
				}
			}
		} else {	
			System.out.println("null list of files");
		}	
		return theFile;

	}
	
	/**

	* A function to look up to a certain number (6, but you can change this if you want)
	* of folders above the current folder for a file of a certain name.
	* It doesn't look in the subfolders. If it finds a file with the desired name,
	* it doesn't look a gift horse in the mouth, it just assumes it's the right one.
	* @param base_dir, the bottom of the file structure where we first started looking for the file
	* @param next_level_dir, the parent directory we are searching in now
	* @param file_to_search, the file we are searching for: filtered
	* to be a .vue or .vpk file
	* @param compString, the UURI of the target component
	* @return LWMap, the map if we find the one with the target component, null otherwise
	* @author Helen Oliver

	*/
	public static File lazyTargetFileExistAbovePath(String base_dir, String next_level_dir, String file_to_search) {
		String strBaseDir = base_dir;

		int height_limit = 6;
		LWMap theMap = null;
		
		File theFile = null;
		
		File f = new File(next_level_dir);
		
		for (int j = 0; j < height_limit; j++) {
			if (f != null) {
				// get the files in this directory
				File[] dir = f.listFiles(appropriateFilterNoDirectories(file_to_search));
				// if we have a list of files, cycle through them
				if (dir != null) {	
					for (int i = 0; i < dir.length; i++) {
						File file_test = dir[i];
						// don't waste time on Subversion folders
						if (".svn".equals(dir[i].getName().toString())) {
							continue;
						}
						// don't waste time on the Trash folder
						if (".Trash".equals(dir[i].getName().toString())) {
							continue;
						}
						if (file_test.isFile()) {	
							if (file_test.getName().equals(file_to_search)) {
								// we found it, or at least its doppelganger
								System.out.println("File Name :" + file_test);
								theFile = file_test;
								break;
							}
						} 
					}
				} else {	
					System.out.println("null list of files");
				}
								
			}
			if (f == null)
				break;
			
			f = f.getParentFile();
		}
		return theFile;

	}
    
	/**

	* @param root_dir, the top of the file structure where we first started looking for the file
	* @param top_level_dir, the parent directory we are searching in now

	* @param file_to_search, the file we are searching for: filtered
	* to be a .vue or .vpk file

	* @param compString, the UURI of the target component

	* @return LWMap, the map if we find the one with the target component, null otherwise
	* @author Helen Oliver

	*/
	// HO 09/08/2011 BEGIN **********
	public static LWMap targetFileExistInPath(String root_dir, String top_level_dir, String file_to_search, String compString) {
		String strRootDir = root_dir;
		// HO 09/08/2011 END **********
		int depth_limit = 6;
		LWMap theMap = null;
		// get the files in the current directory
		File f = new File(top_level_dir);	
		// HO 22/02/2012 BEGIN *********
		// File[] dir = f.listFiles();	
		File[] dir = f.listFiles(appropriateFilter(file_to_search));
		// HO 22/02/2012 END *********
		// if we have a list of files, cycle through them
		if (dir != null) {	
			for (int i = 0; i < dir.length; i++) {
				File file_test = dir[i];
				// HO 09/08/2011 BEGIN *******
				// don't waste time on Subversion folders
				if (".svn".equals(dir[i].getName().toString())) {
					continue;
				}
				// don't waste time on the Trash folder
				if (".Trash".equals(dir[i].getName().toString())) {
					continue;
				}
				// HO 09/08/2011 END *********
				if (file_test.isFile()) {	
					if (file_test.getName().equals(file_to_search)) {
						System.out.println("File Name :" + file_test);
						//v.add(top_level_dir);	
						//v.add(new File(top_level_dir, file_test.getName()));
						theMap = checkIfMapContainsTargetNode(file_test, compString);
						if (theMap != null) 
							break;
					}
				} else if(file_test.isDirectory()){	
					// HO 09/08/2011 BEGIN *********
					// limit number of levels we go below the directory structure
					int count;
					File countFile = file_test;
					for (count = 0; count <=depth_limit; count++) {
						String stepUp = countFile.getParent();
						if (stepUp.equals(strRootDir)) {
							break;
						} else {
							countFile = new File(stepUp);
						}
					}
					if (count < (depth_limit+1)) { 
						// HO 09/08/2011 END ***********
						theMap = targetFileExistInPath(strRootDir, file_test.getAbsolutePath(), file_to_search, compString);
						if (theMap != null)
							break;
					} else {
						continue;
					} 
				}
			}
		} else {	
			System.out.println("null list of files");
		}	
		return theMap;

	}
	
	/**

	* @param base_dir, the bottom of the file structure where we first started looking for the file
	* @param next_level_dir, the parent directory we are searching in now

	* @param file_to_search, the file we are searching for: filtered
	* to be a .vue or .vpk file

	* @param compString, the UURI of the target component

	* @return LWMap, the map if we find the one with the target component, null otherwise
	* @author Helen Oliver

	*/
	// HO 09/08/2011 BEGIN **********
	public static LWMap targetFileExistAbovePath(String base_dir, String next_level_dir, String file_to_search, String compString) {
		String strBaseDir = base_dir;
		// HO 09/08/2011 END **********
		int height_limit = 6;
		LWMap theMap = null;
		
		File f = new File(next_level_dir);
		
		for (int j = 0; j < height_limit; j++) {
			if (f != null) {
				// get the files in this directory
				File[] dir = f.listFiles(appropriateFilterNoDirectories(file_to_search));
				// HO 22/02/2012 END *********
				// if we have a list of files, cycle through them
				if (dir != null) {	
					for (int i = 0; i < dir.length; i++) {
						File file_test = dir[i];
						// HO 09/08/2011 BEGIN *******
						// don't waste time on Subversion folders
						if (".svn".equals(dir[i].getName().toString())) {
							continue;
						}
						// don't waste time on the Trash folder
						if (".Trash".equals(dir[i].getName().toString())) {
							continue;
						}
						// HO 09/08/2011 END *********
						if (file_test.isFile()) {	
							if (file_test.getName().equals(file_to_search)) {
								System.out.println("File Name :" + file_test);
								//v.add(top_level_dir);	
								//v.add(new File(top_level_dir, file_test.getName()));
								theMap = checkIfMapContainsTargetNode(file_test, compString);
								if (theMap != null) 
									break;
							}
						} 
					}
				} else {	
					System.out.println("null list of files");
				}
								
			}
			// HO 06/03/2012 BEGIN *******
			if (theMap != null)
				break;
			// HO 06/03/2012 END ********
			if (f == null)
				break;
			
			f = f.getParentFile();
		}
		return theMap;

	}
	
    /**
     * Convenience class to return a filename filter for a specific file.
     * @author Helen Oliver
     *
     */
	public static class SpecificFilenameFilter implements FilenameFilter {
  		String specificName = "";
  		
  		public SpecificFilenameFilter(String strSpecificName) { 
  			this.specificName = strSpecificName; 
  		} 

  	  public boolean accept(File pathname, String strFilename) {  		
  	    if (strFilename.equals(specificName))
  	    	return true;
  	    
  	    // make sure it's not a matter of mixed encodings
  	    strFilename = decodeURIStringToString(strFilename);
  	    if (strFilename.equals(specificName))
  	    	return true;
  	    
  	    return false;
  	  }
  	}
	
	
    /**
     * Convenience class to return a file filter for either
     * a .vpk file or a directory.
     * @author Helen Oliver
     *
     */
	public static class VueArchiveFileOrDirectoryFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.VueArchiveExtension)) 
  	      return true;
  	    else if (pathname.isDirectory())
  	    	return true;
  	    
  	    return false;
  	  }
  	}
	
    /**
     * Convenience class to return a file filter for a .vue file only,
     * no frills.
     * @author Helen Oliver
     *
     */
	public static class VueFileOnlyFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.VueExtension)) 
  	      return true;
  	    
  	    return false;
  	  }
  	}
	
    /**
     * Convenience class to return a file filter for a .vpk file only,
     * no frills.
     * @author Helen Oliver
     *
     */
	public static class VueArchiveFileOnlyFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.VueArchiveExtension)) 
  	      return true;
  	    
  	    return false;
  	  }
  	}
	
  
    /**
     * Convenience class to return a file filter for either
     * a .vue file or a directory.
     * @author Helen Oliver
     *
     */
	public static class VueFileOrDirectoryFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.VueExtension)) 
  	      return true;
  	    else if (pathname.isDirectory())
  	    	return true;
  	    
  	    return false;
  	  }
  	}
	
    /**
     * A function to return the right kind of file extension
     * filter for a given file.
     * @param strFileName, the filename String for which we need the right extension filter
     * @return either a filter for a .vue file,
     * or a filter for a .vpk file,
     * according to the file type
     * @author Helen Oliver
     */
    public static FileFilter appropriateFilterNoDirectories(String strFileName) {
    	if ((strFileName == null) || (strFileName == ""))
    		return null;
    	
    	if (strFileName.endsWith(VueUtil.VueExtension))
    		return new VueFileOnlyFilter();
    	
    	else if (strFileName.endsWith(VueUtil.VueArchiveExtension))
    		return new VueArchiveFileOnlyFilter();
    	
    	return null;
    	
    }
	
    /**
     * A function to return the right kind of file extension
     * filter for a given file, also including directories.
     * @param strFileName, the filename String for which we need the right extension filter
     * @return either a filter for a .vue file or a directory,
     * or a filter for a .vpk file or a directory,
     * according to the file type
     * @author Helen Oliver
     */
    public static FileFilter appropriateFilter(String strFileName) {
    	if ((strFileName == null) || (strFileName == ""))
    		return null;
    	
    	if (strFileName.endsWith(VueUtil.VueExtension))
    		return new VueFileOrDirectoryFilter();
    	
    	else if (strFileName.endsWith(VueUtil.VueArchiveExtension))
    		return new VueArchiveFileOrDirectoryFilter();
    	
    	return null;
    	
    }	
	
	/**
	 * A function to check whether a given Map file
	 * contains the right target node, identified by
	 * the UURI string.
	 * @param f, the File to check for the presence of the target node.
	 * @param compString, the UURI string identifying the target node.
	 * @return the LWMap that contains the target node if it's found,
	 * null if it's not.
	 * @author Helen Oliver
	 */
	public static LWMap checkIfMapContainsTargetNode(File f, String compString) {
    	if (f == null)
    		return null;
    	
    	LWMap targMap = null;
    	
    	String s = f.getAbsolutePath();
    	LWMap map = OpenAction.loadMap(s);
    	// HO 28/02/2012 BEGIN ********
    	// this can return null because of
    	// backward compatibility issues
    	if (map == null)
    		return null;
    	// HO 28/02/2012 END **********
		// see if this is the right map
    	// by looking to see if the target component is in it
		LWComponent targetComp = map.findChildByURIString(compString);
		// if we found it, this is the right map
		if (targetComp != null) {
			targMap = map;
		}
		
		return targMap;
	}
    
	// HO 13/02/2012 BEGIN *********
	/**
	 * A function to take in a filename String with spaces,
	 * and replace them with HTML-encoded spaces
	 * @param encodeThis, a String representing a filename that has its spaces
	 * without HTML codes
	 * @return the same String, single spaces replaced with html space codes 
	 * @author Helen Oliver
	 */
	public static String replaceHtmlSpaceCodes(String encodeThis) {
		String strEncoded = "";
		String strPeskySpace = " ";
		String strEncodedSpace = "%20";

		strEncoded = encodeThis.replaceAll(strPeskySpace, strEncodedSpace);
		
		return strEncoded;		
	}
	
	/**
	 * A function to get the URI of the parent path of a map.
	 * @param theMap, the LWMap for which to find the parent URI.
	 * @return the URI of the parent path of the map.
	 * @author Helen Oliver
	 */
	public static URI getParentURIOfMap(LWMap theMap) {
		// input validation
		if (theMap == null)
			return null;
		
		// if the map actually has a file,
		// get its parent path
		String strParent = "";
		URI parentURI = null;
		
		// as long as the map has a file, get its parent path
		if (theMap.getFile() != null) {
			strParent = new File(theMap.getFile().getAbsolutePath()).getParent();
		}
		// if the file has a parent path, turn it into a URI
		if ((strParent != null) && (strParent != ""))	{
			// make sure spaces are replaced with HTML codes	
			// HO 24/02/2012 BEGIN *******
				parentURI = VueUtil.getURIFromString(strParent);
			}
		
		return parentURI;
	}
	
    /** 
     * A function to strip the file protocol from a String
     * representing a file path.
     * @param strToStrip, the String from which to remove the file protocol.
     * @return the String stripped of the file protocol.
     * @author Helen Oliver
     */
	public static String stripFilePrefixFromPathString(String strToStrip) {
		
		// trim putative gubbins away
		if (strToStrip.startsWith(strFileProtocol))
			strToStrip = strToStrip.substring(strFileProtocol.length(), strToStrip.length());
		
		if (strToStrip.startsWith(strPossPrefixPlusForwardSlash))
			strToStrip = strToStrip.substring(strPossPrefixPlusForwardSlash.length(), strToStrip.length());
		
		if (strToStrip.startsWith(strPossPrefix))
			strToStrip = strToStrip.substring(strPossPrefix.length(), strToStrip.length());
    	
    	return strToStrip;
    }
	
	/**
	 * A function to work out if a String, representing a file path
	 * to be turned into a URL, needs to have the file protocol
	 * prepended to it.
	 * @param theString, the String representing a file path
	 * to be turned into a URL
	 * @return the String with the file protocol prepended to it
	 * if appropriate.
	 * @author Helen Oliver
	 */
	public static String prependFileProtocol(String theString) {
		// input validation
		if ((theString == null) || (theString == ""))
			return null;
		
		String secondChar = theString.substring(1,2);
		
		// if there's a colon as the second character of the string,
		// it probably needs a file protocol prepending to it.
		if (theColon.equals(secondChar)) {
			theString = strFileProtocol + theString;
		}
		
		return theString;		
	}
	
	/**
	 * Convenience function to take in a String and return a String
	 * encoded to make it suitable for a URI
	 * @param s, the String to turn into a URI-encoded String
	 * @return a String encoded for URI, made from String s
	 * @author Helen Oliver
	 */
	public static String encodeStringForURI(String s) {
		// input validation
		if ((s == null) || (s == ""))
			return null;
		
		// make sure the string being encoded is "clean"
		// so characters don't get double-encoded
		s = decodeURIStringToString(s);

		s = prependFileProtocol(s);
		try {
			s = URLEncoder.encode(s, System.getProperty("file.encoding"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return s;
	}
	
	/**
	 * Convenience function to take in a String and return a URI
	 * @param s, the String to turn into a URI
	 * @return a URI made from String s
	 * @author Helen Oliver
	 */
	public static URI getURIFromString(String s) {
		// input validation
		if ((s == null) || (s == ""))
			return null;
		
		URI theURI = null;
		
		// HO 27/02/2012 BEGIN ******
		// make sure String is "clean" (decoded) 
		// before proceeding
		s = decodeURIStringToString(s);
		// HO 27/02/2012 END ********
		
		// HO 24/02/2012 BEGIN ******
		// replace spaces with HTML codes 
		// HO 27/02/2012 BEGIN *********
		s = prependFileProtocol(s);
		try {
			s = URLEncoder.encode(s, System.getProperty("file.encoding"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		//s = replaceHtmlSpaceCodes(s);
		
		boolean bMalformed = false;
		// HO 27/02/2012 END *********
		
		try {
				theURI = new URI(s);
				} catch (URISyntaxException e) {
					e.printStackTrace();
					return null;
				} catch (NullPointerException e) {
					e.printStackTrace();
					return null;
				} catch (IllegalArgumentException e){
					e.printStackTrace();
					return null;
				}
				
		return theURI;
	}
	
	// HO 27/02/2012 BEGIN ********	
	/**
	 * Convenience method to decode a string that was encoded for a URI and
	 * strip the file protocol from it, if need be.
	 * @param theString, the String to decode
	 * @return theString decoded and stripped of 
	 * its file protocol
	 * @author Helen Oliver
	 */
	public static String decodeURIStringToString(String theString) {
		// input validation
		if ((theString == null) || (theString == ""))
			return null;
		
				
		try {
			theString = URLDecoder.decode(theString, System.getProperty("file.encoding"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		theString = VueUtil.stripFilePrefixFromPathString(theString);
		// HO 27/02/2012 END ********
		
		return theString;
	}
	
	/**
	 * A function to resolve the URI of the target file
	 * relative to the source file.
	 * @param targetURI, the (presumed relative) URI of the target file
	 * @return a File in a location relative to the source file
	 * @author Helen Oliver
	 */
	public static File resolveTargetRelativeToSource(URI targetURI, URI sourceParent) {
		// input validation
		if (targetURI == null)
			return null;
		
		// the target File object, we hope
		File targFile = null;
		
		// HO 16/02/2012 BEGIN ************
		// if that file can't be found, try resolving it relative
		// to the current source root
		// if the source map actually has a file,
		// get its parent path
		//URI sourceParent = getParentURIOfSourceMap();
		String strSourceParent = sourceParent.toString();
		// resolve the relativized target URI to the
		// root of the source map
		URI resolvedTargetParentURI = targetURI.resolve(strSourceParent);
		String strResolvedTargetParent = VueUtil.getStringFromURI(resolvedTargetParentURI);
		String strRelativeTarget = VueUtil.getStringFromURI(targetURI);
				
		// HO 20/02/2012 BEGIN PROBLEM HERE IS
		// THAT URI IS NOT ABSOLUTE
		// AND IT THROWS AN ILLEGALARGUMENTEXCEPTION
		//if ((targetURI != null) && (resolvedTargetURI != null)) {
			//strSourceParent = VueUtil.stripHtmlSpaceCodes(strSourceParent);
			
			targFile = new File(strResolvedTargetParent, strRelativeTarget);
		//}
		
		return targFile;
		
		// HO 16/02/2012 END ************
	}
	
	/**
	 * A convenience method to display a wait cursor
	 * while searching for the target file among the subfolders.
	 * @param root_dir, the top of the file structure where we first started looking for the file
	 * @param top_level_dir, a String representing the parent directory in an iteration
	 * @param file_to_search, a String representing the filename to search for
	 * @param compString, the target component URI string
	 * @return targMap, the LWMap if it was found, null otherwise
	 * @author Helen Oliver
	 */
	public static LWMap findTargetInSubfolders(String root_dir, String top_level_dir, String file_to_search, String compString) {
		// input validation
		if ((root_dir == null) || (root_dir == ""))
			return null;
		if ((top_level_dir == null) || (top_level_dir == ""))
			return null;
		if ((file_to_search == null) || (file_to_search == ""))
			return null;
		if ((compString == null) || (compString == ""))
			return null;
		
		LWMap targMap = null;
		
		MapViewer viewer = VUE.getActiveViewer();
		try {
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			targMap = VueUtil.targetFileExistInPath(root_dir, top_level_dir, file_to_search, compString);
		} finally {
			viewer.setCursor(Cursor.getDefaultCursor());
		}
		
		return targMap;
	}
	
	/**
	 * A convenience method to display a wait cursor
	 * while searching for the target file among the subfolders.
	 * If it finds a file with a matching name, it doesn't look a gift
	 * horse in the mouth.
	 * @param root_dir, the top of the file structure where we first started looking for the file
	 * @param top_level_dir, a String representing the parent directory in an iteration
	 * @param file_to_search, a String representing the filename to search for
	 * @return the target file if it was found, or null.
	 * @author Helen Oliver
	 */
	public static File lazyFindTargetInSubfolders(String root_dir, String top_level_dir, String file_to_search) {
		// input validation
		if ((root_dir == null) || (root_dir == ""))
			return null;
		if ((top_level_dir == null) || (top_level_dir == ""))
			return null;
		if ((file_to_search == null) || (file_to_search == ""))
			return null;
		
		File theFile = null;
		
		MapViewer viewer = VUE.getActiveViewer();
		try {
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			theFile = VueUtil.lazyTargetFileExistInPath(root_dir, top_level_dir, file_to_search);
		} finally {
			viewer.setCursor(Cursor.getDefaultCursor());
		}
		
		return theFile;
	}
	
	/**
	 * A convenience method to display a wait cursor
	 * while searching for the target file above the current path.
	 * @param root_dir, the top of the file structure where we first started looking for the file
	 * @param top_level_dir, a String representing the parent directory in an iteration
	 * @param file_to_search, a String representing the filename to search for
	 * @param compString, the target component URI string
	 * @return targMap, the LWMap if it was found, null otherwise
	 * @author Helen Oliver
	 */
	public static LWMap findTargetAboveCurrentPath(String root_dir, String top_level_dir, String file_to_search, String compString) {
		// input validation
		if ((root_dir == null) || (root_dir == ""))
			return null;
		if ((top_level_dir == null) || (top_level_dir == ""))
			return null;
		if ((file_to_search == null) || (file_to_search == ""))
			return null;
		if ((compString == null) || (compString == ""))
			return null;
		
		LWMap targMap = null;
		
		MapViewer viewer = VUE.getActiveViewer();
		try {
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			targMap = VueUtil.targetFileExistAbovePath(root_dir, top_level_dir, file_to_search, compString);
		} finally {
			viewer.setCursor(Cursor.getDefaultCursor());
		}
		
		return targMap;
	}
	
	/**
	 * A convenience method to display a wait cursor
	 * while searching for the target file above the current path.
	 * If it finds a file with a matching name, it doesn't look a gift horse in the mouth,
	 * just assumes that that's the right file.
	 * @param root_dir, the top of the file structure where we first started looking for the file
	 * @param top_level_dir, a String representing the parent directory in an iteration
	 * @param file_to_search, a String representing the filename to search for
	 * @return the File with the matching name, if one was found, null otherwise.
	 * @author Helen Oliver
	 */
	public static File lazyFindTargetAboveCurrentPath(String root_dir, String top_level_dir, String file_to_search) {
		// input validation
		if ((root_dir == null) || (root_dir == ""))
			return null;
		if ((top_level_dir == null) || (top_level_dir == ""))
			return null;
		if ((file_to_search == null) || (file_to_search == ""))
			return null;
		
		File theFile = null;
		
		MapViewer viewer = VUE.getActiveViewer();
		try {
			viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			theFile = VueUtil.lazyTargetFileExistAbovePath(root_dir, top_level_dir, file_to_search);
		} finally {
			viewer.setCursor(Cursor.getDefaultCursor());
		}
		
		return theFile;
	}
	
	/**
	 * A function to take a source LWMap and a target LWMap
	 * ad relativize the target's file path to the source's file path
	 * @param theSource, the source LWMap
	 * @param theTarget, the target LWMap
	 * @return a String representing the relativized target path
	 * @author Helen Oliver
	 */
	public static String relativizeTargetSpec(LWMap theSource, LWMap theTarget) {
		
		URI targetURI = theTarget.getFile().toURI();
		
		// HO 13/02/2012 BEGIN ********
		// try to relativize the target path based on the source path
		File sourceBaseFile = theSource.getFile();
		String strSourceBase = "";
		String relative = "";
		if (sourceBaseFile != null) {
			File sourceBase = sourceBaseFile.getParentFile();
			if (sourceBase != null) {
				strSourceBase = sourceBase.toString();
				relative = new File(strSourceBase).toURI().relativize(targetURI).getPath();
				System.out.println(relative);
			}
		}
		
		return relative;
		// HO 13/02/2012 END **********
	}
	
	// HO 02/03/2012 BEGIN ***********	
	/**
     * Get the relative path from one file to another, specifying the directory separator. 
     * If one of the provided resources does not exist, it is assumed to be a file unless it ends with '/' or
     * '\'.
     * 
     * @param target targetPath is calculated to this file
     * @param base basePath is calculated from this file
     * @param separator directory separator. The platform default is not assumed so that we can test Unix behaviour when running on Windows (for example)
     * @return
     */
    public static String getRelativePathByStringManipulation(String targetPath, String basePath, String pathSeparator) {

        // Normalize the paths
		String normalizedTargetPath = targetPath;
		String normalizedBasePath = basePath;
		try {
			// normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
			//normalizedTargetPath = FilenameUtils.normalize(targetPath);
		} catch (Exception e) {
			VueUtil.alert(e.getMessage(), "Normalized target pagh");
		}
		try {
			//normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);
		} catch (Exception e) {
			VueUtil.alert(e.getMessage(), "Normalized base path");
		}

        // Undo the changes to the separators made by normalization
        /* if (pathSeparator.equals("/")) {
        	// HO 02/03/2012 BEGIN **********
    		VueUtil.alert("about to change the target path separators to unix","Progress");
    		// HO 02/03/2012 END **********
            normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
         // HO 02/03/2012 BEGIN **********
    		VueUtil.alert("just changed the target path separators to unix","Progress");
    		// HO 02/03/2012 END **********
    		// HO 02/03/2012 BEGIN **********
    		VueUtil.alert("about to change the base path separators to unix","Progress");
    		// HO 02/03/2012 END **********
            normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);
         // HO 02/03/2012 BEGIN **********
    		VueUtil.alert("just changed the base path separators to unix","Progress");
    		// HO 02/03/2012 END **********

        } else if (pathSeparator.equals("\\")) {
        	// HO 02/03/2012 BEGIN **********
    		VueUtil.alert("about to change the target path separators to windows","Progress");
    		// HO 02/03/2012 END **********
            normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
         // HO 02/03/2012 BEGIN **********
    		VueUtil.alert("just changed the target path separators to windows","Progress");
    		// HO 02/03/2012 END **********
    		// HO 02/03/2012 BEGIN **********
    		VueUtil.alert("about to change the base path separators to windows","Progress");
    		// HO 02/03/2012 END **********
            normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);
         // HO 02/03/2012 BEGIN **********
    		VueUtil.alert("just changed the base path separators to windows","Progress");
    		// HO 02/03/2012 END **********

        } else {
            throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
        } */

        String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
        String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        StringBuffer common = new StringBuffer();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex])) {
            common.append(target[commonIndex] + pathSeparator);
            commonIndex++;
        }

		if (commonIndex == 0) {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativized.
            /*throw new PathResolutionException("No common path element found for '" + normalizedTargetPath + "' and '" + normalizedBasePath
                    + "'"); */
        	return "";
        }  

        // The number of directories we have to backtrack depends on whether the base is a file or a dir
        // For example, the relative path from
        //
        // /foo/bar/baz/gg/ff to /foo/bar/baz
        // 
        // ".." if ff is a file
        // "../.." if ff is a directory
        //
        // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
        // the resource referred to by this path may not actually exist, but it's the best I can do
        boolean baseIsFile = true;

        File baseResource = new File(normalizedBasePath);

        if (baseResource.exists()) {
            baseIsFile = baseResource.isFile();

        } else if (basePath.endsWith(pathSeparator)) {
            baseIsFile = false;
        }

        StringBuffer relative = new StringBuffer();

        if (base.length != commonIndex) {
            int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++) {
                relative.append(".." + pathSeparator);
            }
        }
        relative.append(normalizedTargetPath.substring(common.length()));
        return relative.toString();
    }


    static class PathResolutionException extends RuntimeException {
        PathResolutionException(String msg) {
            super(msg);
        }
    }    
    // HO 02/03/2012 END ***********		

	
	/**
	 * Convenience function to take in a URI and return a String
	 * @param u, the URI to turn into a String
	 * @return a String made from URI u
	 * @author Helen Oliver
	 */
	public static String getStringFromURI(URI u) {
		// input validation
		if (u == null) 
			return "";
		
		String theString = u.toString();
		
		// HO 27/02/2012 BEGIN ********		
		try {
			theString = URLDecoder.decode(theString, System.getProperty("file.encoding"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		theString = VueUtil.stripFilePrefixFromPathString(theString);
		// HO 27/02/2012 END ********
		
		// replace spaces with HTML codes 
		//theString = stripHtmlSpaceCodes(u.toString());
		
		return theString;
	}
	// HO 12/05/2011 BEGIN *********
	/**
	 * A function to take in a filename String, strip out the HTML-encoded
	 * spaces, and replace them with single spaces
	 * @param stripThis, a String representing a filename that has its spaces
	 * in the HTML format
	 * @return the same String, html space codes replaced with single spaces
	 * @author Helen Oliver
	 */
	public static String stripHtmlSpaceCodes(String stripThis) {
		String strStripped = "";
		String strPeskySpace = "%20";
		String strCleanSpace = " ";

		strStripped = stripThis.replaceAll(strPeskySpace, strCleanSpace);
		
		return strStripped;		
	}
	// HO 12/05/2011 END ***********
	// HO 13/02/2012 END ***********
    
    public static void  setCurrentDirectoryPath(String cdp) {
        currentDirectoryPath = cdp;
    }
    
    public static String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }    
    
    public static boolean isCurrentDirectoryPathSet() {
        if(currentDirectoryPath.equals("")) 
            return false;
        else
            return true;
    }

    public static File getDefaultUserFolder() {
    	
        File userHome = null;
        
        if (VUE.isApplet())
        	userHome = new File(VUE.getSystemProperty("user.home"));
        else
        {
        	String userHomeString = System.getenv("VUEUSERHOME");
        	
        	if (userHomeString ==null || (userHomeString !=null && userHomeString.length() <1))
            	userHome = new File(VUE.getSystemProperty("user.home"));
        	else
        		userHome = new File(userHomeString);
        }
    	
        if(userHome == null) 
            userHome = new File(VUE.getSystemProperty("java.io.tmpdir"));
        final String vueUserDir = isWindowsPlatform() ? DEFAULT_WINDOWS_FOLDER : DEFAULT_MAC_FOLDER;
        File userFolder = new File(userHome.getPath() + File.separatorChar + vueUserDir);
        if(userFolder.isDirectory())
            return userFolder;
        if(!userFolder.mkdir())
            throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be created");
        return userFolder;
    }
    
    public static void deleteDefaultUserFolder() {
        File userFolder = getDefaultUserFolder();
        File[] files = userFolder.listFiles();
        System.out.println("file count = "+files.length);
        for(int i = 0; i<files.length;i++) {
            if(files[i].isFile() && !files[i].delete()) 
                throw new RuntimeException(files[i].getAbsolutePath()+":cannot be created");
        }
        if(!userFolder.delete()) 
             throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be deleted");
    }

    public static void copyURL(java.net.URL url, java.io.File file)
        throws java.io.IOException
    {
        if (DEBUG.IO) out("VueUtil: copying " + url + " to " + file);
        copyStream(url.openStream(), new java.io.FileOutputStream(file));
    }
        
    public static void copyStream(java.io.InputStream in, java.io.OutputStream out)
        throws java.io.IOException
    {
        int len = 0;
        byte[] buf = new byte[8192];
        while ((len = in.read(buf)) != -1) {
            if (DEBUG.IO) out("VueUtil: copied " + len + " to " + out);
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Compute the intersection point of two lines, as defined
     * by two given points for each line.
     * This already assumes that we know they intersect somewhere (are not parallel), 
     */

    public static float[] computeLineIntersection
        (float s1x1, float s1y1, float s1x2, float s1y2,
         float s2x1, float s2y1, float s2x2, float s2y2, float[] result)
    {
        // We are defining a line here using the formula:
        // y = mx + b  -- m is slope, b is y-intercept (where crosses x-axis)
        
        final boolean m1vertical = (Math.abs(s1x1 - s1x2) < 0.001f);
        final boolean m2vertical = (Math.abs(s2x1 - s2x2) < 0.001f);
        final float m1;
        final float m2;

        if (!m1vertical)
            m1 = (s1y1 - s1y2) / (s1x1 - s1x2);
        else
            m1 = Float.NaN;
        
        if (!m2vertical)
            m2 = (s2y1 - s2y2) / (s2x1 - s2x2);
        else
            m2 = Float.NaN;
        
        // Solve for b using any two points from each line.
        // to solve for b:
        //      y = mx + b
        //      y + -b = mx
        //      -b = mx - y
        //      b = -(mx - y)
        // float b1 = -(m1 * s1x1 - s1y1);
        // float b2 = -(m2 * s2x1 - s2y1);
        // System.out.println("m1=" + m1 + " b1=" + b1);
        // System.out.println("m2=" + m2 + " b2=" + b2);

        // if EITHER line is vertical, the x value of the intersection
        // point will obviously have to be the x value of any point
        // on the vertical line.
        
        float x = 0;
        float y = 0;
        if (m1vertical) {   // first line is vertical
            //System.out.println("setting X to first vertical at " + s1x1);
            float b2 = -(m2 * s2x1 - s2y1);
            x = s1x1; // set x to any x point from the first line
            // using y=mx+b, compute y using second line
            y = m2 * x + b2;
        } else {
            float b1 = -(m1 * s1x1 - s1y1);
            if (m2vertical) { // second line is vertical (has no slope)
                //System.out.println("setting X to second vertical at " + s2x1);
                x = s2x1; // set x to any point from the second line
            } else {
                // second line has a slope (is not veritcal: m is valid)
                float b2 = -(m2 * s2x1 - s2y1);
                x = (b2 - b1) / (m1 - m2);
            }
            // using y=mx+b, compute y using first line
            y = m1 * x + b1;
        }
        //System.out.println("x=" + x + " y=" + y);

        result[0] = x;
        result[1] = y;
        return result;
    }

    public static final float[] NoIntersection = { Float.NaN, Float.NaN, Float.NaN, Float.NaN };
    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" }; // for debug
    
    public static float[] computeIntersection(float rayX1, float rayY1,
                                              float rayX2, float rayY2,
                                              java.awt.Shape shape, java.awt.geom.AffineTransform shapeTransform)
    {
        return computeIntersection(rayX1,rayY1, rayX2,rayY2, shape, shapeTransform, new float[2], 1);
    }
    
    public static Point2D.Float computeIntersection(Line2D.Float l, LWComponent c) {
        float[] p = computeIntersection(l.x1, l.y1, l.x2, l.y2, c.getZeroShape(), c.getZeroTransform(), new float[2], 1);
        return new Point2D.Float(p[0], p[1]);
    }
    public static float[] computeIntersection(float segX1, float segY1, float segX2, float segY2, LWComponent c) {
        return computeIntersection(segX1, segY1, segX2, segY2, c.getZeroShape(), c.getZeroTransform(), new float[2], 1);
    }


    /**
     * Compute the intersection of an arbitrary shape and a line segment
     * that is assumed to pass throught the shape.  Usually used
     * with an endpoint (rayX2,rayY2) that ends in the center of the
     * shape, tho that's not required.
     *
     * @param max - max number of intersections to compute. An x/y
     * pair of coords will put into result up to max times. Must be >= 1.
     *
     * @return float array of size 2: x & y values of intersection,
     * or ff no intersection, returns Float.NaN values for x/y.
     */
    public static float[] computeIntersection(float segX1, float segY1,
                                              float segX2, float segY2,
                                              java.awt.Shape shape, java.awt.geom.AffineTransform shapeTransform,
                                              float[] result, int max)
    {
        java.awt.geom.PathIterator i = shape.getPathIterator(shapeTransform);
        // todo performance: if this shape has no curves (CUBICTO or QUADTO)
        // this flattener is redundant.  Also, it would be faster to
        // actually do the math for arcs and compute the intersection
        // of the arc and the line, tho we can save that for another day.
        i = new java.awt.geom.FlatteningPathIterator(i, 0.5);
        
        float[] seg = new float[6];
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        int cnt = 0;
        int hits = 0;
        while (!i.isDone()) {
            int segType = i.currentSegment(seg);
            if (cnt == 0) {
                firstX = seg[0];
                firstY = seg[1];
            } else if (segType == PathIterator.SEG_CLOSE) {
                seg[0] = firstX; 
                seg[1] = firstY; 
            }
            float endX = seg[0];
            float endY = seg[1];
                
            // at cnt == 0, we have only the first point from the path iterator, and so no line yet.
            if (cnt > 0 && Line2D.linesIntersect(segX1, segY1, segX2, segY2, lastX, lastY, seg[0], seg[1])) {
                //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                if (max <= 1) {
                    return computeLineIntersection(segX1, segY1, segX2, segY2, lastX, lastY, seg[0], seg[1], result);
                } else {
                    float[] tmp = computeLineIntersection(segX1, segY1, segX2, segY2, lastX, lastY, seg[0], seg[1], new float[2]);
                    result[hits*2 + 0] = tmp[0];
                    result[hits*2 + 1] = tmp[1];
                    if (++hits >= max)
                        return result;
                }
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return NoIntersection;
    }

    /** compute the first two y value crossings of the given x_axis and shape */
    public static float[] computeYCrossings(float x_axis, Shape shape, float[] result) {
        return computeIntersection(x_axis, Integer.MIN_VALUE, x_axis, Integer.MAX_VALUE, shape, null, result, 2);
    }
    
    /** compute 2 y values for crossings of at x_axis, and store result in the given Line2D */
    public static Line2D computeYCrossings(float x_axis, Shape shape, Line2D result) {
        float[] coords = computeYCrossings(x_axis, shape, new float[4]);
        result.setLine(x_axis, coords[1], x_axis, coords[3]);
        return result;
    }
    
    /**
     * This will clip the given vertical line to the edges of the given shape.
     * Assumes line start is is min y (top), line end is max y (bottom).
     * @param line - line to clip y values if outside edge of given shape
     * @param shape - shape to clip line to
     * @param pad - padding: keep line endpoints at least this many units away from shape edge
     *
     * todo: presumes only 2 crossings: will only handle concave polygons
     * Should be relatively easy to extend this to work for non-vertical lines if the need arises.
     */
    public static Line2D clipToYCrossings(Line2D line, Shape shape, float pad)
    {
        float x_axis = (float) line.getX1();
        float[] coords = computeYCrossings(x_axis, shape, new float[4]);
        // coords[0] & coords[2], the x values, can be ignored, as they always == x_axis

        if (coords.length < 4) {
            // TODO FIX: if line is outside edge of shape, we're screwed (see d:/test-layout.vue)
            // TODO: we were getting this of NoIntersection being returned (which was only of size
            // 2, and thus give us array bounds exceptions below) -- do we need to do anything
            // here to make sure the NoIntersection case is handled more smoothly?
            System.err.println("clip error " + coords);
            new Throwable("CLIP ERROR shape=" + shape).printStackTrace();
            return null;
        }

        float upper; // y value at top
        float lower; // y value at bottom
        if (coords[1] < coords[3]) {
            // cross1 is min cross (top), cross2 is max cross (bottom)
            upper = coords[1];
            lower = coords[3];
        } else {
            // cross2 is min cross (top), cross1 is max cross (bottom)
            upper = coords[3];
            lower = coords[1];
        }
        upper += pad;
        lower -= pad;
        // clip line to upper & lower (top & bottom)
        float y1 = Math.max(upper, (float) line.getY1());
        float y2 = Math.min(lower, (float) line.getY2());
        line.setLine(x_axis, y1, x_axis, y2);
        return line;
    }

    /** clip the given amount of length off each end of the given line -- negative values will extend the line length */
    public static Line2D.Float clipEnds(final Line2D.Float line, final double clipLength)
    {
        final double rise = line.y1 - line.y2; // delta Y
        final double run = line.x1 - line.x2; // delta X
        final double slope = run / rise; // inverse slope is what works here: due to +y is down in coord system?
        final double theta = Math.atan(slope);
        final double clipX = Math.sin(theta) * clipLength;
        final double clipY = Math.cos(theta) * clipLength;

        if (DEBUG.PATHWAY) {
            out("\nLine: " + fmt(line) + " clipping lenth off ends: " + clipLength);
            out(String.format("XD %.1f YD %.1f Slope %.1f Theta %.2f  clipX %.1f clipY %.1f", run, rise, slope, theta, clipX, clipY));
        }

        if (line.y1 < line.y2) {
            line.x1 += clipX;
            line.x2 -= clipX;
            line.y1 += clipY;
            line.y2 -= clipY;
        } else {
            line.x1 -= clipX;
            line.x2 += clipX;
            line.y1 -= clipY;
            line.y2 += clipY;
        }

        return line;
    }
    

    public static Line2D.Float computeConnector(LWComponent c1, LWComponent c2, Line2D.Float result)
    {
        computeConnectorAndCenterHit(c1, c2, result);
        return result;
    }
    
    
    //public static Line2D.Float computeConnector(LWComponent c1, LWComponent c2, Line2D.Float result)
    /**
     * On a line drawn from the center of head to the center of tail, compute the the line segment
     * from the intersection at the edge of shape head to the intersection at the edge of shape tail.
     * The returned line will be in the LWMap coordinate space.  If the components overlap sufficiently,
     * the segment returned will either be from the center of one component to the edge of the other,
     * or from center-to-center.
     *
     * @param result: this line will be set to the connecting segment
     * @return true if the components overlapped in such a way as to cause the segment to connect at one or
     * or both of the component centers, as opposed to their edges
     */
    public static boolean computeConnectorAndCenterHit(LWComponent head, LWComponent tail, Line2D.Float result)
    {
        // TODO: do these defaults still want to be the map-center now that we do
        // relative coords and parent-local links?  Shouldn't they be the center
        // relative to some desired parent focal? (e.g. a link parent)
        
        final float headX = head.getMapCenterX();
        final float headY = head.getMapCenterY();
        final float tailX = tail.getMapCenterX();
        final float tailY = tail.getMapCenterY();

        // compute intersection at head shape of line from center of head to center of tail shape
        final float[] intersection_at_1 = computeIntersection(headX, headY, tailX, tailY, head);

        boolean overlap = false;

        if (intersection_at_1 == NoIntersection) {
            // default to center of component 1
            result.x1 = headX;
            result.y1 = headY;
            overlap = true;
        } else {
            result.x1 = intersection_at_1[0];
            result.y1 = intersection_at_1[1];
        }
        
        // compute intersection at tail shape of line from prior intersection to center of tail shape
        final float[] intersection_at_2 = computeIntersection(result.x1, result.y1, tailX, tailY, tail);

        if (intersection_at_2 == NoIntersection) {
            // default to center of component 2
            result.x2 = tailX;
            result.y2 = tailY;
            overlap = true;
        } else {
            result.x2 = intersection_at_2[0];
            result.y2 = intersection_at_2[1];
        }
        
        return overlap;
    }

// Old version: could produce "internal" connections if nodes overlapped: directionality of the connector
// would get reversed. E.g., connector would be from the edge of a node back towards it's own center,
// to connect the outer edge of an overlapping node.
    
//     public static boolean computeConnectorAndCenterHit(LWComponent c1, LWComponent c2, Line2D.Float result)
//     {
//         // TODO: do these defaults still want to be the map-center now that we do
//         // relative coords and parent-local links?  Shouldn't they be the center
//         // relative to some desired parent focal? (e.g. a link parent)
        
//         final float segX1 = c1.getMapCenterX();
//         final float segY1 = c1.getMapCenterY();
//         final float segX2 = c2.getMapCenterX();
//         final float segY2 = c2.getMapCenterY();

//         // compute intersection at shape 1 of line from center of shape 1 to center of shape 2
//         final float[] intersection_at_1 = computeIntersection(segX1, segY1, segX2, segY2, c1);
//         // compute intersection at shape 2 of line from center of shape 2 to center of shape 1
//         final float[] intersection_at_2 = computeIntersection(segX2, segY2, segX1, segY1, c2);

//         boolean overlap = false;

//         if (intersection_at_1 == NoIntersection) {
//             // default to center of component 1
//             result.x1 = segX1;
//             result.y1 = segY1;
//             overlap = true;
//         } else {
//             result.x1 = intersection_at_1[0];
//             result.y1 = intersection_at_1[1];
//         }
        
//         if (intersection_at_2 == NoIntersection) {
//             // default to center of component 2
//             result.x2 = segX2;
//             result.y2 = segY2;
//             overlap = true;
//         } else {
//             result.x2 = intersection_at_2[0];
//             result.y2 = intersection_at_2[1];
//         }

//         //System.out.println("connector: " + out(result));
//         //System.out.println("\tfrom: " + c1);
//         //System.out.println("\t  to: " + c2);
        
//         return overlap;
//     }
    

    public static double computeVerticalRotation(Line2D l) {
        return computeVerticalRotation(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }
    
    /**
     * Compute the rotation needed to normalize the line segment to vertical orientation, making it
     * parrallel to the Y axis.  So vertical lines will return either 0 or Math.PI (180 degrees), horizontal lines
     * will return +/- PI/2.  (+/- 90 degrees).  In the rotated space, +y values will move down, +x values will move right.
     */
    public static double computeVerticalRotation(double x1, double y1, double x2, double y2)
    {
        final double xdiff = x1 - x2;
        final double ydiff = y1 - y2;
        final double slope = xdiff / ydiff; // really, inverse slope
        double radians = -Math.atan(slope);

        if (xdiff >= 0 && ydiff >= 0)
            radians += Math.PI;
        else if (xdiff <= 0 && ydiff >= 0)
            radians -= Math.PI;

        return radians;
    }

    /**
     * Move a point a given distance along a line parallel to the
     * ray implied by the the given line.  The direction of projection
     * is parallel to the ray that begins at the first point in the line,
     * and passes through the second point of the line.  The start point
     * does not need to be on the given line.
     * 
     * @return the new point
     */

    public static Point2D projectPoint(float x, float y, Line2D ray, float distance) {

        // todo: this impl could be much simpler

        final Point2D.Float p = new Point2D.Float();

        final double rotation = computeVerticalRotation(ray);

        final java.awt.geom.AffineTransform tx = new java.awt.geom.AffineTransform();

        tx.setToTranslation(x, y);
        tx.rotate(rotation);
        tx.translate(0,distance);
        tx.transform(p,p);

        return p;
    }


    /**
     * @return the point at the "center" of all the given nodes.  If the given
     * collection is null or contains no elements, null is returned.  If the given
     * collection contains only one element, the center point of that element is
     * returned.  The returned point is in coordinates at the top level map.  E.g., even
     * if all the nodes are children of a slide, the returned coordinate will not be
     * relative to the slide, it will be relative to the map.
     */
    public static Point2D.Float computeCentroid(Collection<LWComponent> nodes)
    {
        if (nodes == null || nodes.isEmpty())
            return null;
        
        float sumX = 0, sumY = 0;
        int count = 0;

        for (LWComponent c : nodes) {
            final float cx = c.getMapX() + c.getMapWidth() / 2;
            final float cy = c.getMapY() + c.getMapHeight() / 2;
            sumX += cx;
            sumY += cy;
            count++;
        }

        return new Point2D.Float(sumX / count,
                                 sumY / count);
    }

    
    
    public static Point2D projectPoint(Point2D.Float p, Line2D ray, float distance) {
        return projectPoint(p.x, p.y, ray, distance);
    }

    
    public static void dumpBytes(String s) {
        try {
            dumpBytes(s.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dumpBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.println("byte " + (i<10?" ":"") + i
                               + " (" + ((char)b) + ")"
                               + " " + pad(' ', 4, new Byte(b).toString())
                               + "  " + pad(' ', 2, Integer.toHexString( ((int)b) & 0xFF))
                               + "  " + pad('X', 8, toBinary(b))
                               );
        }
    }
    
    public static String toBinary(byte b) {
        StringBuffer buf = new StringBuffer(8);
        buf.append((b & (1<<7)) == 0 ? '0' : '1');
        buf.append((b & (1<<6)) == 0 ? '0' : '1');
        buf.append((b & (1<<5)) == 0 ? '0' : '1');
        buf.append((b & (1<<4)) == 0 ? '0' : '1');
        buf.append((b & (1<<3)) == 0 ? '0' : '1');
        buf.append((b & (1<<2)) == 0 ? '0' : '1');
        buf.append((b & (1<<1)) == 0 ? '0' : '1');
        buf.append((b & (1<<0)) == 0 ? '0' : '1');
	return buf.toString();
    }
    
    public static void dumpString(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int cv = (int) chars[i];
            System.out.println("char " + (i<10?" ":"") + i
                               + " (" + chars[i] + ")"
                               + " " + pad(' ', 6, new Integer(cv).toString())
                               + " " + pad(' ', 4, Integer.toHexString(cv))
                               + "  " + pad('0', 16, Integer.toBinaryString(cv))
                               );
        }
    }
    

    
    public static Map getQueryData(String query) {
        String[] pairs = query.split("&");
        Map map = new HashMap();
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            if (DEBUG.DATA || DEBUG.IMAGE) System.out.println("query pair " + pair);
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                String key = pair.substring(0, eqIdx);
                String value = pair.substring(eqIdx+1, pair.length());
                map.put(key.toLowerCase(), value);
            }
        }
        return map;
    }

    public static boolean isTransparent(Color c) {
        return c == null || c.getAlpha() == 0;
    }
    public static boolean isTranslucent(Color c) {
        return c == null || c.getAlpha() != 0xFF;
    }
    
    public static void alert(Component parent, Object message, String title) {
        VOptionPane.showWrappingMessageDialog(parent,
                                      message,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));                                      
    }
    
    public static void alert(Component parent, Object message, String title, int messageType) {
        VOptionPane.showWrappingMessageDialog(parent,
                                      message,
                                      title,
                                      messageType,
                                      null);
	}

    public static void alert(String title, Throwable t) {

        java.io.Writer buf = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(buf));
        JComponent msg = new JTextArea(buf.toString());
        msg.setOpaque(false);
        msg.setFont(new Font("Lucida Grande", Font.PLAIN, 9));

        VOptionPane.showWrappingMessageDialog(VUE.getDialogParent(),
                                      msg,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));
        
    }
   
    public static void alert(Object message, String title) {
        VOptionPane.showWrappingMessageDialog(VUE.getDialogParent(),
                                      message,
                                      title,
                                      JOptionPane.ERROR_MESSAGE,
                                      VueResources.getImageIcon("vueIcon32x32"));                                      
    }
   
    public static int confirm(Object message, String title) {
       return VOptionPane.showWrappingConfirmDialog(VUE.getDialogParent(),
                                            message,
                                            title,
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE,
                                            VueResources.getImageIcon("vueIcon32x32"));
    }
    
    public static int confirm(Component parent, Object message, String title) {
        return VOptionPane.showWrappingConfirmDialog(parent,
                                             message,
                                             title,
                                             JOptionPane.YES_NO_OPTION,
                                             JOptionPane.QUESTION_MESSAGE,
                                             VueResources.getImageIcon("vueIcon32x32"));
    }
    
    public static int confirm(Component parent, Object message, String title, int optionType) {
        return VOptionPane.showWrappingConfirmDialog(parent,
                                             message,
                                             title,
                                             optionType,
                                             JOptionPane.QUESTION_MESSAGE,
                                             null);
    }
    
    public static int confirm(Component parent, Object message, String title, int optionType, int messageType) {
        return VOptionPane.showWrappingConfirmDialog(parent,
                                             message,
                                             title,
                                             optionType,
                                             messageType,
                                             null);
    }
    
    public static int option(Component parent, Object message, String title, int optionType, int messageType, Object[] options, Object initialValue) {
        return VOptionPane.showWrappingOptionDialog(parent,
                                             message,
                                             title,
                                             optionType,
                                             messageType,
                                             null,
                                             options,
                                             initialValue);
    }
    
    public static Object input(Object message) {
    	return VOptionPane.showWrappingInputDialog(null,
                                             message,
                                             null,
                                             JOptionPane.QUESTION_MESSAGE,
                                             null,
                                             null,
                                             null);
    }
    
    public static Object input(Component parent, Object message, String title, int messageType,
			Object[] selectionValues, Object initialSelectionValue) {
    	return VOptionPane.showWrappingInputDialog(parent,
                                             message,
                                             title,
                                             messageType,
                                             null,
                                             selectionValues,
                                             initialSelectionValue);
    }


    public static int getMaxLabelLineLength() {
        // todo: this should be cached
        return VueResources.getInt("dataNode.labelLength");
    }

    private static JColorChooser colorChooser;
    private static Dialog colorChooserDialog;
    private static boolean colorChosen;
    /** Convience method for running a JColorChooser and collecting the result */
    public static Color runColorChooser(String title, java.awt.Color c, java.awt.Component chooserParent)
    {
        if (colorChooserDialog == null) {
            colorChooser = new JColorChooser();
            //colorChooser.setDragEnabled(true);
            //colorChooser.setPreviewPanel(new JLabel("FOO")); // makes it dissapear entirely, W2K/1.4.2/Metal
            if (false) {
                final JPanel np = new JPanel();
                np.add(new JLabel(VueResources.getString("jlabel.text")));
                np.setSize(new Dimension(300,100)); // will be invisible otherwise
                np.setBackground(Color.red);
                //np.setBorder(new EmptyBorder(10,10,10,10));
                //np.setBorder(new EtchedBorder());
                np.setBorder(new LineBorder(Color.black));
                np.setOpaque(true);
                np.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            System.out.println("CC " + e.getPropertyName() + "=" + e.getNewValue());
                            if (e.getPropertyName().equals("foreground"))
                                np.setBackground((Color)e.getNewValue());
                        }});
                colorChooser.setPreviewPanel(np); // also makes dissapear entirely
            }
            /*
            JComponent pp = colorChooser.getPreviewPanel();
            System.out.println("CC Preview Panel: " + pp);
            for (int i = 0; i < pp.getComponentCount(); i++)
                System.out.println("#" + i + " " + pp.getComponent(i));
            colorChooser.getPreviewPanel().add(new JLabel("FOO"));
            */
            colorChooserDialog =
                JColorChooser.createDialog(chooserParent,
                                           VueResources.getString("dialog.colorchooser.title"),
                                           true,  
                                           colorChooser,
                                           new ActionListener() { public void actionPerformed(ActionEvent e)
                                               { colorChosen = true; } },
                                           null);
        }
        if (c != null)
            colorChooser.setColor(c);
        if (title != null)
            colorChooserDialog.setTitle(title);

        colorChosen = false;
        // show() blocks until a color chosen or cancled, then automatically hides the dialog:
        colorChooserDialog.setVisible(true);

        JComponent pp = colorChooser.getPreviewPanel();
        System.out.println("CC Preview Panel: " + pp + " children=" + Arrays.asList(pp.getComponents()));
        for (int i = 0; i < pp.getComponentCount(); i++)
            System.out.println("#" + i + " " + pp.getComponent(i));
        
        return colorChosen ? colorChooser.getColor() : null;
    }
    

    //----------------------------------------------------------------------------------------
    // Below generic relational clustiner code by Anoop -- refactored by SMF:
    //----------------------------------------------------------------------------------------
    
    private static final boolean ALL_DATA = true; // use all data while comparing similarity between two LW Components. All includes notes and metadata

    public static void setXYByClustering(LWNode node) {
        setXYByClustering(Collections.singletonList(node));
    }

    public static List<LWComponent> setXYByClustering(Collection<? extends LWComponent> layoutNodes) {
        return setXYByClustering(tufts.vue.VUE.getActiveMap(),
                                 layoutNodes);
    }
        
    public static List<LWComponent> setXYByClustering(LWMap map, Collection<? extends LWComponent> layoutNodes)
    {
        final Collection<LWComponent> all = map.getAllDescendents();
        final Collection<LWNode> relatingNodes = new ArrayList(all.size() / 2);
        
        for (LWNode n : typeFilter(all, LWNode.class)) {
            if (!layoutNodes.contains(n))
                relatingNodes.add(n);
        }

        final List<LWComponent> untouched = new ArrayList();
        
        for (LWComponent c : layoutNodes) {
            try {
                // performance: pre-compute top-level-items for possible pushing and pass it in here:
                if (!setXYByClustering(map, relatingNodes, c))
                    untouched.add(c);
            } catch (Throwable t) {
                Log.warn("weighted cluster failed for " + c, t);
            }
        }

        return untouched;
    }
    
    
    /** relations should NOT contain the node at this point */
    private static boolean setXYByClustering(LWMap map, Collection<LWNode> relations, LWComponent node)
    {
        Log.debug("relating to " + tags(relations) + ": " + node);
        
        float xNumerator = 0 ;
        float yNumerator = 0 ;
        float denominator = 0 ;
        
        for (LWNode mapNode : relations) {
            double score = computeScore(node, mapNode);
            xNumerator += score*score*mapNode.getX();
            yNumerator += score*score*mapNode.getY();
            denominator += score*score;
        }
        
        if (denominator != 0) {
            float x = xNumerator/denominator;
            float y = yNumerator/denominator;
            node.setX(x);
            node.setY(y);
            
            for (LWComponent mapNode : relations) {
                if (checkCollision(mapNode, node)) {
                    // ideally, we'd pre-fetch the list of all top-level items to
                    // push -- projectNodes is going to refetch them for every push:
                    try {
                        //Actions.projectNodes(node, 24, Actions.PUSH_ALL);
                        // performance: pass in pre-computed top-level-items to push, not the map
                        Actions.projectNodes(map.getTopLevelItems(ChildKind.EDITABLE), node, 24);
                    } catch (Throwable t) {
                        Log.warn("projection failure " + node, t);
                    }
                }
            }
            return true;
        } else
            return false;
    }
	
    public static double computeScore (LWComponent n1, LWComponent n2) {
        double score = 0.0;
        String content1 = n1.getLabel();
        String content2 = n1.getLabel();
        if(ALL_DATA) {
            content1 += " "+n1.getNotes();
            content2 += " "+n2.getNotes();
            if(n1.getResource()!= null) content1 += " "+n1.getResource().getSpec();
            if(n2.getResource()!= null) content2 += " "+n2.getResource().getSpec();
            MetadataList mList1 = n1.getMetadataList();
            for(VueMetadataElement vme: mList1.getMetadata()){
                content1 +=" "+vme.getKey();
                content1 +=" "+vme.getValue();
            }
            MetadataList mList2 = n2.getMetadataList();
            for(VueMetadataElement vme: mList2.getMetadata()){
                content2 +=" "+vme.getKey();
                content2 +=" "+vme.getValue();
            }
			
        }
        String[] words1 = content1.split("\\s+");
        String[] words2 = content2.split("\\s+");
        int matches = 0;
        for(int i = 0;i<words1.length;i++) {
            if(n2.getLabel().contains(words1[i])){
                matches++;
            }
        }
        double p1 = (double) matches / words1.length;
        double p2 = (double) matches/words2.length;
        if(p1== 0 && p2 == 0 ){
            score = 0.0; 
        } else {
            score = 2*p1*p2/(p1+p2); // harmonic mean
        }
        return score;
    }
	
    public static boolean checkCollision(LWComponent c1, LWComponent c2)
    {
        boolean collide = false;
        if(c2.getX()>= c1.getX() && c2.getX() <= c1.getX()+c1.getWidth() && c2.getY() >= c1.getY() && c2.getY() <=c1.getY()+c2.getHeight()) {
            collide = true;
        }
        return collide;
    }    

    
}


/**
 * VOptionPane extends JOptionPane for the sole purpose of returning MAX_LINE_LENGTH
 * from getMaxCharactersPerLineCount() so that long messages will wrap.
 */
class VOptionPane extends JOptionPane
{
	static final long	serialVersionUID = 1;
	static final int	MAX_LINE_LENGTH = 80;

	VOptionPane() {
	}

	public int getMaxCharactersPerLineCount() {
		return MAX_LINE_LENGTH;
	}

	static void showWrappingMessageDialog(Component parent, Object message, String title,
			int messageType, Icon icon)
			throws HeadlessException{
		showWrappingOptionDialog(parent, message, title, JOptionPane.DEFAULT_OPTION, messageType, icon, null, null);
	}

	static int showWrappingConfirmDialog(Component parent, Object message, String title,
			int optionType, int messageType, Icon icon)
			throws HeadlessException {
		return showWrappingOptionDialog(parent, message, title, optionType, messageType, icon, null, null);
	}

	static int showWrappingOptionDialog(Component parent, Object message, String title,
			int optionType, int messageType, Icon icon,
			Object[] options, Object initialValue)
			throws HeadlessException {
		int				result = CLOSED_OPTION;
		VOptionPane		optionPane = new VOptionPane();

		optionPane.setMessage(message);
		optionPane.setOptionType(optionType);
		optionPane.setMessageType(messageType);
		optionPane.setIcon(icon);
		optionPane.setOptions(options);
		optionPane.setInitialValue(initialValue);
		optionPane.setComponentOrientation((parent != null ? parent : getRootFrame()).getComponentOrientation());

		JDialog			dialog = optionPane.createDialog(parent, title);

		optionPane.selectInitialValue();
		dialog.setVisible(true);

		Object			selectedValue = optionPane.getValue();

		if (selectedValue != null) {
			if (options == null) {
				if (selectedValue instanceof Integer) {
					result = ((Integer)selectedValue).intValue();
				}
			} else {
				for (int counter = 0, maxCounter = options.length; counter < maxCounter; counter++) {
					if (options[counter].equals(selectedValue)) {
						result = counter;
						break;
					}
				}
			}
		}

		return result;
	}

	static Object showWrappingInputDialog(Component parent, Object message, String title,
			int messageType, Icon icon,
			Object[] selectionValues, Object initialSelectionValue)
			throws HeadlessException {
		Object			result = null;
		VOptionPane		optionPane = new VOptionPane();

		optionPane.setWantsInput(true);
		optionPane.setMessage(message);
		optionPane.setOptionType(OK_CANCEL_OPTION);
		optionPane.setMessageType(messageType);
		optionPane.setIcon(icon);
		optionPane.setSelectionValues(selectionValues);
		optionPane.setInitialSelectionValue(initialSelectionValue);
		optionPane.setComponentOrientation((parent != null ? parent : getRootFrame()).getComponentOrientation());

		JDialog			dialog = optionPane.createDialog(parent, title);

		optionPane.selectInitialValue();
		dialog.setVisible(true);

		Object			value = optionPane.getInputValue();

		if (value != UNINITIALIZED_VALUE) {
			result = value;
		}

		return result;
        }
	
}

