package tufts.vue;

import java.util.*;
import javax.swing.Action;

/**
 * UndoManager
 *
 * Records all changes that take place in a LWMap (as seen from
 * LWCEvent delivery off the LWMap) and provides for arbitrarily
 * marking named points of rollback.
 *
 * For robustness, if the application fails to mark any changes,
 * they'll either have been rolled undo another undo action, or
 * stuffed into an un-named Undo action if they attempt an undo while
 * there are unmarked changes.
 *
 * @author Scott Fraize
 * @version March 2004
 */

public class UndoManager
    implements LWComponent.Listener, VUE.ActiveMapListener
{
    private static boolean sUndoUnderway = false;
    private static boolean sRedoUnderway = false;

    /* The list of undo actions (named groups of property changes) */
    private UndoActionList UndoList = new UndoActionList("Undo"); 
    /* The list of redo actions (named groups of property changes generated from Undo's) */
    private UndoActionList RedoList = new UndoActionList("Redo"); 
    
    /* The map who's modifications we're tracking. */
    private LWMap mMap; 
    
    /* All recorded changes since last mark, mapped by component (for detecting & ignoring repeats) */
    private Map mComponentChanges = new HashMap();
    /* All recorded changes since last mark, marked for sequential processing */
    private List mUndoSequence = new ArrayList(); 
    /* The last LWCEvent we didn't ignore since last mark -- used for guessing at good Undo action title names */
    private LWCEvent mLastEvent;
    /* The total number of recorded or compressed changes since last mark (will be >= mUndoSequence.size()) */
    private int mChangeCount;

    private static class UndoActionList extends ArrayList
    {
        private String name;
        private int current = -1;

        UndoActionList(String name) {
            this.name = name;
        }
        
        public boolean add(Object o) {
            // when adding, flush anything after the top
            if (current < size() - 1) {
                int s = current + 1;
                int e = size();
                if (DEBUG.UNDO) out("flushing " + s + " to " + e + " in " + this);
                removeRange(s, e);
            }
            if (DEBUG.UNDO) out("adding: " + o);
            super.add(o);
            current = size() - 1;
            return true;
        }
        UndoAction pop() {
            if (current < 0)
                return null;
            return (UndoAction) get(current--);
        }
        UndoAction peek() {
            if (current < 0)
                return null;
            return (UndoAction) get(current);
        }

        void advance() {
            current++;
            if (current >= size())
                throw new IllegalStateException(this + " top >= size()");
        }

        public void clear() {
            super.clear();
            current = -1;
        }

        int top() {
            return current;
        }

        private void out(String s) {
            System.out.println("\tUAL[" + name + "] " + s);
        }
        
        public String toString() {
            return "UndoActionList[" + name + " top=" + top() + " size=" + size() + "]";
        }
        
        public void add(int index, Object element) { throw new UnsupportedOperationException(); }
        public Object remove(int index) { throw new UnsupportedOperationException(); }
    }

    /**
     * A named sequence of triples: LWComponent, property key, and old value.
     * LWCEvents are undone in the reverse order that they happened: the changes
     * are peeled back.
     */
    private static class UndoAction {
        private String name;
        private List undoSequence; // list of UndoItem's -- will be sorted by sequence index before first use
        private boolean sorted = false;

        UndoAction(String name, List undoSequence) {
            this.name = name;
            this.undoSequence = undoSequence;
        }
        
        void undo() {
            try {
                sUndoUnderway = true;
                run_undo();
            } finally {
                sUndoUnderway = false;
            }
        }

        private void run_undo() {
            if (DEBUG.UNDO) System.out.println(this + " undoing sequence of size " + changeCount());

            if (!sorted) {
                Collections.sort(undoSequence);
                sorted = true;
                if (DEBUG.UNDO){
                    System.out.println("=======================================================");
                    VueUtil.dumpCollection(undoSequence);
                    System.out.println("-------------------------------------------------------");
                }
            }

            boolean hierarchyChanged = false;
            
            //-------------------------------------------------------
            // First, process all hierarchy events
            //-------------------------------------------------------
            
            ListIterator i = undoSequence.listIterator(undoSequence.size());
            while (i.hasPrevious()) {
                UndoItem undoItem = (UndoItem) i.previous();
                if (undoItem.propKey == LWKey.HierarchyChanging) {
                    undoItem.undo();
                    hierarchyChanged = true;
                }
            }

            //-------------------------------------------------------
            // Second, process all property change events
            //-------------------------------------------------------
            
            i = undoSequence.listIterator(undoSequence.size());
            while (i.hasPrevious()) {
                UndoItem undoItem = (UndoItem) i.previous();
                if (undoItem.propKey != LWKey.HierarchyChanging)
                    undoItem.undo();
            }
            
            if (hierarchyChanged)
                VUE.getSelection().clearDeleted();
        }

        int changeCount() {
            return undoSequence.size();
        }

        String getName() {
            return name;
        }
        
        /**
         *  massage the name of the property to produce a more human
         *  presentable name for the undo action.
         */
        String getDisplayName() {
            if (DEBUG.UNDO) return this.name + " {" + changeCount() + "}";
            
            String name = "";
            String uName = this.name;
            if (uName == LWKey.HierarchyChanging)
                uName = "Change";
            else if (uName.startsWith("hier."))
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
            return name;
        }
    
        public String toString() {
            return "UndoAction["
                + name
                + " cnt=" + changeCount()
                + "]";
        }
    }

    private static class UndoItem implements Comparable
    {
        LWComponent component;
        String propKey;
        Object oldValue;
        int index;

        UndoItem(LWComponent c, String propertyKey, Object oldValue, int index) {
            this.component = c;
            this.propKey = propertyKey;
            this.oldValue = oldValue;
            this.index = index;
        }

        void undo() {
            if (DEBUG.UNDO) System.out.println("UNDOING: " + this);
            if (propKey == LWKey.HierarchyChanging) {
                undoHierarchyChange((LWContainer) component, oldValue);
            } else if (oldValue instanceof Undoable) {
                ((Undoable)oldValue).undo();
            } else {
                tufts.vue.beans.VueLWCPropertyMapper.setProperty(component, propKey, oldValue);
            }
        }
            
        private void undoHierarchyChange(LWContainer parent, Object oldValue)
        {
            if (DEBUG.UNDO) System.out.println("\trestoring children of " + parent + " to " + oldValue);
            // todo: compute additions/deletions and generate childrenAdded & childrenRemoved events
            // for things like the outline view which listen for them (or: redo outline code
            // to just rebuild everything on the HierarchyChanged event)
            parent.notify(LWKey.HierarchyChanging); // this event important for REDO (can we optimize?)
            parent.children = (List) oldValue;
            Iterator ci = parent.children.iterator();
            // now make sure all the children are properly parented,
            // and none of them are marked as deleted.
            while (ci.hasNext()) {
                LWComponent child = (LWComponent) ci.next();
                if (parent instanceof LWPathway) {
                    // Special case for pathways. todo: something cleaner (pathways don't "own" their children)
                    ((LWPathway)parent).addChildRefs(child);
                } else {
                    if (child.isDeleted())
                        child.restoreToModel();
                    child.setParent(parent);
                }
            }
            parent.setScale(parent.getScale());
            parent.layout();
            parent.notify(LWKey.HierarchyChanged);
        }

        public int compareTo(Object o) {
            return index - ((UndoItem)o).index;
        }
        
        public String toString() {
            Object old = oldValue;
            if (oldValue instanceof Collection) {
                Collection c = (Collection) oldValue;
                if (c.size() > 1)
                    old = c.getClass().getName() + "{" + c.size() + "}";
            }
            return "UndoItem["
                + index + (index<10?" ":"")
                + " " + propKey
                + " " + component
                + " old=" + old
                + "]";
        }

    }

    public UndoManager(LWMap map)
    {
        mMap = map;
        map.addLWCListener(this);
        VUE.addActiveMapListener(this);
        activeMapChanged(map); // make sure actions disabled at start
    }

    public void activeMapChanged(LWMap map)
    {
        if (map == mMap)
            updateActionLabels();
    }

    private void updateActionLabels() {
        setActionLabel(Actions.Undo, UndoList);
        setActionLabel(Actions.Redo, RedoList);
    }

    void flush() {
        UndoList.clear();
        RedoList.clear();
        mComponentChanges.clear();
        if (VUE.getActiveMap() == mMap)
            updateActionLabels();
    }

    private boolean checkAndHandleUnmarkedChanges() {
        if (mUndoSequence.size() > 0) {
            new Throwable(this + " UNMARKED CHANGES! " + mComponentChanges).printStackTrace();
            java.awt.Toolkit.getDefaultToolkit().beep();
            boolean olddb = DEBUG.UNDO;
            DEBUG.UNDO = true;
            markChangesAsUndo("Unnamed Actions [last=" + mLastEvent.getWhat() + "]"); // collect whatever's there
            DEBUG.UNDO = olddb;
            return true;
        }
        return false;
    }

    private boolean mRedoCaptured = false; // debug
    public synchronized void redo()
    {
        checkAndHandleUnmarkedChanges();
        mRedoCaptured = false;
        UndoAction redoAction = RedoList.pop();
        if (DEBUG.UNDO) System.out.println(this + " redoing " + redoAction);
        if (redoAction != null) {
            try {
                sRedoUnderway = true;
                redoAction.undo();
            } finally {
                sRedoUnderway = false;
            }
            UndoList.advance();
        }
        updateActionLabels();
    }
    
    public synchronized void undo()
    {
        checkAndHandleUnmarkedChanges();
        
        UndoAction undoAction = UndoList.pop();
        if (DEBUG.UNDO) System.out.println(this + " undoing " + undoAction);
        if (undoAction != null) {
            mRedoCaptured = false;
            undoAction.undo();
        }
        RedoList.add(collectChangesAsUndoAction(undoAction.name));
        updateActionLabels();
        // We've undo everything: we can mark the map as having no modifications
        if (UndoList.peek() == null)
            mMap.markAsSaved();
    }

    private void setActionLabel(Action a, UndoActionList undoList) {
        String label = undoList.name;
        if (DEBUG.UNDO) label += "#" + undoList.top() + "["+undoList.size()+"]";
        if (undoList.top() >= 0) {
            label += " " + undoList.peek().getDisplayName();
            if (DEBUG.UNDO) System.out.println(this + " now available: '" + label + "'");
            a.setEnabled(true);
        } else
            a.setEnabled(false);
        a.putValue(Action.NAME, label);
    }

    /** figure the name of the undo action from the last LWCEvent we stored
     * an old property value for */
    public void mark() {
        markChangesAsUndo(null);
    }

    /**
     * If only one property changed, use the name of that property,
     * otherwise use the @param aggregateName for the group of property
     * changes that took place.
     */
    
    public void mark(String aggregateName) {
        String name = null;
        if (mUndoSequence.size() == 1 && mLastEvent != null)
            name = mLastEvent.getWhat();
        else
            name = aggregateName;
        markChangesAsUndo(name);
    }

    public synchronized void markChangesAsUndo(String name)
    {
        if (mUndoSequence.size() == 0) // if nothing changed, don't bother adding an UndoAction
            return;
        if (name == null) {
            if (mLastEvent == null)
                return;
            name = mLastEvent.getWhat();
        }
        UndoList.add(collectChangesAsUndoAction(name));
        RedoList.clear();
        updateActionLabels();
    }

    private synchronized UndoAction collectChangesAsUndoAction(String name)
    {
        UndoAction newUndoAction = new UndoAction(name, mUndoSequence);
        if (DEBUG.UNDO) System.out.println(this + " marked " + mChangeCount + " property changes as " + newUndoAction);
        mUndoSequence = new ArrayList();
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
    public void LWCChanged(LWCEvent e) {
        if (sRedoUnderway) // ignore everything during redo
            return;

        if (sUndoUnderway) {
            if (!mRedoCaptured && mUndoSequence.size() > 0) 
                throw new Error("Undo Error: have changes at start of redo record: " + mUndoSequence.size() + " " + mComponentChanges + " " + e);
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

    private static class TaggedPropertyValue {
        int index;
        Object value;

        TaggedPropertyValue(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public String toString() {
            return value + "~" + index;
        }
    }
    

    private void recordUndoableChangeEvent(Map map, String propertyKey, LWComponent component, Object oldValue)
    {
        boolean compressed = false; // already had one of these props: can ignore all subsequent
        
        Map cPropMap = (Map) map.get(component);
        TaggedPropertyValue existingPropertyValue = null;
        if (cPropMap != null) {
            //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
            //Object value = cPropMap.get(propertyKey);
            existingPropertyValue = (TaggedPropertyValue) cPropMap.get(propertyKey);
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
            // marked as being at the current end of the undo sequence.
            if (mUndoSequence.size() > 1) {
                UndoItem undoItem = (UndoItem) mUndoSequence.get(existingPropertyValue.index);
                if (DEBUG.UNDO&&DEBUG.META) System.out.println("Moving index "
                                                               +existingPropertyValue.index+" to end index "+mChangeCount
                                                               + " " + undoItem);
                undoItem.index = mChangeCount++;
            }
        } else {
            if (oldValue == HIERARCHY_CHANGE)
                oldValue = ((ArrayList)((LWContainer)component).children).clone();
            cPropMap.put(propertyKey, new TaggedPropertyValue(mUndoSequence.size(), oldValue));
            mUndoSequence.add(new UndoItem(component, propertyKey, oldValue, mChangeCount));
            mChangeCount++;
            if (DEBUG.UNDO) {
                System.out.println(" (stored: " + oldValue + ")");
                //if (DEBUG.META) 
                //else System.out.println(" (stored)");
            }
        }
    }

    private static void out(String s) {
        System.out.println("UndoManger: " + s);
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

