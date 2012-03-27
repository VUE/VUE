/*
 * This addition Copyright 2010-2012 Design Engineering Group, Imperial College London
 * Licensed under the
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

/**
* @author  Helen Oliver, Imperial College London 
*/

package tufts.vue.action;

import java.io.*;
import java.net.URI;
import java.util.zip.*;
import java.util.*;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;

import tufts.vue.*;
import tufts.vue.gui.VueMenuBar;
import tufts.Util;

public class FileLockAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FileLockAction.class);
    
    private static final String UNLOCKABLE_FILE = "NOLOCK";
    
    public FileLockAction(String label) {
        super(label, null, ":general/Lock");
    }
    
    public FileLockAction(File theFile, boolean bOpening) {
        this("Lock");
        setFile(theFile);
        setOpening(bOpening);
    }
    
    public FileLockAction() {
        this("Lock");
    }
    
    private boolean bOpening;
    private File mFile;
    private LWMap mMap;
    
    public void setFile(File theFile) {
    	mFile = theFile;
    }
    
    public File getFile() {
    	return mFile;
    }
    
    public void setMap(LWMap theMap) {
    	mMap = theMap;
    }
    
    public LWMap getMap() {
    	return mMap;
    }
    
    public void setOpening(boolean opening) {
    	bOpening = opening;
    }
    
    public boolean isOpening() {
    	return bOpening;
    }
    
    private void init() {
    	LWMap activeMap = VUE.getMapInActiveTab();
    	if (activeMap != null) {
    		File activeFile = activeMap.getFile();
    		//if (activeFile == null) {
    			//activeFile = new File(activeMap.getSaveFile());
    		//}
    		setFile(activeFile);
    		setMap(activeMap);
    	}
    	setOpening(false);
    }
    
    private static final Object LOCK = new Object();
    private static boolean lockUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (lockUnderway)
                return;
            lockUnderway = true;
        }
        try {
        	init();
        	
        	createLockFile(mFile, mMap, bOpening, true);      
        	
            Log.info(e.getActionCommand() + ": completed.");
        } finally {
            lockUnderway = false;
            
            
        }
    }  
    
    
    
    // HO 04/01/2012 BEGIN *********
    /**
     * Function to determine whether a file is writable by the current
     * user. It's writable if a) it hasn't been marked read-only and
     * b) it isn't locked by any other user.
     * @param theFile, the File object which may or may not be writable.
     * @return the original input param file if the file is just not writable,
     * null if the file is writable and not locked by any other user,
     * the lock File if the file is locked by another user.
     * @author Helen Oliver
     */
    public static File isFileWritableByCurrentUser(File theFile) {
    	File lockFile = null;
    	// HO 02/03/2012 BEGIN **********
		VUE.currentnum = 1;
		// HO 02/03/2012 END **********
    	// input validation
    	if (theFile == null) {
    		return lockFile;
    	}
    	
    	// HO 02/03/2012 BEGIN **********
		VUE.currentnum = 2;
		// HO 02/03/2012 END **********
    	
    	// if for whatever reason the file has been set not-writable,
    	// return the original file
		// HO 05/03/2012 BEGIN *******
		// boolean bWritable = theFile.canWrite();
		boolean bWritable = false;
		try {
			bWritable = theFile.canWrite();
		} catch (SecurityException e) {
			VUE.currentnum = 21;
			return theFile;
		}
		// HO 05/03/2012 END **********
    	
    	// HO 02/03/2012 BEGIN **********
		VUE.currentnum = 3;
		// HO 02/03/2012 END **********
    	if (bWritable == false) {
    		// HO 02/03/2012 BEGIN **********
			VUE.currentnum = 4;
			// HO 02/03/2012 END **********
    		return theFile;
    	}
    	
    	// HO 02/03/2012 BEGIN **********
		VUE.currentnum = 5;
		// HO 02/03/2012 END **********
    	// check and see if the file is locked by another user
    	lockFile = isFileLockedByOtherUser(theFile);
    	// HO 02/03/2012 BEGIN **********
		VUE.currentnum = 6;
		// HO 02/03/2012 END **********
    	    	
    	return lockFile;
    }
    
    /**
     * Function to determine whether a file is locked by the current user.
     * It's locked by the current user if there is a file of type
     * ".vlk" in the same folder, with the same name, prefixed
     * with the current user name.
     * @param theFile, the file which may or may not be locked by the current user.
     * @return true if the file is locked by the current user,
     * false otherwise.
     * @author Helen Oliver
     */
    public static File isFileLockedByCurrentUser(File theFile) {
    	File lockFile = null;
    	
    	// input validation
    	if (theFile == null)
    		return lockFile;

		// HO 02/03/2012 END **********
    	// directory to look in
    	File targetDir = theFile.getParentFile();
    	if (targetDir == null) {
    		VUE.othernum = 31;
    		return null;
    	}
    	// HO 02/03/2012 BEGIN **********
    	// get the name to match against
    	String strMatchingName = lockFilePrefix(theFile);
    	
    	
		// HO 05/03/2012 BEGIN ********
		boolean bDirectory = false;
		try {
			bDirectory = targetDir.isDirectory();
		} catch (SecurityException e) {
			VUE.othernum = 61;
			return null;
		}
		
		// if (targetDir.isDirectory()) {
		if (bDirectory) {
		// HO 05/03/2012 END ******** 
    		// HO 05/03/2012 BEGIN ********
    		VUE.firsttargetdir = targetDir.toString();
    		// HO 05/03/2012 END *********
    		// get all the VUE lock files in the directory
			File[] dir = targetDir.listFiles(appropriateFilter(theFile));	
			
			// if we have a list of files, cycle through them
			if (dir != null) {	
				for (int i = 0; i < dir.length; i++) {
					// get the next file in the directory
					File file_test = dir[i];
					String strNextFilename = file_test.getName().toString();
						
						if (strNextFilename.equals(strMatchingName + lockFileSuffix(theFile.getName()))) {
								String strLockedUserName = userWhoHasLockedAFile(file_test);
								String strCurrentUserName = System.getProperty("user.name");
								if ((strLockedUserName != "") && (strLockedUserName.equals(strCurrentUserName))) {
									lockFile = file_test;
									break;
								}
						}
				}
			}
    	}
    	
		return lockFile;

    }
    
    /**
     * A function to open the lock file, read a line from it,
     * and return the line. The line will be the name
     * of the user that has the lock on the file.
     * @param lockFile, the lock file
     * @return an empty string if there's nothing in the lock file
     * or something goes wrong in the reading of it;
     * or the username of the user who has the lock if there is a line in there
     * @author Helen Oliver
     */
    public static String userWhoHasLockedAFile(File lockFile) {
    	// the user name-to-be
    	String strUserWithLock = "";
    	
    	// open the file and read a line from it
    	try {
	    	BufferedReader br = new BufferedReader(new FileReader(lockFile));
	    	strUserWithLock = br.readLine();
	    	br.close();
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
        
    	// return what we read from the file
    	return strUserWithLock;
    }
    

    
    /**
     * Function to determine whether a file is locked by another user.
     * A file is locked by another user if there is a file of
     * the same name in the same folder, with the extension ".vlk",
     * and prefixed by not-the-current-user's name.
     * @param theFile, the File that may or may not be locked by another user.
     * @return the lock File belonging to the other user, if there is one
     * @author Helen Oliver
     */
    public static File isFileLockedByOtherUser(File theFile) {
    	File lockFile = null;
    	
    	// input validation
    	if (theFile == null) {
    		return lockFile;
    	}
    	
    	// directory to look in
    	File targetDir = theFile.getParentFile();
    	if (targetDir == null) {
    		return null;
    	}

		String strMatchingName = lockFilePrefix(theFile);
		// now go through the directory
		// HO 05/03/2012 BEGIN ********
		boolean bDirectory = false;
		try {
			bDirectory = targetDir.isDirectory();
		} catch (SecurityException e) {
			VUE.othernum = 61;
			return null;
		}
		
		// if (targetDir.isDirectory()) {
		if (bDirectory) {
		// HO 05/03/2012 END ********   	
    		// get all the files in the directory
			File[] dir = targetDir.listFiles(appropriateFilter(theFile));	
			
			// if we have a list of files, cycle through them
			if (dir != null) {	
				for (int i = 0; i < dir.length; i++) {
					// get the next file in the directory
					File file_test = dir[i];
					String strNextFilename = file_test.getName().toString();
						if (strNextFilename.equals(strMatchingName + lockFileSuffix(theFile.getName()))) {
								String strUserName = userWhoHasLockedAFile(file_test);
								if ((strUserName != "") && (!strUserName.equals(System.getProperty("user.name")))) {
									lockFile = file_test;
									break;
								}
						}
				}
			}
    	}
    	
    	return lockFile;
    }
    
    
    /**
     * A function to determine what the lock file prefix should be
     * for the current user.
     * @param theFile, the File object for which to determine the prefix.
     * @return a String representing the lock file prefix
     * @author Helen Oliver
     */
    private static String lockFilePrefix(File theFile) {
    	// input validation
    	if (theFile == null)
    		return null;

		// and use the name of the file being locked too
		String strLockFileName = theFile.getName();
		
		// strip off the existing suffix
		strLockFileName = strLockFileName.substring(0, strLockFileName.length() - 4);
		
		return strLockFileName;
    }
    
    /**
     * A function to return the suffix for the VUE lock file type.
     * @return a String representing the suffix for the VUE lock file type,
     * or a constant 
     * @author Helen Oliver
     */
    private static String lockFileSuffix(String strFileName) {
    	String strLockFileSuffix = UNLOCKABLE_FILE;
    	
    	if (strFileName.endsWith(VueUtil.VueExtension))
    		strLockFileSuffix = VueUtil.VueLockExtension;
    	else if (strFileName.endsWith(VueUtil.VueArchiveExtension))
    		strLockFileSuffix = VueUtil.VueArchiveLockExtension;
    	// HO 23/03/2012 BEGIN *******
    	else if (strFileName.endsWith(VueUtil.designVueArchiveExtension))
    		strLockFileSuffix = VueUtil.designVueArchiveLockExtension;
    	// HO 23/03/2012 END *********

    	return strLockFileSuffix;
    }
    
    /**
     * A method to delete what is probably the previous
     * lock file, when saving to a new file.
     * @param map, the LWMap which may or may not have a File that needs to be released
     * from any existing locks.
     * @param newFile, the new file-to-be
     * @author Helen Oliver
     */
    public static void deletePreviousLockFile(LWMap map, File newFile) {
    	// input validation
    	if (map == null)
    		return;
    	
    	String newName = newFile.getName();
    	String newPath = newFile.getParentFile().toString();
    	
    	File mapFile = map.getFile();
    	
    	if (mapFile != null) {
    		String oldName = mapFile.getName();
        	String oldPath = mapFile.getParentFile().toString();
        	
    		// if they have different names
    		if (!oldName.toLowerCase().equals(newName.toLowerCase())) {
    			deleteLockFile(mapFile);
    		} else if (!oldPath.toLowerCase().equals(newPath.toLowerCase())) {
    			// if they have the same name but are on different paths
    			deleteLockFile(mapFile);    			
    		}
    	}
    }
    // HO 04/01/2012 END **********
    
 // HO 21/12/2011 BEGIN ********
    /**
     * A method to create a lock file.
     * The lock file is in the same directory as the
     * file being locked.
     * The file can't be locked if it's not writable
     * in the first place, or is already locked
     * by someone else. Otherwise it will be
     * locked by the current user.
     * @param theFile, the File object to be locked.
     * @param theMap, the Map with the locked File object.
     * @param bOpening, true if we are in the process of opening a file,
     * false otherwise.
     * @param bNotifying, true if we are notifying the user that the file has been locked,
     * false otherwise.
     * @author Helen Oliver
     */
    public static void createLockFile(File theFile, LWMap theMap, boolean bOpening, boolean bNotifying) {
   
		
    	if (theFile != null) {
    		try {
    				File lockFile = isFileLockedByCurrentUser(theFile);
	    			if (lockFile != null) {
	    				// if the current user already has
	    				// a lock on the file, do nothing
	    				// except making sure it will delete when
	    				// the VM does
	    				lockFile.deleteOnExit();
	    				return;
	    			} else {
	    				// if the file is writable at all
	    				boolean bWritable = checkIfFileIsWritable(theFile, theMap, bOpening, bNotifying);
	    			
	    				// if the file is writable for this user
	    				if (bWritable) {
		    				// create a temporary file so the prefix is the current user name
	    					String strLockFileName = lockFilePrefix(theFile);
		    				String strLockFilePrefix = strLockFileName;
		    				// get appropriate suffix for lock file suffix
		    				String strLockFileSuffix = lockFileSuffix(theFile.getName());
		    				// if the file is of a type that can't be locked,
		    				// notify the user and return 
		    				if (strLockFileSuffix.equals(UNLOCKABLE_FILE)) {
		    					// only show this if we are notifying the user
		    					if (bNotifying) {
			    					JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
			    			                strLockFileName + " cannot be locked.\n"
			    			                + "Only files of type .VUE, .VPK and .VDK can be locked.",
			    			                "File cannot be locked.", 
			    			                JOptionPane.WARNING_MESSAGE);
		    					}
		    					
		    					return;
		    				}
		    					
		    				// and the directory is the same one as the file is in
		    				File lockFileDirectory = theFile.getParentFile();
		    				// now create the lock file in the right directory
		    				try {
		    					// create the lock file
		    					//lockFile = File.createTempFile(strLockFilePrefix, strLockFileSuffix, lockFileDirectory);
		    					lockFile = new File(lockFileDirectory, strLockFilePrefix + strLockFileSuffix);
		    					// write the username to the lock file
		    					BufferedWriter bw = new BufferedWriter(new FileWriter(lockFile));
		    		    	    String strCurrentUser = System.getProperty("user.name");
		    		    	    String strFileName = theFile.getName();
		    					bw.write(strCurrentUser);
		    		    	    bw.close();
		    					// make sure the lock file gets deleted when the virtual machine terminates
		    					lockFile.deleteOnExit();
		    					if ((bNotifying) && (!bOpening)) {
		    						JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
		    				             "You have locked the file " + strFileName + ".",
		    			                "File locked by " + strCurrentUser, 
		    			                JOptionPane.INFORMATION_MESSAGE);
		    					}
		    				} catch (IOException e) {
		    					e.printStackTrace();
		    				}
	    				} else {
	    					
	    				}
	    			} 
    		} catch (SecurityException se) {
    			se.printStackTrace();
    		}
    	} else { // there's no file object
    		if ((bNotifying) && (!bOpening)) { // if we are notifying the user of the lock's success or failure, 
    			// and we are not in the process of opening another file, show message
    			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
    	                "There is no file to lock.",
                    "No file object", 
                    JOptionPane.WARNING_MESSAGE);
    		}
    	}
    	
    }
    
    
    // HO 21/12/2011 END ****** 
    
    
    // HO 05/01/2012 BEGIN ************    

    
    /**
     * A method to find the current user's lock file
     * on the given File object, if there is one, and
     * delete it
     * @param theFile, the File object for which to find and
     * delete the current user's lock file, if there is one
     */
    public static void deleteLockFile(File theFile) {
    	// routine to figure out if there's a lock file,
    	// and delete it if there is
    	File lockFile = isFileLockedByCurrentUser(theFile);
    	if (lockFile != null) {
    		lockFile.delete();    		
    	}
    }
    
    /**
     * A method to find the current user's lock file
     * on the given File object, if there is one, and
     * delete it
     * @param theFile, the File object for which to find and
     * delete the current user's lock file, if there is one
     * @param bShowingMessage, true if we are showing the user a message
     * @author Helen Oliver
     */
    public static void deleteLockFile(File theFile, boolean bShowingMessage) {
    	// routine to figure out if there's a lock file,
    	// and delete it if there is
    	File lockFile = isFileLockedByCurrentUser(theFile);
    	if (lockFile != null) {
    		lockFile.delete();    	
			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
	                "File unlocked.\n"
                	+ "Your changes may be overwritten by other users.",
                "File unlocked", 
                JOptionPane.WARNING_MESSAGE);
    	} else {
    		lockFile = isFileLockedByOtherUser(theFile);
    		if (lockFile != null) {
    			String strUserWithLock = userWhoHasLockedAFile(lockFile);
    			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
	                strUserWithLock + " has locked this file for writing.\n"
                	+ "You cannot unlock this file.",
                "File locked by other user", 
                JOptionPane.WARNING_MESSAGE);
    		} else {
    			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
    	                "There is no file to unlock.",
                    "No file object", 
                    JOptionPane.WARNING_MESSAGE);
    		}
    	}
    }    
    
    // HO 21/12/2011 BEGIN ******
    /**
     * A function to check whether the file is writable
     * @param file, the file to check
     * @param theMap, the LWMap that goes with this file
     * @param bOpening, true if we are in the process of opening a file, false if not
     * @param bNotifying, true if we are notifying the user of the status, false if not
     */
    public static boolean checkIfFileIsWritable(File file, LWMap theMap, boolean bOpening, boolean bNotifying) {
    	// HO 02/03/2012 BEGIN **********
		VUE.writablenum = 1;
		// HO 02/03/2012 END **********
		// HO 05/03/2012 BEGIN ********
		File lockFile = isFileWritableByCurrentUser(file);
		// HO 05/03/2012 END *********
    	// HO 02/03/2012 BEGIN **********
		VUE.writablenum = 2;
		// HO 02/03/2012 END **********// HO 02/03/2012 BEGIN **********
		VUE.writablenum = 3;
		// HO 02/03/2012 END **********
    	
	    // if it's writable, we either get back null or a
    	// File object that is NOT the same one we sent in
    	if (lockFile == null) {
	    	if (bNotifying) {
	    		 // HO 02/03/2012 BEGIN **********
	    		VUE.writablenum = 4;
	    			// HO 02/03/2012 END **********
		    	// HO 19/01/2012 BEGIN *******
		    	notifyTargetFilesThatAreLocked(theMap);
		    	// HO 19/01/2012 END *********
		    	// HO 02/03/2012 BEGIN **********
		    	VUE.writablenum = 5;
    			// HO 02/03/2012 END **********
	    	}
	    	return true;
	    } else if (lockFile.equals(file)) {
	    	// HO 02/03/2012 BEGIN **********
	    	VUE.writablenum = 6;
			// HO 02/03/2012 END **********
	    	if (bNotifying) {
		    	JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
		                file.getName() + " is not writable.", 
		                "Can't save", 
		                JOptionPane.ERROR_MESSAGE);
	    	}
	    	return false;
	    } else {
	    	if (!bOpening) {
	    		if (bNotifying) {
	    			// HO 02/03/2012 BEGIN **********
	    			VUE.writablenum = 7;
	    			// HO 02/03/2012 END **********
			    	notifyThatFileIsLocked(file, lockFile);
			    	// HO 02/03/2012 BEGIN **********
			    	VUE.writablenum = 8;
	    			// HO 02/03/2012 END **********
			    	// HO 19/01/2012 BEGIN *******
			    	notifyTargetFilesThatAreLocked(theMap);
			    	// HO 19/01/2012 END *********
			    	// HO 02/03/2012 BEGIN **********
			    	VUE.writablenum = 9;
	    			// HO 02/03/2012 END **********
	    		}
	    	} else {
	    		if (bNotifying) {
	    			// HO 02/03/2012 BEGIN **********
	    			VUE.writablenum = 10;
	    			// HO 02/03/2012 END **********
	    			notifyThatFileIsLocked(file, lockFile);
	    			// HO 02/03/2012 BEGIN **********
	    			VUE.writablenum = 11;
	    			// HO 02/03/2012 END **********
			    	// HO 19/01/2012 BEGIN *******
			    	notifyTargetFilesThatAreLocked(theMap);
			    	// HO 19/01/2012 END *********
			    	// HO 02/03/2012 BEGIN **********
			    	VUE.writablenum = 12;
	    			// HO 02/03/2012 END **********
	    		}
	    	}
	    	return false;
	    }
    }
    // HO 21/12/2011 END ********
    // HO 05/01/2012 END **************    
    
    /**
     * A function to return the right kind of file extension
     * filter for a given file.
     * @param theFile, the file for which we need the right extension filter
     * @return either a VueLockFileFilter or a VueArchiveLockFileFilter or a DesignVueArchiveLockFileFilter,
     * according to the file type
     * @author Helen Oliver
     */
    private static FileFilter appropriateFilter(File theFile) {
    	if (theFile == null)
    		return null;
    	
    	String strFileName = theFile.getName();
    	if (strFileName.endsWith(VueUtil.VueExtension))
    		return new VueLockFileFilter();
    	
    	else if (strFileName.endsWith(VueUtil.VueArchiveExtension))
    		return new VueArchiveLockFileFilter();
    	
    	// HO 23/03/2012 BEGIN ******
    	else if (strFileName.endsWith(VueUtil.designVueArchiveExtension))
    		return new DesignVueArchiveLockFileFilter();
    	// HO 23/03/2012 END ********
    	
    	return null;
    	
    }
    
    static class VueLockFileFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.VueLockExtension)) 
  	      return true;
  	    return false;
  	  }
  	}
    
    // HO 18/01/2012 BEGIN *******
    /**
     * A method to show a notification that a given file is locked,
     * and by whom.
     * @param file, the File that is locked
     * @param lockFile, the lock file in question
     * @author Helen Oliver
     */
    public static void notifyThatFileIsLocked(File file, File lockFile) {
    	String strUserWithLock = userWhoHasLockedAFile(lockFile);
    	
    	JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                strUserWithLock + " has locked the file\n"
            	+ file.getName() + "\nfor writing.\n"
            	+ "Your changes will not be saved.\n"
            	+ "Wormholes in this file will not be updated.\n",
            "File locked by other user.", 
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * A method to take a map we know is locked,
     * check through it for wormholes,
     * and check if each target file is locked by someone else,
     * and notify the user if it is.
     * @param map, the LWMap we know is locked
     * @author Helen Oliver
     */
    public static void notifyTargetFilesThatAreLocked(LWMap map) {
    	// HO 02/03/2012 BEGIN **********
		VUE.notifynum = 1;
		// HO 02/03/2012 END **********
    	Collection<LWWormholeNode> coll = map.getAllWormholeNodes();
    	// HO 02/03/2012 BEGIN **********
		VUE.notifynum = 2;
		// HO 02/03/2012 END **********
    	
    	Vector fileNames = new Vector();
    	// HO 02/03/2012 BEGIN **********
		VUE.notifynum = 3;
		// HO 02/03/2012 END **********
    	
        // if we found any wormhole nodes
		if (coll.size() > 0) {
			// iterate through them all
			for (LWWormholeNode wn : coll) {
					// if the node has a resource
		            if (wn.hasResource()) {
		            	// get the resource from the node
		            	Resource r = wn.getResource();
		            	// check to make sure the resource is a wormhole resource
		            	if (r.getClass().equals(tufts.vue.WormholeResource.class)) {
		            		// if it is, downcast it to create a proper WormholeResource object
		            		WormholeResource wr = (WormholeResource)r;		            		
		            		String theSpec = wr.getSystemSpec();
		            		// HO 05/03/2012 BEGIN ************
		            		// HO 02/03/2012 BEGIN **********
			    			VUE.notifynum = 4;
			    			// HO 02/03/2012 END **********
		            		//File potentiallyLockedFile = new File(theSpec);
		            		File potentiallyLockedFile = findRelativizedFileToLock(theSpec, map);
		            		// HO 05/03/2012 END ************
		            		// HO 02/03/2012 BEGIN **********
			    			VUE.notifynum = 5;
			    			VUE.biggestnum = 0;
			    			// HO 02/03/2012 END **********
		            		File lockFile = isFileWritableByCurrentUser(potentiallyLockedFile);
		            		// HO 02/03/2012 BEGIN **********
			    			VUE.notifynum = 6;
			    			// HO 02/03/2012 END **********
		            		if ((lockFile != null) && (lockFile != potentiallyLockedFile)) {
		            			// HO 02/03/2012 BEGIN **********
		    	    			VUE.notifynum = 7;
		    	    			// HO 02/03/2012 END **********
		            			fileNames.add(potentiallyLockedFile.getName());
		            			// HO 02/03/2012 BEGIN **********
		    	    			VUE.notifynum = 8;
		    	    			// HO 02/03/2012 END **********
		            		}
		                }
		            	r = null;
		            }
		            wn = null;
		    }
		}
		coll = null;
		
		String strNamesToShow = "";
		// HO 02/03/2012 BEGIN **********
		VUE.notifynum = 9;
		// HO 02/03/2012 END **********
		
		if (!fileNames.isEmpty()) {
			// HO 02/03/2012 BEGIN **********
			VUE.notifynum = 10;
			// HO 02/03/2012 END **********
			int i;
			int j = fileNames.size() - 1;
			for (i = 0; i <= j; i++) {
				String strNextName = fileNames.elementAt(i).toString();
				strNamesToShow = strNamesToShow + strNextName + "\n";				
			}
		}
		
		if (strNamesToShow != "") {
			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
	                "The following files are locked for writing:\n"
					+ strNamesToShow
	            	+ "Wormholes in these files will not be updated.\n",
	            "File locked by other user.", 
	            JOptionPane.ERROR_MESSAGE);
		}
		// HO 02/03/2012 BEGIN **********
		VUE.notifynum = 11;
		// HO 02/03/2012 END **********
		
		
    }
    
    /**
     * A function to find the relativized target file
     * that may or may not be locked.
     * @param targetSpec, a String spec in search of a file
     * @param baseMap, the map relative to which the Spec is
     * @return theFile, the relativized target file that may or may not be locked
     * @author Helen Oliver
     */
    public static File findRelativizedFileToLock(String targetSpec, LWMap baseMap) {
    	// input validation
    	if ((targetSpec == null) || (targetSpec == ""))
    		return null;
    	if (baseMap == null)
    		return null;
    	
    	// parent path of base map as a string
    	String strParentPath = baseMap.getFile().getParent();
    	
    	// turn the URI into a String in case of character clashes
    	String systemSpec = VueUtil.decodeURIStringToString(targetSpec);
    	// HO 09/03/2012 BEGIN ********
    	// make sure slashes are going in the same direction
    	systemSpec = VueUtil.switchSlashDirection(strParentPath, systemSpec);
    	// HO 09/03/2012 END **********
    	
    	// create a file from the spec
    	File theFile = new File(systemSpec);
    	// save the original
    	File origFile = theFile;
    	
    	// get the parent path of the base map
    	URI parentMapURI = VueUtil.getParentURIOfMap(baseMap); 
    	// the parent path as a string
    	
    	// name of target file
    	String strTargetName = theFile.getName();
		    	
		// HO 16/03/2012 BEGIN *******
    	// get the URI from the system spec
    	// URI systemSpecURI = VueUtil.getURIFromString(systemSpec);
    	
		// FIRST ATTEMPT
		// resolve the file relative to the source map    	
		// see if the file is valid "in situ"
		if ((theFile != null) && (theFile.isFile()))
			return theFile;
				
		// SECOND ATTEMPT
		// HO 16/03/2012 END *********
		// try to resolve it relative to the source map
		URI systemSpecURI = VueUtil.getURIFromString(targetSpec);
		// use the URI to resolve it against the base map
		theFile = VueUtil.resolveTargetRelativeToSource(systemSpecURI, parentMapURI);
		// if this gives us the file, return it
		if ((theFile != null) && (theFile.isFile())) {
			// if the file has the right name, just assume it is
			// the right file
			return theFile;
		} 
		// THIRD ATTEMPT	
		//if we still can't find the file, check for one with the same name
		// in the local folder
		try {
				if ((strParentPath != null) && (strParentPath != "")) {	
					// if we got the parent path, create a file out of it
					theFile = new File(strParentPath, strTargetName);
					// if it's a valid file, check and see if the target node
					// exists in it
					if (theFile.isFile()) {
						return theFile;
					}
			} 
		} catch (Exception e) {
			// do nothing
		} 
		// FOURTH ATTEMPT
		if ((theFile != null) && (!theFile.isFile()))  {
			// if we still can't find it in the local folder, 
			// search all the subfolders	    
			File targFile = VueUtil.lazyFindTargetInSubfolders(strParentPath, strParentPath, strTargetName);
			// if a matching filename was found, return it
			if (targFile != null) {
				return theFile;
			}
		} 
		
		// FIFTH ATTEMPT
		if ((theFile != null) && (!theFile.isFile())) {
			// look in the above-folders
			File targFile = VueUtil.lazyFindTargetAboveCurrentPath(strParentPath, strParentPath, strTargetName);
			// if the target node was found in this map, open it
			if (targFile != null) {
				// return the file
				return theFile;
			}
		} 
			
    	// LAST ATTEMPT		
		// if the file can't be relativized, maybe it can be
		// found in its original location
		//if ((origFile != null) && (origFile.isFile())) {
			//return origFile;
		//} 

		// by this time, we're in trouble because
		// we absolutely can't find it
		return null;
    }
    
    static class VueArchiveLockFileFilter implements FileFilter {

    	  public boolean accept(File pathname) {

    	    if (pathname.getName().endsWith(VueUtil.VueArchiveLockExtension))
    	      return true;
    	    return false;
    	  }
    	}
    
    // HO 23/03/2012 BEGIN ******
    // adding new .vdk file type
    static class DesignVueArchiveLockFileFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.designVueArchiveLockExtension))
  	      return true;
  	    return false;
  	  }
  	}
    // HO 23/03/2012 END ********
    
}
