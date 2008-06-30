/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

/*
 * FileManager.java
 *
 * Created on September 16, 2003, 9:31 AM
 *
 *  The software contained in this file is copyright 2003 by Mark J. Norton, all rights reserved.
 */

package tufts.oki.localFiling;
import java.io.*;
import java.util.*;

import javax.swing.filechooser.FileSystemView;

import tufts.oki.shared.*;

/**
 *  The Local Filing Manager manages a local filing system.  It allows manipulation of
 *  both directories and files via Cabinets and ByteStores.  The LocalFilingManager
 *  initializes a set of roots based on drive letters (if PC) or slash (if Unix or Mac).
 *  In addition, an addRoot() method is provided to take an arbitrary path and treat it
 *  as another root.  This allows a GUI to avoid showing unneeded level to deeply nexted 
 *  directories.
 *  <p>
 *  The system defines a current working directory, which is initially the root (or C:\), but can
 *  be changed to any directory added to this filing system.  To avoid an excessive amount
 *  of information in memory at one time, directories below the working directory can be 
 *  opened using LocalCabinet.entries().  This adds CabinetEntry's to the current directory
 *  corresponding to sub-directories and files.
 *
 *  @author  Mark Norton
 *
 */
public class LocalFilingManager extends tufts.oki.OsidManager implements osid.filing.FilingManager
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LocalFilingManager.class);
    
    private static final int BUFFER_SIZE = 1024;    //  Size of buffer to use.
    public boolean trace = false;       //  Set this to true to trace operations.
    private SortedSet rootCabinets = null;
    //private LocalCabinet root = null;  //  The root cabinet for a client-session.
    private LocalCabinet cwd = null;   //  The current working directory.
    
    /** 
     *  Creates a new instance of the LocalFileManager 
     */
    public LocalFilingManager() throws osid.filing.FilingException {
        super();
        rootCabinets = new TreeSet(new LocalCabinetEntryComparator());
        initializeRoots();
    }
    
    /**
     *  Add the path as a root of the local file system.  This root need not be a disk drive
     *  root.  It can be any directory on the local file system.  This subsequently
     *  serves as a virtual root.
     *
     *  @author Mark Norton
     */
    public void addRoot (String path) throws osid.filing.FilingException {
        LocalCabinet root = null;
        try {
            osid.shared.Agent agent = new Agent("unknown", new AgentPersonType());
            if (Log.isDebugEnabled()) Log.debug(this + "; addRoot " + path);
            root = LocalCabinet.instance(path, agent, null);
            rootCabinets.add(root);
            cwd = root;
        }
        catch (osid.shared.SharedException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
        catch (osid.OsidException ex3) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
        
        /*  This code is obsolete now that rootBase is kept by the root Cabinet itself.
        //  Remove final node from root path name.
        String[] parts = this.explodePath(path, root.separatorChar());
        if (trace)
            System.out.println ("setRoot - rootBase part count: " + parts.length);
        
        if (parts.length > 1) {
            String[] baseParts = new String[parts.length];
            for (int i = 0; i < parts.length; i++)
                baseParts[i] = parts[i];
            //rootBase = this.implodePath (baseParts, root.separator());
        }
        //else
            //rootBase = parts[0];
         */
        
        //openDirectory();   //  Open the root directory.
        root.entries();     //  Initialize the entries in this directory.
    }
    
   /**
     * Gets the root cabinets for the local file system.
     */
    
//     private static final ArrayList list = new ArrayList();

//     {
//         Log.debug("File.listRoots...");
//     	File[] roots = File.listRoots();
//     	for (int i=0;i<roots.length;i++)
//             list.add(roots[i]);
//         Log.debug("File.listRoots: " + list);
//     }
    
    
    private void initializeRoots() throws osid.filing.FilingException {
        final String[] drives = {"C","D","E","F","G","H","I","J","K","L","M","N",
                                 "O","P","Q","R","S","T","U","V","W","X","Y","Z"};

        Log.debug("initializeRoots; in " + tufts.Util.tags(this) + "...");
        
        //  Create  dummy owner.
        osid.shared.Agent agent = null;
        try {
            agent = new Agent("unknown", new AgentPersonType());
        }
        catch (osid.shared.SharedException ex) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
        
        //  If there are no roots, then scan the PC drive letters and try to open each.
        //  If files exist, then add that drive as a root.


        if (tufts.Util.isWindowsPlatform()) {

        	if (rootCabinets.size() == 0) {
                    File[] f = File.listRoots();
                    for (int i=0;i<f.length;i++) {
                        File file = f[i];
                        LocalCabinet newRoot = LocalCabinet.instance(file.toString(), agent, null);
                        rootCabinets.add (newRoot);
                        if (drives[i].compareTo("C:") == 0)
                            cwd = newRoot;
                        
                    }
                }
                
        } else {
        		
        	if (rootCabinets.size() == 0) {

                    for (int i = 0; i < drives.length; i++) {
            	  
                        //-----------------------------------------------------------------------------
                        // TODO: Why are we looking for drives on non-window platforms?
                        // Are there cases where such files could ever exists?  SMF 2008-04-16
                        //-----------------------------------------------------------------------------
                    
                        File file = new File(drives[i]+":" + java.io.File.separator);
            	
                        //	* Trying out a test for removable disks here...
     
                        boolean isRemovableDisk = false;
            		
                        //isRemovableDisk = view.getSystemTypeDescription(file).equals("Removable Disk");
                        //	System.out.println("is removable disk : " + isRemovableDisk);
                        //}
            	
                        //isRemovableDisk = !FileSystemView.getFileSystemView().getSystemTypeDescription(file).equals("Removable Disk");
                
                        if (!isRemovableDisk && file.exists()) {
                            if (Log.isDebugEnabled()) Log.debug(this + "; checking " + file);
                            String idStr = drives[i]+":" + java.io.File.separator;
                            //this.rootCabinets.put(idStr, new LocalCabinet(this, null, idStr));
                            LocalCabinet newRoot = LocalCabinet.instance(idStr, agent, null);
                            //	LocalCabinet newRoot = new LocalCabinet (idStr, agent, null);
                            rootCabinets.add (newRoot);
                            if (drives[i].compareTo("C:") == 0)
                                cwd = newRoot;
                        }                
                    }
                }
        	
        }
        
        //  If this is not a PC environment, drive letters won't likely work, so try to
        //  open a Unix root, "/".  This is likely to work for Mac OS-X as well.
        
        // 2004-10-11 SMF: If this is Mac OS X, we should grab the items
        // in /Volumes, which is all mounted drives (including network), and
        // a symlink to /, named as whatever the user wanted to name their hard-drive
        // (e.g., "Macintosh HD")
        
        if (rootCabinets.size() == 0) {
            File file = new File ("/");
            if (file.exists()) {
                rootCabinets.add (new LocalCabinet ("/", agent, null));
            }
        }
        
        //  If the current working directory has not been set, set it to the first root.
        if ((cwd == null) && rootCabinets.size() > 0)
            cwd = (LocalCabinet) rootCabinets.first();

        if (Log.isDebugEnabled()) Log.debug("initializeRoots; in " + tufts.Util.tags(this) + ": completed.");
        
    }

    /**
     *  No mention is made if the entry in question is a cabinet or not.  Since only
     *  root cabinets exist at FilingManager level, we can probably assume that the
     *  search for this entry should recurse to sub-cabinents.
     *
     *  @author Mark Norton
     *
     */
    public osid.filing.CabinetEntry getCabinetEntry(osid.shared.Id id) throws osid.filing.FilingException {

        osid.filing.CabinetEntry found = null;
        
        osid.filing.CabinetEntryIterator it = listRoots();
        
        while (it.hasNext()) {
            /*  Search the root cabinets for the entry with id.  */
            LocalCabinet entry = (LocalCabinet) it.next();
            //LocalCabinet entry = root;
            osid.shared.Id entry_id = entry.getId();

            /*  Check to see if the entry we are looking for is a root cabinet.  */
            try {
                if (entry_id.isEqual(id))
                    found = entry;

                /*  Otherwise, check sub-cabinets to see if it's in there.  */
                else {
                    try {
                        found = entry.getCabinetEntryById(id);
                    }
                    catch (osid.filing.FilingException ex) {
                        /*  An UNKNOWN_ID exception is expected.  Continue search. */
                    }
                }
            }
            catch (osid.shared.SharedException ex) {
                /*  isEqual doesn't really throw an exception.  */
            }
        }
        
        /*  If found is null at this point, we didn't find it.  */
        if (found == null)
            throw new osid.filing.FilingException (osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
        return found;
    }
    
    /**
     *  The documentation indicates that the cabinet being deleted must be empty and
     *  that the owner must have permissions to do this.  Permissions are not implemented
     *  at this time.
     *  <br>
     *  The cabinet entry being deleted is assumed to be the root cabinet.  To delete
     *  sub-cabinets, see Cabinet.remove();  If the entry is not found, nothing happens.
     *
     *  @author Mark Norton
     *
     */
    public void delete(osid.shared.Id cabinetEntryId) throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Return an iterator over all root cabinets.  The osid.filing interface supports
     *  multiple file roots.  The LocalFilingManager only supports a single root.  As
     *  such, this creates a interator which lists a single root.
     *
     *  @author Mark Norton
     *
     *  return A CabinetEntryIterator which the root cabinet.
     */
    public osid.filing.CabinetEntryIterator oldlistRoots() throws osid.filing.FilingException {
        Vector vect = new Vector(10);
        SortedSet set = new TreeSet();
        //vect.add (root);
        osid.filing.CabinetEntryIterator it = (osid.filing.CabinetEntryIterator) new LocalCabinetEntryIterator(set);
        return it;
    }
    public osid.filing.CabinetEntryIterator listRoots() throws osid.filing.FilingException {
        return new LocalCabinetEntryIterator (rootCabinets);
    }


    /*  Local File System Operations  */
    /*  ----------------------------  */
    
    /**
     *  Open and intialized the LocalCabinet given if not done previously.
     */
    public void openDirectory (LocalCabinet dir) throws osid.filing.FilingException {
        dir.entries();  //  executed for the side effect of opening it.
    }
    public void XXopenDirectory (LocalCabinet dir) throws osid.filing.FilingException {
        
        //  Check to see if previuosly initialized.  Force open and return.
        if (dir.isInitialized()) {
            dir.setOpen (true);
            return;
        }
        //  Initialize the directory by getting all entries contained in it.
        String[] files = null;

        //System.out.println ("Open Directory: " + this.cwd.getDisplayName());
        files = dir.getFile().list();
        if (trace)
            System.out.println ("openDirectory - files to open: " + files.length + " in " + dir.getFile().getAbsoluteFile());

        // Iterate over the files returned and create CabinetEntries for them.
        // Note that there is a lot of other information in the FTPFile objects which
        // could be added to the entries being created here.  In particular, creation
        // date.
        //String path = rootBase + dir.getFullName();
        String rootBase = dir.getRootBase();
        String path = rootBase + dir.getFullName();
        if (trace)
            System.out.println ("openDirectory - path name: " + path);
        for (int i = 0; i < files.length; i++) {
            //File temp = new File (cwd.getPath(), files[i]);
            File temp = new File (rootBase+dir.getFullName(), files[i]);
            if (trace)
                System.out.println ("openDirectory - new file: " + rootBase + dir.getFullName() + files[i]);

            String absolute = null;
            if (dir.isRootCabinet())
                absolute = path + temp.getName();
            else
                absolute = path + dir.separator() + temp.getName();
            
            if (temp.isDirectory()) {
                if (trace)
                    System.out.println ("\tDir " + i + ": " + temp.getName() + "\t" + absolute);
                //cwd.createCabinet (temp.getName());
                cwd.createCabinet (absolute);
            }
            else if (temp.isFile()) {
                if (trace)
                    System.out.println ("\tFile " + i + ": " + temp.getName() + "\t" + absolute);
                //cwd.createByteStore (temp.getName());
                cwd.createByteStore (absolute);
            }

            //  Unknown cases are ignored.
        }
        
        //  The current working directory is now set to open and intialized.
        dir.setOpen (true);
        dir.setInitialized (true);
    }
    
    /**
     *  Open directory without an argument causes the current working directory to be 
     *  opened and intialized, if not done previously.
     */
    public void openDirectory () throws osid.filing.FilingException {
        this.openDirectory (cwd);
    }

    /**
     *  Close the current working directory.  Note that this does not deallocate entries
     *  included in the directory.  It merely marks it as closed.
     *
     *  @author Mark Norton
     */
    /*  Obsolete.
    public void closeDirectory() {
        cwd.setOpen(false);
    }
     */
    
    /**
     *  Set the working directory to the Cabinet indicated.
     *
     *  @author Mark Norton
     */
    public void setWorkingDirectory (LocalCabinet cabinet) throws osid.filing.FilingException {

        if (trace)
            System.out.println ("setWorkingDirectory.1 - cabinet to open: " + cabinet.getDisplayName());

        this.cwd = cabinet;
        openDirectory();   //  Open it up.
    }
    
    /**
     *  Set the working directory to the Cabinet name indicated.
     *  <p>
     *  Three cases are supported by this command.  If an isolated cabinet name is given, that
     *  cabinet is assumed to be in the current working directory.  This is like the Unix
     *  command pushd.  If the cabinet name is given as "..", the parent of the current
     *  working directory is assumed.  This is like the Unix command popd.  Finally, if
     *  a full path name is given, the current working directory is set to the cabinet
     *  specified by the path.  Note that this path name is relative to the local root
     *  and which means it is not prefixed by rootBase.
     *  <p>
     *  Examples, assume the local root is c:/dir1 and the cwd is c:/dir1/dir2:<br>
     *  1.  setWorkingDirectory ("dir3") -->  c:/dir1/dir2/dir3<br>
     *  2.  setWorkingDirectory ("..") --> c>/dir1<br>
     *  3.  setWorkingDirectory ("/dir2/dir3") --> c:/dir1/dir2/dir3<br>
     *  4.  setWorkingDirectory ("/dir2/dir3/dir4") --> c:/dir1/dir2/dir3/dir4 (dir3 is opened)
     *
     *  @author Mark Norton
     */
    public void setWorkingDirectory (String cabName) throws osid.filing.FilingException {
        osid.filing.CabinetEntry entry = null;
        
        if (trace)
            System.out.println ("setWorkingDirectory.2 - cabinet to open: " + cabName);

        String parts[] = this.explodePath(cabName, java.io.File.separatorChar);
        if (trace)
            System.out.println ("setWorkingDirectory.2 - node count is: " + parts.length);
        
        //  Check the popd case where name is given as "..".
        if (cabName.compareTo("..") == 0) {
            entry = cwd.getParent();
        }
        //  Check for the local cabinet case.
        else if (parts.length == 1) {
            entry = cwd.getCabinetEntryByName(cabName);
        }
        //  Otherwise, we have a path to unravel.
        else {
            entry = walkPath (cabName);
        }
        
        //  Check to make sure that the entry found is indeed a LocalCabinet.
        if (!(entry instanceof LocalCabinet)) {
            throw new osid.filing.FilingException (osid.filing.FilingException.NOT_A_CABINET);
        }
        
        //  Set the new entry to be the cached current working directory.
        cwd = (LocalCabinet) entry;

        //  Make sure it is open.
        openDirectory();
    }
    
    /**
     *  Return the active root as a LocalCabinet.  The active root is defined to be
     *  the root directory associated with the current working directory.
     */
    public LocalCabinet getRoot () {
        return (LocalCabinet) cwd.getRootCabinet();
    }

    /**
     *  List the contents of the current working directory by returning a 
     *  CabinetEntryIterator for entries in the working directory.  Note that the entries
     *  in this iterator are RemoteCabinetEntry's.
     *
     *  @author Mark Norton
     */
    public osid.filing.CabinetEntryIterator list () throws osid.filing.FilingException {
        return cwd.entries();
    }
    
    /**
     *  Get the current working directory as a RemoteCabinet object.
     *
     *  @author Mark Norton
     */
    public LocalCabinet getWorkingDirectory () {
        return this.cwd;
    }

    /**
     *  Open the name passed for input on the local file system.  The name must be 
     *  an entry in the current working directory.
     *
     *  @author Mark Norton
     */
    public InputStream openForInput (String name) throws osid.filing.FilingException {

        //  Find the entry for name.
        osid.filing.CabinetEntry theEntry = null;
        osid.filing.CabinetEntryIterator it = cwd.entries();
        while (it.hasNext()) {
            osid.filing.CabinetEntry entry = it.next();
            if (name.compareTo(entry.getDisplayName()) == 0) {
                theEntry = entry;
                break;
            }
        }
        
        // Open the file for input access. 
        FileInputStream stream = null;
        File file = ((LocalByteStore)theEntry).getFile();
        if (trace)
            System.out.println ("openForInput - file to open: " + file.getAbsolutePath());
        try {
            stream = new FileInputStream (file);
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
        
        return stream;
    }

    public void closeInput (InputStream stream) throws java.io.IOException {
        stream.close();
    }

    /**
     *  Open the name passed for output on the local file system.  If the file named
     *  exists, an exception is thrown.  If it doesn't exist, it is created in the
     *  current working directory.
     *
     *  @author Mark Norton
     */
    public OutputStream openForOutput (String name) throws osid.filing.FilingException {

        FileOutputStream stream = null;
        
        //  Get the path to the current working directory.
        String rootBase = cwd.getRootBase();
        String path = rootBase + cwd.getFullName();
        
        //  Make a temporary file.
        File temp = new File (rootBase + cwd.getFullName(), name);
        if (trace)
            System.out.println ("openForOutput - new file: " + rootBase + cwd.getFullName() + name);

        //  Create the absolute file name.
        String absolute = null;
        if (cwd.isRootCabinet())
            absolute = path + temp.getName();
        else
            absolute = path + cwd.separator() + temp.getName();
            
        //  Check to see if the file already exists.
        try {
            cwd.getCabinetEntryByName(absolute);
            new osid.filing.FilingException (osid.filing.FilingException.ITEM_ALREADY_EXISTS);
        }
        catch (osid.filing.FilingException ex1) {
            //  If the file doesn't exist, that's good!
        }
        
        //  Open the file for output access.  Add it to the working directory.
        try {
            LocalByteStore bs = (LocalByteStore)cwd.createByteStore (absolute);
            File file = bs.getFile();
            if (trace)
                System.out.println ("openForInput - file to open: " + file.getAbsolutePath());
            stream = new FileOutputStream (file);
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
        
        return stream;
    }

    public void closeOutput (OutputStream stream) throws java.io.IOException {
        stream.close();
    }

    /**
     *  Copy the input stream to the output stream.
     *  <p>
     *  This is currently implemented as a byte by byte copy, but it's reasonably
     *  efficient, since the copying is all done in memory with periodic flushing out
     *  to disk.
     *
     *  @author Mark Norton
     */
    public void copy (InputStream in, OutputStream out) throws osid.filing.FilingException {
        //int data[] = new int[RemoteFilingManager.BUFFER_SIZE/4];
        
        BufferedInputStream bin = new BufferedInputStream (in, LocalFilingManager.BUFFER_SIZE);
        BufferedOutputStream bout = new BufferedOutputStream (out, LocalFilingManager.BUFFER_SIZE);
        try {
            while (true) {
                int datum = bin.read();
                if (datum == -1)
                    break;
                bout.write(datum);
            }
            bout.flush();
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
    }

    /**
     *  Rename the cabinet entry given to a new name.
     *  Works for both cabinets and byte stores.
     *
     *  @author Mark Norton
     */
    public void rename (osid.filing.CabinetEntry old, String absolute, String newName) throws osid.filing.FilingException {
        if (old instanceof LocalByteStore) {
            ((LocalByteStore)old).rename (absolute, newName);
        }
        else if (old instanceof LocalCabinet) {
            ((LocalCabinet)old).rename (absolute, newName);
        }
        else
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
    }
    
    /**
     *  Find the oldName given in the current working directory and rename it to newName.
     *  Works for both cabinets and byte stores.
     *
     *  @author Mark Norton
     */
    public void rename (String oldName, String newName) throws osid.filing.FilingException {
        osid.filing.CabinetEntry entry = cwd.getCabinetEntryByName(oldName);
        String rootBase = cwd.getRootBase();
        String absolute = rootBase + cwd.getFullName() + newName;
        if (trace)
            System.out.println ("rename - new name: " + absolute);
        this.rename (entry, absolute, newName);
    }
    
    /**
     *  Delete the entry given from the current working directory.
     */
    public void delete (osid.filing.CabinetEntry entry) throws osid.filing.FilingException {
        cwd.remove (entry);
    }
    
    /**
     *  Create a directory of the given name in the current working directory.
     *
     *  @author Mark Norton
     */
    public void createDirectory (String name) throws osid.filing.FilingException {
        String rootBase = cwd.getRootBase();
        String path = rootBase + cwd.getFullName();
        if (trace)
            System.out.println ("createDirectory - path: " + path);

        File temp = new File (cwd.getFullName(), name);

        String absolute = null;
        if (cwd.isRootCabinet())
            absolute = path + temp.getName();
        else
            absolute = path + cwd.separator() + temp.getName();

        if (trace)
            System.out.println ("createDirectory - named " + ": " + temp.getName() + "\t" + absolute);

        File fin = new File (absolute);
        if (fin.mkdir()) {
            cwd.createCabinet (absolute);
            if (trace)
                System.out.println ("createDirectory - mkdir result: false");
        }
        else {
        if (trace)
            System.out.println ("createDirectory - mkdir result: false");
        }
    }
    
    /*  Utility Methods  */
    /*  ---------------  */
    
    /**
     *  Get the path of the root cabinet.
     *
     *  @author Mark Norton
     */
    public String getRootPath () {
        LocalCabinet root = (LocalCabinet) cwd.getRootCabinet();
        String rootBase = cwd.getRootBase();
        return rootBase + root.getFullName();
    }
    
    
    /**
     *  Get the rootBase.
     */
    public String getRootbase() {
        String rootBase = cwd.getRootBase();
        return rootBase;
    }
    
    /**
     *  Parse a path using the separator character provided.  The parts of the
     *  path are returned as an array of strings.
     *  <p>
     *  The path provided must be constructed with the separator character given.
     */
    public String[] explodePath (String path, char separator) {
        ArrayList parts = new ArrayList(100);
        //char[] node = new char[256];
        StringBuffer node = new StringBuffer(256);
        
        //  Parse the pathname according to separator provided.
        for (int i = 0; i < path.length(); i++) {
            
            //  If not at the end of a node, copy the characters of the node.
            if (path.charAt(i) != separator) {
                node.append (path.charAt(i));
            }
            //  Otherwise, add this node to the node list.
            else {
                parts.add (node.toString());
                node.setLength(0);
            }
        }
        //  Add the end of the path.
        if (node.length() != 0)
            parts.add(node.toString());
        
        //  Copy the ListArray into a String array.  I shouldn't have to do this, but
        //  ListArray.toArray() would cast into String[] properly.
        String[] strParts = new String[parts.size()];
        for (int j = 0; j < parts.size(); j++)
            strParts[j] = (String)parts.get(j);
        
        return strParts;
    }
    
    /**
     *  Assemble a path string from the parts and separator given.
     */
    public String implodePath (String[] parts, String separator) {
        StringBuffer path = new StringBuffer(1024);
        
        if (parts.length > 0) {
            path.append(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                path.append(separator);
                path.append(parts[i]);
            }
        }
        
        return path.toString();
    }
    
    /**
     *  Given a path to a directory name, walk the path and return the LocalCabinet
     *  corresponding to the terminal node.  If a path node is not currently open,
     *  open it and proceed.
     *  <p>
     *  Please note that the path name given should be relative to the local root.
     *  That means that the first node in the path should correspond to the root
     *  cabinet held by this filing manager.
     */
    public LocalCabinet walkPath (String path) throws osid.filing.FilingException {
        
        LocalCabinet root = (LocalCabinet) cwd.getRootCabinet();

        //  Parse out the path.
        String parts[] = this.explodePath(path, cwd.separatorChar());
        
        //  Check to make sure that the first node is indeed the root node.
        if (root.getDisplayName().compareTo(parts[0]) != 0) {
            throw new osid.filing.FilingException (osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
        }
        
        //  Starting at the root, work down the path to the directory indicated.
        osid.filing.Cabinet ptr = (osid.filing.Cabinet) root;
        for (int i = 1; i < parts.length; i++) {
            //  Make sure that the directory we are at is open.
            if (! ((LocalCabinet)ptr).isOpen())
                openDirectory((LocalCabinet)ptr);
            
            //  Find the next node and set path pointer to that cabinet.
            ptr = (osid.filing.Cabinet) ptr.getCabinetEntryByName(parts[i]);
        }
        
        return (LocalCabinet) ptr;
    }
}
