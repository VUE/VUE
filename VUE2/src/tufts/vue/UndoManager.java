package tufts.vue;

import java.util.*;
import javax.swing.Action;

/**
 * Records all changes that take place in a map and
 * provides for arbitrarily marking named points of rollback.
 */

public class UndoManager
    implements LWComponent.Listener
{
    private static boolean sInUndo = false;

    private ArrayList mUndoActions = new ArrayList(); // the list of undo actions (named groups of property changes)
    
    private Map mPropertyChanges = new HashMap(); // all property changes, mapped by component, since last mark
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
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                if (DEBUG.UNDO) System.out.println("\tprocessing " + e.getKey());
                undoComponentChanges((LWComponent) e.getKey(), (Map) e.getValue());
            }
        }

        private void undoComponentChanges(LWComponent c, Map props)
        {
            Iterator i = props.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                if (DEBUG.UNDO) System.out.println("\tundoing " + e);
                Object propKey = e.getKey();
                Object oldValue = e.getValue();
                if (oldValue instanceof Undoable)
                    ((Undoable)oldValue).undo();
                else
                    tufts.vue.beans.VueLWCPropertyMapper.setProperty(c, propKey, oldValue);
            }
        }

        public String toString() {
            return "UndoAction[" + name
                + " cnt=" + propertyChangeCount
                + " propertyChanges=" + propertyChanges.size() + "]";
        }
    }

    public UndoManager(LWMap map)
    {
        map.addLWCListener(this);
        setUndoActionLabel(null); // disable undo action at start
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

    public void redo()
    {
        if (mChangeCount > 0) new Throwable(this + " UNMARKED CHANGES IN REDO! " + mPropertyChanges);
        if (DEBUG.UNDO) System.out.println(this + ": REDO");
    }
    
    public void undo()
    {
        if (mChangeCount > 0) new Throwable(this + " UNMARKED CHANGES IN UNDO! " + mPropertyChanges);
        
        UndoAction undoAction = pop();
        if (DEBUG.UNDO) System.out.println("UNDO: undoing " + undoAction);
        if (undoAction != null) {
            sInUndo = true;
            try {
                undoAction.undoPropertyChanges();
            } finally {
                sInUndo = false;
            }
        }
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
        if (DEBUG.UNDO) System.out.println("UNDO: new UndoAction '" +  name + "'");
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
        UndoAction newUndoAction = new UndoAction(name, mPropertyChanges, mChangeCount);
        mUndoActions.add(newUndoAction);
        if (DEBUG.UNDO) System.out.println("UNDO: marked " + mChangeCount + " property changes under '" + name + "'");
        setUndoActionLabel(newUndoAction);
        mPropertyChanges = new HashMap();
        mLastEvent = null;
        mChangeCount = 0;
    }

    public void LWCChanged(LWCEvent e) {
        if (sInUndo) {
            if (DEBUG.UNDO) System.out.println("\tredo: " + e);
            return;
        }
        if (DEBUG.UNDO) System.out.print("UNDO: " + e);

        if (e.hasOldValue()) {
            recordUndoablePropertyChangeEvent(e);
        } else {
            if (DEBUG.UNDO) {
                System.out.println(" (ignored: no old value)");
                if (DEBUG.META) new Throwable().printStackTrace();
            }
        }
    }

    private void recordUndoablePropertyChangeEvent(LWCEvent e)
    {
        String propName = e.getWhat();
        LWComponent c = e.getComponent(); // can be list...
        boolean compressed = false; // already had one of these props: can ignore all subsequent
        
        Object oldValue = e.getOldValue();
        Map cPropList = (Map) mPropertyChanges.get(c);
        if (cPropList != null) {
            //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
            Object value = cPropList.get(propName);
            if (value != null) {
                if (DEBUG.UNDO) System.out.println(" (compressed)");
                compressed = true;
            }
        } else {
            cPropList = new HashMap();
            mPropertyChanges.put(c, cPropList);
        }
        
        if (!compressed) {
            cPropList.put(propName, oldValue);
            mChangeCount++;
            mLastEvent = e;
            if (DEBUG.UNDO) {
                if (DEBUG.META) System.out.println(" (stored: " + oldValue + ")");
                else System.out.println(" (stored)");
            }
        }
    }

    public String toString()
    {
        return "UndoManager[" + mChangeCount + "]";
    }
    
}

