package tufts.vue;

/**
 * This provides for tracking the single selection of a given typed
 * object, and providing notifications for interested listeners when this selection
 * changes.  The one drawback to using a generics approach here is that once
 * type-erasure is complete, all the listener signatures are the same, so that a
 * single object can only ever listen for activeChanged calls for a single type
 * if it is declared as implementing an ActiveListener that includes type information.  In
 * practice, it's easy to deal with this limitation using anonymous classes, or
 * by using the same handler method, and checking the class type in the passed
 * ActiveEvent (we can't rely on checking the type of the what's currently active,
 * as it may be null).
 *
 * Instances of this can be created on the fly by calling the static
 * method getHandler for a given class type, which will automatically
 * create a new handler for the given type if one doesn't exist.
 * If special handlers have been created that have side-effects (e.g., in onChange),
 * make sure they're instantiated before anyone asks for a handler
 * for the type that they handle.


 * @author Scott Fraize 2007-05-05
 * @version $Revision: 1.1 $ / $Date: 2007-05-06 20:14:17 $ / $Author: sfraize $
 */

// ResourceSelection could be re-implemented using this, as long
// as we stay with only a singly selected resource object at a time.
public class ActiveChangeSupport<T>
{
    private static final java.util.Map<Class,ActiveChangeSupport> AllActiveHandlers = new java.util.HashMap();
        
    private final java.util.List<ActiveListener<T>> listenerList = new java.util.ArrayList();
    private final Class type;
    private final String typeName; // for debug

    private T currentlyActive;

    private boolean inNotify;

    public ActiveChangeSupport(Class clazz) {
        type = clazz;
        typeName = "<" + type.getName() + ">";
        synchronized (AllActiveHandlers) {
            if (AllActiveHandlers.containsKey(type)) {
                // tho this is an error, the safest thing to do is blow away the old one,
                // as it's likely this accidentally happened by a request for a generic
                // listener before a specialized side-effecting type-handler was initiated.
                // Listeners registered to the old handler will never get updates.
                // todo: could just copy over listener list from old handler
                tufts.Util.printStackTrace("blowing away prior active change handler for " + getClass());
            }
            AllActiveHandlers.put(type, this);
        }
        if (DEBUG.Enabled) System.out.println("Created ActiveChangeSupport"  + typeName);
    }

    public static ActiveChangeSupport getHandler(Class type) {
        synchronized (AllActiveHandlers) {
            ActiveChangeSupport handler = AllActiveHandlers.get(type);
            if (handler == null)
                handler = new ActiveChangeSupport(type);
            return handler;
        }
    }

    public void setActive(Object source, T newActive) {
        if (currentlyActive == newActive)
            return;
        if (DEBUG.EVENTS) System.out.format("ActiveInstance%s nowActive: %s  (source is %s)\n", typeName, newActive, source);
        final T oldActive = currentlyActive;
        currentlyActive = newActive;
        final ActiveEvent e = new ActiveEvent(type, source, oldActive, newActive);
        notifyListeners(e);
        onChange(e);
    }

    protected void onChange(ActiveEvent<T> e) {}

    protected void notifyListeners(ActiveEvent<T> e) {
        if (inNotify) {
            tufts.Util.printStackTrace("notifyLoop in " + this);
            return;
        }

        // todo: synchronize or allow concurrent modification
        
        inNotify = true;
        try {
            for (ActiveListener<T> listener : listenerList) {
                if (listener == e.source)
                    continue;
                if (DEBUG.EVENTS) System.out.println("\tnotify" + typeName + " -> " + listener);
                try {
                    listener.activeChanged(e);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(new Throwable(t), this + "exception notifying " + listener + " with " + e);
                }
            }
        } finally {
            inNotify = false;
        }
    }

    public T getActive() {
        return currentlyActive;
    }

    public void addListener(ActiveListener listener) {
        synchronized (listenerList) {
            listenerList.add(listener);
        }
    }

    public void removeListener(ActiveListener listener) {
        synchronized (listenerList) {
            listenerList.remove(listener);
        }
    }

    public String toString() {
        return "ActiveChangeSupport" + typeName;
        
    }
        
        
}
