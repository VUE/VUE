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

/*
 * C abinet.java
 *
 *  Created on September 17, 2003, 10:04 AM
 */

package tufts.oki.remoteFiling;
import tufts.oki.shared.*;
import org.apache.commons.net.ftp.*;

import java.io.*;
import java.util.*;
import osid.OsidException;

/**
 *  RemoteCabinet is a implementation of osid.filing.Cabinet which models a remote
 *  directory accessed via FTP.  In order to save internal memory space, a cabinet
 *  is not initialized to the entries in it until entries() is called.  The add()
 *  method is provided to allow new entries in this directory to be included in its
 *  list of entries.  The createCabinet() and createByteStore() methods are used to
 *  create a new directory or new file, respectively.
 *
 *  @author  Mark Norton - OKI compatibility.
 *  @author  Salem Berhanu - much of the FTP transactions.
 *
 */
public class RemoteCabinet extends RemoteCabinetEntry implements osid.filing.Cabinet {
    /* parent is inherited from Cabinet Entry.  */
    Vector children = null;
    tufts.oki.shared.Properties properties = null;
    private boolean initialized = false;
    //private boolean open = false;
    //private File dir = null;               //  The remote directory being modeled.
    
    /**
     * Initializes the feilds interited from CabinetEntry and adds a vector of children.
     * Creates a Properties object to hold properties associated with this cabinet.
     *
     * @author Mark Norton
     *
     */
    public RemoteCabinet(String displayName, osid.shared.Agent agentOwner, osid.filing.Cabinet parent, RemoteClient rc) {
        super(displayName, agentOwner, parent,rc);
        children = new Vector(100);
        //FilingCabinetType type = new FilingCabinetType();
        //properties = new osid_mjn.shared.Properties(type);
        
        if (parent == null)
            updateDisplayName("");
        else
            updateDisplayName(displayName);
    }
    
    /**
     *   Add the entry to the list of children.
     *
     *   @author Mark Norton
     */
    public void add(osid.filing.CabinetEntry entry, java.lang.String name) throws osid.filing.FilingException {
        
        /* Update the display name in entry with name.  Does this make sense?  */
        entry.updateDisplayName(name);
        
        /*  Add the element to the Vector array.  */
        children.addElement(entry);
    }
    
    /**
     *   Add the entry to the list of children.
     *
     *   @author Mark Norton
     */
    public void add(osid.filing.CabinetEntry entry) throws osid.filing.FilingException {
        
        /*  Add the element to the Vector array.  */
        children.addElement(entry);
    }
    
