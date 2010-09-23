package org.okip.service.filing.impl.rfs;

import org.okip.service.filing.api.*;

public class RfsEntryCache
    implements java.io.Serializable
{
    static final byte IS_CABINET = 1;
    static final byte IS_BYTESTORE = 2;
    static final byte CAN_READ = 4;
    static final byte CAN_WRITE = 8;
    
    private static final byte TESTING_EXIST = 64;

    /*
     * Here are the non-transients that are sent over the wire
     * whenever any information is requested.
     */
    long length;
    long lastModified;
    byte bits;
    String name;

    transient long lastRefresh = 0;
    transient RfsEntry rfsEntry;
    transient int maxCacheAge = 0;
    transient boolean isStale = false;
    
    protected RfsEntryCache(RfsEntry rfsEntry)
        throws FilingException
    {
        setEntry(rfsEntry);
    }
    
    protected RfsEntryCache(java.io.File file)
    {
        initFromFile(file);
    }

    protected void setEntry(RfsEntry entry)
        throws FilingException
    {
        this.rfsEntry = entry;
        this.maxCacheAge = rfsEntry.factory.getMaxCacheAge();
        this.name = rfsEntry.getName();
        if (entry.isByteStore())
            this.bits |= IS_BYTESTORE;
        else if (entry.isCabinet())
            this.bits |= IS_CABINET;
    }

    protected void setLastRefresh(long time)
    {
        this.lastRefresh = time;
    }

    boolean isByteStore()
    {
        return (this.bits & IS_BYTESTORE) != 0;
    }
    boolean isCabinet()
    {
        return (this.bits & IS_CABINET) != 0;
    }
    
    /*
     * Data Acccess methods
     */
    String getName()
    {
        return this.name;
    }
    long length()
        throws FilingException
    {
        ensureCache();
        return this.length;
    }
    long getLastModifiedTime()
        throws FilingException
    {
        ensureCache();
        return this.lastModified;
    }
    boolean canRead()
        throws FilingException
    {
        ensureCache();
        return (this.bits & CAN_READ) != 0;
    }
    boolean canWrite()
        throws FilingException
    {
        ensureCache();
        return (this.bits & CAN_WRITE) != 0;
    }
    boolean exists()
        throws FilingException
    {
        // don't use ensureCache here, because we don't
        // want to throw an exception if it doesn't exist --
        // we simply want to report that fact.
        if (isCurrent())
            return !this.isStale;
        else {
            bits |= TESTING_EXIST;
            refreshCache();
            bits &= ~TESTING_EXIST;
        }
        return !this.isStale;
    }

    /*
     * Cache-function methdos
     */
    
    private final boolean isCurrent()
    {
        return (System.currentTimeMillis() - this.lastRefresh) < maxCacheAge;
    }

    protected void invalidate()
    {
        this.lastRefresh = 0;
    }

    private void empty()
    {
        this.length = 0;
        this.lastModified = 0;
        this.bits = 0;
    }

    protected void markStale(long t)
    {
        setLastRefresh(t == 0 ? System.currentTimeMillis() : t);
        markStale();
    }
    protected void markStale()
    {
        empty();
        this.isStale = true;
    }

    void ensureCache()
        throws FilingException
    {
        if (isStale)
            throw new FilingException("'" + name + "' - stale RFS entry (no longer exists): " + rfsEntry.getPath());
        if (!isCurrent())
            refreshCache();
    }
    protected boolean isReadableSet()
    {
        return (this.bits & CAN_READ) != 0;
    }

    protected void copyUpdate(RfsEntryCache rc)
    {
        this.lastRefresh = System.currentTimeMillis();
        this.lastModified = rc.lastModified;
        this.length = rc.length;
        this.bits = rc.bits;
    }
    
    protected void copy(RfsEntryCache rc)
    {
        this.lastRefresh = rc.lastRefresh;
        this.lastModified = rc.lastModified;
        this.length = rc.length;
        this.bits = rc.bits;
    }

    protected void refreshCache()
        throws FilingException
    {
        if (!rfsEntry.hasClient)
            throw new FilingException("attempted update of RfsEntryCache without a client");
        
        RfsEntryCache c;
        try {
            c = (RfsEntryCache) rfsEntry.factory.invoke
                (this, "refreshCacheRemotely", String.class, rfsEntry.idStr);
            if (c == null) {
                this.markStale();
            } else {
                this.copyUpdate(c);
                this.isStale = false;
            }
        } catch (FilingException e) {
            this.markStale();
            throw e;
        }
    }
    
    public RfsEntryCache refreshCacheRemotely(String path)
        throws FilingException
    {
        java.io.File file;
        try {
            file = RfsEntry.getEntryFile(path, isCabinet());
        } catch (NotFoundException e) {
            if ((bits & TESTING_EXIST) != 0)
                return null;
            else
                throw e;
        }
        this.bits = 0;
        initFromFile(file);
        return this;
    }

    private void initFromFile(java.io.File file)
    {
        this.length = file.length();
        this.lastModified = file.lastModified();
        this.name = file.getName();

        try {
            if (file.canRead()) this.bits |= CAN_READ;
        } catch (java.security.AccessControlException e) {}
        try {
            if (file.canWrite()) this.bits |= CAN_WRITE;
        } catch (java.security.AccessControlException e) {}
        try {
            if (file.isDirectory()) this.bits |= IS_CABINET;
        } catch (java.security.AccessControlException e) {}
        try {
            if (file.isFile()) this.bits |= IS_BYTESTORE;
        } catch (java.security.AccessControlException e) {}
    }

    public String toString()
    {
        String s = "Rec" + Integer.toHexString(hashCode()) + "["+name;
        if (lastRefresh > 0)
            s += " age=" + (System.currentTimeMillis() - lastRefresh);
        if (isStale)
            s += " STALE";
        s += "]";
        return s;
    }
}
