/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.util.ArrayList;

/**

 * This is for events issued from within an LWComponent object
 * hierarchy.  Usually they are property change events, although in
 * some cases they are used for more general purpose signaling.

 * <p>In addition to the functionality of a
 * java.beans.PropertyChangeEvent, this event also tells you who the
 * change happened to (not always the same as the source: e.g.,
 * hierarchy events are usually sourced from the the parent), and in the
 * case of multiple components changing, can give you an entire list
 * of components who were part of a single change (especially useful
 * for hierarchy events, where issuing individual events would be
 * messy given that the event delivery hierarchy would be changing as
 * each subsequent event is issued).  LWCevent also differs from a
 * PropertyChangeEvent in that it does not include the NEW value,
 * as that can be obtained by asking the changed component to
 * tell us the current value, and because more often than not,
 * the listener already has code to pull that out anyway: it just
 * wants to know it's time to go ahead and do it's thing because
 * something important to it has changed.  As it is, it only contains
 * the oldValue field for the UndoManager.  (And LWComponents are not
 * supposed to fire an LWCEvent notification unless a value has
 * actually changed.)

 * <p>LWCevent is also used for special events placed into the
 * hierachy, such as as "user action completed" event, which can be
 * useful for signaling model listeners that if you've been waiting to
 * update yourself (usually for performance reasons), now is the time
 * to catch up.  E.g., the MapPanner doesn't bother to attempt
 * repainting until it sees this event.  In VUE, the UndoManager
 * singles these events for us, as it already has to have been
 * explicitly told when a user action has been completed in order to
 * know when to stop and collect a new aggregate undo action.
 
 * <p>It is also used in special cases to essentially issue a "repaint"
 * event to the GUI, although these are stop-gap cases that ultimately
 * would be better handled as a recognized property change.
 
 */

// Rename getWhat to getKey and add getName which does key.toString()
// Consider subclassing from PropertyChangeEvent.
// Consider LWCEvent as a keyed event with a source only,
// and adding subclasses for the property change events?

public class LWCEvent
{
    /** for null masking */
    public static final String NO_OLD_VALUE = "no_old_value";
    
    private Object source;
    
    //a LWCevent can either hold a single component or an array of components
    //one of them is always null
    private LWComponent component = null;
    private ArrayList components = null;
    private Object oldValue = null;
    
    private Object key;
    
    // todo: we still using both src & component?
    public LWCEvent(Object source, LWComponent c, Object key, Object oldValue)
    {
        this.source = source;
        this.component = c;
        this.key = key;
        this.oldValue = oldValue;
    }

    public LWCEvent(Object source, LWComponent c, Object key) {
        this (source, c, key, NO_OLD_VALUE);
    }

    public LWCEvent(Object source, ArrayList components, Object key)
    {
        this.source = source;
        this.components = components;
        this.key = key;
        this.oldValue = NO_OLD_VALUE;
    }
    
    public Object getSource()
    {
        return this.source;
    }
    
    public LWComponent getComponent()
    {
        if (component == null && components != null && components.size() > 0) {
            if (/*DEBUG.EVENTS &&*/ components.size() > 1) {
                System.out.println(this + " *** RETURNING FIRST IN LIST IN LIU OF LIST OF LENGTH " + components.size());
                new Throwable().printStackTrace();
            }
            return (LWComponent) components.get(0);
        } else
            return component;
    }
    
    public ArrayList getComponents() {
        return this.components;
    }

    public String getWhat() {
        if (this.key instanceof LWComponent.Key)
            return ((LWComponent.Key)key).name;
        else
            return (String) this.key;
    }
    public Object getKey() {
        return this.key;
    }
    public String getKeyName() {
        if (this.key instanceof LWComponent.Key)
            return ((LWComponent.Key)key).name;
        else
            return (String) this.key;
    }

    /**
     * Returns an old value if one was given to us.  As null is a valid
     * old value, it's distinguished from having no old value set
     * by the the value LWCEvent.NO_OLD_VALUE.  Or you can
     * check for the presence of an old value by calling hasOldValue().
     */
    public Object getOldValue() {
        return this.oldValue;
    }
    /** @return true if there is an old value to this event */
    public boolean hasOldValue() {
        return this.oldValue != NO_OLD_VALUE;
    }

    public String toString()
    {
        String s = "LWCEvent[" + key
            + " s=" + source;
        
        if (component != null && component != source)
            s += " c:" + component;
        //basic information.. if more information wants to be stringfied, need to code this part
        else if (components != null)
            s += " cl:" + components;
        //s += " ArrayList";
              
        return s + "]";
    }
}

