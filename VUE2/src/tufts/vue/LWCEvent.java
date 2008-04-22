/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package tufts.vue;

import java.util.List;
import static tufts.Util.*;

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

 * @version $Revision: 1.35 $ / $Date: 2008-04-22 06:57:07 $ / $Author: sfraize $  
 
 */

// Rename getWhat to getKey and add getName which does key.toString()
// Consider subclassing from PropertyChangeEvent.
// Consider LWCEvent as a keyed event with a source only,
// and adding subclasses for the property change events?

public class LWCEvent
{
    /** This is either a proper LWComponent Key object, or a String */
    public final Object key;
    
    /** This is either an actual old value, or an Undoable that is capable of restoring the old value */
    final Object oldValue;

    /** What initiated this event -- usually the same as component */
    public final Object source;
    
    /** a LWCevent can either hold a single component or an array of components: one of them is always null */
    private List<LWComponent> components = null;
    public final LWComponent component;
    
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

    public LWCEvent(Object source, List<LWComponent> components, Object key)
    {
        this.source = source;
        this.components = components;
        this.component = null;
        this.key = key;
        this.oldValue = NO_OLD_VALUE;
    }
    
    public Object getSource()
    {
        return this.source;
    }

    /** @return true if this event originated within the model itself (e.g., not something like a proxy repaint event) */
    public boolean isModelSourced() {

        // todo: this won't be accurate for group events where component is null and
        // components is set.  We'll return false, tho that's okay for now as this will
        // only increase the handle priority of the event, so it's an okay fallback
        // position.  Eventually, we should set component to the parent LWComponent
        // experiencing the add/remove of a collection of children, so we can accurately
        // determine if this was a model-sourced event (as oppsed to, say, an Action
        // sourced event).
        
        return source == component;
    }

    public LWComponent getComponent()
    {
        if (component == null && components != null && components.size() > 0) {
            if (DEBUG.Enabled && components.size() > 1) {
                tufts.Util.printStackTrace(this + "RETURNING FIRST ONLY WHEN IT CONTAINS "
                                           + components.size() + "\n" + components + "\n");
            }
            return (LWComponent) components.get(0);
        } else
            return component;
    }

    public LWComponent onlyComponent() {
        if (component == null && components != null)
            return null;
        else
            return component;
    }
    
    
    public List<LWComponent> getComponents() {
        return this.components;
    }

    /** @return the name of the key for this LWCEvent */
    public String getName() {
        if (this.key instanceof LWComponent.Key)
            return ((LWComponent.Key)key).name;
        else
            return (String) this.key;
    }

    /** If the key is a proper Key, return it, otherwise, return null */
    public LWComponent.Key getKey() {
        if (this.key instanceof LWComponent.Key)
            return (LWComponent.Key) key;
        else
            return null;
    }

    /**
     * Returns an old value if one was given to us.  As null is a valid
     * old value, it's distinguished from having no old value set
     * by the the value LWCEvent.NO_OLD_VALUE.  Or you can
     * check for the presence of an old value by calling hasOldValue().
     * If our stored oldValue is actually an Undoable, this will unpack
     * the old value from the Undoable and return it (if one was provided).
     */
    public Object getOldValue() {
        if (oldValue instanceof Undoable)
            return ((Undoable)oldValue).old;
        else
            return this.oldValue;
    }
    /** @return true if there is an old value to this event */
    public boolean hasOldValue() {
        return this.oldValue != NO_OLD_VALUE;
    }

    public boolean isUndoable() {
        return hasOldValue();
    }
    
    public String toString() {
        //return "LWCEvent[" + paramString() + "]";
        //return "[" + paramString() + "]";
        return String.format(TERM_PURPLE + "%07X[%s%s]%s", hashCode(), paramString(), TERM_PURPLE, TERM_CLEAR);
        
    }
    
    public String paramString() 
    {
        final StringBuffer buf = new StringBuffer(//TERM_PURPLE +
                                                  String.format("%-20s ", key)
                                                  + source);
        //+ TERM_CLEAR + " " + source);
        
        if (component != null && component != source) {
            buf.append(" c=" + component);
            //basic information.. if more information wants to be stringfied, need to code this part
        } else if (components != null) {
            buf.append(" list(" + components.size() + ": ");

            if (components.size() == 1) {
                buf.append(components.get(0));
            } else {
                java.util.Iterator<LWComponent> iter = components.iterator();
                while (iter.hasNext()) {
                    LWComponent c = iter.next();
                    buf.append(c.getUniqueComponentTypeLabel());
                    if (iter.hasNext())
                        buf.append(", ");
                }
            }
            buf.append(')');
        }
        if (oldValue != null && oldValue != NO_OLD_VALUE)
            buf.append(" (" + oldValue + ")");
              
        return buf.toString();
    }


    /** for null masking */
    public static final String NO_OLD_VALUE = "no_old_value";
}

