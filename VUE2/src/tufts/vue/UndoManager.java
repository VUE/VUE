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

    private ArrayList mChanges;
    private ArrayList mUndoActions = new ArrayList();
    private Map mPropertyChanges = new HashMap();

    /**
     * A list (map) of components each with a list (map) of property changes to them.
     */
    static class UndoAction {
        String name;
        Map propertyChanges;
        UndoAction(String name, Map propertyChanges) {
            this.name = name;
            this.propertyChanges = propertyChanges;
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
            return "UndoAction[" + name + " propertyChanges=" + propertyChanges.size() + "]";
        }
    }

    public UndoManager(LWMap map)
    {
        map.addLWCListener(this);
        mChanges = new ArrayList();
    }

    private UndoAction pop()
    {
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

    public void undo()
    {
        UndoAction undoAction = pop();
        if (DEBUG.UNDO) System.out.println(undoAction + ": UNDO");
        sInUndo = true;
        try {
            undoAction.undoPropertyChanges();
        } finally {
            sInUndo = false;
        }
        setUndoActionLabel(peek());
    }

    private void setUndoActionLabel(UndoAction ua)
    {
        String name = "Undo ";

        if (ua != null) {
            if (DEBUG.UNDO||DEBUG.EVENTS) name += "#" + mUndoActions.size() + " ";
            String uaName = ua.name;
            if (Character.isLowerCase(uaName.charAt(0)))
                uaName = Character.toUpperCase(uaName.charAt(0)) + uaName.substring(1);
            name += uaName;
            //if (DEBUG.UNDO||DEBUG.EVENTS) name += " (" + ua.propertyChanges.size() + ")";
            if (DEBUG.UNDO||DEBUG.EVENTS) name += " (" + mPropCount + ")";
            Actions.Undo.setEnabled(true);
        } else {
            Actions.Undo.setEnabled(false);
        }
        Actions.Undo.putValue(Action.NAME, name);
    }
    
    /** figure the name of the undo action from the last LWCEvent we processed */
    public void markChangesForUndo() {
        markChangesAsUndo(null);
    }

    private LWCEvent mLastEvent;
    private int mPropCount;
    public synchronized void markChangesAsUndo(String name)
    {
        if (name == null) {
            if (mLastEvent == null)
                return;
            name = mLastEvent.getWhat();
        }
        UndoAction newUndoAction = new UndoAction(name, mPropertyChanges);
        mUndoActions.add(newUndoAction);
        if (DEBUG.UNDO) System.out.println("UNDO: marked  " + name + " with cnt=" + mPropCount);
        setUndoActionLabel(newUndoAction);
        mPropertyChanges = new HashMap();
        mPropCount = 0;
    }

    public void LWCChanged(LWCEvent e) {
        if (sInUndo) {
            if (DEBUG.UNDO) System.out.println("\tredo: " + e);
            return;
        }
        if (DEBUG.UNDO) System.out.print("UNDO: " + e);
        String propName = e.getWhat();
        LWComponent c = e.getComponent(); // can be list...
        if (e.hasOldValue()) {
            Object oldValue = e.getOldValue();
            if (DEBUG.UNDO&&DEBUG.META) System.out.println(" old=" + oldValue + " ");
            Map propList = (Map) mPropertyChanges.get(c);
            if (propList != null) {
                //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
                Object value = propList.get(propName);
                if (value != null) {
                    if (DEBUG.UNDO) System.out.println(" (compressed)");
                } else {
                    propList.put(propName, oldValue);
                    mPropCount++;
                    mLastEvent = e;
                    if (DEBUG.UNDO) System.out.println(" (stored)");
                }
            } else {
                propList = new HashMap();
                propList.put(propName, oldValue);
                mPropCount++;
                mLastEvent = e;
                mPropertyChanges.put(c, propList);
                if (DEBUG.UNDO) System.out.println(" (stored)");
            }
        } else {
            if (DEBUG.UNDO) {
                System.out.println(" (unhandled)");
                if (DEBUG.META) new Throwable().printStackTrace();
            }
        }
    }

    public String toString()
    {
        return "UndoManager[]";
    }
    
}

