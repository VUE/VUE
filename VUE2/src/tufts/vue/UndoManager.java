package tufts.vue;

import java.util.*;
import javax.swing.Action;

/**
 * Records all changes that take place in a map and
 * provides for arbitrarily marking named points of rollback.
 */

public class UndoManager
    implements LWComponent.Listener, VUE.ActiveMapListener
{
    private static boolean sInUndo = false;

    private ArrayList mUndoActions = new ArrayList(); // the list of undo actions (named groups of property changes)
    private ArrayList mRedoActions = new ArrayList(); // the list of redo actions (named groups of property changes)
    
    private LWMap mMap; // the map who's modifications we're tracking
    
    private Map mPropertyChanges = new HashMap(); // all property changes, mapped by component, since last mark
    //private Map mHierarchyChanges = new HashMap(); // all hierarchy changes, mapped by component, since last mark
    private LWCEvent mLastEvent; // most recent event since last mark
    private int mChangeCount; // individual property changes since last mark

    /**
     * A list (map) of components each with a list (map) of property changes to them.
     */
    static class UndoAction {
        String name;
        Map propertyChanges;
        int propertyChangeCount;
        UndoAction(String name, Map propertyChanges, int propCount) {
            this.name = name;
            this.propertyChanges = propertyChanges;
            this.propertyChangeCount = propCount;
        }

        void undoPropertyChanges()
        {
            if (DEBUG.UNDO) System.out.println(this + " undoPropertyChanges " + propertyChanges);

            Iterator i = propertyChanges.entrySet().iterator();
            // Handle hierarchy events first
            boolean hierarchyChanged = false;
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                Map props = (Map) e.getValue();
                if (props.containsKey(LWKey.HierarchyChanging)) {
                    undoComponentChanges((LWComponent) e.getKey(), props);
                    hierarchyChanged = true;
                    i.remove();
                }
            }
            i = propertyChanges.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                undoComponentChanges((LWComponent) e.getKey(), (Map) e.getValue());
            }
            if (hierarchyChanged)
                VUE.getSelection().clearDeleted();
        }

        private void undoComponentChanges(LWComponent c, Map props)
        {
            if (DEBUG.UNDO) System.out.println("\tprocessing component " + c);
            Iterator i = props.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                if (DEBUG.UNDO) System.out.println("\tundoing " + e);
                Object propKey = e.getKey();
                Object oldValue = e.getValue();
                if (propKey == LWKey.HierarchyChanging) {
                    undoHierarchyChange(c, oldValue);
                } else if (oldValue instanceof Undoable) {
                    ((Undoable)oldValue).undo();
                } else {
                    tufts.vue.beans.VueLWCPropertyMapper.setProperty(c, propKey, oldValue);
                }
            }
        }

        private void undoHierarchyChange(LWComponent c, Object oldValue)
        {
            if (DEBUG.UNDO) System.out.println(this + " restoring children of " + c + " to " + oldValue);
            LWContainer parent = (LWContainer) c;
            parent.children = (List) oldValue;
            Iterator ci = parent.children.iterator();
            // now make sure all the children are properly parented,
            // and none of them are marked as deleted.
            while (ci.hasNext()) {
                LWComponent child = (LWComponent) ci.next();
                child.setDeleted(false); // in case had been deleted
                child.setParent(parent);
            }
            parent.setScale(parent.getScale());
            parent.notify(LWKey.HierarchyChanging);
        }

        public String toString() {
            return "UndoAction[" + name
                + " cnt=" + propertyChangeCount
                + " propertyChanges=" + propertyChanges.size() + "]";
        }
    }

    public UndoManager(LWMap map)
    {
        mMap = map;
        map.addLWCListener(this);
        VUE.addActiveMapListener(this);
        setUndoActionLabel(null); // disable undo action at start
        new Throwable("UNDOMANAGERE)*FU)UF)*EFJW)*F").printStackTrace();
    }

    public void activeMapChanged(LWMap map)
    {
        if (map == mMap)
            setUndoActionLabel(peek());
    }

    private UndoAction pop()
    {
        if (mUndoActions.size() < 1)
            return null;
        int index = mUndoActions.size() - 1;
        UndoAction ua = (UndoAction) mUndoActions.get(index);
        mUndoActions.remove(index);
        return ua;
    }

    private UndoAction peek()
    {
        if (mUndoActions.size() > 0)
            return (UndoAction) mUndoActions.get(mUndoActions.size() - 1);
        else
            return null;
    }

    void flush() {
        mUndoActions.clear();
        mPropertyChanges.clear();
    }

    private boolean checkAndHandleUnmarkedChanges() {
        if (mChangeCount > 0) {
            new Throwable(this + " UNMARKED CHANGES! " + mPropertyChanges).printStackTrace();
            java.awt.Toolkit.getDefaultToolkit().beep();
            boolean olddb = DEBUG.UNDO;
            DEBUG.UNDO = true;
            markChangesAsUndo("Unmanaged Actions [last=" + mLastEvent.getWhat() + "]"); // collect whatever's there
            DEBUG.UNDO = olddb;
            return true;
        }
        return false;
    }

    private UndoAction mRedoAction;
    public void redo()
    {
        checkAndHandleUnmarkedChanges();

        if (DEBUG.UNDO) System.out.println(this + ": REDO");

        sInUndo = true;
        mRedoCaptured = false;
        mRedoAction.undoPropertyChanges();
        sInUndo = false;
    }
    
    public void undo()
    {
        checkAndHandleUnmarkedChanges();
        
        UndoAction undoAction = pop();
        if (DEBUG.UNDO) System.out.println(this + " undoing " + undoAction);
        if (undoAction != null) {
            try {
                sInUndo = true;
                mRedoCaptured = false;
                undoAction.undoPropertyChanges();
            } finally {
                sInUndo = false;
            }
        }
        //mRedoAction = collectChangesAsUndoAction("Redo " + undoAction.name);
        //Actions.Redo.putValue(Action.NAME, mRedoAction.name);
        setUndoActionLabel(peek());
    }

    private void setUndoActionLabel(UndoAction undoAction)
    {
        String name = "Undo ";

        if (undoAction != null) {
            // massage the name of the property to produce a more human
            // presentable name for the undo action
            if (DEBUG.UNDO||DEBUG.EVENTS) name += "#" + mUndoActions.size() + " ";
            String uaName = undoAction.name.replace('.', ' ');
            if (Character.isLowerCase(uaName.charAt(0)))
                uaName = Character.toUpperCase(uaName.charAt(0)) + uaName.substring(1);
            name += uaName;
            //if (DEBUG.UNDO||DEBUG.EVENTS) name += " (" + ua.propertyChanges.size() + ")";
            if (DEBUG.UNDO||DEBUG.EVENTS) name += " (" + undoAction.propertyChangeCount + ")";
            Actions.Undo.setEnabled(true);
        } else {
            Actions.Undo.setEnabled(false);
        }
        if (DEBUG.UNDO) System.out.println(this + " new UndoAction '" +  name + "'");
        Actions.Undo.putValue(Action.NAME, name);
    }
    
    /** figure the name of the undo action from the last LWCEvent we stored
     * an old property value for */
    public void mark() {
        markChangesAsUndo(null);
    }

    public void mark(String aggregateName) {
        // If only one property changed, use the name of that property,
        // otherwise use the aggregateName for the group of property
        // changes that took place.
        String name = null;
        if (mChangeCount == 1 && mLastEvent != null)
            name = mLastEvent.getWhat();
        else
            name = aggregateName;
        markChangesAsUndo(name);
    }

    public synchronized void markChangesAsUndo(String name)
    {
        if (mChangeCount == 0) // if nothing changed, don't bother adding an UndoAction
            return;
        if (name == null) {
            if (mLastEvent == null)
                return;
            name = mLastEvent.getWhat();
        }
        UndoAction newUndoAction = collectChangesAsUndoAction(name);
        mUndoActions.add(newUndoAction);
        setUndoActionLabel(newUndoAction);
    }

    private synchronized UndoAction collectChangesAsUndoAction(String name)
    {
        UndoAction newUndoAction = new UndoAction(name, mPropertyChanges, mChangeCount);
        if (DEBUG.UNDO) System.out.println(this + " marked " + mChangeCount + " property changes under '" + name + "'");
        mPropertyChanges = new HashMap();
        mLastEvent = null;
        mChangeCount = 0;
        return newUndoAction;
    }

    /**
     * Every event anywhere in the map we're listening to will
     * get delivered to us here.  If the event has an old
     * value in it, we save it for later undo.  If it's
     * a hierarchy event (add/remove/delete/forward/back, etc)
     * we handle it specially.
     */

    private boolean mRedoCaptured = false;
    public void LWCChanged(LWCEvent e) {
        if (sInUndo) {
if (true)return;
            if (!mRedoCaptured && mChangeCount > 0) 
                throw new Error("Undo Error: have changes at start of redo record: " + mChangeCount + " " + mPropertyChanges + " " + e);
            mRedoCaptured = true;
            if (DEBUG.UNDO) System.out.print("\tredo: " + e);
        } else {
            if (DEBUG.UNDO) System.out.print(this + " " + e);
        }
        processEvent(e);
    }

    private void processEvent(LWCEvent e)
    {
        if (e.getWhat() == LWKey.HierarchyChanging || e.getWhat().startsWith("hier.")) {
            recordHierarchyChangingEvent(e);
        } else if (e.hasOldValue()) {
            recordUndoablePropertyChangeEvent(e);
        } else {
            if (DEBUG.UNDO) {
                System.out.println(" (ignored: no old value)");
                if (DEBUG.META) new Throwable().printStackTrace();
            }
        }
    }

    //static class HierUndo extends Undoable { }

    private static final Object HIERARCHY_CHANGE = "hierarchy.change";
    private void recordHierarchyChangingEvent(LWCEvent e)
    {
        LWContainer parent = (LWContainer) e.getSource();
        //Object old = ((ArrayList)parent.children).clone();

        //if (DEBUG.UNDO) System.out.println(" (HIERARCHY)");

        recordUndoableChangeEvent(LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
        //new Undoable(old) { void undo() { parent.children = (ArrayList) old; } });        
        
    }
    
    private void recordUndoablePropertyChangeEvent(LWCEvent e)
    {
        // e.getComponent can really be list... todo: warn us if list (should only be for hier events)
        recordUndoableChangeEvent(e.getWhat(), e.getComponent(), e.getOldValue());
        mLastEvent = e;
    }

    private void recordUndoableChangeEvent(String propertyKey, LWComponent component, Object oldValue)
    {
        boolean compressed = false; // already had one of these props: can ignore all subsequent
        
        Map cPropList = (Map) mPropertyChanges.get(component);
        if (cPropList != null) {
            //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
            Object value = cPropList.get(propertyKey);
            if (value != null) {
                if (DEBUG.UNDO) System.out.println(" (compressed)");
                compressed = true;
            }
        } else {
            cPropList = new HashMap();
            mPropertyChanges.put(component, cPropList);
        }
        
        if (!compressed) {
            if (oldValue == HIERARCHY_CHANGE)
                oldValue = ((ArrayList)((LWContainer)component).children).clone();
            cPropList.put(propertyKey, oldValue);
            mChangeCount++;
            if (DEBUG.UNDO) {
                System.out.println(" (stored: " + oldValue + ")");
                //if (DEBUG.META) 
                //else System.out.println(" (stored)");
            }
        }
    }

    public String toString()
    {
        return "UNDO[" + mMap.getLabel() + " "
            + (mChangeCount<10?" ":"")
            + mChangeCount
            + "]"
            //+ hashCode()
            ;
    }
    
}

