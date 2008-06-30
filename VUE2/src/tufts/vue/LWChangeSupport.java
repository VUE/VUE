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

package tufts.vue;

import tufts.Util;
import static tufts.Util.*;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Handle dispatching of LWCEvents, mainly for LWComponents, but any client
 * class can use this for event dispatch, selective listening, and the heavy-duty
 * diagnostic support.
 * 
 * @version $Revision: 1.10 $ / $Date: 2007/11/19 06:20:27 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class LWChangeSupport
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWChangeSupport.class);
    
    private static int sEventDepth = 0;
    
    private List listeners;
    private Object mClient;
    private boolean mEventsDisabled = false;
    private int mEventSuspensions = 0;
    
    LWChangeSupport(Object client) {
        mClient = client;
    }
    private static class LWCListenerProxy implements LWComponent.Listener
    {
        private final LWComponent.Listener listener;
        private final Object eventMask;

        public LWCListenerProxy(LWComponent.Listener listener, Object eventsDesired) {
            this.listener = listener;
            this.eventMask = eventsDesired;
        }

        /** this should never actually get used, as we pluck the real listener
            out of the proxy in dispatch */
        public void LWCChanged(LWCEvent e) {
            listener.LWCChanged(e);
        }

        public boolean isListeningFor(LWCEvent e)
        {
            if (eventMask instanceof Object[]) {
                for (Object desiredKey : (Object[]) eventMask) {
                    if (e.key == desiredKey)
                        return true;
                }
                return false;
            } else {
                return eventMask == e.key;
            }
        }
        

        public String toString() {
            String s = listener.toString();
            if (eventMask != null) {
                s += ":only<";
                if (eventMask instanceof Object[]) {
                    Object[] eventKeys = (Object[]) eventMask;
                    for (int i = 0; i < eventKeys.length; i++) {
                        if (i>0) s+= ",";
                        s += eventKeys[i];
                    }
                } else {
                    s += eventMask;
                }
                s += ">";
            }
            return s;
        }
    }

    private class LWCListenerList extends java.util.Vector {
        public synchronized int indexOf(Object elem, int index) {
            if (elem == null) {
                for (int i = index ; i < elementCount ; i++)
                    if (elementData[i]==null)
                        return i;
            } else {
                for (int i = index ; i < elementCount ; i++) {
                    Object ed = elementData[i];
                    if (elem.equals(ed))
                        return i;
                    if (ed instanceof LWCListenerProxy && ((LWCListenerProxy)ed).listener == elem)
                        return i;
                }
            }
            return -1;
        }
        public synchronized int lastIndexOf(Object elem, int index) {
            throw new UnsupportedOperationException("lastIndexOf");
        }
    }

    /**
     * Move @param listener - existing listener, to the front of the
     * notification list, so it get's notifications before anyone else.
     */

    /* Todo: create ChangeSupport style class for managing & notifying
     * listeners, with setPriority, etc, that LWSelection, the VUE
     * map & viewer listeners, etc, can all use.
     */
    public synchronized void setPriorityListener(LWComponent.Listener listener) {

        if (listeners == null) {
            Log.error("Attempting to set priorty listener with no listeners at all for client " + mClient);
            return;
        }
        
        int i = listeners.indexOf(listener);
        
        if (i > 0) {
            if (i == 1) {
                // swap first two
                listeners.set(1, listeners.set(0, listener));
            } else {
                // remove & add back at the front
                listeners.remove(listener);
                listeners.add(0, listener);
            }
        } else if (i == 0) {
            ; // already priority listener
        } else
            throw new IllegalArgumentException(mClient + " " + listener + " not already a listener");
    }
    
    public synchronized void addListener(LWComponent.Listener listener) {
        addListener(listener, null);
    }

    public synchronized void addListener(LWComponent.Listener listener, Object eventMask)
    {
        if (listeners == null)
            listeners = new LWCListenerList();
        if (listeners.contains(listener)) {
            // do nothing (they're already listening to us)
            if (DEBUG.EVENTS) {
                if (DEBUG.META) System.out.println("already listening to us: " + listener + " " + mClient);
                //if (DEBUG.META) new Throwable("already listening to us:" + listener + " " + mClient).printStackTrace();
            }
        } else if (listener == mClient) {
            // This is likely to produce event loops, and the exception is here as a safety measure.
            throw new IllegalArgumentException(mClient + " attempt to add self as LWCEvent listener: not allowed");
        } else {
            if (DEBUG.EVENTS && DEBUG.META)
                outln("*** LISTENER " + listener + "\t+++ADDS " + mClient + (eventMask==null?"":(" eventMask=" + eventMask)));
            if (eventMask == null) {
                listeners.add(listener);
            } else
                listeners.add(new LWCListenerProxy(listener, eventMask));
        }
    }
    public synchronized void removeListener(LWComponent.Listener listener)
    {
        if (listeners == null)
            return;
        if (DEBUG.EVENTS && DEBUG.META) System.out.println("*** LISTENER " + listener + "\tREMOVES " + mClient);
        listeners.remove(listener);
    }
    public synchronized void removeAllListeners()
    {
        if (listeners != null) {
            if (DEBUG.EVENTS) System.out.println(mClient + " *** CLEARING ALL LISTENERS " + listeners);
            listeners.clear();
        }
    }

    private void setEventsEnabled(boolean t) {
        if (DEBUG.EVENTS&&DEBUG.META) System.out.println(mClient + " *** EVENTS ENABLED: from " + !mEventsDisabled + " to " + t);
        mEventsDisabled = !t;
    }

    protected synchronized void setEventsSuspended() {
        mEventSuspensions++;
        setEventsEnabled(false);
    }
    protected synchronized void setEventsResumed() {
        mEventSuspensions--;
        if (mEventSuspensions < 0)
            throw new IllegalStateException("events suspend/resume unpaired");
        if (mEventSuspensions == 0)
            setEventsEnabled(true);
    }

    public boolean eventsDisabled() {
        return mEventsDisabled;
    }

    private static final String DELIVERY_ARROW = TERM_GREEN + " => " + TERM_CLEAR;


    /**
     * This method for clients that are LWComponent's ONLY.  Otherwise call dispatchLWCEvent
     * directly.
     */
    synchronized void notifyListeners(LWComponent client, LWCEvent e)
    {
        if (mEventsDisabled) {
            if (DEBUG.EVENTS) System.out.println(e + " (dispatch skipped: events disabled)");
            return;
        }
        
        if (client.isDeleted()) {
            String msg =
                "FYI, ZOMBIE EVENT: notifyListeners; deleted component attempting event notification:"
                + "\n\t        deleted client: " + client
                + "\n\tattempting delivery of: " + e
                + "\n\t     current listeners: " + listeners
                + "\n\tparent (ought be null): " + client.getParent()
                + "\n"
                ;
            // this situation not so serious at this point: we may have no listeners
            if (DEBUG.Enabled)
                Util.printStackTrace(msg);
            else
                Log.warn(msg);
        }

        //if (DEBUG.EVENTS && (DEBUG.META || !DEBUG.THREAD)) {
        if (DEBUG.EVENTS && (DEBUG.META || e.isUndoable())) {
            final String ldesc = (listeners == null
                                  ? " -> <no listeners>"
                                  : ((listeners.size()>0?TERM_GREEN:"") + " => (" + listeners.size() + " listeners)" + TERM_CLEAR
                                     //+ " " + Arrays.asList(listeners)
                                     ));
                                //: (DELIVERY_ARROW + "(" + listeners.size() + " listeners)"));a
            if (client != e.getSource())
                eoutln(e + " => " + client + ldesc);
            else
                eoutln(e + ldesc);
        }
        
        if (listeners != null && listeners.size() > 0) {
            if (DEBUG.EVENTS && DEBUG.META) eoutln(e + " dispatching for client " + client + " to listeners " + Arrays.asList(listeners));
            dispatchLWCEvent(client, listeners, e);
        } else {
            //if (DEBUG.EVENTS && DEBUG.THREAD && (DEBUG.META || DEBUG.CONTAINMENT))
            if (DEBUG.EVENTS && DEBUG.THREAD)
                eoutln(e + " -> " + "<NO LISTENERS>" + (client.isOrphan() ? " (orphan)":""));
        }

        // todo: have a seperate notifyParent? -- every parent
        // shouldn't have to be a listener

        // todo: "added" events don't need to go thru parent chain as
        // a "childAdded" event has already taken place (but
        // listeners, eg, inspectors, may need to know to see if the
        // parent changed)
        
        if (client.getParent() != null) {
            if (DEBUG.EVENTS && DEBUG.META) {
                eoutln(e + " " + client.getParent() + " ** PARENT UP-NOTIFICATION");
            }
            client.getParent().broadcastChildEvent(e);
        } else if (client.isOrphan()) {
            if (listeners != null && listeners.size() > 0) {
                System.out.println("*** ORPHAN NODE w/LISTENERS DELIVERED EVENTS:"
                                   + "\n\torphan=" + client
                                   + "\n\tevent=" + e
                                   + "\n\tlisteners=" + listeners);
                if (DEBUG.PARENTING) new Throwable().printStackTrace();
            }
            /*else if (DEBUG.META && (DEBUG.EVENTS || DEBUG.PARENTING) && !(this instanceof LWGroup))
                // dragged selection group is a null parented object, so we're
                // ignoring all groups for purposes of this diagnostic for now.
                System.out.println(e + " (FYI: orphan node event)");
            */
        }
    }

    private static void eout(String s) {
        synchronized (System.err) {
            //if (DEBUG.THREAD) System.err.format("%-27s", Thread.currentThread().toString().substring(6));
            if (!javax.swing.SwingUtilities.isEventDispatchThread())
                System.err.format("[%s]", Thread.currentThread().getName());
            for (int x = 0; x < sEventDepth; x++) System.err.print("--->");
            System.err.print(s);
        }
    }
    
    private static void eoutln(String s) {
        eout(s + "\n");
    }
    private static void outln(String s) {
        System.err.println(s);
    }
    
    public void dispatchEvent(LWCEvent e) {
        if (listeners != null)
            dispatchLWCEvent(mClient, listeners, e);
    }

    /**
     * Deliver LWCEvent @param e to all the @param listeners
     */
    static synchronized void dispatchLWCEvent(Object source, List listeners, LWCEvent e)
    {
        if (sEventDepth > 5) // guestimate max based on current architecture -- increase if you need to
            throw new IllegalStateException("eventDepth=" + sEventDepth
                                            + ", assumed looping on delivery of "
                                            + e + " in " + source + " to " + listeners);

        if (source instanceof LWComponent && ((LWComponent)source).isDeleted() || listeners == null) {
            System.err.println("ZOMBIE DISPATCH: deleted component or null listeners attempting event dispatch:"
                               + "\n\tsource=" + source
                               + "\n\tlisteners=" + listeners
                               + "\n\tattempted notification=" + e);
            new Throwable("ZOMBIE DISPATCH").printStackTrace();
            return;
        }
        
        // todo perf: take array code out and see if can fix all
        // concurrent mod exceptions (e.g., delete out from under a
        // pathway was giving us some problems, tho I think that may
        // have gone away) or: allow listener removes via nulling, tho
        // that's not really a concern anyway in that a component that
        // removes itself as a listener after having been notified of
        // an event has already had it's notification and we don't
        // need to make sure it doesn't get one further down the list.
        
        //int nlistener = listeners.size();
        //Listener[] listener_array = (Listener[]) listeners.toArray(listener_buf);
        //if (listener_array != listener_buf)
        //    out("FYI: listener count " + nlistener + " exceeded performance buffer.");
        // Of course, using a static buf of course doesn't work the second we have any event depth!
        // and we can't make it a member as this is a static method...
        // We could actually just only allocate a new array if there IS any event depth...

        LWComponent.Listener[] listener_array = new LWComponent.Listener[listeners.size()];
        listeners.toArray(listener_array);
        for (int i = 0; i < listener_array.length; i++) {
            if (DEBUG.EVENTS && DEBUG.META) {
                if (e.getSource() != source)
                    eout(e + " " + i + " => " + source + " >> ");
                else
                    eout(e + " " + i + " >> ");
            }
            LWComponent.Listener target = listener_array[i];
            //-------------------------------------------------------
            // If a listener proxy, extract the real listener
            //-------------------------------------------------------
            if (target instanceof LWCListenerProxy) {
                LWCListenerProxy lp = (LWCListenerProxy) target;
                if (!lp.isListeningFor(e)) {
                    if (DEBUG.EVENTS && DEBUG.META)
                        outln(target + " (filtered)");
                    continue;
                }
                target = lp.listener;
            }
            //-------------------------------------------------------
            // now we know we have the real listener
            //-------------------------------------------------------
            if (DEBUG.EVENTS && DEBUG.THREAD) {
                if (DEBUG.META) {
                    if (e.getSource() == target)
                        outln(target + " (SKIPPED: source)");
                } else if (e.getSource() != target) {
                    if (e.getSource() != source)
                        eout(e + " => " + source + DELIVERY_ARROW);
                    else
                        eout(e + DELIVERY_ARROW);
                }
            }
            if (e.getSource() == target) // this prevents events from going back to their source
                continue;
            sEventDepth++;
            try {
                if (DEBUG.EVENTS && DEBUG.THREAD)
                    outln(target + "");

                //-------------------------------------------------------
                // deliver the event
                //-------------------------------------------------------

                target.LWCChanged(e);

            } catch (Throwable t) {
                tufts.Util.printStackTrace
                    (t,
                     "dispatchLWCEvent: exception during LWCEvent notification:"
                     + "\n\tnotifying component: " + source
                     + "\n\t          event was: " + e
                     + "\n\t   failing listener: " + target);
            } finally {
                sEventDepth--;
            }
            if (DEBUG.EVENTS && DEBUG.META) eoutln(e + " disptach returned from: " + target);
        }
    }
}