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
    private static boolean sUndoUnderway = false;

    private ArrayList mUndoActions = new ArrayList(); // the list of undo actions (named groups of property changes)
    private ArrayList mRedoActions = new ArrayList(); // the list of redo actions (named groups of property changes)
    
    private LWMap mMap; // the map who's modifications we're tracking
    
    private Map mComponentChanges = new HashMap(); // all changes, mapped by component, since last mark
    private LinkedList mUndoSequence = new LinkedList();
    
    private LWCEvent mLastEvent; // most recent event since last mark
    private int mChangeCount; // total recorded changes since last mark

    /**
     * A named sequence of triples: LWComponent, property key, and old value.
     * LWCEvents are undone in the reverse order that they happened: the changes
     * are peeled back.
     */
    private static class UndoAction {
        String name;
        List undoSequence;

        UndoAction(String name, List undoSequence) {
            this.name = name;
            this.undoSequence = undoSequence;
        }
        
        void undo() {
            if (DEBUG.UNDO) System.out.println(this + " undoing sequence of size " + changeCount());
            ListIterator i = undoSequence.listIterator(undoSequence.size());
            boolean hierarchyChanged = false;
            while (i.hasPrevious()) {
                UndoItem undoItem = (UndoItem) i.previous();
                undoItem.undo();
                if (undoItem.propKey == LWKey.HierarchyChanging)
                    hierarchyChanged = true;
            }
            if (hierarchyChanged)
                VUE.getSelection().clearDeleted();
        }

        int changeCount() {
            return undoSequence.size();
        }
        
        public String toString() {
            return "UndoAction["
                + name
                + " cnt=" + changeCount()
                + "]";
        }
    }

    private static class UndoItem
    {
        LWComponent component;
        String propKey;
        Object oldValue;

        UndoItem(LWComponent c, String propertyKey, Object oldValue) {
            this.component = c;
            this.propKey = propertyKey;
            this.oldValue = oldValue;
        }

        void undo() {
            if (DEBUG.UNDO) System.out.println("UNDOING: " + this);
            if (propKey == LWKey.HierarchyChanging) {
                undoHierarchyChange(component, oldValue);
            } else if (oldValue instanceof Undoable) {
                ((Undoable)oldValue).undo();
            } else {
                tufts.vue.beans.VueLWCPropertyMapper.setProperty(component, propKey, oldValue);
            }
        }
            
        private void undoHierarchyChange(LWComponent c, Object oldValue)
        {
            if (DEBUG.UNDO) System.out.println("\trestoring children of " + c + " to " + oldValue);
            LWContainer parent = (LWContainer) c;
            parent.children = (List) oldValue;
            Iterator ci = parent.children.iterator();
            // now make sure all the children are properly parented,
            // and none of them are marked as deleted.
            while (ci.hasNext()) {
                LWComponent child = (LWComponent) ci.next();
                if (parent instanceof LWPathway)
                    ; // special case: todo: something cleaner (pathways don't "own" their children)
                else {
                    if (child.isDeleted())
                        child.restoreToModel();
                    child.setParent(parent);
                }
            }
            parent.setScale(parent.getScale());
            parent.layout();
            parent.notify(LWKey.HierarchyChanging);
        }

        public String toString() {
            Object old = oldValue;
            if (oldValue instanceof Collection) {
                Collection c = (Collection) oldValue;
                if (c.size() > 1)
                    old = c.getClass().getName() + "{" + c.size() + "}";
            }
            return "UndoItem[" + component
                + " key=" + propKey
                + " old=" + old
                + "]";
        }

    }

    public UndoManager(LWMap map)
    {
        mMap = map;
        map.addLWCListener(this);
        VUE.addActiveMapListener(this);
        setUndoActionLabel(null); // disable undo action at start
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
        //mPropertyChanges.clear();
        //mHierarchyChanges.clear();
        mComponentChanges.clear();
    }

    private boolean checkAndHandleUnmarkedChanges() {
        if (mChangeCount > 0) {
            new Throwable(this + " UNMARKED CHANGES! " + mComponentChanges).printStackTrace();
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
        if (true)return;
        checkAndHandleUnmarkedChanges();

        if (DEBUG.UNDO) System.out.println(this + ": REDO");

        sUndoUnderway = true;
        mRedoCaptured = false;
        mRedoAction.undo();
        sUndoUnderway = false;
    }
    
    public void undo()
    {
        checkAndHandleUnmarkedChanges();
        
        UndoAction undoAction = pop();
        if (DEBUG.UNDO) System.out.println(this + " undoing " + undoAction);
        if (undoAction != null) {
            try {
                sUndoUnderway = true;
                mRedoCaptured = false;
                undoAction.undo();
            } finally {
                sUndoUnderway = false;
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
            String uName = undoAction.name;
            if (uName.startsWith("hier."))
                uName = uName.substring(5);
            // Replace all '.' with ' ' and capitalize first letter of each word
            uName = uName.replace('-', '.');
            String[] word = uName.split("\\.");
            for (int i = 0; i < word.length; i++) {
                if (Character.isLowerCase(word[i].charAt(0)))
                    word[i] = Character.toUpperCase(word[i].charAt(0)) + word[i].substring(1);
                if (i > 0)
                    name += " ";
                name += word[i];
            }
            if (DEBUG.UNDO||DEBUG.EVENTS) name += " (" + undoAction.changeCount() + ")";
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
        UndoAction newUndoAction = new UndoAction(name, mUndoSequence);
        if (DEBUG.UNDO) System.out.println(this + " marked " + mChangeCount + " property changes as " + newUndoAction);
        mUndoSequence = new LinkedList();
        mComponentChanges.clear();
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
        if (sUndoUnderway) {
if (true)return;
            if (!mRedoCaptured && mChangeCount > 0) 
                throw new Error("Undo Error: have changes at start of redo record: " + mChangeCount + " " + mComponentChanges + " " + e);
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
            recordPropertyChangeEvent(e);
        } else {
            if (DEBUG.UNDO) {
                System.out.println(" (ignored: no old value)");
                if (DEBUG.META) new Throwable().printStackTrace();
            }
        }
    }

    private static final Object HIERARCHY_CHANGE = "hierarchy.change";
    private void recordHierarchyChangingEvent(LWCEvent e)
    {
        LWContainer parent = (LWContainer) e.getSource();
        //recordUndoableChangeEvent(mHierarchyChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
        recordUndoableChangeEvent(mComponentChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
        mLastEvent = e;
    }
    
    private void recordPropertyChangeEvent(LWCEvent e)
    {
        // e.getComponent can really be list... todo: warn us if list (should only be for hier events)
        //recordUndoableChangeEvent(mPropertyChanges, e.getWhat(), e.getComponent(), e.getOldValue());
        recordUndoableChangeEvent(mComponentChanges, e.getWhat(), e.getComponent(), e.getOldValue());
        mLastEvent = e;
    }

    private static class IndexedProperty {
        int index;
        Object value;

        IndexedProperty(int index, Object value) {
            this.index = index;
            this.value = value;
        }
    }
    

    private void recordUndoableChangeEvent(Map map, String propertyKey, LWComponent component, Object oldValue)
    {
        boolean compressed = false; // already had one of these props: can ignore all subsequent
        
        Map cPropMap = (Map) map.get(component);
        IndexedProperty existingPropertyValue = null;
        if (cPropMap != null) {
            //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
            //Object value = cPropMap.get(propertyKey);
            existingPropertyValue = (IndexedProperty) cPropMap.get(propertyKey);
            if (existingPropertyValue != null) {
                if (DEBUG.UNDO) System.out.println(" (compressed)");
                compressed = true;
            }
        } else {
            cPropMap = new HashMap();
            map.put(component, cPropMap);
        }
        
        if (compressed) {
            // If compressed, still make sure the current property change UndoItem is
            // at the end of the undo sequence.
            if (existingPropertyValue.index != mUndoSequence.size() - 1) {
                UndoItem undoItem = (UndoItem) mUndoSequence.remove(existingPropertyValue.index);
                if (DEBUG.UNDO) System.out.println("Moving index "
                                                   +existingPropertyValue.index+" to end index "+mUndoSequence.size()
                                                   + " " + undoItem);
                existingPropertyValue.index = mUndoSequence.size();
                mUndoSequence.add(undoItem);
            }
        } else {
            if (oldValue == HIERARCHY_CHANGE)
                oldValue = ((ArrayList)((LWContainer)component).children).clone();
            cPropMap.put(propertyKey, new IndexedProperty(mUndoSequence.size(), oldValue));
            mUndoSequence.add(new UndoItem(component, propertyKey, oldValue));
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
        return "UndoManager[" + mMap.getLabel() + " "
            + (mChangeCount<10?" ":"")
            + mChangeCount
            + "]"
            //+ hashCode()
            ;
    }
    
}

