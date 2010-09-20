package org.okip.service.filing.impl.rfs;

import org.okip.service.filing.api.*;

/**
   Copyright (c) 2002 Massachusetts Institute of Technology

   This work, including software, documents, or other related items (the
   "Software"), is being provided by the copyright holder(s) subject to
   the terms of the MIT OKI&#153; API Implementation License. By
   obtaining, using and/or copying this Software, you agree that you have
   read, understand, and will comply with the following terms and
   conditions of the MIT OKI&#153; API Implementation License:

   Permission to use, copy, modify, and distribute this Software and its
   documentation, with or without modification, for any purpose and
   without fee or royalty is hereby granted, provided that you include
   the following on ALL copies of the Software or portions thereof,
   including modifications or derivatives, that you make:

    *  The full text of the MIT OKI&#153; API Implementation License in a
       location viewable to users of the redistributed or derivative
       work.

    *  Any pre-existing intellectual property disclaimers, notices, or
       terms and conditions. If none exist, a short notice similar to the
       following should be used within the body of any redistributed or
       derivative Software:
       "Copyright (c) 2002 Massachusetts Institute of Technology
       All Rights Reserved."

    *  Notice of any changes or modifications to the MIT OKI&#153;
       Software, including the date the changes were made. Any modified
       software must be distributed in such as manner as to avoid any
       confusion with the original MIT OKI&#153; Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
   IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
   CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
   TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
   SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

   The name and trademarks of copyright holder(s) and/or MIT may NOT be
   used in advertising or publicity pertaining to the Software without
   specific, written prior permission. Title to copyright in the Software
   and any associated documentation will at all times remain with the
   copyright holders.

   The export of software employing encryption technology may require a
   specific license from the United States Government. It is the
   responsibility of any person or organization contemplating export to
   obtain such a license before exporting this Software.
*/

/*
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsEntry.java,v $
 */

