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
 * CabinetEntry.java
 *
 * Created on September 16, 2003, 9:54 AM
 *
 *  The software contained in this file is copyright 2003 by Mark J. Norton, all rights reserved.
 */
package tufts.oki.localFiling;
import java.util.*;
import tufts.oki.shared.*;
import osid.OsidException;

/**
 *  Cabinet Entires are the superclass of Cabinet and ByteStore.  Entries contain
 *  a number of data items common to both including:
 *  <table>
 *  <tr><td>Id              </td><td>Unique identifier for this entry.</td></tr>
 *  <tr><td>Display Name    </td><td>Display name for this entry.</td></tr>
 *  <tr><td>Agent           </td><td>Agent-owner of this entry.</td></tr>
 *  <tr><td>Created Time    </td><td>Time this entry was created.</td></tr>
 *  <tr><td>Modified Time   </td><td>Last time this entry was modified.</td></tr>
 *  <tr><td>Accessed Time   </td><td>Last time this entry was accessed.</td></tr>
 *  <tr><td>Modified Times  </td><td>A list of all time this entry was modified.</td></tr>
 *  <tr><td>Parent<         /td><td>The parent cabinet of this entry, or null if it is a root.</td></tr>
 *  </table>
 *  CabinetEntry is fully implemented.
 *
 * @author  Mark Norton
 */
public class LocalCabinetEntry implements osid.filing.CabinetEntry
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LocalCabinetEntry.class);
    
    private osid.shared.Id id;
    private String display_name;
    private final osid.shared.Agent agent_owner;
//     private java.util.Date created_time;
//     private java.util.Date modified_time;
//     private java.util.Date accessed_time;
    private long created_time;
    private long modified_time;
    private long accessed_time;
    private final Vector modified_times;
    private final osid.filing.Cabinet parent;

    /**
     *  Creates a new instance of CabinetEntry.
     *  Requires that a display name, agent owner, and parent cabinet be passed.
     *
     *  @author Mark Norton
     *
     */
    public LocalCabinetEntry(String displayName, osid.shared.Agent agentOwner, osid.filing.Cabinet parentCabinet) {

        /*  Get a new unique ID for this CabinetEntry.  */
        try {
            id = new Id();
        }
        catch (osid.shared.SharedException e) {}
        
        /*  Set the display name and agent-owner.  */
        display_name = displayName;
        agent_owner = agentOwner;
        parent = parentCabinet;

        // TODO: considering we can create tons of these objects (cabinet's and
        // bytestores) when navigating a file-system, we'd be better off with a more
        // efficient impl that doesn't needlessly create tons all these Date objects no
        // matter what, never mind keep a vector of ever change around -- I hope
        // that's not a requirement of the OSID API!

        // Ahah: turns out this impl isn't semantically that meaningful: the created
        // and access times (modified also, actually), are never set by an
        // underlying file object
        
        /*  Initialize the various timestamps.  */
//         java.util.Date now = new Date();
//         created_time = (java.util.Date) now.clone();
//         modified_time = (java.util.Date) now.clone();
//         accessed_time = (java.util.Date) now.clone();
//         modified_times = new Vector();
//         modified_times.add(modified_time);

        final long now = getNow();
        created_time = now;
        modified_time = now;
        accessed_time = now;
        modified_times = new Vector(4);
        
        record(now);
    }

    private static long getNow() {
        return System.currentTimeMillis();
    }

    private void record(long when) {
        //System.out.println(this + "; mod " + (modified_times.size() + 1));
        modified_times.add(new Date(when));
    }
    
    
    /**
     *  Get the Id of this cabinet entry.
     *  @author Mark Norton
     *
     *  @return The id of this CabinetEntry.
     */
    public osid.shared.Id getId() throws osid.filing.FilingException {
        return (osid.shared.Id) id;
    }
    
    /**
     *  Get the display name of this cabinet entry.
     *
     *  @author Mark Norton
     *
     *  @return The display name of this CabinetEntry.
     */
    public String getDisplayName () throws osid.filing.FilingException {
        return display_name;
    }
    
    /**
     *  Update the modified time of this entry.
     *
     *  @author Mark Norton
     */
    public void touch () throws osid.filing.FilingException {
        record(modified_time = getNow());
    }
    
    /**
     *  Update the display name of this entry to the new name given.
     *
     *  @author Mark Norton
     */
    public void updateDisplayName (String new_disp_name) {
        //System.out.println ("CabinetEntry.updateDisplayName - name: " + new_disp_name);
        display_name = new_disp_name;
        record(modified_time = getNow());
    }
    
    /**
     *  Get the parent of this cabinet entry.
     *  Note that a ByteStore cannot be a root cabinet.
     *  @author Mark Norton
     *
     *  @return The parent cabinet of this entry, or null if it is a root cabinet.
     */
    public osid.filing.Cabinet getParent () {
        return parent;
    }
    
    /**
     *  Get the agent-ower of this entry.
     *
     *  @author Mark Norton
     */
    public osid.shared.Agent getCabinetEntryAgent() throws osid.filing.FilingException {
        return agent_owner;
    }
    
    /**
     *  Create an interator over a all times that this entry was modified.
     *
     *  @author Mark Norton
     *
     *  @return A CalendarIterator which lists all times this entry was modified.
     */
    public osid.shared.CalendarIterator getAllModifiedTimes() throws osid.filing.FilingException {
        CalendarIterator it = new CalendarIterator(modified_times);
        return it;
    }
    
    /**
     *  Get the time this entry was created.
     *
     *  @author Mark Norton
     *
     *  @return The time this entry was created.
     */
    public java.util.Calendar getCreatedTime() throws osid.filing.FilingException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(created_time);
        return cal;
    }
    
    /**
     *  Get the time when thsi entry was last accessed.
     *
     *  @author Mark Norton
     *
     *  @return The time this entry was last accessed.
     */
    public java.util.Calendar getLastAccessedTime() throws osid.filing.FilingException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(accessed_time);
        return cal;
    }
    
    /**
     *  Get the last modified time for this entry.
     *
     *  @author Mark Norton
     *
     *  @return The time this entry was last modified.
     */
    public java.util.Calendar getLastModifiedTime() throws osid.filing.FilingException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(modified_time);
        return cal;
    }
    
    public osid.shared.Agent getOwner() {
        return agent_owner;
    }
        
    public boolean isCabinet() {
        return (this instanceof LocalCabinet);
    }

    public String toString() {
        String name = "<unknown>";
        try {
            name = getDisplayName();
        } catch (Throwable t) {
            name = t.toString();
        }
        return String.format("%s@%07x[%s]", getClass().getSimpleName(), System.identityHashCode(this), name);
    }
    
}