    /*
     *  The oldByteStore is copied into the new ByteStore.
     *
     *  @author Mark Norton
     *
     *  @return A new ByteStore with the name provided and this cabinet as parent.
     */
    public osid.filing.ByteStore copyByteStore(String name, osid.filing.ByteStore oldByteStore) throws osid.filing.FilingException {
        throw new osid.filing.FilingException(osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Currently unimplemented.  A lot depends on the eventual use of the filing
     *  system implemented by these classes.  Not all systems will have a restriction
     *  on available and used byes.
     *
     *  @author Mark Norton
     *
     *  @return The number of bytes available in this cabinet.
     */
    public long getAvailableBytes() throws osid.filing.FilingException {
        throw new osid.filing.FilingException(osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Currently unimplemented.  A lot depends on the eventual use of the filing
     *  system implemented by these classes.  Not all systems will have a restriction
     *  on available and used byes.
     *  <p>
     *  This could be implemented as the sum of of the sizes of the ByteStores in this
     *  directory, but its not clear what use that would be.
     *
     *  @author Mark Norton
     *
     *  @return The number of bytes used in this cabinet.
     */
    public long getUsedBytes() throws osid.filing.FilingException {
        throw new osid.filing.FilingException(osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Create a new ByteStore, add it to this Cabinet.
     *
     *  @author Mark Norton
     *
     *  @return A new ByteStore with the name provided and this cabinet as parent.
     */
    public osid.filing.ByteStore createByteStore(String name) {
        
        osid.filing.ByteStore bs = null;
        try {
            bs = new RemoteByteStore(name, this,rc);
            this.add(bs);
        } catch (osid.OsidException ex) {
        }
        return bs;
    }
    
    /**
     *  Create a new cabinet entry with
     *  the agentOwner of this new cabinet as this owner of this Cabinet.
     *
     *  @author Mark Norton
     *
     *  @return A new cabinet with the given displayName.
     */
    public osid.filing.Cabinet createCabinet(String displayName) throws osid.filing.FilingException {
        
        //  Create the new cabinet entry.
        osid.shared.Agent agentOwner = super.getCabinetEntryAgent();
        RemoteCabinet entry = new RemoteCabinet(displayName, agentOwner, this,rc);
        
        //  Make a directory on the remote file system.
        try {
            FTPClient client = rc.getClient();
            if (!client.makeDirectory(entry.getFullName()))
                throw new osid.filing.FilingException(osid.filing.FilingException.ITEM_ALREADY_EXISTS);
        } catch (java.io.IOException ex) {
            throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
        }
        
        /*  Add the element to the Vector array.  */
        children.addElement(entry);
        
        return entry;
    }
    
    /**
     *   Creates an iterator which lists the entries in this cabinet. A check is made to
     *   see if this cabinet was previously opened.  If not, it is opened and initialized
     *   with the entries contained in it.
     *
     *   @author Mark Norton
     *
     *   @return Return an iterator for the entries in this cabinet.
     */
    public osid.filing.CabinetEntryIterator entries() throws osid.filing.FilingException {
        //  Check to see if this cabinet is unopened.  If not, intialize it.
        if (!initialized) {
            //  Initialize the directory by getting all entries contained in it.
            FTPFile[] files = null;
            try {
                String rootBase = rc.getRootBase();
                RemotePath path = new RemotePath(rootBase, this);
                String pathname = path.getPathString();
                //System.out.println("PATHNAME:"+pathname);
                FTPClient client = rc.getClient();
                
                files = client.listFiles(pathname);    // Executes an FTP LIST command.
                osid.shared.Agent agentOwner = super.getCabinetEntryAgent();
                
                // Iterate over the files returned and create CabinetEntries for them.
                // Note that there is a lot of other information in the FTPFile objects which
                // could be added to the entries being created here.  In particular, creation
                // date.
                for (int i = 0;files != null && i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        //System.out.println ("\tDir " + i + ": " + files[i].getName());
                        //RemoteCabinet cab = (RemoteCabinet) this.createCabinet (files[i].getName());
                        add(new RemoteCabinet(files[i].getName(), agentOwner, this,rc));
                    } else if (files[i].isFile()) {
                        //System.out.println ("\tFile " + i + ": " + files[i].getName());
                        //RemoteByteStore store = (RemoteByteStore) this.createByteStore (files[i].getName());
                        add(new RemoteByteStore(files[i].getName(), this,rc));
                    }
                    //  Unknown cases are ignored.
                }
            } catch (java.io.IOException ex1) {
                throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
            }
            
            //  The current working directory is now intialized.
            initialized = true;
        }
        return (osid.filing.CabinetEntryIterator) new RemoteCabinetEntryIterator(children);
    }
    
    /**
     *  Get a cabinet entry given its name.
     *
     *  @author Mark Norton
     *
     *  @return The cabinet entry with the desired display name.  Throws ITEM_DOES_NOT_EXIST if name is unknown.
     */
    public osid.filing.CabinetEntry getCabinetEntryByName(String name) throws osid.filing.FilingException {
        
        for (int i = 0; i < children.size(); i++) {
            RemoteCabinetEntry entry = (RemoteCabinetEntry) children.elementAt(i);
            if (name.compareTo(entry.getDisplayName()) == 0) {
                return (osid.filing.CabinetEntry) entry;
            }
        }
        /*  ITEM_DOES_NOT_EXIST is not exactly the right sentiment for not finding the entry.  */
        throw new osid.filing.FilingException(osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
    }
    
    /**
     *  Get a cabinet entry given its identifier.  Throws ITEM_DOES_NOT_EXIST if Id is unknown.
     *
     *  @author Mark Norton
     *
     *  @return The cabinet entry corresponding to the identifier passed.
     */
    public osid.filing.CabinetEntry getCabinetEntryById(osid.shared.Id id) throws osid.filing.FilingException {
        
        for (int i = 0; i < children.size(); i++) {
            RemoteCabinetEntry entry = (RemoteCabinetEntry) children.elementAt(i);
            try {
                if (id.isEqual(entry.getId())) {
                    return (osid.filing.CabinetEntry) entry;
                }
            } catch (osid.shared.SharedException ex) {
                /*  Not exactly sure what could go wrong with an Id comparison,
                 *  but the compiler insists on catching this exception.
                 *  This will fall through to ITEM_DOES_NOT_EXIST exception throw.
                 */
            }
        }
        /*  ITEM_DOES_NOT_EXIST is not exactly the right sentiment for not finding the entry.  */
        throw new osid.filing.FilingException(osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
    }
    
    /**
     *  Return the properties associated with this cabinet as a HashMap.  This is better
     *  implmented using the expanded Properties objects defined elsewhere.
     *
     *  @author Mark Norton
     *
     *  @return The property set of the Properties object associated with this cabinet as a Map.
     */
    public java.util.Map getProperties() throws osid.filing.FilingException {
        HashMap map = properties.getPropertySet();
        return (Map) map;
    }
    
    /**
     *  Get the root cabinet of this cabinet.
     *
     *  @author Mark Norton
     *
     *  @return The root cabinet of this cabinet by searching up the parent links.
     */
    public osid.filing.Cabinet getRootCabinet() throws osid.filing.FilingException {
        osid.filing.Cabinet cab = this;
        while (cab.getParent() != null)
            cab = cab.getParent();
        return cab;
    }
    
    /**
     *  Currently, this always returns true, since cabinets are always defined to be
     *  listable.  This could be made a property instead.
     *
     *  @author Mark Norton
     *
     *  @return True if this cabinet is listable.
     */
    public boolean isListable() throws osid.filing.FilingException {
        return true;
    }
    
    /**
     *  Currently, this always returns true, since cabinets are defined to be managable
     *  in this implementation.  It could be made a property instead.
     *
     *  @author Mark Norton
     *
     *  @return True if this cabinet is managable.
     */
    public boolean isManageable() throws osid.filing.FilingException {
        return true;
    }
    
    /**
     *  Return true if this is a root cabinet, ie., its parent is null.
     *
     *  @author Mark Norton
     *
     *  @return True if this is a root cabinet (parent == null).
     */
    public boolean isRootCabinet() throws osid.filing.FilingException {
        return (this.getParent() == null);
    }
    
    /**
     *  Remove the entry indicated from the children of this cabinet.  The entry is
     *  identified by comparing the Ids of the entry passed to the Id of the children
     *  present.  This assumes that such Ids are globally unique.
     *  <p>
     *  No error is thrown if entry is not present.
     *  <p>
     *  Note:  remove doesn't remove the entry on the remote file system at this time.
     *
     *  @author Mark Norton
     */
    public void remove(osid.filing.CabinetEntry entry) throws osid.filing.FilingException {
        
        osid.shared.Id entry_id = entry.getId();
        for (int i = 0; i < children.size(); i++) {
            RemoteCabinetEntry ent = (RemoteCabinetEntry)children.elementAt(i);
            try {
                if (entry_id.isEqual(ent.getId())) {
                    children.remove(entry);
                }
            } catch (osid.shared.SharedException ex) {
                /*  Unlikely that isEqual() will throw a SharedException.  */
            }
        }
    }
    
    /**
     *  Return the number of children this cabinent has.
     *  <p>
     *  This is an extension to osid.filing.Cabinet to support classes which imlement
     *  javax.swing.tree.TreeModel.
     *
     *  @author Mark Norton
     *
     *  @return the number of children in this cabinet.
     */
    public int getChildCount() {
        return children.size();
    }
    
    /**
     *  Check to see if this cabinet is initialized.
     *
     *  @author Mark Norton
     *
     *  @return true if the cabinet is initialized.
     */
    public boolean isInitialized() {
        return this.initialized;
    }
    
    /**
     *  Set the initialized flag to the value given.
     *
     *  @author Mark Norton
     */
    public void setInitialized(boolean flag) {
        this.initialized = flag;
    }
    
    /**
     *  Get the full file name for this entry, including path to local root.
     *  <p>
     *  Warning!  This name cannot be used to open local files, since it does
     *  not include rootBase.  The absolute name can be created by concatenating
     *  rootBase and getFullName().
     *
     *  @author Mark Norton
     */
    public String getFullName() {
        StringBuffer fn = new StringBuffer(rc.getRootBase());
        fn = new StringBuffer();
        ArrayList parts = new ArrayList(100);
        
        //  Walk path to root.
        RemoteCabinet ptr = this;
        while (ptr.getParent() != null) {
            parts.add(0, ptr.getDisplayName());
            ptr = (RemoteCabinet)ptr.getParent();
        }
        
        //  Add intermediate path to file name.
        for (int i=0; i < parts.size(); i++) {
            fn.append("/" + parts.get(i));
        }
        
        //  Add the final file name.
       // fn.append("/" + getDisplayName()); //:REMOVED not needed
        
        return fn.toString();
    }
    
    /**
     *  Rename the file corresponding to this cabinet and update it's display
     *  name to the new name.
     *
     *  @author Mark Norton
     */
    public void rename(String newName) throws osid.filing.FilingException {
        //  Check the name of the directory on the remote file system.
        try {
            FTPClient client = rc.getClient();
            if (getParent() == null)
                client.rename(getFullName(), rc.getRootBase() + "/" + newName);
            else
                client.rename(getFullName(), ((RemoteCabinet)getParent()).getFullName() + "/" + newName);
        } catch (java.io.IOException ex) {
            throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
        }
        
        //  Change the name of the Cabinet.
        updateDisplayName(newName);
    }
    public String getUrl() {
        String url = "ftp://"+rc.getUserName()+":"+rc.getPassword()+"@"+ rc.getServerName() + this.getFullName();
        //System.out.println("CABINET URL:"+ url);
        return url;    }
}
