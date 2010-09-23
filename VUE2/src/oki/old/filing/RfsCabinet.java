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
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsCabinet.java,v $
 */

/**
 * Remote file system Cabinet implementation.
 *
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Name: not supported by cvs2svn $ / $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

public class RfsCabinet extends RfsEntry
    implements org.okip.service.filing.api.Cabinet
               ,org.okip.service.filing.api.Refreshable
{
    private transient java.util.Map entries;
    private transient long lastRefresh = 0;
    
    /* There was a policy we had in effect in the original LFS
     * implementation, where CabinetEntry's contained within this
     * cabinet that were not readable were treated as if they didn't
     * exist, and the user would never see these entries.  Not sure if
     * we want to continue it, but it's made optional here.  */
    private boolean ignoreUnreadables = true;
    private boolean caching = false;
    
    protected RfsCabinet(RfsFactory factory, String idStr)
        throws FilingException
    {
        initEntry(factory, idStr, false, null, null);
        initCabinet();
    }
    protected RfsCabinet(RfsFactory factory, String idStr, boolean create)
        throws FilingException
    {
        initEntry(factory, idStr, create, null, null);
        initCabinet();
    }
    protected RfsCabinet(RfsCabinet parent, String idStr, RfsEntryCache rc)
        throws FilingException
    {
        initEntry(parent.factory, idStr, false, rc, parent);
        initCabinet();
    }

    private void initCabinet()
    {
        if (factory != null) {
            // factory will be null remotely
            caching = factory.getMaxCacheAge() > 0;
            ignoreUnreadables = factory.getClassBoolean("RFS_IGNORE_UNREADABLE_FILES", true);
        }
    }

    
  /**
   * Method getProperties
   *
   * Return properties of this cabinet.  The properties map is
   * specified using keys of org.okip.service.shared.api.Type, and values of
   * e.g. Boolean, Long, or Double.  The application prepares a map of
   * desired qualities, e.g.<br>
   * key= new Type("Filing", "MIT", "supportsQuota"),
   *    value= new Boolean(true)<br>
   * key= new Type("Filing", "MIT", "supportsReplication"),
   *    value= new Boolean(true)<br>
   * key= new Type("Filing", "MIT", "minimumReplications"),
   * value= new Integer(2)
   *
   * @return java.util.Map of properties of this Cabinet and implementation
   *
   */
    public java.util.Map getProperties() throws FilingException {
        return RfsFactory.getStaticProperties();
    }

    /**
     * Create new anonymous ByteStore in this cabinet, to be deleted
     * automatically on exit
     *
     * @return  The ByteStore created
     *
     * @throws FilingException - if an IO error occurs or
     * if Factory Owner does not have permission to create a ByteStore.
     */
    public ByteStore createTempByteStore()
        throws FilingException
    {
        throw new UnsupportedFilingOperationException("createTempByteStore not implemented");
    }


  /**
   * Create new ByteStore and add it to this Cabinet under the given
   * name.
   *
   * The name must not include this Cabinet's separationCharacter.
   *
   * @param   name  The name to be used
   *
   * @return  The ByteStore created
   *
   * @throws FilingException - if name is already in use by a CabinetEntry or
   * if an IO error occurs or
   * if Factory Owner does not have permission to create a ByteStore.
   */
    public ByteStore createByteStore(String name)
        throws FilingException
    {
        String newId = this.idStr + factory.getSeparatorChar() + name;
        RfsByteStore bs = new RfsByteStore(factory, newId, true);
        addEntry(bs);
        return bs;
    }
    
    /**
     * Create new Cabinet and add it to this Cabinet under the given
     * name.
     *
     * The name must not include this Cabinet's separationCharacter.
     *
     * @param   name  The name to be used
     *
     * @return  The Cabinet created
     *
     * @throws FilingException - if name is already in use by a CabinetEntry or
     * if name includes the separationCharacter or
     * if an IO error occurs pr
     * if Factory Owner does not have permission to create a Cabinet.
     */
    public Cabinet createCabinet(String name)
        throws FilingException
    {
        String newId = this.idStr + factory.getSeparatorChar() + name;
        RfsCabinet cab = new RfsCabinet(factory, newId, true);
        addEntry(cab);
        return cab;
    }

  /**
   * Method createByteStore
   *
   * create new anonymous ByteStore in this Cabinet by copying  contents,
   * mimeType, and owner of another ByteStore.
   *
   * @param oldByteStore
   *
   * @return ByteStore
   *
   * @throws FilingException - if name of the oldByteStore is
   * already in use by a CabinetEntry in this Cabinet or
   * if an IO error occurs or
   * if Factory Owner does not have permission to create a ByteStore
   */
    public ByteStore createByteStore(ByteStore oldByteStore)
        throws FilingException
    {
        return createByteStore(oldByteStore.getName(), oldByteStore);
    }

  /**
   * Create new named ByteStore in this Cabinet by copying contents,
   * mimeType, and owner of another ByteStore.
   *
   * @param name
   * @param oldByteStore
   *
   * @return ByteStore
   *
   * @throws NameCollisionException - if name is already in use by a
   * CabinetEntry.
   * @throws FilingException - if an IO error occurs or
   * if Factory Owner does not have permission to create a ByteStore.
   */
    public ByteStore createByteStore(String name, ByteStore oldByteStore)
        throws FilingException
    {
        ByteStore newByteStore = this.createByteStore(name);
        newByteStore.setMimeType(oldByteStore.getMimeType());
        try {
            newByteStore.setOwner(oldByteStore.getOwner());
        } catch (UnsupportedFilingOperationException e) {}
        copyByteStore(oldByteStore, newByteStore);
        return newByteStore;
    }

    public static void copyByteStore(ByteStore src, ByteStore dest)
        throws FilingException
    {
        RfsInputStream input = (RfsInputStream) src.getOkiInputStream();
        RfsOutputStream output = (RfsOutputStream) dest.getOkiOutputStream();
        
        try {
            DataBlock dataBlock;
            while ((dataBlock = input.readMax()) != null)
                output.writeThrough(dataBlock);
        } finally {
            output.close();
            input.close();
        }
    }
    
    public static void copyByteStoreOld(ByteStore src, ByteStore dest)
        throws FilingException
    {
        OkiInputStream input = src.getOkiInputStream();
        OkiOutputStream output = dest.getOkiOutputStream();
        try {
            int len = 0;
            byte[] bytearr = new byte[8192];
            while ((len = input.read(bytearr)) != -1)
                output.write(bytearr, 0, len);
        } finally {
            output.close();
            input.close();
        }
    }
    

  /**
   * Add CabinetEntry, must be from same CabinetFactory.
   *
   *
   * @param entry
   * @param name
   *
   * @throws FilingException - if name is already in use by a CabinetEntry or
   * if name includes the separationCharacter or
   * if an IO error occurs or
   * if Factory Owner does not have permission to add a Cabinet or
   * if the add/remove semantics are not supportable in the implmentation.
   */
    public void add(CabinetEntry entry, String name)
        throws FilingException
    {
        throw new UnsupportedFilingOperationException("add/remove cabinetEntry not implemented");
    }

    /**
     * Remove CabinetEntry.  Does not destroy CabinetEntry.
     *
     * @param entry
     *
     * @throws FilingException - if an IO error occurs or
     * if Factory Owner does not have permission to remove a Cabinet or
     * if the add/remove semantics are not supportable in the implmentation.
     */
    public void remove(CabinetEntry entry)
        throws FilingException
    {
        throw new UnsupportedFilingOperationException("add/remove cabinetEntry not implemented");
    }


    /**
     * Get CabinetEntry from Cabinet by ID.
     *
     * @param id
     * @return CabinetEntry which has given ID.
     * @throws FilingException - if Factory Owner
     * does not have read permission on the CabinetEntry with this ID or
     * if an IO error occurs accessing the CabinetEntry with this ID or
     * if there is no CabinetEntry with this ID in this Cabinet.
     */
    public CabinetEntry getCabinetEntry(ID id)
        throws FilingException
    {
        return getCabinetEntry(getBaseName(id.toString()));
    }


    /**
     * Get CabinetEntry by name.  Not all CabinetEntrys have names,
     * but if it has a name, it is unique within a Cabinet.
     *
     * @param name
     *
     * @return CabinetEntry which has given name
     *
     * @throws FilingException - if Factory Owner
     * does not have read permission on the CabinetEntry with this name or
     * if an IO error occurs accessing the CabinetEntry with this name or
     * if there is no CabinetEntry with this name in this Cabinet.
     */
    public CabinetEntry getCabinetEntry(String name)
        throws FilingException
    {
        if (this.entries != null) {
            // check to see if we've already got this entry in our cache
            CabinetEntry ce = (CabinetEntry) this.entries.get(name);
            if (ce != null)
                return ce;
        }
        /*
         * This is a bit different than other methods that ultimately
         * create a cabinet entry in that we don't know type TYPE
         * (bytestore/cabient) until we go over the wire and find
         * out -- so that has to happen before we can do anything.
         * So this is the only place we create RfsEntry's on
         * the remote host and return them back here, where they
         * must be localized to this runtime before we can use them.
         */
        if (hasClient) {
            RfsEntry ce = (RfsEntry) factory.invoke(this, "getCabinetEntry", String.class, name);
            runtimeLocalize(ce);
            addEntry(ce);
            return ce;
        } else {
            return getCabinetEntryLocally(this.idStr + java.io.File.separator + name);
        }
    }
    
    /*
     * Make sure a remotely constructed cabinet entry is based on our cabinetFactory,
     * so that in particular it will pick up our client if we have one.
     */
    private void runtimeLocalize(CabinetEntry ce)
        throws FilingException
    {
        RfsEntry rfsEntry = (RfsEntry) ce;
        rfsEntry.factory = this.factory;
        rfsEntry.parent = this;
        if (factory != null && factory.getClient() != null)
            this.hasClient = true;
        if (caching) {
            if (rfsEntry.cache != null) {
                rfsEntry.cache.setLastRefresh(System.currentTimeMillis());
                rfsEntry.cache.setEntry(rfsEntry);
            } else
                new Throwable("localized entry came over w/out cache").printStackTrace();
        } else
            rfsEntry.cache = null;
    }
    


    private CabinetEntry getCabinetEntryLocally(String idStr)
        throws FilingException
    {
        getLocalFile();
        java.io.File testFile = new java.io.File(idStr);
        if (!testFile.exists())
            throw new NotFoundException("CabinetEntry '" + idStr  + "' not found");
        RfsEntry entry;
        if (testFile.isDirectory())
            entry = new RfsCabinet(factory, idStr);
        else if (testFile.isFile())
            entry = new RfsByteStore(factory, idStr);
        else
            throw new FilingException("CabinetEntry '" + idStr + "' is neither directory or regular file");
        if (caching)
            entry.cache = new RfsEntryCache(testFile);
        return entry;
    }

    /**
     * Get an immutable Iterator over all CabinetEntries in this Cabinet.
     *
     * The iterator will throw an <code>UnsupportedOperationException</code>
     * if remove() is called.
     *
     * @return java.util.Iterator
     *
     */
    public java.util.Iterator entries()
        throws FilingException
    {
        if (hasClient)
            return entriesRemotely();
        else
            return entriesLocally();
    }

    public void refresh()
        throws FilingException
    {
        if (hasClient)
            fetchRemoteEntries();
    }

    public java.util.Iterator entriesLocally()
        throws FilingException
    {
        getLocalFile();

        if (!this.canReadLocally())
            throw new FilingPermissionDeniedException("cannot read contents of " + this.idStr);

        java.io.File[] list = this.file.listFiles();
        if (list == null)
            return emptyIterator;
        
        java.util.List entriesList = new java.util.ArrayList();
        for (int i = 0; i < list.length; i++) {
            if (ignoreUnreadables && !list[i].canRead())
                continue;
            String path;
            try {
                path = list[i].getCanonicalPath();
            } catch (java.io.IOException e) {
                path = list[i].getPath();
            }
            if (list[i].isDirectory())
                entriesList.add(new RfsCabinet(this, path, null));
            else if (list[i].isFile())
                entriesList.add(new RfsByteStore(this, path, null));
            //else
                //System.err.println("Unknown type: " + list[i]);
                // Ignore special files (pipes, sockets, devices, etc)
        }
        return entriesList.iterator();
    }

    /**
     * A remote-only method.
     */
    public java.util.Iterator entriesRemotely()
        throws FilingException
    {
        if (this.entries == null)
            fetchRemoteEntries();
        else {
            // If our list of entries is more than RFS_MAX_CACHE_AGE old,
            // reload it.
            long dataAge;
            if (lastRefresh > 0)
                dataAge = System.currentTimeMillis() - lastRefresh;
            else
                dataAge = -1;
            
            if (dataAge < 0 || dataAge > factory.getMaxCacheAge())
                fetchRemoteEntries();
        }

        if (this.entries == null)
            return emptyIterator;
        else {
            /*
             * Return an immutable iterator over our
             * interal list of entries.
             */
	    return new java.util.Iterator() {
                java.util.Iterator i = entries.values().iterator();

                public boolean hasNext() {return i.hasNext();}
		public Object next() 	 {return i.next();}
		public void remove() {
		    throw new UnsupportedOperationException();
                }
	    };
        }
    }

    protected void notifyDelete(RfsEntry entry)
        throws FilingException
    {
        if (entries != null)
            entries.remove(entry.getName());
    }
    
    protected void notifyRename(RfsEntry entry, String oldName)
        throws FilingException
    {
        if (entries != null) {
            /*
             * Our purpose here is to find an entry in our hash table
             * that's been renamed, take it out and re-hash it back
             * in under it's new name.
             */
            RfsEntry oldEntry = (RfsEntry) entries.remove(oldName);
            String newName = entry.getName();
            if (oldEntry != null) {
                if (oldEntry != entry)
                    throw new FilingException("notifyRename inconsistency: " + oldEntry + " != " + entry);
                /*
                 * Now put it back but under it's newly changed name,
                 * unless there's something under that name already.
                 */
                RfsEntry existingEntry = (RfsEntry) entries.get(entry.getName());                
                if (existingEntry != null) {
                    /*
                     * If there happned to be an entry that already had this
                     * name, we're already done, except that if there's a cache,
                     * update the existing entry with the new cache data.
                     */
                    if (existingEntry.cache != null) {
                        existingEntry.cache.copy(entry.cache);
                        entry.cache = existingEntry.cache;
                    }
                } else {
                    entries.put(entry.getName(), entry);
                }
            }
        }
    }

    private void addEntry(CabinetEntry ce)
        throws FilingException
    {
        RfsEntry entry = (RfsEntry) ce;
        if (entries != null) {
            RfsEntry existingEntry = (RfsEntry) entries.get(entry.getName());
            if (existingEntry != null && existingEntry.cache != null) {
                //System.err.println("making orphan of: " + existingEntry);
                existingEntry.cache.markStale();
                existingEntry.cache = entry.cache;
            }
            entries.put(entry.getName(), entry);
        }
    }
    
    /*
     * A remote-only method.
     */
    private void fetchRemoteEntries()
        throws FilingException
    {
        if (!hasClient)
            throw new FilingException("getRemoteEntries requires a remote client");
        
        java.util.List entryData;
        try {
            /*
             * Remotely grab a block of data with info on all of the CabinetEntries
             * in this cabinet at once.
             */
            entryData = ((java.util.List) factory.invoke(this, "getAllEntryData"));
            
        } catch (FilingPermissionDeniedException e) {
            this.entries = null;
            return;
        }
        if (entryData == null)
            throw new FilingException("No remote response fetching cabinet " + getPath());

        long now = System.currentTimeMillis();

        /*
         * The first two elements in the list are a meta-data
         * update for this cabinet itself.  The children
         * follow.
         */

        if (this.cache != null)
            this.cache.copyUpdate((RfsEntryCache) entryData.get(1));
        if (this.id == null)
            this.id = (RfsID) entryData.get(0);
            
        /*
         * Now update our local cache with the new information
         * (for entries we haven't seen before, create a new cache entry)
         */

        java.util.HashMap curEntries = (java.util.HashMap) this.entries;//.clone();
        if (curEntries == null)
            this.entries = new java.util.HashMap();
        else
            this.entries = new java.util.HashMap((int)(curEntries.size() * 1.4 + 0.5), 0.75f);
        
        /*
         * Now iterate through the block of returned meta-data
         * (a list of RfsEntryCache's for the entire directory)
         * and update anything we already have in cache and
         * create new cached cabient entries for anything we don't
         * have yet.
         */
        String basePath = this.idStr + factory.getSeparatorChar();
        java.util.Iterator iter = entryData.iterator();
        iter.next(); // skip over first entry which is the ID
        iter.next(); // skip over second entry which an entry cache for this cabinet
        while (iter.hasNext()) {
            RfsEntryCache rfsData = (RfsEntryCache) iter.next();
            RfsEntry existingEntry = null;
            if (curEntries != null)
                existingEntry = (RfsEntry) curEntries.remove(rfsData.name);
            if (existingEntry != null) {
                // update our existing entry so anyone out there with a reference
                // to it get's to make use of the updated information.
                this.entries.put(rfsData.name, existingEntry);
                if (existingEntry.cache != null) {
                    existingEntry.cache.copy(rfsData);
                    existingEntry.cache.setLastRefresh(now);
                }
            } else {
                if (ignoreUnreadables && !rfsData.isReadableSet())
                    continue;
                // construct new entry
                rfsData.setLastRefresh(now);
                String path = basePath + rfsData.name;
                if (rfsData.isByteStore())
                    this.entries.put(rfsData.name, new RfsByteStore(this, path, rfsData));
                else if (rfsData.isCabinet())
                    this.entries.put(rfsData.name, new RfsCabinet(this, path, rfsData));
                //else
                //  System.err.println("Unknown entry type: " + rfsData);
                // Ignore special files (pipes, sockets, devices, etc)
            }
        }
        if (curEntries != null) {
            // any remaining entries are deleted files: render them stale
            iter = curEntries.values().iterator();
            while (iter.hasNext()) {
                RfsEntry rfsEntry = (RfsEntry) iter.next();
                if (rfsEntry.cache != null)
                    rfsEntry.cache.markStale(now);
            }
        }
        this.lastRefresh = System.currentTimeMillis();
    }

    /*
     * Return a list of all the elements in the cabinet.
     * The first element is an update of the cabinet itself.
     */
    public java.util.List getAllEntryData()
        throws FilingException
    {
        getLocalFile();
        if (!this.file.canRead())
            throw new FilingPermissionDeniedException("cannot read contents of " + this.idStr);

        java.io.File[] list = this.file.listFiles();
        int size = 2;
        if (list != null)
            size += list.length;
        java.util.List entriesList = new java.util.ArrayList(size);
        entriesList.add(getID());
        entriesList.add(new RfsEntryCache(getLocalFile()));
        for (int i = 0; i < list.length; i++)
            entriesList.add(new RfsEntryCache(list[i]));
        return entriesList;
    }
    
    private static class EmptyIterator implements java.util.Iterator
    {
        public final boolean hasNext() { return false; }
        public final Object next() { return null; }
        public final void remove() {}
    }
    private static final EmptyIterator emptyIterator = new EmptyIterator();

  /**
   * Return the root Cabinet of this cabinet.
   *
   * @return root Cabinet
   *
   * @throws FilingException - if Factory Owner
   * does not have read permission on the root Cabinet or
   * if an IO error occurs.
   */
    public Cabinet getRootCabinet()
        throws FilingException
    {
        try {
            return new RfsCabinet(factory, ""+factory.getSeparatorChar() );
        } catch (FilingException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

  /**
   * Return true if this Cabinet is the root Cabinet.
   *
   *
   * @return true if and only if this Cabinet is the root Cabinet.
   *
   */
    public boolean isRootCabinet()
        throws FilingException
    {
        if (hasClient)
            return ((Boolean) factory.invoke(this, "isRootCabinet")).booleanValue();
        else
            return isRootCabinetLocally();
    }

    private boolean isRootCabinetLocally()
        throws FilingException
    {
        try {
            return getLocalFile().getParentFile() == null;
        } catch (Exception e) {
            return false;
        }
    }

  /**
   * Set quota for Agent in this Cabinet.
   *
   * @param agent
   * @param quotaBytes
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to set quotas or
   * if an IO error occurs or
   * if quotas are not implemented.
   */
  public void setQuota(org.okip.service.shared.api.Agent agent,
                       long quotaBytes)
  throws FilingException {
    throw new UnsupportedFilingOperationException("Quotas not implemented");
  }

  /**
   * Get quota for agent in this Cabinet.
   *
   * @param agent
   *
   * @return long
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to remove a Cabinet or
   * if quotas are not implemented.
   */
  public long getQuota(org.okip.service.shared.api.Agent agent)
  throws FilingException {
    throw new UnsupportedFilingOperationException("Quotas not implemented");
  }

  /**
   * Get quota used by the agent in this Cabinet.
   *
   * @param agent
   *
   * @return long
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to examine the quota of this Agent or
   * if an IO error occurs or
   * if quotas are not implemented.
   */
  public long getQuotaUsed(org.okip.service.shared.api.Agent agent)
  throws FilingException {
    throw new UnsupportedFilingOperationException("Quotas not implemented");
  }

  /**
   * Get space available in Cabinet, for bytes.
   *
   *
   * @return long, space available in Cabinet, in bytes.
   *
   * @throws FilingException - if an IO error occurs or
   * if space restrictions are not implemented.
   */
  public long getAvailableBytes()
  throws FilingException {
    throw new UnsupportedFilingOperationException("Quotas not implemented");
  }

  /**
   * Get number of bytes used in this Cabinet.
   *
   *
   * @return long - space used in Cabinet, in bytes.
   *
   * @throws FilingException - if an IO error occurs or
   * if space restrictions are not implemented.
   */
  public long getUsedBytes()
  throws FilingException {
    throw new UnsupportedFilingOperationException("Space Restrictions not implemented");
  }

    /**
     * Tests this entry for equality with the given object.
     * Returns <code>true</code> if and only if the argument is not
     * <code>null</code> and is equal to this RfsCabinet.
     *
     * This implentation tests to see if the creating
     * factories are the same object, and that getID().toString()
     * for both entries is equivalent.
     *
     * @param object
     *
     * @return  <code>true</code> if and only if the objects are operationally equivalent
     *          <code>false</code> otherwise, or if an error occurs
     */
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        try {
            if (object instanceof RfsCabinet) {
                RfsCabinet rc = (RfsCabinet) object;
                return this.factory == rc.factory
                    // todo: this should call getID().equals
                    && this.getID().toString().equals(rc.getID().toString());
                
            } else
                return false;
        } catch (Throwable t) {
            return false;
        }
    }


    /**
     * Compares this this Cabinet to another Cabinet.
     *
     * @param cabinet The <code>Cabinet</code> to be compared to this Cabinet
     *
     * @return 0 if they are equal, less than 0 or greater than 0 if
     * they differ, depending on the implementation-dependent quality
     * used for the comparison.
     *
     * @throws FilingException - if Factory Owner
     * does not have read permission on the Cabinet being compared to or
     * if an IO error occurs reading the or Cabinet being compared to
     *
     * @see java.lang.Comparable
     */
    public int compareTo(Cabinet cabinet)
        throws FilingException
    {
        try {
            return getID().toString().compareTo(cabinet.getID().toString());
        } catch (Throwable e) {}
        return -1000;
    }

    public String toString()
    {
        return "RfsCabinet[" + super.toString() + "]";
    }

    /**
     * Tests whether this is a Cabinet.
     * @return boolean - always returns true
     */
    public final boolean isCabinet() {
        return true;
    }

    /**
     * Tests whether this is a ByteStore.
     * @return boolean - always returns false
     */
    public final boolean isByteStore() {
        return false;
    }

    
}
