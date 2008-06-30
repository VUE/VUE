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

package tufts.oki.remoteFiling;
import org.apache.commons.net.ftp.*;
import tufts.oki.shared.*;
import tufts.oki.OsidManager;
import java.io.*;
import java.util.*;

/**
 *  The RemoteFilingManager provides a means to represent a remote file system logically 
 *  in memory.  Operations on the remote file system is implemented using FTP.
 *  The RemoteFileManager creates the FTP client and active session.  Since osid.filing
 *  doesn't have provisions in ByteStore or Cabinet for opening a stream, those operations
 *  are included in the RemoteFilingManager.  Remote files may be opened for read or
 *  write access.  Paired with a local file manager, file uploading and downloading
 *  is possible.
 *  <p>
 *  The remote filing manager also implements the concept of current working directory
 *  and provides a number of methods for operating on it or its entries.
 *
 *  @author  Mark Norton
 *
 */
public class RemoteFilingManager extends tufts.oki.OsidManager implements osid.filing.FilingManager {
    private static final int BUFFER_SIZE = 1024;    //  Size of buffer to use.
    //private String rootBase = null;                 //  Path to the remote file system root.
    private RemoteCabinet root = null;              //  The root cabinet for a client-session.
    private RemoteCabinet cwd = null;               //  The current working directory.
    
    /** Creates a new instance of the RemoteFileManager */
    public RemoteFilingManager() {
        super();
    }
    
    /**
     *  Create a client by connecting to the given host using the given username & password.
     *  The rootBase is initialized by grabbing the current working directory after establishing
     *  the connection.  A root cabinet named "/" is created to represent the remote root
     *  in the file system.
     *  <p>
     *  The remote server name, username, and password are all cached in the RemoteFilingManager.
     *  This is necessary to re-establish connections that time out remotely.
     *
     *  @author Mark Norton
     */     
    public void createClient(String host, String username, String password) throws osid.filing.FilingException {
        try {
            RemoteClient rc = new RemoteClient (host, username, password);
            FTPClient client = rc.getClient();
            tufts.oki.shared.Agent agent = new tufts.oki.shared.Agent(username, new tufts.oki.shared.AgentPersonType());
            this.root = new RemoteCabinet("/", agent, null,rc);
            this.cwd = this.root;           //  Set the root as current working directory.
            cwd.entries();
        }
        catch (osid.shared.SharedException ex2) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
        catch (osid.OsidException ex3) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
    }
     
    /**
     *  Return the root RemoteCabinet.
     */
    public RemoteCabinet getRoot () {
        return root;
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
        
        /*  Search the root cabinets for the entry with id.  */
        RemoteCabinet entry = root;
        osid.shared.Id entry_id = root.getId();

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
     *  The cabinet entry being deleted is assumed to be a root cabinet.  To delete
     *  sub-cabinets, see Cabinet.remove();  If the entry is not found, nothing happens.
     *
     *  @author Mark Norton
     *
     */
    public void delete(osid.shared.Id cabinetEntryId) throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Return an iterator over all root cabinets.  Since a remote filing system can only
     *  have a single root, a temp vector is created and the single root added to it.
     *
     *  @author Mark Norton
     *
     *  return A CabinetEntryIterator which lists all root cabinets.
     */
    public osid.filing.CabinetEntryIterator listRoots() throws osid.filing.FilingException {
        Vector vect = new Vector(10);
        vect.add (root);
        osid.filing.CabinetEntryIterator it = (osid.filing.CabinetEntryIterator) new RemoteCabinetEntryIterator(vect);
        return it;
    }

    /*  Remote File System Operations  */
    /*  -----------------------------  */
    
    /**
     *  Get the current working directory as a RemoteCabinet object.
     *
     *  @author Mark Norton
     */
    public RemoteCabinet getWorkingDirectory () {
        return this.cwd;
    }

    /**
     *  Set the working directory to the Cabinet indicated.
     *
     *  @author Mark Norton
     */
  //These method's should be either in RemoteClient or RemoteClient should be passed to them
  //Commented them, NEED TO REDESIGN- Anoop
    public void setWorkingDirectory (RemoteCabinet cabinet) throws osid.filing.FilingException {
        throw new osid.filing.FilingException(osid.filing.FilingException.UNSUPPORTED_OPERATION );
        /**
        cwd = cabinet;
        
        //  Do a remote Change Working Directory command.
        try {
            FTPClient client = RemoteClient.getClient();
            String current = client.printWorkingDirectory();
            client.changeWorkingDirectory(cabinet.getFullName());
        }
        catch (java.io.IOException ex) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
         */
    }
    
    /**
     *  Set the working directory to the Cabinet name indicated.  Supports changing
     *  directories to a sub-directory in the curent working directory.  Also supports
     *  a popd type command by passing "..", which causes the parent of the current
     *  directory to become active.
     *
     *  @author Mark Norton
     */
    public void setWorkingDirectory (String cabName) throws osid.filing.FilingException {
          throw new osid.filing.FilingException(osid.filing.FilingException.UNSUPPORTED_OPERATION );
          /**
        osid.filing.CabinetEntry entry = cwd.getCabinetEntryByName(cabName);
        
        if (!(entry instanceof RemoteCabinet)) {
            throw new osid.filing.FilingException (osid.filing.FilingException.NOT_A_CABINET);
        }
        
        //  Check the popd case where name is given as "..".
        if (cabName.compareTo("..") == 0) {
            entry = cwd.getParent();
        }
        //  Check for the local cabinet case.
        else {
            entry = cwd.getCabinetEntryByName(cabName);
        }
        
        //  Change the working directory remotely.
        try {
            FTPClient client = RemoteClient.getClient();
            String current = client.printWorkingDirectory();
            client.changeWorkingDirectory(current+ "/" + entry.getDisplayName());
        }
        catch (java.io.IOException ex) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
        
        cwd = (RemoteCabinet) entry;
        cwd.entries();      //  Make sure it is initialized
           */
    }
    
    /**
     *  List the contents of the current working directory by returning a 
     *  CabinetEntryIterator for entries in this directory.  Note that the entries
     *  in this iterator are RemoteCabinetEntry's.
     *
     *  @author Mark Norton
     */
    public osid.filing.CabinetEntryIterator list () throws osid.filing.FilingException {
        return cwd.entries();
    }
        
}
