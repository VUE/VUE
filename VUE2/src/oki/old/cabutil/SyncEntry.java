import org.okip.service.filing.api.*;

import java.util.*;
import java.text.*;

public class SyncEntry
{
    private static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

    int syncStatus = SyncList.STATUS_UNKNOWN;
    long lastSync = 0;
    boolean isGhost = false;
    boolean isSyncRequested = false;

    private final String STATUS_NAMES[] = {
        "UNKNOWN  ",
        "CURRENT  ",
        "NEWLOCAL ",
        "CHGLOCAL ",
        "NEWREMOTE",
        "CHGREMOTE",
        "CONFLICT ",
    };
    private final String STATUS_DESCRIPTIONS[] = {
        "-unknown-",
        "Current",
        "New",          //new entry - "new"
        "Changed",      //new version - "newer"
        "New(Remote)",  //ghost
        "Old",          //old version - "old"
        "*Conflict*",
    };
    private final String STATUS_CODES[] = { "u?", "CU", "NL", "CL", "NR", "CR", "*C", };

    // in all cases except where syncStatys == STATUS_NEW_REMOTE,
    // cabEntry is in a LOCAL cabinet.  Otherwise, it's temporarily
    // pointed at the remote cabinet entry
    private CabinetEntry localEntry;

    public SyncEntry(CabinetEntry ce)
    {
        setCabinetEntry(ce);
    }

    /**
     * is a sync specifically requested for this item?
     */
    public boolean isSyncRequested()
    {
        return isSyncRequested;
    }
    protected void doSetSyncRequested(boolean tv)
    {
        isSyncRequested = tv;
    }
    public boolean isGhost()
    {
        return isGhost;
    }

    public void setIsGhost(boolean tv)
    {
        isGhost = tv;
        syncStatus = SyncList.STATUS_NEW_REMOTE;
    }

    public void makeGhostOf(SyncEntry se)
    {
        setIsGhost(true);
        setCabinetEntry(se.getCabinetEntry());
    }
    
    public long getLastSyncTime()
    {
        return this.lastSync;
    }
        
    public void setLastSyncTime(long date)
    {
        this.lastSync = date;
    }
        
    protected boolean setStatus(int s)
    {
        if (syncStatus == s)
            return false;
        syncStatus = s;
        if (s == SyncList.STATUS_NEW_REMOTE)
            setIsGhost(true);
        return true;
    }
    public int getStatus()
    {
        return this.syncStatus;
    }
        
    public void setCabinetEntry(CabinetEntry ce)
    {
        this.localEntry = ce;
        if (ce == null)
            throw new RuntimeException("null CabinetEntry");
    }
        
    public CabinetEntry getCabinetEntry()
    {
        return localEntry;
    }

    public ByteStore getByteStore()
    {
        // manage the cast exception / the problem of SyncEntries
        // not really being bytestores...
        return (ByteStore) getCabinetEntry();
    }

    /*
     * assuming the names are the same, does
     * the data look identical?
     */
//     public boolean isEqualData(CabinetEntry ce)
//     {
//         if (ce instanceof ByteStore) {
//             if (!isByteStore())
//                 return false;
//             ByteStore rbs = (ByteStore) ce;
//             ByteStore lbs = (ByteStore) localEntry;;
//             try {
//                 return
//                     rbs.getLastModifiedTime() == lbs.getLastModifiedTime()
//                     && rbs.length() == lbs.length();
//             } catch (Exception e) {
//                 System.err.println(e);
//                 return false;
//             }
//         }
//         else if (ce instanceof Cabinet) {
//             //todo
//         }
//         return false;
//     }

    public String toString()
    {
        if (isGhost)
            // meaning, our CabinetEntry is a temporary pointer to the remote cabinet entry
            return "SyncEntry<" + descriptionString() + ">";
        else
            return "SyncEntry(" + descriptionString() + ")";
    }

    public String descriptionString()
    {
        String s = new String();

        CabinetEntry ce = getCabinetEntry();
        long length = -1;
        
        String lastMtimeStr = "-";
        String lastSyncTimeStr = "-";
        try {
            s += getStatusCode() + " ";
            s += ce.isCabinet() ? 'd' : '-';
            s += ce.canRead() ? 'r' : '-';
            s += ce.canWrite() ? 'w' : '-';
            s += " '" + ce.getPath() + "'";
            
            try {
                if (ce.isByteStore()) {
                    ByteStore bs = ((ByteStore)ce);
                    length = bs.length();
                    lastMtimeStr = dateFormatter.format(new Date(getLastModifiedTime()));
                    lastSyncTimeStr = dateFormatter.format(new Date(getLastSyncTime()));
                }
            } catch (Exception e) {
                lastMtimeStr = "-";
                //System.err.println(e);
            }
        } catch (Exception e) {
            s += "<"+e.getMessage()+">";
        }

            
        s += " " + lastMtimeStr + " / " + lastSyncTimeStr;
        if (length >= 0)
            s += " s=" + length;
        return s;
    }


    public String getStatusName()
    {
        return STATUS_NAMES[syncStatus];
    }

    public String getStatusCode()
    {
        return STATUS_CODES[syncStatus];
    }
    public String getStatusDescription()
    {
        return STATUS_DESCRIPTIONS[syncStatus];
    }

    public String getName()
    {
        try {
            return localEntry == null ? "<null>" : localEntry.getName();
        } catch (FilingException e) {
            return e.toString();
        }
    }
    public boolean isCabinet()
    {
        try {
            return localEntry.isCabinet();
        } catch (FilingException e) {
            return false;
        }
    }
    public boolean isByteStore()
    {
        try {
            return localEntry.isByteStore();
        } catch (FilingException e) {
            return false;
        }
    }

    public long getLastModifiedTime()
    {
        long t = 0;
        try {
            if (localEntry instanceof ByteStore)
                t = ((ByteStore) localEntry).getLastModifiedTime();
            else if (localEntry instanceof Cabinet)
                t = ((Cabinet) localEntry).getLastModifiedTime();
        } catch (FilingException e) {
            //debug.log(e.toString());
            //System.out.println(e.getMessage());
            t = -1;
        }
        return t;
    }

    public boolean exists()
    {
        try {
            return localEntry.exists();
        } catch (FilingException e) {
            System.err.println(e);
            return false;
        }
    }
}
//-----------------------------------------------------------------------------
// END OF SyncEntry
//-----------------------------------------------------------------------------

