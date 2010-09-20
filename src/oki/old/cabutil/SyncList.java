
// SyncList.java
// MIT OKI Filing Demo
// S.Fraize July 2002

// ?bug: if remove local entry, ghost created -- if them remove
// REMOTE entry, ghost is not removed.  However, removing
// something else, which triggers a rescan, fixes it, so
// we're just not triggering a rescan.  BTW, same applies
// in reverse direction.
// ?BUG 2002-08-20 15:31.50 Tuesday zippy
// if you remove a remote entry (ghost created), then remove local entry, remote
// ghost is not removed (the reverse procedure is working however:remove local, then remote)

import java.util.*;
import java.io.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.okip.service.filing.api.*;
import org.okip.service.filing.impl.*;
import org.okip.service.shared.api.Agent;
import org.okip.util.agents.RemoteAgent;

public class SyncList
    implements Runnable
{
    Logger log = new Logger(SyncList.class);
    
    public static final int STATUS_UNKNOWN = 0;        // unknown status
    public static final int STATUS_CURRENT = 1;        // everything is copacetic
    public static final int STATUS_NEW_LOCAL = 2;      // new local entry, no remote data yet
    public static final int STATUS_CHANGED_LOCAL = 3;  // local version more current than remote version
    public static final int STATUS_NEW_REMOTE = 4;     // new remote entry, no local data yet
    public static final int STATUS_CHANGED_REMOTE = 5; // remote version more current than local version
    public static final int STATUS_CONFLICT = 6;       // versions in conflict -- both changed since last sync

    public static final int SYNC_ALL = 0;
    public static final int SYNC_UPLOADS_ONLY = 1;
    public static final int SYNC_DOWNLOADS_ONLY = 2;


    int opposingStatus[] = {
        STATUS_UNKNOWN, STATUS_CURRENT,
        STATUS_NEW_REMOTE, STATUS_CHANGED_REMOTE,
        STATUS_NEW_LOCAL, STATUS_CHANGED_LOCAL,
        STATUS_CONFLICT
    };

    static final String PropsFileName = ".cabsync";
    static final String LockName = ".~cablock~";
    static final String IncomingFileExtension = "~incoming~";
    
    HashMap entries = new HashMap();
    Cabinet localCabinet;
    Cabinet remoteCabinet;
    SyncList remoteList;
    Properties props = new Properties();
    ByteStore propsBS;

    long bytesIncoming;
    long bytesOutgoing;

    boolean isLocalList;
    boolean importAll = true;
    boolean takeAction = true;

    int pollInterval = 0;
    boolean polling = false;
    boolean syncRunning = false;
    boolean refreshRunning = false;
    long lastRefresh = 0;
    boolean autoSync = false;
    
    /*
     * are ghosts considered material (visible) list entries?
     * Effects ONLY change event notifications.
     */
    boolean visibleGhosts = false;
    
    public SyncList(Cabinet localCab, Cabinet remoteCab)
    {
        this(localCab, remoteCab, null);
    }
    
    /*
     * create the remote mirror for a given list
     */
    private SyncList(SyncList local)
    {
        this.localCabinet = local.remoteCabinet;
        //this.remoteCabinet = local.localCabinet;
        this.remoteList = local;
        this.isLocalList = false;
        log.copyStates(local.log);
        try {
            log.setPrefix(localCabinet.getName());
        } catch (FilingException e) {
            log.errout(e);
            log.setPrefix(e.getMessage());
        }
        if (log.d) log.debug("new remote " + this);
    }
    public SyncList(Cabinet localCab, Cabinet remoteCab, Logger l)
    {
        this(localCab, remoteCab, l.action, l.v, l.d);
    }

    public SyncList(Cabinet localCab, Cabinet remoteCab, boolean ta, boolean v, boolean d)
    {
        setStates(ta, v, d);
        try {
            log.setPrefix(localCab.getName());
        } catch (FilingException e) {
            log.errout(e);
            log.setPrefix(e.getMessage());
        }
        if (log.d) log.debug("new SyncList local=" + localCab + ", remote=" + remoteCab);
        this.localCabinet = localCab;
        this.remoteCabinet = remoteCab;
        this.isLocalList = true;
        this.remoteList = new SyncList(this);

        refresh();
    }

    /**
     * Set a polling interval.  This is the minimum
     * number of seconds that will elapse between
     * automatic calls to refresh.
     *
     * @param seconds seconds between calls to refresh
     * a value of 0 seconds means stop polling
     */
    public void setPollInterval(int seconds)
    {
        this.pollInterval = seconds;
        polling = pollInterval > 0 ? true : false;
        if (log.v) log.verbose("poll interval set to " + seconds);
        if (polling)
            startPolling();
    }
    public int getPollInterval()
    {
        return this.pollInterval;
    }

    void startPolling()
    {
        new Thread(this, "SyncList refresh poll").start();
    }

    /*
     * For running the optional polling thread.
     */
    public void run()
    {
        if (log.d) log.debug("poll thread started");
        long sleepInterval = pollInterval * 1000;
        while (polling) {
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                polling = false;
                break;
            }
            if (!polling)
                break;
            sleepInterval = pollInterval * 1000;
            if (!syncRunning) {
                long elapsed = System.currentTimeMillis() - this.lastRefresh;
                if (elapsed >= sleepInterval)
                    doAutoRefresh();
                else
                    sleepInterval -= elapsed;
            }
        }
        if (log.d) log.debug("poll thread exited");
    }

    synchronized void doAutoRefresh()
    {
        if (syncRunning || refreshRunning)
            return;
        if (log.v) log.verbose("doAutoRefresh");

        this.refresh(false);
    }

    public void setAutoSync(boolean tv)
    {
        this.autoSync = tv;
    }
    public boolean isAutoSync()
    {
        return this.autoSync;
    }
    public boolean isSyncRunning()
    {
        return syncRunning;
    }


    public void setVisibleGhosts(boolean tv)
    {
        visibleGhosts = tv;
        // Declare that a structural change has taken place so
        // that our listeners can know to rescan all our data.
        notifyChangeListeners(new SyncListChangeEvent(this, 1, 1));
        remoteList.notifyChangeListeners(new SyncListChangeEvent(this, 1, 1));
    }

    public void setImportAll(boolean tv)
    {
        if (importAll == tv)
            return;
        importAll = tv;
        refresh();
        notifyChangeListeners(new SyncListChangeEvent(this, 1, 0));
        remoteList.notifyChangeListeners(new SyncListChangeEvent(this, 1, 0));
    }
    public boolean isImportAll()
    {
        return importAll;
    }

    private SyncEntry newGhostEntry(SyncEntry se)
    {
        SyncEntry ghost = new SyncEntry(se.getCabinetEntry());
        ghost.setIsGhost(true);
        ghost.setStatus(STATUS_NEW_REMOTE);
        return ghost;
    }

    public Collection values()
    {
        return entries.values();
    }
    public Iterator iterator()
    {
        return entries.values().iterator();
    }
    void put(String nameKey, SyncEntry se)
    {
        entries.put(nameKey, se);
    }
    SyncEntry get(String nameKey)
    {
        return (SyncEntry) entries.get(nameKey);
    }
    Object remove(String nameKey)
    {
        return entries.remove(nameKey);
    }
    public int size()
    {
        return entries.size();
    }
    public void setStates(boolean ta, boolean v, boolean d)
    {
        takeAction = ta;
        log.setVerbose(v);
        log.setDebug(d);
    }
    public void setStates(Logger l)
    {
        setStates(l.action, l.v, l.d);
    }

    private void setEntrySyncTime(SyncEntry se, String prop)
    {
        props.setProperty(se.getName()+".sync", prop);
    }
    private String getEntrySyncTime(SyncEntry se)
    {
        return props.getProperty(se.getName()+".sync");
    }
    private void setEntryDoSkip(SyncEntry se, boolean doSkip)
    {
        String key = se.getName()+".skip";
        if (doSkip)
            props.remove(key);
        else
            props.setProperty(key, "t");
    }
    
    private boolean getEntryDoSkip(SyncEntry se)
    {
        String v = props.getProperty(se.getName()+".skip");
        return v != null && v.equals("t");
    }

    private void readProperties()
    {
        if (!isLocalList)
            throw new RuntimeException("SyncList: remote list attempted properties access");
        
        this.propsBS = getPropsByteStore();
        if (propsBS == null)
            log.errout("coudn't find a properties store in: " + this);
        else {
            try {
                InputStream in = new JavaInputStreamAdapter(propsBS.getOkiInputStream());
                this.props.load(in);
                in.close();
                if (log.d) log.debug("loaded properties from BS " + propsBS);
            } catch (Exception e) {
                log.errout(e);
                log.errout("failed to read properties from BS " + propsBS);
            }
        }
    }
        
    public void saveProperties()
    {
if (false) return;
        // TODO: cull out dead entries? -- if there's no CabinetEntry
        // for a name, delete it.

        // todo: save all the sync request bits -- change this
        // over to actually traversing the sync list each time
        // instead of relying on setting the prop value at runtime
        // constantly?

        String header = " CabSync properties, entries=(" + size() + "). ";
        header += "\n#  Local cabinet is " + getLocalCabinetName();
        header += "\n# Remote cabinet is " + getRemoteCabinetName();
        try {
            OutputStream out = new JavaOutputStreamAdapter(propsBS.getOkiOutputStream());
            this.props.store(out, header);
            out.close();
        } catch (Exception e) {
            log.errout(e);
            log.errout("Couldn't save properties store " + propsBS);
        }
    }
        
    private ByteStore getPropsByteStore()
    {
        ByteStore bs = null;

        try {
            bs = (ByteStore) localCabinet.getCabinetEntry(PropsFileName);
        } catch (NotFoundException e) {
            try {
                bs = localCabinet.createByteStore(PropsFileName);
                if (log.d) log.debug("created properties BS " + bs);
            } catch (Exception ex) {
                log.errout("failed to create properties BS " + PropsFileName);
                log.errout(ex);
                bs = null;
            }
        } catch (Exception e) {
            log.errout(e, "couldn't get props file");
        }
        return bs;
    }

    public Cabinet getLocalCabinet()
    {
        return localCabinet;
    }
    public Cabinet getRemoteCabinet()
    {
        return remoteCabinet;
    }
    public String getLocalCabinetName()
    {
        if (localCabinet == null)
            return "<lc>";
        else
            return getCabinetName(localCabinet);
    }
    
    public String getRemoteCabinetName()
    {
        if (remoteCabinet == null)
            return "<rc>";
        else
            return getCabinetName(remoteCabinet);
    }

    static String getCabinetName(Cabinet cab)
    {
        String s = "";
        try {
            Agent owner = cab.getOwner();
            if (owner != null) {
                RemoteAgent ra = (RemoteAgent) owner.getProxy(RemoteAgent.class);
                if (ra != null)
                    s += ra.getHost() + ":" + ra.getPort() + "/";
            }
            s += cab.getID().toString();
        } catch (Exception e) {
            s += ceName(cab);
        }
        return s;
    }

    public SyncList getRemoteList()
    {
        return remoteList;
    }

    public void setSyncRequested(SyncEntry rse, boolean doRequest)
    {
        SyncEntry se = get(rse.getName());
        if (se == null) {
            log.errout("setSyncRequested: not in our list!: " + rse);
            return;
        }
        if (se.isSyncRequested() != doRequest) {
            se.doSetSyncRequested(doRequest);
            if (se.isGhost() && doRequest == false)
                // will end up removing the ghost
                refresh();
        }
    }


    /*
     * Make sure we're set up to sync to a given
     * entry in a remote list.  Create a ghost
     * in the remote list if we're not.
     * Only call this on a local list!
     */
    public synchronized SyncEntry addRemote(SyncEntry remoteEntry)
    {
        if (!isLocalList)
            throw new RuntimeException("addRemote on remote list");

        int changes = 0;
        int updates = 0;
        SyncEntry localMatch = this.get(remoteEntry.getName());
        if (localMatch == null) {
            // if sync-all isn't set, there might not already be a ghost.
            changes++;
            localMatch = newGhostEntry(remoteEntry);
            localMatch.doSetSyncRequested(true);
            this.put(localMatch.getName(), localMatch);
            if (log.d) log.debug("addRemote: new ghost " + localMatch);
        }
        else if (!localMatch.isSyncRequested()) {
            updates++;
            localMatch.doSetSyncRequested(true);
        }
        if (updates > 0 || changes > 0)
            notifyChangeListeners(new SyncListChangeEvent(localMatch, updates, changes));

        return localMatch;
    }

    public void refresh()
    {
        try {
            refreshRunning = true;
            doRefresh(false);
        } finally {
            refreshRunning = false;
        }
        // should this be checking isLocalList also?
    }
    
    public void refresh(boolean force)
    {
        /*
         * this intended to be called on either
         * local or remote list (e.g., there's a TableModel
         * somewhere monitoring them) but we only ever
         * want to refresh the local list.
         */
        try {
            refreshRunning = true;
            if (isLocalList)
                doRefresh(force);
            else
                getRemoteList().doRefresh(force);
        } finally {
            refreshRunning = false;
        }
    }

    /*
     * refresh both cabinet listings & compaired status
     * Do NOT call this for a remote list - this is
     * a local-list method only.
     */
    // note that the force arg does NOT at moment
    // refer to making use of the filing.api.Refreshable interface
    private synchronized void doRefresh(boolean force)
    {
        int updates = 0;
        int changes = 0;
        int tosync = 0;
        int remoteUpdates = 0;
        int remoteChanges = 0;
        
        // TODO: if there's a lock (sombodies syncing)
        // wait a while for it to clear or throw exception.
        // (we don't changes happening during rescan);

        if (log.d) log.debug("refresh (force=" + force + ") " + this);
        if (!isLocalList) {
            // okay, only place that might ever call this is
            // our hack when we get an exception in the data model...
            //todo:clean
            log.errout("REMOTE REFRESH " + this);
            return;
        }

        if (isLocalList)
            readProperties(); // just in case somebody else has been syncing here...
        
        /*
         * get a clear picture of the local cabinet
         */
        if (force)
            entries.clear();
        SyncListChangeEvent slev = rescan();
        updates += slev.updates;
        changes += slev.changes;

        /*
         * get a clear picture of the REMOTE cabinet, unless
         * we're already a remote synclist.
         */
        if (isLocalList) {
            slev = remoteList.rescan();
            remoteUpdates += slev.updates;
            remoteChanges += slev.changes;
        }
        
        /*
         * Go through local cabinet, finding matches in the remote cabinet,
         * and compare them.
         */
        
        bytesIncoming = 0;
        bytesOutgoing = 0;

        /*
         * If the cabinet is really locked, somebody is the in
         * the middle of updating it and we can't reliably
         * compute sync status.
         */

        boolean locked = false;

        if (findLock(localCabinet) != null) {
            /*
             * We found a lock: try a couple
             * more times just in case it's about
             * to clear.
             */
            locked = true;
            ByteStore lockBS = null;
            for (int trys = 0; trys < 3; trys++) {
                try {       
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                lockBS = findLock(localCabinet);
                if (lockBS == null)
                    break;
            }
            if (lockBS == null)
                locked = false;
            else {
                java.util.Properties lock = readLock(lockBS);
                //throw new LockException(readLock(lockBS), "cannot safely read locked cabinet");
                log.errout(new LockException(lock, "cannot be sure of status in a locked cabinet: lock="+lock));
            }
        }


        if (log.d) log.debug("refresh COMPAIRING " + this);
        Iterator i = this.iterator();
        while (i.hasNext())
        {
            SyncEntry localEntry = (SyncEntry) i.next();
            if (localEntry.isGhost()) {
                if (importAll || localEntry.isSyncRequested()) {
                    tosync++;
                    continue;
                }
                // we're not default to import all, and
                // this hasn't been specifically requested,
                // so delete this ghost...
                i.remove();
                if (log.d) log.debug("removed unwanted ghost " + localEntry);
                changes++;
                continue;
            }
            SyncEntry remoteMatch = (SyncEntry) remoteList.get(localEntry.getName());
            if (log.d) log.debug("CMPL: " + localEntry);
            if (log.d) log.debug("CMPR: " + remoteMatch);

            int status = STATUS_UNKNOWN;
            
//             if (localEntry.isGhost()) {
//                 // ghosts have STATUS_NEW_REMOTE by definition
//                 continue;
//             }
//            else

            // everyones status will remain unknown if we're locked
            if (!locked) {
                if (remoteMatch == null) {
                    status = STATUS_NEW_LOCAL;
                    if (importAll) {
                        // create the paired remote entry
                        remoteMatch = newGhostEntry(localEntry);
                        remoteList.put(localEntry.getName(), remoteMatch);
                        remoteChanges++;
                    } else
                        if (log.d) log.debug("no remote ghost for " + localEntry);
                }
                //            else if (remoteMatch.isGhost())
                //                continue;
                else {
                    status = computeStatus(localEntry, remoteMatch);
                }
            }

            if (status != STATUS_CURRENT && status != STATUS_UNKNOWN)
                tosync++;
            
            // todo: compute byte transfers -- do in computeStatus?
            if (localEntry.setStatus(status)) {
                updates++;
                if (log.d) log.debug("\t localStatus=" + localEntry.getStatusName());
            }
            if (remoteMatch != null && remoteMatch.setStatus(opposingStatus[status])) {
                updates++;
                if (log.d) log.debug("\tremoteStatus=" + remoteMatch.getStatusName());
            }
            
            // TODO: keep a running entry of # of bytes to upload / download
        }

        /*
         * Go through REMOTE file list, looking for
         * any new entries we don't have locally yet.
         * Create ghosts for any new entries.
         */

        if (importAll) {
            i = remoteList.iterator();
            while (i.hasNext()) {
                SyncEntry remoteEntry = (SyncEntry) i.next();
                
                if (remoteEntry.isGhost())
                    continue;
                
                SyncEntry localMatch = (SyncEntry) this.get(remoteEntry.getName());
                if (localMatch == null) {
                    // no match found, create new sync entry
                    localMatch = newGhostEntry(remoteEntry);
                    this.put(remoteEntry.getName(), localMatch);
                    changes++;
                    if (remoteEntry.setStatus(STATUS_NEW_LOCAL))
                        updates++;
                }
            }
        }

        if (updates > 0 || changes > 0 || remoteUpdates > 0 || remoteChanges > 0 || tosync > 0) {
            // if anything changes on either side, send a change event to
            // everyone just in case.
            if (log.d) log.debug(changes + " changes, " + updates + " updates, " + tosync + " tosync, notifying listeners");
            notifyChangeListeners(new SyncListChangeEvent(this, updates, changes, tosync));
            remoteList.notifyChangeListeners(new SyncListChangeEvent(this, remoteUpdates, remoteChanges, tosync));
        }

        this.lastRefresh = System.currentTimeMillis();
        //return changes + updates;
    }


    /**
     * Produce an accurate picture of the current cabinet.
     * @return the # of significant changes -- meaning
     * the addition or deletion of an item (as opposed to
     * just a status change).
     */
    private synchronized SyncListChangeEvent rescan()
    {
        SyncListChangeEvent slev = new SyncListChangeEvent(this, 0, 0);
        
        if (log.d) log.debug("rescan: " + localCabinet);

        if (localCabinet instanceof Refreshable) {
            // If this implemntation supports some kind of
            // cabinet meta-data caching, trigger a refresh now as we're about
            // to scan the entire directory.
            try {
                ((Refreshable)localCabinet).refresh();
            } catch (FilingException e) {
                log.errout(e);
                return slev;
            }
        }
        
        /*
         * first, detect any deletions (SyncEntry's who's
         * CabinetEntry no longer exists)
         */
        Iterator i = this.iterator();
        while (i.hasNext())
        {
            SyncEntry se = (SyncEntry) i.next();
            
            // don't bother with ghosts -- they don't reflect
            // a local CabinetEntry to check for (they point
            // to a remote entry)
            if (se.isGhost())
                continue;

            if (!se.exists()) {
                SyncEntry remoteMatch = remoteList.get(se.getName());
                
                if (se.isSyncRequested()) {
                    // an item was deleted out from under us, but
                    // it's sync bit was set, so we still want to honor
                    // that -- so we'll need to turn it into a ghost.
                    slev.updates++;
                    se.makeGhostOf(remoteMatch);
                } else {
                    slev.changes++;
                    i.remove();
                    if (log.d) log.debug("Removed " + se);
                    if (remoteMatch != null && remoteMatch.isGhost()) {
                        remoteList.remove(se.getName());
                        if (log.d) log.debug("cleared remote ghost " + remoteMatch);
                    }
                }
            }
            else
                if (log.d) log.debug(" exists " + se);
        }

        /*
         * now, try and get the local cabinet list
         */
        try {
            i = localCabinet.entries();
            //todo: if entire directory has been deleted,
            // we'll start getting NullPointerExceptions out
            // of LfsCabient...
        } catch (FilingException ex) {
            log.errout(ex);
            return slev;
        }

        /*
         * Search the cabinet for any new entries, and
         * create new SyncEntry's as appropriate.
         * First time this is called, all will be new.
         */
        java.text.DateFormat dateFormatter =
            new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        // This date format is literally taken from Date.toString(), which is
        // what's being used to generate the output.
        // todo: when move to java 1.4 Preferences, create a date format that
        // inclues milliseconds and use it for both input & output.
        
        while (i.hasNext()) {
            CabinetEntry ce = (CabinetEntry) i.next();
            String ceName;

            try {
                ceName = ce.getName();
            } catch (FilingException e) {
                log.errout(e);
                continue;
            }
            
            if (ceName.equals(PropsFileName) || ceName.endsWith(IncomingFileExtension))
                // ignore special files
                continue;
            // ignore windows temp files (~xxx) -- todo: make configurable option
            // ignore likely unix tmp files (xxx~)
            // ignore likely config files (.xxx)
            // on mac: ignore .DS_Store files
            char ic = ceName.charAt(0);
            if (ic == '~' || ic == '.' || ceName.endsWith("~"))
                continue;

            SyncEntry se = this.get(ceName);

            if (se != null) {
                /*
                 * We already have a sync entry for this cabinet entry
                 * Nothing to do in that case, unless something
                 * exceptional happened.
                 */
                if (se.isGhost()) {
                    // somehow a ghost was back-filled -- somebody
                    // else must be syncing out from under us.
                    // Convert the ghost to a real SyncEntry.
                    /*
                     * if ghosts always shown, this only an update
                     * otherwise if ghosts not shown, this a structural change.
                     */
                    if (visibleGhosts)
                        slev.updates++;
                    else
                        slev.changes++;
                                        // 
                    if (log.d) log.debug("GHOST FILL " + se);
                    se.setCabinetEntry(ce);
                    se.setIsGhost(false);
                    se.setStatus(STATUS_UNKNOWN);
                } else
                    if (log.d) log.debug("confirm " + se);
            } else {
                /*
                 * There is no SyncEntry for this CabinetEntry -- create one
                 */
                slev.changes++;
                se = new SyncEntry(ce);
                if (this.isLocalList) {
                    String dateStr = getEntrySyncTime(se);
                    if (dateStr != null)
                        //se.setLastSyncTime(Date.parse(dateStr));
                        try {
                            se.setLastSyncTime(dateFormatter.parse(dateStr).getTime());
                        } catch (java.text.ParseException e) {
                            log.errout(e, "bad date: [" + dateStr + "]");
                        }
                }
                this.put(se.getName(), se);
                if (log.d) log.debug("  added " + se);
                if (se.isCabinet())
                    if (log.d) log.debug("TODO: new SyncList" + se);
                //todo: this is where we're going to insert
                // the recursion -- we need to be able to
                // create synclists that are syncentry's
                // (so we'll need to subclass sl from se)
                // and come up with a sensible status
                // for a directoriy entry based on status
                // of sub-entries (unless it's just NEW_REMOTE),
                // and there are no sub-entries yet, or maybe
                // we want to scan all sub entries, yeah, that,
                // and they'll all have NEW_REMOTE status.
                // may want a new status code of just CHANGED
                // to reflect directories with varied status children.
            }
        }
        
        return slev;
        // do NOT want to notifyChangeListeners here
    }


    /**
     * THIS IS THE MEAT WHERE SYNC SEMANATICS ARE WORKED OUT.
     *
     * Compare local & remote entries, returning computed
     * appropriate status for the local entry.
     * The remoteEntry is passed as a SyncEntry just for
     * convenience -- we're really just comparing the CabinetEntry instances.
     */
        
    protected int computeStatus(SyncEntry localEntry, SyncEntry remoteEntry)
    {
        if (localEntry.isGhost()) {
            if (remoteEntry.getStatus() == STATUS_CURRENT) {
                // when we do a sync, remote status doesn't get updated, so we do it here
                localEntry.setStatus(STATUS_CURRENT);
                return STATUS_CURRENT;
            }
            return STATUS_NEW_REMOTE;
        }
        if (remoteEntry.isGhost()) {
            if (localEntry.getStatus() == STATUS_CURRENT) {
                // when we do a sync, remote status doesn't get updated, so we do it here
                remoteEntry.setStatus(STATUS_CURRENT);
                // wait, BOGUS -- we need a new sync entry!
                return STATUS_CURRENT;
            }
            return STATUS_NEW_LOCAL;
        }
        
        try {

            if (localEntry.isByteStore() && remoteEntry.isByteStore())
            {
                ByteStore localByteStore = localEntry.getByteStore();
                ByteStore remoteByteStore = remoteEntry.getByteStore();
                long localSyncTime = localEntry.getLastSyncTime();
                long localMTime = localByteStore.getLastModifiedTime();
                long remoteMTime = remoteByteStore.getLastModifiedTime();

                boolean remoteChange = remoteMTime > localSyncTime;
                boolean localChange = localMTime > localSyncTime;
                    
                if (remoteChange && localChange)    return STATUS_CONFLICT;
                else if (localChange)               return STATUS_CHANGED_LOCAL;
                else if (remoteChange)              return STATUS_CHANGED_REMOTE;
                else {
                    long ll = localByteStore.length();
                    long rl = remoteByteStore.length();
                    if (ll != rl) {
                        log.errout("*SYNC ANOMALY: apparently current, yet sizes different: " + ll + " != " + rl);
                        log.errout("* syncEntryLocal=" + localEntry);
                        log.errout("*syncEntryRemote=" + remoteEntry);
                        log.errout("* ByteStorelocal=" + localByteStore);
                        log.errout("*ByteStoreRemote=" + remoteByteStore);
                        return STATUS_CONFLICT;
                    } else
                        return STATUS_CURRENT;
                }
            }
            // if both are CABINETS, status is going to wind up
            // unknown at the moment -- eventually cabinet status
            // will be derived from the sub-components, so unknown
            // okay for now. TODO - fix

            return STATUS_UNKNOWN;
                    
        } catch (Exception e) {
            log.errout("compairing " + localEntry + " " + e);
            return STATUS_UNKNOWN;
        }
    }
        
    protected ArrayList listeners = new ArrayList();
    public void addChangeListener(ChangeListener cl)
    {
        listeners.add(cl);
    }

    public void notifyChangeListeners(Object src)
    {
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            ChangeListener cl = (ChangeListener) i.next();
            ChangeEvent event;
            if (src instanceof ChangeEvent)
                event = (ChangeEvent) src;
            else
                event = new ChangeEvent(src);
            cl.stateChanged(event);
        }
    }

    public void removeChangeListener(ChangeListener cl)
    {
        listeners.remove(listeners.indexOf(cl));
    }
    public void removeAllChangeListeners()
    {
        listeners.clear();
    }
        
    boolean cancelRequested = false;
    public void requestSyncCancel()
    {
        cancelRequested = true;
        if (log.v) log.verbose("Cancel requested");
    }
    
    //TODO: okay, we want do be able to do a directional
    // sync anyway (all upload or all download) so split
    // this into directional, with less code, but run
    // it twice, once for each sync list?  So for instance,
    // would only handle NEW_LOCAL & CHANGED_LOCAL.

    
    public int doSynchronize()
        throws FilingException
    {
        return doSynchronize(SYNC_ALL);
    }
    
    public synchronized int doSynchronize(int syncType)
        throws FilingException
    {
        int changes = 0;
        syncRunning = true;
        try {
            changes = doSyncType(syncType);
        } finally {
            syncRunning = false;
            cancelRequested = false;
            //if any locks were obtained, release them.
            releaseLocks();
        }
        return changes;
    }

    private synchronized int doSyncType(int syncType)
        throws FilingException
    {
        if (!isLocalList)
            throw new RuntimeException("SyncList: remote list attempted sync");

        if (log.v) log.verbose("Synchronizing cabinets: type="+syncType);
        if (log.v) log.verbose("\t (local) " + getLocalCabinet());
        if (log.v) log.verbose("\t(remote) " + getRemoteCabinet());

        refresh(); // always refresh before a sync

        int updates = 0;
        Iterator i = this.iterator();
        while (i.hasNext())
        {
            if (cancelRequested)
                break;

            SyncEntry localEntry = (SyncEntry) i.next();

            if (!importAll && !localEntry.isSyncRequested())
                continue;
            
            int s = localEntry.getStatus();
            if (syncType == SYNC_UPLOADS_ONLY) {
                if (s == STATUS_NEW_REMOTE || s == STATUS_CHANGED_REMOTE)
                    continue;
            } else if (syncType == SYNC_DOWNLOADS_ONLY) {
                if (s == STATUS_NEW_LOCAL || s == STATUS_CHANGED_LOCAL)
                    continue;
            } else if (s == STATUS_CURRENT)
                continue;
                    
            if (doSynchronizeEntry(localEntry))
                updates++;
        }
        /*
         * if any locks were obtained, release them.
         */
        releaseLocks();
        
        if (takeAction && updates > 0) {
            this.saveProperties();
            this.readProperties();
        }

        if (updates > 0)
            refresh();
        
        // if we've created a new entry in remote cabinet,
        // we'll need to see it.  This is because the remote
        // cabinet may not be using ghosts... (why?)
        //if (updates > 0) remoteList.rescanNotify();
        return updates;
    }

    // just in case the VM makes this available to us
    // on abort (it doesn't on the mac...)
    protected void finalize()
    {
        releaseLocks();
    }

    /**
     * if any locks are held, release them.
     */
    public void releaseLocks()
    {
        releaseCabinetLock(localCabinet);
        releaseCabinetLock(remoteCabinet);
    }

    private HashMap cabinetLocks = new HashMap();
    /**
     * release a lock on cabinet c if we created it
     * @return true if a lock was released, false
     */
    protected boolean releaseCabinetLock(Cabinet c)
    {
        Object lock = cabinetLocks.get(c);
        if (lock == null)
            return false;
        
        CabinetEntry lockEntry = null;
        try {
            lockEntry = c.getCabinetEntry(LockName);
        } catch (Exception e) {
            cabinetLocks.remove(c);
            log.errout(e, "couldn't find our lock: somebody broke it?");
            return false;
        }

        try {
            lockEntry.delete();
        } catch (FilingException e) {
            log.errout(e, "failed to delete our own lock!");
            return false;
        }
        cabinetLocks.remove(c);
        if (log.v) log.verbose("Released lock on " + ceName(c) + " " + lock);
        return true;
    }

    /*
     * Cabinet locks are just a file created in the destination
     * directory during writes.  It's currently left to the user to decide
     * to break an old lock (which means you should present them
     * the information in the current lock to make that decision).
     * The lock is actually obtained the first time any write is
     * attempted, and held to the end of the entire sync, where
     * any locks that were created are cleared.
     *
     * Possible future semantics: to ensure we don't leave a lock
     * around, all locks considered no-good after certain interval
     * (e.g., 5 minutes) and it's the responsibility of the
     * lock-creating instance to keep it current (which means
     * implementing our own monitored version of copying which checks
     * how much time has gone by for each bufferful of data sent).
     * It would also be great if we could make use of File.deleteOnExit
     * to ensure a lock file was deleted no matter what when the VM
     * exits.  This could be done through Cabinet.createTempByteStore,
     * which is intended to have these semantics, if we added an
     * argument to the call which allowed us to specify the name of
     * the temp file.
     */

    /**
     * Look for an existing lock on a cabinet.
     * @return props of lock, or null if none found
     */
    ByteStore findLock(Cabinet c)
    {
        ByteStore lockEntry = null;
        try {
            lockEntry = (ByteStore) c.getCabinetEntry(LockName);
        } catch (NotFoundException e) {
            // no problem
        } catch (Exception e) {
            log.errout(e);
        }
        return lockEntry;
    }

    java.util.Properties readLock(Cabinet c)
    {
        return readLock(findLock(c));
    }
        
    java.util.Properties readLock(ByteStore lockEntry)
    {
        java.util.Properties lock = new java.util.Properties();
        if (lockEntry == null) {
            lock.setProperty("message", "false alarm, try again.");
            return null;
        }
        try {
            InputStream in = new JavaInputStreamAdapter(lockEntry.getOkiInputStream());
            lock.load(in);
            in.close();
        } catch (Exception e) {
            log.errout(e, "error reading lock");
            lock.setProperty("exception", e.toString());
        }
        return lock;
    }
        
    boolean obtainCabinetLock(Cabinet c)
    {
        if (cabinetLocks.containsKey(c)) {
            // we already have this lock -- nothing to do
            return true;
        }

        /*
         * look to see if there's an existing lock
         */
        CabinetEntry lockEntry = findLock(c);
        if (lockEntry != null) {
            if (log.v) log.verbose("obtainCabinetLock: already locked: " + lockEntry);
            return false;
        }
        
        /*
         * There's no existing lock -- we can create one
         */
        ByteStore lockBS = null;
        try {
            lockBS = c.createByteStore(LockName);
        } catch (FilingException e) {
            log.errout(e);
            return false;
        }
        Properties lock = new java.util.Properties();
        Date now = new Date();
        lock.setProperty("now", new Long(now.getTime()).toString());
        lock.setProperty("date", now.toString());
        try {
            lock.setProperty("this", c.getPath());
        } catch (FilingException e) {
            log.errout(e);
            lock.setProperty("this", e.toString());
        }

        lock.setProperty("user", System.getProperty("user.name"));
        String host = "[unknown host]";
        try {
            host = java.net.InetAddress.getLocalHost().toString();
        } catch (java.net.UnknownHostException e) {
            if (log.d) log.errout(e);
        }
        lock.setProperty("host", host);
        try {
            OutputStream os = new JavaOutputStreamAdapter(lockBS.getOkiOutputStream());
            lock.store(os, "Cabinet lock on " + c);
            os.close();
        } catch (Exception e) {
            log.errout(e);
            // if we created the lock file, yet
            // we failed to write handy information into it,
            // we still allow the lock to succeed.
        }
        cabinetLocks.put(c, lock);
        if (log.v) log.verbose("Created lock on " + ceName(c) + " " + lock);
        return true;
    }

    

    public boolean doSynchronizeEntry(SyncEntry localEntry)
        throws FilingException
    {
        SyncEntry remoteEntry = remoteList.get(localEntry.getName());
        boolean wasLocalGhost = localEntry.isGhost();
        boolean wasRemoteGhost = remoteEntry.isGhost();
        boolean change = false;
        
        // TODO: how do we handle desired deletions?
        // would need to flag somehow that something was deliberately
        // deleted... This implies either a master source, who's
        // simple non-presence of an entry takes priority, or we
        // need a special utility to flagging this..
        
        int syncStatus = localEntry.getStatus();
        CabinetEntry newEntry;
        if (log.d) log.debug("doSyncEntry:" + localEntry.toString());
        switch (syncStatus) {
        case STATUS_UNKNOWN:
            // skip -- print anything?
            break;
        case STATUS_CURRENT:
            // nothing to do.
            break;
        case STATUS_NEW_LOCAL:
            // create new byteStore in remote cabinet
            //            try {
                newEntry = copyCabinetEntry(getRemoteCabinet(), localEntry.getCabinetEntry(), "create", "remote");
                SyncEntry remoteMatch = remoteEntry;
                if (takeAction) {
                    if (remoteMatch != null) {
                        // must have found a ghost (assert isGhost)
                        // -- bring it to life now that it has it's own data.
                        remoteMatch.setCabinetEntry(newEntry);
                        remoteMatch.setIsGhost(false);
                    }
                    setLastSyncProperty(localEntry);
                    change = true;
                }
                //            } catch (Exception e) {
                //                log.errout(e);
                //            }
            break;
        case STATUS_CHANGED_LOCAL:
            // ship bytes to remote byteStore
            // TODO: should do a compare so if all bytes same, don't bother cascading updates
            // out to everyone who's listenting for changes.  Now, should we do the compare
            // locally or remotely?  (This also applies to STATUS_CHANGED_REMOTE)
            //            try {
                copyCabinetEntry(getRemoteCabinet(), localEntry.getCabinetEntry(), "copy", "remote");
                if (takeAction) {
                    setLastSyncProperty(localEntry);
                    change = true;
                }
                //            } catch (Exception e) {
                //                log.errout(e);
                //            }
            break;
        case STATUS_NEW_REMOTE:
            // in this special case (syncStats == STATUS_NEW_REMOTE)
            // and syncEntry.isGhost() == true, the
            // cabinet entry of the SyncEntry was pointed at
            // the REMOTE cabinet entry temporarily until we
            // synchronize it.
                
            CabinetEntry remoteCabEntry = localEntry.getCabinetEntry();

            // create new local entry to match the new remote
            //            try {
                newEntry = copyCabinetEntry(getLocalCabinet(), remoteCabEntry, "create", "local");
                // restore the local SyncEntry to sanity with a real local cabinetEntry
                if (takeAction) {
                    localEntry.setCabinetEntry(newEntry);
                    localEntry.setIsGhost(false);
                    setLastSyncProperty(localEntry);
                    change = true;
                }
                //            } catch (Exception e) {
                ///                log.errout(e);
                //            }
		    
            break;
        case STATUS_CHANGED_REMOTE:
            // grab bytes from remote byteStore
            //            try {
                CabinetEntry remoteCE = getRemoteCabinet().getCabinetEntry(localEntry.getName());
                copyCabinetEntry(getLocalCabinet(), remoteCE, "copy", "local");
                if (takeAction) {
                    setLastSyncProperty(localEntry);
                    change = true;
                }
                //            } catch (Exception e) {
                //                log.errout(e);
                //            }
            break;
        case STATUS_CONFLICT:
            // inform the user...
            break;
        default:
            log.errout("doSynchronize: unknown status: " + syncStatus);
        }

        if (change) {
            localEntry.setStatus(STATUS_CURRENT);
            remoteEntry.setStatus(STATUS_CURRENT);
            /*
             * Saving the properties here ensures that even if another
             * viewer looks at this cabinet, the sync state won't
             * look all out of whack.
             */
            this.saveProperties();

            if (visibleGhosts) {
                notifyChangeListeners(localEntry);
                remoteList.notifyChangeListeners(remoteEntry);
            } else {
                notifyChangeListeners(new SyncListChangeEvent(localEntry, 1, wasLocalGhost?1:0));
                remoteList.notifyChangeListeners(new SyncListChangeEvent(remoteEntry, 1, wasRemoteGhost?1:0));
            }
        }
        
        return change;
    }

    private void setLastSyncProperty(SyncEntry se)
    {
        long time = System.currentTimeMillis()+2000;
        // we need to add 999ms to the time because we may
        // lose it -- the properties file doesn't currently
        // save times with millisecond resolution.
        // Then we need to add another second because sometimes
        // on a PC, the file time is set to a time AFTER it's
        // call to copy the file has returned, which I assume
        // is due to slow write-back caching or something.
        
        se.setLastSyncTime(time);
        setEntrySyncTime(se, new Date(time).toString());
    }

    /**
     * @param destCabinet Cabinet we're creating the NEW CabinetEntry in
     * @param sourceEntry CabinetEntry we're copying
     * @param loc helpful string used only for verbose output (no function)
     *
     TODO: implement copy ourself so that we can progress-monitor it...
     InputStream in = new BufferedInputStream(
                          new ProgressMonitorInputStream(
                                  parentComponent,
                                  "Reading " + fileName,
                                  new FileInputStream(fileName)));
     */
    private CabinetEntry copyCabinetEntry(Cabinet destCabinet, CabinetEntry sourceEntry, String actionName, String loc)
        throws FilingException
    {
        if (!sourceEntry.canRead())
            throw new FilingException("copyCabinetEntry: can't read source " + sourceEntry);
        
        CabinetEntry newCabinetEntry = null;

        // Lfs filing BUG: this test doesn't seem to be working...
        if (sourceEntry.getParent().equals(destCabinet))
            log.errout("ack! incestous copy! cab=" + destCabinet + ", entry=" + sourceEntry);
	    
        if (sourceEntry.isByteStore()) {
            actionOut(sourceEntry.getPath() + " -> " + destCabinet.getPath());

            if (takeAction) {
                if (log.d) log.debug(actionName + " ByteStore '" + sourceEntry.getName() +
                      "' in/to " + loc + " cabinet " + destCabinet.getPath());

                if (!obtainCabinetLock(destCabinet)) {
                    throw new LockException(readLock(destCabinet),
                                            "cannont copy '" + sourceEntry.getName()
                                            + "' to " + destCabinet.getPath() + ": destination cabinet is locked");
                }
                
                ByteStore sourceBS = (ByteStore) sourceEntry;
                String sourceName = sourceBS.getName();
                String tmpName = sourceName + IncomingFileExtension;

                /*
                 * Go ahead and COPY THE BYTE STORE.  Note that createByteStore,
                 * when given a source bytestore argument, actually copies it.
                 */
                long srcLen = sourceBS.length();
                try {
                    newCabinetEntry = destCabinet.createByteStore(tmpName, sourceBS);
                    /*} catch (NameCollisionException e) {*/
                } catch (FilingException e) {
                    destCabinet.getCabinetEntry(tmpName).delete();
                    newCabinetEntry = destCabinet.createByteStore(tmpName, sourceBS);
                }
                
                /*
                 * Now do a simple verification check -- make sure the sizes
                 * are the same.
                 */
                ByteStore newBS = (ByteStore) newCabinetEntry;
                long destLen = newBS.length();
                if (srcLen != destLen) {
                    throw new FilingIOException("copy failed: sizes differ:"
                                                + "\n\t" + sourceBS + "=" + srcLen
                                                + "\n\t" + newBS + "=" + destLen);
                }
                /*
                 * Okay, we've got our bytes, and they look good.
                 * Now rename the temporary file to the real file name.
                 */
                // if we like, this is where we could backup the existing item --
                // move it out of the way to file.bak or something
                if (!newCabinetEntry.rename(sourceName))
                    throw new FilingException("rename of " + newCabinetEntry + " to " + sourceName + " failed");

            }
        }
        else if (sourceEntry.isCabinet()) {
            actionOut("copy Cabinet '" + sourceEntry.getName() + "' in " + loc + " cabinet " + destCabinet);
            if (takeAction)
                newCabinetEntry = destCabinet.createCabinet(sourceEntry.getName());
            // TODO: Need to recurse... somewhere.
        } else {
            log.errout(new Exception("copyCabinetEntry: neither ByteStore or Cabinet!"));
        }

        return newCabinetEntry;
    }

    private static String ceName(CabinetEntry ce)
    {
        String s;
        try {
            s = ce.getName();
        } catch (FilingException e) {
            s = "<"+e+">";
        }
        return s;
    }

    public String toString()
    {
        return "SyncList(" + getLocalCabinet() + ")";
    }

    private void actionOut(String s) { if (log.v) log.outln((takeAction ? "" : "(Skipped) ") + s); }


    //-----------------------------------------------------------------------------
    // END OF SyncList
    //-----------------------------------------------------------------------------


}