/**
 * Shared code for remote file system entries -- used by RfsCabinet & RfsByteStore.
 *
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Name: not supported by cvs2svn $ / $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

public abstract class RfsEntry
    implements org.okip.service.filing.api.CabinetEntry
{
    /*
     * You could think of the factory in this Rfs code
     * as also a type of filesystem object because it
     * holds some specfic information, such as file
     * separator, as well as gataeways access to the
     * actual data via the invoke calls.
     */
    protected RfsEntryCache             cache = null;
    protected String                    idStr = null;
    protected transient boolean        hasClient = false; // always false remotely
    protected transient RfsFactory      factory = null;
    protected transient java.io.File    file = null;
    protected transient RfsID           id = null;
    protected transient RfsCabinet      parent = null;
    protected transient String          name = null;

    public abstract boolean isCabinet();
    public abstract boolean isByteStore();
    
    protected void initEntry(RfsFactory factory, String idStr, boolean create, RfsEntryCache cache, RfsCabinet parent)
        throws FilingException
    {
        this.factory = factory;
        this.idStr = idStr;
        this.parent = parent;
        this.hasClient = (factory != null && factory.getClient() != null);
        if (cache != null) {
            this.cache = cache;
            this.cache.setEntry(this);
        } else if (hasClient && factory.getMaxCacheAge() > 0)
            this.cache = new RfsEntryCache(this);
        if (create)
            createFile();
    }

    protected java.io.File getLocalFile()
        throws FilingException
    {
        if (hasClient)
            throw new FilingException("attempted local access to a remote RfsEntry");
        
        if (this.file == null)
            return this.file = getEntryFile(this.idStr, isCabinet());
        else
            return this.file;
    }
    
    protected static java.io.File getEntryFile(String path, boolean isCabinet)
        throws FilingException
    {
        java.io.File file;
        try {
            // Yes, every single rmi call will re-instantiate a File object here.
            // Fortunately, it's actually a light-weight operation -- the file
            // isn't even opened.
            file = new java.io.File(path);
            /* Besides files that never existed in the first
             * place, a file that was created and then went
             * missing out from under us will come up missing
             * here.
             */
            if (!file.exists())
                throw new NotFoundException(path + ": doesn't exist");
                
            if (isCabinet) {
                if (!file.isDirectory()) {
                    String msg = " not found as directory: ";
                    if (file.isFile())
                        msg += "is file";
                    else
                        msg += "is of unhandled type";
                    throw new NameCollisionException(path + msg);
                }
            } else if (!file.isFile()) {
                String msg = " not found as file: ";
                if (file.isDirectory())
                    msg += "is directory";
                else
                    msg += "is of unhandled type";
                throw new NameCollisionException(path + msg);
            }
        } catch (FilingException e) {
            throw e;
        } catch (Exception e) {
            throw new FilingException(e);
        }
        return file;
    }


    protected void createFile()
        throws FilingException
    {
        if (hasClient) {
            RfsEntryCache rfsData = (RfsEntryCache) factory.invoke(this, "createFileRemotely");
            if (this.cache != null)
                this.cache.copyUpdate(rfsData);
        } else
            createFileLocally();
    }
    
    public RfsEntryCache createFileRemotely()
        throws FilingException
    {
        createFileLocally();
        return new RfsEntryCache(getLocalFile());
    }
    
    /*
     * Only to be called locally.
     */
    protected void createFileLocally()
        throws FilingException
    {
        try {
            this.file = new java.io.File(this.idStr);
            if (this.isByteStore()) {
                boolean alreadyExists = false;
                String exMsg = "file '" + this.idStr + "' could not be created";
                try {
                    alreadyExists = !this.file.createNewFile();
                } catch (java.io.IOException e) {
                    if (e.getMessage().equals("Permission denied"))
                        throw new FilingPermissionDeniedException(e, exMsg);
                    else
                        throw new FilingIOException(e, exMsg);
                }
                if (alreadyExists)
                    throw new NameCollisionException(exMsg + ": already exists");
            } else if (this.isCabinet()) {
                String exMsg = "directory '" + this.idStr + "' could not be created";
                if (file.exists())
                    throw new NameCollisionException(exMsg + ": already exists");
                try {
                    if (!this.file.mkdir())
                        throw new FilingIOException(exMsg);
                } catch (java.security.AccessControlException e) {
                    if (e.getMessage().indexOf("ermission") >= 0)
                        throw new FilingPermissionDeniedException(e, exMsg);
                    else
                        throw new FilingIOException(e, exMsg);
                }
            } else
                throw new FilingException("tried to create RfsEntry of unknown type");
        } catch (FilingException e) {
            throw e;
        } catch (Exception e) {
            throw new FilingIOException(e, "failed to create " + this.idStr);
        }
    }


    /**
     * @return ID
     */
    public ID getID()
        throws FilingException
    {
        if (this.id != null)
            return this.id;

        if (hasClient)
            return this.id = (RfsID) factory.invoke(this, "getID");
        else {
            String cPath;
            try {
                cPath = getLocalFile().getCanonicalPath();
            } catch (java.io.IOException e) {
                throw new FilingException(e, "getID: couldn't get canonical path");
            }
            return this.id = new RfsID(cPath);
        }
    }
    
    /**
     * Return the path, the sequence of names of CabinetEntries leading
     * to this CabinetEntry from its root Cabinet, separated by the
     * separatorCharactor.
     *
     * @return path
     *
     */
    public String getPath()
    {
        return this.idStr;
    }

    /**
     * Check whether the entry with this ID exists
     *
     */
    public boolean exists()
        throws FilingException
    {
        try {
            if (hasClient) {
                if (cache != null)
                    return cache.exists();
                else
                    return ((Boolean) factory.invoke(this, "exists")).booleanValue();
            } else
                return existsLocally();
        } catch (NotFoundException e) {
            return false;
        }
    }

    public boolean existsLocally()
    {
        try {
            java.io.File f = new java.io.File(this.idStr);
            return f.exists();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * rename this entry
     *
     * @param newName the new name it should take on
     */
    public boolean rename(String newName)
        throws FilingException
    {
        if (hasClient) {
            if (((Boolean) factory.invoke(this, "rename", String.class, newName)).booleanValue()) {
                // rename succeded remotely -- now we need to patch up our idStr
                this.idStr = getParentPath(this.idStr) + factory.getSeparatorChar() + newName;
                this.id = null; // make sure this gets rebuilt
                String oldName = getName();
                this.name = newName;
                if (this.cache != null)
                    this.cache.setEntry(this);
                if (this.parent != null)
                    this.parent.notifyRename(this, oldName);
                return true;
            }
        } else
            return renameLocally(newName);
        return false;
    }
    
    
    /**
     * rename this entry
     * @return true if succeeds
     */
    private boolean renameLocally(String newName)
        throws FilingException
    {
        java.io.File f = new java.io.File(this.idStr);
        java.io.File newFile = new java.io.File(f.getParent(), newName);
        if (f.renameTo(newFile)) {
            try {
                this.idStr = newFile.getCanonicalPath();
            } catch (java.io.IOException e) {
                throw new FilingException(e, "failed to set new ID in rename");
            }
            this.file = newFile;
            this.name = newName;
            return true;
        }
        return false;
    }

    /**
     * Returns the time that this entry was last modified.
     *
     * @return  A <code>long</code> value representing the time the file was
     *          last modified, measured in milliseconds since the epoch
     *          (00:00:00 GMT, January 1, 1970)
     *
     * @throws FilingIOException - if an IO error occurs.
     */
    
    public long getLastModifiedTime()
        throws FilingException
    {
        if (hasClient) {
            if (cache != null)
                return cache.getLastModifiedTime();
            else
                return ((Long) factory.invoke(this, "getLastModifiedTime")).longValue();
        } else 
            return getLastModifiedTimeLocally();
    }

    protected long getLastModifiedTimeLocally()
        throws FilingException
    {
        long lastModified = getLocalFile().lastModified();

        if (lastModified == 0)
            throw new FilingIOException("invalid lastModified time");
        else
            return lastModified;
    }
    
    /**
     * Sets the last-modified time of this entry
     *
     * <p> All platforms support file-modification times to the nearest second,
     * but some provide more precision.  The argument will be truncated to fit
     * the supported precision.  If the operation succeeds and no intervening
     * operations on the file take place, then the next invocation of the
     * <code>{@link #getLastModifiedTime}</code> method will return the (possibly
     * truncated) <code>time</code> argument that was passed to this method.
     *
     * @param  time  The new last-modified time, measured in milliseconds since
     *               the epoch (00:00:00 GMT, January 1, 1970)
     *
     * @return <code>true</code> if and only if the operation succeeded;
     *          <code>false</code> otherwise
     *
     * @throws FilingException - If the argument is negative or
     * if Factory Owner does not have permission to set the lastModified time or
     * if an IO error occurs setting the lastModified time.
     */
    public boolean setLastModifiedTime(long time)
        throws FilingException
    {
        boolean tv = setLastModifiedTimeRemotely(new Long(time)).booleanValue();
        if (cache != null)
            cache.invalidate();
        return tv;
    }

    public Boolean setLastModifiedTimeRemotely(Long time)
        throws FilingException
    {
        if (hasClient)
            return (Boolean) factory.invoke(this, "setLastModifiedTimeRemotely", Long.class, time);
        else 
            return new Boolean(setLastModifiedTimeLocally(time.longValue()));
    }

    private boolean setLastModifiedTimeLocally(long time)
        throws FilingException
    {
        if (time < 0L)
            throw new FilingException("IllegalArgumentException - negative time");

        boolean success = getLocalFile().setLastModified(time);
        
        if (!success) {
            if (!this.file.canWrite())
                throw new FilingPermissionDeniedException("cannot set lastModifiedTime");
            else
                throw new FilingIOException("IO Error setting lastModifiedTime");
        } else {
            return true;
        }
    }

    /**
     * Returns the time that this entry was last accessed.
     *
     * Not all implementations will record last access times accurately,
     * due to caching and for performance.  The value returned will be
     * at least the last modified time, the actual time when a read was
     * performed may be later.
     *
     * @return  A <code>long</code> value representing the time the file was
     *          last accessed, measured in milliseconds since the epoch
     *          (00:00:00 GMT, January 1, 1970)
     *
     * @throws FilingIOException - if an IO error occurs.  */
    public long getLastAccessedTime()
        throws FilingException
    {
        return this.getLastModifiedTime();
    }

    
    /**
     * Returns the time that this entry was created.
     *
     * Not all implementations will record the time of creation
     * accurately.  The value returned will be at least the last
     * modified time, the actual creation time may be earlier.
     *
     * @return  A <code>long</code> value representing the time the file was
     *          created, measured in milliseconds since the epoch
     *          (00:00:00 GMT, January 1, 1970)
     *
     * @throws FilingException - if an IO error occurs.
     */
    public long getCreatedTime()
        throws FilingException
    {
        return this.getLastModifiedTime();
    }


    /**
     * Deletes this entry.
     *
     * The Owner of the Factory must have sufficient permissions.
     *
     * If this is a Cabinet, it must be empty to delete it.
     *
     * @return  <code>true</code> if and only if this entry is
     *          successfully deleted; <code>false</code> otherwise
     *
     * @throws FilingException - if Factory Owner
     * does not have permission to delete this entry or
     * if an IO error occurs deleting the lastModified time.
     */
    public boolean delete()
        throws FilingException
    {
        if (hasClient) {
            Boolean bv = (Boolean) factory.invoke(this, "delete");
            if (bv.booleanValue()) {
                if (cache != null)
                    cache.markStale(0);
                if (parent != null)
                    parent.notifyDelete(this);
                return true;
            } else
                return false;
        } else
            return deleteLocally();
    }

    private boolean deleteLocally()
        throws FilingException
    {
        getLocalFile();

        if (isCabinet() && this.file.list().length > 0)
            throw new FilingException("cannot delete cabinet: is not empty");
            
        boolean success = this.file.delete();

        if (!success) {
            if (!this.file.canWrite())
                throw new FilingPermissionDeniedException("read only");
            else
                throw new FilingIOException("IO Error on deleting");
        } else
            return true;
    }
    

    /**
     * Return owner of this entry.
     *
     * Owner may be used for quota and/or access control.
     *
     * @return owner
     *
     * @throws FilingException - if Factory Owner
     * does not have permission to learn who owns this entry or
     * if an IO error occurs or
     * if owners of entrys are not implemented.
     */
    public org.okip.service.shared.api.Agent getOwner()
        throws FilingException
    {
        return factory.getOwner();
    }

    
    /**
     * Set owner of this entry.
     *
     * Owner may be used for quota and/or access control.
     *
     * @param owner
     *
     * @throws FilingException - if Factory Owner
     * does not have permission to change ownership of this entry or
     * if an IO error occurs or
     * if owners of entrys are not implemented.
     */
    public void setOwner(org.okip.service.shared.api.Agent owner)
        throws FilingException
    {
        throw new UnsupportedFilingOperationException("Entry Ownership not implemented");
    }

    /**
     * Tests whether the Factory Owner may read this entry.
     *
     * @return <code>true</code> if and only if this entry can be
     *          read by the Factory Owner; <code>false</code> otherwise
     *
     */
    public boolean canRead()
        throws FilingException
    {
        if (hasClient) {
            if (cache != null)
                return cache.canRead();
            else
                return ((Boolean) factory.invoke(this, "canRead")).booleanValue();
        } else
            return canReadLocally();
    }

    protected boolean canReadLocally()
        throws FilingException
    {
        getLocalFile();
        boolean readable = false;
        try {
            readable = this.file.canRead();
        } catch (java.security.AccessControlException e) {
            readable = false;
        }
        return readable;
    }


    /**
     * Marks this entry so that only read operations are allowed.
     * After invoking this method this entry is guaranteed not to
     * change until it is either deleted or marked to allow write
     * access.
     *
     * Note that whether or not a read-only entry may be deleted
     * depends upon the underlying system of the implementation.
     *
     * @return <code>true</code> if and only if the operation succeeded;
     *          <code>false</code> otherwise
     *
     * @throws FilingException - if Factory Owner
     * does not have permission to set the entry readOnly or
     * if an IO error occurs.
     */
    public boolean setReadOnly()
        throws FilingException
    {
        if (hasClient) {
            if (cache != null)
                cache.invalidate();
            return ((Boolean) factory.invoke(this, "setReadOnly")).booleanValue();
        } else {
            return setReadOnlyLocally();
        }
    }

    private boolean setReadOnlyLocally()
        throws FilingException
    {
        getLocalFile();
        boolean success = this.file.setReadOnly();

        if (!success) {
            if (!this.file.canWrite())
                throw new FilingPermissionDeniedException("cannot set ReadOnly");
            else
                throw new FilingIOException("IO Error setting ReadOnly");
        } else
            return true;
    }
    
    
    /**
     * Tests whether the Factory Owner may modify this entry.
     *
     * @return <code>true</code> if and only if the Factory Owner is
     *          allowed to write to this entry
     *          <code>false</code> otherwise.
     *
     */
    public boolean canWrite()
        throws FilingException
    { 
        if (hasClient) {
            if (cache != null)
                return cache.canWrite();
            else
                return ((Boolean) factory.invoke(this, "canWrite")).booleanValue();
        } else
            return canWriteLocally();
    }

    protected boolean canWriteLocally()
        throws FilingException
    {
        getLocalFile();
        boolean writeable = false;
        try {
            writeable = this.file.canWrite();
        } catch (java.security.AccessControlException e) {
            writeable = false;
        }
        return writeable;
    }


    /**
     * Marks this entry so that write operations are allowed.
     *
     * @return <code>true</code> if and only if the operation succeeded;
     *          <code>false</code> otherwise
     *
     * @throws FilingException - if Factory Owner
     * does not have permission to set the entry writable or
     * if an IO error occurs or
     * if it is not possible to set writable in this implementation
     */
    public boolean setWritable()
        throws FilingException
    {
        throw new UnsupportedFilingOperationException("Cannot set an entry Writable in this implementation");
    }

    

  /**
   * Returns the Cabinet in which this is an entry, or null if it has
   * no parent (for example is the root cabinet).
   *
   *
   * @return Cabinet - the parent Cabinet of this entry, or null if it has
   * no parent (e.g. is the root cabinet)
   *
   * @throws FilingIOException - if an IO error occurs.
   * @throws FilingPermissionDeniedException - if Factory Owner
   * does not have read permission on the parent Cabinet.
   */
    public Cabinet getParent()
        throws FilingException
    {
        if (this.parent != null)
            return this.parent;
        
        if (hasClient) {
            try {
                return (Cabinet) factory.invoke(this, "getParent");
            } catch (Exception ex) {
                throw new FilingException(ex);
            }
        } else {
            return getParentLocally();
        }
    }

    private Cabinet getParentLocally()
        throws FilingException
    {
        getLocalFile();
        try {
            java.io.File parentFile = this.file.getParentFile();

            if (parentFile != null) {
                String parentString = parentFile.getCanonicalPath();
                return new RfsCabinet(factory, parentString);
            } else
                return null;
        } catch (Exception e) {
            throw new FilingException(e, "Error getting parent:" + e.getMessage());
        }
    }


    /**
     * Return the name of this CabinetEntry in its parent Cabinet.
     *
     * @return name
     */
    public String getName()
        throws FilingException
    {
        if (name != null)
            return name;
        if (cache != null)
            return name = cache.getName();
        if (file != null)
            return name = file.getName();
        return name = getBaseName(this.idStr);
    }

    protected String getNameInternal()
    {
        if (name != null)
            return name;
        if (cache != null)
            return cache.name;
        if (file != null)
            return file.getName();
        try {
            return getBaseName(this.idStr);
        } catch (Exception e) {
            return "<<"+this.idStr+">>";
        }
    }

    /**
     * Compare this RfsEtnry to another Object.  If the object is not
     * an RfsEntry, it throws a <code>ClassCastException</code>.
     *
     * @param o  The <code>Object</code> to be compared to this RfsEntry
     *
     * @return 0 if they are equal, less than 0 or greater than 0 if
     * they differ, depending on the implementation-dependent quality
     * used for the comparison.  If an error occurs, return less than 0.
     *
     * Note: "this class may have a natural ordering that is
     * inconsistent with equals."  Comparison is intended to be done by
     * name only -- thus it is possible to compare an
     * RfsByteStore to an RfsCabinet and sort them by name.
     * (Whereas they would never be considered equal by x.equals(y)).
     *
     * @see java.lang.Comparable
     */
    public int compareTo(Object o)
    {
        try {
            return getID().toString().compareTo(((RfsEntry)o).getID().toString());
        } catch (ClassCastException e) {
            throw e;
        } catch (Throwable e) {}
        return -1000;
    }

    
    /***********************************************************************
     *
     * Below are methods shared by RfsByteStore & RfsCabinet, but
     * not part of the CabinetEntry interface.
     *
     ***********************************************************************/
    
    protected String getBaseName(String path)
        throws FilingException
    {
        if (path.length() <= 1)
            return path;
        char separator;
        if (hasClient)
            separator = factory.getSeparatorChar();
        else
            separator = java.io.File.separatorChar;
        // strip all separator chars off end of string
        while (path.length() > 1 && path.charAt(path.length() - 1) == separator)
            path = path.substring(0, path.length() - 1);
        if (path.length() == 1)
            return path;
        // now make sure we're only taking the basename
        int i = path.lastIndexOf(separator);
        if (i >= 0)
            return path.substring(i + 1);
        else
            return path;
    }

    protected String getParentPath(String path)
        throws FilingException
    {
        char separator = factory.getSeparatorChar();
        // strip all separator chars off end of string
        while (path.charAt(path.length() - 1) == separator)
            path = path.substring(0, path.length() - 1);
        // now take the root parent path
        int i = path.lastIndexOf(separator);
        if (i > 0)
            return path.substring(0, i);
        else
            return "" + separator;
    }
    
    public String toString()
    {
        String s = getNameInternal() + " ";
            
        if (this.file != null) {
            try {
                s += " file='"+this.file.getCanonicalPath()+"'";
            } catch (Exception e) {
                s += " file=<"+this.file.getPath()+">";
            }
        } else
            s += "idStr='" + this.idStr + "'";

        if (this.id != null) {
            if (!this.id.toString().equals(idStr))
                s += " id='" + this.id + "'";
        }
        s += " "+factory;
        if (this.cache != null)
            s += " "+this.cache;
        return s;
    }
}
