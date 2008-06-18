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

import java.util.*;
import java.util.concurrent.*;
import tufts.Util;
import static tufts.Util.*;

/**
 * This provides for generic global event delivery / change support.
 *
 * The event object may be any object.  One handler is intended to be created
 * for each event type in the runtime.  Any source can post an event,
 * and all listeners will get the event.
 *
 * The listener must be a subclass of EventHandler.Listener.
 * This is designed so that the listener can be created
 * as an empty subclass, providing only type information.
 *
 * E.g.: <code>interface MyListener extends EventHandler.Listener&lt;MyEvent&gt; {}</code>
 *
 * Or when the listener and event are static inner classes to the class providing
 * the real event, a standard pattern can literally be used:
 *
 * E.g.: <code>interface Listener extends EventHandler.Listener&lt;Event&gt; {}</code>
 *
 * The above defines MyClass.Listener, and all that remains is to define MyClass.Event.
 *
 * Instances of this can be created on the fly by calling the static
 * method getHandler for a given class type, which will automatically
 * create a new handler for the given event type if one doesn't exist.
 *
 * @author Scott Fraize 2008-06-17
 * @version $Revision: 1.1 $ / $Date: 2008-06-18 02:31:34 $ / $Author: sfraize $
 */

// todo: see if we can subclass ActiveInstance from this to share code
public class EventHandler<E>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(EventHandler.class);

    public interface Listener<E> {
        public void eventRaised(E event);
    }
    
    private static final Map<Class,EventHandler> AllEventHandlers = new HashMap();
    //private static final List<Listener> ListenersForAllEvents = new CopyOnWriteArrayList();

    public static void addListener(Class clazz, Listener listener) {
        getHandler(clazz).addListener(listener);
    }
    public static void removeListener(Class clazz, Listener listener) {
        getHandler(clazz).removeListener(listener);
    }

    public static EventHandler getHandler(Class type) {
        EventHandler handler = null;
        lock(null, "getHandler");
        synchronized (AllEventHandlers) {
            handler = AllEventHandlers.get(type);
            if (handler == null)
                handler = new EventHandler(type, true);
        }
        unlock(null, "getHandler");
        return handler;
    }


    protected static int _depth = -1; // event delivery depth

    //=============================================================================
    
    protected final CopyOnWriteArrayList<Listener> mListeners = new CopyOnWriteArrayList();
    
    protected final Class _itemType;
    protected final String _itemTypeName; // for debug
    
    private E _lastEvent;
    private Object _lastSource;
    private boolean _inNotify;

    protected EventHandler(Class clazz, boolean track) {
        _itemType = clazz;
        _itemTypeName = "<" + _itemType.getName() + ">";

        if (track) {
        
        lock(clazz, "INIT");
        synchronized (AllEventHandlers) {
            if (AllEventHandlers.containsKey(_itemType)) {
                // tho this is not ideal, the safest thing to do is blow away the old one,
                // as it's likely this accidentally happened by a request for a generic
                // listener before a specialized side-effecting type-handler was initiated.
                // We copy over the listeners from the old handler if there were any.
                tufts.Util.printStackTrace("ignoring prior active change handler for " + getClass() + " and taking over listeners");
                mListeners.addAll(getHandler(_itemType).mListeners);
            }
            AllEventHandlers.put(_itemType, this);
        }
        unlock(clazz, "INIT");
        
        }
        
        if (DEBUG.INIT || DEBUG.EVENTS) Log.debug("created " + this);
    }

    public synchronized void redeliver() {
        raise(_lastSource, _lastEvent);
    }

    public synchronized void raise(final Object source, final E newEvent)
    {
        if (DEBUG.EVENTS) {
            Log.debug(TERM_GREEN + _itemTypeName);
            System.out.println(    "\t_lastEvent: " + _lastEvent
                               + "\n\t newEvent: " + newEvent
                               + "\n\t   source: " + sourceName(source)
                               + "\n\tlisteners: " + mListeners.size() + " in " + Thread.currentThread().getName()
                               + TERM_CLEAR
                               );
        }

        notifyListeners(source, newEvent);
    }


    protected void notifyListeners(final Object source, final E event)
    {
        if (_inNotify) {
            tufts.Util.printStackTrace(this + " event loop! aborting delivery of: " + event);
            return;
        }
        
        _inNotify = true;
        try {
            _depth++;
            if (mListeners.size() > 0)
                notifyListenerList(source, event, mListeners);
//             if (ListenersForAllEvents.size() > 0)
//                 notifyListenerList(this, source, event, ListenersForAllEvents);
        } finally {
            _depth--;
            _inNotify = false;
        }
        
        _lastEvent = event;
        _lastSource = source;
    }
            
    protected void notifyListenerList(final Object source,
                                      final E event,
                                      final Collection<Listener> listenerList)
    {
        int count = 0;

        for (Listener target : listenerList) {
            
            count++;

            if (source == target) {
                if (DEBUG.EVENTS) outf("    %2dskipSrc %s -- %s\n", count, _itemTypeName, target);
                continue;
            } else
                if (DEBUG.EVENTS) outf("    %2d notify %s -> %s\n", count, _itemTypeName, target);
                
            try {
                
                //-------------------------------------------------------
                // dispatch the event
                //-------------------------------------------------------
                
                dispatch(target, event);
                
            } catch (Throwable t) {
                Util.printStackTrace(t, this + " exception notifying " + target + " with " + event);
            }
        }
    }

    /** override to provide complex dispatch */
    protected void dispatch(Listener target, E event) {
        target.eventRaised(event);
    }

    public void addListener(Listener listener) {
        if (mListeners.addIfAbsent(listener)) {
            if (DEBUG.EVENTS) Log.debug(String.format(TERM_GREEN + "%-50s added listener %s" + TERM_CLEAR, _itemTypeName, listener));
        } else {
            Log.warn(this + "; add: is already listening: " + listener);
            if (DEBUG.EVENTS)
                Util.printStackTrace(this + "; FYI: already listening: " + listener);
        }
    }

    public void removeListener(Listener listener) {
        if (mListeners.remove(listener)) {
            if (DEBUG.Enabled) 
                outf(TERM_GREEN + "%-50s removed listener %s\n" + TERM_CLEAR, this, listener);
        } else if (DEBUG.EVENTS) {
            Log.warn(this + "; remove: didn't contain listener " + listener);
        }
    }

    protected static String sourceName(Object s) {
        if (s == null)
            return "null";
        else if (s instanceof ActiveEvent || s instanceof EventHandler)
            return s.toString();
        else
            return s.toString();
        //return Util.tags(s);
        //return s.getClass().getName() + ":" + s;
    }


    protected static void lock(Object o, String msg) {
        if (DEBUG.THREAD) System.err.println((o == null ? "EventHandler" : o) + " " + msg + " LOCK");
    }
    protected static void unlock(Object o, String msg) {
        if (DEBUG.THREAD) System.err.println((o == null ? "EventHandler" : o) + " " + msg + " UNLOCK");
    }
    
    protected static void outf(String fmt, Object... args) {
        for (int x = 0; x < _depth; x++) System.out.print("    ");
        System.out.format(fmt, args);
        //Log.debug(String.format(fmt, args));
    }
    

    @Override
    public String toString() {
        return getClass().getSimpleName() + _itemTypeName;
    }

    
        
        
}
